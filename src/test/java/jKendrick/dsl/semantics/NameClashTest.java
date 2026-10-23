package jKendrick.dsl.semantics;

import jKendrick.dsl.parser.KendrickParserFacade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The names of the concerns of a composition live in one namespace: an attribute name, an attribute
 * value and a parameter name are distinct across all the concerns and all three categories, and N and
 * t are reserved. Without this rule, a parameter I declared by one concern was read where another
 * concern writes beta*I/N, and an unchanged expression changed its meaning when a concern was added
 * (a referee's counterexample, October 2026): the composition is now rejected.
 */
class NameClashTest {

    static final String INFECTION = String.join("\n",
            "KModel Infection",
            "    attribute: #(status in (S I));",
            "    parameters: #(beta lambda);",
            "    lambda: #(beta*I/N);",
            "    transitions: #(S -- lambda --> I).");

    static String composed(String other, String scenario) {
        return INFECTION + "\n" + other + "\nComposition C model: 'Infection'; model: 'Other'.\n"
                + "Scenario Sc on: 'C'; beta: 1; " + scenario + " S: 100.\n"
                + "Simulation Sim solver: 'rungeKutta'; scenario: 'Sc'; from: 0; to: 1; step: 1.";
    }

    static void rejected(String script, String... words) {
        SemanticException e = assertThrows(SemanticException.class,
                () -> ModelBuilder.build(KendrickParserFacade.parse(script, "clash.kendrick")));
        for (String w : words) assertTrue(e.getMessage().contains(w), e.getMessage());
    }

    @Test
    void aParameterNamedLikeAValueOfAnotherConcernIsRejected() {
        rejected(composed("KModel Other attribute: #(flag in (present)); parameters: #(I).", "I: 7;"), "I", "Other", "Infection");
    }

    @Test
    void aValueNamedLikeAParameterOfAnotherConcernIsRejected() {
        rejected(composed("KModel Other attribute: #(flag in (beta)).", ""), "beta", "Other", "Infection");
    }

    @Test
    void aParameterNamedLikeAnAttributeOfAnotherConcernIsRejected() {
        rejected(composed("KModel Other attribute: #(flag in (present)); parameters: #(status).", "status: 1;"), "status", "Other");
    }

    @Test
    void theReservedNamesAreRejected() {
        // N and t are reserved words of the grammar, so the parser rejects them before the composition can
        for (String script : new String[] {
                composed("KModel Other attribute: #(flag in (present)); parameters: #(N).", "N: 1;"),
                composed("KModel Other attribute: #(flag in (t)).", "")}) {
            Exception e = assertThrows(Exception.class,
                    () -> ModelBuilder.build(KendrickParserFacade.parse(script, "reserved.kendrick")));
            assertTrue(e instanceof SemanticException || e instanceof KendrickParserFacade.KendrickSyntaxException, e.toString());
        }
    }

    @Test
    void distinctNamesAreAccepted() {
        ModelBuilder.build(KendrickParserFacade.parse(
                composed("KModel Other attribute: #(flag in (present)); parameters: #(k).", "k: 7;"), "ok.kendrick"));
    }
}
