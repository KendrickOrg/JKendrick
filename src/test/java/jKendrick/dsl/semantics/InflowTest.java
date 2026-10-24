package jKendrick.dsl.semantics;

import jKendrick.dsl.parser.KendrickParserFacade;
import jKendrick.dsl.semantics.SemanticException;
import jKendrick.model.GroundModel;
import org.junit.jupiter.api.Test;

import static jKendrick.dsl.semantics.ModelBuilderTest.compartment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The placement of inflows by the composition (Definition 4 of the paper, Appendix B.3), checked on the
 * derivatives at the initial state with unequal populations and heterogeneous inflows:
 * an inherited attribute replicates an inflow over its values, each localised to its own population;
 * an acquired attribute sends the whole inflow to its entry value, at a rate evaluated with the
 * attribute summed out.
 */
class InflowTest {

    private static final String SI = String.join("\n",
            "KModel SI",
            "    attribute: #(status in (S I));",
            "    parameters: #(beta mu iota);",
            "    transitions: #(",
            "        S -- beta*I/N --> I.",
            "        Empty -- mu*N --> S.",
            "        Empty -- iota --> S.",
            "        status -- mu --> Empty).");

    private static GroundModel build(String tail) {
        String script = SI + "\n" + tail + "\nSimulation Sim solver: 'rungeKutta'; scenario: 'Scr'; from: 0; to: 1; step: 1.";
        return ModelBuilder.build(KendrickParserFacade.parse(script, "inflow.kendrick"));
    }

    private static double derivative(GroundModel g, String... compartment) {
        double[] d = g.derivatives(0, g.initialState());
        return d[g.model.indexOf(compartment(compartment))];
    }

    @Test
    void anImmigrationRateIndexedByAnInheritedAttributeEntersEachValueAtItsOwnRate() {
        GroundModel g = build(String.join("\n",
                "KModel Zone attribute: #(zone in (1 2)).",
                "Composition M model: 'SI'; model: 'Zone'.",
                "Scenario Scr on: 'M'; beta: 0.5; mu: 0; iota_zone: #(10 0); S_zone: #(100 50); I: 0."));
        // no infection (I = 0) and no births or deaths (mu = 0): dS/dt is the immigration alone
        assertEquals(10.0, derivative(g, "status", "S", "zone", "1"), 1e-12);
        assertEquals(0.0, derivative(g, "status", "S", "zone", "2"), 1e-12);
    }

    @Test
    void birthsAreReplicatedOverAnInheritedAttributeInProportionToEachPopulation() {
        GroundModel g = build(String.join("\n",
                "KModel Zone attribute: #(zone in (1 2)).",
                "Composition M model: 'SI'; model: 'Zone'.",
                "Scenario Scr on: 'M'; beta: 0.5; mu: 0.1; iota: 0; S_zone: #(100 50); I: 0."));
        // zone 1: births mu*N_1 = 10, deaths mu*S_1 = 10; zone 2: 5 and 5. N is the local population.
        assertEquals(10.0 - 10.0, derivative(g, "status", "S", "zone", "1"), 1e-12);
        assertEquals(5.0 - 5.0, derivative(g, "status", "S", "zone", "2"), 1e-12);
        assertEquals(150.0, total(g), 1e-12);
    }

    @Test
    void birthsEnterTheEntryValueOfAnAcquiredAttributeAtTheRateOfTheWholePopulation() {
        GroundModel g = build(String.join("\n",
                "KModel Isolation",
                "    attribute: #(isolation in (free isolated));",
                "    entry: #(free).",
                "Composition M model: 'SI'; model: 'Isolation'.",
                "Scenario Scr on: 'M'; beta: 0.5; mu: 0.1; iota: 0; S_isolation: #(100 50); I: 0."));
        // all births (mu * (100 + 50) = 15) enter S_free; deaths are local: 10 among the free, 5 among the isolated
        assertEquals(15.0 - 10.0, derivative(g, "status", "S", "isolation", "free"), 1e-12);
        assertEquals(0.0 - 5.0, derivative(g, "status", "S", "isolation", "isolated"), 1e-12);
        assertTrue(g.model.warnings.isEmpty(), g.model.warnings.toString());
    }

