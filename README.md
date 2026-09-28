# JKendrick2
JKendrick2 is a new, cleaner, implementation of JKendrick. JKendrick is a Java implementation of Kendrick.

https://github.com/KendrickOrg/kendrick/wiki

## Key Concepts

The object-oriented [meta-model of Kendrick] (https://www.plantuml.com/plantuml/uml/XLDDRzim3BthLmWv5cYnNeCT6gm1UjYQ81vwfsCs5QOFJn_5Xgt_VQBbihAThZVnlKVfaHHlGWGVaz72Wi7Gmn4bzah-4TzSXrgwoqkvkbx9PdC_8YEmkzqslqD4Kmi_NLrThEGUF1YCwEMIAy1QXjGQEWRnflxyD-iE476PjedHgpPDgUk1-k9C3puQj77kEK9CGLG2Wfac_1uCIi6drsifMPbUu_PdKi-WqHxnW1gYSbOADbCSZy-yYAg5sKV8FUGcrOOpU9vu0A-WrKZiYB-1kMSQ7-CTsAvHtLIp_TLx326RMqklJa-icfeou7-uPnhjSDXxTynPfGYQxoP5Po0wMtV-kmSRL2cy5h1hAXz59QkQU3X7NH1Lqd9HZzwP3oJHYMolVfEDb9yqXZ0j5xahD8ejHwNWFDM42vH7m6qlE7_IHSQkHv-T-8nBpXgOhtUDg5YUrYjMatLnrGjhwKXZky3Yych-bw3du5wdETGaCqCs-UQJfMNOzQ84I95aV-yfM425BV6PSVyqMmdG9smzo3mBAV-pNlMOOtm0dGQSakgONyqWxUwxF265vRND54Mtpe2o4ueopjrSf8JM_ihLzHXBYsiR_2S6Zkzy4JTeksJqFm00)
is still to be improved.

The real foundation is its mathematical model, see publications, which relies on stochastic automata networks (SANs).

### Models and Concerns

An epidemic ***model*** is a Continuous Time Markov Chain (CTMC), although it is typically described as aset of Ordinary Differential Equations (ODEs). A full epidimic model is a composition of so-called concerns.

A ***concern*** is an aspect of a model which is also described as a CTMC and which may be seen as an incomplete model. For instance, spatial or age structures, host species and quarantine policies may be defined as concerns. Of course, introducing a concern only makes sense if it captures some heterogenity in the propragation of the considered disease(s), otherwise it only introduces useless complexity.

To be a full epidemic model that can be simulated, a model must include a ***core*** concern such as SIR, SIS, SEIR ...

### Solvers, Scenarios, and Simualtions

A ***scenario*** is a ground model, i.e. a model in which all parameters have been assigned a value so that it is ready to be "solved" i.e. in our context, run by a ***solver*** in a ***simulation*** for a given duration with a given time step.

Since our models are stochastic automata they can be run using, say, Gillespie's algorithm or the faster Tau Leap, but their underlying ODEs can also be numerically solved using, for instance, RK4.

## The Kendrick DSL front end (new syntax)

The textual language is parsed by an ANTLR 4 grammar, `src/main/antlr4/jKendrick/dsl/grammar/Kendrick.g4`,
which implements the *new* Kendrick syntax used by the paper
"A Formal Approach to Separation of Concerns in Epidemiology Modeling" (see `kendrick models/SYNTAX.md`
in the paper repository). The lexer, parser and visitor are generated at build time by the
`antlr4-maven-plugin` into `target/generated-sources/antlr4` (package `jKendrick.dsl.grammar`); do not commit them.

- `jKendrick.dsl.parser.KendrickParserFacade` -- text in, `Ast.Script` out; syntax errors are collected and
  thrown together as a `KendrickSyntaxException` with `file:line:column` positions.
- `jKendrick.dsl.ast.Ast` -- the abstract syntax: `KModel`, `Composition`, `Scenario`, `Simulation`,
  `Visualization`, expressions (`Sum`, `Binary`, `Name`...), and `Indexed` names (`beta_ki` = base `beta`,
  indices `[ki]`). The AST is purely syntactic: binding of index variables, attribute lookup, the tensor-sum
  composition and the scenario assignment rules belong to the semantic layer, still to be written.
- `src/test/resources/models/Model1..4.kendrick` -- the four models of the paper, parsed by
  `KendrickParserFacadeTest` (`mvn test`).

Grammar in one screen:

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

## The semantic layer: from the AST to the mathematical meta-model

`jKendrick.dsl.semantics.ModelBuilder` gives a script its meaning by building the objects of the
meta-model (package `jKendrick.model`), which is how the paper defines the semantics of Kendrick:

- a `KModel` denotes a `Concern`: an `Attribute` (an equivalence relation on the population, whose
  values are the states of the concern's automaton), the parameters it introduces, and its dynamics
  as local transitions between attribute values (`transitions:`, or a `connectionMatrix:` with a
  `transitionRate:`) or as one equation per value (`equations:`). A concern may only name its own
  parameters and values (plus `N` and `t`): `ModelBuilder` rejects structural dependencies between concerns.
- a `Composition` denotes a `ComposedModel`, the tensor sum of its concerns: the `Compartment`s are the
  Cartesian product of the attributes' values (lexicographic order, concerns in composition order),
  and `transitions()` replicates each local transition over every combination of the other concerns' states.
- a `Scenario` denotes a `GroundModel`: `var:` binds index variables to attributes; each assignment
  gives a parameter a `ParameterValue` (a `Constant`, a `Table` indexed by attributes or variables such
  as `mu_species` / `beta_ki`, or a `Formula` such as `lambda_pk`) or a compartment its initial
  cardinality (`S_species: #(...)`, `S_birds_1: 49990`; the amount is spread evenly over the compartments
  designated, and the last assignment wins). Parameter formulas given in a concern (`lambda: #(beta*I/N)`)
  are defaults that the scenario may override.
- `Evaluator` fixes the meaning of names in an expression: a parameter is evaluated for the indices the
  reference supplies or for the current compartment; `N` and attribute values (`S`, `I_ip_free`) are
  cardinalities, localised to the current compartment when unindexed (all other attributes fixed, except
  the owner concern's attribute for `N`) and summed over the attributes not mentioned when indexed;
  `sum_ (i in a) e` sums over the values of `a`; `t` is the time (`beta*(1+cos(t))`).
- `RateCompiler` compiles an expression, once per transition, into a closure over `(t, state)` that
  means exactly what `Evaluator` computes (`RateCompilerTest` checks both agree on the four models):
  tables become constants, cardinalities sums over precomputed compartment indices, `sum_` is unrolled and
  formulas are inlined with their variables bound. The solvers use these compiled rates, and the
  compiler records which compartments each rate reads, so that Gillespie recomputes after an event only
  the propensities that can have changed. Model 3 (275 000 individuals, 130 transitions) runs 550 days
  of Gillespie in about 5 s instead of 20 minutes with the interpreter.
- `GroundModel.derivatives(t, y)` is the right-hand side of the composed ODE system, produced
  automatically: for each compartment, the localised equation of every ODE-defined concern, plus the
  flows of every transition of the composed automaton.

`GroundModelSimulationTest` integrates the four models of the paper with RK4 and checks the totals at
550 days against the hand-written MATLAB versions (paper repository, `matlab models/M1..M4`, run with
Octave): the 40- and 80-compartment systems that the tensor sum produces from independent concerns give
the same dynamics as the MATLAB systems written by hand.

## Running a script: solvers over the ground model

`jKendrick.Kendrick` is the entry point of the pipeline (`load` a script, `build` the ground model, `run` its
`Simulation` entity); as a program, `java -cp ... jKendrick.Kendrick model.kendrick [--csv out.csv] [--png out.png] [--show]`
prints the totals per status and species at the end of the run, writes the trajectory as CSV, and draws
the script's `Visualization` entities as PNG files or in windows.

The solvers of package `jKendrick.simulation` work on a `GroundModel` and return `Trajectory` objects
(times x compartments, with `aggregate("status", "species")` to sum over the other attributes):

- `RungeKuttaSolver`: fixed-step RK4 on `GroundModel.derivatives`, i.e. the composed ODE system;
- `GillespieSolver` and `TauLeapSolver`: stochastic simulation of the composed CTMC, whose events are the
  transitions of the composed automaton (`GroundModel.transitions()`) with propensity rate x source cardinality.
  They need every concern's dynamics as transitions. A concern given by ODEs provides them under the
  hypotheses that let a deterministic model be read as a CTMC (Section 3 of the paper): when every term of
  its equations is a flow proportional to its source compartment, `FlowExtractor` turns the equations into
  transitions (`-lambda*S` in S matched by `+lambda*S` in E is `S -- lambda --> E`; an unmatched `-mu*S` is a
  death, an unmatched `+mu*N` a birth from the outside), and the derivatives computed from these transitions
  are those of the equations. Equations that are not in flow form keep their ODE reading only, and the
  stochastic solvers say so. In the transition syntax, `Empty` names the outside (`status -- mu --> Empty`,
  `Empty -- mu*N --> S`).

`SimulationRunner` reads the `Simulation` entity: `solver:` ('rungeKutta', 'gillespie', 'tauLeap'), `from:`,
`to:`, `step:`, and for stochastic runs `runs:` and `seed:`; `substeps:` refines the integration or leap step.

`VisualizationSpec` reads a `Visualization` entity: `plot: 'diagram'` (series against time, the only kind
for now), `data: #(I_species)`, `legendTitle:`, `legends:`, `xLabel:`, and optionally `yLabel:` and
`title:`. A data name is an attribute value followed by attribute names: `I_species` is one series per
species, the infectious summed over the other attributes (zone...); `I` alone is the total; without
`data:` there is one series per status. `legends:` renames the series in order. `ChartRenderer` is the
only class that depends on XChart; with several stochastic runs it draws every run as a thin curve of its
series' colour. `java -cp target/classes:... jKendrick.Kendrick src/test/resources/models/Model3.kendrick --png m3.png`
gives the curves of the MATLAB `visualization.m` of the paper.

Semantic errors (`SemanticException`) name the entity and end with the position of the faulty
assignment, transition, equation or entity in the script (`Scenario Sc: 'gamma' is neither a parameter
nor a compartment of C (line 4, column 22)`); syntax errors are reported as `file:line:column: message`.

## The Swing front end

`jKendrick.ui.KendrickStudio [script.kendrick]` opens a script editor, runs it (Run button or
Ctrl+Enter, in the background) and shows the chart of its `Visualization` entity next to the editor,
with the totals per status and species in the log; errors are logged with their position and the
caret is moved to the first one. The chart can be saved as PNG. This is the front end of the DSL;
the older `jKendrick.IHM.ui` application, `jKendrick.solvers` and `jKendrick.scenarios` are the
hand-written SIR/SEIR/SIS models of the first prototype and do not use the DSL.

## Publications
See [my web page] (https://perso.lip6.fr/Mikal.Ziane/)




