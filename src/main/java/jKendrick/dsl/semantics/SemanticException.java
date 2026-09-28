package jKendrick.dsl.semantics;

/** A script that parses but does not denote a model: unknown names, wrong shapes, dependencies between concerns... */
public final class SemanticException extends RuntimeException {
    public SemanticException(String message) {
        super(message);
    }
}
