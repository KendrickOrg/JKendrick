# JKendrick

## How to install

### Importing in Eclipse works well using 

Import\Git\Project from git (using smart import)

Obviously dependencies need to be installed and since this is not a Maven or Ant project it must be checked what Eclipse installs.

### Dependencies 

#### Apache Commons RungeKuttaIntegrator

Apache's implementation of RK4 is used: org.apache.commons.math3.ode.nonstiff.RungeKuttaIntegrator

To add Apache commons to an Eclipse project: 
>https://docs.blackberry.com/en/development-tools/blackberry-web-services-for-blackberry-uem/12_10/java-development-guide/bma1439578367525/bma1439578368853

To use RungeKuttaIntegrator and other solvers from Apache Commons: 
>[https://commons.apache.org/proper/commons-math/userguide/ode.html](https://commons.apache.org/proper/commons-math/userguide/ode.html)

>"The user should describe his problem in his own classes which should implement the FirstOrderDifferentialEquations interface.
 Then they should pass it to the integrator they prefer among all the classes that implement the FirstOrderIntegrator interface."
 
So in the very first example SIR_ODE implements FirstOrderDifferentialEquations.

The SIR_ODE.getDimension method return the size of the array holding the cardinality of each compartment.
So for this simple SIR example it is 3 because the compartments are  S, I and R with as many differential equations for dS/dt dI/dt and dR/dt.

#### XChart
>[https://github.com/knowm/XChart](https://github.com/knowm/XChart)

It is not mandatory to create a Maven project to use XChart

### Importing JKendrick in Eclipse
Check that you have correctly configured Git by following this procedure 
> [https://eclipsesource.com/blogs/tutorials/egit-tutorial/](https://eclipsesource.com/blogs/tutorials/egit-tutorial/)

Then, follow this step

> Import\Git\Project from git (using smart import)

## Examples

### SIR

Be careful with the incidence: mass action or  standard.

a) dS/dt = - beta/N IS

in this case beta does not change.

b) dS/dt = - beta IS 

in this case beta must be kept proportional to N.

The example is adapted from https://github.com/KendrickOrg/kendrick/wiki/Basic-SIR-(Benchmark-Model-1)

Une version d??trministe est l???? : https://github.com/KendrickOrg/kendrick/blob/ba11051d2c8976bb27a12da0e3e51650cee99e51/src/Kendrick-Examples/KEDeterministicExamples.class.st#L1183

#### Visualization of a SIR model

We use the equations and parameters from this link [https://homepages.warwick.ac.uk/~masfz/ModelingInfectiousDiseases/Chapter2/Program_2.1/index.html](https://homepages.warwick.ac.uk/~masfz/ModelingInfectiousDiseases/Chapter2/Program_2.1/index.html).

> **SIR Model with Java code**

![SIR Model with Java code](https://github.com/YvanGuifo/PhdThesisYvanGuifo_2019-2022/blob/master/notes-lectures/Images%20de%20document%20de%20note%20de%20lecture/Images/SIRModelJavaCode.png)

> **SIR Model with Pharo code**

![SIR Model with Pharo code](https://github.com/YvanGuifo/PhdThesisYvanGuifo_2019-2022/blob/master/notes-lectures/Images%20de%20document%20de%20note%20de%20lecture/Images/SIRModelPharoCode.png)
