package myutils;

public class MyCountDownLatch {
    private int count;

    public MyCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be not negative");
        }
        this.count = count;
    }

    public synchronized void countDown() {
        if (count > 0) {
            count--;
            if (count == 0) {
                notifyAll(); // Notify all waiting threads when count reaches zero
            }
        }
    }

    public synchronized void await() throws InterruptedException {
        while (count > 0) {
            wait(); // Wait until count reaches zero
        }
    }
}
