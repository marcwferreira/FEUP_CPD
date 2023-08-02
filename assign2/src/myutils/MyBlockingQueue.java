package myutils;

import java.util.LinkedList;

public class MyBlockingQueue<T> {
    private LinkedList<T> queue = new LinkedList<>();
    private int capacity;

    public MyBlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();
        }
        queue.addLast(item);
        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        T item = queue.removeFirst();
        notifyAll();
        return item;
    }
    
    public synchronized void clear() {
        queue.clear();
    }
    
    public synchronized int size() {
        return queue.size();
    }
}
