package eu.hansolo.crac7;

import jdk.crac.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;


public class Main implements Resource {
    private static final int                           N                = 500_000; // Max number to evaluate
    private static final Random                        RND              = new Random();
    private static final long                          RUNTIME_IN_NS    = 2_000_000_000;
    private static final int                           RANGE            = 25_000;
    private static final long                          SECOND_IN_NS     = 1_000_000_000;
    private static final List<String>                  RESULTS_SINGLE   = new ArrayList<>();
    private static final ConcurrentSkipListSet<String> RESULTS_PARALLEL = new ConcurrentSkipListSet<>();
    private static final Integer                       NO_OF_PROCESSORS = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    private static final Integer                       NO_OF_THREADS    = Runtime.getRuntime().availableProcessors();
    private static final ForkJoinPool                  FORK_JOIN_POOL   = new ForkJoinPool(NO_OF_THREADS);
    private        final List<Integer>                 randomNumberPool = new ArrayList<>();
    private        final ExecutorService               executorService  = Executors.newSingleThreadExecutor();
    private        final Callable<Integer>             task;
    private static       long                          startTime;


    public Main() {
        // Define task
        task = () -> {
            while(System.nanoTime() - startTime < RUNTIME_IN_NS) {
                final int     number  = randomNumberPool.get(ThreadLocalRandom.current().nextInt(randomNumberPool.size() - 1));
                final boolean isPrime = isPrimeLoop(number);
                //final boolean isPrime = isPrimeStream(number);
                RESULTS_SINGLE.add(number + " -> " + isPrime);
            }
            return RESULTS_SINGLE.size();
        };

        Core.getGlobalContext().register(Main.this);

        init();

        start();

        startTime = System.nanoTime();

        startParallel();

        System.out.println("Total number of loaded classes -> " + ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
        System.out.println("Total time of compilation -> " + ManagementFactory.getCompilationMXBean().getTotalCompilationTime() + "ms");
    }


    @Override public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {}

    @Override public void afterRestore(Context<? extends Resource> context) throws Exception {
         startTime = System.nanoTime();

         start();

         startTime = System.nanoTime();

         startParallel();

         System.out.println("Total number of loaded classes -> " + ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
         System.out.println("Total time of compilation -> " + ManagementFactory.getCompilationMXBean().getTotalCompilationTime() + "ms");
    }


    private void init() {
        randomNumberPool.clear();
        randomNumberPool.addAll(createRandomNumberPool());
    }

    private void start() {
        try {
            final long numberOfTransactions = this.executorService.submit(task).get();
            System.out.println("Number of transcations in " + (RUNTIME_IN_NS / SECOND_IN_NS) + "s -> " + numberOfTransactions);

            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.SECONDS);
            if (!executorService.isShutdown()) { executorService.shutdownNow(); }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Interrupted");
        }
    }

    private void startParallel() {
        FORK_JOIN_POOL.invoke(new EvaluatePrime(0, N));
        System.out.println("Checking " + RESULTS_PARALLEL.size() + " numbers for prime in parallel (" + NO_OF_THREADS +  " threads) took -> " + ((System.nanoTime() - startTime) / SECOND_IN_NS) + "s");
    }

    private boolean isPrimeLoop(final long number) {
        if (number < 1) { return false; }
        boolean isPrime = true;
        for (long n = number ; n > 0 ; n--) {
            if (n == number || n == 1 || number % n != 0) { continue; }
            isPrime = false;
            break;
        }
        return isPrime;
    }

    private boolean isPrimeStream(final long number) {
        if (number < 1) { return false; }
        return LongStream.iterate(number, n -> n > 0, n -> n - 1).noneMatch(n -> n != number && n != 1 && number % n == 0);
    }

    private List<Integer> createRandomNumberPool() {
        final List<Integer> randomNumberPool = new ArrayList<>(1_000_000);
        for (int i = 0 ; i < 1_000_000 ; i++) {
            final int number = RND.nextInt(RANGE);
            randomNumberPool.add(number);
        }
        return randomNumberPool;
    }

    public static void main(String[] args) {
        startTime = System.nanoTime();
        final long currentTime = System.currentTimeMillis();
        final long vmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        System.out.println("JVM startup time -> " + (currentTime - vmStartTime) + "ms");
        new Main();
    }

    static class EvaluatePrime extends RecursiveAction {
        private int from;
        private int to;


        public EvaluatePrime(final int from, final int to) {
            this.from = from;
            this.to   = to;
        }


        @Override public void compute() {
            if ( (to - from) <= N / (NO_OF_THREADS) ) {
                for (int i = from; i <= to; i++) {
                    RESULTS_PARALLEL.add(i + " -> " + evaluatePrime(i));
                }
            } else {
                int mid = (from + to) / 2;
                invokeAll(new EvaluatePrime(from, mid), new EvaluatePrime(mid + 1, to));
            }
        }

        private static boolean evaluatePrime(final int number) {
            if (number < 1) { return false; }
            Boolean isPrime = Boolean.TRUE;
            for (long n = number ; n > 0 ; n--) {
                if (n == number || n == 1 || number % n != 0) { continue; }
                isPrime = Boolean.FALSE;
                break;
            }
            return isPrime;
        }
    }
}