package jKendrick.solvers;

import jKendrick.core.Model;
import jKendrick.core.Scenario;
import jKendrick.scenarios.SIRScenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GillespieTest {
    @Test
    void testMultiRunDimensions() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        int runs = 20;
        double step = 0.1;
        double end = 50.0;

        Gillespie solver = new Gillespie(scenario, step, end, runs);
        solver.solve();

        double[][][] results = solver.getMultiRunResult();
        assertEquals(runs, results.length);
        assertEquals((int) (end / step) + 1, results[0].length);
        assertEquals(3, results[0][0].length);
    }

    @Test
    void testNoNaNValues() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        Gillespie solver = new Gillespie(scenario, 0.1, 20.0, 10);
        solver.solve();

        double[][][] results = solver.getMultiRunResult();

        for (double[][] run : results) {
            for (double[] step : run) {
                for (double val : step) {
                    assertFalse(Double.isNaN(val), "NaN found in result");
                }
            }
        }
    }

    @Test
    void testMedianPathIsNotNull() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        Gillespie solver = new Gillespie(scenario, 0.1, 10.0, 10);
        solver.solve();

        double[][] median = solver.getResult();
        assertNotNull(median);
        assertEquals((int) (10.0 / 0.1) + 1, median.length);
    }
}
