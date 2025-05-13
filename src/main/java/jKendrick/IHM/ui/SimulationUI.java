package jKendrick.IHM.ui;

import jKendrick.core.ISolver;
import jKendrick.core.Model;
import jKendrick.core.Scenario;
import jKendrick.integration.Simulation;
import jKendrick.integration.Visualization;
import jKendrick.scenarios.*;
import jKendrick.solvers.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SimulationUI extends JFrame {
    private JComboBox<String> scenarioBox, solverBox;
    private JTextField betaField, gammaField, sigmaField, muField, stepField, endField;
    private JTextArea logArea;
    private final Visualization viz = new Visualization();

    private static final Map<String, double[]> populationMap = new HashMap<>();

    static {
        populationMap.put("SIR", new double[]{900, 100, 0});
        populationMap.put("SIS", new double[]{999, 1, 0});
        populationMap.put("SIRD", new double[]{999, 1, 0, 0});
        populationMap.put("SEIR", new double[]{990, 5, 5, 0});
    }

    public SimulationUI() {
        setTitle("Simulateur Epidémiologique - jKendrick");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panneau pour les champs de saisie
        JPanel inputPanel = new JPanel(new GridLayout(9, 2));
        scenarioBox = new JComboBox<>(new String[]{"SIR", "SIS", "SIRD", "SEIR"});
        solverBox = new JComboBox<>(new String[]{"RK4", "Gillespie", "TauLeap"});

        betaField = new JTextField("0.3");
        gammaField = new JTextField("0.1");
        sigmaField = new JTextField("0.2");
        muField = new JTextField("0.01");
        stepField = new JTextField("0.1");
        endField = new JTextField("160");

        inputPanel.add(new JLabel("Scénario:"));
        inputPanel.add(scenarioBox);
        inputPanel.add(new JLabel("Solveur:"));
        inputPanel.add(solverBox);
        inputPanel.add(new JLabel("Taux de transmission Beta:"));
        inputPanel.add(betaField);
        inputPanel.add(new JLabel("Taux de guérison Gamma:"));
        inputPanel.add(gammaField);
        inputPanel.add(new JLabel("Taux de progression de la maladie Sigma:"));
        inputPanel.add(sigmaField);
        inputPanel.add(new JLabel("Taux de mortalité Mu:"));
        inputPanel.add(muField);
        inputPanel.add(new JLabel("Pas de temps:"));
        inputPanel.add(stepField);
        inputPanel.add(new JLabel("Fin:"));
        inputPanel.add(endField);

        // Bouton pour lancer la simulation
        JButton runButton = new JButton("Lancer la simulation");
        runButton.addActionListener(this::runSimulation);
        runButton.setBackground(Color.GREEN);

        // Bouton pour générer le fichier Kendrick
        JButton generateKendrickButton = new JButton("Générer fichier Kendrick");
        generateKendrickButton.addActionListener(this::generateKendrickFile);
        generateKendrickButton.setBackground(Color.BLUE);
        generateKendrickButton.setForeground(Color.WHITE);

        // Panneau pour les boutons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(runButton);
        buttonPanel.add(generateKendrickButton);

        // Zone de journalisation

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Disposition de l'interface
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        add(inputPanel);
        add(buttonPanel);
        add(scrollPane);
    }

    private void runSimulation(ActionEvent e) {
        String selectedScenario = (String) scenarioBox.getSelectedItem();
        String selectedSolver = (String) solverBox.getSelectedItem();

        double beta = Double.parseDouble(betaField.getText());
        double gamma = Double.parseDouble(gammaField.getText());
        double mu = Double.parseDouble(muField.getText());
        double sigma = Double.parseDouble(sigmaField.getText());
        double step = Double.parseDouble(stepField.getText());
        double end = Double.parseDouble(endField.getText());

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

        logArea.append("Simulation " + selectedScenario + " avec " + selectedSolver + "\n");
        new Simulation(solver, viz, selectedScenario + " - " + selectedSolver).simulate();
    }

    private void generateKendrickFile(ActionEvent e) {
        // Étape 2 : Collecter et valider les données
        String selectedScenario = (String) scenarioBox.getSelectedItem();
        String selectedSolver = (String) solverBox.getSelectedItem();

        double beta, gamma, mu, sigma, step, end;
        try {
            beta = Double.parseDouble(betaField.getText());
            gamma = Double.parseDouble(gammaField.getText());
            mu = Double.parseDouble(muField.getText());
            sigma = Double.parseDouble(sigmaField.getText());
            step = Double.parseDouble(stepField.getText());
            end = Double.parseDouble(endField.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez entrer des valeurs numériques valides.",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            logArea.append("Erreur : Entrées non numériques\n");
            return;
        }

        double[] population = populationMap.get(selectedScenario);

        // Étape 3 : Générer le code Kendrick
        String kendrickCode = generateKendrickCode(
                selectedScenario, selectedSolver, beta, gamma, mu, sigma, step, end, population);

        // Étape 4 : Sauvegarder dans un fichier
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Sauvegarder le fichier Kendrick");
        fileChooser.setSelectedFile(new File(selectedScenario + "_" + selectedSolver + ".kendrick"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".kendrick")) {
                file = new File(file.getAbsolutePath() + ".kendrick");
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(kendrickCode);
                logArea.append("Fichier Kendrick sauvegardé : " + file.getAbsolutePath() + "\n");
            } catch (IOException ex) {
                logArea.append("Erreur lors de la sauvegarde : " + ex.getMessage() + "\n");
                JOptionPane.showMessageDialog(this,
                        "Erreur lors de la sauvegarde du fichier.",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String generateKendrickCode(String scenario, String solver,
                                        double beta, double gamma, double mu, double sigma,
                                        double step, double end, double[] population) {
        StringBuilder sb = new StringBuilder();

        // Commentaire d'en-tête
        sb.append("# Modèle épidémiologique ").append(scenario).append("\n\n");

        // Définir le modèle
        sb.append("model ").append(scenario).append("\n");

        // Compartiments
        sb.append("  compartments {\n");
        switch (scenario) {
            case "SIR":
                sb.append("    S, I, R\n");
                break;
            case "SIS":
                sb.append("    S, I\n");
                break;
            case "SIRD":
                sb.append("    S, I, R, D\n");
                break;
            case "SEIR":
                sb.append("    S, E, I, R\n");
                break;
        }
        sb.append("  }\n\n");

        // Paramètres
        sb.append("  parameters {\n");
        sb.append("    beta = ").append(String.format("%.3f", beta)).append("\n");
        sb.append("    gamma = ").append(String.format("%.3f", gamma)).append("\n");
        if (scenario.equals("SIRD")) {
            sb.append("    mu = ").append(String.format("%.3f", mu)).append("\n");
        }
        if (scenario.equals("SEIR")) {
            sb.append("    sigma = ").append(String.format("%.3f", sigma)).append("\n");
        }
        sb.append("  }\n\n");

        // Populations initiales
        sb.append("  initial {\n");
        switch (scenario) {
            case "SIR":
            case "SIS":
                sb.append("    S = ").append((int) population[0]).append("\n");
                sb.append("    I = ").append((int) population[1]).append("\n");
                if (scenario.equals("SIR")) {
                    sb.append("    R = ").append((int) population[2]).append("\n");
                }
                break;
            case "SIRD":
                sb.append("    S = ").append((int) population[0]).append("\n");
                sb.append("    I = ").append((int) population[1]).append("\n");
                sb.append("    R = ").append((int) population[2]).append("\n");
                sb.append("    D = ").append((int) population[3]).append("\n");
                break;
            case "SEIR":
                sb.append("    S = ").append((int) population[0]).append("\n");
                sb.append("    E = ").append((int) population[1]).append("\n");
                sb.append("    I = ").append((int) population[2]).append("\n");
                sb.append("    R = ").append((int) population[3]).append("\n");
                break;
        }
        sb.append("  }\n\n");

        // Transitions
        sb.append("  transitions {\n");
        switch (scenario) {
            case "SIR":
                sb.append("    S -> I : beta * S * I / population\n");
                sb.append("    I -> R : gamma * I\n");
                break;
            case "SIS":
                sb.append("    S -> I : beta * S * I / population\n");
                sb.append("    I -> S : gamma * I\n");
                break;
            case "SIRD":
                sb.append("    S -> I : beta * S * I / population\n");
                sb.append("    I -> R : gamma * I\n");
                sb.append("    I -> D : mu * I\n");
                break;
            case "SEIR":
                sb.append("    S -> E : beta * S * I / population\n");
                sb.append("    E -> I : sigma * E\n");
                sb.append("    I -> R : gamma * I\n");
                break;
        }
        sb.append("  }\n\n");

        // Simulation
        sb.append("simulation ").append(scenario).append("_").append(solver).append("\n");
        sb.append("  solver = ").append(solver).append("\n");
        sb.append("  step = ").append(String.format("%.3f", step)).append("\n");
        sb.append("  end = ").append(String.format("%.1f", end)).append("\n");
        if (solver.equals("Gillespie")) {
            sb.append("  runs = 20\n");
        } else if (solver.equals("TauLeap")) {
            sb.append("  runs = 100\n");
        }
        sb.append("end\n");

        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimulationUI().setVisible(true));
    }
}