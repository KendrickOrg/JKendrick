package jKendrick.simulation;

import jKendrick.Kendrick;
import jKendrick.model.GroundModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the stochastic solvers check, and what they refuse. */
class AdmissibilityTest {

    static final String SIR = "KModel SIR\n"
            + "    attribute: #(status in (S I R));\n"
            + "    parameters: #(beta gamma lambda);\n"
            + "    lambda: #(beta*I/N);\n"
            + "    transitions: #(S -- lambda --> I. I -- gamma --> R).\n";

    static final String ZONES = "KModel Zones attribute: #(zone in (a b)).\n";

    @Test
    void aNegativeRateStopsTheRun() {
        GroundModel m = Kendrick.load(SIR + "Composition C model: 'SIR'.\n"
                + "Scenario Sc on: 'C'; beta: 0.5; gamma: #(0-0.25); S: 990; I: 10.\n"
                + "Simulation Sim solver: 'gillespie'; scenario: 'Sc'; from: 0; to: 10; step: 1.\n");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Kendrick.run(m));
        assertTrue(ex.getMessage().contains("I --> R"), ex.getMessage());
        assertTrue(ex.getMessage().contains("nonnegative"), ex.getMessage());
    }

    @Test
    void anUndefinedRateStopsTheRun() {
        // the force of infection of zone a reads the empty zone b: 0/0
        GroundModel m = Kendrick.load(SIR + ZONES + "Composition C model: 'SIR'; model: 'Zones'.\n"
                + "Scenario Sc on: 'C'; beta: 0.5; gamma: 0.25; lambda: #(beta*I_b/N_b); S_zone: #(990 0); I_zone: #(10 0).\n"
                + "Simulation Sim solver: 'gillespie'; scenario: 'Sc'; from: 0; to: 10; step: 1.\n");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Kendrick.run(m));
        assertTrue(ex.getMessage().contains("NaN"), ex.getMessage());
        assertTrue(ex.getMessage().contains("S_a --> I_a"), ex.getMessage());
    }

    @Test
    void theNextReactionSolverChecksToo() {
        GroundModel m = Kendrick.load(SIR + "Composition C model: 'SIR'.\n"
                + "Scenario Sc on: 'C'; beta: 0.5; gamma: #(0-0.25); S: 990; I: 10.\n"
                + "Simulation Sim solver: 'nextReaction'; scenario: 'Sc'; from: 0; to: 10; step: 1.\n");
        assertThrows(IllegalStateException.class, () -> Kendrick.run(m));
    }

    @Test
    void aFractionalInitialStateIsRefused() {
        // 5 susceptibles spread evenly over two zones are 2.5 each
        GroundModel m = Kendrick.load(SIR + ZONES + "Composition C model: 'SIR'; model: 'Zones'.\n"
                + "Scenario Sc on: 'C'; beta: 0.5; gamma: 0.25; S: 5; I: 1.\n"
                + "Simulation Sim solver: 'gillespie'; scenario: 'Sc'; from: 0; to: 2; step: 1.\n");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Kendrick.run(m));
        assertTrue(ex.getMessage().contains("S_a is 2.5"), ex.getMessage());
        // the deterministic solver takes it
        assertEquals(2.5, new RungeKuttaSolver(1).solve(m, 0, 2, 1).get(0).states[0][0], 0.0);
    }

    @Test
    void tauLeapingConservesThePopulationWhenALeapOvershoots() {
        GroundModel m = Kendrick.load(SIR + "Composition C model: 'SIR'.\n"
                + "Scenario Sc on: 'C'; beta: 50; gamma: 0.25; S: 10; I: 990.\n"
                + "Simulation Sim solver: 'tauLeap'; scenario: 'Sc'; from: 0; to: 5; step: 1; seed: 7.\n");
        List<Trajectory> runs = Kendrick.run(m);
        for (double[] state : runs.get(0).states) {
            assertEquals(1000.0, state[0] + state[1] + state[2], 0.0);
            for (double v : state) assertTrue(v >= 0);
        }
        assertEquals(0.0, runs.get(0).states[1][0], 0.0); // the ten susceptibles are all infected in the first leap
    }
}
