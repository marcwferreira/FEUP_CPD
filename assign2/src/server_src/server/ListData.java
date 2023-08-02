package server;

import java.util.HashMap;
import java.security.SecureRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;

import java.net.Socket;
import java.util.Iterator;
import java.util.Map;

import myutils.*;

public class ListData<T> {

    private HashMap<T, Socket> connectionsList;
    private HashMap<T, Long> timestampList;
    private HashMap<T, String> loginTokens;
    private ReadWriteLock dataLock = new ReentrantReadWriteLock();
    private Lock readLock = dataLock.readLock();
    private Lock writeLock = dataLock.writeLock();

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public ListData() {
        this.connectionsList = new HashMap<>();
        this.timestampList = new HashMap<>();
        this.loginTokens = new HashMap<>();
    }

    public Socket getConnection(T key) {
        readLock.lock();
        try {
            return this.connectionsList.get(key);
        } finally {
            readLock.unlock();
        }
    }

    public void setConnection(T key, Socket value) {
        writeLock.lock();
        try {
            this.connectionsList.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    public Long getTimetamp(T key) {
        readLock.lock();
        try {
            return this.timestampList.get(key);
        } finally {
            readLock.unlock();
        }
    }

    public void setTimestamp(T key, Long value) {
        writeLock.lock();
        try {
            this.timestampList.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // add and remove connections
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public String addConnection(T key, Socket value) {
        writeLock.lock();
        try {
            connectionsList.put(key, value);
            long currentTimeStamp = System.currentTimeMillis();
            timestampList.put(key, currentTimeStamp);
            String newLoginToken = generateToken(32);
            loginTokens.put(key,newLoginToken);
            return newLoginToken;
        } finally {
            writeLock.unlock();
        }
    }
    
    public void removeConnection(T key) {
        writeLock.lock();
        try {
            connectionsList.remove(key);
        } finally {
            writeLock.unlock();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Operations
    ///////////////////////////////////////////////////////////////////////////////////////////////

    //verify player connections
    //if player is not active remove from list and also update timestamps
    public void verifyConnectionAndUpdateTimestamps() {
        writeLock.lock();
        try {
            Iterator<Map.Entry<T, Socket>> iterator = connectionsList.entrySet().iterator();
            long currentTimeStamp = System.currentTimeMillis();
            while (iterator.hasNext()) {
                Map.Entry<T, Socket> entry = iterator.next();
                Socket socket = entry.getValue();
                if (socket == null || socket.isClosed() || !socket.isConnected()) {
                    iterator.remove();
                } else {
                    timestampList.put(entry.getKey(), currentTimeStamp);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    
    //get player socket and update if necessary
    public Socket getOpenSocket(T key) {
        writeLock.lock();
        try {
            Socket socket = this.connectionsList.get(key);
            if (socket != null && socket.isConnected() && !socket.isClosed()) {
                return socket;
            } else {
                connectionsList.remove(key);
                return null;
            }
        } finally {
            writeLock.unlock();
        }
    }

    //check if tolerance to be in queue
    public boolean checkTolerance(T key, long toleranceTime, MyTimeUnit timeUnit) {
        writeLock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            long defaultTime = currentTime - MyTimeUnit.MINUTES.toMillis(toleranceTime+5);
            long lastUpdateTime = timestampList.getOrDefault(key, defaultTime);
            long elapsedTime = currentTime - lastUpdateTime;

            if (timeUnit.toMillis(toleranceTime) <= elapsedTime) {
                connectionsList.remove(key);
                return false;
            }

            timestampList.put(key, currentTime);
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    //check if player is already connected
    public boolean isConnected(T key) {
        readLock.lock();
        try {
            Socket socket = connectionsList.get(key);
            if(socket != null && socket.isConnected() && !socket.isClosed()){
                return true;
            }
            connectionsList.remove(key);
            return false;
        } finally {
            readLock.unlock();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Tokens
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private static String generateToken(int length) {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder tokenBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(randomIndex);
            tokenBuilder.append(randomChar);
        }
        return tokenBuilder.toString();
    }

    //get token for a player
    public String getToken(T key) {
        readLock.lock();
        try{
            return loginTokens.get(key);
        } finally {
            readLock.unlock();
        }
    }
    
    //verify if token is available
    private boolean checkTolerancePrivate(T key, long toleranceTime, MyTimeUnit timeUnit) {

        long currentTime = System.currentTimeMillis();
        long defaultTime = currentTime - MyTimeUnit.MINUTES.toMillis(toleranceTime+5);
        long lastUpdateTime = timestampList.getOrDefault(key, defaultTime);
        long elapsedTime = currentTime - lastUpdateTime;

        if (timeUnit.toMillis(toleranceTime) <= elapsedTime) {
            return false;
        }

        timestampList.put(key, currentTime);
        return true;
    }

    //check if player can use token to login
    public boolean allowLoginToken(T key, String token){
        readLock.lock();
        try{
            if(loginTokens.get(key).equals(token)){
                if(checkTolerancePrivate(key, 3, MyTimeUnit.MINUTES)){
                    return true;
                }
            }
        }  catch (NullPointerException e){
            return false;
        } finally {
            readLock.unlock();
        }
        return false;
    }


}
