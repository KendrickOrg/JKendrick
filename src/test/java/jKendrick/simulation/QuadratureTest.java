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

    @Test
    void theTimeToReachATargetIsFoundOnTheSameHazard() {
        // H(t) = t - sin(32 pi t) / (32 pi) equals 0.5 at t = 0.5 and 1 at t = 1
        assertEquals(0.5, Quadrature.timeToReach(HAZARD, 0, 0.5, 10, 1.0 / 8), 1e-6);
        assertEquals(1.0, Quadrature.timeToReach(HAZARD, 0, 1.0, 10, 1.0 / 8), 1e-6);
        assertTrue(Double.isInfinite(Quadrature.timeToReach(HAZARD, 0, 20, 10, 1.0 / 8)));
    }

    @Test
    void smoothHazardsAreIntegratedToTheTolerance() {
        assertEquals(0.1 * (10 + Math.sin(10)), Quadrature.integral(t -> 0.1 * (1 + Math.cos(t)), 0, 10, 1.0 / 8), 1e-9);
        assertEquals(0.05 * 100, Quadrature.integral(t -> 0.1 * t, 0, 10, 1.0 / 8), 1e-9);
    }
}
