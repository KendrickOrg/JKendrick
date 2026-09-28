/*
 * Kendrick DSL grammar (new syntax, see kendrick models/SYNTAX.md in the paper repository).
 *
 * A script is a sequence of entities, each terminated by a period, in the style of
 * Smalltalk cascades: an entity name followed by keyword clauses separated by semicolons.
 *
 *   KModel SEIRS
 *       attribute: #(status in (S E I R));
 *       parameters: #(beta lambda gamma mu sigma nu);
 *       lambda: #(beta*I/N);
 *       equations: #( S:t=mu*N - lambda*S - mu*S + nu*R. ... ).
 *
 * Underscored identifiers such as beta_ki or I_ip_free are single tokens here; splitting
 * them into a base name and indices is done by the semantic layer (jKendrick.dsl.ast.Indexed).
 */
grammar Kendrick;

// ---------------------------------------------------------------- parser rules

script
    : entity+ EOF
    ;

entity
    : kmodel
    | composition
    | scenario
    | simulation
    | visualization
    ;

// ---- KModel ------------------------------------------------------------------

kmodel
    : 'KModel' name=ID modelClause (';' modelClause)* '.'
    ;

modelClause
    : 'attribute:' '#(' attr=ID 'in' domain ')'                          # attributeClause
    | 'parameters:' '#(' ID+ ')'                                          # parametersClause
    | 'equations:' '#(' equation ('.' equation)* '.'? ')'                 # equationsClause
    | 'transitions:' '#(' transition ('.' transition)* '.'? ')'           # transitionsClause
    | 'connectionMatrix:' '#(' row+ ')'                                   # connectionMatrixClause
    | 'transitionRate:' '#(' expr ')'                                     # transitionRateClause
    | KEYWORD '#(' expr ')'                                               # parameterFormulaClause
    ;

domain
    : '(' value+ ')'                                                      # enumeratedDomain
    | '(' 'range' lo=NUMBER hi=NUMBER ')'                                 # rangeDomain
    ;

value
    : ID
    | NUMBER
    ;

// S:t=mu*N - lambda*S ...   (the compartment name is lexed together with its colon)
equation
    : compartment=KEYWORD 't' '=' expr
    ;

transition
    : from=ID '--' rate=expr '-->' to=ID
    ;

row
    : '#(' NUMBER+ ')'
    ;

// ---- Composition -------------------------------------------------------------

composition
    : 'Composition' name=ID modelRef (';' modelRef)* '.'
    ;

modelRef
    : 'model:' STRING
    ;

// ---- Scenario ----------------------------------------------------------------

scenario
    : 'Scenario' name=ID 'on:' target=STRING (';' assignment)* '.'
    ;

assignment
    : 'var:' '#(' binding+ ')'                                            # varAssignment
    | KEYWORD numberArray                                                 # arrayAssignment
    | KEYWORD NUMBER                                                      # scalarAssignment
    | KEYWORD '#(' expr ')'                                               # formulaAssignment
    ;

binding
    : '(' var=ID 'in' attr=ID ')'
    ;

// #(0 0.012)  or  #(#(0 0.012) #(0 0.24))
numberArray
    : '#(' NUMBER+ ')'
    | '#(' numberArray+ ')'
    ;

// ---- Simulation / Visualization ----------------------------------------------

simulation
    : 'Simulation' name=ID keywordArg (';' keywordArg)* '.'
    ;

visualization
    : 'Visualization' name=ID keywordArg (';' keywordArg)* '.'
    ;

keywordArg
    : KEYWORD literal
    ;

literal
    : NUMBER
    | STRING
    | ID
    | '#(' literal* ')'
    ;

// ---- Expressions -------------------------------------------------------------

// Precedence, highest first: unary minus, * /, + -, and last the sum, whose body
// extends as far right as possible: sum_ (i in species) a * b / c + d  sums a*b/c + d.
expr
    : '-' expr                                                            # negExpr
    | expr op=('*' | '/') expr                                            # mulExpr
    | expr op=('+' | '-') expr                                            # addExpr
    | 'sum_' '(' var=ID 'in' attr=ID ')' expr                             # sumExpr
    | func=ID '(' expr (',' expr)* ')'                                    # callExpr
    | ID                                                                  # nameExpr
    | NUMBER                                                              # numberExpr
    | '(' expr ')'                                                        # parenExpr
    ;

// ---------------------------------------------------------------- lexer rules

KEYWORD : [a-zA-Z][a-zA-Z0-9_]* ':' ;
ID      : [a-zA-Z][a-zA-Z0-9_]* ;
NUMBER  : [0-9]+ ('.' [0-9]+)? ([eE] [+-]? [0-9]+)? ;
STRING  : '\'' (~['] | '\'\'')* '\'' ;

COMMENT : '"' ~["]* '"' -> skip ;
WS      : [ \t\r\n]+ -> skip ;
