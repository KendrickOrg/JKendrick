package jKendrick.solvers;

import jKendrick.core.ISolver;
import jKendrick.core.Scenario;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Gillespie implements ISolver {
    private final Scenario scenario;
    private final double step, end;
    private final int nbRuns;
    private double[][][] allRuns;

    public Gillespie(Scenario scenario, double step, double end, int nbRuns) {
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
            double[] current = scenario.getCardinalities().clone();
            double[][] result = new double[steps][nbCompartments];

            double time = 0.0;
            int index = 0;
            Random rand = new Random(run);

            result[index++] = normalize(current);

            while (time < end && index < steps) {
                List<Map<String, String>> events = scenario.getPossibleEvents();
                double[] rates = new double[events.size()];
                double totalRate = 0;

                for (int i = 0; i < events.size(); i++) {
                    Map<String, String> event = events.get(i);
                    rates[i] = scenario.getRate(event.get("from"), event.get("to"));
                    totalRate += rates[i];
                }

                if (totalRate <= 0) break;

                double tau = -Math.log(rand.nextDouble()) / totalRate;
                time += tau;

                double threshold = rand.nextDouble() * totalRate;
                double sum = 0;
                for (int i = 0; i < events.size(); i++) {
                    sum += rates[i];
                    if (sum >= threshold) {
                        Map<String, String> event = events.get(i);
                        scenario.transition(event.get("from"), event.get("to"));
                        break;
                    }
                }

                while (index < steps && index * step < time) {
                    result[index++] = normalize(scenario.getCardinalities());
                }
            }

            while (index < steps) {
                result[index++] = normalize(scenario.getCardinalities());
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

    @Override public List<String> getLabels() { return scenario.getCompartments(); }

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