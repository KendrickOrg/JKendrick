package jKendrick.model;

import jKendrick.dsl.ast.Ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles an expression into a {@link Compiled} closure for a fixed context (compartment,
 * owner concern, bound variables). It resolves names exactly as {@link Evaluator} does, but
 * once: parameters given by tables become constants, cardinalities become sums over
 * precomputed compartment indices, {@code sum_} is unrolled over the attribute's values and
 * formulas are inlined with their indices bound. Only {@code t} and the state remain to be read
 * at evaluation time, so evaluating a compiled rate allocates nothing.
 * <p>
 * The context passed to the compiler needs no state: only its ground model, compartment,
 * owner and environment are used.
 */
public final class RateCompiler {
    private RateCompiler() {}

    /** What a compiled expression reads: the state indices, and whether it reads the time. */
    public static final class Reads {
        public final java.util.Set<Integer> state = new java.util.TreeSet<>();
        public boolean time;
    }

    public static Compiled compile(Ast.Expr expr, Context ctx) {
        return compile(expr, ctx, new Reads());
    }

    /** Compiles, recording in {@code reads} the state indices and the time the closure reads. */
    public static Compiled compile(Ast.Expr expr, Context ctx, Reads reads) {
        return expr.accept(new Ast.ExprVisitor<Compiled>() {
            private Compiled compile(Ast.Expr e, Context c) { return RateCompiler.compile(e, c, reads); }

            @Override public Compiled visitNumber(Ast.Num e) { return new Compiled.Const(e.value); }
            @Override public Compiled visitName(Ast.Name e) { return name(e.name, ctx, reads); }

            @Override public Compiled visitNeg(Ast.Neg e) {
                Compiled c = compile(e.operand, ctx);
                if (c instanceof Compiled.Const) return new Compiled.Const(-((Compiled.Const) c).value);
                return (t, s) -> -c.eval(t, s);
            }

            @Override public Compiled visitBinary(Ast.Binary e) {
                Compiled l = compile(e.left, ctx), r = compile(e.right, ctx);
                if (l instanceof Compiled.Const && r instanceof Compiled.Const)
                    return new Compiled.Const(apply(e.op, ((Compiled.Const) l).value, ((Compiled.Const) r).value));
                switch (e.op) {
                    case '+': return (t, s) -> l.eval(t, s) + r.eval(t, s);
                    case '-': return (t, s) -> l.eval(t, s) - r.eval(t, s);
                    case '*':
                        if (l instanceof Compiled.Const) { double k = ((Compiled.Const) l).value; return (t, s) -> k * r.eval(t, s); }
                        if (r instanceof Compiled.Const) { double k = ((Compiled.Const) r).value; return (t, s) -> l.eval(t, s) * k; }
                        return (t, s) -> l.eval(t, s) * r.eval(t, s);
                    case '/': return (t, s) -> l.eval(t, s) / r.eval(t, s);
                    default: throw new IllegalStateException("operator " + e.op);
                }
            }

            @Override public Compiled visitCall(Ast.Call e) {
                Compiled[] args = new Compiled[e.arguments.size()];
                for (int i = 0; i < args.length; i++) args[i] = compile(e.arguments.get(i), ctx);
                Compiled a0 = args.length > 0 ? args[0] : null, a1 = args.length > 1 ? args[1] : null;
                switch (e.function) {
                    case "sin": return (t, s) -> Math.sin(a0.eval(t, s));
                    case "cos": return (t, s) -> Math.cos(a0.eval(t, s));
                    case "exp": return (t, s) -> Math.exp(a0.eval(t, s));
                    case "log": return (t, s) -> Math.log(a0.eval(t, s));
                    case "sqrt": return (t, s) -> Math.sqrt(a0.eval(t, s));
                    case "abs": return (t, s) -> Math.abs(a0.eval(t, s));
                    case "min": return (t, s) -> Math.min(a0.eval(t, s), a1.eval(t, s));
                    case "max": return (t, s) -> Math.max(a0.eval(t, s), a1.eval(t, s));
                    case "pow": return (t, s) -> Math.pow(a0.eval(t, s), a1.eval(t, s));
                    default: throw new IllegalStateException("unknown function " + e.function);
                }
            }

            @Override public Compiled visitSum(Ast.Sum e) {
                Attribute a = ctx.ground.model.attribute(e.attribute);
                if (a == null) throw new IllegalStateException("sum over unknown attribute " + e.attribute);
                List<Compiled> terms = new ArrayList<>();
                double constant = 0;
                for (String v : a.values) {
                    Compiled c = compile(e.body, ctx.bind(e.variable, v));
                    if (c instanceof Compiled.Const) constant += ((Compiled.Const) c).value;
                    else terms.add(c);
                }
                if (terms.isEmpty()) return new Compiled.Const(constant);
                Compiled[] ts = terms.toArray(new Compiled[0]);
                double k = constant;
                return (t, s) -> {
                    double total = k;
                    for (Compiled c : ts) total += c.eval(t, s);
                    return total;
                };
            }
        });
    }

    private static double apply(char op, double l, double r) {
        switch (op) {
            case '+': return l + r;
            case '-': return l - r;
            case '*': return l * r;
            case '/': return l / r;
            default: throw new IllegalStateException("operator " + op);
        }
    }

    private static Compiled name(Ast.Indexed n, Context ctx, Reads reads) {
        ComposedModel m = ctx.ground.model;
        if (n.base.equals("t") && !n.hasIndices()) { reads.time = true; return (t, s) -> t; }
        if (m.isParameter(n.base)) {
            ParameterValue v = ctx.ground.parameters.get(n.base);
            if (v == null) throw new IllegalStateException("parameter " + n.base + " has no value in the scenario");
            return v.compile(Evaluator.indexValues(n, ctx), ctx, reads);
        }
        if (n.base.equals("N")) return cardinality(Evaluator.constraints(new LinkedHashMap<>(), n, ctx, true), m, reads);
        Attribute a = m.attributeOfValue(n.base);
        if (a != null) {
            Map<String, String> constraints = new LinkedHashMap<>();
            constraints.put(a.name, n.base);
            return cardinality(Evaluator.constraints(constraints, n, ctx, false), m, reads);
        }
        throw new IllegalStateException("unknown name " + n);
    }

    /** The sum of the state over the compartments matching the constraints. */
    private static Compiled cardinality(Map<String, String> constraints, ComposedModel m, Reads reads) {
        List<Compartment> comps = m.matching(constraints);
        int[] idx = new int[comps.size()];
        for (int i = 0; i < idx.length; i++) { idx[i] = m.indexOf(comps.get(i)); reads.state.add(idx[i]); }
        if (idx.length == 0) return new Compiled.Const(0);
        if (idx.length == 1) { int i = idx[0]; return (t, s) -> s[i]; }
        return (t, s) -> {
            double total = 0;
            for (int i : idx) total += s[i];
            return total;
        };
    }
}
