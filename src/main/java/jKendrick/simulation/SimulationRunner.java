package jKendrick.simulation;

import jKendrick.dsl.ast.Ast;
import jKendrick.model.GroundModel;

import java.util.List;
import java.util.Map;

/**
 * Interprets the {@code Simulation} entity of a script and runs it:
 * <pre>
 * Simulation InfluenzaSim
 *     solver: 'rungeKutta';      -- or 'gillespie', 'nextReaction', 'tauLeap'
 *     scenario: 'Scr1';
 *     from: 0; to: 1500; step: 1;
 *     runs: 10; seed: 42;        -- stochastic solvers only (defaults: 1 run, seed 0)
 *     substeps: 4;               -- integration or leap sub-steps per recording step (default 1)
 *     panels: 8.                 -- nextReaction only: Simpson panels per step for rates that read the time (default 8)
 * </pre>
 */
public final class SimulationRunner {
    private SimulationRunner() {}

    public static final class Settings {
        public final String solver;
        public final double from, to, step;
        public final int runs, substeps, panels;
        public final long seed;

        Settings(String solver, double from, double to, double step, int runs, long seed, int substeps, int panels) {
            this.solver = solver; this.from = from; this.to = to; this.step = step;
            this.runs = runs; this.seed = seed; this.substeps = substeps; this.panels = panels;
        }

        public static Settings of(Map<String, Ast.Literal> args) {
            String solver = string(args, "solver", "rungeKutta");
            double from = number(args, "from", 0), to = number(args, "to", -1), step = number(args, "step", 1);
            if (to < 0) throw new IllegalArgumentException("Simulation: 'to:' is required");
            if (step <= 0 || to <= from) throw new IllegalArgumentException("Simulation: need from < to and step > 0");
            int runs = (int) number(args, "runs", 1), substeps = (int) number(args, "substeps", 1);
            long seed = (long) number(args, "seed", 0);
            int panels = (int) number(args, "panels", 8);
            return new Settings(solver, from, to, step, runs, seed, substeps, panels);
        }

        public Solver solver() {
            switch (solver) {
                case "rungeKutta": case "rk4": return new RungeKuttaSolver(substeps);
                case "gillespie": return new GillespieSolver(runs, seed);
                case "nextReaction": case "anderson": return new NextReactionSolver(runs, seed, panels);
                case "tauLeap": return new TauLeapSolver(runs, seed, substeps);
                default: throw new IllegalArgumentException("Simulation: unknown solver '" + solver
                        + "' (rungeKutta, gillespie, nextReaction, tauLeap)");
            }
        }

        private static String string(Map<String, Ast.Literal> args, String key, String dflt) {
            Ast.Literal l = args.get(key);
            if (l == null) return dflt;
            if (l.string != null) return l.string;
            if (l.identifier != null) return l.identifier;
            throw new IllegalArgumentException("Simulation: '" + key + ":' expects a string");
        }

        private static double number(Map<String, Ast.Literal> args, String key, double dflt) {
            Ast.Literal l = args.get(key);
            if (l == null) return dflt;
            if (l.number == null) throw new IllegalArgumentException("Simulation: '" + key + ":' expects a number");
            return l.number;
        }
    }

    /** Runs the ground model with the settings of its Simulation entity. */
    public static List<Trajectory> run(GroundModel model) {
        return run(model, Settings.of(model.simulation));
    }

    public static List<Trajectory> run(GroundModel model, Settings settings) {
        return settings.solver().solve(model, settings.from, settings.to, settings.step);
    }
}
