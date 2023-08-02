package server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;


import javax.swing.plaf.metal.MetalBorders.PaletteBorder;

import java.util.ArrayList;
import java.net.Socket;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.json.simple.parser.ParseException;

import myutils.*;

public class GameQueueSystem extends Thread {

    //gameInfo
    GameInfo gameInfo;

    //Scheduler
    private final MySchedulerRepeater CasualSchedulerRepeater;
    private final MySchedulerRepeater RankedschedulerRepeater;

    //MyAtomicBooleans to control access to changing the queue
    MyAtomicBoolean casualQueueBlocked;
    MyAtomicBoolean rankedQueueBlocked;

    public GameQueueSystem(GameInfo gameInfo, MyThreadPool queuesThreadPool) throws IOException {
        this.gameInfo = gameInfo;
        this.CasualSchedulerRepeater = new MySchedulerRepeater();
        this.RankedschedulerRepeater = new MySchedulerRepeater();
        this.casualQueueBlocked = gameInfo.getCasualQueueBlocked();
        this.rankedQueueBlocked = gameInfo.getRankedQueueBlocked();
    }

    public void run() {
        //casual match making
        CasualSchedulerRepeater.scheduleRepeat(() -> {
            System.out.println("[QUEUE - Casual] Games matching.");
            while(casualQueueBlocked.get()) { } //wait for it to be not blocked 
            casualQueueBlocked.set(true);
            casualMatchMaking();
            casualQueueBlocked.set(false);
        }, 20, 20, MyTimeUnit.SECONDS);
        //Ranked match making
        RankedschedulerRepeater.scheduleRepeat(() -> {
            System.out.println("[QUEUE - Ranked] Games matching.");
            while(rankedQueueBlocked.get()) {} //wait for it to be not blocked
            rankedQueueBlocked.set(true);
            rankedMatchMaking();
            rankedQueueBlocked.set(false);
        }, 30, 20, MyTimeUnit.SECONDS);
    }

    //verify casual queue
    public void casualMatchMaking(){
        
        // get players in casual queue
        List<String> playersInQueue = new ArrayList<>(this.gameInfo.getCasualQueue().getList());

        for(String username: playersInQueue){
            try{
                processCasualPlayer(username);
            } catch (IOException | ParseException | NullPointerException e){
                //e.printStackTrace();
            }
        }

    }

    private Boolean processCasualPlayer(String username) throws IOException, ParseException, NullPointerException {

        // Define a socket
        Socket socket = null;

        //verify if player is active
        if(!this.gameInfo.getConnectionsData().isConnected(username)){
            if(this.gameInfo.getConnectionsData().checkTolerance(username, 3 , MyTimeUnit.MINUTES)){
                return false;
            } else{
                this.gameInfo.getCasualQueue().removeEntryByValue(username);
                return false;
            }
        } else{
            socket = this.gameInfo.getConnectionsData().getOpenSocket(username);
        }

        OutputStream output = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);

        HashMap<Integer, Pair<GameData, MyThreadPool>> currentGames = new HashMap<>(this.gameInfo.getAvailGamesCasual());
        
        //verify if there no game to be in the queue for
        if(currentGames.isEmpty()){
            this.gameInfo.getCasualQueue().removeEntryByValue(username);
            writer.print("casual-queue-empty");
            System.out.println("[QUEUE - Casual] "+username+" removed from queue (empty).");

            return false;
        }
        
