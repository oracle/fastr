# FastR

FastR is an implementation of the [R Language](http://www.r-project.org/) in Java atop [Truffle](https://github.com/graalvm/), a framework for building self-optimizing AST interpreters.

FastR is:

* polyglot

R is very powerful and flexible, but certain tasks are best solved by using R in combination with other programming languages.
Interfaces to languages, e.g., Java, Fortran and C/C++, incur a significant overhead, which is caused, to a large degree, by the different execution strategies employed by different languages, e.g., compiled vs. interpreted, and by incompatible internal data representations.

The Truffle framework addresses these issues at a very fundamental level, and builds the necessary polyglot primitives directly into the runtime.
Consequently, FastR leverages this infrastructure to allow multiple languages to interact transparently and seamlessly.
All parts of a polyglot application can be compiled by the same optimizing compiler, and can be executed and debugged simultaneously, with little to no overhead at the language boundary.

* efficient

R is a highly dynamic language that employs a unique combination of data type immutability, lazy evaluation, argument matching, large amount of built-in functionality, and interaction with C and Fortran code.
Consequently, applications that spend a lot of time in R code often have performance problems.
Common solutions are to try to apply primitives to large amounts of data at once and to convert R code to a native language like C.

FastR makes extensive use of the dynamic optimization features provided by the Truffle framework to remove the abstractions that the R language introduces, and can use the Graal compiler to create optimized machine code on the fly.

* compatible

The hardest challenge for implementations of the R language is the tradeoff between compatibility and performance.
If an implementation is very compatible, e.g., by using the traditional internal data layout, it cannot perform optimizations that imply a radically different internal structure.
If an implementation is very efficient, e.g., by adapting internal data structures to the current requirements, it will find it difficult to implement some parts of the GNUR system that are interfacing with applications and packages.

FastR employs many different solution strategies in order to overcome these problems, and also explores possible solutions at a grander scale, like evolution and emulation of R’s native interfaces.

## Getting FastR

FastR is available in two forms:

1. As a pre-built binary. Note that this also includes (Truffle) implementations of JavaScript and optionally Ruby and Python. The pre-built binaries are available for Linux and Mac OS X. The binary release is updated monthly. To install and setup GraalVM and FastR follow the Getting Started instructions in FastR [README](../../README.md#getting_started).
2. As a source release on [GitHub](https://github.com/graalvm/fastr) for developers wishing to contribute to the project and/or study the implementation. The source release is updated regularly and always contains the latest tested version.
    * Note: there is a comunity provided and maintained [Dockerfile](https://github.com/nuest/fastr-docker) for FastR.

## Documentation

Reference manual for FastR, its limitations, compatibility and additional functionality is
available at [GraalVM website](http://www.graalvm.org/docs/reference-manual/languages/r/).

Further documentation is in the [documentation folder](documentation/Index.md) of this repository.
