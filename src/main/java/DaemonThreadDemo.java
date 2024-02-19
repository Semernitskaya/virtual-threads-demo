public class DaemonThreadDemo {

    private record Task() implements Runnable {

        public void run() {
           while (true) {
               System.out.println("Hello!");
           }
        }

    }

    public static void main(String args[]) throws Exception {
        Thread.ofVirtual().start(new Task());
    }

}