# Introduction

The Truffle implementation of the R FFI is based on the Truffle implementation of LLVM intermediate code, named [Sulong](https://github.com/graalvm/sulong).


# Building
Special setup is required to build FastR to use the Truffle R FFI implementation.

## Pre-Requisites

The DragonEgg plugin requires that `gcc 4.6` and `gfortran 4.6` be available. On Mac OS, these can be installed with MacPorts (recommended) or Brew. Having installed these, set the following environment variables:

    export SULONG_GPP=/opt/local/bin/g++-mp-4.6
    export SULONG_GFORTRAN=/opt/local/bin/gfortran-mp-4.6
    export SULONG_GCC=/opt/local/bin/gcc-mp-4.6

The above definitions assume a MacPorts installation.

Both GNU R and FastR native code must be compiled to generate LLVM code. This is handled by special "wrapper" compiler scripts that encapsulate the required steps.
To ensure that the wrapper compiler scripts are used in the GNU R build set:

    export FASTR_RFFI=llvm

If you have an existing build, you must unset any definition of `GNUR_NOCLEAN` then run `mx build -c`. The wrapper scripts add quite a bit of overhead to the build process, particularly the GNU R configure step, but fortunately this only has to be done once.

## Building Sulong
The `sulong` repository must be cloned to a sibling directory of `fastr` and built:

    cd $FASTR_HOME
    git clone https://github.com/graalvm/sulong.git
    cd sulong
    mx build
    mx su-pulldragonegg

The `mx build` step will clone the `graal` repository, if necessary, and build the `truffle` suite also. The `mx su-pulldragonegg` step is required to be able to compile Fortran code to LLVM, which is required by FastR.

## Building FastR

Both GNU R and FastR native code must be compiled to generate LLVM code. This is handled by special "wrapper" compiler scripts that encapsulate the required steps.
To ensure that the `llvm` variant of the native build is generated, set:

    export FASTR_RFFI=llvm

If you have an existing build, you must unset any definition of `GNUR_NOCLEAN` then run `mx build -c`. The wrapper scripts add quite a bit of overhead to the build process, particularly the GNU R configure step, but fortunately this only has to be done once.

## Running

There is no compile-time dependency between FastR and Sulong; all communication is via the Truffle Interop API. Therefore Sulong must be dynamically imported using either `mx --dynamicimport sulong` or by setting the environment variable `DEFAULT_DYNAMIC_IMPORTS=sulong`, with latter being most convenient. With this in effect, a normal `mx R` will make SuLong available.

Note that if the `LLVM_PARSE_TIME` environment variable is set to any value, the time taken to parse each LLVM module is logged to the console, which is also an indication that the LLVM implementation variant is being used.

# Implementation Details

## Compiler Wrapper Scripts

The compiler wrapper scripts are simple shell scripts that first test for the existence of the `sulong` sibling directory and, if it exists and the environment variable `FASTR_SULONG_IGNORE` is not set, invoke associated `mx` commands to perform the compilation. Otherwise, the standard compiler is used. The scripts are stored in the `compilers` sub-directory of `mx.fastr` and are named: `fastr-cc`, `fastr-fc`, `fastr-c++` and `fastr-cpp`. The associated `mx` commands are in `mx.fastr/mx_fastr_compilers.py`.

In order to support both LLVM and non-LLVM execution (no longer actually necessary), each native source file is compiled twice, once to generate native machine code and once to generate LLVM IR. The LLVM IR is actually stored in the object file and extracted at runtime. This avoids having to disrupt the normal R package build process by allowing it to be completely unaware of the existence of LLVM.

Currently, for convenience, the Python wrappers invoke code in the Sulong `sulong/mx.sulong` directory. Eventually, they will modified to be independent of Sulong.

## Limitations
At the time of writing all the `RFFI` interfaces are implemented for LLVM. However, owing to a bug in DragonEgg, the actual Fortran code for the Lapack library is not executed under LLVM, only the wrapper.

Also, not all callbacks are implemented at this time, just those necessary to run a basic system.
