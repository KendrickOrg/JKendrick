package jKendrick.IHM.ui;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class KendrickFileGenerator {
    private final SimulationUI ui;

    public KendrickFileGenerator(SimulationUI ui) {
        this.ui = ui;
    }

    public void generateKendrickFile() {
        try {
            String selectedScenario = ui.getSelectedScenario();
            String selectedSolver = ui.getSelectedSolver();
            double beta = Double.parseDouble(ui.getBetaText());
            double gamma = Double.parseDouble(ui.getGammaText());
            double mu = Double.parseDouble(ui.getMuText());
            double sigma = Double.parseDouble(ui.getSigmaText());
            double step = Double.parseDouble(ui.getStepText());
            double end = Double.parseDouble(ui.getEndText());

            double[] population = SimulationUI.populationMap.get(selectedScenario); // Accès via champ statique

            String kendrickCode = generateKendrickCode(
                    selectedScenario, selectedSolver, beta, gamma, mu, sigma, step, end, population);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Sauvegarder le fichier Kendrick");
            fileChooser.setSelectedFile(new File(selectedScenario + "_" + selectedSolver + ".kendrick"));

            if (fileChooser.showSaveDialog(ui) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".kendrick")) {
                    file = new File(file.getAbsolutePath() + ".kendrick");
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(kendrickCode);
                    ui.getLogArea().append("Fichier Kendrick sauvegardé : " + file.getAbsolutePath() + "\n");
                } catch (IOException ex) {
                    ui.getLogArea().append("Erreur lors de la sauvegarde : " + ex.getMessage() + "\n");
                    JOptionPane.showMessageDialog(ui,
                            "Erreur lors de la sauvegarde du fichier.",
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(ui,
                    "Veuillez entrer des valeurs numériques valides.",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            ui.getLogArea().append("Erreur : Entrées non numériques\n");
        }
    }

    private String generateKendrickCode(String scenario, String solver,
                                        double beta, double gamma, double mu, double sigma,
                                        double step, double end, double[] population) {
        StringBuilder sb = new StringBuilder();

        sb.append("KendrickModel ").append(scenario).append("\n");

        sb.append("    attributes:\n");
        sb.append("        (status -> ");
        switch (scenario) {
            case "SIR":
                sb.append("S I R");
                break;
            case "SIS":
                sb.append("S I");
                break;
            case "SIRD":
                sb.append("S I R D");
                break;
            case "SEIR":
                sb.append("S E I R");
                break;
        }
        sb.append(");\n");

        sb.append("    parameters:\n");
        sb.append("        (");
        switch (scenario) {
            case "SIR":
            case "SIS":
                sb.append("beta gamma");
                break;
            case "SIRD":
                sb.append("beta gamma mu");
                break;
            case "SEIR":
                sb.append("beta gamma sigma");
                break;
        }
        sb.append(");\n");

        sb.append("    lambda:\n");
        sb.append("        (beta*I/N);\n");

        sb.append("    equations:\n");
        switch (scenario) {
            case "SIR":
                sb.append("        S := -beta*S*I/N;\n");
                sb.append("        I := beta*S*I/N - gamma*I;\n");
                sb.append("        R := gamma*I;\n");
                break;
            case "SIS":
                sb.append("        S := -beta*S*I/N + gamma*I;\n");
                sb.append("        I := beta*S*I/N - gamma*I;\n");
                break;
            case "SIRD":
                sb.append("        S := -beta*S*I/N;\n");
                sb.append("        I := beta*S*I/N - gamma*I - mu*I;\n");
                sb.append("        R := gamma*I;\n");
                sb.append("        D := mu*I;\n");
                break;
            case "SEIR":
                sb.append("        S := -beta*S*I/N;\n");
                sb.append("        E := beta*S*I/N - sigma*E;\n");
                sb.append("        I := sigma*E - gamma*I;\n");
                sb.append("        R := gamma*I;\n");
                break;
        }

        sb.append("    initial:\n");
        switch (scenario) {
            case "SIR":
            case "SIS":
                sb.append("        S := ").append((int) population[0]).append(";\n");
                sb.append("        I := ").append((int) population[1]).append(";\n");
                if (scenario.equals("SIR")) {
                    sb.append("        R := ").append((int) population[2]).append(";\n");
                }
                break;
            case "SIRD":
                sb.append("        S := ").append((int) population[0]).append(";\n");
                sb.append("        I := ").append((int) population[1]).append(";\n");
                sb.append("        R := ").append((int) population[2]).append(";\n");
                sb.append("        D := ").append((int) population[3]).append(";\n");
                break;
            case "SEIR":
                sb.append("        S := ").append((int) population[0]).append(";\n");
                sb.append("        E := ").append((int) population[1]).append(";\n");
                sb.append("        I := ").append((int) population[2]).append(";\n");
                sb.append("        R := ").append((int) population[3]).append(";\n");
                break;
        }

        sb.append("    parameter_values:\n");
        sb.append("        beta := ").append(String.format("%.3f", beta)).append(";\n");
        sb.append("        gamma := ").append(String.format("%.3f", gamma)).append(";\n");
        if (scenario.equals("SIRD")) {
            sb.append("        mu := ").append(String.format("%.3f", mu)).append(";\n");
        }
        if (scenario.equals("SEIR")) {
            sb.append("        sigma := ").append(String.format("%.3f", sigma)).append(";\n");
        }
        sb.append("\n");

        sb.append("simulation ").append(scenario).append("_").append(solver).append("\n");
        sb.append("    solver: ").append(solver).append(";\n");
        sb.append("    step: ").append(String.format("%.3f", step)).append(";\n");
        sb.append("    end: ").append(String.format("%.1f", end)).append(";\n");
        if (solver.equals("Gillespie")) {
            sb.append("    runs: 20;\n");
        } else if (solver.equals("TauLeap")) {
            sb.append("    runs: 100;\n");
        }

        return sb.toString();
    }
}