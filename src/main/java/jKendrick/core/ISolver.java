package jKendrick.core;

import java.util.List;

public interface ISolver {
    /**
     * Résout le système ou la modélisation définie dans l'implémentation du solveur.
     * La méthode effectue les calculs nécessaires et stocke les résultats pour y accéder ultérieurement.
     */
    void solve();
    double[][] getResult();
    default double[][][] getMultiRunResult() { return new double[][][] { getResult() }; }
    default boolean isStochastic() { return false; }
    List<String> getLabels();
    double[] getTimes();
}
