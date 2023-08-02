package myutils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyScheduler {
    private Timer timer;
    private Lock lock;
    private boolean cancelTask;
    private Runnable task;

    public MyScheduler() {
        timer = new Timer();
        lock = new ReentrantLock();
    }

    public void schedule(Runnable task, long delay, MyTimeUnit timeUnit) {
        lock.lock();
        try {
            if (cancelTask) {
                throw new IllegalStateException("Scheduler has been canceled. Create a new instance to schedule tasks.");
            }
            this.task = task;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!cancelTask) {
                        MyScheduler.this.task.run();
                    }
                }
            }, timeUnit.toMillis(delay));
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        lock.lock();
        try {
            if (!cancelTask && task != null) {
                task.run();
            }
            resetState();
        } finally {
            lock.unlock();
        }
    }

    public void cancelTask() {
        lock.lock();
        try {
            cancelTask = true;
            resetState();
        } finally {
            lock.unlock();
        }
    }

    private void resetState() {
        timer.cancel();
        timer = new Timer();
        cancelTask = false;
        task = null;
    }
}
