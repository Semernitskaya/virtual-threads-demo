import java.util.concurrent.CountDownLatch;

/**

 "ForkJoinPool-1-worker-1" prio=0 tid=0x0 nid=0x0 waiting on condition
    java.lang.Thread.State: TIMED_WAITING
 on java.util.concurrent.ForkJoinPool@29cf7b1b
     at java.base@21.0.1/jdk.internal.misc.Unsafe.park(Native Method)
     at java.base@21.0.1/java.util.concurrent.locks.LockSupport.parkUntil(LockSupport.java:449)
     at java.base@21.0.1/java.util.concurrent.ForkJoinPool.awaitWork(ForkJoinPool.java:1891)
     at java.base@21.0.1/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1809)
     at java.base@21.0.1/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188)

 {
   "tid": "21",
   "name": "",
   "stack": [
      "java.base\/java.lang.VirtualThread.parkNanos(VirtualThread.java:621)",
      "java.base\/java.lang.VirtualThread.sleepNanos(VirtualThread.java:793)",
      "java.base\/java.lang.Thread.sleep(Thread.java:507)",
      "StacktraceWaitingDemo$Task.methodB(StacktraceWaitingDemo.java:30)",
      "StacktraceWaitingDemo$Task.methodA(StacktraceWaitingDemo.java:25)",
      "StacktraceWaitingDemo$Task.run(StacktraceWaitingDemo.java:21)",
      "java.base\/java.lang.VirtualThread.run(VirtualThread.java:309)"
   ]
 }

 */
public class StacktraceWaitingDemo {

    private record Task(CountDownLatch latch) implements Runnable {

        public void run() {
            methodA();
        }

        private void methodA() {
            methodB();
        }

        private void methodB() {
            try {
                Thread.sleep(300_000);
            } catch (InterruptedException e) {}
            latch.countDown();
        }
    }

    public static void main(String args[]) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread.ofVirtual().start(new Task(latch));
        latch.await();
    }

}