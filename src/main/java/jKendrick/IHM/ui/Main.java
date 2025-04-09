package jKendrick.IHM.ui;

import jKendrick.core.ISolver;
import jKendrick.core.Model;
import jKendrick.core.Scenario;
import jKendrick.integration.Simulation;
import jKendrick.integration.Visualization;
import jKendrick.scenarios.SEIRScenario;
import jKendrick.scenarios.SIRDScenario;
import jKendrick.scenarios.SIRScenario;
import jKendrick.scenarios.SISScenario;
import jKendrick.solvers.Gillespie;
import jKendrick.solvers.RKSolver;
import jKendrick.solvers.TauLeap;

public class Main {

    private static final double[] SIRPopulation = {900, 100, 0};
    private static final  double[] SISPopulation = {999, 1, 0};
    private static final  double[] SIRDPopulation = {999, 1, 0, 0};
    private static final  double[] SEIRPopulation = {990, 5, 5, 0};

    public static void main(String[] args) {
        // Initial Parameters
        double beta = 0.3, gamma = 0.1, step = 0.1, end = 160.0, mu = 0.01, sigma = 0.2;
        int population = 1000, gillespieRuns = 20, tauLeapRuns = 100;

        // Model
        Model model = new Model();
        model.setParameter("beta", beta);
        model.setParameter("gamma", gamma);
        model.setParameter("population", population);
        model.setParameter("mu", mu);
        model.setParameter("sigma", sigma);

        Visualization viz = new Visualization();

        // Simulation
        runModel("SIR", new SIRScenario(SIRPopulation, model), model, step, end, gillespieRuns, tauLeapRuns, viz);
        runModel("SIS", new SISScenario(SISPopulation, model), model, step, end, gillespieRuns, tauLeapRuns, viz);
        runModel("SIRD", new SIRDScenario(SIRDPopulation, model), model, step, end, gillespieRuns, tauLeapRuns, viz);
        runModel("SEIR", new SEIRScenario(SEIRPopulation, model), model, step, end, gillespieRuns, tauLeapRuns, viz);

    }

    public static void runModel(String label, Scenario scenario, Model model,
                                double step, double end, int gillespieRuns, int tauLeapRuns, Visualization viz) {

        // RK4
        Scenario rk4Scenario = cloneScenario(scenario, model);
        ISolver rk4 = new RKSolver(rk4Scenario, step, end);
        new Simulation(rk4, viz, label + " - RK4 (Determinist)").simulate();

        // Gillespie multi-run
        Scenario gillespieScenario = cloneScenario(scenario, model);
        Gillespie gillespie = new Gillespie(gillespieScenario, step, end, gillespieRuns);
        new Simulation(gillespie, viz, label + " - Gillespie (Multi-run)").simulate();

        // TauLeap multi-run
        Scenario tauScenario = cloneScenario(scenario, model);
        TauLeap tauLeap = new TauLeap(tauScenario, step, end, tauLeapRuns);
        new Simulation(tauLeap, viz, label + " - TauLeap (Multi-run)").simulate();

        System.out.println("========== Scénario " + label + " lancé avec population initiale : " + scenario.getCompartments().toString());
    }

    // Pour cloner les scénario et éviter la modification sur un même scénario
    public static Scenario cloneScenario(Scenario scenario, Model model) {
        System.out.println("=== Clonage de " + scenario.getClass().getSimpleName() + " avec : " + java.util.Arrays.toString(scenario.getCardinalities().clone()));

        if (scenario instanceof SIRScenario) return new SIRScenario(SIRPopulation.clone(), model);
        if (scenario instanceof SISScenario) return new SISScenario(SISPopulation.clone(), model);
        if (scenario instanceof SIRDScenario) return new SIRDScenario(SIRDPopulation.clone(), model);
        if (scenario instanceof SEIRScenario) return new SEIRScenario(SEIRPopulation.clone(), model);
        throw new IllegalArgumentException("Scénario non reconnu");
    }
}
