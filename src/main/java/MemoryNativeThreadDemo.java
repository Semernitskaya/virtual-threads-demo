import java.util.concurrent.CountDownLatch;

/**
Thread (reserved=2102390KB, committed=2102390KB)
    (thread #1019)
    (stack: reserved=2099140KB, committed=2099140KB)
    (malloc=2057KB #6181)
    (arena=1193KB #2037)

 */
public class MemoryNativeThreadDemo {

    private record Task(CountDownLatch latch) implements Runnable {

        public void run() {
            try {
                Thread.sleep(60 * 1_000);
                latch.countDown();
            } catch (Exception e) {}
        }

    }

    public static void main(String args[]) throws Exception {
        CountDownLatch latch = new CountDownLatch(1_000_000);
        for (int counter = 0; counter < 1_000_000; ++counter) {
            Thread.ofPlatform().start(new Task(latch));
        }
        latch.await();
    }

}