package jKendrick.simulation;

import jKendrick.core.Model;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

public class Scenario implements FirstOrderDifferentialEquations {
    private double[] population;
    private Model model;

    public Scenario(double[] initialPopulation, Model model) {
        this.population = initialPopulation;
        this.model = model;
    }

    public double[] getPopulation() {
        return population;
    }

    public int getNbCompartments() {
        return population.length;
    }

    public void saveInitialParams() {
        // Save initial parameters if needed
    }

    public void updateParameters(double[] newPopulation) {
        this.population = newPopulation;
    }

    @Override
    public int getDimension() {
        return population.length;
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
