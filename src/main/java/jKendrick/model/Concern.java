package jKendrick.model;

import jKendrick.dsl.ast.Ast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A concern: a (possibly incomplete) model, i.e. an attribute whose values are the states of
 * an automaton, the parameters the concern introduces, and its dynamics given either as
 * transitions between the attribute's values (the transition rate matrix Q) or as one ODE per
 * value (from which Q is not recovered; the equations are localised and summed at composition).
 *
 * A concern that only declares an attribute (e.g. {@code MultiSpecies}) has a zero Q: its
 * automaton never changes state on its own.
 */
public final class Concern {
    public final String name;
    /** Null when the concern has no attribute. */
    public final Attribute attribute;
    public final List<String> parameters;
    /** Default formulas given in the concern, e.g. {@code lambda: #(beta*I/N)}; scenarios may override them. */
    public final Map<String, Ast.Expr> parameterFormulas;
    /** One equation per attribute value, when the dynamics is given as ODEs. */
    public final Map<String, Ast.Expr> equations;
    /** Transitions between attribute values, when the dynamics is given as a SAN. */
    public final List<LocalTransition> transitions;

    public Concern(String name, Attribute attribute, List<String> parameters, Map<String, Ast.Expr> parameterFormulas,
                   Map<String, Ast.Expr> equations, List<LocalTransition> transitions) {
        this.name = name;
        this.attribute = attribute;
        this.parameters = Collections.unmodifiableList(parameters);
        this.parameterFormulas = Collections.unmodifiableMap(new LinkedHashMap<>(parameterFormulas));
        this.equations = Collections.unmodifiableMap(new LinkedHashMap<>(equations));
        this.transitions = Collections.unmodifiableList(transitions);
    }

    public boolean hasEquations() {
        return !equations.isEmpty();
    }

    @Override
    public String toString() {
        return "Concern " + name + (attribute == null ? "" : " (" + attribute + ")");
    }

    /** A transition {@code from -- rate --> to} between two values of the concern's attribute. */
    public static final class LocalTransition {
        public final String from;
        public final String to;
        public final Ast.Expr rate;

        public LocalTransition(String from, Ast.Expr rate, String to) {
            this.from = from;
            this.rate = rate;
            this.to = to;
        }

        @Override
        public String toString() {
            return from + " --> " + to;
        }
    }
}
