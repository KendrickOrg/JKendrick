package jKendrick;

import jKendrick.dsl.ast.Ast;
import jKendrick.dsl.parser.KendrickParserFacade;
import jKendrick.dsl.semantics.ModelBuilder;
import jKendrick.model.GroundModel;
import jKendrick.simulation.SimulationRunner;
import jKendrick.simulation.Trajectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Entry point of the DSL pipeline: script -> AST -> ground model -> trajectories.
 *
 * Command line: {@code java -cp ... jKendrick.Kendrick model.kendrick [out.csv]} runs the script's
 * Simulation entity and prints the totals per status (and species) at the last time, writing the
 * full trajectory of the first run to the CSV file when one is given.
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

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: jKendrick.Kendrick <script.kendrick> [out.csv]");
            System.exit(2);
        }
        GroundModel model = load(Paths.get(args[0]));
        System.out.printf("%s: %d concerns, %d compartments, %d transitions%n", model.model.name,
                model.model.concerns.size(), model.model.compartments.size(), model.transitions().size());
        List<Trajectory> runs = run(model);
        Trajectory first = runs.get(0);
        System.out.printf("%d run(s), t = %s .. %s%n", runs.size(), first.times[0], first.times[first.size() - 1]);
        String[] keys = model.model.attribute("species") == null ? new String[]{"status"} : new String[]{"status", "species"};
        for (Map.Entry<String, double[]> e : first.aggregate(keys).entrySet())
            System.out.printf("  %-24s %12.4f%n", e.getKey(), e.getValue()[first.size() - 1]);
        if (args.length > 1) {
            Files.write(Paths.get(args[1]), first.toCsv().getBytes());
            System.out.println("trajectory written to " + args[1]);
        }
    }
}
