package jKendrick.IHM.ui.KendrickFile;

public class ModelParameters {
    private final String scenario;
    private final String solver;
    private final double beta, gamma, mu, sigma, step, end;
    private final double[] population;

    public ModelParameters(String scenario, String solver, double beta, double gamma,
                           double mu, double sigma, double step, double end, double[] population) {
        this.scenario = scenario;
        this.solver = solver;
        this.beta = beta;
        this.gamma = gamma;
        this.mu = mu;
        this.sigma = sigma;
        this.step = step;
        this.end = end;
        this.population = population.clone();
    }

    // Getters
    public String getScenario() { return scenario; }
    public String getSolver() { return solver; }
    public double getBeta() { return beta; }
    public double getGamma() { return gamma; }
    public double getMu() { return mu; }
    public double getSigma() { return sigma; }
    public double getStep() { return step; }
    public double getEnd() { return end; }
    public double[] getPopulation() { return population.clone(); }
}