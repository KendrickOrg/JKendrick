package jKendrick.solvers;

import jKendrick.core.ISolver;
import jKendrick.simulation.Scenario;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;


public class RKSolver implements ISolver {
    private FirstOrderIntegrator integrator;
    private double step; // time step
    private Scenario scenario;
    private int nbSteps;
    private double[][] result;

    public RKSolver(Scenario scenario, double step, double end) {
        this.step = step;
        this.scenario = scenario;
        this.nbSteps = (int) Math.ceil(end / step);
        this.integrator = new ClassicalRungeKuttaIntegrator(step);
    }

    @Override
    public void solve() {
        double t = 0.0;
        result = new double[nbSteps + 1][scenario.getNbCompartments()];  // +1 to account for initial state

        for (int i = 0; i <= nbSteps; t += step, ++i) {  // Ensure loop runs nbSteps + 1 times
            double[] population = scenario.getCardinalities();
            result[i] = population.clone();
            if (i < nbSteps) {
                integrator.integrate(scenario, t, population, t + step, population);
                scenario.updateParameters(population);
            }
        }
    }

    @Override
    public double[][] getResult() {
        return result;
    }
}
