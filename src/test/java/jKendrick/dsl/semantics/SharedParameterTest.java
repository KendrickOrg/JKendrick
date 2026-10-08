package jKendrick.dsl.semantics;

import jKendrick.dsl.parser.KendrickParserFacade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A parameter belongs to one concern: two concerns declaring the same parameter name are not
 * silently merged, the composition is rejected. Two concerns that need the same value bind their
 * own parameters to it in the scenario.
 */
class SharedParameterTest {

    private static final String TWO_CONCERNS = String.join("\n",
            "KModel SIR",
            "    attribute: #(status in (S I R));",
            "    parameters: #(beta gamma mu);",
            "    transitions: #(S -- beta*I/N --> I. I -- gamma --> R. status -- mu --> Empty. Empty -- mu*N --> S.).",
            "KModel Species",
            "    attribute: #(species in (humans birds));",
            "    parameters: #(mu).",
            "Composition M model: 'SIR'; model: 'Species'.",
            "Scenario Scr on: 'M'; beta: 0.5; gamma: 0.1; mu: 0.01; S: 99; I: 1.",
            "Simulation Sim solver: 'rungeKutta'; scenario: 'Scr'; from: 0; to: 10; step: 1.");

    @Test
    void aParameterDeclaredByTwoConcernsIsRejected() {
        SemanticException e = assertThrows(SemanticException.class,
                () -> ModelBuilder.build(KendrickParserFacade.parse(TWO_CONCERNS, "shared.kendrick")));
        assertTrue(e.getMessage().contains("mu") && e.getMessage().contains("SIR") && e.getMessage().contains("Species"),
                e.getMessage());
    }

    @Test
    void theSameValueIsSharedThroughTheScenario() {
        String script = TWO_CONCERNS.replace("parameters: #(mu).", "parameters: #(muS).")
                .replace("mu: 0.01;", "mu: 0.01; muS: #(mu);");
        ModelBuilder.build(KendrickParserFacade.parse(script, "shared-ok.kendrick"));
    }
}
