package server;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import myutils.*;

import org.json.simple.*;
import org.json.simple.parser.*;
import org.json.simple.parser.ParseException;

public class GameServer {

    private static final String SALT_ALGORITHM = "SHA1PRNG";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;
    
    // Info related to the game
    static GameInfo gameInfo;

    public static void main(String[] args) throws java.text.ParseException, FileNotFoundException, IOException, ParseException {

        if (args.length < 1) return;
        gameInfo = new GameInfo();

        //add games in gameInfo to execute in a theirThread -> this will make sure the players can enter the game to continue if they wish and as a garbage collector
        for (Integer key : gameInfo.getCurrentGames().keySet()) {
            // Get the value associated with the current key
            Pair<GameData, MyThreadPool> gameEntry = gameInfo.getCurrentGames().get(key);

        
            // Access the GameData and MyThreadPool objects in the Pair
            GameData gameData = gameEntry.getFirst();
            MyThreadPool gameThreadPool = gameEntry.getSecond();

            GameRun gameRun = new GameRun(gameData.getGameCode(), gameInfo);
            gameThreadPool.execute(gameRun);
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            //start connectionsHandler
            try {
                ConnectionHandler connectionHandler = new ConnectionHandler(gameInfo);
                gameInfo.getConnectionThreadPool().execute(connectionHandler);
            } catch (IOException ex) {
                throw ex;
            }

            //start gameQueueSystem
            try{
                GameQueueSystem gameQueueSystem = new GameQueueSystem(gameInfo, gameInfo.getQueuesThreadPool());
                gameInfo.getQueuesThreadPool().execute(gameQueueSystem);
            } catch (IOException ex){
                throw ex;
            }

            System.out.println("Server is listening on port " + port);

            //wait for players connections
            while (true) {
                Socket socket = serverSocket.accept();

                if(socket != null){
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            try{
                                handleConnection(socket);
                            } catch (IOException | ParseException | java.text.ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    gameInfo.getMainThreadPool().execute(task);
                }
                

            }           
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }   
    }

    //ConnectionHandler
    private static void handleConnection(Socket socket) throws IOException, ParseException, java.text.ParseException {
        try{
            Boolean connecting = true;
            InputStream input = socket.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            while(connecting){

                String myInput = reader.readLine();

                // Validate input using regular expression
                if (!myInput.matches("[a-zA-Z0-9@#$%&|\\-_,.]+")) {
                    writer.println("input-invalid-chars");
                    writer.flush();
                    continue;
                }

                String[] tokens = myInput.split("\\|");

                switch(tokens[0]){
                    case "register":
                        if(tokens.length == 3){
                            if(register(tokens[1], tokens[2])){
                                String loginToken = gameInfo.getConnectionsData().addConnection(tokens[1], socket);
                                if(loginToken != null){
                                    SessionHandler sessionHandler = new SessionHandler(socket, tokens[1], gameInfo);
                                    gameInfo.getMainThreadPool().execute(sessionHandler);
                                    writer.println("register-successful|"+loginToken);
                                    System.out.println("[CONNECTION] New user "+tokens[1]+" registered.");
                                    connecting = false;
                                } else{
                                    writer.println("register-failed");
                                    writer.flush();
                                }
                            }
                            else{
                                writer.println("register-failed");
                                writer.flush();
                            }
                        } else{
                            writer.println("input-invalid");
                            writer.flush();
                        }
                        break;
                    case "login":
                        if(tokens.length == 3){
                            if(login(tokens[1], tokens[2])){
                                if(!gameInfo.getConnectionsData().isConnected(tokens[1])){
                                    String loginToken = gameInfo.getConnectionsData().addConnection(tokens[1], socket);
                                    if(loginToken != null){
                                        SessionHandler sessionHandler = new SessionHandler(socket, tokens[1], gameInfo);
                                        gameInfo.getMainThreadPool().execute(sessionHandler);
                                        writer.println("login-successful|"+loginToken);
                                        System.out.println("[CONNECTION] "+tokens[1]+" logged in.");
                                        connecting = false;
                                    } else {
                                        writer.println("login-failed");
                                        writer.flush();
                                    }
                                } else{
                                    writer.println("login-failed-already");
                                    socket.close();
                                }
                                
                            }
                            else{
                                writer.println("login-failed");
                                writer.flush();
                            }
                        } else{
                            writer.println("input-invalid");
                            writer.flush();
                        }
                        break;
                    case "login-token":
                        if(tokens.length == 3){
                            if(gameInfo.getConnectionsData().allowLoginToken(tokens[1],tokens[2])){
                                String loginToken = gameInfo.getConnectionsData().addConnection(tokens[1], socket);
                                if(loginToken != null){
                                    String alertQueue = "|no-alert";
                                    if((gameInfo.getCasualQueue().verifyEntry(tokens[1]) != null) || (gameInfo.getRankedQueue().verifyEntry(tokens[1]) != null)){
                                        alertQueue = "|queue-alert";
                                    }
                                    SessionHandler sessionHandler = new SessionHandler(socket, tokens[1], gameInfo);
                                    gameInfo.getMainThreadPool().execute(sessionHandler);
                                    writer.println("login-token-successful|"+loginToken+alertQueue);
                                    System.out.println("[CONNECTION] "+tokens[1]+" logged in (token).");
                                    connecting = false;
                                } else {
                                    writer.println("login-failed");
                                    writer.flush();
                                }
                            } else {
                                writer.println("login-failed");
                                writer.flush();
                            }
                        } else{
                            writer.println("input-invalid");
                            writer.flush();
                        }
                        break;
                    case "leave":
                        connecting = false;
                        socket.close();
                        break;
                    default:
                        writer.println("input-invalid");
                        writer.flush();
                        break;
                }
            }
        } catch (IOException ex) {
            //throw ex;
        } catch (ParseException | NullPointerException ex) {
            //throw ex;
        }
    }

    //register
    public static boolean register(String username, String password) throws IOException, ParseException, java.text.ParseException {
        try{

            if(password.length() < 4 || username.length() < 4){
                return false;
            }
            
            Pair<String, String> account = new Pair<>();
            account = gameInfo.getAccountLoginInfo(username);
            if (account != null) {
                return false;
            } 

            byte[] salt = generateSalt();
            String hashedPassword = hashPassword(password, salt.toString().getBytes());

            gameInfo.addAccount(username, hashedPassword, salt.toString());

            return true;
        } catch (IOException ex){
            throw ex;
        } //catch (ParseException ex){
          //  throw ex;
         catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }


    //login
    private static boolean login(String username, String password) throws IOException, ParseException{
        try{
            Pair<String, String> account = new Pair<>();
            account = gameInfo.getAccountLoginInfo(username);
            if (account == null) {
                return false;
            }
            if(hashPassword(password, account.getSecond().getBytes()).equals(account.getFirst())){
                return true;
            }
            return false;
        } catch (IOException ex){
            throw ex;
        } catch (ParseException ex){
            throw ex;
        }
    }

    private static byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance(SALT_ALGORITHM);
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static String hashPassword(String password, byte[] salt) {
        String hashedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedBytes = md.digest(password.getBytes());
            hashedPassword = Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashedPassword;
    }
  
}

class SessionHandler extends Thread {

