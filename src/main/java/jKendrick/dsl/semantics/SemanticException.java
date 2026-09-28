package jKendrick.dsl.semantics;

/** A script that parses but does not denote a model: unknown names, wrong shapes, dependencies between concerns... */
public final class SemanticException extends RuntimeException {
    public SemanticException(String message) {
        super(message);
    }

    /** The same error, located at the node, unless the message is already located. */
    public SemanticException at(jKendrick.dsl.ast.Ast.Node node) {
        if (node == null || node.line <= 0 || getMessage().contains("(line ")) return this;
        return new SemanticException(getMessage() + node.position());
    }
}
