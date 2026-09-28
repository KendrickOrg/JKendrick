package jKendrick.model;

import jKendrick.Kendrick;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The compiled rates must mean exactly what the interpreter ({@link Evaluator}) computes. */
class RateCompilerTest {

    private static double[] randomState(GroundModel g, Random random) {
        double[] y = g.initialState();
        for (int i = 0; i < y.length; i++) y[i] = Math.rint(y[i] * (0.5 + random.nextDouble()) + random.nextInt(50));
        return y;
    }

    @Test
    void compiledPropensitiesEqualInterpretedOnesOnEveryModel() throws IOException {
        Random random = new Random(3);
        for (int n = 1; n <= 4; n++) {
            GroundModel g = GroundModelSimulationTest.model(n);
            List<ComposedModel.Transition> trs = g.transitions();
            for (int trial = 0; trial < 5; trial++) {
                double[] y = randomState(g, random);
                double t = random.nextDouble() * 100;
                for (int i = 0; i < trs.size(); i++) {
                    double interpreted = g.propensity(trs.get(i), t, y);
                    double compiled = g.propensity(i, t, y);
                    assertEquals(interpreted, compiled, Math.abs(interpreted) * 1e-12 + 1e-12,
                            "model " + n + ", " + trs.get(i));
                }
            }
        }
    }

    @Test
    void derivativesOfOdeConcernsAreCompiledToo() {
        // an equation not in flow form (the concern keeps its ODE reading) and a time-dependent rate
        String script = "KModel A attribute: #(status in (S I)); parameters: #(beta);\n"
                + "    equations: #(S:t=0 - beta*I*sin(t). I:t=beta*I*sin(t)).\n"
                + "Composition C model: 'A'.\nScenario Sc on: 'C'; beta: 0.3; S: 10; I: 4.\n";
        GroundModel g = Kendrick.load(script);
        double[] dy = g.derivatives(2.0, new double[]{10, 4});
        assertEquals(-0.3 * 4 * Math.sin(2.0), dy[0], 1e-12);
        assertEquals(0.3 * 4 * Math.sin(2.0), dy[1], 1e-12);
    }

    @Test
    void dependenciesCoverEveryPropensityThatAnEventCanChange() throws IOException {
        Random random = new Random(11);
        for (int n = 1; n <= 4; n++) {
            GroundModel g = GroundModelSimulationTest.model(n);
            int m = g.transitions().size();
            double[] y = randomState(g, random);
            double[] a = new double[m];
            g.propensities(0.5, y, a);
            for (int k = 0; k < m; k++) {
                double[] z = y.clone();
                int src = g.source(k), dst = g.target(k);
                if (src >= 0) z[src] -= 1;
                if (dst >= 0) z[dst] += 1;
                double[] full = new double[m];
                g.propensities(0.5, z, full);
                double[] incremental = a.clone();
                if (src >= 0) for (int i : g.dependents(src)) incremental[i] = g.propensity(i, 0.5, z);
                if (dst >= 0) for (int i : g.dependents(dst)) incremental[i] = g.propensity(i, 0.5, z);
                for (int i = 0; i < m; i++)
                    assertEquals(full[i], incremental[i], Math.abs(full[i]) * 1e-12 + 1e-12,
                            "model " + n + ": firing " + g.transitions().get(k) + " misses " + g.transitions().get(i));
            }
            assertEquals(0, g.timeDependent().length, "the four models have no time-dependent rate");
        }
        String timed = "KModel SIR attribute: #(status in (S I R)); parameters: #(beta gamma lambda);\n"
                + "    lambda: #(beta*(1+cos(t))*I/N);\n"
                + "    transitions: #(S -- lambda --> I. I -- gamma --> R).\n"
                + "Composition C model: 'SIR'.\nScenario Sc on: 'C'; beta: 0.5; gamma: 0.25; S: 990; I: 10.\n";
        GroundModel g = Kendrick.load(timed);
        assertEquals(1, g.timeDependent().length);
        assertTrue(g.transitions().get(g.timeDependent()[0]).toString().startsWith("S --> I"));
    }
}
