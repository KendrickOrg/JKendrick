package jKendrick.solvers;

import jKendrick.core.Model;
import jKendrick.core.Scenario;
import jKendrick.scenarios.SIRScenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class RKSolverTest {

    @Test
    void testSolveProducesExpectedSize() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        RKSolver rk = new RKSolver(scenario, 0.1, 50.0);
        rk.solve();

        double[][] result = rk.getResult();
        assertEquals(501, result.length); // (50.0 / 0.1) + 1
        assertEquals(3, result[0].length);
    }

    @Test
    void testPopulationConservation() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        RKSolver rk = new RKSolver(scenario, 0.1, 50.0);
        rk.solve();

        for (double[] row : rk.getResult()) {
            double sum = row[0] + row[1] + row[2];
            assertEquals(1000.0, sum, 1e-3);
        }
    }

    @Test
    void testResultsAreNotNaN() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        RKSolver rk = new RKSolver(scenario, 0.1, 10.0);
        rk.solve();

        for (double[] row : rk.getResult()) {
            for (double val : row) {
                assertFalse(Double.isNaN(val), "Unexpected NaN in result");
            }
        }
    }
}
