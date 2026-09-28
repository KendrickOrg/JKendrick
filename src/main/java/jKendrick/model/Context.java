package jKendrick.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Where an expression is evaluated: the ground model and its current state, the compartment
 * the expression is localised to (if any), the concern that owns the expression (if any) and
 * the values of the bound index variables.
 *
 * <ul>
 *   <li>{@code current} is the compartment whose equation or outgoing transition is being
 *       evaluated; unindexed names are localised to it ({@code S} means "the sibling of the
 *       current compartment with status S").</li>
 *   <li>{@code owner} is the concern the expression belongs to; {@code N} in an owner's
 *       expression sums over the owner's attribute, the other attributes being those of
 *       {@code current}. Scenario formulas have no owner and must index their names.</li>
 *   <li>{@code env} maps single-letter variables ({@code (k in species)}) to attribute values.</li>
 * </ul>
 */
public final class Context {
    public final GroundModel ground;
    public final double t;
    public final double[] state;
    public final Compartment current;
    public final Concern owner;
    public final Map<String, String> env;

    public Context(GroundModel ground, double t, double[] state, Compartment current, Concern owner, Map<String, String> env) {
        this.ground = ground;
        this.t = t;
        this.state = state;
        this.current = current;
        this.owner = owner;
        this.env = Collections.unmodifiableMap(new HashMap<>(env));
    }

    public Context bind(String variable, String value) {
        Map<String, String> e = new HashMap<>(env);
        e.put(variable, value);
        return new Context(ground, t, state, current, owner, e);
    }

    public Context withOwner(Concern newOwner) {
        return new Context(ground, t, state, current, newOwner, env);
    }

    public Context withEnv(Map<String, String> newEnv) {
        return new Context(ground, t, state, current, owner, newEnv);
    }
}
