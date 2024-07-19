package jKendrick.IHM;

import jKendrick.core.Model;
import jKendrick.simulation.Scenario;
import jKendrick.simulation.Simulation;
import jKendrick.solvers.RKSolver;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        double[] initialPopulation = {0.99, 0.01, 0.0}; // Initial state: S=0.99, I=0.01, R=0.0
        double beta = 0.3;
        double gamma = 0.1;
        double step = 0.1;
        double end = 160.0;

        Model model = new Model();
        model.setParameter("beta", beta);
        model.setParameter("gamma", gamma);

        Scenario scenario = new Scenario(initialPopulation, model);

        RKSolver solver = new RKSolver(scenario, step, end);

        List<String> compartments = Arrays.asList("Susceptible", "Infected", "Recovered");
        Visualization viz = new Visualization("Epidemic Model", "Time", "Population", compartments);

        Simulation simulation = new Simulation(solver, viz, "Epidemic Model Simulation", step, end);
        simulation.simulate();

        // Print the results every 10 units of time
        double[][] results = solver.getResult();
        for (int i = 0; i < results.length; i++) {
            double time = i * step;
            if (time % 10 == 0) {
                System.out.printf("t = %.1f S = %.4f I = %.4f R = %.4f%n", time, results[i][0], results[i][1], results[i][2]);
            }
        }
    }
}

