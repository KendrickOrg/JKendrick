package jKendrick.simulation;

import jKendrick.Kendrick;
import jKendrick.model.GroundModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolversTest {

    static GroundModel model(int n) throws IOException {
        String path = "/models/Model" + n + ".kendrick";
        try (InputStream in = SolversTest.class.getResourceAsStream(path)) {
            return Kendrick.load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /** SIR with the transition syntax, so that the stochastic solvers apply. */
    static final String SIR = "KModel SIR\n"
            + "    attribute: #(status in (S I R));\n"
            + "    parameters: #(beta gamma lambda);\n"
            + "    lambda: #(beta*I/N);\n"
            + "    transitions: #(S -- lambda --> I. I -- gamma --> R).\n"
            + "Composition C model: 'SIR'.\n"
            + "Scenario Sc on: 'C'; beta: 0.5; gamma: 0.25; S: 990; I: 10.\n"
            + "Simulation Sim solver: 'gillespie'; scenario: 'Sc'; from: 0; to: 60; step: 1; runs: 20; seed: 7.\n";

    @Test
    void theSimulationEntityDrivesTheRun() throws IOException {
        GroundModel m = model(3);
        List<Trajectory> runs = Kendrick.run(m); // rungeKutta, 0..550, step 1
        assertEquals(1, runs.size());
        Trajectory t = runs.get(0);
        assertEquals(551, t.size());
        assertEquals(550.0, t.times[550]);
        Map<String, double[]> byStatusSpecies = t.aggregate("status", "species");
        assertEquals(5 * 9.321, byStatusSpecies.get("I_birds")[550], 5 * 9.321 * 5e-3); // Octave, M3
        assertEquals(8, byStatusSpecies.size());
        assertTrue(t.toCsv().startsWith("t,S_humans_1,S_humans_2"));
    }

    @Test
    void substepsRefineTheIntegration() throws IOException {
        GroundModel m = model(1);
        double coarse = new RungeKuttaSolver(1).solve(m, 0, 550, 1).get(0).last()[2];
        double fine = new RungeKuttaSolver(8).solve(m, 0, 550, 1).get(0).last()[2];
        assertEquals(22.46, fine, 22.46 * 2e-3);
        assertEquals(fine, coarse, fine * 2e-3);
    }

    @Test
    void gillespieRunsAreIntegerConservativeAndCloseToTheOdeOnAverage() {
        GroundModel m = Kendrick.load(SIR);
        List<Trajectory> runs = Kendrick.run(m);
        assertEquals(20, runs.size());
        double meanR60 = 0;
        for (Trajectory t : runs) {
            for (double[] state : t.states) {
                double total = 0;
                for (double v : state) {
                    assertEquals(v, Math.rint(v), 0.0);
                    assertTrue(v >= 0);
                    total += v;
                }
                assertEquals(1000.0, total, 0.0);
            }
            meanR60 += t.last()[2];
        }
        meanR60 /= runs.size();
        double odeR60 = new RungeKuttaSolver(4).solve(m, 0, 60, 1).get(0).last()[2];
        // R0 = 2: the mean final size of 20 stochastic epidemics is within 5% of the ODE's
        assertEquals(odeR60, meanR60, odeR60 * 0.05);
        // same seed, same trajectory
        Trajectory again = new GillespieSolver(1, 7).solve(m, 0, 60, 1).get(0);
        assertEquals(runs.get(0).states[45][1], again.states[45][1], 0.0);
    }

    @Test
    void tauLeapIsCloseToGillespieOnAverage() {
        GroundModel m = Kendrick.load(SIR);
        double odeR60 = new RungeKuttaSolver(4).solve(m, 0, 60, 1).get(0).last()[2];
        double mean = 0;
        List<Trajectory> runs = new TauLeapSolver(20, 3, 4).solve(m, 0, 60, 1);
        for (Trajectory t : runs) mean += t.last()[2];
        mean /= runs.size();
        assertEquals(odeR60, mean, odeR60 * 0.15);
    }

    @Test
    void concernsGivenByEquationsInFlowFormRunStochastically() throws IOException {
        GroundModel m = model(1); // SEIRS is given by ODEs, read as flows (with births and deaths)
        Trajectory t = new GillespieSolver(1, 0).solve(m, 0, 20, 1).get(0);
        double total = 0;
        for (double v : t.last()) total += v;
        assertTrue(total > 49000 && total < 51000, "births and deaths keep the population around 50000: " + total);
        for (double v : t.last()) assertTrue(v >= 0 && v == Math.rint(v));
    }

    @Test
    void stochasticSolversRefuseEquationsThatAreNotFlows() {
        String script = "KModel A attribute: #(status in (S I)); parameters: #(beta);\n"
                + "    equations: #(S:t=0 - beta*I. I:t=beta*I).\n"
                + "Composition C model: 'A'.\nScenario Sc on: 'C'; beta: 1; S: 10; I: 1.\n";
        GroundModel m = Kendrick.load(script);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new GillespieSolver(1, 0).solve(m, 0, 10, 1));
        assertTrue(ex.getMessage().contains("A"), ex.getMessage());
        assertTrue(ex.getMessage().contains("flow"), ex.getMessage());
    }

    @Test
    void simulationSettingsAreValidated() {
        String bad = SIR.replace("to: 60", "to: 0");
        assertThrows(IllegalArgumentException.class, () -> Kendrick.run(Kendrick.load(bad)));
        String unknown = SIR.replace("'gillespie'", "'euler'");
        assertThrows(IllegalArgumentException.class, () -> Kendrick.run(Kendrick.load(unknown)));
    }
}
