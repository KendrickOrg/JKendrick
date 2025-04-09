package jKendrick.scenarios;

import jKendrick.core.Model;
import jKendrick.core.Scenario;

import java.util.Arrays;
import java.util.List;

public class SEIRScenario extends Scenario {
    public SEIRScenario(double[] initialCardinalities, Model model) {
        super(initialCardinalities, model);
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        double S = y[0], E = y[1], I = y[2], R = y[3];
        double beta = getModel().getParameter("beta");
        double gamma = getModel().getParameter("gamma");
        double sigma = getModel().getParameter("sigma");
        double N = getModel().getParameter("population");

        yDot[0] = -beta * S * I / N;
        yDot[1] = beta * S * I / N - sigma * E;
        yDot[2] = sigma * E - gamma * I;
        yDot[3] = gamma * I;
    }

    @Override
    public List<String> getCompartments() {
        return Arrays.asList("S", "E", "I", "R");
    }

    @Override
    public List<Transition> getTransitions() {
        return Arrays.asList(
                new Transition("S", "E"),
                new Transition("E", "I"),
                new Transition("I", "R")
        );
    }
}