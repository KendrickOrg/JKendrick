package jKendrick.solvers;

import jKendrick.core.ISolver;
import jKendrick.core.Scenario;
import jKendrick.utils.PoissonGenerator;

import java.util.List;
import java.util.Map;

public class TauLeap implements ISolver {
    private final Scenario scenario;
    private final double step, end;
    private final int nbRuns;
    private double[][][] allRuns;
    private final PoissonGenerator poisson = new PoissonGenerator();

    public TauLeap(Scenario scenario, double step, double end, int nbRuns) {
        this.scenario = scenario;
        this.step = step;
        this.end = end;
        this.nbRuns = nbRuns;
    }

    @Override
    public void solve() {
        int steps = (int) (end / step) + 1;
        int nbCompartments = scenario.getNbCompartments();
        allRuns = new double[nbRuns][steps][nbCompartments];

        for (int run = 0; run < nbRuns; run++) {
            scenario.reset();
            double[][] result = new double[steps][nbCompartments];
            result[0] = normalize(scenario.getCardinalities());

            for (int t = 1; t < steps; t++) {
                // Copier état précédent
                for (int i = 0; i < nbCompartments; i++) {
                    scenario.getCardinalities()[i] = result[t - 1][i] * scenario.getModel().getParameter("population");
                }

                List<Map<String, String>> events = scenario.getPossibleEvents();

                for (Map<String, String> event : events) {
                    double rate = scenario.getRate(event.get("from"), event.get("to"));
                    int n = poisson.poissonSample(rate * step);

                    for (int k = 0; k < n; k++) {
                        scenario.transition(event.get("from"), event.get("to"));
                    }
                }

                result[t] = normalize(scenario.getCardinalities());
            }

            allRuns[run] = result;
        }
    }

    private double[] normalize(double[] array) {
        double pop = scenario.getModel().getParameter("population");
        double[] norm = new double[array.length];
        for (int i = 0; i < array.length; i++) norm[i] = array[i] / pop;
        return norm;
    }

    @Override public double[][] getResult() { return getMedianPath(); }

    @Override public double[][][] getMultiRunResult() { return allRuns; }

    @Override public boolean isStochastic() { return true; }

    @Override public List<String> getLabels() {
        return scenario.getCompartments();
    }

    @Override public double[] getTimes() {
        int len = allRuns[0].length;
        double[] times = new double[len];
        for (int i = 0; i < len; i++) times[i] = i * step;
        return times;
    }

    private double[][] getMedianPath() {
        int medianIndex = nbRuns / 2;
        return allRuns[medianIndex];
    }
}