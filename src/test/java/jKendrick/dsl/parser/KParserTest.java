package jKendrick.dsl.parser;

import jKendrick.dsl.core.KModel;
import jKendrick.dsl.core.KScenario;
import jKendrick.dsl.integration.KSimulation;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class KParserTest {

//    @Test
//    void testParseSimpleFile() throws IOException {
//        // Crée un fichier temporaire avec du contenu DSL
//        File tempFile = File.createTempFile("test", ".kmodel");
//        PrintWriter writer = new PrintWriter(tempFile);
//        writer.println("compartments: S, I, R");
//        writer.println("param: beta = 0.3");
//        writer.println("param: gamma = 0.1");
//        writer.println("init: 900, 100, 0");
//        writer.close();
//
//        // Parse le fichier
//        KSimulation sim = KParser.parseFile(tempFile.getAbsolutePath());
//
//        // Vérifie le contenu
//        KScenario scenario = sim.getScenario(); // Ajoute getScenario() dans KSimulation si pas encore fait
//        KModel model = scenario.getModel();     // Ajoute getModel() dans KScenario si pas encore fait
//
//        assertArrayEquals(new String[]{"S", "I", "R"}, model.getCompartments().toArray());
//        assertEquals(0.3, model.getParameter("beta"));
//        assertEquals(0.1, model.getParameter("gamma"));
//        assertArrayEquals(new double[]{900, 100, 0}, scenario.getInitialPopulation());
//
//        tempFile.delete(); // Nettoyage
//    }
}
