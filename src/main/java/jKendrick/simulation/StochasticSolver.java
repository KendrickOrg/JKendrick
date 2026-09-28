package jKendrick.simulation;

import jKendrick.model.ComposedModel;
import jKendrick.model.Concern;
import jKendrick.model.GroundModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Common part of the stochastic solvers: they simulate the composed CTMC, whose events are the
 * transitions of the composed automaton with propensity rate(c) * |c|, so every concern must be
 * given by transitions (a concern given by ODEs has no transitions to draw from).
 */
public abstract class StochasticSolver implements Solver {
    protected final int runs;
    protected final long seed;

    protected StochasticSolver(int runs, long seed) {
        if (runs < 1) throw new IllegalArgumentException("runs must be >= 1");
        this.runs = runs;
        this.seed = seed;
    }

    @Override
    public boolean isStochastic() {
        return true;
    }

    @Override
    public List<Trajectory> solve(GroundModel g, double from, double to, double step) {
        for (Concern k : g.model.concerns)
            if (k.definedByOdesOnly())
                throw new IllegalStateException("stochastic simulation needs every concern to be given by transitions, "
                        + "or by equations in flow form (every term a flow proportional to its source compartment); "
                        + "the equations of " + k.name + " could not be read as flows");
        List<Trajectory> result = new ArrayList<>();
        for (int r = 0; r < runs; r++) result.add(run(g, from, to, step, new Random(seed + r)));
        return result;
    }

    protected abstract Trajectory run(GroundModel g, double from, double to, double step, Random random);

    /** Propensities of all transitions in the current state. */
    protected static double[] propensities(GroundModel g, List<ComposedModel.Transition> transitions, double t, double[] y) {
        double[] a = new double[transitions.size()];
        for (int i = 0; i < a.length; i++) a[i] = g.propensity(transitions.get(i), t, y);
        return a;
    }

    /** Applies one firing of a transition to the state. */
    protected static void fire(GroundModel g, ComposedModel.Transition tr, double[] y, int times) {
        if (tr.from != null) y[g.model.indexOf(tr.from)] -= times;
        if (tr.to != null) y[g.model.indexOf(tr.to)] += times;
    }
}
