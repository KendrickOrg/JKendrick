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
        assertTrue(solver.isStochastic(), "The solver should be stochastic.");
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

        // Vérifier que le chemin médian n'est pas nul
        assertNotNull(median, "Median path should not be null");
        assertEquals((int) (10.0 / 0.1) + 1, median.length, "Number of steps in the median path should match.");
       // assertEquals("[S, I, R]", solver.getLabels(), "Label should match expected scenario label.");
    }

    @Test
    void testStepSizeConsistency() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        double step = 0.1;
        double end = 10.0;
        int runs = 5;

        Gillespie solver = new Gillespie(scenario, step, end, runs);
        solver.solve();

        double[][][] results = solver.getMultiRunResult();

        // Vérifier que le nombre de pas est cohérent avec la taille du step
        for (int i = 0; i < runs; i++) {
            double[][] runResults = results[i];
            assertTrue(runResults.length <= (end / step) + 1, "Step size mismatch");
        }
    }

    @Test
    void testCorrectIndexing() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        Scenario scenario = new SIRScenario(new double[]{900, 100, 0}, model);
        Gillespie solver = new Gillespie(scenario, 0.1, 10.0, 10);
        solver.solve();

        double[][][] results = solver.getMultiRunResult();

        // Vérifier qu'aucun index ne dépasse les dimensions attendues
        for (double[][] run : results) {
            for (int t = 0; t < run.length; t++) {
                assertTrue(t < (10.0 / 0.1) + 1, "Index " + t + " exceeds the step size limit.");
            }
        }
    }

}
