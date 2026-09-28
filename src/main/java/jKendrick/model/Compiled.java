package jKendrick.model;

/**
 * An expression compiled for a fixed compartment, concern and variable environment: only the
 * time and the state vary between evaluations. See {@link RateCompiler}.
 */
@FunctionalInterface
public interface Compiled {
    double eval(double t, double[] state);

    /** A constant, so that the compiler can fold products of constants. */
    final class Const implements Compiled {
        public final double value;

        public Const(double value) {
            this.value = value;
        }

        @Override
        public double eval(double t, double[] state) {
            return value;
        }
    }
}
