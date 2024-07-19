package jKendrick.simulation;

import java.io.IOException;

import jKendrick.IHM.Visualization;
import jKendrick.core.ISolver;

public class Simulation {
	private ISolver solver;
	private Visualization v;
	private String title;
	private double step;
	private double end;

	public Simulation(ISolver solver, Visualization v, String title, double step, double end) {
		this.solver = solver;
		this.v = v;
		this.title = title;
		this.step = step;
		this.end = end;
	}

	public double getStep() {
		return step;
	}

	public double getEnd() {
		return end;
	}

	public void simulate() {
		solver.solve();
		try {
			v.getChart(solver, title, "time", "population");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
