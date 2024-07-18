package jKendrick.IHM;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Visualization {

    private String title;
    private String xAxisLabel;
    private String yAxisLabel;
    private List<String> compartmentNames;

    public Visualization(String title, String xAxisLabel, String yAxisLabel, List<String> compartmentNames) {
        this.title = title;
        this.xAxisLabel = xAxisLabel;
        this.yAxisLabel = yAxisLabel;
        this.compartmentNames = compartmentNames;
    }

    public void plotResults(double[][] results, double step) {
        int length = results.length;
        List<Double> time = new ArrayList<>();
        Map<String, List<Double>> dataSeries = createDataSeriesMap();

        for (int i = 0; i < length; i++) {
            time.add(i * step);
            for (int j = 0; j < compartmentNames.size(); j++) {
                dataSeries.get(compartmentNames.get(j)).add(results[i][j]);
            }
        }

        XYChart chart = new XYChartBuilder().width(800).height(600).title(title).xAxisTitle(xAxisLabel).yAxisTitle(yAxisLabel).build();

        chart.getStyler().setChartTitleVisible(true);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(6);

        for (String compartment : compartmentNames) {
            chart.addSeries(compartment, time, dataSeries.get(compartment));
        }

        new SwingWrapper<>(chart).displayChart();
    }

    private Map<String, List<Double>> createDataSeriesMap() {
        Map<String, List<Double>> dataSeries = new HashMap<>();
        for (String compartment : compartmentNames) {
            dataSeries.put(compartment, new ArrayList<>());
        }
        return dataSeries;
    }

    public static void main(String[] args) {
        // Example data
        double step = 0.1;
        double[][] results = {
                {0.99, 0.01, 0.0},
                {0.98, 0.02, 0.0},
                {0.97, 0.03, 0.0}
                // Add more data points as needed
        };

        List<String> compartments = new ArrayList<>();
        compartments.add("Susceptible");
        compartments.add("Infected");
        compartments.add("Recovered");

        Visualization viz = new Visualization("Epidemic Model", "Time", "Population", compartments);
        viz.plotResults(results, step);
    }
}
