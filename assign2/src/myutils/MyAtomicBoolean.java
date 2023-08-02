package myutils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyAtomicBoolean {
    private Lock lock = new ReentrantLock();
    private boolean value;

    public MyAtomicBoolean(boolean initialValue) {
        this.value = initialValue;
    }

    public boolean get() {
        lock.lock();
        try {
            return value;
        } finally {
            lock.unlock();
        }
    }

    public void set(boolean newValue) {
        lock.lock();
        try {
            value = newValue;
        } finally {
            lock.unlock();
        }
    }
}
