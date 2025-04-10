package jKendrick.integration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class VisualizationTest {

    @Test
    void testChartRenderSingleRun() {
        Visualization viz = new Visualization();

        double[][] result = new double[5][3];
        double[] times = new double[]{0, 1, 2, 3, 4};
        List<String> labels = List.of("S", "I", "R");

        for (int i = 0; i < result.length; i++) {
            result[i][0] = 900 - i * 10;
            result[i][1] = 100 + i * 5;
            result[i][2] = i * 5;
        }

        assertDoesNotThrow(() -> viz.getChart(result, times, labels, "Test Visualization - Single Run"));
    }


    @Test
    void testChartRenderMultiRun() {
        Visualization viz = new Visualization();

        int runs = 3;
        int steps = 5;
        int compartments = 3;
        double[][][] multiRun = new double[runs][steps][compartments];
        double[] times = new double[]{0, 1, 2, 3, 4};
        List<String> labels = List.of("S", "I", "R");

        for (int r = 0; r < runs; r++) {
            for (int t = 0; t < steps; t++) {
                multiRun[r][t][0] = 900 - t * 10;
                multiRun[r][t][1] = 100 + t * 5;
                multiRun[r][t][2] = t * 5;
            }
        }

        assertDoesNotThrow(() -> viz.getChart(multiRun, times, labels, "Test Visualization - Multi Run"));
    }
}
