package jKendrick.model;

import jKendrick.dsl.ast.Ast;

import java.util.ArrayList;
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
 * the inflows and outflows of every transition of the composed automaton (including births
 * and deaths), plus, for a concern whose equations could not be read as flows, its localised
 * equation at each compartment.
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
    /** Arguments of the Visualization entities, by entity name (plot, data, legends...), or empty. */
    public final Map<String, Map<String, Ast.Literal>> visualizations;

    public GroundModel(String name, ComposedModel model, Map<String, ParameterValue> parameters,
                       Map<String, String> variables, double[] initialState, Map<String, Ast.Literal> simulation) {
        this(name, model, parameters, variables, initialState, simulation, Collections.emptyMap());
    }

    public GroundModel(String name, ComposedModel model, Map<String, ParameterValue> parameters,
                       Map<String, String> variables, double[] initialState, Map<String, Ast.Literal> simulation,
                       Map<String, Map<String, Ast.Literal>> visualizations) {
        this.name = name;
        this.model = model;
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        this.variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
        this.initialState = initialState.clone();
        this.transitions = model.transitions();
        this.simulation = Collections.unmodifiableMap(new LinkedHashMap<>(simulation));
        Map<String, Map<String, Ast.Literal>> v = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Ast.Literal>> e : visualizations.entrySet())
            v.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
        this.visualizations = Collections.unmodifiableMap(v);
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

    /**
     * The context of a transition's rate: at its source, or at its target for an inflow, where the
     * attributes with an entry value are left free in the cardinalities (the newcomers are born
     * from the whole population, not from the entry compartments only).
     */
    public Context context(double t, double[] state, ComposedModel.Transition tr) {
        if (!tr.isBirth()) return context(t, state, tr.from, tr.concern);
        return new Context(this, t, state, tr.to, tr.concern, new HashMap<>(), model.entryAttributes(tr.concern.attribute.name));
    }

    /** Value of a parameter at a compartment (declared indices resolved from the compartment). */
    public double parameter(String name, Compartment c, double t, double[] state) {
        ParameterValue v = parameters.get(name);
        if (v == null) throw new IllegalStateException("parameter " + name + " has no value");
        return v.at(Collections.emptyList(), context(t, state, c, model.concernOfParameter(name)));
    }

    /**
     * Rate of a transition of the composed automaton in the given state: per capita of the
     * source compartment, or absolute for a birth. Evaluated by the interpreter; the solvers use
     * the compiled rates ({@link #propensity(int, double, double[])}).
     */
    public double rate(ComposedModel.Transition tr, double t, double[] state) {
        return Evaluator.eval(tr.local.rate, context(t, state, tr));
    }

    /** Propensity of a transition: rate times the source cardinality, or the rate itself for a birth. */
    public double propensity(ComposedModel.Transition tr, double t, double[] state) {
        return propensity(rate(tr, t, state), tr, state);
    }

    private double propensity(double rate, ComposedModel.Transition tr, double[] state) {
        if (tr.isBirth()) return Math.max(0, rate);
        double from = state[model.indexOf(tr.from)];
        return from > 0 ? rate * from : 0;
    }

    // ------------------------------------------------------------------ compiled rates

    private Compiled[] compiledRates;
    private int[] sources;
    private int[] targets;
    private Compiled[] compiledOdes;
    private int[] odeTargets;
    /** For each compartment, the transitions whose propensity depends on its cardinality. */
    private int[][] dependents;
    /** The transitions whose rate depends on the time. */
    private int[] timeDependent;

    /** Compiles the rates of the transitions and the right-hand sides of the ODE-only concerns, once. */
    private synchronized void compile() {
        if (compiledRates != null) return;
        Compiled[] rates = new Compiled[transitions.size()];
        int[] from = new int[rates.length], to = new int[rates.length];
        List<List<Integer>> deps = new ArrayList<>();
        for (int c = 0; c < model.compartments.size(); c++) deps.add(new ArrayList<>());
        List<Integer> timed = new ArrayList<>();
        for (int i = 0; i < rates.length; i++) {
            ComposedModel.Transition tr = transitions.get(i);
            RateCompiler.Reads reads = new RateCompiler.Reads();
            rates[i] = RateCompiler.compile(tr.local.rate, context(0, null, tr), reads);
            from[i] = tr.from == null ? -1 : model.indexOf(tr.from);
            to[i] = tr.to == null ? -1 : model.indexOf(tr.to);
            if (from[i] >= 0) reads.state.add(from[i]); // the propensity is rate x source cardinality
            for (int c : reads.state) deps.get(c).add(i);
            if (reads.time) timed.add(i);
        }
        dependents = new int[deps.size()][];
        for (int c = 0; c < deps.size(); c++) dependents[c] = deps.get(c).stream().mapToInt(Integer::intValue).toArray();
        timeDependent = timed.stream().mapToInt(Integer::intValue).toArray();
        List<Compiled> odes = new ArrayList<>();
        List<Integer> odeIdx = new ArrayList<>();
        for (Concern k : model.concerns) {
            if (!k.definedByOdesOnly()) continue;
            for (Compartment c : model.compartments) {
                Ast.Expr rhs = k.equations.get(c.get(k.attribute.name));
                if (rhs == null) continue; // no equation for this value: the concern leaves it unchanged
                odes.add(RateCompiler.compile(rhs, context(0, null, c, k)));
                odeIdx.add(model.indexOf(c));
            }
        }
        compiledOdes = odes.toArray(new Compiled[0]);
        odeTargets = new int[odeIdx.size()];
        for (int i = 0; i < odeTargets.length; i++) odeTargets[i] = odeIdx.get(i);
        sources = from;
        targets = to;
        compiledRates = rates; // last, as the flag
    }

    /** Propensity of the i-th transition of {@link #transitions()}, from its compiled rate. */
    public double propensity(int i, double t, double[] state) {
        if (compiledRates == null) compile();
        double rate = compiledRates[i].eval(t, state);
        int from = sources[i];
        if (from < 0) return Math.max(0, rate);
        double n = state[from];
        return n > 0 ? rate * n : 0;
    }

    /** Propensities of all transitions, written into {@code a} (of length {@code transitions().size()}). */
    public void propensities(double t, double[] state, double[] a) {
        if (compiledRates == null) compile();
        for (int i = 0; i < a.length; i++) a[i] = propensity(i, t, state);
    }

    /** The transitions whose propensity may change when the cardinality of compartment {@code c} changes. */
    public int[] dependents(int c) {
        if (compiledRates == null) compile();
        return dependents[c];
    }

    /** The transitions whose rate reads the time (their propensity changes between events). */
    public int[] timeDependent() {
        if (compiledRates == null) compile();
        return timeDependent;
    }

    /** Index of the source compartment of the i-th transition, or -1 for a birth. */
    public int source(int i) {
        if (compiledRates == null) compile();
        return sources[i];
    }

    /** Index of the target compartment of the i-th transition, or -1 for a death. */
    public int target(int i) {
        if (compiledRates == null) compile();
        return targets[i];
    }

    /** dy/dt for the composed model. */
    public double[] derivatives(double t, double[] y) {
        if (compiledRates == null) compile();
        double[] dy = new double[y.length];
        for (int i = 0; i < compiledOdes.length; i++) dy[odeTargets[i]] += compiledOdes[i].eval(t, y);
        for (int i = 0; i < compiledRates.length; i++) {
            double flow = propensity(i, t, y);
            if (sources[i] >= 0) dy[sources[i]] -= flow;
            if (targets[i] >= 0) dy[targets[i]] += flow;
        }
        return dy;
    }
}
