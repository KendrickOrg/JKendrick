# JKendrick2

JKendrick2 is a new, cleaner, implementation of JKendrick. JKendrick is a Java implementation of Kendrick.

https://github.com/KendrickOrg/kendrick/wiki

## Key Concepts

The object-oriented meta-model of Kendrick is still to be improved. The real foundation is its mathematical model.
https://www.plantuml.com/plantuml/uml/XLDDRzim3BthLmWv5cYnNeCT6gm1UjYQ81vwfsCs5QOFJn_5Xgt_VQBbihAThZVnlKVfaHHlGWGVaz72Wi7Gmn4bzah-4TzSXrgwoqkvkbx9PdC_8YEmkzqslqD4Kmi_NLrThEGUF1YCwEMIAy1QXjGQEWRnflxyD-iE476PjedHgpPDgUk1-k9C3puQj77kEK9CGLG2Wfac_1uCIi6drsifMPbUu_PdKi-WqHxnW1gYSbOADbCSZy-yYAg5sKV8FUGcrOOpU9vu0A-WrKZiYB-1kMSQ7-CTsAvHtLIp_TLx326RMqklJa-icfeou7-uPnhjSDXxTynPfGYQxoP5Po0wMtV-kmSRL2cy5h1hAXz59QkQU3X7NH1Lqd9HZzwP3oJHYMolVfEDb9yqXZ0j5xahD8ejHwNWFDM42vH7m6qlE7_IHSQkHv-T-8nBpXgOhtUDg5YUrYjMatLnrGjhwKXZky3Yych-bw3du5wdETGaCqCs-UQJfMNOzQ84I95aV-yfM425BV6PSVyqMmdG9smzo3mBAV-pNlMOOtm0dGQSakgONyqWxUwxF265vRND54Mtpe2o4ueopjrSf8JM_ihLzHXBYsiR_2S6Zkzy4JTeksJqFm00


### Models and Concerns

An epidemic ***model*** is a Continuous Time Markov Chain (CTMC), although it is typically described as aset of Ordinary Differential Equations (ODEs). A full epidimic model is a composition of so-called concerns.

A ***concern*** is an aspect of a model which is also described as a CTMC and which may be seen as an incomplete model. For instance, spatial or age structures, host species and quarantine policies may be defined as concerns. Of course, introducing a concern only makes sense if it captures some heterogenity in the propragation of the considered disease(s), otherwise it only introduces useless complexity.

To be a full epidemic model that can be simulated, a model must include a ***core*** concern such as SIR, SIS, SEIR ...

### Solvers,  Scenarios, and Simualtions 

A ***scenario*** is a ground model, i.e. a model in which all parameters have been assigned a value so that it is ready to be "solved" i.e. in our context, run by a ***solver*** in a ***simulation*** for a given duration with a given time step.

Since our models are stochastic automata they can be run using, say, Gillespie's algorithm or the faster Tau Leap, but their underlying ODEs can also be numerically solved using, for instance, RK4.




