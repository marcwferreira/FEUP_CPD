package myutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyThreadPool {
    private MyBlockingQueue<Runnable> queue;
    private List<WorkerThread> threads;
    private Set<Runnable> executingTasks; // set of currently executing tasks
    private boolean isShutdown = false;

    public MyThreadPool(int threadCount, int queueSize) {
        queue = new MyBlockingQueue<>(queueSize);
        threads = new ArrayList<>();
        executingTasks = new HashSet<>();
        for (int i = 0; i < threadCount; i++) {
            threads.add(new WorkerThread());
        }
        for (WorkerThread thread : threads) {
            thread.start();
        }
    }

    public void execute(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("Executor has been shut down");
        }
        try {
            queue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        isShutdown = true;
        for (WorkerThread thread : threads) {
            thread.interrupt();
        }
        queue.clear(); // Clear the task queue
    }

    public void endOtherTasks(Thread callingThread) {
        // Create a copy of the executingTasks set
        Set<Runnable> tasksCopy = new HashSet<>(executingTasks);
    
        for (Runnable task : tasksCopy) {
            if (task != callingThread) {
                if (task instanceof WorkerThread) {
                    WorkerThread workerThread = (WorkerThread) task;
                    workerThread.interrupt();
                }
                executingTasks.remove(task);
            }
        }
    }
    

    public Set<Runnable> getExecutingTasks() {
        return Collections.unmodifiableSet(executingTasks);
    }

    public int getExecutingTaskCount() {
        synchronized (executingTasks) {
            return executingTasks.size();
        }
    }    

    public int getTaskQueueSize() {
        return queue.size();
    }

    private class WorkerThread extends Thread {
        public void run() {
            while (!isInterrupted()) {
                try {
                    Runnable task = queue.take();
                    executingTasks.add(task); // add task to executing tasks
                    task.run();
                    executingTasks.remove(task); // remove task from executing tasks once completed
                } catch (InterruptedException e) {
                    interrupt();
                }
            }
        }
    }
}
