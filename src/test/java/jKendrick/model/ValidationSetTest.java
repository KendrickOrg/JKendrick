package jKendrick.model;

import jKendrick.Kendrick;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The validation set of the translation (Section 4.5 of the paper): the semantic cases that one
 * trajectory does not exercise, checked on the derivatives and on the event system (the transitions
 * of the composed model with their rates) at chosen states, against values computed by hand.
 */
class ValidationSetTest {

    static final double EPS = 1e-12;

    static String sir(String extra) {
        return "KModel SIR\n"
                + "    attribute: #(status in (S I R));\n"
                + "    parameters: #(beta gamma lambda);\n"
                + "    lambda: #(beta*I/N);\n"
                + "    transitions: #(S -- lambda --> I. I -- gamma --> R).\n" + extra
                + "Simulation Sim solver: 'rungeKutta'; scenario: 'Sc'; from: 0; to: 1; step: 1.\n";
    }

    /** The compartment of the model with these attribute values (attribute, value, attribute, value...). */
    static Compartment compartment(GroundModel g, String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
        for (Compartment c : g.model.compartments) {
            boolean match = true;
            for (Map.Entry<String, String> e : m.entrySet()) if (!e.getValue().equals(c.get(e.getKey()))) match = false;
            if (match) return c;
        }
        throw new IllegalArgumentException("no compartment " + m);
    }

    static double derivative(GroundModel g, double[] state, String... pairs) {
        return g.derivatives(0, state)[g.model.indexOf(compartment(g, pairs))];
    }

    /** The transitions from one compartment to another with their rates in the state. */
    static List<Double> rates(GroundModel g, double[] state, Compartment from, Compartment to) {
        List<Double> r = new ArrayList<>();
        for (ComposedModel.Transition tr : g.transitions())
            if (from.equals(tr.from) && to.equals(tr.to)) r.add(g.rate(tr, 0, state));
        return r;
    }

    @Test
    void aHeterogeneousRateGivesEachSpeciesItsOwnTransition() {
        GroundModel g = Kendrick.load(sir("KModel Species attribute: #(species in (humans birds)).\n"
                + "Composition C model: 'SIR'; model: 'Species'.\n"
                + "Scenario Sc on: 'C'; beta: 0; gamma_species: #(0.25 0.1); S_species: #(100 100); I_species: #(10 20).\n"));
        double[] y = g.initialState();
        // dR/dt = gamma_species * I_species: 0.25 * 10 and 0.1 * 20
        assertEquals(2.5, derivative(g, y, "status", "R", "species", "humans"), EPS);
        assertEquals(2.0, derivative(g, y, "status", "R", "species", "birds"), EPS);
        // one I -> R transition per species, with the species' rate
        assertEquals(List.of(0.25), rates(g, y, compartment(g, "status", "I", "species", "humans"), compartment(g, "status", "R", "species", "humans")));
        assertEquals(List.of(0.1), rates(g, y, compartment(g, "status", "I", "species", "birds"), compartment(g, "status", "R", "species", "birds")));
    }

    @Test
    void aFunctionalRateAcrossConcernsSumsOverTheOtherConcern() {
        GroundModel g = Kendrick.load(sir("KModel Species attribute: #(species in (humans birds)).\n"
                + "Composition C model: 'SIR'; model: 'Species'.\n"
                + "Scenario Sc on: 'C'; var: #((k in species) (i in species)); gamma: 0;\n"
                + "    beta_ki: #(#(0.5 0.1) #(0.2 0.3)); lambda_k: #(sum_ (i in species) beta_ki*I_i/N_i);\n"
                + "    S_species: #(90 180); I_species: #(10 20).\n"));
        double[] y = g.initialState();
        // lambda_humans = 0.5 * 10/100 + 0.1 * 20/200 = 0.06; lambda_birds = 0.2 * 10/100 + 0.3 * 20/200 = 0.05
        assertEquals(-0.06 * 90, derivative(g, y, "status", "S", "species", "humans"), EPS);
        assertEquals(-0.05 * 180, derivative(g, y, "status", "S", "species", "birds"), EPS);
        List<Double> r = rates(g, y, compartment(g, "status", "S", "species", "humans"), compartment(g, "status", "I", "species", "humans"));
        assertEquals(1, r.size());
        assertEquals(0.06, r.get(0), EPS);
    }

