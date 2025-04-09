package jKendrick.integration;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Visualization {
    // getChart pour RK4 (determinist)
    public void getChart(double[][] results, double[] xData, List<String> labels, String title) {
        XYChart chart = buildChart(title);

        for (int i = 0; i < labels.size(); i++) {
            double[] yData = new double[results.length];
            double[] safeX = new double[results.length];

            for (int j = 0; j < results.length; j++) {
                yData[j] = results[j][i];
            }
            chart.addSeries(labels.get(i), xData, yData)
                    .setLineColor(getColor(labels.get(i)));
        }

        showChart(chart);
    }

    // getChart pour solver stochastic
    public void getChart(double[][][] multiRunResults, double[] xData, List<String> labels, String title) {
        XYChart chart = buildChart(title);

        int nbRuns = multiRunResults.length;
        int nbSteps = multiRunResults[0].length;

        // Transparent runs
        for (int run = 0; run < nbRuns; run++) {
            for (int c = 0; c < labels.size(); c++) {
                double[] yData = new double[nbSteps];
                for (int t = 0; t < nbSteps; t++) {
                    yData[t] = multiRunResults[run][t][c];
                }
                XYSeries s = chart.addSeries(labels.get(c) + "-run" + run, xData, yData);
                s.setLineColor(new Color(100, 100, 100, 20));
                s.setMarkerColor(new Color(100, 100, 100, 20));
                s.setShowInLegend(false);
            }
        }

        // Median path
        int medianRun = nbRuns - 1;
        for (int c = 0; c < labels.size(); c++) {
            double[] yData = new double[nbSteps];
            for (int t = 0; t < nbSteps; t++) {
                yData[t] = multiRunResults[medianRun][t][c];
            }
            chart.addSeries(labels.get(c), xData, yData)
                    .setLineColor(getColor(labels.get(c)));;
        }

        showChart(chart);
    }

    private XYChart buildChart(String title) {
        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title(title).xAxisTitle("Time").yAxisTitle("Population")
                .build();

        // Styles
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setMarkerSize(4);

        return chart;
    }

    private void showChart(XYChart chart) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("XChart");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new XChartPanel<>(chart));
            frame.pack();
            frame.setVisible(true);
        });
    }

    private Color getColor(String label) {
        return switch (label) {
            case "S", "Susceptible" -> Color.BLUE;
            case "I", "Infected" -> Color.ORANGE;
            case "R", "Recovered" -> Color.MAGENTA;
            case "E", "Exposed" -> Color.CYAN;
            case "D", "Dead" -> Color.GREEN;
            default -> Color.GRAY;
        };
    }
}
