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
                  | ID '(' expr (',' expr)* ')' | ID | NUMBER | '(' expr ')'
    KEYWORD       : [a-zA-Z][a-zA-Z0-9_]* ':'
    ID            : [a-zA-Z][a-zA-Z0-9_]*
    STRING        : '\'' ... '\''      COMMENT : '"' ... '"' (skipped)

## Publications
See [my web page] (https://perso.lip6.fr/Mikal.Ziane/)




