package jKendrick.simulation;

import jKendrick.Kendrick;
import jKendrick.dsl.ast.Ast;
import jKendrick.dsl.semantics.SemanticException;
import jKendrick.model.GroundModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualizationSpecTest {

    @Test
    void theVisualizationEntityIsCollected() throws IOException {
        GroundModel m = SolversTest.model(3);
        assertEquals(1, m.visualizations.size());
        VisualizationSpec spec = Kendrick.visualizations(m).get(0);
        assertEquals("InfluenzaViz", spec.name);
        assertEquals("diagram", spec.plot);
        assertEquals("InfluenzaSim", spec.simulation);
        assertEquals(List.of("humans", "birds"), spec.legends);
        assertEquals("Infectious", spec.legendTitle);
        assertEquals("Time (days)", spec.xLabel);
        assertEquals(1, spec.data.size());
        assertEquals("I", spec.data.get(0).base);
        assertEquals(List.of("species"), spec.data.get(0).indices);
    }

    @Test
    void dataNamesSumOverTheAttributesNotListed() throws IOException {
        GroundModel m = SolversTest.model(3); // status x species x zone(5)
        Trajectory t = new RungeKuttaSolver(1).solve(m, 0, 10, 1).get(0);
        Map<String, double[]> series = Kendrick.visualizations(m).get(0).series(t);
        // legends rename the series I_humans, I_birds in order
        assertEquals(List.of("humans", "birds"), new ArrayList<>(series.keySet()));
        Map<String, double[]> expected = t.aggregate("status", "species");
        assertArrayEquals(expected.get("I_humans"), series.get("humans"), 1e-12);
        assertArrayEquals(expected.get("I_birds"), series.get("birds"), 1e-12);
        assertEquals(11, series.get("birds").length);

        // I alone: the total of the infectious; I_species_zone: one series per (species, zone)
        VisualizationSpec total = VisualizationSpec.of("V", args("data", array(id("I"))));
        double[] all = total.series(t).get("I");
        for (int i = 0; i < all.length; i++)
            assertEquals(expected.get("I_humans")[i] + expected.get("I_birds")[i], all[i], 1e-9);
        VisualizationSpec fine = VisualizationSpec.of("V", args("data", array(id("I_species_zone"))));
        Map<String, double[]> byZone = fine.series(t);
        assertEquals(10, byZone.size());
        assertTrue(byZone.containsKey("I_humans_1") && byZone.containsKey("I_birds_5"));
        assertArrayEquals(t.aggregate("status", "species", "zone").get("I_birds_3"), byZone.get("I_birds_3"), 1e-12);
    }

    @Test
    void withoutDataThereIsOneSeriesPerStatus() throws IOException {
        GroundModel m = SolversTest.model(1); // its Visualization has no data: argument
        Trajectory t = new RungeKuttaSolver(1).solve(m, 0, 5, 1).get(0);
        VisualizationSpec spec = Kendrick.visualizations(m).get(0);
        assertTrue(spec.data.isEmpty());
        assertEquals(List.of("S", "E", "I", "R"), new ArrayList<>(spec.series(t).keySet()));
        assertArrayEquals(t.aggregate("status").get("E"), spec.series(t).get("E"), 1e-12);

        GroundModel sir = Kendrick.load(SolversTest.SIR); // no Visualization entity at all: a default one
        Trajectory u = new RungeKuttaSolver(1).solve(sir, 0, 5, 1).get(0);
        List<VisualizationSpec> specs = Kendrick.visualizations(sir);
        assertEquals(1, specs.size());
        assertEquals("Time", specs.get(0).xLabel);
        assertEquals("Sc", specs.get(0).title);
        assertEquals(List.of("S", "I", "R"), new ArrayList<>(specs.get(0).series(u).keySet()));
    }

    @Test
    void visualizationsAreValidated() throws IOException {
        GroundModel m = SolversTest.model(3);
        Trajectory t = new RungeKuttaSolver(1).solve(m, 0, 2, 1).get(0);
        assertThrows(IllegalArgumentException.class, () -> VisualizationSpec.of("V", args("plot", str("histogram"))));
        assertThrows(IllegalArgumentException.class, () -> VisualizationSpec.of("V", args("data", str("I"))));
        assertThrows(IllegalArgumentException.class,
                () -> VisualizationSpec.of("V", args("data", array(id("Z_species")))).series(t));
        assertThrows(IllegalArgumentException.class,
                () -> VisualizationSpec.of("V", args("data", array(id("I_colour")))).series(t));
        // three legends for two series
        assertThrows(IllegalArgumentException.class, () -> VisualizationSpec.of("V",
                args("data", array(id("I_species")), "legends", array(str("a"), str("b"), str("c")))).series(t));
        // a Visualization for an unknown Simulation is rejected when the model is built
        String script = SolversTest.SIR + "Visualization V plot: 'diagram'; for: 'Nope'.\n";
        assertThrows(SemanticException.class, () -> Kendrick.load(script));
    }

    // ---- literal helpers

    private static Map<String, Ast.Literal> args(Object... kv) {
        Map<String, Ast.Literal> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (Ast.Literal) kv[i + 1]);
        return m;
    }

    private static Ast.Literal str(String s) { return Ast.Literal.string(s); }

    private static Ast.Literal id(String s) { return Ast.Literal.identifier(s); }

    private static Ast.Literal array(Ast.Literal... elements) { return Ast.Literal.array(List.of(elements)); }
}
