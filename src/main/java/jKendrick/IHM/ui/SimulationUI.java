package jKendrick.IHM.ui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SimulationUI extends JFrame {
    private JComboBox<String> scenarioBox, solverBox;
    private JTextField betaField, gammaField, sigmaField, muField, stepField, endField;
    private JTextArea logArea;
    private final SimulationController controller;
    public static final Map<String, double[]> populationMap = new HashMap<>(); // Public pour accès

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

        controller = new SimulationController(this, populationMap);

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

        JButton runButton = new JButton("Lancer la simulation");
        runButton.addActionListener(e -> controller.runSimulation());
        runButton.setBackground(Color.GREEN);

        JButton generateKendrickButton = new JButton("Générer fichier Kendrick");
        generateKendrickButton.addActionListener(e -> controller.generateKendrickFile());
        generateKendrickButton.setBackground(Color.BLUE);
        generateKendrickButton.setForeground(Color.WHITE);

        JButton importKendrickButton = new JButton("Importer fichier Kendrick");
        importKendrickButton.addActionListener(e -> controller.importKendrickFile());
        importKendrickButton.setBackground(Color.ORANGE);
        importKendrickButton.setForeground(Color.WHITE);

        JButton savePngButton = new JButton("Sauvegarder en PNG");
        savePngButton.addActionListener(e -> controller.saveChartAsPNG());
        savePngButton.setBackground(Color.BLUE);
        savePngButton.setForeground(Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(runButton);
        buttonPanel.add(generateKendrickButton);
        buttonPanel.add(importKendrickButton);
        buttonPanel.add(savePngButton);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        add(inputPanel);
        add(buttonPanel);
        add(scrollPane);
    }

    // Getters
    public String getSelectedScenario() { return (String) scenarioBox.getSelectedItem(); }
    public String getSelectedSolver() { return (String) solverBox.getSelectedItem(); }
    public String getBetaText() { return betaField.getText(); }
    public String getGammaText() { return gammaField.getText(); }
    public String getMuText() { return muField.getText(); }
    public String getSigmaText() { return sigmaField.getText(); }
    public String getStepText() { return stepField.getText(); }
    public String getEndText() { return endField.getText(); }
    public JTextArea getLogArea() { return logArea; }

    // Setters
    public void setSelectedScenario(String scenario) { scenarioBox.setSelectedItem(scenario); }
    public void setSelectedSolver(String solver) { solverBox.setSelectedItem(solver); }
    public void setBetaText(String text) { betaField.setText(text); }
    public void setGammaText(String text) { gammaField.setText(text); }
    public void setMuText(String text) { muField.setText(text); }
    public void setSigmaText(String text) { sigmaField.setText(text); }
    public void setStepText(String text) { stepField.setText(text); }
    public void setEndText(String text) { endField.setText(text); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimulationUI().setVisible(true));
    }
}