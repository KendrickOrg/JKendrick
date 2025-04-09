package jKendrick.utils;

import java.util.Random;

public class PoissonGenerator {
    private final Random rand;

    public PoissonGenerator() {
        this.rand = new Random();
    }

    public int poissonSample(double lambda) {
        double l = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= rand.nextDouble();
        } while (p > l);
        return k - 1;
    }
}
