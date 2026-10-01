package jKendrick.simulation;

import jKendrick.dsl.ast.Ast;
import jKendrick.model.Attribute;
import jKendrick.model.Compartment;
import jKendrick.model.ComposedModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interprets the {@code Visualization} entity of a script and extracts the series to plot from
 * a trajectory. It is independent of any charting library (see {@link ChartRenderer}).
 * <pre>
 * Visualization InfluenzaViz
 *     plot: 'diagram';           -- the only kind for now: series against time
 *     for: 'InfluenzaSim';
 *     data: #(I_species);        -- one series per species, infectious summed over the other attributes
 *     legendTitle: 'Infectious';
 *     legends: #('humans' 'birds');
 *     xLabel: 'Time (days)';
 *     yLabel: 'Number of individuals';  -- optional (the default, as in the Pharo implementation)
 *     title: 'Avian influenza'.  -- optional (no title by default)
 * </pre>
 * A data name is an attribute value ({@code I}) optionally followed by attribute names
 * ({@code I_species}, {@code I_species_zone}): the series are the totals of the compartments
 * with that value, one per combination of the listed attributes' values, summed over the
 * attributes not listed. Without {@code data:}, there is one series per value of the model's
 * first attribute (the statuses of the core concern).
 */
public final class VisualizationSpec {
    /** Name of the Visualization entity. */
    public final String name;
    public final String plot;
    public final String simulation;
    public final List<Ast.Indexed> data;
    public final String legendTitle;
    public final List<String> legends;
    public final String xLabel;
    public final String yLabel;
    public final String title;

    private VisualizationSpec(String name, String plot, String simulation, List<Ast.Indexed> data, String legendTitle,
                              List<String> legends, String xLabel, String yLabel, String title) {
        this.name = name;
        this.plot = plot;
        this.simulation = simulation;
        this.data = Collections.unmodifiableList(data);
        this.legendTitle = legendTitle;
        this.legends = Collections.unmodifiableList(legends);
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.title = title;
    }

    public static VisualizationSpec of(String entityName, Map<String, Ast.Literal> args) {
        String plot = string(args, "plot", "diagram");
        if (!plot.equals("diagram")) throw new IllegalArgumentException("Visualization: unknown plot kind '" + plot + "' (diagram)");
        List<Ast.Indexed> data = new ArrayList<>();
        Ast.Literal d = args.get("data");
        if (d != null) {
            if (d.elements == null) throw new IllegalArgumentException("Visualization: 'data:' expects an array such as #(I_species)");
            for (Ast.Literal e : d.elements) {
                if (e.identifier == null) throw new IllegalArgumentException("Visualization: 'data:' expects names such as I_species");
                data.add(Ast.Indexed.parse(e.identifier));
            }
        }
        List<String> legends = new ArrayList<>();
        Ast.Literal l = args.get("legends");
        if (l != null) {
            if (l.elements == null) throw new IllegalArgumentException("Visualization: 'legends:' expects an array of strings");
            for (Ast.Literal e : l.elements) legends.add(e.string != null ? e.string : e.identifier);
        }
        String legendTitle = string(args, "legendTitle", null);
        return new VisualizationSpec(entityName, plot, string(args, "for", null), data, legendTitle, legends,
                string(args, "xLabel", "Time (days)"), string(args, "yLabel", "Number of individuals"),
                string(args, "title", null));
    }

    private static String string(Map<String, Ast.Literal> args, String key, String dflt) {
        Ast.Literal l = args.get(key);
        if (l == null) return dflt;
        if (l.string != null) return l.string;
        if (l.identifier != null) return l.identifier;
        throw new IllegalArgumentException("Visualization: '" + key + ":' expects a string");
    }

    /** The series to plot: label -> values at the trajectory's times, in a stable order. */
    public Map<String, double[]> series(Trajectory t) {
        ComposedModel m = t.model.model;
        Map<String, double[]> all = new LinkedHashMap<>();
        if (data.isEmpty()) {
            Attribute first = m.attributes.get(0);
            for (Map.Entry<String, double[]> e : t.aggregate(first.name).entrySet()) all.put(e.getKey(), e.getValue());
        } else {
            for (Ast.Indexed name : data) all.putAll(seriesOf(name, t));
        }
        if (!legends.isEmpty()) {
            if (legends.size() != all.size())
                throw new IllegalArgumentException("Visualization: " + legends.size() + " legends for " + all.size() + " series " + all.keySet());
            Map<String, double[]> renamed = new LinkedHashMap<>();
            int i = 0;
            for (double[] v : all.values()) renamed.put(legends.get(i++), v);
            return renamed;
        }
        return all;
    }

    /**
     * The series of one data name: its base is an attribute value (the compartments it selects);
     * each index is either an attribute name, which splits the series by the values of that
     * attribute (I_species: one series per species), or an attribute value, which restricts the
     * series to it (I_birds_zone: the infectious birds, one series per zone).
     */
    private static Map<String, double[]> seriesOf(Ast.Indexed name, Trajectory t) {
        ComposedModel m = t.model.model;
        Attribute own = m.attributeOfValue(name.base);
        if (own == null) throw new IllegalArgumentException("Visualization: '" + name + "' does not start with an attribute value");
        List<String> groupBy = new ArrayList<>();
        Map<String, String> fixed = new LinkedHashMap<>(); // attribute -> value
        fixed.put(own.name, name.base);
        for (String token : name.indices) {
            if (m.attribute(token) != null) groupBy.add(token);
            else if (m.attributeOfValue(token) != null) fixed.put(m.attributeOfValue(token).name, token);
            else throw new IllegalArgumentException("Visualization: '" + name + "': " + token + " is neither an attribute nor a value");
        }
        Map<String, double[]> out = new LinkedHashMap<>();
        // keys in the order of the attributes' values: pre-create them
        List<String> keys = new ArrayList<>();
        keys.add("");
        for (String a : groupBy) {
            List<String> next = new ArrayList<>();
            for (String k : keys) for (String v : m.attribute(a).values) next.add(k.isEmpty() ? v : k + "_" + v);
            keys = next;
        }
        String prefix = String.join("_", fixed.values());
        for (String k : keys) out.put(k.isEmpty() ? prefix : prefix + "_" + k, new double[t.times.length]);
        List<Compartment> comps = m.compartments;
        for (int c = 0; c < comps.size(); c++) {
            Compartment comp = comps.get(c);
            boolean selected = true;
            for (Map.Entry<String, String> f : fixed.entrySet()) selected &= f.getValue().equals(comp.get(f.getKey()));
            if (!selected) continue;
            StringBuilder key = new StringBuilder(prefix);
            for (String a : groupBy) key.append('_').append(comp.get(a));
            double[] s = out.get(key.toString());
            for (int i = 0; i < t.times.length; i++) s[i] += t.states[i][c];
        }
        return out;
    }
}
