package jKendrick.dsl.semantics;

import jKendrick.dsl.ast.Ast;
import jKendrick.dsl.parser.KendrickParserFacade;
import jKendrick.model.Compartment;
import jKendrick.model.ComposedModel;
import jKendrick.model.GroundModel;
import jKendrick.model.ParameterValue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The meaning of the four models of the paper: state spaces, resolved values, transitions. */
class ModelBuilderTest {

    static GroundModel model(int n) throws IOException {
        String path = "/models/Model" + n + ".kendrick";
        try (InputStream in = ModelBuilderTest.class.getResourceAsStream(path)) {
            return ModelBuilder.build(KendrickParserFacade.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), path));
        }
    }

    static Compartment compartment(String... attributeValuePairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < attributeValuePairs.length; i += 2) m.put(attributeValuePairs[i], attributeValuePairs[i + 1]);
        return new Compartment(m);
    }

    @Test
    void stateSpacesAreTheProductsOfTheAttributes() throws IOException {
        assertEquals(4, model(1).model.compartments.size());
        assertEquals(8, model(2).model.compartments.size());
        assertEquals(40, model(3).model.compartments.size());
        assertEquals(80, model(4).model.compartments.size());
        // lexicographic order, concerns in composition order
        ComposedModel m3 = model(3).model;
        assertEquals("S_humans_1", m3.compartments.get(0).toString());
        assertEquals("S_humans_2", m3.compartments.get(1).toString());
        assertEquals("S_birds_1", m3.compartments.get(5).toString());
        assertEquals("E_humans_1", m3.compartments.get(10).toString());
    }

    @Test
    void initialCardinalitiesFollowTheSpreadingAndLastWinsRules() throws IOException {
        GroundModel g = model(3);
        // S_species: #(25000 250000) spread over the 5 zones, then S_birds_1: 49990 and I_birds_1: 10
        assertEquals(5000.0, g.initialCardinality(compartment("status", "S", "species", "humans", "zone", "3")));
        assertEquals(50000.0, g.initialCardinality(compartment("status", "S", "species", "birds", "zone", "2")));
        assertEquals(49990.0, g.initialCardinality(compartment("status", "S", "species", "birds", "zone", "1")));
        assertEquals(10.0, g.initialCardinality(compartment("status", "I", "species", "birds", "zone", "1")));
        assertEquals(0.0, g.initialCardinality(compartment("status", "E", "species", "birds", "zone", "1")));
        double total = 0;
        for (double v : g.initialState()) total += v;
        assertEquals(275000.0, total, 1e-9);
    }

    @Test
    void parametersAreResolvedFromTheCompartment() throws IOException {
        GroundModel g = model(3);
        double[] y = g.initialState();
        Compartment birds1 = compartment("status", "S", "species", "birds", "zone", "1");
        Compartment humans1 = compartment("status", "S", "species", "humans", "zone", "1");
        assertEquals(0.00137, g.parameter("mu", birds1, 0, y), 1e-12);
        assertEquals(0.0000365, g.parameter("mu", humans1, 0, y), 1e-12);
        assertEquals(0.1, g.parameter("rho", birds1, 0, y), 1e-12);
        // lambda_pk = sum_i beta_ki * I_ip / N_ip, with only 10 infectious birds in zone 1
        assertEquals(0.24 * 10 / 50000, g.parameter("lambda", birds1, 0, y), 1e-15);
        assertEquals(0.012 * 10 / 50000, g.parameter("lambda", humans1, 0, y), 1e-15);
        assertEquals(0.0, g.parameter("lambda", compartment("status", "S", "species", "birds", "zone", "2"), 0, y), 1e-15);
        assertTrue(g.parameters.get("beta") instanceof ParameterValue.Table);
        assertTrue(g.parameters.get("lambda") instanceof ParameterValue.Formula);
    }

    @Test
    void model1UsesTheConcernsDefaultFormulaForLambda() throws IOException {
        GroundModel g = model(1);
        double[] y = g.initialState();
        assertEquals(0.24 * 10 / 50000, g.parameter("lambda", compartment("status", "S"), 0, y), 1e-15);
    }

    @Test
    void transitionsOfTheComposedAutomatonAreReplicatedOverTheOtherConcerns() throws IOException {
        GroundModel g3 = model(3);
        // the ring: 5 local transitions x 2 species x 4 statuses
        assertEquals(40, g3.transitions().size());
        GroundModel g4 = model(4);
        // ring: 5 x 2 x 4 x 2 isolation states = 80; isolation: 2 local transitions x 4 x 2 x 5 = 80
        assertEquals(160, g4.transitions().size());

        double[] y = g4.initialState();
        Compartment iFree = compartment("status", "I", "species", "birds", "zone", "1", "isolation", "free");
        Compartment rIsolated = compartment("status", "R", "species", "birds", "zone", "1", "isolation", "isolated");
        int seen = 0;
        for (ComposedModel.Transition tr : g4.transitions()) {
            if (tr.from.equals(iFree) && tr.concern.name.equals("Isolation")) {
                assertEquals("I_birds_1_isolated", tr.to.toString());
                assertEquals(0.2, g4.rate(tr, 0, y), 1e-12);
                seen++;
            }
            if (tr.from.equals(iFree) && tr.concern.name.equals("MetaPopulation")) {
                assertEquals("I_birds_2_free", tr.to.toString());
                assertEquals(0.1, g4.rate(tr, 0, y), 1e-12); // rho_species_isolation for birds, free
                seen++;
            }
            if (tr.from.equals(rIsolated) && tr.concern.name.equals("MetaPopulation")) {
                assertEquals(0.0, g4.rate(tr, 0, y), 1e-12); // no migration while isolated
                seen++;
            }
            if (tr.from.equals(rIsolated) && tr.concern.name.equals("Isolation")) {
                assertEquals(1.0, g4.rate(tr, 0, y), 1e-12); // epsilon_status for R
                seen++;
            }
        }
        assertEquals(4, seen);
    }

    @Test
    void isolatedInfectiousAreExcludedFromTheForceOfInfection() throws IOException {
        GroundModel g = model(4);
        double[] y = g.initialState();
        Compartment sBirds1 = compartment("status", "S", "species", "birds", "zone", "1", "isolation", "free");
        assertEquals(0.24 * 10 / 50000, g.parameter("lambda", sBirds1, 0, y), 1e-15);
        // move the 10 infectious birds into isolation: lambda drops to 0, N - I_isolated = 49990
        int free = g.model.indexOf(compartment("status", "I", "species", "birds", "zone", "1", "isolation", "free"));
        int isolated = g.model.indexOf(compartment("status", "I", "species", "birds", "zone", "1", "isolation", "isolated"));
        y[free] = 0;
        y[isolated] = 10;
        assertEquals(0.0, g.parameter("lambda", sBirds1, 0, y), 1e-15);
    }

    @Test
    void derivativesConservePopulationAndMatchTheHandWrittenSEIRS() throws IOException {
        GroundModel g = model(1);
        double[] y = g.initialState(); // S=49990 E=0 I=10 R=0
        double[] dy = g.derivatives(0, y);
        double mu = 0.00137, beta = 0.24, gamma = 0.233, N = 50000, lambda = beta * 10 / N;
        assertEquals(mu * N - lambda * 49990 - mu * 49990, dy[0], 1e-9);
        assertEquals(lambda * 49990, dy[1], 1e-9);
        assertEquals(-gamma * 10 - mu * 10, dy[2], 1e-9);
        assertEquals(gamma * 10, dy[3], 1e-9);
        assertEquals(0.0, dy[0] + dy[1] + dy[2] + dy[3], 1e-9);
    }

    // ---- errors

    @Test
    void aConcernMayNotDependOnAnotherOne() {
        String script = "KModel A attribute: #(status in (S I)); parameters: #(beta); transitions: #(S -- beta*humans --> I).\n"
                + "Composition C model: 'A'.\nScenario Sc on: 'C'; beta: 1.\n";
        SemanticException ex = assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(script)));
        assertTrue(ex.getMessage().contains("humans"), ex.getMessage());
    }

    @Test
    void arrayShapesMustMatchTheAttributes() {
        String script = "KModel A attribute: #(status in (S I)); parameters: #(beta); transitions: #(S -- beta --> I).\n"
                + "KModel B attribute: #(species in (h b)).\n"
                + "Composition C model: 'A'; model: 'B'.\nScenario Sc on: 'C'; beta_species: #(1 2 3).\n";
        SemanticException ex = assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(script)));
        assertTrue(ex.getMessage().contains("3 values given for the 2 values of species"), ex.getMessage());
    }

    @Test
    void unknownTargetsAreReported() {
        String script = "KModel A attribute: #(status in (S I)); parameters: #(beta); transitions: #(S -- beta --> I).\n"
                + "Composition C model: 'A'.\nScenario Sc on: 'C'; gamma: 1.\n";
        SemanticException ex = assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(script)));
        assertTrue(ex.getMessage().contains("'gamma' is neither a parameter nor a compartment"), ex.getMessage());
    }

    @Test
    void theSameAttributeValueMayNotBelongToTwoConcerns() {
        String script = "KModel A attribute: #(status in (S I)).\nKModel B attribute: #(other in (I J)).\n"
                + "Composition C model: 'A'; model: 'B'.\nScenario Sc on: 'C'.\n";
        assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(script)));
    }

    @Test
    void scriptShapeIsChecked() {
        List<String> bad = List.of(
                "KModel A attribute: #(status in (S I)).\nScenario Sc on: 'C'.\n",             // no composition
                "KModel A attribute: #(status in (S I)).\nComposition C model: 'A'.\n",        // no scenario
                "KModel A attribute: #(status in (S I)).\nComposition C model: 'X'.\nScenario Sc on: 'C'.\n");
        for (String s : bad) assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(s)), s);
        Ast.Script ok = KendrickParserFacade.parse("KModel A attribute: #(status in (S I)).\nComposition C model: 'A'.\nScenario Sc on: 'C'.\n");
        assertEquals(2, ModelBuilder.build(ok).dimension());
    }
}
