package myutils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.*;

public class MySchedulerRepeater {
    private Timer timer = new Timer();
    private Lock lock = new ReentrantLock();

    public void scheduleRepeat(Runnable task, long delay, long period, MyTimeUnit timeUnit) {
        lock.lock();
        try {
            timer.schedule(new RepeatingTimerTask(task), timeUnit.toMillis(delay), timeUnit.toMillis(period));
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        lock.lock();
        try {
            timer.cancel();
        } finally {
            lock.unlock();
        }
    }

    private class RepeatingTimerTask extends TimerTask {
        private final Runnable task;

        public RepeatingTimerTask(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            task.run();
        }
    }
}
