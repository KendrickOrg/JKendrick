# JKendrick2

JKendrick2 is a new, cleaner, implementation of JKendrick. JKendrick is a Java implementation of Kendrick.

https://github.com/KendrickOrg/kendrick/wiki

## Key Concepts

### Models and Concerns

An epidemic ***model*** is a Continuous Time Markov Chain (CTMC), although it is typically described as aset of Ordinary Differential Equations (ODEs). A full epidimic model is a composition of so-called concerns.

A ***concern*** is an aspect of a model which is also described as a CTMC and which may be seen as an incomplete model. For instance, spatial or age structures, host species and quarantine policies may be defined as a concern. Of course, introducing a concern only makes sense if it captures some heterogenity in the propragation of the considered disease(s), otherwise it only introduces useless complexity.

To be a full epidemic model that can be simulated, a model must include a **core** concern such as SIR, SIS, SEIR ...

### Solvers,  Scenarios, and Simualtions 

A ***scenario*** is a ground model, i.e. a model in which all parameters have been assigned a value so that it is ready to be "solved" i.e. in our context, run by a ***solver*** in a ***simulation***, i.e. animated for a given duration with a given time step.

Since our models are stochastic automata they can be run using, say, Gillespie's algorithm or the faster Tau Leap, but their underlying ODEs can also be numerically solved using, for instance, RK4.




