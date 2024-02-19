import java.util.concurrent.CountDownLatch;

/**
 **/
public class DeadlockSyncDemo {

    public static void main(String args[]) throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        CountDownLatch internalLatch = new CountDownLatch(2);

        Object lock1 = new Object();
        Object lock2 = new Object();

        new Thread(() -> {
            synchronized (lock1) {
                System.out.println("First lock is acquired");
                internalLatch.countDown();
                try {
                    // wait until another thread acquires the lock
                    internalLatch.await();
                } catch (InterruptedException e) {
                }
                synchronized (lock2) {
                    System.out.println("Second lock is acquired");
                    latch.countDown();
                }
            }
        }).start();

        new Thread(() -> {
            synchronized (lock2) {
                System.out.println("First lock is acquired");
                internalLatch.countDown();
                try {
                    // wait until another thread acquires the lock
                    internalLatch.await();
                } catch (InterruptedException e) {
                }
                synchronized (lock1) {
                    System.out.println("Second lock is acquired");
                    latch.countDown();
                }
            }
        }).start();

        latch.await();
    }

}
