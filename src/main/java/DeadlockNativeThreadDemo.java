import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 Thread[#21,Thread-0,5,main] First lock is acquired
 Thread[#22,Thread-1,5,main] First lock is acquired

 kill -3 PID

 Found one Java-level deadlock:
 =============================
 "Thread-0":
 waiting for ownable synchronizer 0x000000070fd1d908, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
 which is held by "Thread-1"

 "Thread-1":
 waiting for ownable synchronizer 0x000000070fd1d8d8, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
 which is held by "Thread-0"

 Found 1 deadlock.
 */
public class DeadlockNativeThreadDemo {

    public static void main(String args[]) throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        CountDownLatch internalLatch = new CountDownLatch(2);

        ReentrantLock lock1 = new ReentrantLock();
        ReentrantLock lock2 = new ReentrantLock();

        new Thread(() -> {
            lock1.lock();
            System.out.println(Thread.currentThread() + " First lock is acquired");
            internalLatch.countDown();
            try {
                // wait until another thread acquires the lock
                internalLatch.await();
            } catch (InterruptedException e) {}
            lock2.lock();
            System.out.println(Thread.currentThread() + " Second lock is acquired");
            lock2.unlock();
            lock1.unlock();
            latch.countDown();
        }).start();

        new Thread(() -> {
            lock2.lock();
            System.out.println(Thread.currentThread() + " First lock is acquired");
            internalLatch.countDown();
            try {
                // wait until another thread acquires the lock
                internalLatch.await();
            } catch (InterruptedException e) {}
            lock1.lock();
            System.out.println(Thread.currentThread() + " Second lock is acquired");
            lock1.unlock();
            lock2.unlock();
            latch.countDown();
        }).start();

        latch.await();
    }

}
