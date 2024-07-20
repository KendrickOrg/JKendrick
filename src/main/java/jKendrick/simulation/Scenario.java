package jKendrick.simulation;

import jKendrick.core.Model;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

public class Scenario implements FirstOrderDifferentialEquations {
    private double[] cardinalities;
    private Model model;

    public Scenario(double[] initialCardinalities, Model model) {
        this.cardinalities = initialCardinalities;
        this.model = model;
    }

    public double[] getCardinalities() {
        return cardinalities;
    }

    public int getNbCompartments() {
        return cardinalities.length;
    }

    public void updateParameters(double[] newCardinalities) {
        this.cardinalities = newCardinalities;
    }

    @Override
    public int getDimension() {
        return getNbCompartments();
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        double S = y[0];
        double I = y[1];
        double R = y[2];

        double beta = model.getParameter("beta");
        double gamma = model.getParameter("gamma");

        yDot[0] = -beta * S * I;
        yDot[1] = beta * S * I - gamma * I;
        yDot[2] = gamma * I;
    }
}
