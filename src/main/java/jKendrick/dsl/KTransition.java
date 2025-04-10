package jKendrick.dsl;

public class KTransition {
    private final String from;
    private final String to;
    private final String rateExpression;

    public KTransition(String from, String to, String rateExpression) {
        this.from = from;
        this.to = to;
        this.rateExpression = rateExpression;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getRateExpression() {
        return rateExpression;
    }

    @Override
    public String toString() {
        return from + " -> " + to + " : " + rateExpression;
    }
}
