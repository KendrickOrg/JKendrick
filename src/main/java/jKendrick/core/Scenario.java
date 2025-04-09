package jKendrick.core;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

import java.util.*;

public abstract class Scenario implements FirstOrderDifferentialEquations {
    private double[] cardinalities;
    private final double[] initialCardinalities;
    private final Model model;
    private final Map<String, Integer> compartmentIndex = new HashMap<>();

    public Scenario(double[] initialCardinalities, Model model) {
        this.cardinalities = initialCardinalities;
        this.initialCardinalities = initialCardinalities;
        this.model = model;

        // Init index mapping
        List<String> compartments = getCompartments();
        for (int i = 0; i < compartments.size(); i++) {
            compartmentIndex.put(compartments.get(i), i);
        }
    }

    public double[] getCardinalities() { return cardinalities; }

    public void updateParameters(double[] newCardinalities) { this.cardinalities = newCardinalities; }

    public int getNbCompartments() { return cardinalities.length; }
    @Override
    public int getDimension() { return getNbCompartments(); }
    public Model getModel() { return model; }

    public int indexOf(String compartment) { return compartmentIndex.get(compartment); }

    public abstract List<String> getCompartments();

    public abstract List<Transition> getTransitions();

    @Override
    public abstract void computeDerivatives(double t, double[] y, double[] yDot);

    public List<Map<String, String>> getPossibleEvents() {
        List<Transition> transitions = getTransitions();
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < transitions.size(); i++) {
            result.add(new HashMap<>());
            result.get(i).put("from", transitions.get(i).from);
            result.get(i).put("to", transitions.get(i).to);
        }
        return result;
    }

    // Generic rate based on transition type
    public double getRate(String from, String to) {
        double S = cardinalities[0];
        double I = cardinalities.length > 1 ? cardinalities[1] : 0;
        double E = cardinalities.length > 2 ? cardinalities[2] : 0;

        double beta = model.getParameter("beta");
        double gamma = model.getParameter("gamma");
        double mu = model.getParameter("mu") ;
        double sigma = model.getParameter("sigma");
        double N = model.getParameter("population");

        return switch (from + "->" + to) {
            case "S->I", "S->E" -> beta * S * I / N;
            case "I->R", "I->S" -> gamma * I;
            case "I->D" -> mu * I;
            case "E->I" -> sigma * E;
            default -> 0.0;
        };
    }

    public void transition(String from, String to) {
        int fromIdx = indexOf(from), toIdx = indexOf(to);
        if (cardinalities[fromIdx] >= 1) {
            cardinalities[fromIdx] -= 1;
            cardinalities[toIdx] += 1;
        }
    }



    // Utility class to define transitions
    public static class Transition {
        public final String from, to;

        public Transition(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    public void reset() {
        updateParameters(initialCardinalities.clone());
    }
}