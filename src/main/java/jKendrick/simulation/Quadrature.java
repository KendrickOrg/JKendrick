package jKendrick.simulation;

import java.util.function.DoubleUnaryOperator;

/**
 * Integrals of a nonnegative function of the time, for the next reaction solver: the integrated
 * propensity of a transition whose rate reads the time, over an interval during which the state
 * does not change.
 *
 * Adaptive Simpson's rule on starting panels of width at most {@code h}. Every panel is first split,
 * whatever the estimates, until its width is at most h / 2^{@link #MIN_DEPTH}. Only then can a panel be
 * accepted: when the Simpson estimate over it agrees with the sum of the estimates over its two halves to
 * within {@link #RELATIVE} of the integral (plus a floor for integrals near zero). Otherwise it is split
 * further, down to {@link #MAX_DEPTH} halvings. Before any panel is accepted, the function has therefore
 * been sampled on a grid of spacing at most h / 2^(MIN_DEPTH + 2), that is h / 32.
 *
 * The integral is computed to the tolerance for a function that is smooth on the scale of that grid. A
 * function whose mass lies between the points of the grid, such as one that vanishes at all of them, is
 * missed: no rule that samples a function at finitely many points can exclude that, and
 * {@code QuadratureTest} shows such a case. The width h does not depend on the recording step alone: the
 * solver caps it by an absolute width ({@code quadratureStep:}, see {@link SimulationRunner}).
 *
 * The inverse problem (the time at which the integral reaches a target) marches panel by panel. In the
 * panel where the target is reached, it halves the interval repeatedly, integrating each half with the
 * same rule, so that the event time is found with the same resolution as the integral.
 */
final class Quadrature {
    private Quadrature() {}

    /** Relative tolerance of the adaptive rule, on the panel's integral. */
    static final double RELATIVE = 1e-9;
    /** Absolute floor of the tolerance, per unit of panel width, for integrals near zero. */
    static final double FLOOR = 1e-12;
    /** Maximum number of halvings of a panel. */
    static final int MAX_DEPTH = 40;
    /** Halvings of a starting panel made whatever the estimates, before any panel can be accepted. */
    static final int MIN_DEPTH = 3;

    private static double simpson(double a, double fa, double b, double fb, double fm) {
        return (b - a) / 6 * (fa + 4 * fm + fb);
    }

    /**
     * Adaptive Simpson over [a, b], given f at a, at b and at the midpoint, and the whole-panel estimate.
     * A panel wider than minWidth is split whatever its estimates; a narrower one is accepted when its two
     * estimates agree.
     */
    private static double adaptive(DoubleUnaryOperator f, double a, double fa, double b, double fb, double fm,
                                   double whole, int depth, double minWidth) {
        double m = (a + b) / 2;
        double fl = f.applyAsDouble((a + m) / 2), fr = f.applyAsDouble((m + b) / 2);
        double left = simpson(a, fa, m, fm, fl), right = simpson(m, fm, b, fb, fr);
        double halves = left + right;
        double tolerance = RELATIVE * Math.abs(halves) + FLOOR * (b - a);
        boolean narrow = b - a <= minWidth * (1 + 1e-12);
        if (depth >= MAX_DEPTH || (narrow && Math.abs(halves - whole) <= 15 * tolerance))
            return halves + (halves - whole) / 15;
        return adaptive(f, a, fa, m, fm, fl, left, depth + 1, minWidth)
                + adaptive(f, m, fm, b, fb, fr, right, depth + 1, minWidth);
    }

    /** The integral of f over [a, b], adaptive Simpson from the whole interval as the first panel. */
    private static double panel(DoubleUnaryOperator f, double a, double b, double minWidth) {
        if (b <= a) return 0;
        double fa = f.applyAsDouble(a), fb = f.applyAsDouble(b), fm = f.applyAsDouble((a + b) / 2);
        return adaptive(f, a, fa, b, fb, fm, simpson(a, fa, b, fb, fm), 0, minWidth);
    }

    /** The width below which a panel may be accepted, for starting panels of width h. */
    static double minWidth(double h) {
        return h / (1 << MIN_DEPTH);
    }

    /** The integral of f over [t0, t1], by the adaptive rule on starting panels of at most h. */
    static double integral(DoubleUnaryOperator f, double t0, double t1, double h) {
        if (t1 <= t0) return 0;
        double minWidth = minWidth(h);
        int n = (int) Math.max(1, Math.ceil((t1 - t0) / h));
        double w = (t1 - t0) / n, total = 0;
        for (int i = 0; i < n; i++) {
            double a = t0 + i * w, b = i == n - 1 ? t1 : a + w;
            total += panel(f, a, b, minWidth);
        }
        return total;
    }

    /**
     * The smallest x in [t0, horizon] such that the integral of f over [t0, x] reaches {@code target},
     * or +infinity when the integral over [t0, horizon] stays below it.
     */
    static double timeToReach(DoubleUnaryOperator f, double t0, double target, double horizon, double h) {
        if (target <= 0) return t0;
        double minWidth = minWidth(h);
        double acc = 0, a = t0;
        while (a < horizon) {
            double b = Math.min(a + h, horizon);
            double whole = panel(f, a, b, minWidth);
            if (acc + whole >= target) {
                // halve [lo, hi] until it is small: acc is the integral over [t0, lo]
                double lo = a, hi = b;
                for (int i = 0; i < 60 && hi - lo > 1e-12 * Math.max(1, Math.abs(hi)); i++) {
                    double mid = (lo + hi) / 2;
                    double left = panel(f, lo, mid, minWidth);
                    if (acc + left >= target) hi = mid;
                    else { acc += left; lo = mid; }
                }
                return hi;
            }
            acc += whole;
            a = b;
        }
        return Double.POSITIVE_INFINITY;
    }
}
