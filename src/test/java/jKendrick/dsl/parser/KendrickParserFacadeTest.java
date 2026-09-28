package jKendrick.dsl.parser;

import jKendrick.dsl.ast.Ast;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parses the four models of the paper (copied from the paper repository,
 * {@code kendrick models/Model*.st}) and checks the shape of the resulting AST.
 */
class KendrickParserFacadeTest {

    private static Ast.Script model(int n) throws IOException {
        String path = "/models/Model" + n + ".kendrick";
        try (InputStream in = KendrickParserFacadeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing test resource " + path);
            return KendrickParserFacade.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), path);
        }
    }

    @Test
    void model1_isOneConcernWithFourEquations() throws IOException {
        Ast.Script s = model(1);
        List<Ast.KModel> concerns = s.entitiesOf(Ast.KModel.class);
        assertEquals(1, concerns.size());
        Ast.KModel seirs = concerns.get(0);
        assertEquals("SEIRS", seirs.name);
        assertEquals("status", seirs.attribute.name);
        assertEquals(List.of("S", "E", "I", "R"), seirs.attribute.values);
        assertEquals(List.of("beta", "lambda", "gamma", "mu", "sigma", "nu"), seirs.parameters);
        assertEquals(4, seirs.equations.size());
        assertEquals("S", seirs.equations.get(0).compartment);
        assertTrue(seirs.parameterFormulas.containsKey("lambda"));

        Ast.Scenario sc = s.entitiesOf(Ast.Scenario.class).get(0);
        assertEquals("Influenza", sc.composition);
        assertTrue(sc.bindings.isEmpty());
        assertEquals(7, sc.assignments.size());
        Ast.ValueAssignment beta = (Ast.ValueAssignment) sc.assignments.get(1);
        assertEquals("beta", beta.target.base);
        assertEquals(0.24, beta.value.scalar.doubleValue());

        Ast.Simulation sim = s.entitiesOf(Ast.Simulation.class).get(0);
        assertEquals("rungeKutta", sim.arguments.get("solver").string);
        assertEquals(550.0, sim.arguments.get("to").number.doubleValue());
    }

    @Test
    void model3_hasRangeAttributeConnectionMatrixAndBoundVariables() throws IOException {
        Ast.Script s = model(3);
        List<Ast.KModel> concerns = s.entitiesOf(Ast.KModel.class);
        assertEquals(List.of("SEIRS", "MultiSpecies", "MetaPopulation"),
                List.of(concerns.get(0).name, concerns.get(1).name, concerns.get(2).name));

        Ast.KModel meta = concerns.get(2);
        assertEquals(List.of("1", "2", "3", "4", "5"), meta.attribute.values);
        assertEquals(5, meta.connectionMatrix.length);
        assertEquals(1.0, meta.connectionMatrix[4][0]);
        assertNotNull(meta.transitionRate);
        assertNull(concerns.get(1).connectionMatrix);

        assertEquals(List.of("SEIRS", "MultiSpecies", "MetaPopulation"), s.entitiesOf(Ast.Composition.class).get(0).models);

        Ast.Scenario sc = s.entitiesOf(Ast.Scenario.class).get(0);
        assertEquals(3, sc.bindings.size());
        assertEquals("k", sc.bindings.get(0).variable);
        assertEquals("species", sc.bindings.get(0).attribute);
        assertEquals("zone", sc.bindings.get(2).attribute);

        Ast.ValueAssignment beta = find(sc, "beta");
        assertEquals(List.of("ki"), beta.target.indices);
        assertEquals(2, beta.value.rank());
        assertEquals(0.012, beta.value.elements.get(0).elements.get(1).scalar.doubleValue());
    }

    @Test
    void model3_lambdaIsASumOverSpeciesOfAWholeProduct() throws IOException {
        Ast.Scenario sc = model(3).entitiesOf(Ast.Scenario.class).get(0);
        Ast.FormulaAssignment lambda = null;
        for (Ast.Assignment a : sc.assignments) if (a.target.base.equals("lambda")) lambda = (Ast.FormulaAssignment) a;
        assertNotNull(lambda);
        assertEquals(List.of("pk"), lambda.target.indices);

        // sum_ (i in species) beta_ki * I_ip / N_ip  ==  SUM_i ( (beta_ki * I_ip) / N_ip )
        assertTrue(lambda.formula instanceof Ast.Sum);
        Ast.Sum sum = (Ast.Sum) lambda.formula;
        assertEquals("i", sum.variable);
        assertEquals("species", sum.attribute);
        assertTrue(sum.body instanceof Ast.Binary);
        Ast.Binary div = (Ast.Binary) sum.body;
        assertEquals('/', div.op);
        assertEquals("N", ((Ast.Name) div.right).name.base);
        assertEquals(List.of("ip"), ((Ast.Name) div.right).name.indices);
    }

    @Test
    void model4_hasTransitionsAndMixedIndices() throws IOException {
        Ast.Script s = model(4);
        Ast.KModel isolation = s.entitiesOf(Ast.KModel.class).get(3);
        assertEquals("Isolation", isolation.name);
        assertEquals(2, isolation.transitions.size());
        assertEquals("free", isolation.transitions.get(0).from);
        assertEquals("isolated", isolation.transitions.get(0).to);
        assertEquals("delta", ((Ast.Name) isolation.transitions.get(0).rate).name.base);

        Ast.Scenario sc = s.entitiesOf(Ast.Scenario.class).get(0);
        Ast.ValueAssignment rho = find(sc, "rho");
        assertEquals(List.of("species", "isolation"), rho.target.indices);
        assertEquals(2, rho.value.rank());

        // I_ip_free : variables and an attribute value in the same name
        Ast.FormulaAssignment lambda = null;
        for (Ast.Assignment a : sc.assignments) if (a.target.base.equals("lambda")) lambda = (Ast.FormulaAssignment) a;
        Ast.Binary div = (Ast.Binary) ((Ast.Sum) lambda.formula).body;
        Ast.Name iFree = (Ast.Name) ((Ast.Binary) div.left).right;
        assertEquals("I", iFree.name.base);
        assertEquals(List.of("ip", "free"), iFree.name.indices);

        Ast.ValueAssignment init = find(sc, "S_birds_1_free");
        assertEquals(49990.0, init.value.scalar.doubleValue());
    }

    @Test
    void syntaxErrorsAreReportedWithPosition() {
        KendrickParserFacade.KendrickSyntaxException ex = assertThrows(
                KendrickParserFacade.KendrickSyntaxException.class,
                () -> KendrickParserFacade.parse("KModel X attribute: #(status in (S I)) parameters: #(a).", "bad"));
        assertEquals(1, ex.errors.size());
        assertTrue(ex.errors.get(0).startsWith("bad:1:40:"), ex.errors.get(0));
    }

    private static Ast.ValueAssignment find(Ast.Scenario sc, String target) {
        for (Ast.Assignment a : sc.assignments)
            if (a.target.toString().equals(target) || a.target.base.equals(target)) return (Ast.ValueAssignment) a;
        throw new AssertionError("no assignment " + target);
    }
}
