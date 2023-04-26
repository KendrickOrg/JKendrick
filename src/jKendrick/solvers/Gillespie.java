package jKendrick.solvers;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import jKendrick.events.IEvent;
import jKendrick.tools.RouletteWheel;

public class Gillespie {
	private int nbCycle;
	private int nbSteps;
	private Map<String,Integer> nbIndiv;
	private double[][][] result;//[cycle][step][compartment]
	private String[] compartments;
	private IEvent[] events;
	private Random random;
	
	public Gillespie(int nbCycle, int nbStep, Map<String,Integer>nbIndiv, IEvent[] events) {
		this.nbCycle=nbCycle;
		this.nbSteps=nbStep;
		this.nbIndiv=nbIndiv;
		this.compartments=(String[]) nbIndiv.keySet().toArray();
		this.result=initResult();
		this.random=new Random();
		
	}
	
	public double[][][] initResult(){
		double result[][][]=new double[nbCycle][nbSteps][nbIndiv.size()+1];
		Arrays.fill(result, 0.);
		int j=0;
		for(int i=0;i<nbCycle;++i) {
			for(Map.Entry<String, Integer> entry : nbIndiv.entrySet()) {
				result[i][0][j]=(double)entry.getValue();
				++j;
			}
			result[i][0][j]=0.;
			j=0;
		}
		return result;
	}
	
	public double[] getRates() {
		double[] r=new double[events.length];
		for(int i=0;i<r.length;++i) {
			r[i]=events[i].getRate();
		}
		return r;
	}
	
	public double getR() {
		double[] rates=getRates();
		double r=0;
		for(int i=0;i<rates.length;++i) {
			r+=rates[i];
		}
		return r;
	}
	
	public double[][][] solve(){
		double r=getR();
		int timeRow=nbIndiv.size();
		for(int i=0;i<nbCycle;++i) {
			for(int j=1;j<nbSteps;++j) {
				double rand1=random.nextDouble();
				double tau=1/r*Math.log(1/rand1);
				RouletteWheel rw=new RouletteWheel(getRates(),0.0000001);
				int currentEvent=rw.getEvent();
				result[i][j]=events[currentEvent].action(compartments, result[i][j-1]);
				result[i][j][timeRow]=result[i][j-1][timeRow]+tau;
			}
		}
		return result;
	}
	
	public double[][] getAverage(){
		double[][] averages=new double[nbSteps][nbIndiv.size()+1];
		double x=0.;
		for(int i=0;i<nbSteps;++i) {
			for(int j=0;j<nbIndiv.size()+1;++j) {
				x=0.;
				for(int k=0;k<nbCycle;k++) {
					x+=result[k][i][j];
				}
				x=x/(double)nbCycle;
				averages[i][j]=x;
			}
		}
		return averages;
	}
}
