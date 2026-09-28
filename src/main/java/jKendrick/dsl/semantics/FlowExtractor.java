package jKendrick.dsl.semantics;

import jKendrick.dsl.ast.Ast;
import jKendrick.model.Concern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a system of ODEs as a set of transitions, under the hypotheses that let a deterministic
 * compartmental model be interpreted as a CTMC (Section 3 of the paper): every term of a
 * right-hand side is a flow between two compartments, or between a compartment and the outside,
 * proportional to the size of its source compartment, and events are Poisson.
 *
 * <ul>
 *   <li>A term {@code -T} in the equation of X and the same term {@code +T} in the equation of Y
 *       is the transition X --> Y; its per-capita rate is T with one factor X removed
 *       ({@code -lambda*S} in S and {@code +lambda*S} in E give {@code S -- lambda --> E}).</li>
 *   <li>A term {@code -T} in X matched by no {@code +T} is a death, X --> outside, at per-capita rate T/X.</li>
 *   <li>A term {@code +T} matched by no {@code -T} is a birth, outside --> Y, at absolute rate T
 *       ({@code mu*N} in S).</li>
 * </ul>
 * The equations are in flow form when every negative term contains its own compartment as a
 * factor. Otherwise {@link #extract} returns null and the concern keeps its ODE reading only.
 */
public final class FlowExtractor {
    private FlowExtractor() {}

    /** A signed product of factors: {@code sign * (numerator factors) / (denominator factors)}. */
    private static final class Term {
        final int sign;
        final List<Ast.Expr> numerator = new ArrayList<>();
        final List<Ast.Expr> denominator = new ArrayList<>();
        Term(int sign) { this.sign = sign; }

        String key() {
            List<String> n = new ArrayList<>(), d = new ArrayList<>();
            for (Ast.Expr e : numerator) n.add(text(e));
            for (Ast.Expr e : denominator) d.add(text(e));
            Collections.sort(n);
            Collections.sort(d);
            return n + "/" + d;
        }

        /** The term without one occurrence of the compartment factor, as an expression; null if absent. */
        Ast.Expr rateWithout(String compartment) {
            List<Ast.Expr> rest = new ArrayList<>(numerator);
            boolean removed = false;
            for (int i = 0; i < rest.size(); i++) {
                Ast.Expr e = rest.get(i);
                if (e instanceof Ast.Name && ((Ast.Name) e).name.toString().equals(compartment)) {
                    rest.remove(i);
                    removed = true;
                    break;
                }
            }
            return removed ? product(rest, denominator) : null;
        }

        Ast.Expr asExpr() {
            return product(numerator, denominator);
        }

        private static Ast.Expr product(List<Ast.Expr> num, List<Ast.Expr> den) {
            Ast.Expr e = num.isEmpty() ? new Ast.Num(1) : num.get(0);
            for (int i = 1; i < num.size(); i++) e = new Ast.Binary('*', e, num.get(i));
            for (Ast.Expr d : den) e = new Ast.Binary('/', e, d);
            return e;
        }
    }

    /**
     * The transitions denoted by the equations, or null when they are not in flow form.
     *
     * @param equations one right-hand side per compartment (attribute value)
     */
    public static List<Concern.LocalTransition> extract(Map<String, Ast.Expr> equations) {
        // 1. flatten every right-hand side into signed terms
        Map<String, List<Term>> terms = new LinkedHashMap<>();
        for (Map.Entry<String, Ast.Expr> eq : equations.entrySet()) {
            List<Term> list = new ArrayList<>();
            if (!flatten(eq.getValue(), 1, list)) return null;
            terms.put(eq.getKey(), list);
        }
        // 2. index the positive terms by their canonical text, per compartment
        Map<String, Map<String, Term>> positives = new LinkedHashMap<>();
        for (Map.Entry<String, List<Term>> e : terms.entrySet()) {
            Map<String, Term> byKey = new LinkedHashMap<>();
            for (Term t : e.getValue()) if (t.sign > 0) byKey.put(t.key(), t);
            positives.put(e.getKey(), byKey);
        }
        // 3. each negative term is an outflow: to the compartment that has the same term as an inflow, or to the outside
        List<Concern.LocalTransition> result = new ArrayList<>();
        for (Map.Entry<String, List<Term>> e : terms.entrySet()) {
            String from = e.getKey();
            for (Term t : e.getValue()) {
                if (t.sign > 0) continue;
                Ast.Expr rate = t.rateWithout(from);
                if (rate == null) return null; // an outflow not proportional to its source: not a flow
                String to = null;
                for (Map.Entry<String, Map<String, Term>> p : positives.entrySet()) {
                    if (p.getKey().equals(from)) continue;
                    if (p.getValue().remove(t.key()) != null) { to = p.getKey(); break; }
                }
                result.add(new Concern.LocalTransition(from, rate, to));
            }
        }
        // 4. the positive terms left are inflows from the outside (births), at absolute rate
        for (Map.Entry<String, Map<String, Term>> p : positives.entrySet())
            for (Term t : p.getValue().values())
                result.add(new Concern.LocalTransition(null, t.asExpr(), p.getKey()));
        return result;
    }

    /** Splits a sum of products into terms; false when the expression has another shape. */
    private static boolean flatten(Ast.Expr e, int sign, List<Term> out) {
        if (e instanceof Ast.Binary) {
            Ast.Binary b = (Ast.Binary) e;
            if (b.op == '+') return flatten(b.left, sign, out) && flatten(b.right, sign, out);
            if (b.op == '-') return flatten(b.left, sign, out) && flatten(b.right, -sign, out);
        }
        if (e instanceof Ast.Neg) return flatten(((Ast.Neg) e).operand, -sign, out);
        Term t = new Term(sign);
        if (!factors(e, t, false)) return false;
        out.add(t);
        return true;
    }

    /** Collects the factors of a product/quotient tree; false when a sum is nested inside. */
    private static boolean factors(Ast.Expr e, Term t, boolean inverted) {
        if (e instanceof Ast.Binary) {
            Ast.Binary b = (Ast.Binary) e;
            if (b.op == '*') return factors(b.left, t, inverted) && factors(b.right, t, inverted);
            if (b.op == '/') return factors(b.left, t, inverted) && factors(b.right, t, !inverted);
            return false;
        }
        if (e instanceof Ast.Neg || e instanceof Ast.Sum) return false;
        (inverted ? t.denominator : t.numerator).add(e);
        return true;
    }

    /** Canonical text of a factor, for matching terms across equations. */
    public static String text(Ast.Expr e) {
        return e.accept(new Ast.ExprVisitor<String>() {
            @Override public String visitNumber(Ast.Num n) { return Double.toString(n.value); }
            @Override public String visitName(Ast.Name n) { return n.name.toString(); }
            @Override public String visitBinary(Ast.Binary b) { return "(" + text(b.left) + b.op + text(b.right) + ")"; }
            @Override public String visitNeg(Ast.Neg n) { return "(-" + text(n.operand) + ")"; }
            @Override public String visitCall(Ast.Call c) {
                StringBuilder sb = new StringBuilder(c.function).append('(');
                for (Ast.Expr a : c.arguments) sb.append(text(a)).append(',');
                return sb.append(')').toString();
            }
            @Override public String visitSum(Ast.Sum s) { return "sum_" + s.variable + "in" + s.attribute + "(" + text(s.body) + ")"; }
        });
    }
}
