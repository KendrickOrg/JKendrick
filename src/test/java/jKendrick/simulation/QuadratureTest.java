package jKendrick.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The adaptive quadrature of the next reaction solver resolves a rate that varies within a panel.
 * The hazard 1 - cos(32 pi t) is zero at every multiple of 1/16, i.e. at every node of Simpson's rule
 * on eight panels per unit of time (the default), yet its integral over [0, 1] is exactly 1: a fixed
 * grid returns 0 and never fires the transition; the adaptive rule refines the panels and finds it.
 */
class QuadratureTest {

    static final double OMEGA = 32 * Math.PI;
    static final java.util.function.DoubleUnaryOperator HAZARD = t -> 1 - Math.cos(OMEGA * t);

    @Test
    void aHazardThatVanishesAtEveryNodeOfTheGridIsStillIntegrated() {
        double h = 1.0 / 8;                                  // the default: eight panels per recording step
        assertEquals(1.0, Quadrature.integral(HAZARD, 0, 1, h), 1e-7);
        assertEquals(0.5, Quadrature.integral(HAZARD, 0, 0.5, h), 1e-7);
    }

    /** The exact integral over [0, t] of 1 - cos(omega t). */
    static double integrated(double omega, double t) {
        return t - Math.sin(omega * t) / omega;
    }

    @Test
    void theTimeToReachATargetIsFoundOnTheSameHazard() {
        // H(t) = t - sin(32 pi t) / (32 pi) reaches 0.5 at t = 0.5 and 1 at t = 1, where the hazard is zero and
        // H is flat: the time is ill-conditioned there, so the check is on H at the time found
        for (double target : new double[] {0.5, 1.0}) {
            double x = Quadrature.timeToReach(HAZARD, 0, target, 10, 1.0 / 8);
            assertEquals(target, integrated(OMEGA, x), 1e-9);
            assertEquals(target, x, 1e-4);
        }
        assertTrue(Double.isInfinite(Quadrature.timeToReach(HAZARD, 0, 20, 10, 1.0 / 8)));
    }

    @Test
    void fasterHazardsThatVanishAtTheFirstSampledPointsAreIntegrated() {
        // 1 - cos(64 pi t) and 1 - cos(128 pi t) vanish at the five points that a single Simpson comparison
        // samples on a panel of 1/8; a rule that accepts a panel after that comparison returns 0 (referee, Oct 2026)
        for (double omega : new double[] {64 * Math.PI, 128 * Math.PI}) {
            java.util.function.DoubleUnaryOperator hazard = t -> 1 - Math.cos(omega * t);
            assertEquals(1.0, Quadrature.integral(hazard, 0, 1, 1.0 / 8), 1e-7);
            assertEquals(1.0, integrated(omega, Quadrature.timeToReach(hazard, 0, 1.0, 10, 1.0 / 8)), 1e-9);
        }
    }

    @Test
    void aHazardThatVanishesAtEveryPointOfTheFinestForcedGridIsMissedAsDocumented() {
        // the documented limit: with panels of 1/8 every panel is refined to 1/64 and sampled every 1/256;
        // 1 - cos(512 pi t) vanishes at all those points, so its integral (1 over [0, 1]) is missed
        java.util.function.DoubleUnaryOperator hazard = t -> 1 - Math.cos(512 * Math.PI * t);
        assertEquals(0.0, Quadrature.integral(hazard, 0, 1, 1.0 / 8), 1e-12);
    }

    @Test
    void smoothHazardsAreIntegratedToTheTolerance() {
        assertEquals(0.1 * (10 + Math.sin(10)), Quadrature.integral(t -> 0.1 * (1 + Math.cos(t)), 0, 10, 1.0 / 8), 1e-9);
        assertEquals(0.05 * 100, Quadrature.integral(t -> 0.1 * t, 0, 10, 1.0 / 8), 1e-9);
    }
}
