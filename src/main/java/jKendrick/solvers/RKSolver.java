package jKendrick.solvers;

import jKendrick.core.ISolver;
import jKendrick.core.Scenario;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;

import java.util.List;


public class RKSolver implements ISolver {
    private final FirstOrderIntegrator integrator;
    private final double step;
    private final Scenario scenario;
    private final int nbSteps;
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
        double[] population = scenario.getCardinalities().clone();

        for (int i = 0; i <= nbSteps; t += step, ++i) {  // Ensure loop runs nbSteps + 1 times
            result[i] = population.clone();
            if (i < nbSteps) {
                integrator.integrate(scenario, t, population, t + step, population);
                scenario.updateParameters(population.clone());
            }
        }
    }

    @Override
    public double[][] getResult() { return result; }

    @Override
    public List<String> getLabels() { return scenario.getCompartments(); }

    @Override
    public double[] getTimes() {
        double[] times = new double[result.length];
        for (int i = 0; i < result.length; i++) {
            times[i] = i * step;
        }
        return times;
    }
}