package jKendrick;



import java.io.IOException;


import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;

import jKendrick.models.SEIRModel;
import jKendrick.solvers.RK4Solver;


public class MainSEIR {
	public static void main(String[] args) {
		double step = 1;
		double last = 21900.;
		
		double[] arguments ={ 0.1, 0.0001, 0.0001, 0};
		double [][] results = 	integratorExample(step, last, arguments);
		VisualizationSEIR viz = new VisualizationSEIR ();
		
		try {
			viz.xchartExample(results, step, last);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static double[][] integratorExample(double step, double last,
			double[] args) {
		RK4Solver rk4 = new RK4Solver(step);
		SEIRModel ode = new SEIRModel(1.4247, 0.14286, 0.0000391, 0.07143);
		//SIRModelWithDemography ode = new SIRModelWithDemography (1.4247, 0.14286, 0.0000391);
		//SISModel ode = new SISModel(1.4247, 0.14286);
		int nbArgs = args.length;
		int duration = (int) Math.ceil(last / step);
		double[][] results = new double[nbArgs][duration];
		double t = 0.0;
		int i = 0;
		do {
			//System.out.format("Conditions at time %.1f:  S:%.1f  I:%.1f  R:%.1f%n",
			System.out.format("Conditions at time %.1f:  S:%.1f E:%.1f I:%.1f R:%.1f%n",
					t, args[0],  args[1], args[2], args[3]);
			i = (int)(t/step);
			for(int j = 0; j< nbArgs; ++j)
				results[j][i]= args[j];
			rk4.integrate(ode, t, args, t + step, args);
			t += step;
			++i;
			
		} while (t <last);	
		return results;
	}

	
}
