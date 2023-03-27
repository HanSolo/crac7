package eu.hansolo.crac7;

import java.util.concurrent.RecursiveTask;


public class CheckPrimeTask extends RecursiveTask<Boolean> {
    private final int number;


    public CheckPrimeTask(final int number) {
        this.number = number;
    }


    @Override protected Boolean compute() {
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
