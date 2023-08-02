package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import javax.sound.sampled.BooleanControl;

import org.json.simple.*;
import org.json.simple.parser.*;

import myutils.*;

public class GameInfo {

    private static final String ACCOUNTS_PATH = "./classes/server/data/accounts";
    private static final String GAMES_PATH = "./classes/server/data/games";
    private static final String PHRASES_FILE = "./classes/server/data/phrases/phrases.json";

    // main threadPool
    MyThreadPool mainThreadPool = new MyThreadPool(15, 30);

    // verify connection ThreadPool
    MyThreadPool connectionThreadPool = new MyThreadPool(15, 30);
    private ListData<String> connectionsData;

    //account, json
    private HashMap<String, PlayerData> accounts = new HashMap<>();

    // games json
    private HashMap<Integer, Pair<GameData, MyThreadPool>> currentGames = new HashMap<>();

    // waiting queues
    MyThreadPool queuesThreadPool = new MyThreadPool(5,20);
    QueueData<String> casualQueue;
    MyAtomicBoolean casualQueueBlocked;
    QueueData<Pair<String, Integer>> rankedQueue;
    MyAtomicBoolean rankedQueueBlocked;

    public GameInfo() throws FileNotFoundException, IOException, ParseException {
        this.accounts = getAccountsRestart(ACCOUNTS_PATH);

        this.currentGames = getGamesRestart(GAMES_PATH);

        this.connectionsData = new ListData<String>();

        //start queues
        this.casualQueue = new QueueData<String>();
        this.rankedQueue = new QueueData<Pair<String, Integer>>();

        //define the atomic booleans
        this.casualQueueBlocked = new MyAtomicBoolean(false);
        this.rankedQueueBlocked = new MyAtomicBoolean(false);
    }

    // Getters
    public MyThreadPool getMainThreadPool(){
        return mainThreadPool;
    }

    public MyThreadPool getConnectionThreadPool(){
        return connectionThreadPool;
    }

    public ListData<String> getConnectionsData() {
        return connectionsData;
    } 

    public String getAccountPath(){
        return ACCOUNTS_PATH;
    } 

    public String getGamesPath(){
        return GAMES_PATH;
    } 
    
    public MyThreadPool getQueuesThreadPool(){
        return this.queuesThreadPool;
    } 

    public QueueData<String> getCasualQueue(){
        return this.casualQueue;
    }
    
    public QueueData<Pair<String, Integer>> getRankedQueue(){
        return this.rankedQueue;
    }

    public MyAtomicBoolean getCasualQueueBlocked(){
        return this.casualQueueBlocked;
    }

