package jKendrick.simulation;

import java.util.function.DoubleUnaryOperator;

/**
 * Integrals of a nonnegative function of the time, for the next reaction solver: the integrated
 * propensity of a transition whose rate reads the time, over an interval during which the state
 * does not change.
 *
 * Adaptive Simpson's rule, started on panels of at most {@code h}: a panel is accepted when the
 * Simpson estimate over it agrees with the sum of the estimates over its two halves to within
 * {@link #RELATIVE} of the integral (plus a floor for integrals near zero), and is split otherwise,
 * down to {@link #MAX_DEPTH} halvings. A rate that varies within a panel is thus resolved by refining
 * the grid, not missed; a rate that vanishes at every node of every dyadic refinement of the grid,
 * down to the maximum depth, would still be missed, which is the documented limit of the method.
 * The inverse problem (the time at which the integral reaches a target) is solved by marching panel
 * by panel with the same adaptive estimates and bisecting in the panel where the target is reached.
 */
final class Quadrature {
    private Quadrature() {}

    /** Relative tolerance of the adaptive rule, on the panel's integral. */
    static final double RELATIVE = 1e-9;
    /** Absolute floor of the tolerance, per unit of panel width, for integrals near zero. */
    static final double FLOOR = 1e-12;
    /** Maximum number of halvings of a panel. */
    static final int MAX_DEPTH = 40;

    private static double simpson(double a, double fa, double b, double fb, double fm) {
        return (b - a) / 6 * (fa + 4 * fm + fb);
    }

    /** Adaptive Simpson over [a, b], given f at a, at b and at the midpoint, and the whole-panel estimate. */
    private static double adaptive(DoubleUnaryOperator f, double a, double fa, double b, double fb, double fm,
                                   double whole, int depth) {
        double m = (a + b) / 2;
        double fl = f.applyAsDouble((a + m) / 2), fr = f.applyAsDouble((m + b) / 2);
        double left = simpson(a, fa, m, fm, fl), right = simpson(m, fm, b, fb, fr);
        double halves = left + right;
        double tolerance = RELATIVE * Math.abs(halves) + FLOOR * (b - a);
        if (depth >= MAX_DEPTH || Math.abs(halves - whole) <= 15 * tolerance) return halves + (halves - whole) / 15;
        return adaptive(f, a, fa, m, fm, fl, left, depth + 1) + adaptive(f, m, fm, b, fb, fr, right, depth + 1);
    }

    /** The integral of f over [a, b], adaptive Simpson from the whole interval as the first panel. */
    private static double panel(DoubleUnaryOperator f, double a, double b) {
        if (b <= a) return 0;
        double fa = f.applyAsDouble(a), fb = f.applyAsDouble(b), fm = f.applyAsDouble((a + b) / 2);
        return adaptive(f, a, fa, b, fb, fm, simpson(a, fa, b, fb, fm), 0);
    }

    /** The integral of f over [t0, t1], by the adaptive rule on panels of at most h. */
    static double integral(DoubleUnaryOperator f, double t0, double t1, double h) {
        if (t1 <= t0) return 0;
        int n = (int) Math.max(1, Math.ceil((t1 - t0) / h));
        double w = (t1 - t0) / n, total = 0;
        for (int i = 0; i < n; i++) {
            double a = t0 + i * w, b = i == n - 1 ? t1 : a + w;
            total += panel(f, a, b);
        }
        return total;
    }

    /**
     * The smallest x in [t0, horizon] such that the integral of f over [t0, x] reaches {@code target},
     * or +infinity when the integral over [t0, horizon] stays below it.
     */
    static double timeToReach(DoubleUnaryOperator f, double t0, double target, double horizon, double h) {
        if (target <= 0) return t0;
        double acc = 0, a = t0;
        while (a < horizon) {
            double b = Math.min(a + h, horizon);
            double whole = panel(f, a, b);
            if (acc + whole >= target) {
                // bisection on x in [a, b]: acc + integral over [a, x] = target
                double lo = a, hi = b;
                for (int i = 0; i < 60 && hi - lo > 1e-12 * Math.max(1, Math.abs(hi)); i++) {
                    double mid = (lo + hi) / 2;
                    if (acc + panel(f, a, mid) >= target) hi = mid; else lo = mid;
                }
                return hi;
            }
            acc += whole;
            a = b;
        }
        return Double.POSITIVE_INFINITY;
    }
}
