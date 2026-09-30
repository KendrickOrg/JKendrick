package jKendrick.dsl.semantics;

import jKendrick.dsl.ast.Ast;
import jKendrick.dsl.parser.KendrickParserFacade;
import jKendrick.model.Compartment;
import jKendrick.model.ComposedModel;
import jKendrick.model.Concern;
import jKendrick.model.Evaluator;
import jKendrick.model.GroundModel;
import jKendrick.model.ParameterValue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
        assertEquals(120, model(3).model.compartments.size());
        assertEquals(240, model(4).model.compartments.size());
        // lexicographic order, concerns in composition order
        ComposedModel m3 = model(3).model;
        assertEquals("S_humans_1", m3.compartments.get(0).toString());
        assertEquals("S_humans_2", m3.compartments.get(1).toString());
        assertEquals("S_birds_1", m3.compartments.get(15).toString());
        assertEquals("E_humans_1", m3.compartments.get(30).toString());
    }

    @Test
    void initialCardinalitiesFollowTheSpreadingAndLastWinsRules() throws IOException {
        GroundModel g = model(3);
        // S_species: #(75000 750000) spread over the 15 zones, then S_birds_1: 49900 and I_birds_1: 100
        assertEquals(5000.0, g.initialCardinality(compartment("status", "S", "species", "humans", "zone", "3")));
        assertEquals(50000.0, g.initialCardinality(compartment("status", "S", "species", "birds", "zone", "2")));
        assertEquals(49900.0, g.initialCardinality(compartment("status", "S", "species", "birds", "zone", "1")));
        assertEquals(100.0, g.initialCardinality(compartment("status", "I", "species", "birds", "zone", "1")));
        assertEquals(0.0, g.initialCardinality(compartment("status", "E", "species", "birds", "zone", "1")));
        double total = 0;
        for (double v : g.initialState()) total += v;
        assertEquals(825000.0, total, 1e-9);
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
        // lambda_pk = sum_i beta_ki * I_ip / N_ip, with only 100 infectious birds in zone 1
        assertEquals(0.24 * 100 / 50000, g.parameter("lambda", birds1, 0, y), 1e-15);
        assertEquals(0.012 * 100 / 50000, g.parameter("lambda", humans1, 0, y), 1e-15);
        assertEquals(0.0, g.parameter("lambda", compartment("status", "S", "species", "birds", "zone", "2"), 0, y), 1e-15);
        assertTrue(g.parameters.get("beta") instanceof ParameterValue.Table);
        assertTrue(g.parameters.get("lambda") instanceof ParameterValue.Formula);
    }

    @Test
    void model1UsesTheConcernsDefaultFormulaForLambda() throws IOException {
        GroundModel g = model(1);
        double[] y = g.initialState();
        assertEquals(0.24 * 100 / 50000, g.parameter("lambda", compartment("status", "S"), 0, y), 1e-15);
    }

    @Test
    void transitionsOfTheComposedAutomatonAreReplicatedOverTheOtherConcerns() throws IOException {
        GroundModel g3 = model(3);
        // the ring: 15 local transitions x 2 species x 4 statuses = 120;
        // SEIRS read as flows: 9 local transitions (4 moves, 4 deaths, 1 birth) x 2 species x 15 zones = 270
        assertEquals(390, g3.transitions().size());
        GroundModel g4 = model(4);
        // ring: 15 x 2 x 4 x 2 isolation states = 240; isolation: 2 x 4 x 2 x 15 = 240; SEIRS: 9 x 2 x 15 x 2 = 540
        assertEquals(1020, g4.transitions().size());

        double[] y = g4.initialState();
        Compartment iFree = compartment("status", "I", "species", "birds", "zone", "1", "isolation", "free");
        Compartment rIsolated = compartment("status", "R", "species", "birds", "zone", "1", "isolation", "isolated");
        int seen = 0;
        for (ComposedModel.Transition tr : g4.transitions()) {
            if (tr.from == null) continue; // births
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
        assertEquals(0.24 * 100 / 50000, g.parameter("lambda", sBirds1, 0, y), 1e-15);
        // move the 100 infectious birds into isolation: lambda drops to 0, N - I_isolated = 49900
        int free = g.model.indexOf(compartment("status", "I", "species", "birds", "zone", "1", "isolation", "free"));
        int isolated = g.model.indexOf(compartment("status", "I", "species", "birds", "zone", "1", "isolation", "isolated"));
        y[free] = 0;
        y[isolated] = 100;
        assertEquals(0.0, g.parameter("lambda", sBirds1, 0, y), 1e-15);
    }

    @Test
    void derivativesConservePopulationAndMatchTheHandWrittenSEIRS() throws IOException {
        GroundModel g = model(1);
        double[] y = g.initialState(); // S=49900 E=0 I=100 R=0
        double[] dy = g.derivatives(0, y);
        double mu = 0.00137, beta = 0.24, gamma = 0.233, N = 50000, lambda = beta * 100 / N;
        assertEquals(mu * N - lambda * 49900 - mu * 49900, dy[0], 1e-9);
        assertEquals(lambda * 49900, dy[1], 1e-9);
        assertEquals(-gamma * 100 - mu * 100, dy[2], 1e-9);
        assertEquals(gamma * 100, dy[3], 1e-9);
        assertEquals(0.0, dy[0] + dy[1] + dy[2] + dy[3], 1e-9);
    }

    @Test
    void equationsInFlowFormAreReadAsTransitions() throws IOException {
        Concern seirs = model(1).model.concerns.get(0);
        assertTrue(seirs.hasEquations());
        assertTrue(!seirs.definedByOdesOnly());
        List<String> got = new ArrayList<>();
        for (Concern.LocalTransition t : seirs.transitions) got.add(t + " @ " + FlowExtractor.text(t.rate));
        assertEquals(List.of(
                "S --> E @ lambda", "S --> outside @ mu",
                "E --> I @ sigma", "E --> outside @ mu",
                "I --> R @ gamma", "I --> outside @ mu",
                "R --> outside @ mu", "R --> S @ nu",
                "outside --> S @ (mu*N)"), got);
    }

    @Test
    void theDerivedTransitionsGiveTheSameDerivativesAsTheEquations() throws IOException {
        GroundModel g = model(3);
        double[] y = g.initialState();
        for (int i = 0; i < y.length; i++) y[i] += 7 * (i % 3); // a generic state
        double[] viaTransitions = g.derivatives(0, y);
        // the same model with the SEIRS equations kept as ODEs (no transitions): evaluate them directly
        Concern seirs = g.model.concerns.get(0);
        Concern odeOnly = new Concern(seirs.name, seirs.attribute, seirs.parameters, seirs.parameterFormulas, seirs.equations, List.of());
        double[] viaEquations = new double[y.length];
        for (Compartment c : g.model.compartments)
            viaEquations[g.model.indexOf(c)] += Evaluator.eval(odeOnly.equations.get(c.get("status")), g.context(0, y, c, odeOnly));
        for (ComposedModel.Transition tr : g.transitions()) {
            if (tr.concern == seirs) continue; // the ring's flows, added on both sides
            double flow = g.propensity(tr, 0, y);
            viaEquations[g.model.indexOf(tr.from)] -= flow;
            viaEquations[g.model.indexOf(tr.to)] += flow;
        }
        for (int i = 0; i < y.length; i++) assertEquals(viaEquations[i], viaTransitions[i], 1e-9 * Math.max(1, Math.abs(viaEquations[i])));
    }

    @Test
    void equationsNotInFlowFormKeepTheOdeReadingOnly() {
        String script = "KModel A attribute: #(status in (S I)); parameters: #(beta);\n"
                + "    equations: #(S:t=0 - beta*I. I:t=beta*I).\n" // the outflow of S is not proportional to S
                + "Composition C model: 'A'.\nScenario Sc on: 'C'; beta: 1; S: 10; I: 1.\n";
        GroundModel g = ModelBuilder.build(KendrickParserFacade.parse(script));
        assertTrue(g.model.concerns.get(0).definedByOdesOnly());
        assertEquals(0, g.transitions().size());
        double[] dy = g.derivatives(0, g.initialState());
        assertEquals(-1.0, dy[0], 1e-12);
        assertEquals(1.0, dy[1], 1e-12);
    }

    @Test
    void emptyDenotesTheOutsideInTheTransitionSyntax() {
        String script = "KModel A attribute: #(status in (S I)); parameters: #(beta mu);\n"
                + "    transitions: #(S -- beta --> I. status -- mu --> Empty. Empty -- mu*N --> S).\n"
                + "Composition C model: 'A'.\nScenario Sc on: 'C'; beta: 0.5; mu: 0.1; S: 90; I: 10.\n";
        GroundModel g = ModelBuilder.build(KendrickParserFacade.parse(script));
        assertEquals(4, g.transitions().size()); // S->I, S->out, I->out, out->S
        double[] dy = g.derivatives(0, g.initialState());
        assertEquals(0.1 * 100 - 0.5 * 90 - 0.1 * 90, dy[0], 1e-9);
        assertEquals(0.5 * 90 - 0.1 * 10, dy[1], 1e-9);
    }

    @Test
    void topologiesDenoteTheirAdjacencyMatrices() throws IOException {
        String head = "KModel Z attribute: #(zone in (range 1 4)); parameters: #(rho); transitionRate: #(rho); topology: #(%s).\n"
                + "Composition C model: 'Z'.\nScenario Sc on: 'C'; rho: 1.\n";
        assertEquals(List.of("1>2", "2>3", "3>4", "4>1"), arcs(String.format(head, "ring oneWay")));
        assertEquals(List.of("1>2", "1>4", "2>1", "2>3", "3>2", "3>4", "4>1", "4>3"), arcs(String.format(head, "ring")));
        assertEquals(arcs(String.format(head, "ring")), arcs(String.format(head, "ring twoWay")));
        assertEquals(List.of("1>2", "2>3", "3>4"), arcs(String.format(head, "line oneWay")));
        assertEquals(List.of("1>2", "2>1", "2>3", "3>2", "3>4", "4>3"), arcs(String.format(head, "line")));
        assertEquals(12, arcs(String.format(head, "complete")).size());
        // the ring of Model 3 is the one the explicit matrix used to give
        assertEquals(15 * 8, model(3).transitions().stream().filter(t -> t.concern.name.equals("MetaPopulation")).count());
        SemanticException ex = assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(String.format(head, "torus"))));
        assertTrue(ex.getMessage().contains("unknown topology 'torus'"), ex.getMessage());
        ex = assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(String.format(head, "ring backwards"))));
        assertTrue(ex.getMessage().contains("oneWay or twoWay"), ex.getMessage());
        String both = head.replace("topology: #(%s)", "topology: #(ring); connectionMatrix: #(#(0 1 0 0) #(0 0 1 0) #(0 0 0 1) #(1 0 0 0))");
        ex = assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(both)));
        assertTrue(ex.getMessage().contains("exclusive"), ex.getMessage());
    }

    private static List<String> arcs(String script) throws SemanticException {
        GroundModel g = ModelBuilder.build(KendrickParserFacade.parse(script));
        List<String> arcs = new java.util.ArrayList<>();
        for (Concern.LocalTransition t : g.model.concerns.get(0).transitions) arcs.add(t.from + ">" + t.to);
        java.util.Collections.sort(arcs);
        return arcs;
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
    void semanticErrorsAreLocatedInTheScript() {
        // the faulty assignment is on line 4, column 22; the faulty transition on line 1, column 77
        String script = "KModel A attribute: #(status in (S I)); parameters: #(beta); transitions: #(S -- beta --> I).\n"
                + "Composition C model: 'A'.\n"
                + "Scenario Sc on: 'C';\n"
                + "    beta: 1;         gamma: 1.\n";
        SemanticException ex = assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(script)));
        assertTrue(ex.getMessage().endsWith("(line 4, column 22)"), ex.getMessage());
        String bad = script.replace("S -- beta --> I", "S -- beta --> J").replace("gamma: 1", "beta: 2");
        ex = assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(bad)));
        assertTrue(ex.getMessage().contains("transition to J"), ex.getMessage());
        assertTrue(ex.getMessage().endsWith("(line 1, column 77)"), ex.getMessage());
        // an error about the entity as a whole points at the entity
        String twice = script.replace("Composition C model: 'A'.", "KModel A attribute: #(x in (a b)).\nComposition C model: 'A'.");
        ex = assertThrows(SemanticException.class, () -> ModelBuilder.build(KendrickParserFacade.parse(twice)));
        assertTrue(ex.getMessage().endsWith("defined twice (line 2, column 1)"), ex.getMessage());
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
