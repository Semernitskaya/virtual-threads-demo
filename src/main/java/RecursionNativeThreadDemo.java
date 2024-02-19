import java.util.concurrent.CountDownLatch;

public class RecursionNativeThreadDemo {

    private record Task() implements Runnable {

        public void run() {
            method(1);
        }

        private static void method(int i) {
          //  out.println(i);
            method(++i);
        }

    }

    public static void main(String args[]) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        for (int counter = 0; counter < 1_000; ++counter) {
            Thread.ofPlatform().start(new Task());
        }
        latch.await();
    }

}