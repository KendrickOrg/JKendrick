package jKendrick.dsl.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract syntax of a Kendrick script (new syntax, see SYNTAX.md in the paper repository).
 *
 * The AST is a faithful, resolved-free image of the text: names are kept as written,
 * underscored identifiers are split into a base name and its indices ({@link Indexed}),
 * but nothing is checked against the meta-model yet (that is the job of a later
 * semantic-analysis pass: attribute existence, bound variables, concern independence...).
 *
 * Plain final classes with public final fields are used (no records) to stay compatible
 * with the Java level configured in pom.xml.
 */
public final class Ast {
    private Ast() {}

    // ------------------------------------------------------------------ script

    /** A whole script: the entities in textual order. */
    public static final class Script {
        public final List<Entity> entities;
        public Script(List<Entity> entities) { this.entities = Collections.unmodifiableList(entities); }

        public <T extends Entity> List<T> entitiesOf(Class<T> kind) {
            List<T> result = new ArrayList<>();
            for (Entity e : entities) if (kind.isInstance(e)) result.add(kind.cast(e));
            return result;
        }
    }

    /** Common part of the five entity kinds. */
    /** A node that knows where it comes from in the script (1-based line and column, or 0 when unknown). */
    public abstract static class Node {
        public int line;
        public int column;

        /** Sets the position; returns this for chaining. */
        public Node at(int line, int column) {
            this.line = line;
            this.column = column;
            return this;
        }

        /** {@code " (line 12, column 5)"} or empty when the position is unknown. */
        public String position() {
            return line > 0 ? " (line " + line + ", column " + column + ")" : "";
        }
    }

    public abstract static class Entity extends Node {
        public final String name;
        protected Entity(String name) { this.name = name; }
    }

    // ------------------------------------------------------------------ KModel

    /** {@code KModel <name> ...}: a concern, i.e. a (possibly incomplete) model. */
    public static final class KModel extends Entity {
        /** The single attribute of the concern, or null when the concern has none. */
        public final Attribute attribute;
        public final List<String> parameters;
        /** Parameter formulas given in the concern, e.g. {@code lambda: #(beta*I/N)}. */
        public final Map<String, Expr> parameterFormulas;
        public final List<Equation> equations;
        public final List<Transition> transitions;
        /** Adjacency matrix of {@code connectionMatrix:}, or null. */
        public final double[][] connectionMatrix;
        /** Named graph of {@code topology:} ({@code ring}, {@code line}, {@code complete}), or null. */
        public final Topology topology;
        /** Rate of {@code transitionRate:}, or null. */
        public final Expr transitionRate;
        /** Value of {@code entry:}, taken by individuals entering the population (births, immigration), or null. */
        public final String entry;

        public KModel(String name, Attribute attribute, List<String> parameters,
                      Map<String, Expr> parameterFormulas, List<Equation> equations,
                      List<Transition> transitions, double[][] connectionMatrix, Topology topology, Expr transitionRate, String entry) {
            super(name);
            this.attribute = attribute;
            this.parameters = Collections.unmodifiableList(parameters);
            this.parameterFormulas = Collections.unmodifiableMap(new LinkedHashMap<>(parameterFormulas));
            this.equations = Collections.unmodifiableList(equations);
            this.transitions = Collections.unmodifiableList(transitions);
            this.connectionMatrix = connectionMatrix;
            this.topology = topology;
            this.entry = entry;
            this.transitionRate = transitionRate;
        }
    }

    /** {@code topology: #(ring oneWay)}: a named graph over the attribute's values ({@code ring}, {@code line},
     *  {@code complete}), with an optional direction ({@code oneWay} or {@code twoWay}, the default). */
    public static final class Topology extends Node {
        public final String kind;
        /** {@code oneWay}, {@code twoWay} or null (two-way). */
        public final String direction;
        public Topology(String kind, String direction) { this.kind = kind; this.direction = direction; }
        public boolean oneWay() { return "oneWay".equals(direction); }
    }