    public MyAtomicBoolean getRankedQueueBlocked(){
        return this.rankedQueueBlocked;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Account
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private HashMap<String, PlayerData> getAccountsRestart(String folderPath) throws FileNotFoundException, IOException, ParseException {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException(folderPath + " is not a directory");
        }
    
        File[] files = folder.listFiles();
        if (files == null) {
            throw new IOException("Error reading files in directory " + folderPath);
        }
    
        HashMap<String, PlayerData> accounts = new HashMap<>();
    
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                String username = file.getName().replace(".json", "");
    
                // read the JSON file
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(new FileReader(file));
                if(obj instanceof JSONObject){
                    JSONObject jsonObject = (JSONObject) obj;
                    
                    // extract the values
                    int rank = (int) (long) jsonObject.get("rank");
        
                    PlayerData playerData = new PlayerData(username, rank, folderPath+"/"+username+".json");
                    accounts.put(username,playerData);
                }
            }
        }
    
        return accounts;
    }

    // get info from specific account
    public Pair<String, String> getAccountLoginInfo(String username) throws IOException, ParseException {
        if(accounts.get(username) != null){
            return accounts.get(username).getAccountLoginInfo();
        }
        return null;
    }

    // create account object
    public Boolean addAccount(String username, String hashPassword, String salt) throws IOException, ParseException{
        PlayerData newPlayer = new PlayerData(username, hashPassword, salt, ACCOUNTS_PATH);
        accounts.put(username,newPlayer);
        return true;
    }

    // change accounts password
    public boolean updateAccountPassword(String username, String newPassword, String newSalt) throws IOException, ParseException {
        PlayerData playerData = accounts.get(username);
        if(playerData != null){
            playerData.changePassword(newPassword, newSalt);
            return true;
        }
        return false;
    }

    // changePLayer rank
    public void changePlayerRank(String username) throws IOException, ParseException {
        if (accounts.containsKey(username)) {
            accounts.get(username).changeRank();
        }
    }

    // view my own rank
    public int getPlayerRank(String username) {
        if (accounts.containsKey(username)) {
            return accounts.get(username).getPlayerRank();
        } else {
            return 0; // creates a default rank
        }
    }

    // get top 10 rank
    public List<Pair<String, Integer>> getTopPlayers() {
        List<Pair<String, Integer>> topPlayers = new ArrayList<>();
        
        // Convert map entries to a list of pairs
        List<Map.Entry<String, PlayerData>> list = new ArrayList<>(accounts.entrySet());
        
        // Sort the list based on the player rank in descending order
        Collections.sort(list, new Comparator<Map.Entry<String, PlayerData>>() {
            @Override
            public int compare(Map.Entry<String, PlayerData> o1, Map.Entry<String, PlayerData> o2) {
                return o2.getValue().getPlayerRank() - o1.getValue().getPlayerRank();
            }
        });
        
        // Get the top players from the sorted list, up to a maximum of 10
        int count = 0;
        for (Map.Entry<String, PlayerData> entry : list) {
            if (count == accounts.size()) {
                break;
            }
            topPlayers.add(new Pair<>(entry.getKey(), entry.getValue().getPlayerRank()));
            count++;
        }
        
        return topPlayers;
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    //  View on going games
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    // read game_casual -> list of games
    private HashMap<Integer, Pair<GameData, MyThreadPool>> getGamesRestart(String folderPath) throws FileNotFoundException, IOException, ParseException {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        HashMap<Integer, Pair<GameData, MyThreadPool>> games = new HashMap<>();

        int i = 0;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                String gameCode = file.getName().replace(".json", "");

                // read the JSON file
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(new FileReader(file));
                JSONObject jsonObject = (JSONObject) obj;

                // Modify the JSON object
                JSONArray players = (JSONArray) jsonObject.get("players");
                for (Object playerObj : players) {
                    JSONObject player = (JSONObject) playerObj;
                    player.put("present", false);
                }

                // Save the modified JSON back to file
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(jsonObject.toJSONString());
                }

                // extract the values
                int num_player = (int) (long) jsonObject.get("num_player");
                int rank = (int) (long) jsonObject.get("rank");

                GameData gameData = new GameData(Integer.parseInt(gameCode), rank, folderPath + "/" + gameCode + ".json");
                MyThreadPool gameThreadPool = new MyThreadPool(num_player + 1, num_player * 2);
                Pair<GameData, MyThreadPool> newgame = new Pair<>(gameData, gameThreadPool);
                games.put(Integer.parseInt(gameCode), newgame);
            }
        }

        return games;
    }


    // Return next availabel gamecode
    public int availGameCode(){
        int number = 1;
        while (currentGames.containsKey(number)) {
            number++;
        }
        return number;
    }

    //get current games (used in the restart of server)
    public HashMap<Integer, Pair<GameData, MyThreadPool>> getCurrentGames(){
        return this.currentGames;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Game
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Add a new game to the currentGames
    public boolean addGame(int gameCode, int rank, MyThreadPool gameThreadPool) {
        GameData gameMeta = new GameData(gameCode, rank, GAMES_PATH+"/"+Integer.toString(gameCode)+".json");
        Pair<GameData , MyThreadPool> newGame = new Pair<>(gameMeta, gameThreadPool);
        currentGames.put(gameCode, newGame);
        return true;
    }

    // Get a game
    public Pair<GameData, MyThreadPool> getGame(int gameCode) {
        return currentGames.get(gameCode);
    }

    // Remove a game from currentGames
    public void removeGame(int gameCode) {
        currentGames.remove(gameCode);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Phrases
    ///////////////////////////////////////////////////////////////////////////////////////////////

    //select 10 random strings from the phrases json (to minimize the chance of repeated one apearing)
    public static List<String> selectRandomPhrases(int numPhrases) throws IOException, ParseException {
        // Read the JSON file
        JSONParser parser = new JSONParser();
        Object obj = null;
        try {
            obj = parser.parse(new FileReader(PHRASES_FILE));
        } catch (IOException | ParseException e) {
            throw e;
        }
        JSONArray jsonArray = (JSONArray) obj;
    
        // Initialize a hashmap to store the strings and their indices
        Map<Integer, String> stringMap = new HashMap<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String text = (String) jsonObject.get("text");
            stringMap.put(i, text);
        }
    
        // Select random strings from the hashmap
        Random rand = new Random();
        List<String> selectedStrings = new ArrayList<>();
        for (int i = 0; i < numPhrases; i++) {
            int randomIndex = rand.nextInt(stringMap.size());
            String randomString = stringMap.get(randomIndex);
            selectedStrings.add(randomString);
            stringMap.remove(randomIndex); // remove the selected string from the map
        }
    
        return selectedStrings;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Get if there are games available in queues ( returns the number of games in the queue 
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public HashMap<Integer, Pair<GameData, MyThreadPool>> getAvailGamesCasual(){
        // Iterate over the HashMap
        HashMap<Integer, Pair<GameData, MyThreadPool>> returnHashMap = new HashMap<>();
        for (Map.Entry<Integer, Pair<GameData, MyThreadPool>> entry : this.currentGames.entrySet()) {
            GameData game = entry.getValue().getFirst();
            if( (game.getGameRank() == -1) &&  (!game.isGameFull() && (game.getGameState() == GameState.GAME_SETUP))){
                returnHashMap.put(entry.getKey(),entry.getValue());
                }
            }
        return returnHashMap;
    }
    
    public HashMap<Integer, Pair<GameData, MyThreadPool>> getAvailGamesRank(){
        // Iterate over the HashMap
        HashMap<Integer, Pair<GameData, MyThreadPool>> returnHashMap = new HashMap<>();
        for (Map.Entry<Integer, Pair<GameData, MyThreadPool>> entry : this.currentGames.entrySet()) {
            Boolean playerFoundGame = false;
            GameData game = entry.getValue().getFirst();
            if( (game.getGameRank() != -1) &&  (!game.isGameFull() && (game.getGameState() == GameState.GAME_SETUP))){
                returnHashMap.put(entry.getKey(),entry.getValue());
                }
            }
        return returnHashMap;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Verify if player is in game
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public Boolean verifyPlayerGaming(String username){
        for(HashMap.Entry<Integer, Pair<GameData, MyThreadPool>> gameObj : currentGames.entrySet()){
            try{
                GameData gameSpec = gameObj.getValue().getFirst();
                List<String> playersActiveInGame = gameSpec.getActiveUsernames();
                if (playersActiveInGame.contains(username))
                    return true;
            } catch (IOException | ParseException e){
                return false;
            }
        }
        return false;
    }

}

