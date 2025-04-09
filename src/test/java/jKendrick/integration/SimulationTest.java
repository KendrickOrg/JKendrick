package jKendrick.integration;

import jKendrick.core.Model;
import jKendrick.core.Scenario;
import jKendrick.scenarios.SIRScenario;
import jKendrick.solvers.RKSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimulationTest {

    @Test
    void testSimulationRunsWithoutCrash() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        RKSolver solver = new RKSolver(scenario, 0.1, 10.0);
        Visualization dummyViz = new Visualization();

        assertDoesNotThrow(() -> {
            Simulation sim = new Simulation(solver, dummyViz, "SIR - Test Run");
            sim.simulate();
        });
    }

    @Test
    void testSimulationStepConsistency() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        RKSolver solver = new RKSolver(scenario, 0.1, 10.0);
        Simulation sim = new Simulation(solver, new Visualization(), "Test");

        sim.simulate();

        double[][] result = solver.getResult();
        assertEquals(101, result.length); // from 0.0 to 10.0 with step 0.1
    }
}