        // Iterate over the games in a random order
        List<Map.Entry<Integer, Pair<GameData, MyThreadPool>>> entries = new ArrayList<>(currentGames.entrySet());
        Collections.shuffle(entries);
        for (Map.Entry<Integer, Pair<GameData, MyThreadPool>> entry : entries) {
            Boolean playerFoundGame = false;
            GameData gameData = entry.getValue().getFirst();
            if(gameData.getGameRank() == -1){
                if(!gameData.isGameFull()){

                    //verify if player is still in the queue
                    if(this.gameInfo.getCasualQueue().verifyEntry(username) == null){
                        return false;
                    }

                    playerFoundGame = gameData.addPlayer(username);

                    if(playerFoundGame){
                        //get game info
                        Integer gameCode = entry.getKey();
                        MyThreadPool gameThreadPool = entry.getValue().getSecond(); 

                        //throw the player to the playerHandler
                        PlayerHandler playerHandler = new PlayerHandler(username, gameCode, gameInfo);
                        gameThreadPool.execute(playerHandler);

                        gameData.startGameCheck();
                    }
                }
            }
            if(playerFoundGame){
                //remove player for queue
                this.gameInfo.getCasualQueue().removeEntryByValue(username);
                writer.println("game-enter-successful|"+entry.getKey());
                System.out.println("[QUEUE - Casual] "+username+" assigned to game.");
                return true;
            }
        }
        return false;
    }

    //verify ranked queue
    public void rankedMatchMaking(){
        
        // get players in casual queue
        List<Pair<String, Integer>> playersInQueue = new ArrayList<>(this.gameInfo.getRankedQueue().getList());

        for(Pair<String, Integer> playerData: playersInQueue){
            try{
                //System.out.println("ranked user processing: "+playerData.getFirst());
                processRankedPlayer(playerData);
            } catch (IOException | ParseException | NullPointerException e){
                //e.printStackTrace();
            }
        }

    }

    private Boolean processRankedPlayer(Pair<String, Integer> playerData) throws IOException, ParseException, NullPointerException {

        //get plyaer info separately
        String username = playerData.getFirst();
        int descriminator = playerData.getSecond();

        // Define a socket
        Socket socket = null;

        //verify if player is active
        if(!this.gameInfo.getConnectionsData().isConnected(username)){
            if(this.gameInfo.getConnectionsData().checkTolerance(username, 3 , MyTimeUnit.MINUTES)){
                return false;
            } else{
                this.gameInfo.getRankedQueue().removeEntryByValue(playerData);
                return false;
            }
        } else{
            socket = this.gameInfo.getConnectionsData().getOpenSocket(username);
        }

        OutputStream output = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);

        HashMap<Integer, Pair<GameData, MyThreadPool>> currentGames = new HashMap<>(this.gameInfo.getAvailGamesRank());
        
        //verify if there no game to be in the queue for
        if(currentGames.isEmpty()){
            this.gameInfo.getRankedQueue().removeEntryByValue(playerData);
            writer.print("ranked-queue-empty");
            System.out.println("[QUEUE - Ranked] "+username+" removed from queue (empty).");
            return false;
        }
        
        int gameCode = -1;
        // Iterate over the games in a random order
        List<Map.Entry<Integer, Pair<GameData, MyThreadPool>>> entries = new ArrayList<>(currentGames.entrySet());
        Collections.shuffle(entries);
        for (Map.Entry<Integer, Pair<GameData, MyThreadPool>> entry : entries) {
            Boolean playerFoundGame = false;
            GameData gameData = entry.getValue().getFirst();
            int gameRank = gameData.getGameRank();
            int playerRank = gameInfo.getPlayerRank(username);
            //System.out.println("Player rank: "+playerRank+" game rank: "+gameRank+" descriminator: "+descriminator);
            if(gameRank != -1){
                int minAcceptableRank = Math.max(playerRank-descriminator, 0);
                int maxAcceptableRank = playerRank+descriminator;
                if( (gameRank >= minAcceptableRank) && (gameRank <= maxAcceptableRank)){
                    if(!gameData.isGameFull()){

                        //verify if player is still in the queue
                        if(this.gameInfo.getRankedQueue().verifyEntry(username) == null){
                            return false;
                        }

                        playerFoundGame = gameData.addPlayer(username);

                        if(playerFoundGame){
                            //get game info
                            gameCode = entry.getKey();
                            MyThreadPool gameThreadPool = entry.getValue().getSecond(); 

                            //throw the player to the playerHandler
                            PlayerHandler playerHandler = new PlayerHandler(username, gameCode, gameInfo);
                            gameThreadPool.execute(playerHandler);

                            gameData.startGameCheck();
                        }
                    }
                }
            }
            if(playerFoundGame){
                //remove player for queue
                this.gameInfo.getRankedQueue().removeEntryByValue(playerData);
                writer.println("game-enter-successful|"+entry.getKey());
                System.out.println("[QUEUE - Ranked] "+username+" assigned to game.");
                return true;
            }
        }
        Pair<String, Integer> newValue = new Pair<>(username, descriminator+1);
        this.gameInfo.getRankedQueue().updateValue(playerData,newValue);
        return false;
    }

}
