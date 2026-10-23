package jKendrick.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        // One namespace: an attribute name, an attribute value and a parameter name are distinct from each
        // other across all the concerns, and from the reserved N and t. Otherwise a name of one concern
        // would capture, in the expressions of another, a name that the other owns (a parameter I of a
        // concern would be read where SEIRS writes beta*I/N), which the independence of concerns forbids.
        Map<String, String> declared = new HashMap<>();
        declared.put("N", "the reserved name N (the population)");
        declared.put("t", "the reserved name t (the time)");
        for (Concern c : concerns) {
            if (c.attribute != null) {
                if (attributeByName.containsKey(c.attribute.name))
                    throw new IllegalArgumentException("attribute " + c.attribute.name + " is declared by two concerns");
                declare(declared, c.attribute.name, "the attribute of " + c.name);
                attrs.add(c.attribute);
                attributeByName.put(c.attribute.name, c.attribute);
                concernByAttribute.put(c.attribute.name, c);
                for (String v : c.attribute.values) {
                    if (attributeByValue.containsKey(v))
                        throw new IllegalArgumentException("value " + v + " belongs to attributes "
                                + attributeByValue.get(v).name + " and " + c.attribute.name);
                    declare(declared, v, "a value of the attribute " + c.attribute.name + " of " + c.name);
                    attributeByValue.put(v, c.attribute);
                }
            }
            for (String p : c.parameters) {
                Concern other = concernByParameter.putIfAbsent(p, c);
                if (other != null)
                    throw new IllegalArgumentException("parameter " + p + " is declared by both " + other.name + " and " + c.name
                            + ": a parameter belongs to one concern; bind the two to the same value in the scenario");
                declare(declared, p, "a parameter of " + c.name);
            }
        }
        this.attributes = Collections.unmodifiableList(attrs);
        List<Compartment> comps = new ArrayList<>();
        product(0, new LinkedHashMap<>(), comps);
        this.compartments = Collections.unmodifiableList(comps);
        for (int i = 0; i < comps.size(); i++) index.put(comps.get(i), i);
    }

    /** Registers a declared name, rejecting one already declared in another category or by another concern. */
    private static void declare(Map<String, String> declared, String name, String what) {
        String before = declared.putIfAbsent(name, what);
        if (before != null)
            throw new IllegalArgumentException("name " + name + " is " + what + " and " + before
                    + ": a name belongs to one concern and one category (attribute, value or parameter), and N and t are reserved");
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
            for (Concern.LocalTransition t : k.transitions) {
                String a = k.attribute.name;
                for (Compartment c : compartments) {
                    if (t.isBirth()) {
                        if (t.to.equals(c.get(a)) && isEntry(c, a)) result.add(new Transition(k, t, null, c));
                    } else if (t.from.equals(c.get(a))) {
                        result.add(new Transition(k, t, c, t.isDeath() ? null : c.with(a, t.to)));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Whether {@code c} may receive an inflow of the concern owning attribute {@code own}: for
     * every other attribute with an entry value, {@code c} has that value. Attributes without an
     * entry value are inherited and the inflow is replicated over their values.
     */
    boolean isEntry(Compartment c, String own) {
        for (Attribute b : attributes)
            if (!b.name.equals(own) && b.entry != null && !b.entry.equals(c.get(b.name))) return false;
        return true;
    }

    /** The names of the attributes with an entry value, other than {@code own}. */
    public Set<String> entryAttributes(String own) {
        Set<String> names = new java.util.LinkedHashSet<>();
        for (Attribute b : attributes) if (!b.name.equals(own) && b.entry != null) names.add(b.name);
        return names;
    }

    /**
     * A transition of the composed model, between two compartments differing on one attribute,
     * or between a compartment and the outside (birth: {@code from} null, absolute rate; death:
     * {@code to} null).
     */
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

        public boolean isBirth() { return from == null; }
        public boolean isDeath() { return to == null; }

        /** The compartment the rate is evaluated at: the source, or the target for a birth. */
        public Compartment site() { return from != null ? from : to; }

        @Override
        public String toString() {
            return (from == null ? "outside" : from.toString()) + " --> " + (to == null ? "outside" : to.toString()) + " (" + concern.name + ")";
        }
    }
}
