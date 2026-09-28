package jKendrick.model;

import jKendrick.dsl.ast.Ast;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A ground model: a composed model whose parameters all have a value and whose compartments
 * all have an initial cardinality, i.e. a scenario, ready to be simulated.
 *
 * {@link #derivatives(double, double[])} computes the right-hand side of the ODEs of the
 * composed model, which is what the tensor sum produces automatically from the concerns:
 * for each compartment, the localised equation of every concern defined by ODEs, plus the
 * inflows and outflows of every transition of the composed automaton.
 */
public final class GroundModel {
    public final String name;
    public final ComposedModel model;
    public final Map<String, ParameterValue> parameters;
    /** Bound variables of the scenario: variable -> attribute. */
    public final Map<String, String> variables;
    private final double[] initialState;
    private final List<ComposedModel.Transition> transitions;
    /** Arguments of the Simulation entity (solver, from, to, step...), or empty. */
    public final Map<String, Ast.Literal> simulation;

    public GroundModel(String name, ComposedModel model, Map<String, ParameterValue> parameters,
                       Map<String, String> variables, double[] initialState, Map<String, Ast.Literal> simulation) {
        this.name = name;
        this.model = model;
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        this.variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
        this.initialState = initialState.clone();
        this.transitions = model.transitions();
        this.simulation = Collections.unmodifiableMap(new LinkedHashMap<>(simulation));
    }

    public double[] initialState() {
        return initialState.clone();
    }

    public double initialCardinality(Compartment c) {
        return initialState[model.indexOf(c)];
    }

    public int dimension() {
        return initialState.length;
    }

    public List<ComposedModel.Transition> transitions() {
        return transitions;
    }

    /** The attribute a bound variable ranges over, or null. */
    public String attributeOfVariable(String variable) {
        return variables.get(variable);
    }

    /** A context for evaluating an expression of {@code owner} localised to {@code current}. */
    public Context context(double t, double[] state, Compartment current, Concern owner) {
        return new Context(this, t, state, current, owner, new HashMap<>());
    }

    /** Value of a parameter at a compartment (declared indices resolved from the compartment). */
    public double parameter(String name, Compartment c, double t, double[] state) {
        ParameterValue v = parameters.get(name);
        if (v == null) throw new IllegalStateException("parameter " + name + " has no value");
        return v.at(Collections.emptyList(), context(t, state, c, model.concernOfParameter(name)));
    }

    /** Rate of a transition of the composed automaton in the given state. */
    public double rate(ComposedModel.Transition tr, double t, double[] state) {
        return Evaluator.eval(tr.local.rate, context(t, state, tr.from, tr.concern));
    }

    /** dy/dt for the composed model. */
    public double[] derivatives(double t, double[] y) {
        double[] dy = new double[y.length];
        for (Concern k : model.concerns) {
            if (!k.hasEquations()) continue;
            for (Compartment c : model.compartments) {
                Ast.Expr rhs = k.equations.get(c.get(k.attribute.name));
                if (rhs == null) continue; // no equation for this value: the concern leaves it unchanged
                dy[model.indexOf(c)] += Evaluator.eval(rhs, context(t, y, c, k));
            }
        }
        for (ComposedModel.Transition tr : transitions) {
            int from = model.indexOf(tr.from), to = model.indexOf(tr.to);
            double flow = rate(tr, t, y) * y[from];
            dy[from] -= flow;
            dy[to] += flow;
        }
        return dy;
    }
}
