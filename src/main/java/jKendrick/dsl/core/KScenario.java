package jKendrick.dsl.core;


public class KScenario {
    private final KModel model;
    private double[] initialPop;

    public KScenario(KModel model) {
        this.model = model;
        this.initialPop = new double[model.getCompartments().size()];
    }

    public void setInitialPopulation(double... pop) {
        this.initialPop = pop;
    }

    public double[] getInitialPopulation() {
        return initialPop;
    }

    public KModel getModel() {
        return model;
    }
}
