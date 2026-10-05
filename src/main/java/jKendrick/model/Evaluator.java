package jKendrick.model;

import jKendrick.dsl.ast.Ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates expressions of the DSL in a {@link Context}: this is where the meaning of names
 * is fixed.
 *
 * <ul>
 *   <li>{@code t} is the time.</li>
 *   <li>A parameter name ({@code mu}, {@code beta_ki}, {@code lambda}) denotes the parameter's
 *       value for the indices supplied by the reference, or for the current compartment.</li>
 *   <li>{@code N} and a value of an attribute ({@code S}, {@code I_ip_free}) denote cardinalities:
 *       the sum of the compartments selected by the name and its indices. An unindexed name is
 *       localised to the current compartment (all other attributes fixed to its values, except
 *       the owner's attribute for {@code N}); an indexed name fixes only the attributes its
 *       indices mention and sums over the others.</li>
 *   <li>An index token is an attribute name (value taken from the current compartment), an
 *       attribute value ({@code free}, {@code 1}) or a run of bound single-letter variables
 *       ({@code ip}).</li>
 *   <li>{@code sum_ (i in a) e} sums e over the values of attribute a.</li>
 * </ul>
 */
public final class Evaluator {
    private Evaluator() {}

    public static double eval(Ast.Expr expr, Context ctx) {
        return expr.accept(new Ast.ExprVisitor<Double>() {
            @Override public Double visitNumber(Ast.Num e) { return e.value; }
            @Override public Double visitName(Ast.Name e) { return name(e.name, ctx); }
            @Override public Double visitNeg(Ast.Neg e) { return -eval(e.operand, ctx); }

            @Override public Double visitBinary(Ast.Binary e) {
                double l = eval(e.left, ctx), r = eval(e.right, ctx);
                switch (e.op) {
                    case '+': return l + r;
                    case '-': return l - r;
                    case '*': return l * r;
                    case '/': return l / r;
                    default: throw new IllegalStateException("operator " + e.op);
                }
            }

            @Override public Double visitCall(Ast.Call e) {
                double[] a = new double[e.arguments.size()];
                for (int i = 0; i < a.length; i++) a[i] = eval(e.arguments.get(i), ctx);
                switch (e.function) {
                    case "sin": return Math.sin(a[0]);
                    case "cos": return Math.cos(a[0]);
                    case "exp": return Math.exp(a[0]);
                    case "log": return Math.log(a[0]);
                    case "sqrt": return Math.sqrt(a[0]);
                    case "abs": return Math.abs(a[0]);
                    case "min": return Math.min(a[0], a[1]);
                    case "max": return Math.max(a[0], a[1]);
                    case "pow": return Math.pow(a[0], a[1]);
                    default: throw new IllegalStateException("unknown function " + e.function);
                }
            }

            @Override public Double visitSum(Ast.Sum e) {
                Attribute a = ctx.ground.model.attribute(e.attribute);
                if (a == null) throw new IllegalStateException("sum over unknown attribute " + e.attribute);
                double total = 0;
                for (String v : a.values) total += eval(e.body, ctx.bind(e.variable, v));
                return total;
            }
        });
    }

    // ------------------------------------------------------------------ names

    private static double name(Ast.Indexed n, Context ctx) {
        ComposedModel m = ctx.ground.model;
        if (n.base.equals("t") && !n.hasIndices()) return ctx.t;
        if (m.isParameter(n.base)) {
            ParameterValue v = ctx.ground.parameters.get(n.base);
            if (v == null) throw new IllegalStateException("parameter " + n.base + " has no value in the scenario");
            return v.at(indexValues(n, ctx), ctx);
        }
        if (n.base.equals("N")) return cardinality(new LinkedHashMap<>(), n, ctx, true);
        Attribute a = m.attributeOfValue(n.base);
        if (a != null) {
            Map<String, String> constraints = new LinkedHashMap<>();
            constraints.put(a.name, n.base);
            return cardinality(constraints, n, ctx, false);
        }
        throw new IllegalStateException("unknown name " + n);
    }

    /** The attribute values a parameter reference supplies through its indices, in order. */
    static List<String> indexValues(Ast.Indexed n, Context ctx) {
        ComposedModel m = ctx.ground.model;
        List<String> values = new ArrayList<>();
        for (String token : n.indices) {
            if (m.attribute(token) != null) {
                if (ctx.current == null) throw new IllegalStateException(n + ": index " + token + " needs a compartment");
                values.add(ctx.current.get(token));
            } else if (m.attributeOfValue(token) != null) {
                values.add(token);
            } else {
                for (char c : token.toCharArray()) values.add(variableValue(String.valueOf(c), token, n, ctx));
            }
        }
        return values;
    }

    private static String variableValue(String var, String token, Ast.Indexed n, Context ctx) {
        if (ctx.ground.attributeOfVariable(var) == null)
            throw new IllegalStateException(n + ": '" + token + "' is neither an attribute, a value nor bound variables");
        String value = ctx.env.get(var);
        if (value == null) throw new IllegalStateException(n + ": variable " + var + " is not bound here");
        return value;
    }

    /** The constraints (attribute -> value) that index tokens add to a cardinality. */
    private static void constrain(Map<String, String> constraints, Ast.Indexed n, Context ctx) {
        ComposedModel m = ctx.ground.model;
        for (String token : n.indices) {
            Attribute byName = m.attribute(token);
            Attribute byValue = m.attributeOfValue(token);
            if (byName != null) {
                if (ctx.current == null) throw new IllegalStateException(n + ": index " + token + " needs a compartment");
                constraints.put(token, ctx.current.get(token));
            } else if (byValue != null) {
                constraints.put(byValue.name, token);
            } else {
                constraints.putAll(variables(token, n, ctx));
            }
        }
    }

    /** {@code "ip"} -> {species=<env(i)>, zone=<env(p)>}. */
    private static Map<String, String> variables(String token, Ast.Indexed n, Context ctx) {
        Map<String, String> result = new LinkedHashMap<>();
        for (char c : token.toCharArray()) {
            String var = String.valueOf(c);
            String value = variableValue(var, token, n, ctx);
            String attr = ctx.ground.attributeOfVariable(var);
            if (result.containsKey(attr))
                throw new IllegalStateException(n + ": two variables of '" + token + "' range over " + attr);
            result.put(attr, value);
        }
        return result;
    }

    private static double cardinality(Map<String, String> constraints, Ast.Indexed n, Context ctx, boolean isN) {
        ComposedModel m = ctx.ground.model;
        double total = 0;
        for (Compartment c : m.matching(constraints(constraints, n, ctx, isN))) total += ctx.state[m.indexOf(c)];
        return total;
    }

    /**
     * The compartments a cardinality name denotes, as constraints attribute -> value, given the
     * initial constraints (the owner attribute's value for an attribute value, none for N).
     * This depends on the compartment and the environment of the context only, not on its state.
     */
    static Map<String, String> constraints(Map<String, String> constraints, Ast.Indexed n, Context ctx, boolean isN) {
        ComposedModel m = ctx.ground.model;
        if (n.hasIndices()) {
            constrain(constraints, n, ctx);
        } else {
            if (ctx.current == null)
                throw new IllegalStateException(n + " must be indexed outside a compartment's equation or transition");
            String free = null;
            if (isN) {
                if (ctx.owner == null || ctx.owner.attribute == null)
                    throw new IllegalStateException("N must be indexed in a scenario formula");
                free = ctx.owner.attribute.name;
            }
            for (Attribute a : m.attributes)
                if (!constraints.containsKey(a.name) && !a.name.equals(free) && !ctx.free.contains(a.name))
                    constraints.put(a.name, ctx.current.get(a.name));
        }
        return constraints;
    }
}
