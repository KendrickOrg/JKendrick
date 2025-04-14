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
        inputPanel.add(new JLabel(" Taux de transmission Beta:"));
        inputPanel.add(betaField);
        inputPanel.add(new JLabel("Taux de guérison Gamma:"));
        inputPanel.add(gammaField);
        inputPanel.add(new JLabel("Taux de progression de la maladie  Sigma:"));
        inputPanel.add(sigmaField);
        inputPanel.add(new JLabel("Taux de mortalité Mu:"));
        inputPanel.add(muField);
        inputPanel.add(new JLabel("Pas de temps:"));
        inputPanel.add(stepField);
        inputPanel.add(new JLabel("Fin:"));
        inputPanel.add(endField);

        JButton runButton = new JButton("Lancer la simulation");
        runButton.addActionListener(this::runSimulation);
        runButton.setPreferredSize(new Dimension(40, 40));
        runButton.setBackground(Color.GREEN);// Taille plus grande et visible


        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(inputPanel, BorderLayout.NORTH);
        add(runButton, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimulationUI().setVisible(true));
    }
}
