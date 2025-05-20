package jKendrick.IHM.ui;

import java.util.HashMap;
import java.util.Map;

public class KendrickModelData {
    private String scenario;
    private String solver;
    private Map<String, Object> parameters;
    private Map<String, Object> initialPopulations;
    private double step;
    private double end;
    private int runs;

    public KendrickModelData() {
        parameters = new HashMap<>();
        initialPopulations = new HashMap<>();
        runs = 1;
    }

    // Setters
    public void setScenario(String scenario) { this.scenario = scenario; }
    public void setSolver(String solver) { this.solver = solver; }
    public void setParameter(String key, Object value) { parameters.put(key, value); }
    public void setInitialPopulation(String compartment, Object value) { initialPopulations.put(compartment, value); }
    public void setStep(double step) { this.step = step; }
    public void setEnd(double end) { this.end = end; }
    public void setRuns(int runs) { this.runs = runs; }

    // Getters
    public String getScenario() { return scenario; }
    public String getSolver() { return solver; }
    public Map<String, Object> getParameters() { return parameters; }
    public Map<String, Object> getInitialPopulations() { return initialPopulations; }
    public double getStep() { return step; }
    public double getEnd() { return end; }
    public int getRuns() { return runs; }

    public double getScalarParameter(String key, double defaultValue) {
        Object value = parameters.getOrDefault(key, defaultValue);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof double[]) {
            double[] v = (double[]) value;
            return v.length > 0 ? v[0] : defaultValue;
        } else if (value instanceof double[][]) {
            double[][] m = (double[][]) value;
            return m.length > 0 && m[0].length > 0 ? m[0][0] : defaultValue;
        }
        return defaultValue;
    }

    public double[] getVectorPopulation(String compartment, double[] defaultValue) {
        Object value = initialPopulations.getOrDefault(compartment, defaultValue);
        if (value instanceof Double) {
            return new double[]{(Double) value};
        } else if (value instanceof double[]) {
            return (double[]) value;
        }
        return defaultValue;
    }
}