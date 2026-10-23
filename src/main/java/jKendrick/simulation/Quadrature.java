package jKendrick.simulation;

import java.util.function.DoubleUnaryOperator;

/**
 * Integrals of a nonnegative function of the time, for the next reaction solver: the integrated
 * propensity of a transition whose rate reads the time, over an interval during which the state
 * does not change.
 *
 * Composite Simpson's rule on panels of at most {@code h}; the inverse problem (the time at which
 * the integral reaches a target) is solved by marching panel by panel and bisecting in the panel
 * where the target is reached.
 */
final class Quadrature {
    private Quadrature() {}

    /** The integral of f over [t0, t1], by Simpson's rule on panels of at most h. */
    static double integral(DoubleUnaryOperator f, double t0, double t1, double h) {
        if (t1 <= t0) return 0;
        int n = (int) Math.max(1, Math.ceil((t1 - t0) / h));
        double w = (t1 - t0) / n, total = 0, left = f.applyAsDouble(t0);
        for (int i = 0; i < n; i++) {
            double a = t0 + i * w, b = i == n - 1 ? t1 : a + w;
            double right = f.applyAsDouble(b);
            total += (b - a) / 6 * (left + 4 * f.applyAsDouble((a + b) / 2) + right);
            left = right;
        }
        return total;
    }

    /**
     * The smallest x in [t0, horizon] such that the integral of f over [t0, x] reaches {@code target},
     * or +infinity when the integral over [t0, horizon] stays below it.
     */
    static double timeToReach(DoubleUnaryOperator f, double t0, double target, double horizon, double h) {
        if (target <= 0) return t0;
        double acc = 0, a = t0, fa = f.applyAsDouble(a);
        while (a < horizon) {
            double b = Math.min(a + h, horizon);
            double fm = f.applyAsDouble((a + b) / 2), fb = f.applyAsDouble(b);
            double panel = (b - a) / 6 * (fa + 4 * fm + fb);
            if (acc + panel >= target) {
                // bisection on x in [a, b]: acc + integral over [a, x] = target
                double lo = a, hi = b;
                for (int i = 0; i < 60 && hi - lo > 1e-12 * Math.max(1, Math.abs(hi)); i++) {
                    double mid = (lo + hi) / 2;
                    double part = (mid - a) / 6 * (fa + 4 * f.applyAsDouble((a + mid) / 2) + f.applyAsDouble(mid));
                    if (acc + part >= target) hi = mid; else lo = mid;
                }
                return hi;
            }
            acc += panel;
            a = b;
            fa = fb;
        }
        return Double.POSITIVE_INFINITY;
    }
}