    @Test
    void mixedInheritedAndAcquiredEntryStatesPlaceTheBirthsPerZoneAmongTheFree() {
        String si = "KModel SI\n"
                + "    attribute: #(status in (S I));\n"
                + "    parameters: #(beta mu);\n"
                + "    transitions: #(S -- beta*I/N --> I. Empty -- mu*N --> S. status -- mu --> Empty).\n"
                + "KModel Zone attribute: #(zone in (a b)).\n"
                + "KModel Isolation attribute: #(isolation in (free isolated)); entry: #(free).\n"
                + "Composition C model: 'SI'; model: 'Zone'; model: 'Isolation'.\n"
                + "Scenario Sc on: 'C'; beta: 0.5; mu: 0.1; I: 0;\n"
                + "    S_a_free: 80; S_a_isolated: 20; S_b_free: 40; S_b_isolated: 10.\n"
                + "Simulation Sim solver: 'rungeKutta'; scenario: 'Sc'; from: 0; to: 1; step: 1.\n";
        GroundModel g = Kendrick.load(si);
        double[] y = g.initialState();
        // zone a: births mu * N_a = 10 enter S_a_free; deaths 8 among the free and 2 among the isolated
        assertEquals(10.0 - 8.0, derivative(g, y, "status", "S", "zone", "a", "isolation", "free"), EPS);
        assertEquals(-2.0, derivative(g, y, "status", "S", "zone", "a", "isolation", "isolated"), EPS);
        // zone b: births 5, deaths 4 and 1
        assertEquals(5.0 - 4.0, derivative(g, y, "status", "S", "zone", "b", "isolation", "free"), EPS);
        assertEquals(-1.0, derivative(g, y, "status", "S", "zone", "b", "isolation", "isolated"), EPS);
        // the population of each zone is conserved: births equal deaths
        double da = 0, db = 0;
        double[] d = g.derivatives(0, y);
        for (Compartment c : g.model.compartments) {
            if (c.get("zone").equals("a")) da += d[g.model.indexOf(c)]; else db += d[g.model.indexOf(c)];
        }
        assertEquals(0.0, da, EPS);
        assertEquals(0.0, db, EPS);
    }

    @Test
    void twoTransitionsBetweenTheSameCompartmentsStayTwoEventsWhoseRatesAdd() {
        String two = "KModel SI\n"
                + "    attribute: #(status in (S I));\n"
                + "    parameters: #(beta lambda iota);\n"
                + "    lambda: #(beta*I/N);\n"
                + "    transitions: #(S -- lambda --> I. S -- iota --> I).\n"
                + "Composition C model: 'SI'.\n"
                + "Scenario Sc on: 'C'; beta: 0.5; iota: 0.02; S: 90; I: 10.\n"
                + "Simulation Sim solver: 'rungeKutta'; scenario: 'Sc'; from: 0; to: 1; step: 1.\n";
        GroundModel g = Kendrick.load(two);
        double[] y = g.initialState();
        // lambda = 0.5 * 10 / 100 = 0.05: two events S -> I, rates 0.05 and 0.02, dS/dt = -(0.05 + 0.02) * 90
        assertEquals(List.of(0.05, 0.02), rates(g, y, compartment(g, "status", "S"), compartment(g, "status", "I")));
        assertEquals(-0.07 * 90, derivative(g, y, "status", "S"), EPS);
        assertEquals(0.07 * 90, derivative(g, y, "status", "I"), EPS);
    }

