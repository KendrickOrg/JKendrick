package jKendrick.IHM.ui;

import jKendrick.core.ISolver;
import jKendrick.core.Model;
import jKendrick.core.Scenario;
import jKendrick.integration.Simulation;
import jKendrick.integration.Visualization;
import jKendrick.scenarios.*;
import jKendrick.solvers.*;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYSeries;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class SimulationController {
    private final SimulationUI ui;
    private final Visualization viz;
    private final Map<String, double[]> populationMap;
    private final KendrickFileGenerator fileGenerator;
    private final KendrickFileParser fileParser;
    private XYChart lastChart; // Pour stocker le graphique

    public SimulationController(SimulationUI ui, Map<String, double[]> populationMap) {
        this.ui = ui;
        this.viz = new Visualization();
        this.populationMap = populationMap;
        this.fileGenerator = new KendrickFileGenerator(ui);
        this.fileParser = new KendrickFileParser();
    }

    public void runSimulation() {
        try {
            String selectedScenario = ui.getSelectedScenario();
            String selectedSolver = ui.getSelectedSolver();
            double beta = Double.parseDouble(ui.getBetaText().replace(",", "."));
            double gamma = Double.parseDouble(ui.getGammaText().replace(",", "."));
            double mu = Double.parseDouble(ui.getMuText().replace(",", "."));
            double sigma = Double.parseDouble(ui.getSigmaText().replace(",", "."));
            double step = Double.parseDouble(ui.getStepText().replace(",", "."));
            double end = Double.parseDouble(ui.getEndText().replace(",", "."));

            double[] population = populationMap.get(selectedScenario);
            int populationTotal = (int) Arrays.stream(population).sum();

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
            Simulation simulation = new Simulation(solver, viz, selectedScenario + " - " + selectedSolver);
            simulation.simulate();

            // Générer le graphique
            String title = selectedScenario + " - " + selectedSolver;
            XYChart chart = viz.buildChart(title);
            if (solver.isStochastic()) {
                // Logique stochastique (inspirée de createStochasticChart)
                double[][][] multiRunResults = solver.getMultiRunResult();
                double[] xData = solver.getTimes();
                java.util.List<String> labels = solver.getLabels();
                int nbRuns = multiRunResults.length;
                int nbSteps = multiRunResults[0].length;

                // Runs transparents
                for (int run = 0; run < nbRuns; run++) {
                    for (int c = 0; c < labels.size(); c++) {
                        double[] yData = new double[nbSteps];
                        for (int t = 0; t < nbSteps; t++) {
                            yData[t] = multiRunResults[run][t][c];
                        }
                        XYSeries s = chart.addSeries(labels.get(c) + "-run" + run, xData, yData);
                        s.setLineColor(new Color(100, 100, 100, 20));
                        s.setMarkerColor(new Color(100, 100, 100, 20));
                        s.setShowInLegend(false);
                    }
                }

                // Chemin médian
                int medianRun = nbRuns - 1;
                for (int c = 0; c < labels.size(); c++) {
                    double[] yData = new double[nbSteps];
                    for (int t = 0; t < nbSteps; t++) {
                        yData[t] = multiRunResults[medianRun][t][c];
                    }
                    chart.addSeries(labels.get(c), xData, yData)
                            .setLineColor(viz.getColor(labels.get(c)));
                }
            } else {
                // Logique déterministe
                double[][] results = solver.getResult();
                double[] xData = solver.getTimes();
                java.util.List<String> labels = solver.getLabels();
                for (int i = 0; i < labels.size(); i++) {
                    double[] yData = new double[results.length];
                    for (int j = 0; j < results.length; j++) {
                        yData[j] = results[j][i];
                    }
                    chart.addSeries(labels.get(i), xData, yData)
                            .setLineColor(viz.getColor(labels.get(i)));
                }
            }

            // Stocker le graphique
            lastChart = chart;
            ui.getLogArea().append("Graphique généré : " + title + "\n");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(ui,
                    "Veuillez entrer des valeurs numériques valides (utilisez un point comme séparateur décimal).",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            ui.getLogArea().append("Erreur : Entrées non numériques\n");
        }
    }

    public void generateKendrickFile() {
        fileGenerator.generateKendrickFile();
    }

    public void importKendrickFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Importer un fichier Kendrick");

        if (fileChooser.showOpenDialog(ui) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                KendrickModelData data = fileParser.parseFile(file.getAbsolutePath());
                updateUIFromData(data);
                ui.getLogArea().append("Fichier Kendrick importé : " + file.getAbsolutePath() + "\n");
            } catch (IOException ex) {
                ui.getLogArea().append("Erreur lors de l'importation : " + ex.getMessage() + "\n");
                JOptionPane.showMessageDialog(ui,
                        "Erreur lors de l'importation du fichier : " + ex.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void saveChartAsPNG() {
        if (lastChart == null) {
            JOptionPane.showMessageDialog(ui, "Aucune simulation n'a été exécutée.", "Erreur", JOptionPane.ERROR_MESSAGE);
            ui.getLogArea().append("Erreur : Aucune simulation exécutée\n");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Sauvegarder le graphique en PNG");
        fileChooser.setSelectedFile(new File(lastChart.getTitle() + ".png"));

        if (fileChooser.showSaveDialog(ui) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }
                BitmapEncoder.saveBitmap(lastChart, file.getAbsolutePath(), BitmapEncoder.BitmapFormat.PNG);
                ui.getLogArea().append("Graphique sauvegardé : " + file.getAbsolutePath() + "\n");
            } catch (IOException ex) {
                ui.getLogArea().append("Erreur lors de la sauvegarde : " + ex.getMessage() + "\n");
                JOptionPane.showMessageDialog(ui, "Erreur lors de la sauvegarde du graphique.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateUIFromData(KendrickModelData data) {
        String scenario = data.getScenario();
        if (!Arrays.asList("SIR", "SIS", "SIRD", "SEIR").contains(scenario)) {
            ui.getLogArea().append("Avertissement : Scénario inconnu (" + scenario + "), SIR sélectionné\n");
            scenario = "SIR";
        }
        ui.setSelectedScenario(scenario);

        String solver = data.getSolver();
        if (!Arrays.asList("RK4", "Gillespie", "TauLeap").contains(solver)) {
            ui.getLogArea().append("Avertissement : Solveur inconnu (" + solver + "), RK4 sélectionné\n");
            solver = "RK4";
        }
        ui.setSelectedSolver(solver);

        // Extraire des scalaires pour les champs, avec avertissement pour les données composites
        Map<String, Object> params = data.getParameters();
        // Vérification pour beta
        Object betaObj = params.get("beta");
        if (betaObj instanceof double[] || betaObj instanceof double[][]) {
            ui.getLogArea().append("Avertissement : beta est un vecteur/matrice, première valeur utilisée\n");
        }
        // Vérification pour gamma
        Object gammaObj = params.get("gamma");
        if (gammaObj instanceof double[] || gammaObj instanceof double[][]) {
            ui.getLogArea().append("Avertissement : gamma est un vecteur/matrice, première valeur utilisée\n");
        }
        ui.setBetaText(String.format(Locale.US, "%.3f", data.getScalarParameter("beta", 0.3)));
        ui.setGammaText(String.format(Locale.US, "%.3f", data.getScalarParameter("gamma", 0.1)));
        ui.setMuText(String.format(Locale.US, "%.3f", data.getScalarParameter("mu", 0.01)));
        ui.setSigmaText(String.format(Locale.US, "%.3f", data.getScalarParameter("sigma", 0.2)));
        ui.setStepText(String.format(Locale.US, "%.3f", data.getStep()));
        ui.setEndText(String.format(Locale.US, "%.1f", data.getEnd()));

        // Mettre à jour populationMap avec les vecteurs
        Map<String, Object> initialPop = data.getInitialPopulations();
        double[] population;
        switch (scenario) {
            case "SIR":
                population = new double[]{
                        data.getVectorPopulation("S", new double[]{900})[0],
                        data.getVectorPopulation("I", new double[]{100})[0],
                        data.getVectorPopulation("R", new double[]{0})[0]
                };
                Object sPop = initialPop.get("S");
                if (sPop instanceof double[] && ((double[]) sPop).length > 1) {
                    ui.getLogArea().append("Avertissement : Populations initiales composites, premières valeurs utilisées\n");
                }
                break;
            case "SIS":
                population = new double[]{
                        data.getVectorPopulation("S", new double[]{999})[0],
                        data.getVectorPopulation("I", new double[]{1})[0]
                };
                sPop = initialPop.get("S");
                if (sPop instanceof double[] && ((double[]) sPop).length > 1) {
                    ui.getLogArea().append("Avertissement : Populations initiales composites, premières valeurs utilisées\n");
                }
                break;
            case "SIRD":
                population = new double[]{
                        data.getVectorPopulation("S", new double[]{999})[0],
                        data.getVectorPopulation("I", new double[]{1})[0],
                        data.getVectorPopulation("R", new double[]{0})[0],
                        data.getVectorPopulation("D", new double[]{0})[0]
                };
                sPop = initialPop.get("S");
                if (sPop instanceof double[] && ((double[]) sPop).length > 1) {
                    ui.getLogArea().append("Avertissement : Populations initiales composites, premières valeurs utilisées\n");
                }
                break;
            case "SEIR":
                population = new double[]{
                        data.getVectorPopulation("S", new double[]{990})[0],
                        data.getVectorPopulation("E", new double[]{5})[0],
                        data.getVectorPopulation("I", new double[]{5})[0],
                        data.getVectorPopulation("R", new double[]{0})[0]
                };
                sPop = initialPop.get("S");
                if (sPop instanceof double[] && ((double[]) sPop).length > 1) {
                    ui.getLogArea().append("Avertissement : Populations initiales composites, premières valeurs utilisées\n");
                }
                break;
            default:
                population = populationMap.get("SIR");
        }
        populationMap.put(scenario, population);
    }
}