    private static final String SALT_ALGORITHM = "SHA1PRNG";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final String username;

    GameInfo gameInfo;

    public SessionHandler(Socket socket, String username, GameInfo gameInfo) throws IOException {
        this.socket = socket;
        this.username = username;
        this.gameInfo = gameInfo;

        InputStream input = socket.getInputStream();

        this.reader = new BufferedReader(new InputStreamReader(input));
        OutputStream output = socket.getOutputStream();
        this.writer = new PrintWriter(output, true);
    }

    public void run(){

        boolean menuConnection = true;
        while(menuConnection){
            try{
                String myInput = this.reader.readLine();

                if (myInput == null) {
                    menuConnection = false;
                    this.socket.close();
                    break;
                }

                if (gameInfo.verifyPlayerGaming(this.username))
                    return;

                // Validate input using regular expression
                if (!myInput.matches("[a-zA-Z0-9@#$%&|\\-_,.]+")) {
                    writer.println("input-invalid-chars");
                    writer.flush();
                    continue;
                }

                String[] tokens = myInput.split("\\|");

                if( (this.gameInfo.getCasualQueue().verifyEntry(this.username) != null)|| (this.gameInfo.getRankedQueue().verifyEntry(this.username) != null)){
                    switch(tokens[0]){
                        case "queue-leave":
                            if(tokens.length == 1){
                                if(this.gameInfo.getCasualQueue().verifyEntry(this.username) != null){
                                    //get casual queue control
                                    MyAtomicBoolean casualQueueBlocked = gameInfo.getCasualQueueBlocked();
                                    while(casualQueueBlocked.get()) { } //wait for it to be not blocked 
                                    casualQueueBlocked.set(true);
                                    Boolean leftQueue = gameInfo.getCasualQueue().removeEntryByValue(this.username);
                                    if(leftQueue){
                                        writer.println("queue-leave-successful");
                                        System.out.println("[QUEUE -  Casual] "+this.username+" left the queue.");
                                        
                                    } else{
                                        writer.println("queue-leave-failed");
                                        writer.flush();
                                    }
                                    casualQueueBlocked.set(false);
                                    break;
                                } else if(this.gameInfo.getRankedQueue().verifyEntry(this.username) != null) {
                                    //get casual queue control
                                    MyAtomicBoolean rankedQueueBlocked = gameInfo.getRankedQueueBlocked();
                                    while(rankedQueueBlocked.get()) { } //wait for it to be not blocked 
                                    rankedQueueBlocked.set(true);
                                    Boolean leftQueue = gameInfo.getRankedQueue().removeEntryByKey(this.username);
                                    if(leftQueue){
                                        writer.println("queue-leave-successful");
                                        System.out.println("[QUEUE -  Ranked] "+this.username+" left the queue.");
                                    } else{
                                        writer.println("queue-leave-failed");
                                        writer.flush();
                                    }
                                    rankedQueueBlocked.set(false);
                                    break;
                                }
                                writer.println("queue-leave-failed");
                                writer.flush();
                            }
                            break;
                        case "leave":
                            menuConnection = false;
                            gameInfo.getConnectionsData().removeConnection(this.username);
                            this.socket.close();
                            break;
                        case "submission-send":
                        case "vote-send":
                            return;
                        case "session-exit-private":
                            menuConnection = false;
                            writer.println("session-exit-private-successful");
                            return;
                        default:
                            if (gameInfo.verifyPlayerGaming(this.username))
                                menuConnection = false;
                            this.writer.println("operation-invalid");
                            this.writer.flush();
                            break;
                    }

                } else{
                    switch(tokens[0]){
                        case "password-change":
                            if(tokens.length == 3){
                                try{
                                    if(this.changePassword(tokens[1], tokens[2])) writer.println("password-change-successful");
                                    else writer.println("password-change-failed");
                                } catch (ParseException | NoSuchAlgorithmException e){
                                    e.printStackTrace();
                                }
                            } else{
                                writer.println("input-invalid");
                                writer.flush();
                            }
                            break;
                        case "game-create":
                            if(tokens.length == 3){
                                int numPlayers = -1;
                                Boolean isRanked = false;
                                try{
                                    numPlayers = Integer.parseInt(tokens[1]);
                                    isRanked = Boolean.parseBoolean(tokens[2]);
                                } catch(Exception e){
                                    writer.println("game-create-failed");
                                    writer.flush();
                                }
                                int gameCode = createGame(numPlayers, isRanked);
                                Boolean enteredGame = false;
                                if(gameCode != -1) enteredGame = joinGame(this.username, gameCode);
                                if(enteredGame){
                                    writer.println("game-create-successful|"+gameCode);
                                    menuConnection = false; //to free a task for the mainThreadPool instead of hoarding it until player comes back
                                    System.out.println("[GAME] "+this.username+" created a new game.");
                                } else{
                                    writer.println("game-create-failed");
                                    writer.flush();
                                }
                            }
                            break;
                        case "casual-queue-join":
                            if(tokens.length == 1){
                                Boolean enteredQueue = enterCasualQueue();
                                if(enteredQueue){
                                    writer.println("casual-queue-join-successful");
                                    System.out.println("[QUEUE -  Casual] "+this.username+" entered the queue.");
                                } else{
                                    writer.println("casual-queue-join-failed");
                                    writer.flush();
                                }
                            }
                            break;
                        case "ranked-queue-join":
                            if(tokens.length == 1){
                                Boolean enteredQueue = enterRankedQueue();
                                if(enteredQueue){
                                    writer.println("ranked-queue-join-successful");
                                    System.out.println("[QUEUE -  Ranked] "+this.username+" entered the queue.");
                                } else{
                                    writer.println("ranked-queue-join-failed");
                                    writer.flush();
                                }
                            }
                            break;
                        case "rejoin":
                            if(tokens.length == 2){
                                int gameCode = Integer.parseInt(tokens[1]);
                                Boolean enteredGame = false;
                                if(gameCode != -1) enteredGame = joinGame(this.username, gameCode);
                                if(enteredGame){
                                    writer.println("rejoin-successful");
                                    menuConnection = false;
                                    System.out.println("[GAME] "+this.username+" rejoined a game.");
                                } else{
                                    writer.println("rejoin-failed");
                                    writer.flush();
                                }
                            }
                            break;
                        case "rank-show":
                            if(tokens.length == 1){
                            writer.println(this.showRank());
                            } else{
                                writer.println("input-invalid");
                                writer.flush();
                            }
                            break;
                        case "leave":
                            menuConnection = false;
                            gameInfo.getConnectionsData().removeConnection(this.username);
                            this.socket.close();
                            System.out.println("[CONNECTION] "+this.username+" logged out.");
                            break;
                        case "game-leave":
                            break;
                        case "submission-send":
                        case "vote-send":
                        case "session-exit-private":
                            menuConnection = false;
                            writer.println("session-exit-private-successful");
                            return;
                        default:
                            if (gameInfo.verifyPlayerGaming(this.username)){
                                menuConnection = false;
                                return;
                            }
                            this.writer.println("operation-invalid");
                            this.writer.flush();
                            break;
                    }
                }
            } catch (IOException | ParseException ex) {
                continue;
            }
        }    
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Game options
    ////////////////////////////////////////////////////////////////////////////////////////////

    // create game
    private int createGame(int num_players, Boolean isRanked) throws IOException{

        if(num_players < 2 || num_players > 8) {
            return -1;
        }

        String folderPath = gameInfo.getGamesPath();
        int gameCode = this.gameInfo.availGameCode();

        int gameRank = -1;
        if(isRanked) gameRank = gameInfo.getPlayerRank(this.username);

        JSONObject json = new JSONObject();
        JSONArray players = new JSONArray();
        json.put("players", new JSONArray());
        json.put("cards", new JSONArray());
        json.put("judge", 0);
        json.put("num_player", num_players);
        json.put("is_full", false);
        json.put("rank",gameRank);

        try (FileWriter writer = new FileWriter(folderPath+"/"+gameCode+".json")) {
            writer.write(json.toJSONString());
        } catch (IOException ex) {
            throw ex;
        }

        // create threadPool for the game
        MyThreadPool gameThreadPool = new MyThreadPool(num_players+3, num_players*2);

        gameInfo.addGame(gameCode, gameRank, gameThreadPool);

        GameRun gameRun = new GameRun(gameCode, gameInfo);
        gameThreadPool.execute(gameRun);

        return gameCode;
    }

     
    // Entter casual queue
    private Boolean enterCasualQueue(){
        //check if there is games available
        if(!this.gameInfo.getAvailGamesCasual().isEmpty()){
            gameInfo.getCasualQueue().addEntry(this.username);
            return true;
        }
        else return false;
    }
    
    // Enter ranked queue
    private Boolean enterRankedQueue(){
        //check if there is games available
        if(!this.gameInfo.getAvailGamesRank().isEmpty()){
            Pair<String, Integer> newEntry = new Pair<>(username, 0);
            gameInfo.getRankedQueue().addEntry(newEntry);
            return true;
        }
        else return false;
    }
    

    //join game
    private Boolean joinGame(String username, int gameCode) throws IOException, ParseException{

        if(gameCode == -1) return false;

        Pair<GameData, MyThreadPool> gameObject = gameInfo.getGame(gameCode);
        if(gameObject == null) return false;
        GameData enterGame = gameObject.getFirst();
        if(enterGame == null) return false;

        //verify if game is a game that has already started in this case verify if player is in game
        if(enterGame.getGameState() == GameState.GAME_RUNNING){
            List<String> playersGame = enterGame.getUsernames();
            if(!playersGame.contains(this.username)){
                return false;
            }
        } 

        Boolean enteredGame = enterGame.addPlayer(this.username);

        Pair<GameData, MyThreadPool> gameLoad = this.gameInfo.getGame(gameCode);
        GameData gameData = gameLoad.getFirst();
        MyThreadPool gameThreadPool = gameLoad.getSecond(); 

        //add players as present at the game
        gameData.setPlayerPresence(this.username, true);

        //throw the player to the playerHandler
        PlayerHandler playerHandler = new PlayerHandler(this.username, gameCode, gameInfo);
        gameThreadPool.execute(playerHandler);

        gameData.startGameCheck();

        return enteredGame;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    // account options
    /////////////////////////////////////////////////////////////////////////////////////////

    private Boolean changePassword(String oldPassword, String newPassword) throws IOException, ParseException, NoSuchAlgorithmException{
        try {

            if(newPassword.length() < 4){
                return false;
            }

            Pair<String, String> account = gameInfo.getAccountLoginInfo(this.username);
            if (account == null) {
                return false;
            }
            byte[] oldSalt = account.getSecond().getBytes();
            String oldHashedPassword = hashPassword(oldPassword, oldSalt);
            if (!oldHashedPassword.equals(account.getFirst())) {
                return false;
            }
            byte[] newSalt = generateSalt();
            String newHashedPassword = hashPassword(newPassword, newSalt.toString().getBytes());
            gameInfo.updateAccountPassword(this.username, newHashedPassword, newSalt.toString());
            return true;
        } catch (IOException | ParseException | NoSuchAlgorithmException e) {
            throw e;
        }
    }

    private static byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance(SALT_ALGORITHM);
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static String hashPassword(String password, byte[] salt) {
        String hashedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedBytes = md.digest(password.getBytes());
            hashedPassword = Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashedPassword;
    }

    public String showRank() {
        List<Pair<String, Integer>> topPlayers = gameInfo.getTopPlayers();
        StringBuilder sb = new StringBuilder();
        Boolean playerInRank = false;
        
        int count = 0;
        for (Pair<String, Integer> entry : topPlayers) {
            if(entry.getFirst().equals(this.username)) playerInRank = true;
            sb.append((count + 1) + " " + entry.getFirst() + " " + entry.getSecond()+"|");
            count++;
        }

        //Print players rank
        if (!playerInRank) {
            sb.append("\n" + this.username + " " + gameInfo.getPlayerRank(this.username)+"|");
        }
        
        return sb.toString();
    }
    
    

}

class ConnectionHandler extends Thread {

    //gameInfo
    GameInfo gameInfo;

    //Scheduler
    private final MySchedulerRepeater schedulerRepeater;

    public ConnectionHandler(GameInfo gameInfo) throws IOException {
        this.gameInfo = gameInfo;
        this.schedulerRepeater = new MySchedulerRepeater();
    }

    public void run() {
        schedulerRepeater.scheduleRepeat(() -> {
            System.out.println("[SERVER] verifying connections.");
            gameInfo.getConnectionsData().verifyConnectionAndUpdateTimestamps();
        }, 1, 1, MyTimeUnit.MINUTES);
    }
}