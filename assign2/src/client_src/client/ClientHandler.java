package client;

import java.net.*;
import java.io.*;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Scanner;

import javax.crypto.spec.GCMParameterSpec;
import javax.security.auth.login.LoginContext;

import org.json.simple.JSONArray;
import org.json.simple.parser.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import myutils.*;
 

public class ClientHandler {

    private String filePath = "./classes/client/data/info.json";
    private String hostname = "127.0.0.1";
    private int port = 8450;
    private Socket socket;

    private ClientState currentState;

    public ClientHandler(String hostname, int port, String filePath){
        this.hostname = hostname;
        this.port = port;
        if(!filePath.equals("default"))
            this.filePath = filePath;
    }

    public boolean runClient(Boolean uncheckedStart) {
        
        if(uncheckedStart)
            currentState = ClientState.LOGIN;
        else
            currentState = ClientState.RESTORE;

        try {
            //try to start a socket 
            this.socket = new Socket(hostname, port);
            Scanner scanner = new Scanner(System.in);

            boolean exit = false;

            while (!exit) {
                switch (this.currentState) {
                    case RESTORE:
                        currentState = restoreSystem();
                        break;
                    case LOGIN:
                        currentState = loginSystem();
                        break;
                    case MENU:
                        currentState = menuSystem();
                        break;
                    case MENU_QUEUEING:
                        currentState = queueSystem();
                        break;
                    case GAME:
                        currentState = gameSystem();
                        break;
                    case CLOSE:
                        leaveAndClose();
                        exit = true;
                        return true;
                    default:
                        System.out.println("Invalid state");
                        return false;
                }
            }
        } catch (SocketException e) {
            // Connection lost with the server
            System.out.println("No connection with the server. Closing.");
            return false;
        }catch (UnknownHostException ex) {
            //System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            //System.out.println("I/O error: " + ex.getMessage());
        }
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // RESTORE PHASE
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private ClientState restoreSystem(){

        try{

            OutputStream output = this.socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            InputStream input = this.socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            //read info from json
            Pair<String, String> userInfo = parseInfoJson();
            String username = userInfo.getFirst();
            String loginToken = userInfo.getSecond();

            //verify if there is available info to login with token
            if( (username == null) || (loginToken == null) ) return ClientState.LOGIN;

            //try to login
            String commandSend = "login-token|"+username+"|"+loginToken;
            writer.println(commandSend);
            
            //wait server response
            String response = reader.readLine();
            String[] tokens = response.split("\\|");

            if(tokens[0].equals("login-token-successful")){
                updateTokenJson(username, tokens[1]);
                if(tokens.length == 3){
                    if(tokens[2].equals("queue-alert")) return ClientState.MENU_QUEUEING;
                    else return ClientState.MENU;
                }
            }

        } catch (SocketException e) {
            // Connection lost with the server
            System.out.println("You lost connection with the server. Closing.");
            return ClientState.CLOSE;
        } catch (IOException e){
            //e.printStackTrace();
        }

        return ClientState.LOGIN;   
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // LOGIN PHASE
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private ClientState loginSystem(){

        boolean successfulLogin = false;

        try{
            OutputStream output = this.socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            InputStream input = this.socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            int choice = -1;
            String username = null;
            String password = null;

            while (!successfulLogin) {
                System.out.println("+--------------------------------+");
                System.out.println("|      *** CARDS AGAINST ***     |");
                System.out.println("|        *** FEUP ***            |");
                System.out.println("+--------------------------------+");
                System.out.println("|  1. Login                      |");
                System.out.println("|  2. Register                   |");
                System.out.println("|  0. Leave Game                 |");
                System.out.println("+--------------------------------+");
                System.out.println();

                if (choice == -1) {
                    String inputString = readInputForPrompt("Enter your choice: ");
                    try {
                        choice = Integer.parseInt(inputString);
                    } catch (NumberFormatException e) {
                        System.out.println("+--------------------------------+");
                        System.out.println("|          Invalid input         |");
                        continue;
                    }
                }

                if (choice != 1 && choice != 2) {
                    if(choice == 0 ){
                        return ClientState.CLOSE;
                    }
                    System.out.println("+--------------------------------+");
                    System.out.println("|          Invalid choice        |");
                    choice = -1;
                    continue;
                }

                username = readInputForPrompt("Username: ");
                password = MyPasswordPrompt.readPasswordFromConsole("password: ");

                String commandSend = null;
                if (choice == 1) {
                    commandSend = "login|" + username + "|" + password;
                } else if (choice == 2) {
                    commandSend = "register|" + username + "|" + password;
                }

                // Send commandSend to the server and read the response
                writer.println(commandSend);
                String response = reader.readLine();
                
                if (response == null) {
                    // Connection lost with the server
                    System.out.println("\nYou lost connection with the server. Closing.");
                    return ClientState.CLOSE;
                }

                String[] tokens = response.split("\\|");

                if (tokens[0].equals("register-successful") || tokens[0].equals("login-successful") || tokens[0].equals("login-token-successful")) {
                    String newToken=tokens[1];
                    updateTokenJson(username,newToken);
                    successfulLogin = true;
                    return ClientState.MENU;
                } else if (tokens[0].equals("register-failed")){
                    System.out.println("\nRegistration failed");
                    choice = -1; // Prompt again for choice and credentials
                } else if (tokens[0].equals("login-failed")){
                    System.out.println("+--------------------------------+");
                    System.out.println("|           Login failed         |");
                    choice = -1; // Prompt again for choice and credentials
                } else if (tokens[0].equals("login-failed-already")){
                    System.out.println("+--------------------------------+");
                    System.out.println("|       User already logged in   |");
                    this.socket.close();
                    this.socket = new Socket(hostname, port);
                    choice = -1; // Prompt again for choice and credentials
                    return ClientState.LOGIN;
                } else if (tokens[0].equals("input-invalid")){
                    System.out.println("+--------------------------------+");
                    System.out.println("|           Invalid input        |");
                    choice = -1; // Prompt again for choice and credentials
                } else {
                    System.out.println("+--------------------------------+");
                    System.out.println("| Unexpected response from server|");
                    choice = -1; // Prompt again for choice and credentials
                }
            }
        } catch (SocketException e) {
            // Connection lost with the server
            System.out.println("\nYou lost connection with the server. Closing.");
            return ClientState.CLOSE;
        } catch (Exception e){
            return ClientState.LOGIN;
        }

        return ClientState.LOGIN;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // MENU PHASE
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private ClientState menuSystem() {
        boolean exit = false;

        try{

            OutputStream output = this.socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            InputStream input = this.socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            writer.println("game-leave");

            while (!exit) {

                System.out.println("+--------------------------------+");
                System.out.println("|              MENU              |");
                System.out.println("+--------------------------------+");
                System.out.println("|  1. Join Casual Queue          |");
                System.out.println("|  2. Join Ranked Queue          |");
                System.out.println("|  3. Create Game                |");
                System.out.println("|  4. Rejoin Game                |");
                System.out.println("|  5. Show Rank                  |");
                System.out.println("|  6. Change Password            |");
                System.out.println("|  0. Log Out                    |");
                System.out.println("+--------------------------------+");
                System.out.println();
        
                int choice = -1;
                while (choice == -1) {
                    String inputString = readInputForPrompt("Enter your choice: ");
                    try {
                        choice = Integer.parseInt(inputString);
                    } catch (NumberFormatException e) {
                        System.out.println("+--------------------------------+");
                        System.out.println("|           Invalid input        |");
                        System.out.println("+--------------------------------+");
                        System.out.println();
                        continue;
                    }
                }

                String response = null;
                String commandSend = null;
        
                switch (choice) {
                    case 1: // join casual queue
                        writer.println("casual-queue-join");
                        response = reader.readLine();
                        if (response == null) {
                            // Connection lost with the server
                            System.out.println("\nYou lost connection with the server. Closing.");
                            return ClientState.CLOSE;
                        }
                        if(response.equals("casual-queue-join-successful")){
                            return ClientState.MENU_QUEUEING;
                        } else if (response.equals("casual-queue-join-failed")){
                            System.out.println("+--------------------------------+");
                            System.out.println("|   Could not join casual queue  |");
                        } else {
                            System.out.println("+--------------------------------+");
                            System.out.println("| Unexpected response from server|");
                        }
                        break;
                    case 2: //join ranked queue
                        writer.println("ranked-queue-join");
                        response= reader.readLine();
                        if (response == null) {
                            // Connection lost with the server
                            System.out.println("\nYou lost connection with the server. Closing.");
                            return ClientState.CLOSE;
                        }
                        if(response.equals("ranked-queue-join-successful")){
                            return ClientState.MENU_QUEUEING;
                        } else if (response.equals("ranked-queue-join-failed")){
                            System.out.println("+--------------------------------+");
                            System.out.println("|   Could not join ranked queue  |");
                        } else{
                            System.out.println("+--------------------------------+");
                            System.out.println("| Unexpected response from server|");
                        }
                        break;
                    case 3: // create game
                        String gameRank = readGameTypeInput();
                        if(gameRank.equals("cancel")){
                            System.out.println("+--------------------------------+");
                            System.out.println("|     Canceled game creation     |");
                            break;
                        }
                        int numPlayers = getNumOfPlayers();
                        if(numPlayers == 0) {
                            System.out.println("+--------------------------------+");
                            System.out.println("| Canceled game creation / error |");
                            break;
                        }
                        commandSend = "game-create|"+numPlayers+"|"+gameRank;
                        writer.println(commandSend);
                        response = reader.readLine();
                        if (response == null) {
                            // Connection lost with the server
                            System.out.println("\nYou lost connection with the server. Closing.");
                            return ClientState.CLOSE;
                        }
                        String[] tokens = response.split("\\|");
                        if(tokens[0].equals("game-create-successful")){
                            if(tokens.length >= 2){
                                int gameCodeRcv = -1;
                                try{
                                    gameCodeRcv = Integer.parseInt(tokens[1]);
                                } catch ( NumberFormatException e){
                                    //do nothing
                                }
                                updateGameCodeJson(gameCodeRcv);
                            }
                            return ClientState.GAME;
                        } else if (tokens[0].equals("game-create-failed")){
                            System.out.println("+--------------------------------+");
                            System.out.println("|     Could not create game      |");
                        } else {
                            System.out.println("+--------------------------------+");
                            System.out.println("| Unexpected response from server|");
                        }
                        break;
                    case 4: //rejoin game
                        int gameCode = getGameCodeJson();
                        if(gameCode == -1) break;
                        writer.println("rejoin|"+gameCode);
                        response = reader.readLine();
                        if (response == null) {
                            // Connection lost with the server
                            System.out.println("\nYou lost connection with the server. Closing.");
                            return ClientState.CLOSE;
                        }
                        if(response.equals("rejoin-successful")){
                           return ClientState.GAME;
                        } else if (response.equals("rejoin-failed")){
                            System.out.println("+--------------------------------+");
                            System.out.println("|     Could not rejoin game      |");
                        } else {
                            System.out.println("+--------------------------------+");
                            System.out.println("| Unexpected response from server|");
                        }
                        break;
                    case 5: // show rank
                        writer.println("rank-show");
                        response = reader.readLine();
                        if (response == null) {
                            // Connection lost with the server
                            System.out.println("\nYou lost connection with the server. Closing.");
                            return ClientState.CLOSE;
                        }
                        response = removeBarEndString(response);
                        if(response != "input-invalid"){
                            String[] tokensRank = response.split("\\|");
                            displayRanks(tokensRank);
                        } else if (response == "input-invalid"){
                            System.out.println("+--------------------------------+");
                            System.out.println("|    Error - commands was wrong  |");
                        } else {
                            System.out.println("+--------------------------------+");
                            System.out.println("| Unexpected response from server|");
                        }
                        break;
                    case 6:  // change password
                        String oldPassword = MyPasswordPrompt.readPasswordFromConsole("Current password: ");
                        String newPassword = MyPasswordPrompt.readPasswordFromConsole("New password: ");
                        commandSend = "password-change|"+oldPassword+"|"+newPassword;
                        writer.println(commandSend);
                        response = reader.readLine();
                        if (response == null) {
                            // Connection lost with the server
                            System.out.println("\nYou lost connection with the server. Closing.");
                            return ClientState.CLOSE;
                        }
                        if(response.equals("password-change-successful")){
                            System.out.println("+--------------------------------+");
                            System.out.println("|  Password update successfully  |");
                        } else if (response.equals("password-change-failed")){
                            System.out.println("+--------------------------------+");
                            System.out.println("|  Password could not be updated |");
                        } else {
                            System.out.println("+--------------------------------+");
                            System.out.println("| Unexpected response from server|");
                        }
                        break;
                    case 0: // Leave
                        leaveAndClose();
                        updateTokenJson("", "");
                        this.socket.close();
                        this.socket = new Socket(hostname, port);
                        exit = true;    
                        return ClientState.LOGIN;
                    default:
                        System.out.println("+--------------------------------+");
                        System.out.println("|          Invalid choice        |");
                        break;
                }
            } 
        } catch (SocketException e) {
            // Connection lost with the server
            System.out.println("\nYou lost connection with the server. Closing.");
            return ClientState.CLOSE;
        } catch (Exception e) {
            if(this.socket.isClosed() || !this.socket.isConnected()) {
                System.out.println("\nYou lost connection with the server. Closing.");
                return ClientState.CLOSE;
            }
            return ClientState.MENU;
        }
        return ClientState.MENU;
    }
    
    private ClientState queueSystem() {
        boolean exit = false;
    
        System.out.println("+--------------------------------+");
        System.out.println("|          Entered queue         |");
        System.out.println("+--------------------------------+");
    
        try {
            OutputStream output = this.socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            InputStream input = this.socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    
            MyAtomicBoolean sendAvail = new MyAtomicBoolean(true);
    
            Thread inputThread = new Thread(() -> {
               
                while (sendAvail.get()) {
                    String inputString = readInputForPrompt(sendAvail);
                    try {
                        int commandSend = Integer.parseInt(inputString);
                        if (commandSend == 1) {
                            writer.println("queue-leave");
                            break; // Exit the inputThread loop
                        } else {
                            System.out.println("| Seaching for game. 1 to leave  |");
                            System.out.println("+--------------------------------+");
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            });
            inputThread.start();
    
            while (!exit) {
    
                // Stay listening for input
                String response = reader.readLine();
    
                if (response == null) {
                    // Connection lost with the server
                    System.out.println("\nYou lost connection with the server. Closing.");
                    sendAvail.set(false);
                    inputThread.interrupt();
                    return ClientState.CLOSE;
                }
    
                String[] tokens = response.split("\\|");
    
                if (tokens[0].equals("queue-leave-successful")) {
                    System.out.println("Leaving queue!");
                    sendAvail.set(false);
                    inputThread.interrupt();
                    return ClientState.MENU;
                } else if (tokens[0].equals("queue-leave-failed")) {
                    System.out.println("+--------------------------------+");
                    System.out.println("|       Failed leaving queue     |");
                    return ClientState.MENU_QUEUEING;
                } else if (tokens[0].equals("game-enter-successful")) {
                    if (tokens.length > 1) {
                        try {
                            int gameCode = Integer.parseInt(tokens[1]);
                            updateGameCodeJson(gameCode);
                        } catch (NumberFormatException e) {
                            System.out.println("+--------------------------------+");
                            System.out.println("|  Error - game code not saved   |");
                        }
                    }
                    sendAvail.set(false);
                    inputThread.interrupt();
                    writer.println("session-exit-private");
                    return ClientState.GAME;
                } else if (tokens[0].equals("casual-queue-empty")) {
                    sendAvail.set(false);
                    inputThread.interrupt();
                    System.out.println("+--------------------------------+");
                    System.out.println("|   No casual games available    |");
                    return ClientState.MENU;
                } else if (tokens[0].equals("ranked-queue-empty")) {
                    sendAvail.set(false);
                    inputThread.interrupt();
                    System.out.println("+--------------------------------+");
                    System.out.println("|   No ranked games available    |");
                    return ClientState.MENU;
                } else {
                    // Unexpected
                    System.out.println("+--------------------------------+");
                    System.out.println("| Unexpected response from server|");
                    return ClientState.MENU_QUEUEING;
                }
            }
        } catch (SocketException e) {
            // Connection lost with the server
            System.out.println("\nYou lost connection with the server. Closing.");
            return ClientState.CLOSE;
        } catch (Exception e) {
            if (this.socket.isClosed() || !this.socket.isConnected()) {
                System.out.println("\nYou lost connection with the server. Closing.");
                return ClientState.CLOSE;
            }
        }
        return ClientState.MENU_QUEUEING;
    }
    

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // GAME PHASE
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private ClientState gameSystem(){

        MyAtomicBoolean gameRunning = new MyAtomicBoolean(true);
        MyAtomicEnum<GamePermState> gamePermState = new MyAtomicEnum<>(GamePermState.class, GamePermState.NONE);
        String response = null;
        String roundsJudge = null;

        try{

            OutputStream output = this.socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            InputStream input = this.socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            writer.println("session-exit-private");

            System.out.println("+--------------------------------+");
            System.out.println("|       Entered Game Lobby       |");
            System.out.println("+--------------------------------+");
            System.out.println("|    Leave by sending: @leave    |");
            System.out.println("+--------------------------------+");
            System.out.println();
            System.out.println("input: ");

            Thread inputThread = new Thread(() -> {
                while(gameRunning.get()){
                    String inputString = readInputForPrompt(gameRunning);
                    try{
                        if(inputString.equals("@leave")){
                            writer.println("game-leave");
                        } else if (gamePermState.get().equals(GamePermState.SUBMISSIONS)){
                            writer.println("submission-send|"+inputString);
                        } else if (gamePermState.get().equals(GamePermState.VOTING)){
                            writer.println("vote-send|"+inputString);
                        } else{

                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            });
            inputThread.start();

            while(gameRunning.get()){
                try{

                    //stay listening to server
                    response = reader.readLine();

                    if (response == null) {
                        // Connection lost with the server
                        System.out.println("\nYou lost connection with the server. Closing.");
                        return ClientState.CLOSE;
                    }

                    String[] tokens = response.split("\\|");

                    switch(tokens[0]){
                        case "round-start":
                            roundsJudge = processRoundStart(tokens);
                            if(roundsJudge != null){
                                System.out.println("+--------------------------------+");
                                System.out.println("|         Round Starting         |");
                                System.out.println("+--------------------------------+");
                                System.out.println("|  Rounds' judge: "+roundsJudge);
                                System.out.println("+--------------------------------+");
                            } else{
                                System.out.println("|         Round Starting         |");
                                System.out.println("+--------------------------------+");
                                System.out.println("|    Error getting judge name    |");
                                System.out.println("+--------------------------------+");
                            }
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "submission-start":
                            String gamePhrase = processSubmissions(tokens);
                            if(gamePhrase != null){
                                System.out.println("+--------------------------------+");
                                System.out.println("|  Phrase: "+gamePhrase);
                                System.out.println("+--------------------------------+");
                                System.out.println("|     Please send you phrase     |");
                                System.out.println("+--------------------------------+");
                                System.out.println();
                            } else{
                                System.out.println("+--------------------------------+");
                                System.out.println("|       Error getting phrase     |");
                                System.out.println("+--------------------------------+");
                                System.out.println("|     Please send you phrase     |");
                                System.out.println("+--------------------------------+");
                                System.out.println();
                            }
                            System.out.println();
                            System.out.print("input: ");
                            gamePermState.set(GamePermState.SUBMISSIONS);
                            break;
                        case "vote-phase":
                            List<String> submissions = processVotingAndPoints(tokens);
                            if(!submissions.isEmpty()){
                                System.out.println("+--------------------------------+");
                                System.out.println("|  Submissions closed! Entries:  |");
                                System.out.println("+--------------------------------+");
                                for (String entry : submissions) {
                                    System.out.println(entry);
                                }
                                System.out.println("+--------------------------------+");
                            } else{
                                System.out.println("+--------------------------------+");
                                System.out.println("|    Error getting submissions   |");
                                System.out.println("+--------------------------------+");
                            }
                            System.out.println("|      Judge can now vote        |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            gamePermState.set(GamePermState.VOTING);
                            break;
                        case "round-end":
                            Pair<String, String> winnerReveal = processRoundEnd(tokens);
                            if(winnerReveal.getFirst() == null || winnerReveal.getSecond() == null){
                                System.out.println("+--------------------------------+");
                                System.out.println("|  Error getting round winner    |");
                                System.out.println("+--------------------------------+");
                            } else{
                                System.out.println("+--------------------------------+");
                                System.out.println("| Votinhg ended! Round winner is |");
                                System.out.println("+--------------------------------+");
                                System.out.println("|  Player: "+winnerReveal.getFirst());
                                System.out.println("|  Submissions: "+winnerReveal.getSecond());
                                System.out.println("+--------------------------------+");
                            }
                            System.out.println();
                            System.out.print("input: ");
                            gamePermState.set(GamePermState.NONE);
                            break;
                        case "points-update":
                            List<String> updatedPoints = processVotingAndPoints(tokens);
                            if(!updatedPoints.isEmpty()){
                                System.out.println("+--------------------------------+");
                                System.out.println("|      Updated score board       |");
                                System.out.println("+--------------------------------+");
                                for (String entry : updatedPoints) {
                                    System.out.println("=== "+entry);
                                }
                            } else{
                                System.out.println("+--------------------------------+");
                                System.out.println("|    Error getting score board   |");
                                System.out.println("+--------------------------------+");
                            }
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "game-end":
                            String winner = processGameEnd(tokens);
                            if(winner != null){
                                System.out.println("+--------------------------------+");
                                System.out.println("|          Game ended            |");
                                System.out.println("+--------------------------------+");
                                System.out.println("|  Winner: "+ winner);
                                System.out.println("+--------------------------------+");
                                System.out.println("|    Type @leave to leave game   |");
                                System.out.println("+--------------------------------+");
                            } else{
                                System.out.println("+--------------------------------+");
                                System.out.println("|          Game ended            |");
                                System.out.println("+--------------------------------+");
                                System.out.println("|   Error getting winners name   |");
                                System.out.println("+--------------------------------+");
                                System.out.println("|    Type @leave to leave game   |");
                                System.out.println("+--------------------------------+");
                            }
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "submission-successful":
                            System.out.println("+--------------------------------+");
                            System.out.println("|     Submission registred       |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "submission-invalid":
                            System.out.println("+--------------------------------+");
                            System.out.println("| Error on submission. Try again |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "vote-successful":
                            System.out.println("+--------------------------------+");
                            System.out.println("|        Vote registred          |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "vote-failed":
                            System.out.println("+--------------------------------+");
                            System.out.println("|    Error on vote. Try again    |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "vote-invalid":
                            System.out.println("+--------------------------------+");
                            System.out.println("|        You cannot vote         |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "game-leave-successful":
                            System.out.println("+--------------------------------+");
                            System.out.println("|     Successfully left game     |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            gameRunning.set(false);
                            return ClientState.MENU;
                        case "game-leave-failed":
                            System.out.println("+--------------------------------+");
                            System.out.println("|       Error leaving game       |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "game-leave-alert":
                            System.out.println("+--------------------------------+");
                            System.out.println("|        You left the game       |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            gameRunning.set(false);
                            return ClientState.MENU;
                        case "game-leave-alert-failed":
                            System.out.println("\nError automatically leaving game. Send @leave to leave.");
                            break;
                        case "output-invalid":
                            System.out.println("+--------------------------------+");
                            System.out.println("|         Output invalid         |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "input-invalid":
                            System.out.println("+--------------------------------+");
                            System.out.println("| Error communicating with server|");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "input-invalid-chars":
                            System.out.println("+--------------------------------+");
                            System.out.println("| Message contains invalid chars |");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                        case "session-exit-private-successful":
                            break;
                        default: 
                            System.out.println("+--------------------------------+");
                            System.out.println("| Unexpected response from server|");
                            System.out.println("+--------------------------------+");
                            System.out.println();
                            System.out.print("input: ");
                            break;
                    }
                   
                }catch (Exception e){
                    if(this.socket.isClosed() || !this.socket.isConnected()) {
                        System.out.println("\nYou lost connection with the server. Closing.");
                        return ClientState.CLOSE;
                    }
                    continue;
                }
            }
        } catch (SocketException e) {
            // Connection lost with the server
            System.out.println("\nYou lost connection with the server. Closing.");
            return ClientState.CLOSE;
        } catch (Exception e){
            if(this.socket.isClosed() || !this.socket.isConnected()) {
                System.out.println("\nYou lost connection with the server. Closing.");
                return ClientState.CLOSE;
            }
        }

        return ClientState.LOGIN;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //  CLOSE PHASE
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void leaveAndClose(){
        try {

            OutputStream output = this.socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            InputStream input = this.socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            writer.println("leave");


        } catch (Exception e){
            return;
        }
        return;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // UTILS
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static String readInputForPrompt(String prompt) {
        Scanner scanner = new Scanner(System.in);
    
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
    
        return input;
    }

    public static String readGameTypeInput(){
        System.out.println("+--------------------------------+");
        System.out.println("| Choose type of game:           |");
        System.out.println("+--------------------------------+");
        System.out.println("| 1. Casual Game                 |");
        System.out.println("| 2. Ranked Game                 |");
        System.out.println("| 0. cancel Game Creation        |");
        System.out.println("+--------------------------------+");
        System.out.println();

        String input = "";

        while(true){
            input = readInputForPrompt("Enter your choice: ");
            if(input.equals("1")){
                return "false";
            }
            else if(input.equals("2")){
                return "true";
            }
            else if(input.equals("0")){
                return "cancel";
            }
            else System.out.println("Invalid choice");
        }
    }

    private static int getNumOfPlayers(){
        String prompt = ("How many player (between 4 and 12 and 0 to cancel):");

        int numPlayers = -1;
        while(numPlayers == -1){
            String inputString = readInputForPrompt(prompt);
            try{
                numPlayers = Integer.parseInt(inputString);
                if ( (numPlayers >= 4 && numPlayers <= 8) || (numPlayers == 0) ){
                    return numPlayers;
                } else{
                    numPlayers = -1;
                    System.out.println("Please choose a valid number!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input");
            }
        }

        return 0;
    }

    private Pair<String, String> parseInfoJson() {
        Pair<String, String> resultPair = new Pair<>();
    
        File file = new File(this.filePath);
        if (!file.exists()) {
            // File does not exist, return null values
            return new Pair<>(null, null);
        }
    
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(new FileReader(file));
    
            String username = (String) json.get("username");
            String loginToken = (String) json.get("loginToken");
    
            resultPair = new Pair<>(username, loginToken);
        } catch (IOException | ParseException e) {
            return new Pair<>(null, null);
        }
    
        return resultPair;
    }
    
    private int getGameCodeJson(){
        int gameCode = -1;
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(new FileReader(this.filePath));

            gameCode = (int) (long) json.get("gameCode");

        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return -1;
        }

        return gameCode;
    }

    private void updateGameCodeJson(int gameCode) {
        JSONParser parser = new JSONParser();
        try (FileReader readerJson = new FileReader(this.filePath)) {
            JSONObject user = (JSONObject) parser.parse(readerJson);
            user.put("gameCode", gameCode);
            try (FileWriter writerJson = new FileWriter(this.filePath)) {
                writerJson.write(user.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
    
    private void updateTokenJson(String username, String newToken) {
        JSONParser parser = new JSONParser();
        try (FileReader readerJson = new FileReader(this.filePath)) {
            JSONObject user = (JSONObject) parser.parse(readerJson);
            user.put("username", username);
            user.put("loginToken", newToken);
            try (FileWriter writerJson = new FileWriter(this.filePath)) {
                writerJson.write(user.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e){
            createNewJsonFile(username, newToken);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void displayRanks(String[] tokens){

        System.out.println("+--------------------------------+");
        System.out.println("| Player Rank:                   |");
        System.out.println("+--------------------------------+");
        
        for (String token: tokens){
            System.out.println("| "+token);
        }
    }

    private static String removeBarEndString(String str){
        if (str.charAt(str.length() - 1) == '|') {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }


    private void createNewJsonFile(String username, String loginToken) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", username);
        jsonObject.put("gameCode", -1);
        jsonObject.put("loginToken", loginToken);
    
        try {
            File file = new File(this.filePath);
    
            if (!file.exists()) {
                File directory = file.getParentFile();
                if (!directory.exists()) {
                    directory.mkdirs();  // Create the directory and its parents if they don't exist
                }
    
                if (file.createNewFile()) {
                    FileWriter writer = new FileWriter(file);
                    writer.write(jsonObject.toJSONString());
                    writer.flush();
                    writer.close();
                    System.out.println("New JSON file created successfully.");
                } else {
                    System.out.println("Failed to create the JSON file.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // USED FOR THE QUEUE SYSTEM / GAME SYSTEM
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static String readInputForPrompt(MyAtomicBoolean sendAvail) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        StringBuilder input = new StringBuilder();

        while (sendAvail.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (reader.ready()) {
                    int character = reader.read();
                    if (character == '\n') {
                        break;
                    } else {
                        input.append((char) character);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        String userInput = input.toString().trim();
        input.setLength(0); // Clean the StringBuilder
        return userInput;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // USED FOR THE GAME SYSTEM
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private String processRoundStart(String[] tokens) {
        String roundJudge = null;
        if(tokens.length == 2){
            roundJudge = tokens[1];
        }
        return roundJudge;
    }

    private String processSubmissions(String[] tokens){
        String gamePhrase = null;
        if(tokens.length == 2){
            gamePhrase = tokens[1];
        }
        return gamePhrase;
    }

    private List<String> processVotingAndPoints(String[] tokens){
        List<String> submissions = new ArrayList<>();
        if(tokens.length > 1){
            submissions = Arrays.asList(tokens);
            submissions = submissions.subList(1, submissions.size());
        }
        return submissions;
    }

    private Pair<String, String> processRoundEnd(String[] tokens){
        Pair<String, String> submissionReveal = new Pair<>();
        if(tokens.length == 3){
            submissionReveal = new Pair<>(tokens[1], tokens[2]);
        }
        return submissionReveal;
    }

    private String processGameEnd(String[] tokens){
        String winner = null;
        if(tokens.length == 2){
            winner = tokens[1];
        }
        return winner;
    }

}