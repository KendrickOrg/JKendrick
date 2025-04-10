package jKendrick.scenarios;

import jKendrick.core.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SIRDScenarioTest {

    @Test
    void testCompartmentsAndDerivatives() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("mu", 0.05);
        model.setParameter("population", 1000);

        double[] initial = {999, 1, 0, 0};
        SIRDScenario scenario = new SIRDScenario(initial, model);

        assertEquals(4, scenario.getCompartments().size());
        assertEquals("S", scenario.getCompartments().get(0));
        assertEquals("I", scenario.getCompartments().get(1));
        assertEquals("R", scenario.getCompartments().get(2));
        assertEquals("D", scenario.getCompartments().get(3));

        double[] y = {999, 1, 0, 0};
        double[] yDot = new double[4];
        scenario.computeDerivatives(0.0, y, yDot);

        assertTrue(yDot[0] < 0); // S decrease
        assertTrue(yDot[2] > 0); // R increase
        assertTrue(yDot[3] > 0); // D increase
    }


    @Test
    void testTransitionAndRate() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("mu", 0.05);
        model.setParameter("population", 1000);

        double[] initial = {999, 1, 0, 0};
        SIRDScenario scenario = new SIRDScenario(initial, model);

        double rateInfect = scenario.getRate("S", "I");
        double rateRecover = scenario.getRate("I", "R");
        double rateDeath = scenario.getRate("I", "D");

        assertEquals(0.3 * 999 * 1 / 1000, rateInfect, 1e-6);
        assertEquals(0.1 * 1, rateRecover, 1e-6);
        assertEquals(0.05 * 1, rateDeath, 1e-6);

        scenario.transition("I", "R");
        double[] after = scenario.getCardinalities();

        assertEquals(999, after[0], 1e-6); // S unchanged
        assertEquals(0, after[1], 1e-6);   // I decreased
        assertEquals(1, after[2], 1e-6);   // R increased
        assertEquals(0, after[3], 1e-6);   // D unchanged
    }
}