package jKendrick.simulation;

import jKendrick.model.Compartment;
import jKendrick.model.GroundModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of a simulation: the cardinality of every compartment at every recorded time.
 * A stochastic solver returns one trajectory per run.
 */
public final class Trajectory {
    public final GroundModel model;
    public final double[] times;
    /** {@code states[i][c]}: cardinality of compartment c at times[i]. */
    public final double[][] states;

    public Trajectory(GroundModel model, double[] times, double[][] states) {
        this.model = model;
        this.times = times;
        this.states = states;
    }

    public int size() {
        return times.length;
    }

    public double[] last() {
        return states[states.length - 1];
    }

    /** The compartment names, in the model's order (column headers). */
    public List<String> labels() {
        List<String> l = new ArrayList<>();
        for (Compartment c : model.model.compartments) l.add(c.toString());
        return l;
    }

    /**
     * Totals over the compartments that agree on the given attributes, e.g. {@code aggregate("status", "species")}
     * gives one series per (status, species), summed over the zones. The keys are the values joined by underscores.
     */
    public Map<String, double[]> aggregate(String... attributes) {
        Map<String, double[]> series = new LinkedHashMap<>();
        List<Compartment> comps = model.model.compartments;
        for (int c = 0; c < comps.size(); c++) {
            StringBuilder key = new StringBuilder();
            for (String a : attributes) {
                String v = comps.get(c).get(a);
                if (v == null) continue;
                if (key.length() > 0) key.append('_');
                key.append(v);
            }
            double[] s = series.computeIfAbsent(key.toString(), k -> new double[times.length]);
            for (int i = 0; i < times.length; i++) s[i] += states[i][c];
        }
        return series;
    }

    /** The trajectory as CSV: a time column, then one column per compartment. */
    public String toCsv() {
        StringBuilder sb = new StringBuilder("t");
        for (String l : labels()) sb.append(',').append(l);
        sb.append('\n');
        for (int i = 0; i < times.length; i++) {
            sb.append(times[i]);
            for (double v : states[i]) sb.append(',').append(v);
            sb.append('\n');
        }
        return sb.toString();
    }
}
