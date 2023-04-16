---
layout: docs-experimental
toc_group: fastr
link_title: R Reference
permalink: /reference-manual/r/
---
# GraalVM R Runtime

GraalVM provides a GNU-compatible R runtime to run R programs directly or in the REPL mode.
It can run R code at [unparalleled performance](Performance.md), and seamlessly [integrates with the GraalVM ecosystem](#graalvm-integration).
The project name behind GraalVM's R runtime development is [FastR](https://github.com/oracle/fastr).

>Note: The GraalVM R runtime is currently considered experimental, and not available for Windows AMD64 and macOS AArch64 (Apple Silicon).

## Installing R

The R language runtime is not provided by default, and can be added to GraalVM with the [GraalVM Updater](https://github.com/oracle/graal/blob/master/docs/reference-manual/graalvm-updater.md), `gu`, tool:
```shell
gu install r
```

After this step, the `R` and `Rscript` launchers will become available in the `JAVA_HOME/bin` directory.

The R language home directory, which will be further referenced as `$R_HOME`, is located in `languages/R`.

Note: Please have in mind that FastR is not available for ARM64 processors yet.

## Prerequisites

GraalVM's R runtime requires [zlib](https://zlib.net/) and the [OpenMP runtime library](https://www.openmp.org).
Zlib is default part of MacOS and most modern Linux distributions.
The following commands should install the OpenMP runtime library:

* Ubuntu 18.04 and 19.10: `apt-get install libgomp1`
* Oracle Linux 7 and 8: `yum install libgomp`
* macOS: `libgomp` should be already installed

As of version 20.1.0 and later, GraalVM's R runtime on Linux supports and bundles the GFortran version 3 runtime libraries.
On macOS it bundles and supports the GFortran version 8.3.0 runtime libraries.
It is not necessary to install the runtime libraries.
However, note that a runtime library is only compatible with the GFortran compiler of that same library version or later.

On a Linux system, `$R_HOME/bin/configure_fastr` can be used to check that the necessary libraries are installed, and if not, it will suggest how to install them.

Moreover, to install R packages that contain C/C++ or Fortran code, compilers for those languages must be present on the target system.
The following packages satisfy the dependencies of the most common R packages:

* Ubuntu 18.04 and 19.10:
```shell
apt-get install build-essential gfortran libxml2 libc++-dev
```
* Oracle Linux 7 and 8:
```shell
yum groupinstall 'Development Tools' && yum install gcc-gfortran bzip2 libxml2-devel
```
* macOS
```shell
brew install gcc
```

Note: If the `gfortran` executable is not on your system path, you will need to configure
the full path to it in `$R_HOME/etc/Makeconf`, the `FC` variable.


### Search Paths for Packages
The default R library location is within the GraalVM installation directory.
In order to allow installation of additional packages for users who do not have write access to the GraalVM installation directory, edit the `R_LIBS_USER` variable in the `$JAVA_HOME/etc/Renviron` file.

## Running R

Run R code directly or in the REPL mode with the `R` and `Rscript` commands:
```shell
R [polyglot options] [R options] [filename]
```
```shell
Rscript [polyglot options] [R options] [filename]
```

The GraalVM R runtime uses the same [polyglot options](https://github.com/oracle/graal/blob/master/docs/reference-manual/polyglot-programming.md#polyglot-options) as other GraalVM languages runtimes and the same R options as [GNU R](https://cran.r-project.org/doc/manuals/r-release/R-intro.html#Invoking-R-from-the-command-line), e.g., `bin/R --vanilla`.
Use `--help` to print the list of supported options. The most important options include:
  - `--jvm`: to enable Java interoperability
  - `--polyglot`: to enable interoperability with other GraalVM languages
  - `--vm.Djava.net.useSystemProxies=true`: to pass any options to the JVM; this will be translated to `-Djava.net.useSystemProxies=true`.

Note: Unlike other GraalVM languages runtimes, R does not yet ship with a [Native Image](https://github.com/oracle/graal/blob/master/docs/reference-manual/native-image/README.md) version of its runtime.
Therefore the `--native` option, which is the default, will still start `Rscript` on top of the JVM,
but for the sake of future compatibility the Java interoperability will not be available in this case.

You can optionally build the native image:
```shell
gu rebuild-images R
```
The native launcher for R is intended only for curious users and experiments.
There are known issues and limitations. Once the native launcher is built, you can use
the `--jvm` flag to run R again in the JVM mode.

## GraalVM Integration

The R language integration with the GraalVM ecosystem includes:
   - seamless interoperability with other GraalVM languages and with Java
   - debugging with [Chrome DevTools](https://github.com/oracle/graal/blob/master/docs/tools/chrome-debugger.md)
   - [CPU and memory profiling](https://github.com/oracle/graal/blob/master/docs/tools/profiling.md)
   - [VisualVM integration](https://github.com/oracle/graal/blob/master/docs/tools/visualvm.md)

To start debugging R code, start the launcher with the `--inspect` option:
```shell
Rscript --inspect myScript.R
```
Note: The GNU-compatible debugging using, for example, `debug(myFunction)`, is also supported.
