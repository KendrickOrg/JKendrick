package jKendrick.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The composition of concerns by the tensor sum.
 *
 * The state space is the Cartesian product of the concerns' attribute values (the compartments),
 * ordered lexicographically with the concerns in composition order, as in the pseudo-commutativity
 * proof of the tensor sum. The transitions of the composed automaton are those of each concern,
 * replicated for every combination of the other concerns' states ({@link #transitions()}); their
 * rates keep the concern's expression, which becomes a functional rate once the scenario gives it
 * heterogeneous values.
 */
public final class ComposedModel {
    public final String name;
    public final List<Concern> concerns;
    public final List<Attribute> attributes;
    public final List<Compartment> compartments;
    private final Map<Compartment, Integer> index = new HashMap<>();
    private final Map<String, Attribute> attributeByName = new LinkedHashMap<>();
    private final Map<String, Attribute> attributeByValue = new HashMap<>();
    private final Map<String, Concern> concernByAttribute = new HashMap<>();
    private final Map<String, Concern> concernByParameter = new HashMap<>();

    public ComposedModel(String name, List<Concern> concerns) {
        this.name = name;
        this.concerns = Collections.unmodifiableList(new ArrayList<>(concerns));
        List<Attribute> attrs = new ArrayList<>();
        for (Concern c : concerns) {
            if (c.attribute != null) {
                if (attributeByName.containsKey(c.attribute.name))
                    throw new IllegalArgumentException("attribute " + c.attribute.name + " is declared by two concerns");
                attrs.add(c.attribute);
                attributeByName.put(c.attribute.name, c.attribute);
                concernByAttribute.put(c.attribute.name, c);
                for (String v : c.attribute.values) {
                    if (attributeByValue.containsKey(v))
                        throw new IllegalArgumentException("value " + v + " belongs to attributes "
                                + attributeByValue.get(v).name + " and " + c.attribute.name);
                    attributeByValue.put(v, c.attribute);
                }
            }
            for (String p : c.parameters) concernByParameter.putIfAbsent(p, c);
        }
        this.attributes = Collections.unmodifiableList(attrs);
        List<Compartment> comps = new ArrayList<>();
        product(0, new LinkedHashMap<>(), comps);
        this.compartments = Collections.unmodifiableList(comps);
        for (int i = 0; i < comps.size(); i++) index.put(comps.get(i), i);
    }

    private void product(int i, Map<String, String> current, List<Compartment> out) {
        if (i == attributes.size()) {
            out.add(new Compartment(current));
            return;
        }
        Attribute a = attributes.get(i);
        for (String v : a.values) {
            current.put(a.name, v);
            product(i + 1, current, out);
        }
        current.remove(a.name);
    }

    public int indexOf(Compartment c) {
        Integer i = index.get(c);
        if (i == null) throw new IllegalArgumentException("unknown compartment " + c);
        return i;
    }

    public Attribute attribute(String name) {
        return attributeByName.get(name);
    }

    /** The attribute one of whose values is {@code value}, or null. */
    public Attribute attributeOfValue(String value) {
        return attributeByValue.get(value);
    }

    public Concern concernOfAttribute(String attribute) {
        return concernByAttribute.get(attribute);
    }

    /** The concern declaring the parameter, or null when no concern declares it. */
    public Concern concernOfParameter(String parameter) {
        return concernByParameter.get(parameter);
    }

    public boolean isParameter(String name) {
        return concernByParameter.containsKey(name);
    }

    /** The compartments whose values agree with {@code constraints} (attribute -> value). */
    public List<Compartment> matching(Map<String, String> constraints) {
        List<Compartment> result = new ArrayList<>();
        outer:
        for (Compartment c : compartments) {
            for (Map.Entry<String, String> e : constraints.entrySet())
                if (!e.getValue().equals(c.get(e.getKey()))) continue outer;
            result.add(c);
        }
        return result;
    }

    /**
     * The transitions of the composed automaton: every local transition of a concern, for every
     * compartment whose value of the concern's attribute is the transition's source.
     */
    public List<Transition> transitions() {
        List<Transition> result = new ArrayList<>();
        for (Concern k : concerns) {
            if (k.attribute == null) continue;
            for (Concern.LocalTransition t : k.transitions)
                for (Compartment c : compartments)
                    if (t.from.equals(c.get(k.attribute.name)))
                        result.add(new Transition(k, t, c, c.with(k.attribute.name, t.to)));
        }
        return result;
    }

    /** A transition of the composed model, between two compartments differing on one attribute. */
    public static final class Transition {
        public final Concern concern;
        public final Concern.LocalTransition local;
        public final Compartment from;
        public final Compartment to;

        Transition(Concern concern, Concern.LocalTransition local, Compartment from, Compartment to) {
            this.concern = concern;
            this.local = local;
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return from + " --> " + to + " (" + concern.name + ")";
        }
    }
}