    /** {@code attribute: #(status in (S E I R))} or {@code #(zone in (range 1 5))}. */
    public static final class Attribute {
        public final String name;
        /** The values in declaration order; a range is expanded ("1", "2", ...). */
        public final List<String> values;
        public Attribute(String name, List<String> values) {
            this.name = name;
            this.values = Collections.unmodifiableList(values);
        }
    }

    /** {@code S:t = expr}. */
    public static final class Equation extends Node {
        public final String compartment;
        public final Expr rhs;
        public Equation(String compartment, Expr rhs) { this.compartment = compartment; this.rhs = rhs; }
    }

    /** {@code from -- rate --> to}. */
    public static final class Transition extends Node {
        public final String from;
        public final Expr rate;
        public final String to;
        public Transition(String from, Expr rate, String to) { this.from = from; this.rate = rate; this.to = to; }
    }

    // ------------------------------------------------------------------ Composition

    /** {@code Composition <name> model: 'A'; model: 'B'.} */
    public static final class Composition extends Entity {
        public final List<String> models;
        public Composition(String name, List<String> models) {
            super(name);
            this.models = Collections.unmodifiableList(models);
        }
    }

    // ------------------------------------------------------------------ Scenario

    /** {@code Scenario <name> on: 'Composition'; ...} */
    public static final class Scenario extends Entity {
        public final String composition;
        /** {@code var: #((k in species) (p in zone))}, in order. */
        public final List<Binding> bindings;
        public final List<Assignment> assignments;
        public Scenario(String name, String composition, List<Binding> bindings, List<Assignment> assignments) {
            super(name);
            this.composition = composition;
            this.bindings = Collections.unmodifiableList(bindings);
            this.assignments = Collections.unmodifiableList(assignments);
        }
    }

    /** {@code (k in species)}: an index variable bound to an attribute. */
    public static final class Binding {
        public final String variable;
        public final String attribute;
        public Binding(String variable, String attribute) { this.variable = variable; this.attribute = attribute; }
    }

    /** One assignment of a scenario: a value or a formula for an indexed name. */
    public abstract static class Assignment extends Node {
        /** The left-hand side, e.g. {@code beta_ki}, {@code S_birds_1}, {@code lambda_pk}. */
        public final Indexed target;
        protected Assignment(Indexed target) { this.target = target; }
    }

    /** {@code mu_species: #(0.0000365 0.00137)} or {@code nu: 0.00274}. */
    public static final class ValueAssignment extends Assignment {
        public final NumberArray value;
        public ValueAssignment(Indexed target, NumberArray value) { super(target); this.value = value; }
    }

    /** {@code lambda_pk: #(sum_ (i in species) beta_ki * I_ip / N_ip)}. */
    public static final class FormulaAssignment extends Assignment {
        public final Expr formula;
        public FormulaAssignment(Indexed target, Expr formula) { super(target); this.formula = formula; }
    }

    /** A scalar or a (nested) array of numbers, as written. */
    public static final class NumberArray {
        /** Non-null for a scalar. */
        public final Double scalar;
        /** Non-null for an array. */
        public final List<NumberArray> elements;
        public NumberArray(double scalar) { this.scalar = scalar; this.elements = null; }
        public NumberArray(List<NumberArray> elements) { this.scalar = null; this.elements = Collections.unmodifiableList(elements); }
        public boolean isScalar() { return scalar != null; }
        /** Number of nesting levels: 0 for a scalar, 1 for a vector, 2 for a matrix... */
        public int rank() { return isScalar() ? 0 : 1 + elements.get(0).rank(); }
    }

    // ------------------------------------------------------------------ Simulation / Visualization

    /** {@code Simulation <name> solver: 'rungeKutta'; from: 0; ...} */
    public static final class Simulation extends Entity {
        public final Map<String, Literal> arguments;
        public Simulation(String name, Map<String, Literal> arguments) {
            super(name);
            this.arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
        }
    }

    /** {@code Visualization <name> plot: 'diagram'; for: 'Sim'; ...} */
    public static final class Visualization extends Entity {
        public final Map<String, Literal> arguments;
        public Visualization(String name, Map<String, Literal> arguments) {
            super(name);
            this.arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
        }
    }

