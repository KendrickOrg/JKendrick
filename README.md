# JKendrick2

JKendrick2 is a new, cleaner, implementation of JKendrick. JKendrick is a Java implementation of Kendrick.

https://github.com/KendrickOrg/kendrick/wiki

## Key Concepts

### Models and Concerns

An epidemic **model** is a Continuous Time Markov Chain (CTMC), although it is typically described as an Ordinary Differential Equations (ODE). A full epidimic model is a composition of so-called concerns.

A **concern** is an aspect of a model which is also described as a CTMC and may be seen as an incomplete model. For instance the spatial structure of a model is a concern. 

To be a full epidemic model that can be simulated, a model must include a **core** concern such as SIR, SIS, SEIR ...

### Scenarios

A **scenario** is a ground model, i.e. a model in which all parameters have been assigned a value.

### Simulations

A **simulation** is a call to a 




