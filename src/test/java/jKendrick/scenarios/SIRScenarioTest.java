package jKendrick.scenarios;

import jKendrick.core.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SIRScenarioTest {

    @Test
    void testCompartmentsAndDerivatives() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        double[] initial = new double[]{900, 100, 0};
        SIRScenario scenario = new SIRScenario(initial, model);

        assertEquals(3, scenario.getCompartments().size());
        assertEquals("S", scenario.getCompartments().get(0));
        assertEquals("I", scenario.getCompartments().get(1));
        assertEquals("R", scenario.getCompartments().get(2));

        double[] y = {900, 100, 0};
        double[] yDot = new double[3];
        scenario.computeDerivatives(0.0, y, yDot);

        assertTrue(yDot[0] < 0);  // dS/dt < 0
        assertTrue(yDot[1] > 0);  // dI/dt > 0
        assertEquals(10, yDot[2]);  // dR/dt = gamma * I = 0.1 * 100 = 10
    }

    @Test
    void testRateAndTransition() {
        Model model = new Model();
        model.setParameter("beta", 0.3);
        model.setParameter("gamma", 0.1);
        model.setParameter("population", 1000);

        double[] initial = new double[]{900, 100, 0};
        SIRScenario scenario = new SIRScenario(initial, model);

        double rateSI = scenario.getRate("S", "I");
        double rateIR = scenario.getRate("I", "R");

        assertEquals(27.0, rateSI, 1e-6); // 0.3 * 900 * 100 / 1000
        assertEquals(10.0, rateIR, 1e-6); // 0.1 * 100

        scenario.transition("S", "I");
        double[] newCard = scenario.getCardinalities();

        assertEquals(899, newCard[0], 1e-6);  // S decreased
        assertEquals(101, newCard[1], 1e-6);  // I increased
        assertEquals(0, newCard[2], 1e-6);    // R unchanged
    }
}
