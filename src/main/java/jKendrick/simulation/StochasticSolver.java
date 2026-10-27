package jKendrick.simulation;

import jKendrick.model.Compartment;
import jKendrick.model.Concern;
import jKendrick.model.GroundModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Common part of the stochastic solvers: they simulate the composed CTMC, whose events are the
 * transitions of the composed automaton with propensity rate(c) * |c|, so every concern must be
 * given by transitions (a concern given by ODEs has no transitions to draw from).
 *
 * What they check. Before the runs: every concern has transitions, and the initial state is a
 * vector of nonnegative integers (an unindexed initial cardinality is spread evenly over the
 * compartments it designates, which may give fractions: index it so that every compartment gets
 * an integer). During a run: every propensity evaluated is finite and nonnegative, else the run
 * stops with an error naming the transition and the time. A rate is evaluated only where its
 * source compartment is nonempty, so a quotient by an empty sub-population is not an error until
 * an individual is there to move. Not checked: the number of events before the end of the run,
 * which is finite when the inflow rates grow at most linearly with the population.
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
        double[] y0 = g.initialState();
        for (int i = 0; i < y0.length; i++)
            if (!(y0[i] >= 0) || y0[i] != Math.rint(y0[i])) {
                Compartment c = g.model.compartments.get(i);
                throw new IllegalStateException("stochastic simulation needs a nonnegative integer initial state; "
                        + c + " is " + y0[i] + " (an unindexed initial cardinality is spread evenly over the "
                        + "compartments it designates: index it so that every compartment gets an integer)");
            }
        List<Trajectory> result = new ArrayList<>();
        // one seed per run from SplitMix64: consecutive seeds of java.util.Random give nearly equal first
        // draws, which made the first clocks of the next reaction method nearly equal across runs
        java.util.SplittableRandom seeds = new java.util.SplittableRandom(seed);
        for (int r = 0; r < runs; r++) result.add(run(g, from, to, step, new Random(seeds.nextLong())));
        return result;
    }

    protected abstract Trajectory run(GroundModel g, double from, double to, double step, Random random);

    /**
     * The propensity of the i-th transition in the given state and time, checked: a propensity
     * that is negative, infinite or undefined stops the run.
     */
    protected static double propensity(GroundModel g, int i, double t, double[] y) {
        double a = g.propensity(i, t, y);
        if (!(a >= 0) || Double.isInfinite(a))
            throw new IllegalStateException("the rate of " + g.transitions().get(i) + " gives the propensity " + a
                    + " at t = " + t + ": a propensity must be finite and nonnegative");
        return a;
    }

    /** Propensities of all transitions in the current state, checked, into {@code a}. */
    protected static void propensities(GroundModel g, double t, double[] y, double[] a) {
        for (int i = 0; i < a.length; i++) a[i] = propensity(g, i, t, y);
    }

    /** Applies {@code times} firings of the i-th transition to the state. */
    protected static void fire(GroundModel g, int i, double[] y, int times) {
        int from = g.source(i), to = g.target(i);
        if (from >= 0) y[from] -= times;
        if (to >= 0) y[to] += times;
    }
}
