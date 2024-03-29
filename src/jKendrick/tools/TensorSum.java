package jKendrick.tools;

public class TensorSum {
	
	public static double[][][][] getTensorSum(double[][] a,double[][] b){
		int xA=a.length;
		int yA=a[0].length;
		int xB=b.length;
		int yB=b[0].length;
		assert xA==yA && xB==yB;
		double[][][][] s=new double[xA][yA][xB][yB];
		double[][] iA=getIdentity(a);
		double[][] iB=getIdentity(b);
		double[][][][] pA=tensorProduct(a,iB);
		double[][][][] pB=tensorProduct(iA,b);
		for(int i=0;i<xA;++i) {
			for(int j=0;j<yA;++j) {
				for(int k=0;k<xB;++k) {
					for(int l=0;l<yB;++l) {
						s[i][j][k][l]=pA[i][j][k][l]+pB[i][j][k][l];
					}
				}
			}
		}
		return s;
	}
	
	public static double[][] getIdentity(double[][] m){
		int x=m.length;
		int y=m[0].length;
		assert x==y;
		double[][] identity=new double[x][y];
		for(int i=0; i<x;++i) {
			for(int j=0;j<y;++j) {
				if(i==j) {
					identity[i][j]=1.;
				}
				else {
					identity[i][j]=0.;
				}
			}
		}
		return identity;
	}
	
	public static double[][][][] tensorProduct(double[][] a, double[][] b){
		int xA=a.length;
		int yA=a[0].length;
		int xB=b.length;
		int yB=b[0].length;
		assert xA==yA && xB==yB;
		double[][][][] tP=new double[xA][yA][xB][yB];
		for(int i=0;i<xA;++i) {
			for(int j=0;j<yA;++j) {
				for(int k=0;k<xB;++k) {
					for(int l=0;l<yB;++l) {
						tP[i][j][k][l]=a[i][j]*b[k][l];
					}
				}
			}
		}
		return tP;	
	}

}
