import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 VirtualThread[#21]/runnable@ForkJoinPool-1-worker-1 First lock is acquired
 VirtualThread[#23]/runnable@ForkJoinPool-1-worker-2 First lock is acquired

 "ForkJoinPool-1-worker-1" #22 [29699] daemon prio=5 os_prio=31 cpu=1.84ms elapsed=30.51s tid=0x0000000128010600 nid=29699 waiting on condition  [0x000000016d912000]
 java.lang.Thread.State: WAITING (parking)
     at jdk.internal.misc.Unsafe.park(java.base@21.0.1/Native Method)
     - parking to wait for  <0x000000070fd33cf0> (a java.util.concurrent.ForkJoinPool)
     at java.util.concurrent.locks.LockSupport.park(java.base@21.0.1/LockSupport.java:371)
     at java.util.concurrent.ForkJoinPool.awaitWork(java.base@21.0.1/ForkJoinPool.java:1893)
     at java.util.concurrent.ForkJoinPool.runWorker(java.base@21.0.1/ForkJoinPool.java:1809)
     at java.util.concurrent.ForkJoinWorkerThread.run(java.base@21.0.1/ForkJoinWorkerThread.java:188)

 "ForkJoinPool-1-worker-2" #24 [29443] daemon prio=5 os_prio=31 cpu=0.53ms elapsed=30.49s tid=0x0000000118088400 nid=29443 waiting on condition  [0x000000016db1e000]
 java.lang.Thread.State: WAITING (parking)
     at jdk.internal.misc.Unsafe.park(java.base@21.0.1/Native Method)
     - parking to wait for  <0x000000070fd33cf0> (a java.util.concurrent.ForkJoinPool)
     at java.util.concurrent.locks.LockSupport.park(java.base@21.0.1/LockSupport.java:371)
     at java.util.concurrent.ForkJoinPool.awaitWork(java.base@21.0.1/ForkJoinPool.java:1893)
     at java.util.concurrent.ForkJoinPool.runWorker(java.base@21.0.1/ForkJoinPool.java:1809)
     at java.util.concurrent.ForkJoinWorkerThread.run(java.base@21.0.1/ForkJoinWorkerThread.java:188)


 */
public class DeadlockDemo {

    public static void main(String args[]) throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        CountDownLatch internalLatch = new CountDownLatch(2);

        ReentrantLock lock1 = new ReentrantLock();
        ReentrantLock lock2 = new ReentrantLock();

        Thread.ofVirtual().start(() -> {
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
        });

        Thread.ofVirtual().start(() -> {
            lock2.lock();
            System.out.println(Thread.currentThread() + " First lock is acquired");
            internalLatch.countDown();
            try {
                // wait until another thread acquires the lock
                internalLatch.await();
            } catch (InterruptedException e) {}
            lock1.lock();
            lock1.unlock();
            System.out.println(Thread.currentThread() + " Second lock is acquired");
            lock2.unlock();
            latch.countDown();
        });

        latch.await();
    }

}
