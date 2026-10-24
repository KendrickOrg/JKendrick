package jKendrick.simulation;

import jKendrick.model.GroundModel;

import java.util.Random;

/**
 * Anderson's modified next reaction method on the composed CTMC, for models in which some rates read
 * the time, which Gillespie's direct method refuses. The law it simulates is exact up to the error of the
 * quadrature that integrates those rates ({@link Quadrature}).
 *
 * Every transition k is driven by its own unit-rate Poisson process: P_k is the internal time of
 * its next firing and T_k the internal time reached so far, the integral of its propensity a_k
 * over the elapsed real time. The transition fires at the real time t + dt_k at which the integral
 * of a_k over [t, t + dt_k] equals P_k - T_k; the next event is the transition with the smallest
 * firing time. When a propensity does not read the time it is constant between two changes of the
 * state and dt_k = (P_k - T_k) / a_k; when it does, the integral is computed by adaptive Simpson's rule
 * ({@link Quadrature}) on starting panels of width min(step / panels, quadratureStep), so that the
 * resolution does not depend on the recording step alone. The firing time of a
 * transition is recomputed only when the event changes a compartment its propensity reads, or
 * fires it; its internal time is brought up to date at that moment, with the propensity the
 * transition had meanwhile.
 *
 * A transition whose integrated propensity stays below P_k - T_k up to the end of the simulation
 * does not fire before it; when no transition fires before the end, the run is over.
 */
public final class NextReactionSolver extends StochasticSolver {
    /** Default of {@link #quadratureStep}: one eighth of a time unit. */
    public static final double QUADRATURE_STEP = 0.125;
    /** Simpson panels per recording step for the propensities that read the time. */
    private final int panels;
    /** Largest width of a starting panel, whatever the recording step. */
    private final double quadratureStep;

    public NextReactionSolver(int runs, long seed) {
        this(runs, seed, 8, QUADRATURE_STEP);
    }

    public NextReactionSolver(int runs, long seed, int panels) {
        this(runs, seed, panels, QUADRATURE_STEP);
    }

    public NextReactionSolver(int runs, long seed, int panels, double quadratureStep) {
        super(runs, seed);
        if (panels < 1) throw new IllegalArgumentException("panels must be >= 1");
        if (!(quadratureStep > 0)) throw new IllegalArgumentException("quadratureStep must be > 0");
        this.panels = panels;
        this.quadratureStep = quadratureStep;
    }

    @Override
    protected Trajectory run(GroundModel g, double from, double to, double step, Random random) {
        double[] times = Solver.grid(from, to, step);
        double[] y = g.initialState();
        double[][] states = new double[times.length][];
        states[0] = y.clone();
        int m = g.transitions().size();
        double h = Math.min(step / panels, quadratureStep);
        boolean[] timed = new boolean[m];
        for (int i : g.timeDependent()) timed[i] = true;
        double[] P = new double[m], T = new double[m], firing = new double[m], since = new double[m];
        for (int k = 0; k < m; k++) {
            P[k] = exponential(random);
            since[k] = from;
            firing[k] = from + timeToFire(g, k, timed[k], from, y, P[k], to, h);
        }
        boolean[] marked = new boolean[m];
        int[] affected = new int[m];
        int next = 1;
        while (true) {
            int mu = -1;
            double t = Double.POSITIVE_INFINITY;
            for (int k = 0; k < m; k++) if (firing[k] < t) { t = firing[k]; mu = k; }
            if (mu < 0) break; // nothing fires before the end of the simulation
            while (next < times.length && times[next] <= t) states[next++] = y.clone();
            if (next >= times.length) break;
            // the transitions whose propensity the event may change, and the fired one
            int n = 0;
            marked[mu] = true;
            affected[n++] = mu;
            int src = g.source(mu), dst = g.target(mu);
            if (src >= 0) for (int k : g.dependents(src)) if (!marked[k]) { marked[k] = true; affected[n++] = k; }
            if (dst >= 0) for (int k : g.dependents(dst)) if (!marked[k]) { marked[k] = true; affected[n++] = k; }
            // their internal times, with the propensities they had before the event
            for (int i = 0; i < n; i++) {
                int k = affected[i];
                if (k != mu) T[k] += integrated(g, k, timed[k], since[k], t, y, h);
                since[k] = t;
            }
            T[mu] = P[mu];
            P[mu] += exponential(random);
            fire(g, mu, y, 1);
            // their firing times, with the propensities they have after it
            for (int i = 0; i < n; i++) {
                int k = affected[i];
                firing[k] = t + timeToFire(g, k, timed[k], t, y, P[k] - T[k], to, h);
                marked[k] = false;
            }
        }
        while (next < times.length) states[next++] = y.clone();
        return new Trajectory(g, times, states);
    }

    /** The integral of the propensity of k over [t0, t1], the state being fixed. */
    private static double integrated(GroundModel g, int k, boolean timed, double t0, double t1, double[] y, double h) {
        if (!timed) return propensity(g, k, t0, y) * (t1 - t0);
        return Quadrature.integral(s -> propensity(g, k, s, y), t0, t1, h);
    }

    /** The delay after t at which the integrated propensity of k reaches target, or +infinity if not before to. */
    private static double timeToFire(GroundModel g, int k, boolean timed, double t, double[] y, double target, double to, double h) {
        if (!timed) {
            double a = propensity(g, k, t, y);
            return a > 0 ? target / a : Double.POSITIVE_INFINITY;
        }
        double x = Quadrature.timeToReach(s -> propensity(g, k, s, y), t, target, to, h);
        return x - t;
    }

    private static double exponential(Random random) {
        return -Math.log(1 - random.nextDouble());
    }
}
