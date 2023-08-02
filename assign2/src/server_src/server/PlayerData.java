package server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.print.PrintException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.json.simple.*;
import org.json.simple.parser.*;

import myutils.*;

public class PlayerData {

    private String username;
    private int playerRank;
    private String filePath;

    private ArrayList<Integer> dataList = new ArrayList<Integer>();
    private ReadWriteLock dataLock = new ReentrantReadWriteLock();
    private Lock readLock = dataLock.readLock();
    private Lock writeLock = dataLock.writeLock();

    public PlayerData(String username, int playerRank, String filePath){
        this.username = username;
        this.playerRank = playerRank;
        this.filePath = filePath;
    }

    public PlayerData(String username, String password, String salt, String filePath) throws IOException, ParseException{
        this.username = username;
        this.playerRank = 0;
        this.filePath = filePath+"/"+username+".json";
        this.writeNewAccount(this.username, password, salt);
    }

    //getters
    public String getUsername() {
        return this.username;
    }
    
    public int getPlayerRank() {
        readLock.lock();
        try{
            return this.playerRank;
        } finally {
            readLock.unlock();
        }
    }

    //get account info
    public Pair<String, String> getAccountLoginInfo() throws IOException, ParseException {
        readLock.lock();
        Pair<String, String> accountInfo = null;
        try{
            JSONParser parser = new JSONParser();
        
            File accountFile = new File(this.filePath);
            if (!accountFile.exists()) {
                return accountInfo;
            }
            
            try (FileReader reader = new FileReader(accountFile)) {
                Object obj = parser.parse(reader);
                JSONObject jsonObject = (JSONObject) obj;

                String password = (String) jsonObject.get("password");
                String salt = (String) jsonObject.get("salt");
                accountInfo = new Pair<>(password, salt);
            } catch (IOException ex) {
                throw ex;
            }

        } finally {
            readLock.unlock();
        }
        return accountInfo;
    }

    //write json
    private void writeNewAccount(String username, String password, String salt) throws IOException, ParseException {
        writeLock.lock();
        try{
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("password", password);
            json.put("salt", salt);
            json.put("rank", 0);
        
            try (FileWriter writer = new FileWriter(this.filePath)) {
                writer.write(json.toJSONString());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } finally {
            writeLock.unlock();
        }
    }

    //change rank
    public void changeRank() throws IOException, ParseException {
        writeLock.lock();
        try {
            JSONParser parser = new JSONParser();
    
            File accountFile = new File(this.filePath);
            if (!accountFile.exists()) {
                return;
            }
    
            try (FileReader reader = new FileReader(accountFile)) {
                Object obj = parser.parse(reader);
                JSONObject jsonObject = (JSONObject) obj;
    
                int currentRank = (int) (long) jsonObject.get("rank");
                int newRank = currentRank + 1;
                jsonObject.put("rank", newRank);
    
                try (FileWriter writer = new FileWriter(this.filePath)) {
                    writer.write(jsonObject.toJSONString());
                } catch (IOException ex) {
                    throw ex;
                }
    
                //change rank of player in the variable object
                this.playerRank = newRank;
    
            } catch (IOException ex) {
                throw ex;
            }
        } finally {
            writeLock.unlock();
        }
    }

    //change password
    public void changePassword(String newPassword, String newSalt) throws IOException, ParseException {
        writeLock.lock();
        try {
            JSONParser parser = new JSONParser();
            File accountFile = new File(this.filePath);
            if (!accountFile.exists()) {
                throw new IOException("Account file does not exist.");
            }
            try (FileReader reader = new FileReader(accountFile)) {
                Object obj = parser.parse(reader);
                JSONObject jsonObject = (JSONObject) obj;
                jsonObject.put("password", newPassword);
                jsonObject.put("salt", newSalt);
                try (FileWriter writer = new FileWriter(accountFile)) {
                    writer.write(jsonObject.toJSONString());
                }
            } catch (IOException | ParseException ex) {
                throw ex;
            }
        } finally {
            writeLock.unlock();
        }
    }
}
