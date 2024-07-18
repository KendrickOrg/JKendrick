package jKendrick.IHM;

import jKendrick.core.Model;
import jKendrick.simulation.Scenario;
import jKendrick.solvers.RKSolver;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        double[] initialPopulation = {0.99, 0.01, 0.0}; // Initial state: S=0.99, I=0.01, R=0.0
        double beta = 0.3;
        double gamma = 0.1;
        double end = 160.0;

        Model model = new Model(0.1, end);
        model.setParameter("beta", beta);
        model.setParameter("gamma", gamma);

        Scenario scenario = new Scenario(initialPopulation, model);

        RKSolver solver = new RKSolver(model, scenario);
        solver.solve();

        double[][] result = solver.getResult();

        List<String> compartments = Arrays.asList("Susceptible", "Infected", "Recovered");
        Visualization viz = new Visualization("Epidemic Model", "Time", "Population", compartments);
        viz.plotResults(result, model.getStep());
    }
}