    @Test
    void twoCoincidentTermsOfEquationsInFlowFormAreTwoEvents() {
        String two = "KModel SI\n"
                + "    attribute: #(status in (S I));\n"
                + "    parameters: #(beta lambda iota);\n"
                + "    lambda: #(beta*I/N);\n"
                + "    equations: #(S:t=0 - lambda*S - iota*S. I:t=lambda*S + iota*S).\n"
                + "Composition C model: 'SI'.\n"
                + "Scenario Sc on: 'C'; beta: 0.5; iota: 0.02; S: 90; I: 10.\n"
                + "Simulation Sim solver: 'rungeKutta'; scenario: 'Sc'; from: 0; to: 1; step: 1.\n";
        GroundModel g = Kendrick.load(two);
        double[] y = g.initialState();
        assertEquals(List.of(0.05, 0.02), rates(g, y, compartment(g, "status", "S"), compartment(g, "status", "I")));
        assertEquals(-0.07 * 90, derivative(g, y, "status", "S"), EPS);
    }

    /** The compartment's attribute values, in a fixed order of the attributes, as a key independent of the model. */
    static String key(Compartment c, List<String> attributes) {
        StringBuilder sb = new StringBuilder();
        for (String a : attributes) sb.append(a).append('=').append(c.get(a)).append(';');
        return sb.toString();
    }

    @Test
    void theCompositionInTheOtherOrderIsTheSameModelUpToThePermutationOfTheCompartments() {
        String zones = "KModel Zones\n"
                + "    attribute: #(zone in (a b));\n"
                + "    parameters: #(rho);\n"
                + "    transitions: #(a -- rho --> b. b -- 2*rho --> a).\n";
        String scenario = "Scenario Sc on: 'C'; beta: 0.5; gamma: 0.25; rho: 0.1; S_zone: #(80 40); I_zone: #(10 20); R: 0.\n";
        GroundModel g1 = Kendrick.load(sir(zones + "Composition C model: 'SIR'; model: 'Zones'.\n" + scenario));
        GroundModel g2 = Kendrick.load(sir(zones + "Composition C model: 'Zones'; model: 'SIR'.\n" + scenario));
        List<String> attributes = new ArrayList<>();
        for (Attribute a : g1.model.attributes) attributes.add(a.name);
        java.util.Collections.sort(attributes);
        // the compartments are a permutation of each other
        Map<String, Integer> i1 = new TreeMap<>(), i2 = new TreeMap<>();
        for (Compartment c : g1.model.compartments) i1.put(key(c, attributes), g1.model.indexOf(c));
        for (Compartment c : g2.model.compartments) i2.put(key(c, attributes), g2.model.indexOf(c));
        assertEquals(i1.keySet(), i2.keySet());
        assertEquals(6, i1.size());
        // a random admissible state, the same under the permutation
        Random random = new Random(5);
        double[] y1 = new double[6], y2 = new double[6];
        for (String k : i1.keySet()) { double v = random.nextInt(200); y1[i1.get(k)] = v; y2[i2.get(k)] = v; }
        // the derivatives agree under the permutation
        double[] d1 = g1.derivatives(0, y1), d2 = g2.derivatives(0, y2);
        for (String k : i1.keySet()) assertEquals(d1[i1.get(k)], d2[i2.get(k)], EPS, k);
        // the event systems are equal as multisets of (source, target, rate)
        assertEquals(events(g1, y1, attributes), events(g2, y2, attributes));
        assertEquals(10, g1.transitions().size()); // 2 x 2 infection and recovery, 3 x 2 migrations
    }

    static Map<String, Integer> events(GroundModel g, double[] state, List<String> attributes) {
        Map<String, Integer> m = new TreeMap<>();
        for (ComposedModel.Transition tr : g.transitions()) {
            String e = key(tr.from, attributes) + " -> " + key(tr.to, attributes) + " @ " + Math.round(g.rate(tr, 0, state) * 1e9);
            m.merge(e, 1, Integer::sum);
        }
        return m;
    }
}
