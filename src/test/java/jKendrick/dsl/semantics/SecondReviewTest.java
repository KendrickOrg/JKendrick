package jKendrick.dsl.semantics;

import jKendrick.Kendrick;
import jKendrick.dsl.parser.KendrickParserFacade;
import jKendrick.model.Concern;
import jKendrick.simulation.Trajectory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Three defects found by a referee in October 2026, one test each.
 * <ul>
 *   <li>Replicate runs were seeded with consecutive seeds, whose first draws are nearly equal: the next
 *       reaction method then started every run with nearly the same clocks, and the probability that an
 *       SIR epidemic dies out at its first event came out 0.535 instead of 1/2.98 = 0.336.</li>
 *   <li>{@code S:t = -beta*S*I/N} was not read as flows, because unary minus binds tightest and a negated
 *       factor stopped the extraction.</li>
 *   <li>A concern could name another concern's attribute through a {@code sum_}.</li>
 * </ul>
 */
class SecondReviewTest {

    static final int RUNS = 2000;

    /** SIR with beta = 2, gamma = 1, 99 susceptibles and 1 infectious: the first event is a recovery with probability 1/2.98. */
    static String sir(String solver) {
        return String.join("\n",
                "KModel SIR",
                "    attribute: #(status in (S I R));",
                "    parameters: #(beta gamma);",
                "    transitions: #(S -- beta*I/N --> I. I -- gamma --> R).",
                "Composition C model: 'SIR'.",
                "Scenario Sc on: 'C'; beta: 2; gamma: 1; S: 99; I: 1.",
                "Simulation Sim solver: '" + solver + "'; scenario: 'Sc'; from: 0; to: 50; step: 50; runs: " + RUNS + "; seed: 0.");
    }

    static void assertExtinctionAtFirstEvent(String solver) {
        List<Trajectory> runs = Kendrick.run(Kendrick.load(sir(solver)));
        assertEquals(RUNS, runs.size());
        int noInfection = 0;
        for (Trajectory t : runs) {
            double[] last = t.states[t.states.length - 1];
            if (last[0] == 99) noInfection++; // S is the first compartment: nobody was ever infected
        }
        double p = 1 / 2.98, observed = (double) noInfection / RUNS, se = Math.sqrt(p * (1 - p) / RUNS);
        assertEquals(p, observed, 3 * se, solver + ": " + noInfection + " of " + RUNS + " runs without infection");
    }

    @Test
    void theNextReactionRunsAreIndependent() {
        assertExtinctionAtFirstEvent("nextReaction");
    }

    @Test
    void theGillespieRunsAreIndependent() {
        assertExtinctionAtFirstEvent("gillespie");
    }

    @Test
    void aNegatedFactorIsReadAsAnOutflow() {
        String script = String.join("\n",
                "KModel SIR",
                "    attribute: #(status in (S I R));",
                "    parameters: #(beta gamma);",
                "    equations: #(",
                "        S:t = -beta*S*I/N.",
                "        I:t = beta*S*I/N - gamma*I.",
                "        R:t = gamma*I.).",
                "Composition C model: 'SIR'.",
                "Scenario Sc on: 'C'; beta: 2; gamma: 1; S: 99; I: 1.",
                "Simulation Sim solver: 'gillespie'; scenario: 'Sc'; from: 0; to: 5; step: 1; runs: 2; seed: 1.");
        GroundModelHolder h = new GroundModelHolder(script);
        Concern sir = h.concern();
        assertFalse(sir.definedByOdesOnly(), "the equations should be read as flows");
        assertEquals(2, sir.transitions.size(), sir.transitions.toString());
        assertEquals(2, Kendrick.run(Kendrick.load(script)).size()); // a stochastic solver accepts it
    }

    @Test
    void aConcernCannotSumOverAnotherConcernsAttribute() {
        String script = String.join("\n",
                "KModel SIR attribute: #(status in (S I)); parameters: #(beta); transitions: #(S -- beta --> I).",
                "KModel Zone attribute: #(zone in (a b)); inherited; parameters: #(rho); topology: #(complete); transitionRate: #(sum_ (x in status) rho).",
                "Composition C model: 'SIR'; model: 'Zone'.",
                "Scenario Sc on: 'C'; beta: 1; rho: 0.1; S_a: 10.",
                "Simulation Sim solver: 'rungeKutta'; scenario: 'Sc'; from: 0; to: 1; step: 1.");
        SemanticException e = assertThrows(SemanticException.class,
                () -> ModelBuilder.build(KendrickParserFacade.parse(script, "sum.kendrick")));
        assertTrue(e.getMessage().contains("status"), e.getMessage());
    }

    /** The concern as the builder sees it, before composition. */
    static final class GroundModelHolder {
        final String script;
        GroundModelHolder(String script) { this.script = script; }
        Concern concern() {
            return ModelBuilder.build(KendrickParserFacade.parse(script, "flow.kendrick")).model.concerns.get(0);
        }
    }
}
