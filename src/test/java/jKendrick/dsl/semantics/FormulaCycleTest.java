package jKendrick.dsl.semantics;

import jKendrick.dsl.parser.KendrickParserFacade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A parameter formula that depends on itself, directly or through another formula, is rejected. */
class FormulaCycleTest {

    private static final String SIR = String.join("\n",
            "KModel SIR",
            "    attribute: #(status in (S I R));",
            "    parameters: #(beta gamma lambda);",
            "    lambda: #(beta*I/N);",
            "    transitions: #(S -- lambda --> I. I -- gamma --> R.).",
            "Composition M model: 'SIR'.");

    private static SemanticException failure(String scenario) {
        String script = SIR + "\n" + scenario
                + "\nSimulation Sim solver: 'rungeKutta'; scenario: 'Scr'; from: 0; to: 10; step: 1.";
        return assertThrows(SemanticException.class,
                () -> ModelBuilder.build(KendrickParserFacade.parse(script, "cycle.kendrick")));
    }

    @Test
    void aFormulaThatDependsOnItselfIsRejected() {
        SemanticException e = failure("Scenario Scr on: 'M'; gamma: 0.1; beta: #(beta + 1); S: 99; I: 1.");
        assertTrue(e.getMessage().contains("depend on each other"), e.getMessage());
    }

    @Test
    void aCycleThroughTheDefaultFormulaOfAConcernIsRejected() {
        // lambda is beta*I/N in the concern; binding beta to a formula on lambda closes the cycle
        SemanticException e = failure("Scenario Scr on: 'M'; gamma: 0.1; beta: #(lambda*2); S: 99; I: 1.");
        assertTrue(e.getMessage().contains("lambda") && e.getMessage().contains("beta"), e.getMessage());
    }

    @Test
    void aFormulaOnAnotherFormulaWithoutCycleIsAccepted() {
        String script = SIR + "\nScenario Scr on: 'M'; gamma: 0.1; beta: #(gamma*3); S: 99; I: 1."
                + "\nSimulation Sim solver: 'rungeKutta'; scenario: 'Scr'; from: 0; to: 10; step: 1.";
        ModelBuilder.build(KendrickParserFacade.parse(script, "nocycle.kendrick"));
    }
}
