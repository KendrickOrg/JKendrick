package jKendrick.model;

import java.util.Collections;
import java.util.List;

/**
 * An attribute of the population, e.g. {@code status} with values {@code S E I R}.
 *
 * In the mathematical meta-model an attribute is an equivalence relation on the population
 * ("having the same value of this attribute"); its values name the equivalence classes, i.e.
 * the states of the concern's automaton.
 */
public final class Attribute {
    public final String name;
    public final List<String> values;
    /**
     * The value taken by individuals entering the population (births, immigration), or null when
     * the attribute is inherited: an inflow is then replicated over all the values.
     */
    public final String entry;
    /** Whether the attribute is declared {@code inherited}: the inherited default is then intended, and no warning is given. */
    public final boolean inherited;

    public Attribute(String name, List<String> values) {
        this(name, values, null, false);
    }

    public Attribute(String name, List<String> values, String entry) {
        this(name, values, entry, false);
    }

    public Attribute(String name, List<String> values, String entry, boolean inherited) {
        this.name = name;
        this.values = Collections.unmodifiableList(values);
        this.entry = entry;
        this.inherited = inherited;
    }

    public int indexOf(String value) {
        return values.indexOf(value);
    }

    @Override
    public String toString() {
        return name + " in " + values;
    }
}
