# FastR graphics subsystem
In this document, we list all the differences between graphics implementation in GNU-R and FastR.

Fastr graphics subsystem is compatible with GNU-R.
See [Reference of GNU-R's graphical subsystem](https://cran.r-project.org/doc/manuals/r-release/R-ints.html#Graphics-Devices).
The GNU-R's graphical subsystem (`grid`), and graphical engine are reused as is, just some fastr-specific graphic devices are implemented.

In GraalVM release 22.2.0, we plan to remove `FastRGrid` altogether.

## grid
### grid from gnur (JavaGD)
In the end of the year 2021 (GraalVM release 22.1.0), we decided to throw away our custom `FastRGrid` implementation and rather reuse the `grid` package from gnur as is.
More specifically, we use a fork of `JavaGD` package to provide the glue between R code and Java code.
The upstream version of `JavaGD` registers new "JavaGD" graphical device to the R engine, that has callbacks to Java via JNI.
More specifically, R graphical engine routes graphical device specific operations (for example `grid.rect`) to JavaGD, which routes them to JavaGD upcalls (for example `gdRect`).

The main difference between the upstream version of JavaGD and our version is that our version uses our upcall system (NFI/LLVM) instead of JNI (See `com.oracle.truffle.r.ffi.impl.upcalls.JavaGDUpCalls.java`).
Java sources of the forked `JavaGD` are located in `com.oracle.truffle.r.ffi.impl.javaGD`, and C sources are located in `com.oracle.truffle.r.native/fficall/src/JavaGD`.

SVG device uses Apache Batik library to generate SVG pictures.

### FastRGrid (deprecated)
Internal grid graphics is a FastR internal implementation of `grid` package functionality.
Most of the R functions defined in `grid` package are reused as is, however, most native functions are reimplemented in Java.
The disadvantage is that whenever there is a modification of some native function in `grid` package, we should also update our Java implementation.
The initial motivation for our custom grid implementation was performance, however with the pace of upstream updates, we were not able to maintain this custom implementation anymore.
Currently, this functionality is marked as deprecated in FastR, and is stuck on patched version of the `grid` package, because of the changes to the grid units internal representation in 4.0.3.
You can switch to this (now experimental) functionality with `--R.UseInternalGridGraphics=true` option.

The external functions implemented by fastr grid are routed through `com.oracle.truffle.r.library.fastrGrid.FastRGridExternalLookup.java`.
FastR-specific grid implementation is located in `com.oracle.truffle.r.library.fastrGrid.*` Java packages.

