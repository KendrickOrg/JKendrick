package jKendrick.dsl.core;

import java.util.*;

public class KModel {
    private final List<String> compartments = new ArrayList<>();
    private final Map<String, Double> parameters = new HashMap<>();
    private final List<KTransition> transitions = new ArrayList<>();

    public void setCompartments(String... comps) {
        compartments.addAll(Arrays.asList(comps));
    }

    public void setParameter(String name, double value) {
        parameters.put(name, value);
    }

    public void addTransition(String from, String to) {
        transitions.add(new KTransition(from, to));
    }

    public List<String> getCompartments() {
        return compartments;
    }

    public Map<String, Double> getParameters() {
        return parameters;
    }

    public Double getParameter(String param) {
        return parameters.get(param);
    }

    public List<KTransition> getTransitions() {
        return transitions;
    }
}