    /** A literal argument: number, string, identifier, or array of literals. */
    public static final class Literal {
        public final Double number;
        public final String string;
        public final String identifier;
        public final List<Literal> elements;
        private Literal(Double number, String string, String identifier, List<Literal> elements) {
            this.number = number; this.string = string; this.identifier = identifier;
            this.elements = elements == null ? null : Collections.unmodifiableList(elements);
        }
        public static Literal number(double d) { return new Literal(d, null, null, null); }
        public static Literal string(String s) { return new Literal(null, s, null, null); }
        public static Literal identifier(String id) { return new Literal(null, null, id, null); }
        public static Literal array(List<Literal> elements) { return new Literal(null, null, null, elements); }
    }

    // ------------------------------------------------------------------ names

    /**
     * An underscored name split at its underscores: {@code beta_ki} is base {@code beta}
     * with indices {@code ["ki"]}; {@code I_ip_free} is base {@code I} with indices
     * {@code ["ip", "free"]}. Each index is either a run of bound single-letter variables
     * (e.g. {@code ki}), an attribute name (e.g. {@code species}), or an attribute value
     * (e.g. {@code free}, {@code 1}); telling them apart needs the scenario's bindings and the
     * composed model, so it is left to the semantic layer.
     */
    public static final class Indexed {
        public final String base;
        public final List<String> indices;
        public Indexed(String base, List<String> indices) {
            this.base = base;
            this.indices = Collections.unmodifiableList(indices);
        }
        public static Indexed parse(String text) {
            String[] parts = text.split("_");
            List<String> idx = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) idx.add(parts[i]);
            return new Indexed(parts[0], idx);
        }
        public boolean hasIndices() { return !indices.isEmpty(); }
        @Override public String toString() {
            StringBuilder sb = new StringBuilder(base);
            for (String i : indices) sb.append('_').append(i);
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------ expressions

    /** Arithmetic expressions of rates and formulas. */
    public abstract static class Expr {
        public abstract <R> R accept(ExprVisitor<R> v);
    }

    public interface ExprVisitor<R> {
        R visitNumber(Num e);
        R visitName(Name e);
        R visitBinary(Binary e);
        R visitNeg(Neg e);
        R visitCall(Call e);
        R visitSum(Sum e);
    }

    public static final class Num extends Expr {
        public final double value;
        public Num(double value) { this.value = value; }
        @Override public <R> R accept(ExprVisitor<R> v) { return v.visitNumber(this); }
    }

    /** A (possibly indexed) name: {@code beta}, {@code I_ip}, {@code N_ip}, {@code I_ip_free}. */
    public static final class Name extends Expr {
        public final Indexed name;
        public Name(Indexed name) { this.name = name; }
        @Override public <R> R accept(ExprVisitor<R> v) { return v.visitName(this); }
    }

    public static final class Binary extends Expr {
        public final char op; // + - * /
        public final Expr left, right;
        public Binary(char op, Expr left, Expr right) { this.op = op; this.left = left; this.right = right; }
        @Override public <R> R accept(ExprVisitor<R> v) { return v.visitBinary(this); }
    }

    public static final class Neg extends Expr {
        public final Expr operand;
        public Neg(Expr operand) { this.operand = operand; }
        @Override public <R> R accept(ExprVisitor<R> v) { return v.visitNeg(this); }
    }

    /** {@code sin(t)}, {@code sqrt(x)}... */
    public static final class Call extends Expr {
        public final String function;
        public final List<Expr> arguments;
        public Call(String function, List<Expr> arguments) { this.function = function; this.arguments = Collections.unmodifiableList(arguments); }
        @Override public <R> R accept(ExprVisitor<R> v) { return v.visitCall(this); }
    }

    /** {@code sum_ (i in species) body}: the body is summed over the values of the attribute. */
    public static final class Sum extends Expr {
        public final String variable;
        public final String attribute;
        public final Expr body;
        public Sum(String variable, String attribute, Expr body) { this.variable = variable; this.attribute = attribute; this.body = body; }
        @Override public <R> R accept(ExprVisitor<R> v) { return v.visitSum(this); }
    }
}
