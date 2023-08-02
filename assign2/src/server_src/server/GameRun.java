package server;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import myutils.*;

import org.json.simple.*;
import org.json.simple.parser.*;
import org.json.simple.parser.ParseException;


public class GameRun implements Runnable {

    private int gameCode;
    private GameData gameData;
    private GameInfo gameInfo;

    private int pointsToWin;

    //Scheduler
    private final MyScheduler scheduler;

    public GameRun(int gameCode, GameInfo gameInfo) {
        this.gameCode = gameCode;
        this.gameInfo = gameInfo;

        //get gameData for easier access
        Pair<GameData, MyThreadPool> gameLoad = this.gameInfo.getGame(this.gameCode);
        this.gameData = gameLoad.getFirst();

        //points to win
        this.pointsToWin = 10;


        this.scheduler = new MyScheduler();
    }

    public void run() {

        if(this.gameData.getGameState() == GameState.GAME_SETUP){


            MyAtomicBoolean gameVerification = new MyAtomicBoolean(false);

            //if game hasnt started in 5 minutes end game
            scheduler.schedule(() -> {
                //check if game has not started and end game
                try{
                    eraseGame();
                } catch ( IOException | ParseException e){
                    e.printStackTrace();
                }

                gameVerification.set(true);
            }, 5, MyTimeUnit.MINUTES);

            while(!gameVerification.get()){
                if(this.gameData.getGameState() != GameState.GAME_SETUP){
                    gameVerification.set(true);
                    scheduler.cancelTask();
                }
            }
        }

        while(gameData.getGameState() == GameState.GAME_RUNNING ){

            try{
                //String to make commands to send to player
                String commandSend;

                //get the current judge
                int judgeIndex = gameData.getJudge();
                //System.out.println(judgeIndex);
                
                //send judge to players
                String judgeName = gameData.getCurrentJudgeUsername();
                commandSend = "round-start|"+judgeName;
                sendPhraseToPlayers(commandSend);

                // Find a phrase to send and send to all players
                String phraseSend = findGamePhrase(10);
                commandSend = "submission-start|" + phraseSend;
                sendPhraseToPlayers(commandSend); // Signal players to send phrases

                // Receives submission from non judge players
                //This allows for submissions, by changing the perms of the player 
                //Then it counts for 2 minutes and closes the perms
                //if phrase is not found game ends -> no points, ended earlier!
                waitForSubmissions();

                //send submissions to all players
                commandSend = "vote-phase|";
                sendSubmissions(commandSend);

                // Receive vote from judge
                // Allows judge to send the vote
                judgeVoteSubmission(); //signal judge to vote

                //Send phrase and winner to players
                String message = getMessageFromVote();
                commandSend = "round-end|"+message;
                sendPhraseToPlayers(commandSend);

                // Give point to player
                gameData.roundEnd();

                //send new point to players
                sendNewPoints();

                //changeJudge for the next round
                gameData.changeJudge();

                //check if game is over
                commandSend = "game-end|";
                Boolean gameEnded = checkGameEndOrAbandoned(commandSend);
                if(gameEnded){
                    eraseGame();
                }


            } catch (IOException | InterruptedException | ParseException e) {
                //System.out.println("Caught " + e.getClass().getSimpleName());
            }
        }
    }

    private String findGamePhrase(int numPhrases) throws IOException, ParseException {
        int tries = 10;
        for(int i=0; i < tries; i++){
            List<String> randomPhrases = gameInfo.selectRandomPhrases(3);
            for(String phrase: randomPhrases){
                if(!gameData.cardExists(phrase)) {
                    gameData.addCard(phrase);
                    return phrase;
                }
            }
        }
        return "Write the best phrase you can!";
    }

    private void sendPhraseToPlayers(String phrase) throws InterruptedException, IOException, ParseException {
        List<String> usernames = gameData.getActiveUsernames();
        for(String username: usernames){
            Socket socket = gameInfo.getConnectionsData().getOpenSocket(username);
            if(socket != null){
                try{
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                //send Phrase to player
                writer.println(phrase);
                } catch (IOException ex) {
                    throw ex;
                }
            } 
        }
    }
    
