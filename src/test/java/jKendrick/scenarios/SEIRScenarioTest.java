package jKendrick.scenarios;

import jKendrick.core.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SEIRScenarioTest {

    @Test
    void testCompartmentsAndDerivatives() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("sigma", 0.2);
        model.setParameter("population", 1000);

        double[] initial = {990, 5, 5, 0}; // S, E, I, R
        SEIRScenario scenario = new SEIRScenario(initial, model);

        assertEquals(4, scenario.getCompartments().size());
        assertEquals("S", scenario.getCompartments().get(0));
        assertEquals("E", scenario.getCompartments().get(1));
        assertEquals("I", scenario.getCompartments().get(2));
        assertEquals("R", scenario.getCompartments().get(3));

        double[] y = {990, 5, 5, 0};
        double[] yDot = new double[4];
        scenario.computeDerivatives(0.0, y, yDot);

        assertTrue(yDot[0] < 0);  // S should decrease
        assertTrue(yDot[1] > 0);  // E should increase then decrease
        assertTrue(yDot[2] > 0);  // I should increase then decrease
        assertTrue(yDot[3] > 0);  // R should increase
    }

    @Test
    void testTransitionAndRates() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("sigma", 0.2);
        model.setParameter("population", 1000);

        double[] initial = {990, 5, 5, 0}; // S, E, I, R
        SEIRScenario scenario = new SEIRScenario(initial, model);

        double rateSE = scenario.getRate("S", "E");
        double rateEI = scenario.getRate("E", "I");
        double rateIR = scenario.getRate("I", "R");

        assertEquals(0.3 * 990 * 5 / 1000, rateSE, 1e-6);
        assertEquals(0.2 * 5, rateEI, 1e-6);
        assertEquals(0.1 * 5, rateIR, 1e-6);

        scenario.transition("E", "I");
        double[] after = scenario.getCardinalities();

        assertEquals(5 - 1, after[1], 1e-6); // E decreased
        assertEquals(5 + 1, after[2], 1e-6); // I increased
        assertEquals(0, after[3], 1e-6);     // R unchanged
    }
}
