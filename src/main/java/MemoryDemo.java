import java.util.concurrent.CountDownLatch;

/**
 -XX:NativeMemoryTracking=summary


 Thread (reserved=57764KB, committed=57764KB)
     (thread #28)
     (stack: reserved=57680KB, committed=57680KB)
     (malloc=52KB #173)
     (arena=32KB #55)


 */
public class MemoryDemo {

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
            Thread.ofVirtual().start(new Task(latch));
        }
        latch.await();
    }

}