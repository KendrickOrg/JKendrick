package jKendrick.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A compartment of a (possibly composed) model: one value for each attribute of the model,
 * e.g. {@code {status=I, species=birds, zone=1, isolation=free}}.
 *
 * Compartments are the equivalence classes of the conjunction of the attributes' equivalence
 * relations, and the states of the composed stochastic automaton. Two compartments are equal
 * when they have the same values; the attribute order (that of the composition) is kept for
 * printing as {@code I_birds_1_free}.
 */
public final class Compartment {
    private final Map<String, String> values;

    public Compartment(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** The value of an attribute, or null when the model has no such attribute. */
    public String get(String attribute) {
        return values.get(attribute);
    }

    public Map<String, String> values() {
        return values;
    }

    /** This compartment with one attribute changed, e.g. the sibling with status R. */
    public Compartment with(String attribute, String value) {
        Map<String, String> copy = new LinkedHashMap<>(values);
        copy.put(attribute, value);
        return new Compartment(copy);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Compartment && ((Compartment) o).values.equals(values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return String.join("_", values.values());
    }
}
