package jKendrick.dsl.integration;

import jKendrick.core.Model;
import jKendrick.core.Scenario;
import jKendrick.dsl.core.KModel;
import jKendrick.dsl.core.KScenario;
import jKendrick.scenarios.SIRScenario;
import jKendrick.integration.Simulation;
import jKendrick.solvers.RKSolver;
import jKendrick.integration.Visualization;

import java.util.Map;

public class KSimulation {
    private final KScenario kScenario;
    private final double step;
    private final double end;

    public KSimulation(KScenario kScenario, double step, double end) {
        this.kScenario = kScenario;
        this.step = step;
        this.end = end;
    }

    public void run() {
        KModel km = kScenario.getModel();
        Model m = new Model();
        for (Map.Entry<String, Double> entry : km.getParameters().entrySet()) {
            m.setParameter(entry.getKey(), entry.getValue());
        }

        Scenario scenario = new SIRScenario(kScenario.getInitialPopulation(), m);
        RKSolver solver = new RKSolver(scenario, step, end);
        Visualization viz = new Visualization();
        Simulation sim = new Simulation(solver, viz, "DSL-SIR");
        sim.simulate();
    }
}
