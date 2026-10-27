package jKendrick.dsl.semantics;

import jKendrick.dsl.parser.KendrickParserFacade;
import jKendrick.model.Compartment;
import jKendrick.model.GroundModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The renaming clause of a composition: {@code model: 'Zone' renaming: #((1 z1) (2 z2))} composes a
 * concern under other names for some of the names it declares, so that two concerns written
 * independently, with the same values or the same parameter names, compose without editing either
 * (Appendix B.3 of the paper). The scenario uses the names after renaming.
 */
class RenamingTest {

    static final String INFECTION = String.join("\n",
            "KModel Infection",
            "    attribute: #(status in (S I));",
            "    parameters: #(beta lambda mu);",
            "    lambda: #(beta*I/N);",
            "    transitions: #(S -- lambda --> I. Empty -- mu*N --> S. status -- mu --> Empty).");

    static final String TAIL = "\nSimulation Sim solver: 'rungeKutta'; scenario: 'Sc'; from: 0; to: 1; step: 1.";

    static GroundModel built(String script) {
        return ModelBuilder.build(KendrickParserFacade.parse(script, "renaming.kendrick"));
    }

    static List<String> compartmentNames(GroundModel g) {
        List<String> out = new ArrayList<>();
        for (Compartment c : g.model.compartments) out.add(c.toString());
        return out;
    }

    @Test
    void twoConcernsWithTheSameValuesComposeOnceOneIsRenamed() {
        String script = INFECTION
                + "\nKModel Age attribute: #(age in (1 2)); inherited."
                + "\nKModel Zone attribute: #(zone in (1 2)); inherited; parameters: #(rho); topology: #(complete); transitionRate: #(rho)."
                + "\nComposition C model: 'Infection'; model: 'Age'; model: 'Zone' renaming: #((1 z1) (2 z2))."
                + "\nScenario Sc on: 'C'; beta: 1; mu: 0; rho: 0.1; S_1_z1: 100; I_1_z1: 1." + TAIL;
        GroundModel g = built(script);
        List<String> names = compartmentNames(g);
        assertEquals(8, names.size());
        assertTrue(names.contains("S_1_z1"), names.toString());
        assertTrue(names.contains("I_2_z2"), names.toString());
    }

    @Test
    void aRenamedParameterIsBoundUnderItsNewNameAndReadInTheConcernsExpressions() {
        String script = INFECTION
                + "\nKModel Vaccination attribute: #(vac in (unvaccinated vaccinated)); parameters: #(mu); transitions: #(unvaccinated -- mu --> vaccinated)."
                + "\nComposition C model: 'Infection'; model: 'Vaccination' renaming: #((mu birth))."
                + "\nScenario Sc on: 'C'; beta: 1; mu: 0.01; birth: 0.02; S_unvaccinated: 100; I_unvaccinated: 1." + TAIL;
        GroundModel g = built(script);
        assertTrue(g.parameters.containsKey("birth"), g.parameters.keySet().toString());
        assertTrue(g.parameters.containsKey("mu"), g.parameters.keySet().toString());
    }

    @Test
    void withoutTheRenamingTheSameValuesAreRejected() {
        String script = INFECTION
                + "\nKModel Age attribute: #(age in (1 2)); inherited."
                + "\nKModel Zone attribute: #(zone in (1 2)); inherited."
                + "\nComposition C model: 'Infection'; model: 'Age'; model: 'Zone'."
                + "\nScenario Sc on: 'C'; beta: 1; mu: 0; S_1_1: 100." + TAIL;
        SemanticException e = assertThrows(SemanticException.class, () -> built(script));
        assertTrue(e.getMessage().contains("1"), e.getMessage());
    }

    @Test
    void renamingAnUndeclaredNameIsRejected() {
        String script = INFECTION
                + "\nKModel Zone attribute: #(zone in (1 2)); inherited."
                + "\nComposition C model: 'Infection'; model: 'Zone' renaming: #((3 z3))."
                + "\nScenario Sc on: 'C'; beta: 1; mu: 0; S_1: 100." + TAIL;
        SemanticException e = assertThrows(SemanticException.class, () -> built(script));
        assertTrue(e.getMessage().contains("declares no '3'"), e.getMessage());
    }

    @Test
    void aRenamingThatStillClashesIsRejected() {
        String script = INFECTION
                + "\nKModel Zone attribute: #(zone in (1 2)); inherited."
                + "\nComposition C model: 'Infection'; model: 'Zone' renaming: #((1 S))."
                + "\nScenario Sc on: 'C'; beta: 1; mu: 0; S_2: 100." + TAIL;
        SemanticException e = assertThrows(SemanticException.class, () -> built(script));
        assertTrue(e.getMessage().contains("S"), e.getMessage());
    }

    @Test
    void renamingToAReservedNameIsRejected() {
        String script = INFECTION
                + "\nKModel Zone attribute: #(zone in (a b)); inherited; parameters: #(rho); topology: #(complete); transitionRate: #(rho)."
                + "\nComposition C model: 'Infection'; model: 'Zone' renaming: #((rho N))."
                + "\nScenario Sc on: 'C'; beta: 1; mu: 0; S_a: 100." + TAIL;
        Exception e = assertThrows(Exception.class, () -> built(script));
        assertTrue(e instanceof SemanticException || e instanceof KendrickParserFacade.KendrickSyntaxException, e.toString());
    }
}
