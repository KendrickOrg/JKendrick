package jKendrick.core;

import java.util.List;

public interface ISolver {
    void solve();
    double[][] getResult();
    default double[][][] getMultiRunResult() { return new double[][][] { getResult() }; }
    default boolean isStochastic() { return false; }
    List<String> getLabels();
    double[] getTimes();
}
