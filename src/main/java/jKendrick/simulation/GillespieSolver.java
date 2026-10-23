package jKendrick.simulation;

import jKendrick.model.ComposedModel;
import jKendrick.model.GroundModel;

import java.util.List;
import java.util.Random;

/**
 * Gillespie's direct method (stochastic simulation algorithm) on the composed CTMC: the time to
 * the next event is exponential with rate the sum of the propensities, the event is drawn in
 * proportion to its propensity. The propensities are held constant between two events, which is
 * exact when no rate reads the time; a model with a rate that reads the time is refused and
 * directed to the next reaction solver, which integrates the propensities between events.
 */
public final class GillespieSolver extends StochasticSolver {

    public GillespieSolver(int runs, long seed) {
        super(runs, seed);
    }

    @Override
    protected Trajectory run(GroundModel g, double from, double to, double step, Random random) {
        int[] timed = g.timeDependent();
        if (timed.length > 0)
            throw new IllegalStateException("the rate of " + g.transitions().get(timed[0]) + " reads the time: "
                    + "Gillespie's direct method holds the rates constant between events; "
                    + "use the nextReaction solver, which integrates them");
        double[] times = Solver.grid(from, to, step);
        List<ComposedModel.Transition> transitions = g.transitions();
        double[] y = g.initialState();
        double[][] states = new double[times.length][];
        states[0] = y.clone();
        int next = 1;
        double t = from;
        double[] a = new double[transitions.size()];
        propensities(g, t, y, a);
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
            // only the propensities that read a changed compartment need recomputing
            int src = g.source(chosen), dst = g.target(chosen);
            if (src >= 0) for (int i : g.dependents(src)) a[i] = propensity(g, i, t, y);
            if (dst >= 0) for (int i : g.dependents(dst)) a[i] = propensity(g, i, t, y);
        }
        while (next < times.length) states[next++] = y.clone();
        return new Trajectory(g, times, states);
    }
}
