# R Extensions Support

The GraalVM R engine can run [R extensions](https://cran.r-project.org/doc/manuals/r-release/R-exts.html) in two modes:

* **native**: the native machine code is run directly on your CPU, this is the same as how GNU-R runs R extensions.
* **llvm**: if the LLVM bitcode is available, it can be interpreted by [GraalVM LLVM](https://www.graalvm.org/docs/reference-manual/llvm/).

The *native* mode is better suited for code that does not extensively interact with the R API, for example,
plain C or Fortran numerical computations working on primitive arrays. The *llvm* mode provides significantly
better performance for extensions that frequently call between R and the C/C++ code, because GraalVM LLVM
engine is also partially evaluated by the [Truffle compiler](https://github.com/oracle/graal/tree/master/truffle) like the R code, both can be inlined and optimized
as one compilation unit. Moreover, GraalVM LLVM is supported by
[GraalVM tools](http://graalvm.org/docs/tools/) which allows to, for instance,
debug R and C code together.

In one GraalVM R process, any R package can be loaded in either mode. That is, GraalVM R supports
mixing packages loaded in the *native* mode with packages loaded in the *llvm* mode in one process.

## Generating LLVM Bitcode

As of version 19.3.0, the GraalVM R engine is configured to use the
[LLVM toolchain](https://www.graalvm.org/docs/reference-manual/llvm/)
to compile R packages native code. This toolchain produces standard executable binaries for
a given system, but it also embeds the corresponding LLVM bitcode into them.
The binaries produced by the LLVM Toolchain can be loaded in both modes: *native* or *llvm*.

The GraalVM R engine can be reconfigured to use your system default compilers
when installing R packages by running

```shell
# use local installation of GGC:
$ R -e 'fastr.setToolchain("native")'
# to revert back to using the GraalVM's LLVM toolchain:
$ R -e 'fastr.setToolchain("llvm")'
```

Using the system default compilers may be more reliable, but you loose the
ability to load the R packages built with the LLVM toolchain in the *llvm* mode,
because they will not contain the embedded bitcode. Moreover, mixing packages
built by the local system default compilers and packages built by the LLVM
toolchain in one GraalVM R process may cause linking issues.

## Fortran compiler

As of version 20.1.0, the GraalVM R engine uses `gfortran` as the default Fortran
compiler when installing R packages. Since `gfortran` cannot produce bitcode,
packages that contain Fortran code will not work in the LLVM mode.

The GraalVM R engine distribution contains tool F2C, which can convert Fortran code to C
and then compile it with the Labs LLVM Toolchain. One can configure The GraalVM R engine
to use this tool by editing configuration file `R_HOME/etc/Makeconf`, variable `FC`.

## Choosing the Running Mode

Starting from the version 19.3.0, the GraalVM R engine uses the following defaults:
* **native** mode to load the packages
* **llvm** toolchain to build their sources

To enable the *llvm* mode for loading the packages, use `--R.BackEnd=llvm`.
You can also enable each mode selectively for given R packages by using:
* `--R.BackEndLLVM=package1,package2`
* `--R.BackEndNative=package1,package2`

Moreover, you can configure which packages will be always run in the native mode
in file `R_HOME/etc/native-packages`. FastR comes with a default configuration that
covers some popular R packages that are known to not work yet in the LLVM mode.
