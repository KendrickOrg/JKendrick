package jKendrick.model;

import jKendrick.dsl.ast.Ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * The same concern with the declared names of {@code renaming} (its attribute, values or parameters)
     * replaced in its declarations and in every expression, as a composition reads it under a
     * {@code renaming:} clause. Names not in the map are kept; the map may only name declared names.
     */
    public Concern renamed(Map<String, String> renaming) {
        if (renaming.isEmpty()) return this;
        Attribute a = attribute == null ? null
                : new Attribute(rename(attribute.name, renaming), renameAll(attribute.values, renaming),
                        attribute.entry == null ? null : rename(attribute.entry, renaming), attribute.inherited);
        Map<String, Ast.Expr> formulas = new LinkedHashMap<>();
        for (Map.Entry<String, Ast.Expr> e : parameterFormulas.entrySet())
            formulas.put(rename(e.getKey(), renaming), rename(e.getValue(), renaming));
        Map<String, Ast.Expr> eqs = new LinkedHashMap<>();
        for (Map.Entry<String, Ast.Expr> e : equations.entrySet())
            eqs.put(rename(e.getKey(), renaming), rename(e.getValue(), renaming));
        List<LocalTransition> ts = new ArrayList<>();
        for (LocalTransition t : transitions)
            ts.add(new LocalTransition(t.from == null ? null : rename(t.from, renaming), rename(t.rate, renaming),
                    t.to == null ? null : rename(t.to, renaming)));
        return new Concern(name, a, renameAll(parameters, renaming), formulas, eqs, ts);
    }

    private static String rename(String n, Map<String, String> renaming) {
        return renaming.getOrDefault(n, n);
    }

    private static List<String> renameAll(List<String> names, Map<String, String> renaming) {
        List<String> out = new ArrayList<>();
        for (String n : names) out.add(rename(n, renaming));
        return out;
    }

    /** The expression with every name of the map replaced; a concern's names carry no index. */
    private static Ast.Expr rename(Ast.Expr e, Map<String, String> renaming) {
        return e.accept(new Ast.ExprVisitor<Ast.Expr>() {
            @Override public Ast.Expr visitNumber(Ast.Num x) { return x; }
            @Override public Ast.Expr visitName(Ast.Name x) {
                List<String> idx = renameAll(x.name.indices, renaming);
                return new Ast.Name(new Ast.Indexed(rename(x.name.base, renaming), idx));
            }
            @Override public Ast.Expr visitBinary(Ast.Binary x) { return new Ast.Binary(x.op, x.left.accept(this), x.right.accept(this)); }
            @Override public Ast.Expr visitNeg(Ast.Neg x) { return new Ast.Neg(x.operand.accept(this)); }
            @Override public Ast.Expr visitCall(Ast.Call x) {
                List<Ast.Expr> args = new ArrayList<>();
                for (Ast.Expr a : x.arguments) args.add(a.accept(this));
                return new Ast.Call(x.function, args);
            }
            @Override public Ast.Expr visitSum(Ast.Sum x) { return new Ast.Sum(x.variable, rename(x.attribute, renaming), x.body.accept(this)); }
        });
    }

    /** The names this concern declares: its attribute, the attribute's values and its parameters. */
    public Set<String> declaredNames() {
        Set<String> out = new LinkedHashSet<>(parameters);
        if (attribute != null) {
            out.add(attribute.name);
            out.addAll(attribute.values);
        }
        return out;
    }

    /**
     * True when the dynamics is only known as ODEs: the equations were given and could not be
     * read as flows (see {@code FlowExtractor}). Such a concern can be integrated but not
     * simulated stochastically.
     */
    public boolean definedByOdesOnly() {
        return !equations.isEmpty() && transitions.isEmpty();
    }

    /** True when the equations were given (whether or not transitions were derived from them). */
    public boolean hasEquations() {
        return !equations.isEmpty();
    }

    @Override
    public String toString() {
        return "Concern " + name + (attribute == null ? "" : " (" + attribute + ")");
    }

    /**
     * A transition {@code from -- rate --> to} between two values of the concern's attribute.
     * A null {@code from} is a birth from the outside, whose rate is absolute; a null {@code to}
     * is a death to the outside. Otherwise the rate is per capita of the source compartment.
     */
    public static final class LocalTransition {
        /** Null for a birth (from the outside). */
        public final String from;
        /** Null for a death (to the outside). */
        public final String to;
        public final Ast.Expr rate;

        public LocalTransition(String from, Ast.Expr rate, String to) {
            this.from = from;
            this.rate = rate;
            this.to = to;
        }

        public boolean isBirth() { return from == null; }
        public boolean isDeath() { return to == null; }

        @Override
        public String toString() {
            return (from == null ? "outside" : from) + " --> " + (to == null ? "outside" : to);
        }
    }
}
