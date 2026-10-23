package jKendrick;

import jKendrick.dsl.ast.Ast;
import jKendrick.dsl.parser.KendrickParserFacade;
import jKendrick.dsl.semantics.ModelBuilder;
import jKendrick.model.GroundModel;
import jKendrick.simulation.ChartRenderer;
import jKendrick.simulation.SimulationRunner;
import jKendrick.simulation.Trajectory;
import jKendrick.simulation.VisualizationSpec;
import org.knowm.xchart.XYChart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Entry point of the DSL pipeline: script -> AST -> ground model -> trajectories.
 *
 * Command line: {@code java -cp ... jKendrick.Kendrick model.kendrick [--csv out.csv] [--png out.png] [--show]}
 * runs the script's Simulation entity and prints the totals per status (and species) at the last
 * time. {@code --csv} writes the full trajectory of the first run; {@code --png} draws the script's
 * Visualization entities (one file per entity when there are several, suffixed with the entity's
 * name) and {@code --show} opens them in windows. Without a Visualization entity the chart shows
 * one curve per status.
 */
public final class Kendrick {
    private Kendrick() {}

    public static GroundModel load(Path script) throws IOException {
        return build(KendrickParserFacade.parseFile(script));
    }

    public static GroundModel load(String source) {
        return build(KendrickParserFacade.parse(source));
    }

    public static GroundModel build(Ast.Script ast) {
        return ModelBuilder.build(ast);
    }

    public static List<Trajectory> run(GroundModel model) {
        return SimulationRunner.run(model);
    }

    /** The Visualization entities of the model as specifications; a default one when it has none. */
    public static List<VisualizationSpec> visualizations(GroundModel model) {
        List<VisualizationSpec> specs = new ArrayList<>();
        for (Map.Entry<String, Map<String, Ast.Literal>> e : model.visualizations.entrySet())
            specs.add(VisualizationSpec.of(e.getKey(), e.getValue()));
        if (specs.isEmpty()) specs.add(VisualizationSpec.of(model.name, Collections.emptyMap()));
        return specs;
    }

    public static void main(String[] args) throws IOException {
        String script = null, csv = null, png = null;
        boolean show = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--csv": csv = args[++i]; break;
                case "--png": png = args[++i]; break;
                case "--show": show = true; break;
                default:
                    if (script == null) script = args[i];
                    else if (csv == null) csv = args[i]; // legacy positional CSV
                    else script = null;
            }
        }
        if (script == null) {
            System.err.println("usage: jKendrick.Kendrick <script.kendrick> [--csv out.csv] [--png out.png] [--show]");
            System.exit(2);
        }
        GroundModel model = load(Paths.get(script));
        for (String w : model.model.warnings) System.err.println("warning: " + w);
        System.out.printf("%s: %d concerns, %d compartments, %d transitions%n", model.model.name,
                model.model.concerns.size(), model.model.compartments.size(), model.transitions().size());
        List<Trajectory> runs = run(model);
        Trajectory first = runs.get(0);
        System.out.printf("%d run(s), t = %s .. %s%n", runs.size(), first.times[0], first.times[first.size() - 1]);
        String[] keys = model.model.attribute("species") == null ? new String[]{"status"} : new String[]{"status", "species"};
        for (Map.Entry<String, double[]> e : first.aggregate(keys).entrySet())
            System.out.printf("  %-24s %12.4f%n", e.getKey(), e.getValue()[first.size() - 1]);
        if (csv != null) {
            Files.write(Paths.get(csv), first.toCsv().getBytes());
            System.out.println("trajectory written to " + csv);
        }
        if (png != null || show) {
            List<VisualizationSpec> specs = visualizations(model);
            for (VisualizationSpec spec : specs) {
                XYChart chart = ChartRenderer.chart(spec, runs);
                if (png != null) {
                    Path file = Paths.get(png);
                    if (specs.size() > 1) {
                        String name = file.getFileName().toString().replaceFirst("\\.png$", "") + "-" + spec.name + ".png";
                        file = file.resolveSibling(name);
                    }
                    ChartRenderer.savePng(chart, file);
                    System.out.println("chart written to " + file);
                }
                if (show) ChartRenderer.show(chart);
            }
        }
    }
}
