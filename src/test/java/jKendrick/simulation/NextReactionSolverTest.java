package jKendrick.simulation;

import jKendrick.Kendrick;
import jKendrick.model.GroundModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The next reaction solver against processes whose law is known exactly. A pure death process
 * A -- r --> B from n individuals leaves at time t a binomial number of them, with mean
 * n * exp(-H(t)) where H is the integrated hazard; its mean-field equation is exact, and the
 * ensemble mean of the runs must agree with it to within the sampling error, which the binomial
 * variance gives.
 */
class NextReactionSolverTest {

    static final int N = 1000, RUNS = 200;

    static String decay(String rate, String solver) {
        return "KModel Decay\n"
                + "    attribute: #(status in (A B));\n"
                + "    parameters: #(r);\n"
                + "    transitions: #(A -- r --> B).\n"
                + "Composition C model: 'Decay'.\n"
                + "Scenario Sc on: 'C'; r: " + (rate.matches("[0-9.]+") ? rate : "#(" + rate + ")") + "; A: " + N + "; B: 0.\n"
                + "Simulation Sim solver: '" + solver + "'; scenario: 'Sc'; from: 0; to: 10; step: 1; runs: " + RUNS + "; seed: 11.\n";
    }

    /** Mean of A at the given day over the runs. */
    static double meanA(List<Trajectory> runs, int day) {
        double m = 0;
        for (Trajectory t : runs) m += t.states[day][0];
        return m / runs.size();
    }

    static double varianceA(List<Trajectory> runs, int day, double mean) {
        double v = 0;
        for (Trajectory t : runs) v += (t.states[day][0] - mean) * (t.states[day][0] - mean);
        return v / (runs.size() - 1);
    }

    /** Asserts the ensemble mean of A at the day within three standard errors of n * survival. */
    static void assertSurvival(List<Trajectory> runs, int day, double survival) {
        double expected = N * survival, sd = Math.sqrt(N * survival * (1 - survival));
        double standardError = sd / Math.sqrt(RUNS);
        assertEquals(expected, meanA(runs, day), 3 * standardError,
                "mean of A at day " + day + " (standard error " + standardError + ")");
    }

    @Test
    void constantHazardAgreesWithTheExactLaw() {
        List<Trajectory> runs = Kendrick.run(Kendrick.load(decay("0.1", "nextReaction")));
        assertEquals(RUNS, runs.size());
        assertSurvival(runs, 5, Math.exp(-0.5));
        assertSurvival(runs, 10, Math.exp(-1));
        // the variance too: binomial, n p (1 - p); the sample variance of 200 runs has a relative standard error of sqrt(2/199)
        double p = Math.exp(-1), expectedVariance = N * p * (1 - p);
        assertEquals(expectedVariance, varianceA(runs, 10, meanA(runs, 10)), 3 * expectedVariance * Math.sqrt(2.0 / (RUNS - 1)));
        for (Trajectory t : runs)
            for (double[] state : t.states) {
                assertEquals(N, state[0] + state[1], 0.0);
                assertTrue(state[0] >= 0 && state[0] == Math.rint(state[0]));
            }
    }

    @Test
    void constantHazardAgreesWithGillespie() {
        List<Trajectory> nrm = Kendrick.run(Kendrick.load(decay("0.1", "nextReaction")));
        List<Trajectory> ssa = Kendrick.run(Kendrick.load(decay("0.1", "gillespie")));
        double sd = Math.sqrt(N * Math.exp(-1) * (1 - Math.exp(-1)));
        assertEquals(meanA(ssa, 10), meanA(nrm, 10), 3 * sd * Math.sqrt(2.0 / RUNS));
        assertSurvival(ssa, 5, Math.exp(-0.5));                 // the exact law under Gillespie too
        assertSurvival(ssa, 10, Math.exp(-1));
    }

    @Test
    void linearHazardFromZeroAgreesWithTheExactLaw() {
        // r = 0.1 t: H(t) = 0.05 t^2; the rate is zero at the start, where the direct method would see an absorbing state
        List<Trajectory> runs = Kendrick.run(Kendrick.load(decay("0.1*t", "nextReaction")));
        assertSurvival(runs, 5, Math.exp(-0.05 * 25));
        assertSurvival(runs, 10, Math.exp(-0.05 * 100));
    }

