import java.util.concurrent.CountDownLatch;

/**

 java.lang.Exception: Stack trace
     at java.base/java.lang.Thread.dumpStack(Thread.java:2209)
     at StacktraceDemo$Task.methodB(StacktraceDemo.java:16)
     at StacktraceDemo$Task.methodA(StacktraceDemo.java:12)
     at StacktraceDemo$Task.run(StacktraceDemo.java:8)
     at java.base/java.lang.VirtualThread.run(VirtualThread.java:309)

 */
public class StacktraceDemo {

    private record Task(CountDownLatch latch) implements Runnable {

        public void run() {
            methodA();
        }

        private void methodA() {
            methodB();
        }

        private void methodB() {
            Thread.dumpStack();
            latch.countDown();
        }
    }

    public static void main(String args[]) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread.ofVirtual().start(new Task(latch));
        latch.await();
    }

}