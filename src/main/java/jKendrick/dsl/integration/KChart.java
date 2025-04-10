package jKendrick.dsl.integration;

import jKendrick.dsl.core.KScenario;
import jKendrick.integration.Visualization;
import jKendrick.core.Scenario;
import jKendrick.core.Model;
import jKendrick.core.ISolver;
import jKendrick.scenarios.*;
import jKendrick.solvers.*;
import jKendrick.integration.Simulation;

import java.util.List;

public class KChart {
    private final KScenario kScenario;
    private final String solver;
    private final double from;
    private final double to;
    private final double step;
    private final String title;

    public KChart(KScenario scenario, String solver, double from, double to, double step, String title) {
        this.kScenario = scenario;
        this.solver = solver.toLowerCase();
        this.from = from;
        this.to = to;
        this.step = step;
        this.title = title;
    }

    public void show() {
        // Construction du modèle interne
        Model model = new Model();
        for (String param : kScenario.getModel().getParameters().keySet()) {
            model.setParameter(param, kScenario.getModel().getParameter(param));
        }

        double[] initialValues = kScenario.getInitialPopulation();
        List<String> compartments = kScenario.getModel().getCompartments();

        Scenario scenario;

        if (compartments.contains("E") && compartments.size() == 4) {
            scenario = new SEIRScenario(initialValues, model);
        } else if (compartments.contains("D") && compartments.size() == 4) {
            scenario = new SIRDScenario(initialValues, model);
        } else if (compartments.contains("S") && compartments.contains("I") && compartments.contains("R")) {
            scenario = new SIRScenario(initialValues, model);
        } else if (compartments.contains("S") && compartments.contains("I") && compartments.contains("S")) {
            scenario = new SISScenario(initialValues, model);
        } else {
            throw new IllegalArgumentException("Unrecognized scenario type.");
        }

        ISolver solverInstance;

        if (solver.equalsIgnoreCase("rk4") || solver.equalsIgnoreCase("rungekutta")) {
            solverInstance = new RKSolver(scenario, step, to);
        } else if (solver.equalsIgnoreCase("gillespie")) {
            solverInstance = new Gillespie(scenario, step, to, 50);
        } else if (solver.equalsIgnoreCase("tauleap")) {
            solverInstance = new TauLeap(scenario, step, to, 100);
        } else {
            throw new IllegalArgumentException("Unknown solver: " + solver);
        }

        Simulation sim = new Simulation(solverInstance, new Visualization(), title);
        sim.simulate();
    }
}
