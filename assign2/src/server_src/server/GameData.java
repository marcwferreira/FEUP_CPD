package server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Set;

import java.util.HashSet;


import org.json.simple.JSONArray;
import org.json.simple.parser.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import myutils.*;

public class GameData {

    private int gameCode;
    private int player_num;
    private int gameRank;
    private String filePath;

    //this is the permissions of a player in the game
    // First one is send phrase permission
    // Second one is vote permission
    private HashMap<String, Pair<Boolean, Boolean>> playerPermissions = new HashMap<>();

    //list of phrase submissions for the round
    private List<Pair<String, String>> roundSubmissions = new ArrayList<>();

    //verifies if the game is running
    private GameState gameState;
    private int judgeVote;

    private ReadWriteLock dataLock = new ReentrantReadWriteLock();
    private Lock readLock = dataLock.readLock();
    private Lock writeLock = dataLock.writeLock();

    public GameData(int gameCode, int gameRank, String filePath){
        this.gameCode = gameCode;
        this.gameRank = gameRank;
        this.filePath = filePath;
        this.gameState = GameState.GAME_SETUP;
        this.judgeVote = -1;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // getters
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public String getFilePath(){
        return this.filePath;
    }

    public int getGameRank(){
        return this.gameRank;
    }

    public int getGameCode(){
        return this.gameCode;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Get number of players
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public int getPlayerNum(){
        readLock.lock();
        try{
            return this.player_num;
        } finally {
            readLock.unlock();
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    // Player Functions
    //////////////////////////////////////////////////////////////////////////////

    //add player to the game or return true so it can rejoin
    public Boolean addPlayer(String username) throws IOException, ParseException {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        writeLock.lock();
        try {
            // Read the JSON file
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(filePath)) {
                JSONObject obj = (JSONObject) parser.parse(reader);
                JSONArray players = (JSONArray) obj.get("players");
                int numPlayer = (int) (long) obj.get("num_player");
                boolean isFull = (boolean) obj.get("is_full");
    
                // Check if the player is already in the list
                boolean playerFound = false;
                for (Object player : players) {
                    JSONObject playerObj = (JSONObject) player;
                    String currentPlayer = (String) playerObj.get("username");
                    if (currentPlayer.equals(username)) {
                        playerFound = true;
                        //Set player permission to false
                        playerPermissions.put(username, new Pair<>(false, false));
                        break;
                    }
                }
    
                if (!playerFound) {
                    if (players.size() < numPlayer) {
                        players.add(new JSONObject(Map.of("username", username, "present", true, "points", 0)));

                        // Add player permission entry to the HashMap
                       this. playerPermissions.put(username, new Pair<>(false, false));
                    }
    
                    if (players.size() >= numPlayer && !isFull) {
                        obj.put("is_full", true);
                        isFull = true;
                    }
    
                    // Write the updated JSON back to the file
                    try (FileWriter writer = new FileWriter(filePath)) {
                        obj.writeJSONString(writer);
                    }
                }
    
                return true;
            } catch (IOException | ParseException e) {
                e.printStackTrace();
                return false;
            }
        } finally {
            writeLock.unlock();
        }
    }
 
    //return list of usernames
    public List<String> getUsernames() throws IOException, ParseException {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        readLock.lock();
        try{
            JSONParser parser = new JSONParser();
            List<String> usernames = new ArrayList<>();

            try (FileReader reader = new FileReader(filePath)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                JSONArray playersArray = (JSONArray) jsonObject.get("players");

                for (Object obj : playersArray) {
                    JSONObject player = (JSONObject) obj;
                    String username = (String) player.get("username");
                    usernames.add(username);
                }
            }

            return usernames;
        } finally {
            readLock.unlock();
        }
    }

    //return list of usernames that are active in the game
    public List<String> getActiveUsernames() throws IOException, ParseException {
        if (this.gameState == GameState.GAME_ENDED) {
            return new ArrayList<>();
        }
    
        readLock.lock();
        try {
            JSONParser parser = new JSONParser();
            List<String> usernames = new ArrayList<>();
    
            try (FileReader reader = new FileReader(this.filePath)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                JSONArray playersArray = (JSONArray) jsonObject.get("players");
    
                for (Object obj : playersArray) {
                    JSONObject player = (JSONObject) obj;
                    boolean present = (boolean) player.get("present");
                    if (present) {
                        String username = (String) player.get("username");
                        usernames.add(username);
                    }
                }
            }
    
            return usernames;
        } finally {
            readLock.unlock();
        }
    }

    //return if players are still in the game after its end
    public List<String> getRemainersUsernames() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        List<String> usernames = new ArrayList<>();

        try (FileReader reader = new FileReader(filePath)) {
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            JSONArray playersArray = (JSONArray) jsonObject.get("players");

            for (Object obj : playersArray) {
                JSONObject player = (JSONObject) obj;
                boolean present = (boolean) player.get("present");
                if (present) {
                    String username = (String) player.get("username");
                    usernames.add(username);
                }
            }
        }

        return usernames;
    }

    // Set player as not in the game
    public boolean setPlayerPresence(String username, Boolean active) throws IOException, ParseException {
        boolean success = false;
        writeLock.lock();
        try {
            JSONParser parser = new JSONParser();

            try (FileReader reader = new FileReader(filePath)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                JSONArray playersArray = (JSONArray) jsonObject.get("players");

                for (int i = 0; i < playersArray.size(); i++) {
                    JSONObject player = (JSONObject) playersArray.get(i);
                    String currentPlayerUsername = (String) player.get("username");
                    if (currentPlayerUsername.equals(username)) {
                        player.put("present", active);
                        playersArray.set(i, player); // Replace the existing player object with the updated version
                        success = true;
                        break;
                    }
                }

                // Write the modified JSON content back to the file
                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write(jsonObject.toJSONString());
                }
            }
        } finally {
            writeLock.unlock();
        }

        return success;
    }

    
    

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // PLayers Permissions
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public void changeSendPhrasePerm(String playerName, Boolean newPerm) {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return;

        writeLock.lock();
        try{
            Pair<Boolean, Boolean> permissions = playerPermissions.get(playerName);
            if (permissions != null) {
                playerPermissions.put(playerName, new Pair<>(newPerm, permissions.getSecond()));
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    public void changeVotePerm(String playerName, Boolean newPerm) {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return;

        writeLock.lock();
        try{
            Pair<Boolean, Boolean> permissions = playerPermissions.get(playerName);
            if (permissions != null) {
                playerPermissions.put(playerName, new Pair<>(permissions.getFirst(), newPerm));
            }
        } finally {
            writeLock.unlock();
        }
    }

    public Boolean getSendPhrasePerm(String playerName){

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return false;

        readLock.lock();
        try {
            Pair<Boolean, Boolean> permissions = playerPermissions.get(playerName);
            if (permissions == null) return false;
            return permissions.getFirst();
        } finally {
            readLock.unlock();
        }
    }

    public Boolean getVotePerm(String playerName){

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return false;

        readLock.lock();
        try {
            Pair<Boolean, Boolean> permissions = playerPermissions.get(playerName);
            if (permissions == null) return false;
            return permissions.getSecond();
        } finally {
            readLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Control game start
    ////////////////////////////////////////////////////////////////////////////////

    public Boolean isGameFull() {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        readLock.lock();
        try {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(filePath)) {
                JSONObject obj = (JSONObject) parser.parse(reader);
                return (Boolean) obj.get("is_full");
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        } finally {
            readLock.unlock();
        }
        return false;
    }

    public GameState getGameState(){

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        readLock.lock();
        try{
            return this.gameState;
        } finally {
            readLock.unlock();
        }
    }

    public Boolean setGameState(GameState newState){

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        writeLock.lock();
        try{
            this.gameState = newState;
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    //get Judge
    public Boolean startGameCheck() throws IOException, ParseException {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        readLock.lock();
        try{
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(this.filePath));
            JSONObject jsonObj = (JSONObject) obj;
            Boolean isFull = (Boolean) jsonObj.get("is_full");
            if(isFull){
                this.gameState = GameState.GAME_RUNNING;
            }
            return true;
        } finally{
            readLock.unlock();
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////
    // Cards
    //////////////////////////////////////////////////////////////////////////////

    //add card
    public void addCard(String newCard) {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return;

        writeLock.lock();
        try {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(filePath)) {
                // Parse the JSON file and get the root JSONObject
                JSONObject onj = (JSONObject) parser.parse(reader);
    
                // Get the "cards" array and add the new card
                JSONArray cards = (JSONArray) onj.get("cards");
                cards.add(newCard);
    
                // Update the "cards" field in the root object
                onj.put("cards", cards);
    
                // Write the modified JSON back to the file
                try (FileWriter writer = new FileWriter(filePath)) {
                    onj.writeJSONString(writer);
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        } finally {
            writeLock.unlock();
        }
    }

    //get hashmap of cards
    public Map<String, String> getCards() {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        Map<String, String> cardsMap = new HashMap<>();
        readLock.lock();
        try {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(filePath)) {
                JSONObject obj = (JSONObject) parser.parse(reader);
                JSONArray cards = (JSONArray) obj.get("cards");
                for (int i = 0; i < cards.size(); i++) {
                    String card = (String) cards.get(i);
                    cardsMap.put(card, card);
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        } finally {
            readLock.unlock();
        }
        return cardsMap;
    }

    // check if card was already played
    public Boolean cardExists(String cardString) {
        Map<String, String> cardsMap = getCards();
        return cardsMap.containsKey(cardString);
    }

    /////////////////////////////////////////////////////////////////////////////
    // Judges
    /////////////////////////////////////////////////////////////////////////////
    
    //change_judge
    public void changeJudge() {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return;

        writeLock.lock();
        try{
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(filePath)) {
                // Parse the JSON file and get the root JSONObject
                JSONObject obj = (JSONObject) parser.parse(reader);
        
                // Get the array of players and the current judge index
                JSONArray players = (JSONArray) obj.get("players");
                int judgeIndex = (int) (long) obj.get("judge");
        
                // Increment the judge index and wrap around to 0 if necessary
                judgeIndex = (judgeIndex + 1) % players.size();
        
                // Update the judge field in the root object
                obj.put("judge", judgeIndex);
        
                // Write the modified JSON back to the file
                try (FileWriter writer = new FileWriter(filePath)) {
                    obj.writeJSONString(writer);
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    //get Judge
    public Integer getJudge() throws IOException, ParseException {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        readLock.lock();
        try{
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(this.filePath));
            JSONObject jsonObj = (JSONObject) obj;
            int judge = (int) (long) jsonObj.get("judge");
            return judge;
        } finally{
            readLock.unlock();
        }
    }

    //get judge username
    public String getCurrentJudgeUsername() {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        readLock.lock();
        try {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(filePath)) {
                // Parse the JSON file and get the root JSONObject
                JSONObject obj = (JSONObject) parser.parse(reader);
        
                // Get the array of players and the current judge index
                JSONArray players = (JSONArray) obj.get("players");
                int judgeIndex = (int) (long) obj.get("judge");
        
                // Retrieve the username of the player at the current judge index
                JSONObject judge = (JSONObject) players.get(judgeIndex);
                return (String) judge.get("username");
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }

    private String getPrivateJudgeUsername(){
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(filePath)) {
            // Parse the JSON file and get the root JSONObject
            JSONObject obj = (JSONObject) parser.parse(reader);
    
            // Get the array of players and the current judge index
            JSONArray players = (JSONArray) obj.get("players");
            int judgeIndex = (int) (long) obj.get("judge");
    
            // Retrieve the username of the player at the current judge index
            JSONObject judge = (JSONObject) players.get(judgeIndex);
            return (String) judge.get("username");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    //change judge vote
    public Boolean judgeVoting(int newVote) {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        writeLock.lock();
        try {
            if (newVote >= 0 && newVote < roundSubmissions.size()) {
                this.judgeVote = newVote;
                return true;
            } else {
                return false;
            }
        } finally {
            writeLock.unlock();
        }
    }

    //clear judge vote and return 
    private void clearJudgeVote(){
        this.judgeVote = -1;
    }

    public int getJudgeVote(){
        readLock.lock();
        try{
            return this.judgeVote;
        } finally{
            readLock.unlock();
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    // Round Submissions
    //////////////////////////////////////////////////////////////////////////////////////////////

    public void addRoundSubmission(String username, String phraseSubmitted) {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return;

        writeLock.lock();
        try {
            roundSubmissions.add(new Pair<>(username, phraseSubmitted));
        } finally {
            writeLock.unlock();
        }
    }

    public List<String> getRoundSubmissions() {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        readLock.lock();
        try{
            List<String> submissionsList = new ArrayList<>();
            for (int i = 0; i < roundSubmissions.size(); i++) {
                Pair<String, String> submission = roundSubmissions.get(i);
                String formattedSubmission = i + ". " + submission.getSecond();
                submissionsList.add(formattedSubmission);
            }
            return submissionsList;
        } finally {
            readLock.unlock();
        }
    }
    
    private void clearRoundSubmissions() {
        roundSubmissions.clear();
    }

    public Boolean verifyIfAllSubmissions() throws FileNotFoundException, IOException, ParseException {

        // Do not allow players to access a game that has ended
        if (this.gameState == GameState.GAME_ENDED) return null;
    
        readLock.lock();
        try {
            // Set of players with submissions
            Set<String> playersWithSubmissions = new HashSet<>();
            for (Pair<String, String> pair : roundSubmissions) {
                playersWithSubmissions.add(pair.getFirst());
            }
    
            // Set of players in the game (not necessarily online)
            Set<String> playersInGame = new HashSet<>();
    
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(filePath)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                JSONArray playersArray = (JSONArray) jsonObject.get("players");
    
                for (Object obj : playersArray) {
                    JSONObject player = (JSONObject) obj;
                    String username = (String) player.get("username");
                    playersInGame.add(username);
                }
            }
    
            String judgeUsername = getPrivateJudgeUsername();
            if (judgeUsername != null) {
                playersInGame.remove(judgeUsername);
            }
    
            return playersWithSubmissions.equals(playersInGame);
    
        } finally {
            readLock.unlock();
        }
    }
    


///////////////////////////////////////////////////////////////////////////////////////////////
// Public get vote info 
///////////////////////////////////////////////////////////////////////////////////////////////

    public String getUsernameFromVotePublic () {
        readLock.lock();
        try{
            if(this.judgeVote == -1) return null;
            Pair<String, String> pair = roundSubmissions.get(this.judgeVote);
            return pair.getFirst();
        } finally{
            readLock.unlock();
        }
    }

    public String getPhraseFromVotePublic () {
        readLock.lock();
        try{
            if(this.judgeVote == -1) return null;
            Pair<String, String> pair = roundSubmissions.get(this.judgeVote);
            return pair.getSecond();
        } finally{
            readLock.unlock();
        }
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Give Point to player 
    ///////////////////////////////////////////////////////////////////////////////////////////////
    
    //find the player that got the point
    private String getUsernameFromVote() {
        Pair<String, String> pair = roundSubmissions.get(this.judgeVote);
        return pair.getFirst();
    }

    //add point to player
    private void addPoint(String username) {

        JSONParser parser = new JSONParser();
    
        try (FileReader reader = new FileReader(filePath)) {
            // Parse the JSON file and get the root JSONObject
            JSONObject obj = (JSONObject) parser.parse(reader);
    
            // Get the array of players
            JSONArray players = (JSONArray) obj.get("players");
    
            // Find the user with the given username and increment their points
            for (int i = 0; i < players.size(); i++) {
                JSONObject player = (JSONObject) players.get(i);
                String playerUsername = (String) player.get("username");
                if (playerUsername.equals(username)) {
                    long points = (long) player.get("points");
                    points++;
                    player.put("points", points);
                    break;
                }
            }
    
            // Write the modified JSON back to the file
            try (FileWriter writer = new FileWriter(filePath)) {
                obj.writeJSONString(writer);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    // round add point -> it adds point to player and cleans the submission list
    public String roundEnd(){

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        writeLock.lock();
        try{
            String playerPoint = null;
            if(this.judgeVote != -1){
                playerPoint = getUsernameFromVote();
                addPoint(playerPoint);
                clearJudgeVote();
            }
            clearRoundSubmissions();
            return playerPoint;
        } finally {
            writeLock.unlock();
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Points
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public String getPlayerPoints() {
        writeLock.lock();
        try{
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(this.filePath)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);

                JSONArray playersArray = (JSONArray) jsonObject.get("players");

                Map<String, Integer> playerPoints = new HashMap<>();
                for (Object obj : playersArray) {
                    JSONObject player = (JSONObject) obj;
                    String username = (String) player.get("username");
                    long points = (long) player.get("points");
                    playerPoints.put(username, (int) points);
                }

                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Integer> entry : playerPoints.entrySet()) {
                    String username = entry.getKey();
                    int points = entry.getValue();
                    sb.append(username).append(":").append(points).append("|");
                }

                // Remove the trailing "|" if there are players
                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }

                return sb.toString();
            } catch (IOException | ParseException e) {
                e.printStackTrace();
                return null;
            }
        } finally {
            writeLock.unlock();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Function to verify game end
    ///////////////////////////////////////////////////////////////////////////////////////////////

    //find if a player has n points
    public String getPlayerPointsWinner(int points) {

        //do not allows players to access a game that ended
        if(this.gameState == GameState.GAME_ENDED) return null;

        readLock.lock();
        try{
            try {
                JSONParser parser = new JSONParser();
                FileReader reader = new FileReader(filePath);
                JSONObject obj = (JSONObject) parser.parse(reader);
                JSONArray players = (JSONArray) obj.get("players");
                for (Object playerObj : players) {
                    JSONObject player = (JSONObject) playerObj;
                    if ( (int) (long) player.get("points") == points) {
                        return (String) player.get("username");
                    }
                }
                reader.close();
            } catch (IOException | ParseException e) {
                System.out.println("[GAME - Error] Failed reading winner in game.");
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }
    
}
