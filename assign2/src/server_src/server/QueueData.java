package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;

import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import myutils.*;

public class QueueData<T> {

    private List<T> queueList;
    private ReadWriteLock dataLock = new ReentrantReadWriteLock();
    private Lock readLock = dataLock.readLock();
    private Lock writeLock = dataLock.writeLock();

    public QueueData() {
        this.queueList = new ArrayList<>();
    }

    //add player to queue data
    public void addEntry(T data){
        writeLock.lock();
        try{
            if(!queueList.contains(data))
                this.queueList.add(data);
        } finally {
            writeLock.unlock();
        }
    }

    //verify if player is in queue
    public <K> T verifyEntry(K keyVerify) {
        readLock.lock();
        try {
            if (!queueList.isEmpty()) {
                if (queueList.get(0).getClass() == keyVerify.getClass()) {
                    if (queueList.contains(keyVerify)) {
                        return (T) keyVerify;
                    }
                } else if (queueList.get(0) instanceof Pair<?, ?>) {
                    Pair<?, ?> firstElement = (Pair<?, ?>) queueList.get(0);
                    if (firstElement.getFirst().getClass() == keyVerify.getClass()) {
                        for (Object item : queueList) {
                            Pair<?,?> pair = (Pair<?,?>) item;
                            if (pair.getFirst().equals(keyVerify)) {
                                return (T) pair;
                            }
                        }
                    }
                }
            }
        } finally {
            readLock.unlock();
        }
        return null;
    }

    //Process queue
    public List<T> getList(){
        readLock.lock();
        try{
            return queueList;
        } finally {
            readLock.unlock();
        }
    }

    // Remove player from the queue
    public boolean removeEntryByValue(T value) {
        writeLock.lock();
        try {
            return queueList.remove(value);
        } finally {
            writeLock.unlock();
        }
    }

    // Remove player by username
    //only works if T is a type of pair
    public <K> boolean removeEntryByKey(K keyRemove) {
        writeLock.lock();
        try {
            if (!queueList.isEmpty()) {
                if (queueList.get(0) instanceof Pair<?, ?>) {
                    Pair<?, ?> firstElement = (Pair<?, ?>) queueList.get(0);
                    if (firstElement.getFirst().getClass().equals(keyRemove.getClass())) {
                        for (Object item : queueList) {
                            Pair<?, ?> pair = (Pair<?, ?>) item;
                            if (pair.getFirst().equals(keyRemove)) {
                                System.out.println(pair);
                                queueList.remove(item);
                                return true;
                            }
                        }
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
        return false;
    }

    // Update a value in the queue
    public void updateValue(T valueToUpdate, T newValue) {
        writeLock.lock();
        try {
            int index = queueList.indexOf(valueToUpdate);
            if (index != -1) {
                queueList.set(index, newValue);
            }
        } finally {
            writeLock.unlock();
        }
    }

}
