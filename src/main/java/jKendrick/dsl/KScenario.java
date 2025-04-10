package jKendrick.dsl;

import java.util.HashMap;
import java.util.Map;

public class KScenario {
    private final String name;
    private final KModel model;

    private final Map<String, Double> parameters = new HashMap<>();
    private final Map<String, Double> initialPopulations = new HashMap<>();

    public KScenario(String name, KModel model) {
        this.name = name;
        this.model = model;
    }

    public void setParameter(String key, double value) {
        parameters.put(key, value);
    }

    public void setInitialPopulation(String compartment, double value) {
        initialPopulations.put(compartment, value);
    }

    public double getParameter(String key) {
        return parameters.getOrDefault(key, 0.0);
    }

    public double getInitialPopulation(String compartment) {
        return initialPopulations.getOrDefault(compartment, 0.0);
    }

    public Map<String, Double> getAllParameters() {
        return parameters;
    }

    public Map<String, Double> getAllInitialPopulations() {
        return initialPopulations;
    }

    public KModel getModel() {
        return model;
    }

    public String getName() {
        return name;
    }
}