    @Test
    void oscillatingHazardAgreesWithTheExactLaw() {
        // r = 0.1 (1 + cos t): H(t) = 0.1 (t + sin t), a seasonal forcing with a period of 2 pi days
        List<Trajectory> runs = Kendrick.run(Kendrick.load(decay("0.1*(1+cos(t))", "nextReaction")));
        assertSurvival(runs, 5, Math.exp(-0.1 * (5 + Math.sin(5))));
        assertSurvival(runs, 10, Math.exp(-0.1 * (10 + Math.sin(10))));
    }

    @Test
    void hazardThatVanishesAtEveryNodeOfTheDefaultGridIsNotMissed() {
        // r = 1 - cos(32 pi t) is zero at every multiple of 1/16, the nodes of Simpson's rule on the default
        // eight panels per day; H(t) = t - sin(32 pi t) / (32 pi), so H(5) = 5 and H(10) = 10: a fixed grid
        // would fire nothing, the adaptive quadrature finds the hazard (a referee's counterexample, Oct 2026)
        String script = decay("1-cos(100.53096491487338*t)", "nextReaction").replace("to: 10;", "to: 1;");
        List<Trajectory> runs = Kendrick.run(Kendrick.load(script));
        assertSurvival(runs, 1, Math.exp(-1));
    }

    @Test
    void hazardThatStartsLaterAgreesWithTheExactLaw() {
        // r = 0.2 max(0, t - 5): nothing before day 5, then H(t) = 0.1 (t - 5)^2
        List<Trajectory> runs = Kendrick.run(Kendrick.load(decay("0.2*max(0, t-5)", "nextReaction")));
        for (Trajectory t : runs) assertEquals(N, t.states[5][0], 0.0);
        assertSurvival(runs, 10, Math.exp(-0.1 * 25));
    }

    @Test
    void gillespieRefusesARateThatReadsTheTime() {
        GroundModel m = Kendrick.load(decay("0.1*t", "gillespie"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Kendrick.run(m));
        assertTrue(ex.getMessage().contains("reads the time"), ex.getMessage());
        assertTrue(ex.getMessage().contains("nextReaction"), ex.getMessage());
    }

    @Test
    void sameSeedSameTrajectory() {
        GroundModel m = Kendrick.load(decay("0.1*(1+cos(t))", "nextReaction"));
        Trajectory a = new NextReactionSolver(1, 3).solve(m, 0, 10, 1).get(0);
        Trajectory b = new NextReactionSolver(1, 3).solve(m, 0, 10, 1).get(0);
        for (int i = 0; i < a.size(); i++) assertEquals(a.states[i][0], b.states[i][0], 0.0);
    }

    @Test
    void sirEpidemicWithSeasonalForcingIsCloseToTheOdeOnAverage() {
        String sir = "KModel SIR\n"
                + "    attribute: #(status in (S I R));\n"
                + "    parameters: #(beta gamma lambda);\n"
                + "    lambda: #(beta*(1+0.5*cos(t/10))*I/N);\n"
                + "    transitions: #(S -- lambda --> I. I -- gamma --> R).\n"
                + "Composition C model: 'SIR'.\n"
                + "Scenario Sc on: 'C'; beta: 0.5; gamma: 0.25; S: 9900; I: 100.\n"
                + "Simulation Sim solver: 'nextReaction'; scenario: 'Sc'; from: 0; to: 60; step: 1; runs: 20; seed: 7.\n";
        GroundModel m = Kendrick.load(sir);
        List<Trajectory> runs = Kendrick.run(m);
        double meanR60 = 0;
        for (Trajectory t : runs) {
            for (double[] state : t.states) assertEquals(10000.0, state[0] + state[1] + state[2], 0.0);
            meanR60 += t.last()[2];
        }
        meanR60 /= runs.size();
        double odeR60 = new RungeKuttaSolver(4).solve(m, 0, 60, 1).get(0).last()[2];
        assertEquals(odeR60, meanR60, odeR60 * 0.05);
    }
}
