package jKendrick.IHM.ui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KendrickFileParser {
    private static final Set<String> VALID_SCENARIOS = new HashSet<>(Arrays.asList("SIR", "SIS", "SIRD", "SEIR"));
    private static final Set<String> VALID_SOLVERS = new HashSet<>(Arrays.asList("RK4", "Gillespie", "TauLeap"));
    private static final Pattern VECTOR_PATTERN = Pattern.compile("\\[\\s*([0-9,.\\s]*)\\s*\\]");
    private static final Pattern MATRIX_PATTERN = Pattern.compile("\\[\\s*\\[\\s*([0-9,.\\s]*)\\s*\\](?:\\s*,\\s*\\[\\s*([0-9,.\\s]*)\\s*\\])*\\s*\\]");

    public KendrickModelData parseFile(String filePath) throws IOException {
        KendrickModelData data = new KendrickModelData();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        String currentSection = "";
        String scenario = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("KendrickModel ")) {
                scenario = line.substring(14).trim();
                if (!VALID_SCENARIOS.contains(scenario)) {
                    throw new IOException("Scénario inconnu : " + scenario);
                }
                data.setScenario(scenario);
                continue;
            }

            if (line.startsWith("attributes:")) {
                currentSection = "attributes";
                continue;
            } else if (line.startsWith("parameters:")) {
                currentSection = "parameters";
                continue;
            } else if (line.startsWith("lambda:")) {
                currentSection = "lambda";
                continue;
            } else if (line.startsWith("equations:")) {
                currentSection = "equations";
                continue;
            } else if (line.startsWith("initial:")) {
                currentSection = "initial";
                continue;
            } else if (line.startsWith("parameter_values:")) {
                currentSection = "parameter_values";
                continue;
            } else if (line.startsWith("simulation ")) {
                currentSection = "simulation";
                String simName = line.substring(11).trim();
                if (!simName.startsWith(scenario + "_")) {
                    throw new IOException("Nom de simulation invalide : " + simName);
                }
                String solver = simName.substring(scenario.length() + 1);
                if (!VALID_SOLVERS.contains(solver)) {
                    throw new IOException("Solveur inconnu : " + solver);
                }
                data.setSolver(solver);
                continue;
            }

            if (currentSection.equals("initial") && line.contains(":=")) {
                String[] parts = line.split(":=");
                if (parts.length != 2) {
                    throw new IOException("Format initial invalide : " + line);
                }
                String compartment = parts[0].trim();
                String valueStr = parts[1].replace(";", "").trim();
                Object value = parseValue(valueStr);
                data.setInitialPopulation(compartment, value);
            } else if (currentSection.equals("parameter_values") && line.contains(":=")) {
                String[] parts = line.split(":=");
                if (parts.length != 2) {
                    throw new IOException("Format de paramètre invalide : " + line);
                }
                String param = parts[0].trim();
                String valueStr = parts[1].replace(";", "").trim();
                Object value = parseValue(valueStr);
                data.setParameter(param, value);
            } else if (currentSection.equals("simulation")) {
                if (line.startsWith("solver:")) {
                    String solver = line.split(":")[1].replace(";", "").trim();
                    if (!VALID_SOLVERS.contains(solver)) {
                        throw new IOException("Solveur inconnu : " + solver);
                    }
                    data.setSolver(solver);
                } else if (line.startsWith("step:")) {
                    try {
                        String valueStr = line.split(":")[1].replace(";", "").trim().replace(",", ".");
                        data.setStep(Double.parseDouble(valueStr));
                    } catch (NumberFormatException e) {
                        throw new IOException("Valeur de step non numérique : " + line);
                    }
                } else if (line.startsWith("end:")) {
                    try {
                        String valueStr = line.split(":")[1].replace(";", "").trim().replace(",", ".");
                        data.setEnd(Double.parseDouble(valueStr));
                    } catch (NumberFormatException e) {
                        throw new IOException("Valeur de end non numérique : " + line);
                    }
                } else if (line.startsWith("runs:")) {
                    try {
                        data.setRuns(Integer.parseInt(line.split(":")[1].replace(";", "").trim()));
                    } catch (NumberFormatException e) {
                        throw new IOException("Valeur de runs non numérique : " + line);
                    }
                }
            }
        }

        reader.close();
        validateData(data);
        return data;
    }

    private Object parseValue(String valueStr) throws IOException {
        // Essayer de parser comme matrice
        Matcher matrixMatcher = MATRIX_PATTERN.matcher(valueStr);
        if (matrixMatcher.matches()) {
            // Nettoyer la chaîne pour un split propre
            String cleanedMatrix = valueStr.replaceAll("\\s*\\[\\s*\\[", "[[").replaceAll("\\s*\\]\\s*\\]", "]]").replaceAll("\\s*\\],\\s*\\[", "],[");
            String[] rows = cleanedMatrix.split("\\],\\[");
            double[][] matrix = new double[rows.length][];
            for (int i = 0; i < rows.length; i++) {
                String row = rows[i].replaceAll("[\\[\\]]", "").trim();
                if (row.isEmpty()) {
                    throw new IOException("Ligne de matrice vide : " + valueStr);
                }
                String[] values = row.split("\\s*,\\s*");
                matrix[i] = new double[values.length];
                for (int j = 0; j < values.length; j++) {
                    String cleanedValue = cleanNumericValue(values[j]);
                    try {
                        matrix[i][j] = Double.parseDouble(cleanedValue);
                    } catch (NumberFormatException e) {
                        System.out.println("Erreur sur valeur: " + values[j] + " | Nettoyée: " + cleanedValue);
                        throw new IOException("Valeur non numérique dans la matrice : " + valueStr);
                    }
                }
            }
            return matrix;
        }

        // Essayer de parser comme vecteur
        Matcher vectorMatcher = VECTOR_PATTERN.matcher(valueStr);
        if (vectorMatcher.matches()) {
            String content = vectorMatcher.group(1).trim();
            if (content.isEmpty()) {
                return new double[0];
            }
            String[] values = content.split("\\s*,\\s*");
            double[] vector = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                String cleanedValue = cleanNumericValue(values[i]);
                try {
                    vector[i] = Double.parseDouble(cleanedValue);
                } catch (NumberFormatException e) {
                    System.out.println("Erreur sur valeur: " + values[i] + " | Nettoyée: " + cleanedValue);
                    throw new IOException("Valeur non numérique dans le vecteur : " + valueStr);
                }
            }
            return vector;
        }

        // Parser comme scalaire
        String cleanedValue = cleanNumericValue(valueStr);
        try {
            return Double.parseDouble(cleanedValue);
        } catch (NumberFormatException e) {
            System.out.println("Erreur sur scalaire: " + valueStr + " | Nettoyée: " + cleanedValue);
            throw new IOException("Valeur non numérique : " + valueStr);
        }
    }

    private String cleanNumericValue(String value) {
        // Supprimer les espaces
        value = value.trim();
        // Remplacer les virgules par des points pour les décimaux
        value = value.replace(",", ".");
        // Supprimer un point décimal final sans chiffres (ex. : "0.3." → "0.3")
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        // Remplacer plusieurs points par un seul (ex. : "0..3" → "0.3")
        value = value.replaceAll("\\.+", ".");
        // Supprimer les espaces internes (ex. : "0 . 3" → "0.3")
        value = value.replaceAll("\\s+", "");
        // S'assurer qu'il n'y a qu'un seul point décimal
        int firstDot = value.indexOf(".");
        if (firstDot != -1) {
            value = value.substring(0, firstDot + 1) + value.substring(firstDot + 1).replace(".", "");
        }
        return value;
    }

    private void validateData(KendrickModelData data) throws IOException {
        if (data.getScenario() == null) {
            throw new IOException("Aucun scénario défini dans le fichier");
        }
        if (data.getSolver() == null) {
            throw new IOException("Aucun solveur défini dans le fichier");
        }

        String scenario = data.getScenario();
        Map<String, Object> initialPop = data.getInitialPopulations();
        String[] expectedCompartments;

        switch (scenario) {
            case "SIR":
                expectedCompartments = new String[]{"S", "I", "R"};
                break;
            case "SIS":
                expectedCompartments = new String[]{"S", "I"};
                break;
            case "SIRD":
                expectedCompartments = new String[]{"S", "I", "R", "D"};
                break;
            case "SEIR":
                expectedCompartments = new String[]{"S", "E", "I", "R"};
                break;
            default:
                throw new IOException("Scénario non supporté : " + scenario);
        }

        for (String comp : expectedCompartments) {
            if (!initialPop.containsKey(comp)) {
                throw new IOException("Population initiale manquante pour " + comp);
            }
        }

        Map<String, Object> params = data.getParameters();
        if (!params.containsKey("beta") || !params.containsKey("gamma")) {
            throw new IOException("Paramètres beta ou gamma manquants");
        }
        if (scenario.equals("SIRD") && !params.containsKey("mu")) {
            throw new IOException("Paramètre mu manquant pour SIRD");
        }
        if (scenario.equals("SEIR") && !params.containsKey("sigma")) {
            throw new IOException("Paramètre sigma manquant pour SEIR");
        }
    }
}