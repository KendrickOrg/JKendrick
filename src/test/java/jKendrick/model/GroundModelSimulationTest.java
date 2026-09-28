package jKendrick.model;

import jKendrick.dsl.parser.KendrickParserFacade;
import jKendrick.dsl.semantics.ModelBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integrates the composed models with a fixed-step RK4 and compares the totals at t = 550 days
 * with the results of the hand-written MATLAB versions of the same models (paper repository,
 * {@code matlab models/M1..M4}, run with Octave 8.4 and ode45). The MATLAB models write the
 * combined ODE system by hand; here it is produced by the tensor sum of the concerns.
 */
class GroundModelSimulationTest {

    private static final double RELATIVE_TOLERANCE = 5e-3; // ode45 (adaptive) vs RK4 with h = 1 day

    static GroundModel model(int n) throws IOException {
        String path = "/models/Model" + n + ".kendrick";
        try (InputStream in = GroundModelSimulationTest.class.getResourceAsStream(path)) {
            return ModelBuilder.build(KendrickParserFacade.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), path));
        }
    }

    /** Classical fixed-step Runge-Kutta 4. */
    static double[] rk4(GroundModel g, double t0, double t1, double h) {
        double[] y = g.initialState();
        int n = y.length;
        double[] tmp = new double[n];
        for (double t = t0; t < t1 - 1e-9; t += h) {
            double[] k1 = g.derivatives(t, y);
            for (int i = 0; i < n; i++) tmp[i] = y[i] + h / 2 * k1[i];
            double[] k2 = g.derivatives(t + h / 2, tmp);
            for (int i = 0; i < n; i++) tmp[i] = y[i] + h / 2 * k2[i];
            double[] k3 = g.derivatives(t + h / 2, tmp);
            for (int i = 0; i < n; i++) tmp[i] = y[i] + h * k3[i];
            double[] k4 = g.derivatives(t + h, tmp);
            for (int i = 0; i < n; i++) y[i] += h / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
        }
        return y;
    }

    /** Totals per status (and species when present) at the end of the run. */
    static Map<String, Double> totals(GroundModel g, double[] y) {
        Map<String, Double> t = new TreeMap<>();
        for (int i = 0; i < y.length; i++) {
            Compartment c = g.model.compartments.get(i);
            String key = c.get("status") + (c.get("species") == null ? "" : "_" + c.get("species"));
            t.merge(key, y[i], Double::sum);
        }
        return t;
    }

    static void assertClose(double expected, double actual, String what) {
        assertEquals(expected, actual, Math.abs(expected) * RELATIVE_TOLERANCE, what);
    }

    @Test
    void model1_matchesMatlabSEIRS() throws IOException {
        GroundModel g = model(1);
        Map<String, Double> t = totals(g, rk4(g, 0, 550, 1));
        // Octave, M1: final state [4.902e+04 7.858 22.46 946.7]
        assertClose(49020, t.get("S"), "S");
        assertClose(7.858, t.get("E"), "E");
        assertClose(22.46, t.get("I"), "I");
        assertClose(946.7, t.get("R"), "R");
    }

    @Test
    void model2_birdsBehaveAsInModel1AndHumansAreInfectedByBirds() throws IOException {
        GroundModel g = model(2);
        Map<String, Double> t = totals(g, rk4(g, 0, 550, 1));
        // Octave, M2: [S_h S_b E_h E_b I_h I_b R_h R_b] = [4994 4.902e+04 0.05377 7.864 0.1074 22.45 5.973 946.7]
        assertClose(4994, t.get("S_humans"), "S_humans");
        assertClose(49020, t.get("S_birds"), "S_birds");
        assertClose(0.05377, t.get("E_humans"), "E_humans");
        assertClose(7.864, t.get("E_birds"), "E_birds");
        assertClose(0.1074, t.get("I_humans"), "I_humans");
        assertClose(22.45, t.get("I_birds"), "I_birds");
        assertClose(5.973, t.get("R_humans"), "R_humans");
        assertClose(946.7, t.get("R_birds"), "R_birds");
    }

    @Test
    void model3_matchesTheHandWrittenMetapopulationSystem() throws IOException {
        GroundModel g = model(3);
        double[] y = rk4(g, 0, 550, 1);
        Map<String, Double> t = totals(g, y);
        // Octave, M3, per patch at t=550 (all five patches equal): S 4998 / 4.969e4, E 0.02224 / 3.298,
        // I 0.04399 / 9.321, R 1.805 / 298.5 (humans / birds)
        assertClose(5 * 4998, t.get("S_humans"), "S_humans");
        assertClose(5 * 49690, t.get("S_birds"), "S_birds");
        assertClose(5 * 0.02224, t.get("E_humans"), "E_humans");
        assertClose(5 * 3.298, t.get("E_birds"), "E_birds");
        assertClose(5 * 0.04399, t.get("I_humans"), "I_humans");
        assertClose(5 * 9.321, t.get("I_birds"), "I_birds");
        assertClose(5 * 1.805, t.get("R_humans"), "R_humans");
        assertClose(5 * 298.5, t.get("R_birds"), "R_birds");
        double total = 0;
        for (double v : y) total += v;
        assertEquals(275000, total, 1e-6);
    }

    @Test
    void model4_isolationExtinguishesTheEpidemicAsInMatlab() throws IOException {
        GroundModel g = model(4);
        double[] y = rk4(g, 0, 550, 1);
        Map<String, Double> t = totals(g, y);
        // Octave, M4: infectious ~1e-7 or less; R per patch 0.002776 (humans), 0.4804 (birds)
        assertEquals(0.0, t.get("I_birds"), 1e-6);
        assertEquals(0.0, t.get("I_humans"), 1e-6);
        assertClose(5 * 0.4804, t.get("R_birds"), "R_birds");
        assertClose(5 * 0.002776, t.get("R_humans"), "R_humans");
        double total = 0;
        for (double v : y) total += v;
        assertEquals(275000, total, 1e-6);
    }
}
