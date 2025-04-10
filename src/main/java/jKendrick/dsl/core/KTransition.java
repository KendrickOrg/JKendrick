package jKendrick.dsl.core;

public class KTransition {
    private final String from;
    private final String to;

    public KTransition(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    @Override
    public String toString() {
        return from + " → " + to;
    }
}
