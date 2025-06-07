package jKendrick.IHM.ui;

import jKendrick.IHM.ui.KendrickFile.KendrickFileGenerator;
import jKendrick.IHM.ui.KendrickFile.KendrickFileParser;
import jKendrick.IHM.ui.KendrickFile.KendrickModelData;
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
    private XYChart lastChart;
    private double[] currentIValues;
    private double currentN;
    private double[] lastValues; // Pour stocker les dernières valeurs des groupes

    public SimulationController(SimulationUI ui, Map<String, double[]> populationMap) {
        this.ui = ui;
        this.viz = new Visualization();
        this.populationMap = populationMap;
        this.fileGenerator = new KendrickFileGenerator(ui);
        this.fileParser = new KendrickFileParser();
        this.currentIValues = new double[]{populationMap.get("SIR")[1]}; // I initial pour SIR
        this.currentN = Arrays.stream(populationMap.get("SIR")).sum(); // N total
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
            currentN = Arrays.stream(population).sum();

            Model model = new Model();
            model.setParameter("beta", beta);
            model.setParameter("gamma", gamma);
            model.setParameter("mu", mu);
            model.setParameter("sigma", sigma);
            model.setParameter("population", currentN);

            // Choisir le scénario selon le modèle
            Scenario scenario;
            int iIndex; // Index de I dans les résultats
            String[] labels;
            if ("SIR".equals(selectedScenario)) {
                scenario = new SIRScenario(population.clone(), model);
                iIndex = 1; // I est à l'index 1 dans SIR
                labels = new String[]{"S", "I", "R"};
            } else if ("SEIR".equals(selectedScenario)) {
                scenario = new SEIRScenario(population.clone(), model);
                iIndex = 2; // I est à l'index 2 dans SEIR
                labels = new String[]{"S", "E", "I", "R"};
            } else if ("SIS".equals(selectedScenario)) {
                scenario = new SISScenario(population.clone(), model);
                iIndex = 1; // I est à l'index 1 dans SIS
                labels = new String[]{"S", "I"};
            } else {
                throw new IllegalArgumentException("Scénario inconnu");
            }

            ISolver solver = switch (selectedSolver) {
                case "RK4" -> new RKSolver(scenario, step, end);
                case "Gillespie" -> new Gillespie(scenario, step, end, 20);
                case "TauLeap" -> new TauLeap(scenario, step, end, 100);
                default -> throw new IllegalArgumentException("Solveur inconnu");
            };

            ui.getLogArea().append("Simulation " + selectedScenario + " avec " + selectedSolver + "\n");
            Simulation simulation = new Simulation(solver, viz, selectedScenario + " - " + selectedSolver);
            simulation.simulate();

            // Mettre à jour currentIValues et lastValues
            double[][] results = solver.getResult();
            currentIValues = new double[results.length];
            lastValues = new double[labels.length];
            for (int i = 0; i < results.length; i++) {
                currentIValues[i] = results[i][iIndex];
            }
            // Récupérer les dernières valeurs de chaque groupe
            for (int j = 0; j < labels.length; j++) {
                lastValues[j] = results[results.length - 1][j];
            }

            // Afficher les dernières valeurs dans infoArea
            StringBuilder infoText = new StringBuilder("Simulation terminée.\n");
            for (int j = 0; j < labels.length; j++) {
                infoText.append(labels[j]).append(" = ").append(String.format("%.0f", lastValues[j])).append("\n");
            }
            ui.getInfoArea().setText(infoText.toString());

            // Générer le graphique
            String title = selectedScenario + " - " + selectedSolver;
            XYChart chart = viz.buildChart(title);
            if (solver.isStochastic()) {
                double[][][] multiRunResults = solver.getMultiRunResult();
                double[] xData = solver.getTimes();
                java.util.List<String> labelsList = solver.getLabels();
                int nbRuns = multiRunResults.length;
                int nbSteps = multiRunResults[0].length;

                for (int run = 0; run < nbRuns; run++) {
                    for (int c = 0; c < labelsList.size(); c++) {
                        double[] yData = new double[nbSteps];
                        for (int t = 0; t < nbSteps; t++) {
                            yData[t] = multiRunResults[run][t][c];
                        }
                        XYSeries s = chart.addSeries(labelsList.get(c) + "-run" + run, xData, yData);
                        s.setLineColor(new Color(100, 100, 100, 20));
                        s.setMarkerColor(new Color(100, 100, 100, 20));
                        s.setShowInLegend(false);
                    }
                }

                int medianRun = nbRuns - 1;
                for (int c = 0; c < labelsList.size(); c++) {
                    double[] yData = new double[nbSteps];
                    for (int t = 0; t < nbSteps; t++) {
                        yData[t] = multiRunResults[medianRun][t][c];
                    }
                    chart.addSeries(labelsList.get(c), xData, yData)
                            .setLineColor(viz.getColor(labelsList.get(c)));
                }
            } else {
                double[][] resultsDet = solver.getResult();
                double[] xData = solver.getTimes();
                java.util.List<String> labelsList = solver.getLabels();
                for (int i = 0; i < labelsList.size(); i++) {
                    double[] yData = new double[resultsDet.length];
                    for (int j = 0; j < resultsDet.length; j++) {
                        yData[j] = resultsDet[j][i];
                    }
                    chart.addSeries(labelsList.get(i), xData, yData)
                            .setLineColor(viz.getColor(labelsList.get(i)));
                }
            }

            lastChart = chart;
            ui.getLogArea().append("Graphique généré : " + title + "\n");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(ui,
                    "Veuillez entrer des valeurs numériques valides (utilisez un point comme séparateur décimal).",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            ui.getLogArea().append("Erreur : Entrées non numériques\n");
        } catch (Exception ex) {
            ui.getLogArea().append("Erreur lors de la simulation : " + ex.getMessage() + "\n");
            JOptionPane.showMessageDialog(ui, "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
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
        if (!Arrays.asList("SIR", "SEIR", "SIS").contains(scenario)) {
            ui.getLogArea().append("Erreur : Scénario inconnu (" + scenario + ")\n");
            return;
        }
        ui.setSelectedScenario(scenario);

        String solver = data.getSolver();
        if (!Arrays.asList("RK4", "Gillespie", "TauLeap").contains(solver)) {
            ui.getLogArea().append("Avertissement : Solveur inconnu (" + solver + "), RK4 sélectionné\n");
            solver = "RK4";
        }
        ui.setSelectedSolver(solver);

        Map<String, Object> params = data.getParameters();
        Object betaObj = params.get("beta");
        if (betaObj instanceof double[] || betaObj instanceof double[][]) {
            ui.getLogArea().append("Erreur : Paramètres composites non supportés\n");
            return;
        }
        ui.setBetaText(String.format(Locale.US, "%.3f", data.getScalarParameter("beta", 0.3)));
        ui.setGammaText(String.format(Locale.US, "%.3f", data.getScalarParameter("gamma", 0.1)));
        ui.setMuText(String.format(Locale.US, "%.3f", data.getScalarParameter("mu", 0.01)));
        ui.setSigmaText(String.format(Locale.US, "%.3f", data.getScalarParameter("sigma", 0.2)));
        ui.setStepText(String.format(Locale.US, "%.3f", data.getStep()));
        ui.setEndText(String.format(Locale.US, "%.1f", data.getEnd()));

        Map<String, Object> initialPop = data.getInitialPopulations();
        double[] population;
        if ("SIR".equals(scenario)) {
            population = new double[]{
                    data.getVectorPopulation("S", new double[]{900})[0],
                    data.getVectorPopulation("I", new double[]{100})[0],
                    data.getVectorPopulation("R", new double[]{0})[0]
            };
        } else if ("SEIR".equals(scenario)) {
            population = new double[]{
                    data.getVectorPopulation("S", new double[]{800})[0],
                    data.getVectorPopulation("E", new double[]{100})[0],
                    data.getVectorPopulation("I", new double[]{50})[0],
                    data.getVectorPopulation("R", new double[]{0})[0]
            };
        } else if ("SIS".equals(scenario)) {
            population = new double[]{
                    data.getVectorPopulation("S", new double[]{900})[0],
                    data.getVectorPopulation("I", new double[]{100})[0]
            };
        } else {
            return;
        }
        Object sPop = initialPop.get("S");
        if (sPop instanceof double[] && ((double[]) sPop).length > 1) {
            ui.getLogArea().append("Erreur : Populations composites non supportées\n");
            return;
        }
        populationMap.put(scenario, population);
    }

    public double[] getCurrentIValues() {
        return currentIValues;
    }

    public double getCurrentN() {
        return currentN;
    }
}