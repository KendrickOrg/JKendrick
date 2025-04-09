package jKendrick.scenarios;

import jKendrick.core.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SISScenarioTest {

    @Test
    void testCompartmentsAndDerivatives() {
        Model model = new Model();
        model.setParameter("beta", 0.4);
        model.setParameter("gamma", 0.2);
        model.setParameter("population", 1000);

        double[] initial = {999, 1, 0};
        SISScenario scenario = new SISScenario(initial, model);

        assertEquals(2, scenario.getCompartments().size());
        assertEquals("S", scenario.getCompartments().get(0));
        assertEquals("I", scenario.getCompartments().get(1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> scenario.getCompartments().get(2));  // included for structural consistency

        double[] y = {999, 1, 0};
        double[] yDot = new double[3];
        scenario.computeDerivatives(0.0, y, yDot);

        assertTrue(yDot[0] < 0);   // S decreases
        assertTrue(yDot[1] > 0);   // I increases
        assertEquals(0.0, yDot[2], 1e-6); // R stays unused in SIS
    }

    @Test
    void testRateAndTransition() {
        Model model = new Model();
        model.setParameter("beta", 0.4);
        model.setParameter("gamma", 0.2);
        model.setParameter("population", 1000);

        double[] initial = {999, 1, 0};
        SISScenario scenario = new SISScenario(initial, model);

        double rateSI = scenario.getRate("S", "I");
        double rateIS = scenario.getRate("I", "S");

        assertEquals(0.4 * 999 * 1 / 1000, rateSI, 1e-6);
        assertEquals(0.2 * 1, rateIS, 1e-6);

        scenario.transition("I", "S"); // simulate recovery
        double[] newCard = scenario.getCardinalities();

        assertEquals(1000, newCard[0], 1e-6); // S increased
        assertEquals(0, newCard[1], 1e-6);    // I decreased
        assertEquals(0, newCard[2], 1e-6);    // R unused
    }
}
