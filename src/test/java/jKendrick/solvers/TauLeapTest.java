package jKendrick.solvers;

import jKendrick.core.Model;
import jKendrick.core.Scenario;
import jKendrick.scenarios.SIRScenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TauLeapTest {

    @Test
    void testMultiRunDimensions() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        double step = 0.1;
        double end = 50.0;
        int nbRuns = 100;

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        TauLeap solver = new TauLeap(scenario, step, end, nbRuns);
        solver.solve();

        double[][][] results = solver.getMultiRunResult();
        assertEquals(nbRuns, results.length);
        assertEquals((int)(end / step) + 1, results[0].length);
        assertEquals(3, results[0][0].length);
    }

    @Test
    void testMedianPathNotNullAndCorrectSize() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        TauLeap solver = new TauLeap(scenario, 0.2, 40.0, 50);
        solver.solve();

        double[][] median = solver.getResult();
        assertNotNull(median);
        assertEquals((int)(40.0 / 0.2) + 1, median.length);
        assertEquals(3, median[0].length);
    }

    @Test
    void testNoNaNValuesInMultiRuns() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        TauLeap solver = new TauLeap(scenario, 0.1, 30.0, 20);
        solver.solve();

        for (double[][] run : solver.getMultiRunResult()) {
            for (double[] step : run) {
                for (double val : step) {
                    assertFalse(Double.isNaN(val), "TauLeap run contains NaN");
                }
            }
        }
    }
}
