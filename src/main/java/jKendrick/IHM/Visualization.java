package jKendrick.IHM;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;

import java.util.List;
import java.util.ArrayList;

public class Visualization {

    public void plotResults(double[][] results, double step) {
        int length = results.length;
        List<Double> time = new ArrayList<>();
        List<Double> susceptible = new ArrayList<>();
        List<Double> infected = new ArrayList<>();
        List<Double> recovered = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            time.add(i * step);
            susceptible.add(results[i][0]);
            infected.add(results[i][1]);
            recovered.add(results[i][2]);
        }

        XYChart chart = new XYChartBuilder().width(800).height(600).title("SIR Model Results").xAxisTitle("Time").yAxisTitle("Population").build();

        chart.getStyler().setChartTitleVisible(true);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(6);

        chart.addSeries("Susceptible", time, susceptible);
        chart.addSeries("Infected", time, infected);
        chart.addSeries("Recovered", time, recovered);

        new SwingWrapper<>(chart).displayChart();
    }


}

