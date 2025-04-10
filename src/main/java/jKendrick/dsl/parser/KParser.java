package jKendrick.dsl.parser;

import jKendrick.dsl.core.KModel;
import jKendrick.dsl.core.KScenario;
import jKendrick.dsl.integration.KSimulation;

import java.io.*;
import java.util.*;

public class KParser {
    public static KSimulation parseFile(String path) throws IOException {
        KModel model = new KModel();
        KScenario scenario = new KScenario(model);

        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("compartments")) {
                String[] parts = line.split(":")[1].split(",");
                model.setCompartments(Arrays.stream(parts).map(String::trim).toArray(String[]::new));
            } else if (line.startsWith("param")) {
                String[] parts = line.split(":")[1].split("=");
                model.setParameter(parts[0].trim(), Double.parseDouble(parts[1].trim()));
            } else if (line.startsWith("init")) {
                String[] vals = line.split(":")[1].split(",");
                double[] pop = Arrays.stream(vals).mapToDouble(Double::parseDouble).toArray();
                scenario.setInitialPopulation(pop);
            }
        }

        return new KSimulation(scenario, 0.1, 100.0); // hardcoded for now
    }
}
