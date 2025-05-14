package jKendrick.IHM.ui;

import jKendrick.core.ISolver;
import jKendrick.core.Model;
import jKendrick.core.Scenario;
import jKendrick.integration.Simulation;
import jKendrick.integration.Visualization;
import jKendrick.scenarios.*;
import jKendrick.solvers.*;

import javax.swing.*;
import java.util.Map;

public class SimulationController {
    private final SimulationUI ui;
    private final Visualization viz;
    private final Map<String, double[]> populationMap;
    private final KendrickFileGenerator fileGenerator;

    public SimulationController(SimulationUI ui, Map<String, double[]> populationMap) {
        this.ui = ui;
        this.viz = new Visualization();
        this.populationMap = populationMap;
        this.fileGenerator = new KendrickFileGenerator(ui);
    }

    public void runSimulation() {
        try {
            String selectedScenario = ui.getSelectedScenario();
            String selectedSolver = ui.getSelectedSolver();
            double beta = Double.parseDouble(ui.getBetaText());
            double gamma = Double.parseDouble(ui.getGammaText());
            double mu = Double.parseDouble(ui.getMuText());
            double sigma = Double.parseDouble(ui.getSigmaText());
            double step = Double.parseDouble(ui.getStepText());
            double end = Double.parseDouble(ui.getEndText());

            double[] population = populationMap.get(selectedScenario);
            int populationTotal = (int) java.util.Arrays.stream(population).sum();

            Model model = new Model();
            model.setParameter("beta", beta);
            model.setParameter("gamma", gamma);
            model.setParameter("mu", mu);
            model.setParameter("sigma", sigma);
            model.setParameter("population", populationTotal);

            Scenario scenario = switch (selectedScenario) {
                case "SIR" -> new SIRScenario(population.clone(), model);
                case "SIS" -> new SISScenario(population.clone(), model);
                case "SIRD" -> new SIRDScenario(population.clone(), model);
                case "SEIR" -> new SEIRScenario(population.clone(), model);
                default -> throw new IllegalArgumentException("Scénario inconnu");
            };

            ISolver solver = switch (selectedSolver) {
                case "RK4" -> new RKSolver(scenario, step, end);
                case "Gillespie" -> new Gillespie(scenario, step, end, 20);
                case "TauLeap" -> new TauLeap(scenario, step, end, 100);
                default -> throw new IllegalArgumentException("Solveur inconnu");
            };

            ui.getLogArea().append("Simulation " + selectedScenario + " avec " + selectedSolver + "\n");
            new Simulation(solver, viz, selectedScenario + " - " + selectedSolver).simulate();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(ui,
                    "Veuillez entrer des valeurs numériques valides.",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            ui.getLogArea().append("Erreur : Entrées non numériques\n");
        }
    }

    public void generateKendrickFile() {
        fileGenerator.generateKendrickFile();
    }
}