# JKendrick2

JKendrick2 is a Java implementation of [Kendrick](https://github.com/KendrickOrg/kendrick/wiki), a
domain-specific language for epidemiological modeling in which a model is composed from independent
*concerns* (the disease dynamics, the host species, the spatial structure, a control policy...). It is
the reference implementation of the *new* Kendrick syntax used by the paper "A Formal Approach to
Separation of Concerns in Epidemiology Modeling": a script is parsed, given its meaning by building
the objects of the mathematical meta-model (concerns as automata, their tensor-sum composition, the
ground model of a scenario), and run deterministically (RK4 on the composed ODE system, produced
automatically) or stochastically (Gillespie, tau-leap on the composed continuous-time Markov chain).

## Contents

- [Installation](#installation)
- [Usage](#usage)
  - [Writing a model](#writing-a-model)
  - [Running a script from the command line](#running-a-script-from-the-command-line)
  - [KendrickStudio, the graphical front end](#kendrickstudio-the-graphical-front-end)
  - [Using JKendrick2 from Java](#using-jkendrick2-from-java)
- [The language](#the-language)
  - [Key concepts](#key-concepts)
  - [Grammar](#grammar)
  - [Meaning of names](#meaning-of-names)
  - [Simulation and Visualization](#simulation-and-visualization)
  - [Errors](#errors)
- [How it works](#how-it-works)
- [Tests](#tests)
- [Publications](#publications)

## Installation

Requirements: a JDK 14 or later (17 recommended) and Apache Maven 3.6 or later. Check with
`java -version` and `mvn -version`.

    git clone https://github.com/KendrickOrg/JKendrick2.git
    cd JKendrick2
    mvn package

`mvn package` generates the parser from the grammar (`antlr4-maven-plugin`), compiles, runs the tests
and produces two jars in `target/`:

- `jkendrick2.jar`: a self-contained ("fat") jar including XChart and the ANTLR runtime, whose main
  class is the graphical front end; `java -jar target/jkendrick2.jar` is all that is needed to use it;
- `JKendrick2-0.1-SNAPSHOT.jar`: the classes alone, for use as a library with Maven.

`mvn package -DskipTests` skips the tests (about ten seconds). In IntelliJ IDEA or Eclipse, open the
project as a Maven project: the generated parser (`target/generated-sources/antlr4`, package
`jKendrick.dsl.grammar`) is picked up automatically; do not commit it.

The dependencies are [XChart](https://knowm.org/open-source/xchart/) 3.8.8 (charts), the
[ANTLR 4](https://www.antlr.org/) 4.13.2 runtime (parsing) and JUnit 5 (tests only); Maven downloads
them.

## Usage

### Writing a model

A script is a text file, conventionally `*.kendrick`, made of entities ended by a period. The
following script is `src/test/resources/models/Model1.kendrick`, an SEIRS model of influenza:

    KModel SEIRS
        attribute: #(status in (S E I R));
        parameters: #(beta lambda gamma mu sigma nu);
        lambda: #(beta*I/N);
        equations: #(
             S:t=mu*N - lambda*S - mu*S + nu*R.
             E:t=lambda*S - sigma*E - mu*E.
             I:t=sigma*E - gamma*I - mu*I.
             R:t=gamma*I - mu*R - nu*R.).

    Composition Influenza
        model: 'SEIRS'.

    Scenario Scr1
        on: 'Influenza';
        mu: 0.00137;
        beta: 0.5;
        gamma: 0.233;
        sigma: 0.67;
        nu: 0.00274;
        S: 49900;
        I: 100.

    Simulation InfluenzaSim
        solver: 'rungeKutta';
        scenario: 'Scr1';
        from: 0;
        to: 1500;
        step: 1.

    Visualization InfluenzaViz
        plot: 'diagram';
        for: 'InfluenzaSim';
        xLabel: 'Time (days)'.

A `KModel` is a concern: an attribute partitioning the population (here the infection `status`),
the parameters it introduces, and its dynamics, given as ODEs (`equations:`) or as transitions
between attribute values (`transitions: #(S -- lambda --> E. E -- sigma --> I. ...)`). A
`Composition` lists the concerns of the model; a `Scenario` gives every parameter and initial
cardinality a value; a `Simulation` says how to run it and a `Visualization` what to plot.

Separation of concerns shows when the model grows. `Model2.kendrick` adds a second concern,

    KModel MultiSpecies
        attribute: #(species in (humans birds)).

and the composition `model: 'SEIRS'; model: 'MultiSpecies'` yields the 8-compartment model
(S/E/I/R x humans/birds) automatically; the scenario then gives the species-dependent values with
indexed names: `mu_species: #(0.0000365 0.00137)`, a transmission matrix `beta_ki: #(#(0 0.025) #(0 0.5))`
over variables bound by `var: #((k in species) (i in species))`, and the force of infection
`lambda_k: #(sum_ (i in species) beta_ki*I_i/N_i)`. `Model3.kendrick` adds a `MetaPopulation` concern
(15 zones on a one-way ring, `topology: #(ring oneWay)`, and a `transitionRate:`) and `Model4.kendrick` an `Isolation` concern
given by transitions; none of the earlier concerns is modified. The four scripts are the models of the
paper; `kendrick models/SYNTAX.md` in the paper repository is the language reference.

### Running a script from the command line

    java -cp target/jkendrick2.jar jKendrick.Kendrick model.kendrick [--csv out.csv] [--png out.png] [--show]

runs the script's `Simulation` entity and prints the model's size and the totals per status (and
species) at the end of the run:

    Influenza: 1 concerns, 4 compartments, 9 transitions
    1 run(s), t = 0.0 .. 1500.0
      S                          23422.4028
      E                            160.0782
      I                            458.5267
      R                          25958.9923

`--csv` writes the full trajectory of the first run (one column per compartment), `--png` draws the
script's `Visualization` entities (one PNG per entity, suffixed with the entity's name when there are
several) and `--show` opens them in windows. Without a `Visualization` entity, the chart shows one
curve per status.

### KendrickStudio, the graphical front end

    java -jar target/jkendrick2.jar [model.kendrick]

opens KendrickStudio: a script editor on the left, the chart of the script's `Visualization` entity
on the right, a log below. *Open...* loads a script, *Run* (or Ctrl+Enter) runs it in the background
and shows the chart and the totals; errors are logged with their line and column and the caret is
moved to the first one. *Save* and *Save as...* write the script, *Save chart as PNG...* the chart.

### Using JKendrick2 from Java

    import jKendrick.Kendrick;
    import jKendrick.model.GroundModel;
    import jKendrick.simulation.*;

    GroundModel model = Kendrick.load(Paths.get("model.kendrick"));   // parse + build; or Kendrick.load(String)
    List<Trajectory> runs = Kendrick.run(model);                        // as the Simulation entity says
    Trajectory t = runs.get(0);
    Map<String, double[]> byStatus = t.aggregate("status");            // "S", "E", "I", "R" -> values at t.times
    double[] dy = model.derivatives(0, model.initialState());           // the composed ODE system, if needed

    // or choose the solver yourself
    Trajectory ode = new RungeKuttaSolver(4).solve(model, 0, 1500, 1).get(0);         // 4 substeps per step
    List<Trajectory> ssa = new GillespieSolver(20, 7L).solve(model, 0, 1500, 1);       // 20 runs, seed 7
    XYChart chart = ChartRenderer.chart(Kendrick.visualizations(model).get(0), ssa);  // XChart
    ChartRenderer.savePng(chart, Paths.get("ssa.png"));

The library jar is `target/JKendrick2-0.1-SNAPSHOT.jar`; `mvn install` puts it in the local Maven
repository as `fr.lip6:JKendrick2:0.1-SNAPSHOT`.

## The language

### Key concepts

An epidemic ***model*** is a continuous-time Markov chain (CTMC), although it is typically described
as a set of ordinary differential equations (ODEs). A full epidemic model is a composition of
***concerns***: aspects of the model that are themselves CTMCs and may be seen as incomplete models,
such as the disease dynamics (SIR, SEIRS...), a spatial or age structure, the host species or a
control policy. A concern only makes sense if it captures some heterogeneity in the propagation of
the disease; the *core* concern (SIR, SIS, SEIR...) is the one every model needs. Mathematically the
concerns are the automata of a stochastic automata network and the model is their tensor sum: its
compartments are the Cartesian product of the concerns' states, its transitions are those of the
concerns replicated over the other concerns' states, and its rates may depend on the whole state.

A ***scenario*** is a ground model: a model whose parameters and initial cardinalities have all
been given a value, so that it can be run by a ***solver*** in a ***simulation*** for a given
duration and time step. Since the models are CTMCs they can be run with Gillespie's algorithm or the
faster tau-leap; their ODEs (the mean-field limit) can also be solved numerically, with RK4.

### Grammar

The grammar is `src/main/antlr4/jKendrick/dsl/grammar/Kendrick.g4` (ANTLR 4). In one screen:

    script        : entity+ EOF
    entity        : 'KModel' ID clause (';' clause)* '.'
                  | 'Composition' ID ('model:' STRING) (';' 'model:' STRING)* '.'
                  | 'Scenario' ID 'on:' STRING (';' assignment)* '.'
                  | 'Simulation' ID (KEYWORD literal) (';' KEYWORD literal)* '.'
                  | 'Visualization' ID (KEYWORD literal) (';' KEYWORD literal)* '.'
    clause        : 'attribute:' '#(' ID 'in' domain ')'
                  | 'parameters:' '#(' ID+ ')'
                  | 'equations:' '#(' equation ('.' equation)* '.'? ')'
                  | 'transitions:' '#(' transition ('.' transition)* '.'? ')'
                  | 'connectionMatrix:' '#(' ('#(' NUMBER+ ')')+ ')'
                  | 'topology:' '#(' ID ID? ')'                 -- ring | line | complete, oneWay | twoWay
                  | 'transitionRate:' '#(' expr ')'
                  | KEYWORD '#(' expr ')'                       -- default formula of a parameter
    domain        : '(' (ID | NUMBER)+ ')' | '(' 'range' NUMBER NUMBER ')'
    equation      : KEYWORD 't' '=' expr                        -- S:t = ...
    transition    : ID '--' expr '-->' ID
    assignment    : 'var:' '#(' ('(' ID 'in' ID ')')+ ')'
                  | KEYWORD (NUMBER | numberArray | '#(' expr ')')
    expr          : '-' expr | expr ('*'|'/') expr | expr ('+'|'-') expr
                  | 'sum_' '(' ID 'in' ID ')' expr              -- lowest precedence: the body extends to the right
                  | ID '(' expr (',' expr)* ')' | ID | 't' | NUMBER | '(' expr ')'
    KEYWORD       : [a-zA-Z][a-zA-Z0-9_]* ':'
    ID            : [a-zA-Z][a-zA-Z0-9_]*
    STRING        : '\'' ... '\''      COMMENT : '"' ... '"' (skipped)

The functions are `sin`, `cos`, `exp`, `log`, `sqrt`, `abs`, `min`, `max`, `pow`. `Empty` names the
outside of the population in transitions: `status -- mu --> Empty` (deaths from every status),
`Empty -- mu*N --> S` (births). A concern may only name its own parameters and attribute values (plus
`N` and `t`): dependencies between concerns belong to the scenario.

A spatial concern gives the graph of its zones either explicitly, as an adjacency matrix
(`connectionMatrix: #(#(0 1 0) #(0 0 1) #(1 0 0))`, row = origin, column = destination), or as a named
`topology:` over the values of its attribute, in their order: `ring` (each zone is linked to the next
one, the last to the first), `line` (each zone to the next one) or `complete` (every pair of zones).
A topology is two-way unless `oneWay` is given (`topology: #(ring oneWay)`; `twoWay` is accepted
for emphasis). Both are exclusive and both need a `transitionRate:`, the rate at which an individual
follows an arc: the concern's transitions are one per arc, replicated over the other concerns.

### Meaning of names

- A **parameter** name denotes its value in the scenario: a constant (`mu: 0.00137`), a table indexed by
  attributes (`mu_species: #(0.0000365 0.00137)`, one value per species, in the order of the attribute's
  values) or by bound variables (`beta_ki: #(#(0 0.025) #(0 0.5))` after `var: #((k in species) (i in species))`),
  or a formula (`lambda_pk: #(sum_ (i in species) beta_ki*I_ip/N_ip)`). An unindexed reference (`mu`) takes
  the indices from the compartment it is evaluated at; an indexed one (`beta_ki`) supplies them.
  A formula given in a concern (`lambda: #(beta*I/N)`) is a default the scenario may override.
- **`N`** and an **attribute value** (`S`, `I_ip`, `I_humans_free`) denote cardinalities: the number
  of individuals in the compartments they select. Unindexed, the name is localised to the current
  compartment (all other attributes fixed to its values; for `N`, the concern's own attribute is left
  free, so `N` in SEIRS is the population of the current species and zone); indexed, it fixes only the
  attributes its indices mention and sums over the others. An index token is an attribute name (value
  taken from the current compartment), an attribute value (`free`, `1`) or a run of bound single-letter
  variables (`ip`).
- **`sum_ (i in a) e`** sums `e` over the values of attribute `a`; **`t`** is the time.
- An **initial cardinality** is assigned to a compartment name: `S: 49990` spreads 49 990 evenly over
  all the S compartments, `S_birds_1: 49990` gives it to one; the last assignment wins.

### Simulation and Visualization

`Simulation` takes `solver:` (`'rungeKutta'`, `'gillespie'` or `'tauLeap'`), `from:`, `to:`, `step:`
(the times at which the state is recorded), for the stochastic solvers `runs:` and `seed:`, and
`substeps:` (RK4 sub-steps per step, or tau-leap leaps per step). The stochastic solvers need every
concern's dynamics as transitions: a concern given by ODEs provides them when every term of its
equations is a flow proportional to its source compartment (`-lambda*S` in `S` matched by `+lambda*S`
in `E` is `S -- lambda --> E`; an unmatched `-mu*S` is a death, an unmatched `+mu*N` a birth), which
are the hypotheses under which a deterministic model reads as a CTMC; other equations keep their ODE
reading and the stochastic solvers say so.

`Visualization` takes `plot: 'diagram'` (series against time, the only kind for now), `for:` (a
Simulation), `data:` (`#(I_species)`: one series per species, the infectious summed over the other
attributes; `#(I)`: the total; `#(I_species_zone)`: one per species and zone; absent: one series per
status), `legendTitle:`, `legends:` (renames the series, in order), `xLabel:`, and optionally `yLabel:`
and `title:`. With several stochastic runs every run is drawn as a thin curve of its series' colour.

### Errors

Syntax errors are reported together as `file:line:column: message`. Semantic errors name the entity
and end with the position of the faulty assignment, transition, equation or entity:
`Scenario Sc: 'gamma' is neither a parameter nor a compartment of C (line 4, column 22)`.

## How it works

The pipeline is script -> AST -> meta-model objects -> trajectories, one package per stage:

- `jKendrick.dsl.parser.KendrickParserFacade` parses a script with the generated ANTLR parser into
  `jKendrick.dsl.ast.Ast` (`KModel`, `Composition`, `Scenario`, `Simulation`, `Visualization`,
  expressions, `Indexed` names such as `beta_ki` = base `beta`, indices `[ki]`). The AST is purely
  syntactic and every node records its position.
- `jKendrick.dsl.semantics.ModelBuilder` gives the AST its meaning by building the objects of
  `jKendrick.model`, which is how the paper defines the semantics of Kendrick: a `KModel` denotes a
  `Concern` (an `Attribute`, parameters, local transitions between attribute values or one equation per
  value); a `Composition` denotes a `ComposedModel`, the tensor sum of its concerns (`Compartment`s =
  Cartesian product of the attributes' values, in lexicographic order, `transitions()` = every local
  transition replicated over the other concerns' states); a `Scenario` denotes a `GroundModel`, with
  a `ParameterValue` (`Constant`, `Table` or `Formula`) per parameter and an initial state.
  `FlowExtractor` reads equations in flow form as transitions.
- `jKendrick.model.Evaluator` is the interpreter of expressions and fixes the meaning of names as
  described above; `RateCompiler` compiles each rate once into a closure over `(t, state)` that means
  exactly what `Evaluator` computes (tables become constants, cardinalities sums over precomputed
  compartment indices, `sum_` is unrolled, formulas are inlined) and records which compartments it
  reads. `GroundModel` exposes the compiled propensities, the dependency lists and `derivatives(t, y)`,
  the right-hand side of the composed ODE system.
- `jKendrick.simulation` holds the solvers (`RungeKuttaSolver`, `GillespieSolver` with incremental
  propensity updates, `TauLeapSolver`), `Trajectory` (times x compartments, `aggregate(...)`, `toCsv()`),
  `SimulationRunner` (reads the `Simulation` entity), `VisualizationSpec` (reads a `Visualization`
  entity and extracts the series of a trajectory) and `ChartRenderer`, the only class depending on XChart.
- `jKendrick.Kendrick` is the command-line entry point and the API (`load`, `build`, `run`,
  `visualizations`); `jKendrick.ui.KendrickStudio` the Swing front end.

Model 3 of the paper (3 concerns, 120 compartments, 390 transitions, 825 000 individuals) runs 1500
days of Gillespie in about a minute and a half and of RK4 in a few tenths of a second.

## Tests

`mvn test` runs the JUnit 5 tests: the parser on the four models (`KendrickParserFacadeTest`), the
semantic rules and their error messages (`ModelBuilderTest`), the agreement of the compiled rates
with the interpreter and the completeness of the dependency lists (`RateCompilerTest`), the composed
ODE systems against the hand-written MATLAB versions of the paper at 1500 days
(`GroundModelSimulationTest`; the MATLAB scripts are in the paper repository, `matlab models/M1..M4`,
and were run with Octave), the solvers (`SolversTest`: Gillespie runs are integer, conservative and
close to the ODE on average, tau-leap close to Gillespie, flow-form equations run stochastically) and
the series extraction of `Visualization` (`VisualizationSpecTest`).

## Publications

See [Mikal Ziane's web page](https://perso.lip6.fr/Mikal.Ziane/). The Kendrick project (Pharo
implementation, wiki) is at https://github.com/KendrickOrg/kendrick.
