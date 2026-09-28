package jKendrick.simulation;

import jKendrick.model.ComposedModel;
import jKendrick.model.GroundModel;

import java.util.List;
import java.util.Random;

/** Gillespie's direct method (stochastic simulation algorithm) on the composed CTMC. */
public final class GillespieSolver extends StochasticSolver {

    public GillespieSolver(int runs, long seed) {
        super(runs, seed);
    }

    @Override
    protected Trajectory run(GroundModel g, double from, double to, double step, Random random) {
        double[] times = Solver.grid(from, to, step);
        List<ComposedModel.Transition> transitions = g.transitions();
        double[] y = g.initialState();
        for (int i = 0; i < y.length; i++) y[i] = Math.round(y[i]);
        double[][] states = new double[times.length][];
        states[0] = y.clone();
        int next = 1;
        double t = from;
        double[] a = new double[transitions.size()];
        propensities(g, t, y, a);
        int[] timed = g.timeDependent();
        while (next < times.length) {
            double total = 0;
            for (double v : a) total += v;
            if (total <= 0) break; // absorbing state: nothing happens any more
            t += -Math.log(1 - random.nextDouble()) / total;
            while (next < times.length && times[next] <= t) states[next++] = y.clone();
            if (next >= times.length) break;
            double u = random.nextDouble() * total, acc = 0;
            int chosen = a.length - 1;
            for (int i = 0; i < a.length; i++) {
                acc += a[i];
                if (u < acc) { chosen = i; break; }
            }
            fire(g, chosen, y, 1);
            // only the propensities that read a changed compartment (or the time) need recomputing
            int src = g.source(chosen), dst = g.target(chosen);
            if (src >= 0) for (int i : g.dependents(src)) a[i] = g.propensity(i, t, y);
            if (dst >= 0) for (int i : g.dependents(dst)) a[i] = g.propensity(i, t, y);
            for (int i : timed) a[i] = g.propensity(i, t, y);
        }
        while (next < times.length) states[next++] = y.clone();
        return new Trajectory(g, times, states);
    }
}
