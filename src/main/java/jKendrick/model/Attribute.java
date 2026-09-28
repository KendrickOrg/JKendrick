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

    public Attribute(String name, List<String> values) {
        this.name = name;
        this.values = Collections.unmodifiableList(values);
    }

    public int indexOf(String value) {
        return values.indexOf(value);
    }

    @Override
    public String toString() {
        return name + " in " + values;
    }
}