    @Test
    void withoutAnEntryValueTheIsolatedWouldGetBirthsOfTheirOwn() {
        GroundModel g = build(String.join("\n",
                "KModel Isolation",
                "    attribute: #(isolation in (free isolated)).",
                "Composition M model: 'SI'; model: 'Isolation'.",
                "Scenario Scr on: 'M'; beta: 0.5; mu: 0.1; iota: 0; S_isolation: #(100 50); I: 0."));
        // the attribute is then inherited: the isolated population breeds isolated newborns, the error of Model 4
        assertEquals(10.0 - 10.0, derivative(g, "status", "S", "isolation", "free"), 1e-12);
        assertEquals(5.0 - 5.0, derivative(g, "status", "S", "isolation", "isolated"), 1e-12);
        assertEquals(1, g.model.warnings.size(), g.model.warnings.toString());
        assertTrue(g.model.warnings.get(0).contains("isolation") && g.model.warnings.get(0).contains("Isolation"),
                g.model.warnings.get(0));
    }

    @Test
    void anInheritedDeclarationKeepsTheInheritedDefaultAndGivesNoWarning() {
        // the same concern, declared inherited on purpose: the default is intended, so no warning
        GroundModel g = build(String.join("\n",
                "KModel Isolation",
                "    attribute: #(isolation in (free isolated)); inherited.",
                "Composition M model: 'SI'; model: 'Isolation'.",
                "Scenario Scr on: 'M'; beta: 0.5; mu: 0.1; iota: 0; S_isolation: #(100 50); I: 0."));
        assertEquals(10.0 - 10.0, derivative(g, "status", "S", "isolation", "free"), 1e-12);
        assertEquals(5.0 - 5.0, derivative(g, "status", "S", "isolation", "isolated"), 1e-12);
        assertTrue(g.model.warnings.isEmpty(), g.model.warnings.toString());
    }

    @Test
    void inheritedAndEntryTogetherAreRejected() {
        String message = rejection(String.join("\n",
                "KModel Isolation",
                "    attribute: #(isolation in (free isolated)); entry: #(free); inherited.",
                "Composition M model: 'SI'; model: 'Isolation'.",
                "Scenario Scr on: 'M'; beta: 0.5; mu: 0.1; iota: 0; S_isolation: #(100 50); I: 0."));
        assertTrue(message.contains("inherited"), message);
    }

    @Test
    void inheritedWithoutAnAttributeIsRejected() {
        String message = rejection(String.join("\n",
                "KModel Isolation",
                "    parameters: #(p); inherited.",
                "Composition M model: 'SI'; model: 'Isolation'.",
                "Scenario Scr on: 'M'; beta: 0.5; mu: 0.1; iota: 0; p: 1; I: 0."));
        assertTrue(message.contains("inherited"), message);
    }

    @Test
    void anInitialSizeSpreadOverAnAcquiredAttributeIsWarnedAbout() {
        // S: 150 does not say whether the initial susceptibles are free or isolated: it is spread over both
        // (75 and 75), which the entry value says is not where individuals start (referee, Oct 2026)
        GroundModel spread = build(String.join("\n",
                "KModel Isolation",
                "    attribute: #(isolation in (free isolated));",
                "    entry: #(free).",
                "Composition M model: 'SI'; model: 'Isolation'.",
                "Scenario Scr on: 'M'; beta: 0.5; mu: 0.1; iota: 0; S: 150; I: 0."));
        double[] y = spread.initialState();
        assertEquals(75.0, y[spread.model.indexOf(compartment("status", "S", "isolation", "free"))], 1e-12);
        assertEquals(75.0, y[spread.model.indexOf(compartment("status", "S", "isolation", "isolated"))], 1e-12);
        assertEquals(1, spread.warnings.size(), spread.warnings.toString());
        assertTrue(spread.warnings.get(0).contains("isolation") && spread.warnings.get(0).contains("free"),
                spread.warnings.get(0));
        // indexed with the entry value: no warning, and everyone starts free
        GroundModel placed = build(String.join("\n",
                "KModel Isolation",
                "    attribute: #(isolation in (free isolated));",
                "    entry: #(free).",
                "Composition M model: 'SI'; model: 'Isolation'.",
                "Scenario Scr on: 'M'; beta: 0.5; mu: 0.1; iota: 0; S_free: 150; I: 0."));
        assertTrue(placed.warnings.isEmpty(), placed.warnings.toString());
        assertEquals(150.0, placed.initialState()[placed.model.indexOf(
                compartment("status", "S", "isolation", "free"))], 1e-12);
    }

    private static String rejection(String script) {
        return assertThrows(SemanticException.class, () -> build(script)).getMessage();
    }

    private static double total(GroundModel g) {
        double t = 0;
        for (double v : g.initialState()) t += v;
        return t;
    }
}
