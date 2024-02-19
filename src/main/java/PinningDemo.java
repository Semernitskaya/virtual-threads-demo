import java.util.concurrent.CountDownLatch;

/**
 Thread[#22,ForkJoinPool-1-worker-1,5,CarrierThreads]
    PinningDemo$Task.syncMethod(PinningDemo.java:14) <== monitors:1
 Thread[#93,ForkJoinPool-1-worker-5,5,CarrierThreads]
     PinningDemo$Task.syncMethod(PinningDemo.java:13) <== monitors:1

 Thread[#91,ForkJoinPool-1-worker-5,5,CarrierThreads]
     java.base/java.lang.VirtualThread$VThreadContinuation.onPinned(VirtualThread.java:183)
     java.base/jdk.internal.vm.Continuation.onPinned0(Continuation.java:393)
     java.base/java.lang.VirtualThread.parkNanos(VirtualThread.java:621)
     java.base/java.lang.VirtualThread.sleepNanos(VirtualThread.java:793)
     java.base/java.lang.Thread.sleep(Thread.java:507)
     PinningDemo$Task.syncMethod(PinningDemo.java:22) <== monitors:1
     PinningDemo$Task.run(PinningDemo.java:16)
     java.base/java.lang.VirtualThread.run(VirtualThread.java:309)
 Thread[#90,ForkJoinPool-1-worker-4,5,CarrierThreads]
     java.base/java.lang.VirtualThread$VThreadContinuation.onPinned(VirtualThread.java:183)
     java.base/jdk.internal.vm.Continuation.onPinned0(Continuation.java:393)
     java.base/java.lang.VirtualThread.park(VirtualThread.java:582)
     java.base/java.lang.System$2.parkVirtualThread(System.java:2639)
     java.base/jdk.internal.misc.VirtualThreads.park(VirtualThreads.java:54)
     java.base/java.util.concurrent.locks.LockSupport.park(LockSupport.java:219)
     java.base/java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:754)
     java.base/java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:990)
     java.base/java.util.concurrent.locks.ReentrantLock$Sync.lock(ReentrantLock.java:153)
     java.base/java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:322)
     java.base/jdk.internal.misc.InternalLock.lock(InternalLock.java:74)
     java.base/java.io.PrintStream.writeln(PrintStream.java:824)
     java.base/java.io.PrintStream.println(PrintStream.java:1078)
     PinningDemo$Task.syncMethod(PinningDemo.java:21) <== monitors:1
     PinningDemo$Task.run(PinningDemo.java:16)
     java.base/java.lang.VirtualThread.run(VirtualThread.java:309)
 */
public class PinningDemo {

    private record Task(int counter, CountDownLatch latch) implements Runnable {

        public void run() {
            syncMethod();
        }

        synchronized private void syncMethod() {
            try {
                System.out.println(counter);
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            latch.countDown();
        }

    }

    public static void main(String args[]) throws Exception {
        CountDownLatch latch = new CountDownLatch(1_000);
        for (int counter = 0; counter < 1_000; ++counter) {
            Thread.ofVirtual().start(new Task(counter, latch));
        }
        latch.await();
    }

}