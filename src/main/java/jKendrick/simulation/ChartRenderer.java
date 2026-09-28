package jKendrick.simulation;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Draws the series selected by a {@link VisualizationSpec} with XChart. This is the only class of
 * the DSL pipeline that depends on the charting library.
 * <p>
 * With a single run (deterministic solver) each series is one curve. With several runs
 * (stochastic solvers) every run of a series is drawn as a thin curve of the series' colour, so
 * that the chart shows the spread of the trajectories as the MATLAB scripts do.
 */
public final class ChartRenderer {
    private ChartRenderer() {}

    /** The chart of the runs according to the specification. */
    public static XYChart chart(VisualizationSpec spec, List<Trajectory> runs) {
        if (runs.isEmpty()) throw new IllegalArgumentException("nothing to plot: no run");
        XYChart chart = new XYChartBuilder().width(800).height(600)
                .title(spec.title).xAxisTitle(spec.xLabel).yAxisTitle(spec.yLabel).build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setMarkerSize(0);
        boolean several = runs.size() > 1;
        Color[] colors = chart.getStyler().getSeriesColors();
        for (int r = 0; r < runs.size(); r++) {
            Trajectory t = runs.get(r);
            int i = 0;
            for (Map.Entry<String, double[]> e : spec.series(t).entrySet()) {
                String label = several ? e.getKey() + " #" + (r + 1) : e.getKey();
                XYSeries s = chart.addSeries(label, t.times, e.getValue());
                s.setMarker(SeriesMarkers.NONE);
                s.setLineColor(colors[i++ % colors.length]);
                if (several) {
                    s.setShowInLegend(r == 0);
                    s.setLineWidth(1f);
                }
            }
        }
        if (several) renameFirstRunLegends(chart, runs.get(0), spec);
        return chart;
    }

    /** With several runs, the legend shows the series names without the run number. */
    private static void renameFirstRunLegends(XYChart chart, Trajectory first, VisualizationSpec spec) {
        for (String name : spec.series(first).keySet()) {
            XYSeries s = chart.getSeriesMap().get(name + " #1");
            if (s != null) s.setLabel(name);
        }
    }

    /** Saves the chart as a PNG file. */
    public static void savePng(XYChart chart, Path file) throws IOException {
        BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);
    }

    /** Opens the chart in a window (returns immediately). */
    public static void show(XYChart chart) {
        new SwingWrapper<>(chart).displayChart();
    }
}
