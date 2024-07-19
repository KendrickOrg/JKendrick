package jKendrick.IHM;
import jKendrick.core.ISolver;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

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

    public void getChart(ISolver solver, String title, String xAxisLabel, String yAxisLabel) throws IOException {
        double[][] results = solver.getResult();
        plotResults(results, 0.1); // Assuming a default step value, adjust as needed
    }

    public static void main(String[] args) {
        // Example data
        double step = 10;
        List<String> compartments = new ArrayList<>();

        compartments.add("Susceptible");
        compartments.add("Infected");
        compartments.add("Recovered");

        double[][] results = {
                {0.9900, 0.0100, 0.0000}, // S I R at time 0
                {0.9045, 0.0654, 0.0301},
                {0.5845, 0.2399, 0.1757},
                {0.2464, 0.2900, 0.4636},
                {0.1210, 0.1784, 0.7005},
                {0.0821, 0.0881, 0.8297},
                {0.0683, 0.0405, 0.8912},
                {0.0628, 0.0181, 0.9191},
                {0.0605, 0.0080, 0.9314},
                {0.0596, 0.0035, 0.9369},
                {0.0591, 0.0016, 0.9393},
                {0.0589, 0.0007, 0.9404},
                {0.0589, 0.0003, 0.9408},
                {0.0588, 0.0001, 0.9410},
                {0.0588, 0.0001, 0.9411},
                {0.0588, 0.0000, 0.9412}
        };



        Visualization viz = new Visualization("Epidemic Model", "Time", "Population", compartments);
        viz.plotResults(results, step);
    }
}

