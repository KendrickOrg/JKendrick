package jKendrick.simulation;

import jKendrick.model.ComposedModel;
import jKendrick.model.GroundModel;

import java.util.List;
import java.util.Random;

/**
 * Tau-leaping with a fixed leap equal to the recording step divided by {@code substeps}: in each
 * leap every transition fires a Poisson number of times with mean propensity * tau, the
 * propensities being those at the start of the leap (a rate that reads the time is thus held
 * constant within a leap, an error of the order of the leap's own). A compartment cannot go
 * negative: the transitions are taken in order and the firings of each beyond what its source
 * compartment still holds are dropped, so the population is conserved.
 */
public final class TauLeapSolver extends StochasticSolver {
    private final int substeps;

    public TauLeapSolver(int runs, long seed) {
        this(runs, seed, 1);
    }

    public TauLeapSolver(int runs, long seed, int substeps) {
        super(runs, seed);
        if (substeps < 1) throw new IllegalArgumentException("substeps must be >= 1");
        this.substeps = substeps;
    }

    @Override
    protected Trajectory run(GroundModel g, double from, double to, double step, Random random) {
        double[] times = Solver.grid(from, to, step);
        List<ComposedModel.Transition> transitions = g.transitions();
        double[] y = g.initialState();
        double[][] states = new double[times.length][];
        states[0] = y.clone();
        double tau = step / substeps;
        double[] a = new double[transitions.size()];
        double[] left = new double[y.length]; // what each compartment still holds for the leap's departures
        double[] delta = new double[y.length];
        for (int i = 1; i < times.length; i++) {
            double t = times[i - 1];
            for (int s = 0; s < substeps; s++) {
                propensities(g, t, y, a);
                System.arraycopy(y, 0, left, 0, y.length);
                java.util.Arrays.fill(delta, 0);
                for (int k = 0; k < a.length; k++) {
                    if (a[k] <= 0) continue;
                    int firings = poisson(a[k] * tau, random);
                    int src = g.source(k);
                    if (src >= 0) {
                        firings = (int) Math.min(firings, left[src]);
                        left[src] -= firings;
                    }
                    fire(g, k, delta, firings);
                }
                for (int j = 0; j < y.length; j++) y[j] += delta[j];
                t += tau;
            }
            states[i] = y.clone();
        }
        return new Trajectory(g, times, states);
    }

    /** Poisson sample: Knuth's method for small means, normal approximation beyond. */
    static int poisson(double mean, Random random) {
        if (mean <= 0) return 0;
        if (mean > 500) return (int) Math.max(0, Math.round(mean + Math.sqrt(mean) * random.nextGaussian()));
        double l = Math.exp(-mean), p = 1;
        int k = 0;
        do {
            k++;
            p *= random.nextDouble();
        } while (p > l);
        return k - 1;
    }
}
