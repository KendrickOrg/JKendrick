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
        while (next < times.length) {
            double[] a = propensities(g, transitions, t, y);
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
            fire(g, transitions.get(chosen), y, 1);
        }
        while (next < times.length) states[next++] = y.clone();
        return new Trajectory(g, times, states);
    }
}
