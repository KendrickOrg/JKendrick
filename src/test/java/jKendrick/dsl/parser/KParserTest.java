package jKendrick.dsl.parser;

import jKendrick.dsl.core.KModel;
import jKendrick.dsl.core.KScenario;
import jKendrick.dsl.integration.KSimulation;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class KParserTest {

    @Test
    void testParseSimpleFile() throws IOException {
        try {
            // Création d'un modèle
            KModel model = new KModel();
            model.setCompartments("S", "I", "R");
            model.setParameter("beta", 0.3);
            model.setParameter("gamma", 0.1);

            // Création d'un scénario basé sur le modèle
            KScenario scenario = new KScenario(model);
            scenario.setInitialPopulation(1000.0, 100.0, 0.0);

            // Accès aux compartiments et aux paramètres
            System.out.println("Compartments: " + model.getCompartments());
            System.out.println("Parameters: " + model.getParameters());
            System.out.println("Initial Population: " + Arrays.toString(scenario.getInitialPopulation()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
