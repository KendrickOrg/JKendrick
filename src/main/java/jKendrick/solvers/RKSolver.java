package jKendrick.solvers;

import jKendrick.core.ISolver;
import jKendrick.core.Model;
import jKendrick.simulation.Scenario;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;

public class RKSolver implements ISolver {
    private FirstOrderIntegrator integrator;
    private double step;
    private double end;
    private Scenario scenario;
    private int nbSteps;
    private double[][] result;

    public RKSolver(Model model, Scenario scenario) {
        this.step = model.getStep();
        this.end = model.getEnd();
        this.scenario = scenario;
        this.nbSteps = (int) Math.ceil(end / step);
        this.integrator = new ClassicalRungeKuttaIntegrator(step);
    }

    @Override
    public void solve() {
        scenario.saveInitialParams();
        double t = 0.0;
        result = new double[nbSteps + 1][scenario.getNbCompartments()];  // +1 to account for initial state

        for (int i = 0; i <= nbSteps; t += step, ++i) {  // Ensure loop runs nbSteps + 1 times
            double[] population = scenario.getPopulation();
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
