import java.util.concurrent.CountDownLatch;

/**

 "Thread-0" prio=0 tid=0x0 nid=0x0 waiting on condition
     java.lang.Thread.State: TIMED_WAITING
     at java.base@21.0.1/java.lang.Thread.sleep0(Native Method)
     at java.base@21.0.1/java.lang.Thread.sleep(Thread.java:509)
     at app//StacktraceWaitingNativeThreadDemo$Task.methodB(StacktraceWaitingNativeThreadDemo.java:30)
     at app//StacktraceWaitingNativeThreadDemo$Task.methodA(StacktraceWaitingNativeThreadDemo.java:25)
     at app//StacktraceWaitingNativeThreadDemo$Task.run(StacktraceWaitingNativeThreadDemo.java:21)
     at java.base@21.0.1/java.lang.Thread.runWith(Thread.java:1596)
     at java.base@21.0.1/java.lang.Thread.run(Thread.java:1583)


 */
public class StacktraceWaitingNativeThreadDemo {

    private record Task(CountDownLatch latch) implements Runnable {

        public void run() {
            methodA();
        }

        private void methodA() {
            methodB();
        }

        private void methodB() {
            try {
                Thread.sleep(30_000);
            } catch (InterruptedException e) {}
            latch.countDown();
        }
    }

    public static void main(String args[]) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread.ofPlatform().start(new Task(latch));
        latch.await();
    }

}