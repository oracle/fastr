# Introduction

This section contains more information regarding the build process. The `mx build` command will build both the Java projects and the native projects.

# Details on Building the Native Code

## Building GNU R

The `com.oracle.truffle.r.native/gnur` directory contains the `Makefile` for building GNU R in such a way that
parts are reusable by FastR. The GNU R source code is download by

It is a multi-step process to build GNU R in such a way that FASTR can use some of the libraries.
After building GNU R we extract configuration information for use in building packages in the FastR environment.
This goes into the file `platform.mk`, which is included in the `Makefile``s for the standard packages built for FastR.
The main change is to define the symbol `FASTR` to ensure that some important modifications to `Rinternals.h` are made
(e.g. changing a `SEXP` to a `void*`).

## Building the Standard GNU R Packages

This directory tree contains the default packages for FastR. Most packages contain native (C/Fortran) code that
must be recompiled for FastR to ensure that the FFI calls are handled correctly. The regenerated `package.so` file overwrites
the file in the `library/package/libs` directory; otherwise the directory contents are identical to GNU R.

As far as possible the native recompilation reference the corresponding source files in the `com.oracle.truffle.r.native/gnur`
directory. In a few case these files have to be modified but every attempt it made to avoid wholesale copy of GNU R source files.

Note that `datasets` doesn`t actually have any native code, but it is convenient to store it here to mirror GNU R.
