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
 * Integrates the composed models with a fixed-step RK4 and compares the totals at t = 1500 days
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
        Map<String, Double> t = totals(g, rk4(g, 0, 1500, 1));
        // Octave, M1 (beta = 0.5, R0 = 2.13): final state [23422.4 160.03 458.58 25959.0];
        // the wave peaks at 6629 infectious on day 37
        assertClose(23422.4, t.get("S"), "S");
        assertClose(160.03, t.get("E"), "E");
        assertClose(458.58, t.get("I"), "I");
        assertClose(25959.0, t.get("R"), "R");
    }

    @Test
    void model2_birdsBehaveAsInModel1AndHumansAreInfectedByBirds() throws IOException {
        GroundModel g = model(2);
        Map<String, Double> t = totals(g, rk4(g, 0, 1500, 1));
        // Octave, M2: [S_h S_b E_h E_b I_h I_b R_h R_b] = [4614.9 23422.4 2.1177 160.05 4.2431 458.55 378.69 25959]
        assertClose(4614.9, t.get("S_humans"), "S_humans");
        assertClose(23422.4, t.get("S_birds"), "S_birds");
        assertClose(2.1177, t.get("E_humans"), "E_humans");
        assertClose(160.05, t.get("E_birds"), "E_birds");
        assertClose(4.2431, t.get("I_humans"), "I_humans");
        assertClose(458.55, t.get("I_birds"), "I_birds");
        assertClose(378.69, t.get("R_humans"), "R_humans");
        assertClose(25959, t.get("R_birds"), "R_birds");
    }

    @Test
    void model3_matchesTheHandWrittenMetapopulationSystem() throws IOException {
        GroundModel g = model(3);
        double[] y = rk4(g, 0, 1500, 1);
        Map<String, Double> t = totals(g, y);
        // Octave, M3, totals over the 15 patches at t=1500 (humans / birds): S 69227.3 / 351444,
        // E 31.934 / 2414.5, I 63.955 / 6914.5, R 5676.8 / 389227; the wave peaks at 67200 infectious birds on day 60
        assertClose(69227.3, t.get("S_humans"), "S_humans");
        assertClose(351444, t.get("S_birds"), "S_birds");
        assertClose(31.934, t.get("E_humans"), "E_humans");
        assertClose(2414.5, t.get("E_birds"), "E_birds");
        assertClose(63.955, t.get("I_humans"), "I_humans");
        assertClose(6914.5, t.get("I_birds"), "I_birds");
        assertClose(5676.8, t.get("R_humans"), "R_humans");
        assertClose(389227, t.get("R_birds"), "R_birds");
        double total = 0;
        for (double v : y) total += v;
        assertEquals(825000, total, 1e-6);
    }

    @Test
    void model4_isolationDampensTheEpidemicAsInMatlab() throws IOException {
        GroundModel g = model(4);
        double[] y = rk4(g, 0, 1500, 1);
        Map<String, Double> t = totals(g, y);
        // Octave, M4 (free + isolated, humans / birds): S 74194.9 / 654902, E 4.017 / 528.4,
        // I 8.002 / 1504.1, R 793.1 / 93064.9; with delta = 0.2 the controlled reproduction number of
        // the birds is 1.15 and the wave peaks at 4330 infectious birds on day 159 instead of 67200.
        // The two models differ slightly: the MATLAB births are mu*N0 into the free susceptibles,
        // whereas the tensor sum replicates the SEIRS births over the isolation attribute, so that
        // the composed model also creates newborns in the isolated compartments (mu * isolated
        // population). Hence the looser tolerance (2% on the small compartments).
        assertEquals(74194.9, t.get("S_humans"), 74194.9 * 1e-3);
        assertEquals(654902, t.get("S_birds"), 654902 * 2e-3);
        assertEquals(1504.1, t.get("I_birds"), 1504.1 * 3e-2);
        assertEquals(8.002, t.get("I_humans"), 8.002 * 3e-2);
        assertEquals(93064.9, t.get("R_birds"), 93064.9 * 2e-2);
        assertEquals(793.1, t.get("R_humans"), 793.1 * 2e-2);
        double total = 0;
        for (double v : y) total += v;
        assertEquals(825000, total, 1e-6);
    }
}
