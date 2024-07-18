package jKendrick.core;

import java.util.HashMap;
import java.util.Map;

public class Model {
    private double step;
    private double end;
    private Map<String, Double> parameters;

    public Model(double step, double end) {
        this.step = step;
        this.end = end;
        this.parameters = new HashMap<>();
    }

    public double getStep() {
        return step;
    }

    public double getEnd() {
        return end;
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
