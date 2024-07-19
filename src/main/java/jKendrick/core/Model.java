package jKendrick.core;

import java.util.HashMap;
import java.util.Map;

public class Model {
    private Map<String, Double> parameters;

    public Model() {
        this.parameters = new HashMap<>();
    }

    public void setParameter(String name, double value) {
        parameters.put(name, value);
    }

    public double getParameter(String name) {
        return parameters.getOrDefault(name, 0.0);
    }

    public Map<String, Double> getParameters() {
        return parameters;
    }
}
