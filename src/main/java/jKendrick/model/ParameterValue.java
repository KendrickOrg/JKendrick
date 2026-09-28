package jKendrick.model;

import jKendrick.dsl.ast.Ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The value of a parameter in a ground model: a function of the compartment and of the bound
 * variables (a functional rate in the sense of stochastic automata networks), given either as a
 * constant, as a table indexed by attributes ({@code mu_species: #(...)}) or by variables
 * ({@code beta_ki: #(...)}), or as a formula ({@code lambda_pk: #(sum_ ...)}).
 */
public interface ParameterValue {

    /**
     * The value for the given reference indices.
     *
     * @param refValues the attribute values the reference supplies, in order ({@code beta_ki}
     *                  supplies the values of k and i); empty when the reference is unindexed
     *                  ({@code mu}), in which case the declared indices are taken from the
     *                  current compartment or the environment of {@code ctx}.
     */
    double at(List<String> refValues, Context ctx);

    /** How one declared index is resolved: by an attribute of the compartment or by a variable. */
    final class IndexSpec {
        public final String attribute;
        /** Null when the index is the attribute itself, e.g. {@code mu_species}. */
        public final String variable;

        public IndexSpec(String attribute, String variable) {
            this.attribute = attribute;
            this.variable = variable;
        }

        @Override
        public String toString() {
            return variable == null ? attribute : variable + " in " + attribute;
        }
    }

    // ------------------------------------------------------------------ implementations

    final class Constant implements ParameterValue {
        public final double value;

        public Constant(double value) {
            this.value = value;
        }

        @Override
        public double at(List<String> refValues, Context ctx) {
            return value;
        }

        @Override
        public String toString() {
            return Double.toString(value);
        }
    }

    /** A (nested) array of values indexed by the declared attributes or variables. */
    final class Table implements ParameterValue {
        public final List<IndexSpec> specs;
        public final List<Attribute> domains;
        private final Ast.NumberArray values;

        public Table(List<IndexSpec> specs, List<Attribute> domains, Ast.NumberArray values) {
            if (specs.size() != domains.size()) throw new IllegalArgumentException("specs/domains mismatch");
            this.specs = Collections.unmodifiableList(specs);
            this.domains = Collections.unmodifiableList(domains);
            this.values = values;
        }

        @Override
        public double at(List<String> refValues, Context ctx) {
            List<String> key = refValues.isEmpty() ? Resolution.declaredValues(specs, ctx) : refValues;
            if (key.size() != specs.size())
                throw new IllegalStateException("parameter indexed by " + specs + " referenced with " + key.size() + " indices");
            Ast.NumberArray cursor = values;
            for (int d = 0; d < key.size(); d++) {
                int i = domains.get(d).indexOf(key.get(d));
                if (i < 0) throw new IllegalStateException(key.get(d) + " is not a value of " + domains.get(d).name);
                if (cursor.isScalar() || i >= cursor.elements.size())
                    throw new IllegalStateException("table of " + specs + " has no entry for " + key);
                cursor = cursor.elements.get(i);
            }
            if (!cursor.isScalar()) throw new IllegalStateException("table of " + specs + " needs more indices than " + key);
            return cursor.scalar;
        }

        @Override
        public String toString() {
            return "table" + specs;
        }
    }

    /** A formula over bound variables, e.g. {@code lambda_pk: #(sum_ (i in species) beta_ki * I_ip / N_ip)}. */
    final class Formula implements ParameterValue {
        public final List<IndexSpec> specs;
        public final Ast.Expr expr;
        /** The concern the formula belongs to (localisation of unindexed names), or null for a scenario formula. */
        public final Concern owner;

        public Formula(List<IndexSpec> specs, Ast.Expr expr, Concern owner) {
            this.specs = Collections.unmodifiableList(specs);
            this.expr = expr;
            this.owner = owner;
        }

        @Override
        public double at(List<String> refValues, Context ctx) {
            List<String> key = refValues.isEmpty() ? Resolution.declaredValues(specs, ctx) : refValues;
            if (key.size() != specs.size())
                throw new IllegalStateException("formula indexed by " + specs + " referenced with " + key.size() + " indices");
            Map<String, String> env = new HashMap<>(ctx.env);
            for (int d = 0; d < specs.size(); d++) env.put(specs.get(d).variable, key.get(d));
            return Evaluator.eval(expr, ctx.withEnv(env).withOwner(owner));
        }

        @Override
        public String toString() {
            return "formula" + specs;
        }
    }

    /** Shared helper: the values of declared indices taken from the context. */
    final class Resolution {
        private Resolution() {}

        static List<String> declaredValues(List<IndexSpec> specs, Context ctx) {
            List<String> key = new ArrayList<>();
            for (IndexSpec s : specs) {
                String v;
                if (s.variable != null) {
                    v = ctx.env.get(s.variable);
                    if (v == null && ctx.current != null) v = ctx.current.get(s.attribute);
                    if (v == null) throw new IllegalStateException("variable " + s.variable + " is not bound");
                } else {
                    if (ctx.current == null)
                        throw new IllegalStateException("index " + s.attribute + " needs a compartment to be resolved");
                    v = ctx.current.get(s.attribute);
                    if (v == null) throw new IllegalStateException("the model has no attribute " + s.attribute);
                }
                key.add(v);
            }
            return key;
        }
    }
}
