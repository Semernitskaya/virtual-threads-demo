import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.System.out;


/**
 * Minimal duration: 2175
 * Maximal duration: 3080
 * Average duration: 2936.26
 *
 *
 * Minimal duration: 337
 * Maximal duration: 2634
 * Average duration: 1525.2
 *
 */
public class CpuDemo {

    private record Result(Duration duration, BigInteger count) {}

    public static void main(String args[]) throws Exception {

        var executor = Executors.newThreadPerTaskExecutor(Thread.ofPlatform().factory());

        //var executor = Executors.newVirtualThreadPerTaskExecutor();

        List<Future<Result>> futures = new ArrayList<>();
        var start = Instant.now();
        for (int counter = 0; counter < 100; ++counter) {
            futures.add(executor.submit(() -> method(start)));
        }
        var durations = futures.stream().map(f -> {
            try {
                return f.get().duration.toMillis();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        out.println("Minimal duration: " + Collections.min(durations));
        out.println("Maximal duration: " + Collections.max(durations));
        out.println("Average duration: " + durations.stream()
                .mapToLong(Long::valueOf)
                .average().getAsDouble());
    }

    private static Result method(Instant start) {
        var count = BigInteger.ZERO;

        for (int i = 0; i < 10_000_000; i++) {
             count = count.add(BigInteger.valueOf(1L));
        }

        return new Result(Duration.between(start, Instant.now()), count);

    }

}