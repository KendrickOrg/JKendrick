package jKendrick.scenarios;

import jKendrick.core.Model;
import jKendrick.core.Scenario;

import java.util.Arrays;
import java.util.List;

public class SISScenario extends Scenario {

    public SISScenario(double[] initialCardinalities, Model model) {
        super(initialCardinalities, model);
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        double S = y[0], I = y[1];
        double beta = getModel().getParameter("beta");
        double gamma = getModel().getParameter("gamma");
        double N = getModel().getParameter("population");

        yDot[0] = -beta * S * I / N + gamma * I;
        yDot[1] = beta * S * I / N - gamma * I;
    }

    @Override
    public List<String> getCompartments() {
        return Arrays.asList("S", "I");
    }

    @Override
    public List<Transition> getTransitions() {
        return Arrays.asList(
                new Transition("S", "I"),
                new Transition("I", "S")
        );
    }
}
