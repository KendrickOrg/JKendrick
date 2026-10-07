package jKendrick.dsl.semantics;

import jKendrick.dsl.ast.Ast;
import jKendrick.model.Attribute;
import jKendrick.model.Compartment;
import jKendrick.model.ComposedModel;
import jKendrick.model.Concern;
import jKendrick.model.GroundModel;
import jKendrick.model.ParameterValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the meta-model objects denoted by a script: this is the semantics of Kendrick,
 * given by translation into the mathematical meta-model.
 *
 * <ol>
 *   <li>Each {@code KModel} denotes a {@link Concern}: its attribute is an equivalence relation
 *       whose classes are the attribute's values; its transitions (or its equations, or its
 *       connection matrix) give the transition rates between those classes; its parameters are
 *       the names it introduces. A concern may only mention its own parameters and values
 *       (structural independence of concerns).</li>
 *   <li>The {@code Composition} denotes the tensor sum of the concerns: a {@link ComposedModel}
 *       whose compartments are the product of the attributes' values.</li>
 *   <li>The {@code Scenario} denotes a {@link GroundModel}: it binds index variables to
 *       attributes, gives parameters constant, tabulated or functional values (the
 *       heterogeneities, which are the only place where concerns interact), and gives the
 *       compartments their initial cardinalities.</li>
 * </ol>
 *
 * Underscore rules of the scenario. The indices of a name are its underscore-separated tokens.
 * A token is an attribute name, an attribute value, or a run of bound single-letter variables.
 * For a value assignment {@code p_a_b: #(...)} the array has one dimension per attribute or
 * variable index, in that order, and the dimension's size is the attribute's number of values.
 * For a compartment assignment {@code S_species: #(...)}, {@code S_birds_1: 49990}, the given
 * amount is spread evenly over the compartments it designates; a later assignment overrides an
 * earlier one for the compartments it covers. A formula {@code lambda_pk: #(...)} is indexed by
 * variables only. {@code N} is never assigned: it denotes cardinalities.
 */
public final class ModelBuilder {

    /** The name of the outside in the transition syntax: {@code Empty -- mu --> S}, {@code status -- mu --> Empty}. */
    public static final String OUTSIDE = "Empty";

    private ModelBuilder() {}

    public static GroundModel build(Ast.Script script) {
        // ---- concerns
        Map<String, Concern> concerns = new LinkedHashMap<>();
        for (Ast.KModel k : script.entitiesOf(Ast.KModel.class)) {
            if (concerns.containsKey(k.name)) throw new SemanticException("KModel " + k.name + " is defined twice").at(k);
            try {
                concerns.put(k.name, concern(k));
            } catch (SemanticException e) {
                throw e.at(k);
            }
        }

        // ---- composition
        List<Ast.Composition> compositions = script.entitiesOf(Ast.Composition.class);
        if (compositions.size() != 1)
            throw new SemanticException("a script must contain exactly one Composition, found " + compositions.size());
        Ast.Composition comp = compositions.get(0);
        List<Concern> members = new ArrayList<>();
        for (String name : comp.models) {
            Concern c = concerns.get(name);
            if (c == null) throw new SemanticException("Composition " + comp.name + ": unknown KModel '" + name + "'").at(comp);
            if (members.contains(c)) throw new SemanticException("Composition " + comp.name + ": " + name + " is composed twice").at(comp);
            members.add(c);
        }
        ComposedModel model;
        try {
            model = new ComposedModel(comp.name, members);
        } catch (IllegalArgumentException e) {
            throw new SemanticException("Composition " + comp.name + ": " + e.getMessage()).at(comp);
        }

        // ---- scenario
        List<Ast.Scenario> scenarios = script.entitiesOf(Ast.Scenario.class);
        if (scenarios.size() != 1)
            throw new SemanticException("a script must contain exactly one Scenario, found " + scenarios.size());
        Ast.Scenario sc = scenarios.get(0);
        if (!sc.composition.equals(comp.name))
            throw new SemanticException("Scenario " + sc.name + " is on '" + sc.composition + "' but the Composition is " + comp.name).at(sc);

        Map<String, Ast.Literal> simulation = new LinkedHashMap<>();
        for (Ast.Simulation sim : script.entitiesOf(Ast.Simulation.class)) simulation.putAll(sim.arguments);
        Map<String, Map<String, Ast.Literal>> visualizations = new LinkedHashMap<>();
        for (Ast.Visualization viz : script.entitiesOf(Ast.Visualization.class)) {
            if (visualizations.put(viz.name, viz.arguments) != null)
                throw new SemanticException("Visualization " + viz.name + " is defined twice").at(viz);
            Ast.Literal target = viz.arguments.get("for");
            if (target != null && target.string != null && !script.entitiesOf(Ast.Simulation.class).isEmpty()) {
                boolean found = false;
                for (Ast.Simulation sim : script.entitiesOf(Ast.Simulation.class)) found |= sim.name.equals(target.string);
                if (!found) throw new SemanticException("Visualization " + viz.name + " is for unknown Simulation '" + target.string + "'").at(viz);
            }
        }

        try {
            return new ScenarioBuilder(model, sc).build(simulation, visualizations);
        } catch (SemanticException e) {
            throw e.at(sc);
        }
    }

    // ------------------------------------------------------------------ concerns

    /**
     * The adjacency matrix denoted by {@code topology: #(kind [direction])} over n values in declaration order:
     * {@code ring} (1 -> 2 -> ... -> n -> 1), {@code line} (1 -> 2 -> ... -> n) and {@code complete} (every
     * pair); two-way unless the direction is {@code oneWay}.
     */
    static double[][] topologyMatrix(Ast.Topology t, int n, String where) {
        if (t.direction != null && !t.direction.equals("oneWay") && !t.direction.equals("twoWay"))
            throw new SemanticException(where + ": topology direction must be oneWay or twoWay, not " + t.direction).at(t);
        double[][] m = new double[n][n];
        switch (t.kind) {
            case "ring":
                if (n < 2) throw new SemanticException(where + ": a ring needs at least two values").at(t);
                for (int i = 0; i < n; i++) m[i][(i + 1) % n] = 1;
                break;
            case "line":
                for (int i = 0; i + 1 < n; i++) m[i][i + 1] = 1;
                break;
            case "complete":
                for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) if (i != j) m[i][j] = 1;
                break;
            default:
                throw new SemanticException(where + ": unknown topology '" + t.kind + "' (ring, line, complete)").at(t);
        }
        if (!t.oneWay()) for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) if (m[i][j] == 1) m[j][i] = 1;
        return m;
    }

    private static Concern concern(Ast.KModel k) {
        String where = "KModel " + k.name;
        if (k.entry != null && k.attribute == null) throw new SemanticException(where + ": entry without attribute").at(k);
        if (k.entry != null && !k.attribute.values.contains(k.entry))
            throw new SemanticException(where + ": entry value " + k.entry + " is not a value of " + k.attribute.name).at(k);
        Attribute attribute = k.attribute == null ? null : new Attribute(k.attribute.name, k.attribute.values, k.entry);
        Set<String> values = attribute == null ? Collections.emptySet() : new HashSet<>(attribute.values);

        Map<String, Ast.Expr> equations = new LinkedHashMap<>();
        for (Ast.Equation e : k.equations) {
            if (!values.contains(e.compartment))
                throw new SemanticException(where + ": equation for " + e.compartment + ", which is not a value of its attribute").at(e);
            if (equations.put(e.compartment, e.rhs) != null)
                throw new SemanticException(where + ": two equations for " + e.compartment).at(e);
        }

        List<Concern.LocalTransition> transitions = new ArrayList<>();
        for (Ast.Transition t : k.transitions) {
            // 'Empty' is the outside (births and deaths); the attribute's name as a source means every value
            String to = t.to.equals(OUTSIDE) ? null : t.to;
            if (to != null && !values.contains(to))
                throw new SemanticException(where + ": transition to " + to + ", which is not a value of its attribute").at(t);
            List<String> froms = new ArrayList<>();
            if (t.from.equals(OUTSIDE)) froms.add(null);
            else if (attribute != null && t.from.equals(attribute.name)) froms.addAll(attribute.values);
            else if (values.contains(t.from)) froms.add(t.from);
            else throw new SemanticException(where + ": transition from " + t.from + ", which is not a value of its attribute").at(t);
            for (String from : froms) {
                if (from == null && to == null) throw new SemanticException(where + ": transition from Empty to Empty").at(t);
                transitions.add(new Concern.LocalTransition(from, t.rate, to));
            }
        }
        double[][] matrix = k.connectionMatrix;
        if (k.topology != null) {
            if (matrix != null) throw new SemanticException(where + ": connectionMatrix and topology are exclusive").at(k.topology);
            if (attribute == null) throw new SemanticException(where + ": topology without attribute").at(k.topology);
            matrix = topologyMatrix(k.topology, attribute.values.size(), where);
        }
        if (matrix != null) {
            String clause = k.topology != null ? "topology" : "connectionMatrix";
            if (attribute == null) throw new SemanticException(where + ": " + clause + " without attribute");
            if (k.transitionRate == null) throw new SemanticException(where + ": " + clause + " needs a transitionRate");
            int n = attribute.values.size();
            if (matrix.length != n)
                throw new SemanticException(where + ": connectionMatrix has " + matrix.length + " rows for " + n + " values");
            for (int i = 0; i < n; i++) {
                if (matrix[i].length != n)
                    throw new SemanticException(where + ": connectionMatrix row " + (i + 1) + " has " + matrix[i].length + " entries for " + n + " values");
                for (int j = 0; j < n; j++) {
                    double a = matrix[i][j];
                    if (a == 0) continue;
                    Ast.Expr rate = a == 1 ? k.transitionRate : new Ast.Binary('*', new Ast.Num(a), k.transitionRate);
                    transitions.add(new Concern.LocalTransition(attribute.values.get(i), rate, attribute.values.get(j)));
                }
            }
        }
        if (!equations.isEmpty() && !transitions.isEmpty())
            throw new SemanticException(where + ": a concern is defined either by equations or by transitions, not both");
        if (!equations.isEmpty()) {
            // Under the hypotheses of the meta-model, equations in flow form denote transitions.
            List<Concern.LocalTransition> derived = FlowExtractor.extract(equations);
            if (derived != null) transitions = derived;
        }

        Concern c = new Concern(k.name, attribute, k.parameters, k.parameterFormulas, equations, transitions);
        checkIndependence(c, where);
        return c;
    }

    /** A concern's expressions may only name its own parameters and values, N and t. */
    private static void checkIndependence(Concern c, String where) {
        Set<String> allowed = new HashSet<>(c.parameters);
        if (c.attribute != null) allowed.addAll(c.attribute.values);
        allowed.add("N");
        allowed.add("t");
        List<Ast.Expr> exprs = new ArrayList<>(c.equations.values());
        exprs.addAll(c.parameterFormulas.values());
        for (Concern.LocalTransition t : c.transitions) exprs.add(t.rate);
        for (Ast.Expr e : exprs) {
            for (Ast.Indexed n : names(e)) {
                if (!allowed.contains(n.base))
                    throw new SemanticException(where + " uses '" + n.base + "', which it does not declare: a concern must not depend on another one");
                if (n.hasIndices())
                    throw new SemanticException(where + " uses the indexed name '" + n + "': heterogeneities belong to scenarios");
            }
        }
    }

    private static List<Ast.Indexed> names(Ast.Expr e) {
        List<Ast.Indexed> out = new ArrayList<>();
        e.accept(new Ast.ExprVisitor<Void>() {
            @Override public Void visitNumber(Ast.Num x) { return null; }
            @Override public Void visitName(Ast.Name x) { out.add(x.name); return null; }
            @Override public Void visitBinary(Ast.Binary x) { x.left.accept(this); x.right.accept(this); return null; }
            @Override public Void visitNeg(Ast.Neg x) { x.operand.accept(this); return null; }
            @Override public Void visitCall(Ast.Call x) { for (Ast.Expr a : x.arguments) a.accept(this); return null; }
            @Override public Void visitSum(Ast.Sum x) { x.body.accept(this); return null; }
        });
        return out;
    }

    // ------------------------------------------------------------------ scenario

    private static final class ScenarioBuilder {
        private final ComposedModel model;
        private final Ast.Scenario sc;
        private final String where;
        private final Map<String, String> variables = new LinkedHashMap<>();
        private final Map<String, ParameterValue> parameters = new LinkedHashMap<>();
        private final double[] state;

        ScenarioBuilder(ComposedModel model, Ast.Scenario sc) {
            this.model = model;
            this.sc = sc;
            this.where = "Scenario " + sc.name;
            this.state = new double[model.compartments.size()];
        }

        GroundModel build(Map<String, Ast.Literal> simulation, Map<String, Map<String, Ast.Literal>> visualizations) {
            for (Ast.Binding b : sc.bindings) {
                if (b.variable.length() != 1)
                    throw new SemanticException(where + ": variable '" + b.variable + "' must be a single letter");
                if (model.attribute(b.attribute) == null)
                    throw new SemanticException(where + ": variable " + b.variable + " ranges over unknown attribute " + b.attribute);
                if (variables.put(b.variable, b.attribute) != null)
                    throw new SemanticException(where + ": variable " + b.variable + " is bound twice");
            }
            // concern defaults first, so that scenario assignments override them
            for (Concern c : model.concerns)
                for (Map.Entry<String, Ast.Expr> f : c.parameterFormulas.entrySet())
                    parameters.put(f.getKey(), new ParameterValue.Formula(Collections.emptyList(), f.getValue(), c));
            for (Ast.Assignment a : sc.assignments) {
                try {
                    assign(a);
                } catch (SemanticException e) {
                    throw e.at(a);
                }
            }
            checkFormulaCycles();
            return new GroundModel(sc.name, model, parameters, variables, state, simulation, visualizations);
        }

        /**
         * A parameter formula must not depend on itself, directly or through other formulas (the
         * default formulas of the concerns included), or its evaluation would not terminate.
         */
        private void checkFormulaCycles() {
            Map<String, List<String>> deps = new LinkedHashMap<>();
            for (Map.Entry<String, ParameterValue> e : parameters.entrySet()) {
                if (!(e.getValue() instanceof ParameterValue.Formula)) continue;
                List<String> d = new ArrayList<>();
                for (Ast.Indexed n : names(((ParameterValue.Formula) e.getValue()).expr))
                    if (parameters.get(n.base) instanceof ParameterValue.Formula && !d.contains(n.base)) d.add(n.base);
                deps.put(e.getKey(), d);
            }
            Set<String> done = new HashSet<>();
            for (String p : deps.keySet()) visit(p, deps, new ArrayList<>(), done);
        }

        private void visit(String p, Map<String, List<String>> deps, List<String> path, Set<String> done) {
            if (done.contains(p)) return;
            int i = path.indexOf(p);
            if (i >= 0) {
                List<String> cycle = new ArrayList<>(path.subList(i, path.size()));
                cycle.add(p);
                throw new SemanticException(where + ": the formulas of " + String.join(", ", path.subList(i, path.size()))
                        + " depend on each other (" + String.join(" -> ", cycle) + ")");
            }
            path.add(p);
            for (String q : deps.get(p)) visit(q, deps, path, done);
            path.remove(path.size() - 1);
            done.add(p);
        }

        private void assign(Ast.Assignment a) {
            String base = a.target.base;
            if (model.isParameter(base)) {
                assignParameter(a);
            } else if (model.attributeOfValue(base) != null) {
                if (!(a instanceof Ast.ValueAssignment))
                    throw new SemanticException(where + ": compartment " + a.target + " cannot be given a formula");
                assignCompartment((Ast.ValueAssignment) a);
            } else if (base.equals("N")) {
                throw new SemanticException(where + ": N is not assignable, it denotes the cardinality of the population");
            } else {
                throw new SemanticException(where + ": '" + base + "' is neither a parameter nor a compartment of " + model.name);
            }
        }

        // ---- parameters

        private void assignParameter(Ast.Assignment a) {
            List<ParameterValue.IndexSpec> specs = new ArrayList<>();
            for (String token : a.target.indices) {
                if (model.attribute(token) != null) {
                    specs.add(new ParameterValue.IndexSpec(token, null));
                } else if (isVariableRun(token)) {
                    for (char ch : token.toCharArray()) {
                        String v = String.valueOf(ch);
                        specs.add(new ParameterValue.IndexSpec(variables.get(v), v));
                    }
                } else if (model.attributeOfValue(token) != null) {
                    throw new SemanticException(where + ": " + a.target + ": a parameter cannot be indexed by the value '" + token
                            + "'; index it by the attribute " + model.attributeOfValue(token).name + " and give one value per attribute value");
                } else {
                    throw new SemanticException(where + ": " + a.target + ": '" + token + "' is neither an attribute nor bound variables");
                }
            }
            if (a instanceof Ast.FormulaAssignment) {
                for (ParameterValue.IndexSpec s : specs)
                    if (s.variable == null)
                        throw new SemanticException(where + ": " + a.target + ": a formula is indexed by variables, not by attribute names");
                parameters.put(a.target.base, new ParameterValue.Formula(specs, ((Ast.FormulaAssignment) a).formula, null));
                return;
            }
            Ast.NumberArray value = ((Ast.ValueAssignment) a).value;
            if (specs.isEmpty()) {
                if (!value.isScalar())
                    throw new SemanticException(where + ": " + a.target + " is not indexed but is given an array");
                parameters.put(a.target.base, new ParameterValue.Constant(value.scalar));
                return;
            }
            List<Attribute> domains = new ArrayList<>();
            for (ParameterValue.IndexSpec s : specs) domains.add(model.attribute(s.attribute));
            checkShape(value, domains, 0, a.target.toString());
            parameters.put(a.target.base, new ParameterValue.Table(specs, domains, value));
        }

        private boolean isVariableRun(String token) {
            if (token.isEmpty()) return false;
            for (char ch : token.toCharArray()) if (!variables.containsKey(String.valueOf(ch))) return false;
            return true;
        }

        private void checkShape(Ast.NumberArray value, List<Attribute> domains, int depth, String target) {
            if (depth == domains.size()) {
                if (!value.isScalar()) throw new SemanticException(where + ": " + target + ": too many nesting levels for its indices");
                return;
            }
            Attribute d = domains.get(depth);
            if (value.isScalar())
                throw new SemanticException(where + ": " + target + ": expected " + d.values.size() + " values for " + d.name);
            if (value.elements.size() != d.values.size())
                throw new SemanticException(where + ": " + target + ": " + value.elements.size() + " values given for the "
                        + d.values.size() + " values of " + d.name);
            for (Ast.NumberArray sub : value.elements) checkShape(sub, domains, depth + 1, target);
        }

        // ---- compartments

        private void assignCompartment(Ast.ValueAssignment a) {
            Attribute own = model.attributeOfValue(a.target.base);
            Map<String, String> fixed = new LinkedHashMap<>();
            fixed.put(own.name, a.target.base);
            List<Attribute> dims = new ArrayList<>();
            for (String token : a.target.indices) {
                Attribute byName = model.attribute(token);
                Attribute byValue = model.attributeOfValue(token);
                if (byName != null) {
                    if (fixed.containsKey(token) || dims.contains(byName))
                        throw new SemanticException(where + ": " + a.target + " mentions " + token + " twice");
                    dims.add(byName);
                } else if (byValue != null) {
                    if (fixed.containsKey(byValue.name) || dims.contains(byValue))
                        throw new SemanticException(where + ": " + a.target + " mentions " + byValue.name + " twice");
                    fixed.put(byValue.name, token);
                } else {
                    throw new SemanticException(where + ": " + a.target + ": '" + token + "' is neither an attribute nor a value"
                            + " (variables are not allowed in compartment assignments)");
                }
            }
            checkShape(a.value, dims, 0, a.target.toString());
            spread(a.value, dims, 0, fixed, a.target.toString());
        }

        private void spread(Ast.NumberArray value, List<Attribute> dims, int depth, Map<String, String> constraints, String target) {
            if (depth < dims.size()) {
                Attribute d = dims.get(depth);
                for (int i = 0; i < d.values.size(); i++) {
                    Map<String, String> c = new LinkedHashMap<>(constraints);
                    c.put(d.name, d.values.get(i));
                    spread(value.elements.get(i), dims, depth + 1, c, target);
                }
                return;
            }
            List<Compartment> targets = model.matching(constraints);
            if (targets.isEmpty()) throw new SemanticException(where + ": " + target + " designates no compartment");
            double each = value.scalar / targets.size();
            for (Compartment c : targets) state[model.indexOf(c)] = each;
        }
    }
}
