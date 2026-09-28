package jKendrick.simulation;

import jKendrick.model.GroundModel;

import java.util.Collections;
import java.util.List;

/**
 * Classical fourth-order Runge-Kutta with a fixed step, on the composed ODE system of the
 * ground model. The integration step is the recording step divided by {@code substeps}.
 */
public final class RungeKuttaSolver implements Solver {
    private final int substeps;

    public RungeKuttaSolver() {
        this(1);
    }

    public RungeKuttaSolver(int substeps) {
        if (substeps < 1) throw new IllegalArgumentException("substeps must be >= 1");
        this.substeps = substeps;
    }

    @Override
    public List<Trajectory> solve(GroundModel g, double from, double to, double step) {
        double[] times = Solver.grid(from, to, step);
        int n = g.dimension();
        double[][] states = new double[times.length][];
        double[] y = g.initialState();
        states[0] = y.clone();
        double h = step / substeps;
        double[] tmp = new double[n];
        for (int i = 1; i < times.length; i++) {
            double t = times[i - 1];
            for (int s = 0; s < substeps; s++) {
                double[] k1 = g.derivatives(t, y);
                for (int j = 0; j < n; j++) tmp[j] = y[j] + h / 2 * k1[j];
                double[] k2 = g.derivatives(t + h / 2, tmp);
                for (int j = 0; j < n; j++) tmp[j] = y[j] + h / 2 * k2[j];
                double[] k3 = g.derivatives(t + h / 2, tmp);
                for (int j = 0; j < n; j++) tmp[j] = y[j] + h * k3[j];
                double[] k4 = g.derivatives(t + h, tmp);
                for (int j = 0; j < n; j++) y[j] += h / 6 * (k1[j] + 2 * k2[j] + 2 * k3[j] + k4[j]);
                t += h;
            }
            states[i] = y.clone();
        }
        return Collections.singletonList(new Trajectory(g, times, states));
    }
}
