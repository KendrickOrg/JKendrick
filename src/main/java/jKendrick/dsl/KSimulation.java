package jKendrick.dsl;

public class KSimulation {
    private String name;
    private String scenarioName;
    private String method; // e.g., "rungeKutta", "gillespie", "tauLeap"
    private double from;
    private double to;
    private double step;

    public KSimulation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public double getFrom() {
        return from;
    }

    public void setFrom(double from) {
        this.from = from;
    }

    public double getTo() {
        return to;
    }

    public void setTo(double to) {
        this.to = to;
    }

    public double getStep() {
        return step;
    }

    public void setStep(double step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "Simulation: " + name +
                " [method=" + method +
                ", from=" + from +
                ", to=" + to +
                ", step=" + step +
                ", scenario=" + scenarioName + "]";
    }
}
