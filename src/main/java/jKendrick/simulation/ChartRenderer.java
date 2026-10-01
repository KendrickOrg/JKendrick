package jKendrick.simulation;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
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

    /** Qualitative palette in the spirit of the Pharo charts (Set1): red, blue, green, purple, orange, brown, pink, grey. */
    private static final Color[] PALETTE = {
            new Color(0xe4, 0x1a, 0x1c), new Color(0x37, 0x7e, 0xb8), new Color(0x4d, 0xaf, 0x4a), new Color(0x98, 0x4e, 0xa3),
            new Color(0xff, 0x7f, 0x00), new Color(0xa6, 0x56, 0x28), new Color(0xf7, 0x81, 0xbf), new Color(0x99, 0x99, 0x99)};

    /** The chart of the runs according to the specification. */
    public static XYChart chart(VisualizationSpec spec, List<Trajectory> runs) {
        if (runs.isEmpty()) throw new IllegalArgumentException("nothing to plot: no run");
        XYChart chart = new XYChartBuilder().width(800).height(500)
                .xAxisTitle(spec.xLabel).yAxisTitle(spec.yLabel).build();
        if (spec.title != null) chart.setTitle(spec.title);
        // the look of the Pharo implementation's charts (Roassal): white, no grid, thin lines,
        // legend in a box under the plot
        XYStyler st = chart.getStyler();
        st.setChartTitleVisible(spec.title != null);
        st.setChartBackgroundColor(Color.WHITE);
        st.setPlotBackgroundColor(Color.WHITE);
        st.setPlotBorderVisible(false);
        st.setPlotGridLinesVisible(false);
        st.setAxisTickMarksColor(new Color(0x88, 0x88, 0x88));
        st.setAxisTickLabelsColor(new Color(0x44, 0x44, 0x44));
        st.setChartFontColor(new Color(0x44, 0x44, 0x44));
        st.setLegendPosition(Styler.LegendPosition.OutsideS);
        st.setLegendLayout(Styler.LegendLayout.Horizontal);
        st.setLegendBackgroundColor(Color.WHITE);
        st.setLegendBorderColor(new Color(0xcc, 0xcc, 0xcc));
        st.setLegendSeriesLineLength(14);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setMarkerSize(0);
        st.setSeriesColors(PALETTE);
        st.setAntiAlias(true);
        // in a window (XChartPanel): drag a rectangle to zoom on a time interval, double-click to reset
        st.setZoomEnabled(true);
        st.setZoomResetByDoubleClick(true);
        st.setZoomResetByButton(true);
        st.setZoomSelectionColor(new Color(0x37, 0x7e, 0xb8, 0x30));
        st.setLocale(Locale.US);
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
                s.setLineWidth(several ? 0.8f : 1.4f);
                if (several) s.setShowInLegend(r == 0);
            }
        }
        if (several) renameFirstRunLegends(chart, runs.get(0), spec);
        if (spec.series(runs.get(0)).size() > 8) { // too many for one row under the plot
            st.setLegendPosition(Styler.LegendPosition.OutsideE);
            st.setLegendLayout(Styler.LegendLayout.Vertical);
        }
        defeatDataPointTicks(chart, runs.get(0).times);
        return chart;
    }

    /**
     * XChart 3.8 puts the ticks of the x axis on data points when the x values of the series are
     * equally spaced, which the times of a trajectory always are, and then labels them with values
     * such as 0, 94, 188... An invisible series with one x value between two times makes the set
     * of x values unequally spaced, so that the usual round ticks (0, 200, 400...) are used.
     */
    private static void defeatDataPointTicks(XYChart chart, double[] times) {
        if (times.length < 2) return;
        double[] x = {times[0], times[0] + 0.5 * (times[1] - times[0])};
        double y = chart.getSeriesMap().values().iterator().next().getYData()[0];
        XYSeries s = chart.addSeries(" ", x, new double[]{y, y});
        s.setShowInLegend(false);
        s.setMarker(SeriesMarkers.NONE);
        s.setLineColor(new Color(0, 0, 0, 0));
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
