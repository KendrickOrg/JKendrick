package jKendrick.IHM.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class SimulationUI extends JFrame {
    private JComboBox<String> scenarioBox, solverBox;
    private JTextField betaField, gammaField, sigmaField, muField, stepField, endField;
    private JTextArea logArea;
    private JTextArea infoArea;
    private JPanel graphPanel;
    private final SimulationController controller;
    public static final Map<String, double[]> populationMap = new HashMap<>();

    static {
        populationMap.put("SIR", new double[]{900, 100, 0}); // S=900, I=100, R=0
        populationMap.put("SEIR", new double[]{800, 100, 50, 0}); // S=800, E=100, I=50, R=0
        populationMap.put("SIS", new double[]{900, 100}); // S=900, I=100
    }

    public SimulationUI() {
        setTitle("Simulateur Epidémiologique - jKendrick");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        controller = new SimulationController(this, populationMap);

        // Panneau pour les choix
        JPanel inputPanel = new JPanel(new GridLayout(9, 2));
        scenarioBox = new JComboBox<>(new String[]{"SIR", "SEIR", "SIS"});
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
        inputPanel.add(new JLabel("Taux de progression Sigma:"));
        inputPanel.add(sigmaField);
        inputPanel.add(new JLabel("Taux de mortalité Mu:"));
        inputPanel.add(muField);
        inputPanel.add(new JLabel("Pas de temps:"));
        inputPanel.add(stepField);
        inputPanel.add(new JLabel("Fin:"));
        inputPanel.add(endField);

        // Boutons
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

        // Utiliser un JPanel avec FlowLayout et wrap pour les boutons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Ajouter un padding
        buttonPanel.add(runButton);
        buttonPanel.add(generateKendrickButton);
        buttonPanel.add(importKendrickButton);
        buttonPanel.add(savePngButton); // Bouton PNG bien ajouté

        // Panneau pour dessiner le schéma selon le modèle choisi
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                String selectedScenario = (String) scenarioBox.getSelectedItem();
                int centerX = 150, centerY = 150, radius = 30;

                if ("SIR".equals(selectedScenario)) {
                    // Dessiner 3 ronds pour SIR (S, I, R)
                    String[] states = {"S", "I", "R"};
                    int[] xOffsets = {0, 100, 200};
                    int[] yOffsets = {0, 0, 0};
                    for (int i = 0; i < states.length; i++) {
                        g2d.drawOval(centerX + xOffsets[i] - radius, centerY + yOffsets[i] - radius, 2 * radius, 2 * radius);
                        g2d.drawString(states[i], centerX + xOffsets[i] - 5, centerY + yOffsets[i] + 5);
                    }
                    // Flèches et taux
                    g2d.drawLine(centerX, centerY, centerX + 100, centerY); // S -> I
                    g2d.drawString("β", centerX + 50, centerY - 10);
                    g2d.drawLine(centerX + 100, centerY, centerX + 200, centerY); // I -> R
                    g2d.drawString("γ", centerX + 150, centerY - 10);
                } else if ("SEIR".equals(selectedScenario)) {
                    // Dessiner 4 ronds pour SEIR (S, E, I, R)
                    String[] states = {"S", "E", "I", "R"};
                    int[] xOffsets = {0, 100, 200, 300};
                    int[] yOffsets = {0, 0, 0, 0};
                    for (int i = 0; i < states.length; i++) {
                        g2d.drawOval(centerX + xOffsets[i] - radius, centerY + yOffsets[i] - radius, 2 * radius, 2 * radius);
                        g2d.drawString(states[i], centerX + xOffsets[i] - 5, centerY + yOffsets[i] + 5);
                    }
                    // Flèches et taux
                    g2d.drawLine(centerX, centerY, centerX + 100, centerY); // S -> E
                    g2d.drawString("β", centerX + 50, centerY - 10);
                    g2d.drawLine(centerX + 100, centerY, centerX + 200, centerY); // E -> I
                    g2d.drawString("σ", centerX + 150, centerY - 10);
                    g2d.drawLine(centerX + 200, centerY, centerX + 300, centerY); // I -> R
                    g2d.drawString("γ", centerX + 250, centerY - 10);
                } else if ("SIS".equals(selectedScenario)) {
                    // Dessiner 2 ronds pour SIS (S, I)
                    String[] states = {"S", "I"};
                    int[] xOffsets = {0, 100};
                    int[] yOffsets = {0, 0};
                    for (int i = 0; i < states.length; i++) {
                        g2d.drawOval(centerX + xOffsets[i] - radius, centerY + yOffsets[i] - radius, 2 * radius, 2 * radius);
                        g2d.drawString(states[i], centerX + xOffsets[i] - 5, centerY + yOffsets[i] + 5);
                    }
                    // Flèches et taux
                    g2d.drawLine(centerX, centerY, centerX + 100, centerY); // S -> I
                    g2d.drawString("β", centerX + 50, centerY - 10);
                    g2d.drawLine(centerX + 100, centerY, centerX, centerY); // I -> S
                    g2d.drawString("γ", centerX + 50, centerY + 30);
                }
            }
        };
        graphPanel.setPreferredSize(new Dimension(450, 200));
        graphPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int centerX = 150, centerY = 150, radius = 30;
                String selectedScenario = (String) scenarioBox.getSelectedItem();

                // Détecter les clics sur les ronds
                if ("SIR".equals(selectedScenario)) {
                    int[] xOffsets = {0, 100, 200};
                    int[] yOffsets = {0, 0, 0};
                    // Clic sur S
                    if (Math.sqrt(Math.pow(x - (centerX + xOffsets[0]), 2) + Math.pow(y - (centerY + yOffsets[0]), 2)) <= radius) {
                        infoArea.setText("Équation pour S (SIR) :\ndS/dt = -β * S * I / N");
                        return;
                    }
                    // Clic sur I
                    if (Math.sqrt(Math.pow(x - (centerX + xOffsets[1]), 2) + Math.pow(y - (centerY + yOffsets[1]), 2)) <= radius) {
                        infoArea.setText("Équation pour I (SIR) :\ndI/dt = β * S * I / N - γ * I");
                        return;
                    }
                    // Clic sur R
                    if (Math.sqrt(Math.pow(x - (centerX + xOffsets[2]), 2) + Math.pow(y - (centerY + yOffsets[2]), 2)) <= radius) {
                        infoArea.setText("Équation pour R (SIR) :\ndR/dt = γ * I");
                        return;
                    }
                    // Clic sur β (S -> I)
                    if (x >= centerX + 40 && x <= centerX + 60 && y >= centerY - 20 && y <= centerY) {
                        infoArea.setText(getBetaValue()); // Corriger le doublon en ne mettant que la valeur
                        return;
                    }
                    // Clic sur γ (I -> R)
                    if (x >= centerX + 140 && x <= centerX + 160 && y >= centerY - 20 && y <= centerY) {
                        infoArea.setText(getGammaValue()); // Corriger le doublon
                        return;
                    }
                } else if ("SEIR".equals(selectedScenario)) {
                    int[] xOffsets = {0, 100, 200, 300};
                    int[] yOffsets = {0, 0, 0, 0};
                    // Clic sur S
                    if (Math.sqrt(Math.pow(x - (centerX + xOffsets[0]), 2) + Math.pow(y - (centerY + yOffsets[0]), 2)) <= radius) {
                        infoArea.setText("Équation pour S (SEIR) :\ndS/dt = -β * S * I / N");
                        return;
                    }
                    // Clic sur E
                    if (Math.sqrt(Math.pow(x - (centerX + xOffsets[1]), 2) + Math.pow(y - (centerY + yOffsets[1]), 2)) <= radius) {
                        infoArea.setText("Équation pour E (SEIR) :\ndE/dt = β * S * I / N - σ * E");
                        return;
                    }
                    // Clic sur I
                    if (Math.sqrt(Math.pow(x - (centerX + xOffsets[2]), 2) + Math.pow(y - (centerY + yOffsets[2]), 2)) <= radius) {
                        infoArea.setText("Équation pour I (SEIR) :\ndI/dt = σ * E - γ * I");
                        return;
                    }
                    // Clic sur R
                    if (Math.sqrt(Math.pow(x - (centerX + xOffsets[3]), 2) + Math.pow(y - (centerY + yOffsets[3]), 2)) <= radius) {
                        infoArea.setText("Équation pour R (SEIR) :\ndR/dt = γ * I");
                        return;
                    }
                    // Clic sur β (S -> E)
                    if (x >= centerX + 40 && x <= centerX + 60 && y >= centerY - 20 && y <= centerY) {
                        infoArea.setText(getBetaValue()); // Corriger le doublon
                        return;
                    }
                    // Clic sur σ (E -> I)
                    if (x >= centerX + 140 && x <= centerX + 160 && y >= centerY - 20 && y <= centerY) {
                        infoArea.setText(getSigmaValue()); // Corriger le doublon
                        return;
                    }
                    // Clic sur γ (I -> R)
                    if (x >= centerX + 240 && x <= centerX + 260 && y >= centerY - 20 && y <= centerY) {
                        infoArea.setText(getGammaValue()); // Corriger le doublon
                        return;
                    }
                } else if ("SIS".equals(selectedScenario)) {
                    int[] xOffsets = {0, 100};
                    int[] yOffsets = {0, 0};
                    // Clic sur S
                    if (Math.sqrt(Math.pow(x - (centerX + xOffsets[0]), 2) + Math.pow(y - (centerY + yOffsets[0]), 2)) <= radius) {
                        infoArea.setText("Équation pour S (SIS) :\ndS/dt = -β * S * I / N + γ * I");
                        return;
                    }
                    // Clic sur I
                    if (Math.sqrt(Math.pow(x - (centerX + xOffsets[1]), 2) + Math.pow(y - (centerY + yOffsets[1]), 2)) <= radius) {
                        infoArea.setText("Équation pour I (SIS) :\ndI/dt = β * S * I / N - γ * I");
                        return;
                    }
                    // Clic sur β (S -> I)
                    if (x >= centerX + 40 && x <= centerX + 60 && y >= centerY - 20 && y <= centerY) {
                        infoArea.setText(getBetaValue()); // Corriger le doublon
                        return;
                    }
                    // Clic sur γ (I -> S)
                    if (x >= centerX + 40 && x <= centerX + 60 && y >= centerY + 20 && y <= centerY + 40) {
                        infoArea.setText(getGammaValue()); // Corriger le doublon
                        return;
                    }
                }
            }
        });

        // Mettre à jour le dessin quand on change de scénario
        scenarioBox.addActionListener(e -> graphPanel.repaint());

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        infoArea = new JTextArea(5, 30);
        infoArea.setEditable(false);
        JScrollPane infoScrollPane = new JScrollPane(infoArea);

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        add(inputPanel);
        add(buttonPanel);
        add(graphPanel);
        add(scrollPane);
        add(infoScrollPane);

        pack(); // Ajuster automatiquement la taille de la fenêtre
        setMinimumSize(new Dimension(800, 650)); // Taille minimale pour tout voir
        setVisible(true);
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
    public JTextArea getInfoArea() { return infoArea; }

    // Setters
    public void setSelectedScenario(String scenario) { scenarioBox.setSelectedItem(scenario); }
    public void setSelectedSolver(String solver) { solverBox.setSelectedItem(solver); }
    public void setBetaText(String text) { betaField.setText(text); }
    public void setGammaText(String text) { gammaField.setText(text); }
    public void setMuText(String text) { muField.setText(text); }
    public void setSigmaText(String text) { sigmaField.setText(text); }
    public void setStepText(String text) { stepField.setText(text); }
    public void setEndText(String text) { endField.setText(text); }

    // Calculer les valeurs des taux
    private String getBetaValue() {
        double beta = Double.parseDouble(betaField.getText().replace(",", "."));
        return String.format("β = %.3f", beta);
    }

    private String getGammaValue() {
        double gamma = Double.parseDouble(gammaField.getText().replace(",", "."));
        return String.format("γ = %.3f", gamma);
    }

    private String getSigmaValue() {
        double sigma = Double.parseDouble(sigmaField.getText().replace(",", "."));
        return String.format("σ = %.3f", sigma);
    }

    public SimulationController getController() {
        return controller;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimulationUI().setVisible(true));
    }
}