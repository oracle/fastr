---
layout: docs-experimental
toc_group: fastr
link_title: R Extensions Support
permalink: /reference-manual/r/ExtensionsSupport/
---
# R Extensions Support

The GraalVM R runtime can run [R extensions](https://cran.r-project.org/doc/manuals/r-release/R-exts.html) in two modes:

* **native**: the native machine code is run directly on your CPU, the same way GNU R runs R extensions.
* **llvm**: if the LLVM bitcode is available, it can be interpreted by the [LLVM interpreter shipped with GraalVM](../llvm/README.md).

The *native* mode is better suited for code that does not extensively interact with the R API, for example, plain C or Fortran numerical computations working on primitive arrays.
The *llvm* mode provides significantly better performance for extensions that frequently call between R and the C/C++ code, because GraalVM's LLVM runtime is also partially evaluated like the R code.
Both can be inlined and optimized as one compilation unit.
Moreover, GraalVM's LLVM runtime is supported by [GraalVM tools](../..docs/tools/tools.md) which allows users to, for instance, debug R and C code together.

In one GraalVM R process, any R package can be loaded in either mode.
That is, GraalVM's R runtime supports mixing packages loaded in the *native* mode with packages loaded in the *llvm* mode in one process.

## Generating LLVM Bitcode

As of version 19.3.0, the GraalVM R runtime is configured to use the [LLVM toolchain](https://github.com/oracle/graal/blob/master/sulong/docs/contributor/TOOLCHAIN.md) to compile R packages' native code.
This toolchain produces standard executable binaries for a given system, but it also embeds the corresponding LLVM bitcode into them.
The binaries produced by the LLVM toolchain can be loaded in both modes: *native* or *llvm*.

The GraalVM R runtime can be reconfigured to use your system default compilers when installing R packages by running:
```shell
# use local installation of GGC:
R -e 'fastr.setToolchain("native")'
# to revert back to using the GraalVM's LLVM toolchain:
R -e 'fastr.setToolchain("llvm")'
```

Using the system default compilers may be more reliable, but you lose the ability to load the R packages built with the LLVM toolchain in the *llvm* mode, because they will not contain the embedded bitcode.
Moreover, mixing packages built by the local system default compilers and packages built by the LLVM toolchain in one R process may cause linking issues.

## Fortran Compiler

As of version 20.1.0, the GraalVM R runtime uses `gfortran` as the default Fortran compiler when installing R packages.
Since `gfortran` cannot produce bitcode, packages that contain Fortran code will not work in the *llvm* mode.

The GraalVM R runtime contains the F2C tool, which can convert Fortran code to C and then compile it with the LLVM toolchain.
Users can configure GraalVM's R runtime to use this tool by editing the configuration file `R_HOME/etc/Makeconf`, variable `FC`.

## Choosing the Running Mode

Starting with version 19.3.0, GraalVM's R runtime uses the following defaults:
* **native** mode to load the packages
* **llvm** toolchain to build their sources

To enable the *llvm* mode for loading the packages, use `--R.BackEnd=llvm`.
You can also enable each mode selectively for the given R packages by using:
* `--R.BackEndLLVM=package1,package2`
* `--R.BackEndNative=package1,package2`

Moreover, you can configure which packages will be always run in the native mode in file `R_HOME/etc/native-packages`. GraalVM's R runtime comes with a default configuration that covers some popular R packages that are known to not work yet in the *llvm* mode.
