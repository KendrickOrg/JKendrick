package jKendrick.dsl;

import java.util.*;

public class KModel {
    private final String name;
    private final List<String> compartments = new ArrayList<>();
    private final Set<String> parameters = new HashSet<>();
    private final List<Transition> transitions = new ArrayList<>();

    public KModel(String name) {
        this.name = name;
    }

    public void addCompartment(String c) {
        compartments.add(c);
    }

    public void addParameter(String p) {
        parameters.add(p);
    }

    public void addTransition(String from, String to, String rateExpression) {
        transitions.add(new Transition(from, to, rateExpression));
    }

    public String getName() {
        return name;
    }

    public List<String> getCompartments() {
        return compartments;
    }

    public Set<String> getParameters() {
        return parameters;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public static class Transition {
        public final String from;
        public final String to;
        public final String rate;

        public Transition(String from, String to, String rate) {
            this.from = from;
            this.to = to;
            this.rate = rate;
        }
    }
}