    private void sendSubmissions(String commandSend) throws IOException, ParseException {
        List<String> submissions = gameData.getRoundSubmissions();
        List<String> usernames = gameData.getActiveUsernames();
    
        // Send submissions to all players
        String submissionString = String.join("|", submissions);
        for (String username : usernames) {
            Socket socket = gameInfo.getConnectionsData().getOpenSocket(username);
            if(socket != null){
                try {
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(commandSend+submissionString);
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Control send submissions phase
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void changeSubmission(Boolean newPerm) throws IOException, ParseException {
        List<String> usernames = gameData.getUsernames();
        String judgeUsername = gameData.getCurrentJudgeUsername();
        for(String username: usernames){
            if(username.equals(judgeUsername)) continue;
            gameData.changeSendPhrasePerm(username, newPerm);
        }
    }

    private void waitForSubmissions() throws IOException, ParseException{

        //allow submissions
        changeSubmission(true);

        MyAtomicBoolean waitingSubs = new MyAtomicBoolean(true);

        //wait for submissions
        scheduler.schedule(() -> {
            try {
                // disallow submissions
                changeSubmission(false);
            } catch (IOException | ParseException e) {
                // Handle the exception
                e.printStackTrace(); // or any other appropriate handling
            } finally {
                // stop the verification of all submissions are in place
                waitingSubs.set(false);
            }
        }, 1, MyTimeUnit.MINUTES);

        while(waitingSubs.get()){
            if(gameData.verifyIfAllSubmissions()) scheduler.stop();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Control voting phase
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void changeVotes(Boolean newPerm){
        String judgeUsername = gameData.getCurrentJudgeUsername();
        gameData.changeVotePerm(judgeUsername,newPerm);
    }

    private void judgeVoteSubmission(){

        //allow submissions
        changeVotes(true);

        MyAtomicBoolean waitingVote = new MyAtomicBoolean(true);

        //wait for vote
        scheduler.schedule(() -> {
            //disallow vote
            changeVotes(false);
            //stop verification of vote if time is up
            waitingVote.set(false);
        }, 1, MyTimeUnit.MINUTES);

        while(waitingVote.get()){
            if(gameData.getJudgeVote() >= 0) scheduler.stop();
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // check game ended
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private Boolean checkGameEndOrAbandoned(String command) throws IOException, ParseException {
        String winner = gameData.getPlayerPointsWinner(this.pointsToWin);

        if (winner != null){

            //send winner to players
            try{
                sendPhraseToPlayers(command+winner);
            } catch (InterruptedException e){
                System.out.println("[SYSTEM - Error] Failed sending message to players.");
            }

            if(gameData.getGameRank() != -1)
                this.gameInfo.changePlayerRank(winner);
            this.gameData.setGameState(GameState.GAME_ENDED);

            return true;
        } else {
            //end game if no one is active in it
            if(gameData.getActiveUsernames().isEmpty()) return true;
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // end game and erase it
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void eraseGame() throws IOException, ParseException{
        //to make sure players will leave the game
        gameData.setGameState(GameState.GAME_ENDED);

        //System.out.println(gameData.getRemainersUsernames());
        while(!gameData.getRemainersUsernames().isEmpty()){
            try{
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                //continue
            }
        }

        //erase the json
        MyJsonFileDeleter.deleteJsonFile(gameData.getFilePath());

        //erase it from the currentGamesList
        gameInfo.removeGame(this.gameCode);
    }   

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Reveal winner to players
    ///////////////////////////////////////////////////////////////////////////////////////////////
    
    private String getMessageFromVote() {
        String messageString = "";

        String helperString;
        helperString = gameData.getUsernameFromVotePublic();
        if (helperString != null){
            messageString += helperString;
        } else{
            return "output-invalid";
        }    
        helperString = null;
        helperString = gameData.getPhraseFromVotePublic();
        if (helperString != null){
            messageString += "|" + helperString;
        } else{
            return "output-invalid";
        }
        return messageString;
    }

    private void sendNewPoints() throws InterruptedException, IOException, ParseException {
        String playerPoints = gameData.getPlayerPoints();
        sendPhraseToPlayers("points-update|"+playerPoints);
    }

}

    

class PlayerHandler implements Runnable {

    private final String username;
    private final int gameCode;
    private final GameInfo gameInfo;
    private final GameData gameData;
    private final MyThreadPool gameThreadPool;
    private boolean isRunning;

    public PlayerHandler(String username, int gameCode, GameInfo gameInfo) {
        this.username = username;
        this.gameCode = gameCode;
        this.gameInfo = gameInfo;

        Pair<GameData, MyThreadPool> gameLoad = this.gameInfo.getGame(this.gameCode);
        this.gameData = gameLoad.getFirst();
        this.gameThreadPool = gameLoad.getSecond(); 

        this.isRunning = true;
    }

    public void run() {

        try{
            Socket socket = gameInfo.getConnectionsData().getOpenSocket(this.username);

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            while (isRunning) {

                try{

                    String myInput = reader.readLine();

                    // Validate input using regular expression
                    if (!myInput.matches("[a-zA-Z0-9@#$%&|\\-_,.\\s]+")) {
                        writer.println("input-invalid-chars");
                        writer.flush();
                        continue;
                    }

                    String[] tokens = myInput.split("\\|");

                    // Process player response
                    switch (tokens[0]) {
                        case "submission-send":
                            if(tokens.length == 2){
                                if (gameData.getSendPhrasePerm(this.username)){
                                    gameData.addRoundSubmission(this.username, tokens[1]);
                                    gameData.changeSendPhrasePerm(this.username, false);

                                    writer.println("submission-successful");
                                } else{
                                    writer.println("submission-invalid");
                                    writer.flush();
                                }
                            } else{
                                writer.println("input-invalid");
                            }
                            break;
                        case "vote-send":
                            if(tokens.length == 2){
                                if (gameData.getVotePerm(this.username)){
                                    int idVote = -1;
                                    try{
                                        idVote = Integer.parseInt(tokens[1]);
                                    } catch(Exception e){
                                        writer.println("vote-failed");
                                        writer.flush();
                                    }
                                    boolean voted = gameData.judgeVoting(idVote);
                                    if (!voted){
                                        writer.println("vote-failed");
                                        writer.flush();
                                    }
                                    else{
                                        gameData.changeVotePerm(this.username,false);
                                        writer.println("vote-successful");
                                    }
                                }
                                else {
                                    writer.println("vote-invalid");
                                    writer.flush();
                                }
                            } else {
                                writer.println("input-invalid");
                                writer.flush();
                            }
                            break;
                        case "game-leave":
                            Boolean leftGame = gameData.setPlayerPresence(this.username, false);
                            if(leftGame){
                                isRunning = false;
                                SessionHandler sessionHandler = new SessionHandler(socket, this.username, this.gameInfo);
                                gameInfo.getMainThreadPool().execute(sessionHandler);
                                writer.println("game-leave-successful");
                            } else{
                                writer.println("game-leave-failed");
                            }
                            break;
                        case "session-exit-private":
                            writer.println("session-exit-private-successful");
                            break;
                        case "game-leave-private":
                            leftGame = gameData.setPlayerPresence(this.username, false);
                            if(leftGame){
                                isRunning = false;
                                SessionHandler sessionHandler = new SessionHandler(socket, this.username, this.gameInfo);
                                gameInfo.getMainThreadPool().execute(sessionHandler);
                            }
                            break;
                        default:
                            writer.println("input-invalid");
                            writer.flush();
                            break;
                    }

                    // Wait for a short time before sending the next command
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // Ignore interruption
                    }
                } catch (NullPointerException ex) {
                    System.out.println("[CONNECTION] "+this.username+" lost connection.");
                    return;
                }catch (IOException ex) {
                    try{
                    Boolean leftGame = gameData.setPlayerPresence(this.username, false);
                    if(leftGame){
                        isRunning = false;
                        SessionHandler sessionHandler = new SessionHandler(socket, this.username, this.gameInfo);
                        gameInfo.getMainThreadPool().execute(sessionHandler);
                        writer.println("game-leave-alert");
                    } else{
                        writer.println("game-leave-alert-failed");
                    }
                    } catch( ParseException e){
                        //e.printStackTrace();
                    }
                    //ex.printStackTrace();
                } catch (ParseException ex){
                    //ex.printStackTrace();
                }
            }
        } catch (IOException ex) {
            //ex.printStackTrace();
        }
    }

}
