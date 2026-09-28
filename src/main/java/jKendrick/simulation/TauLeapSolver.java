package jKendrick.simulation;

import jKendrick.model.ComposedModel;
import jKendrick.model.GroundModel;

import java.util.List;
import java.util.Random;

/**
 * Tau-leaping with a fixed leap equal to the recording step divided by {@code substeps}: in each
 * leap every transition fires a Poisson number of times with mean propensity * tau. A compartment
 * cannot go negative: firings beyond its cardinality are dropped.
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
        for (int i = 0; i < y.length; i++) y[i] = Math.round(y[i]);
        double[][] states = new double[times.length][];
        states[0] = y.clone();
        double tau = step / substeps;
        for (int i = 1; i < times.length; i++) {
            double t = times[i - 1];
            for (int s = 0; s < substeps; s++) {
                double[] a = propensities(g, transitions, t, y);
                double[] delta = new double[y.length];
                for (int k = 0; k < a.length; k++) {
                    if (a[k] <= 0) continue;
                    ComposedModel.Transition tr = transitions.get(k);
                    int fromIdx = g.model.indexOf(tr.from), toIdx = g.model.indexOf(tr.to);
                    int n = poisson(a[k] * tau, random);
                    delta[fromIdx] -= n;
                    delta[toIdx] += n;
                }
                for (int j = 0; j < y.length; j++) y[j] = Math.max(0, y[j] + delta[j]);
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
