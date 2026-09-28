package jKendrick.simulation;

import jKendrick.model.GroundModel;

import java.util.List;

/**
 * Runs a ground model from {@code from} to {@code to}, recording the state every {@code step}.
 *
 * Deterministic solvers integrate {@link GroundModel#derivatives(double, double[])} and return one
 * trajectory; stochastic solvers simulate the composed CTMC through {@link GroundModel#transitions()}
 * and return one trajectory per run.
 */
public interface Solver {

    List<Trajectory> solve(GroundModel model, double from, double to, double step);

    default boolean isStochastic() {
        return false;
    }

    /** The recording grid from..to by step, including both ends. */
    static double[] grid(double from, double to, double step) {
        int n = (int) Math.floor((to - from) / step + 1e-9) + 1;
        double[] t = new double[n];
        for (int i = 0; i < n; i++) t[i] = from + i * step;
        return t;
    }
}
