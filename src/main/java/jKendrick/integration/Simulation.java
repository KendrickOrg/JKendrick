package jKendrick.integration;

import jKendrick.core.ISolver;

public class Simulation {
	private final ISolver solver;
	private final Visualization viz;
	private final String title;

	public Simulation(ISolver solver, Visualization viz, String title) {
		this.solver = solver;
		this.viz = viz;
		this.title = title;
	}
	private boolean isValid(double val) {
		return !(Double.isNaN(val) || Double.isInfinite(val));
	}



	public void simulate() {
		solver.solve();
		double[][] results = solver.getResult();
		for (int i = 0; i < results.length; i++) {
			for (int j = 0; j < results[i].length; j++) {
				if (!isValid(results[i][j])) {
					System.err.printf("Step=%d Compartment=%d => %f (INVALIDE)\n", i, j, results[i][j]);  // Affiche un message d'erreur si la valeur est invalide (NaN ou infinie).
				}
			}
		}
		// Si le solveur est stochastique, génère un graphique pour plusieurs simulations (multi-runs).
		// Sinon, génère un graphique pour une seule simulation (déterministe).
		if (solver.isStochastic()) {
			viz.getChart(solver.getMultiRunResult(), solver.getTimes(), solver.getLabels(), title);
		} else {
			viz.getChart(solver.getResult(), solver.getTimes(), solver.getLabels(), title);
		}
	}
}
