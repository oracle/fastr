# GraalVM implementation of R

GraalVM implementation of R, also known as **[FastR](https://github.com/oracle/fastr)**,
is [compatible with GNU R](Compatibility.md),
can run R code at [unparalleled performance](Performance.md) and [integrates with the GraalVM ecosystem](#graalvm-integration).

## Installing R

The R language runtime is not provided by default, can be added to GraalVM using the [GraalVM Updater](https://www.graalvm.org/docs/reference-manual/gu/) tool:
```
gu install r
```
The installation of the R language component is possible only
[from GitHub catalog](https://www.graalvm.org/docs/reference-manual/gu/#component-installation)
for both GraalVM Community and GraalVM Enterprise users. See `$GRAALVM_HOME/bin/gu --help` for more information.

The R language home directory, which will be referenced as `$R_HOME` in this reference manual, is located in:
* `jre/languages/R` in JDK8 based GraalVM distributions
* `languages/R` in JDK11 based GraalVM distributions

### Requirements
GraalVM R engine requires the [OpenMP runtime library](https://www.openmprtl.org/).
Following commands should install this dependency:

* Ubuntu 18.04 and 19.10: `apt-get install libgomp1`
* Oracle Linux 7: `yum install libgomp`
* Oracle Linux 8: `libgomp` should be already installed
* MacOS: `libgomp` should be already installed

As of version 20.1.0 and later, FastR on Linux supports and bundles GFortran version 3
runtime libraries, and on macOS -- GFortran version 8.3.0
runtime libraries. It is not necessary to install the runtime libraries. However,
note that runtime libraries of certain version are only compatible with GFortran
compiler of that version or later.

On Linux system, `$R_HOME/bin/configure_fastr` can be used to check that the
necessary libraries are installed, and if not, it will suggest how to install them.

Moreover, to install R packages that contain C/C++ or Fortran code, compilers
for those languages must be present on the target system. Following packages
satisfy the dependencies of the most common R packages:

* Ubuntu 18.04 and 19.10:
```
apt-get install build-essential gfortran libxml2 libc++-dev
```
* Oracle Linux 7 and 8:
```
yum groupinstall 'Development Tools' && yum install gcc-gfortran bzip2 libxml2-devel
```
* MacOS
```
brew install gcc
```

Note: if the `gfortran` executable is not on your system path, you will need to configure
the full path to it in `$R_HOME/etc/Makeconf`, variable `FC`.

### Search Paths for Packages
The default R library location is within the GraalVM installation directory.
In order to allow installation of additional packages for users that
do not have write access to the GraalVM installation directory,
edit the `R_LIBS_USER` variable in the `$GRAALVM_HOME/etc/Renviron` file.

## Running R Code

Run R code with the `R` and `Rscript` commands:
```
R [polyglot options] [R options] [filename]
```
```
Rscript [polyglot options] [R options] [filename]
```

GraalVM R engine uses the same [polyglot options](https://www.graalvm.org/docs/reference-manual/polyglot-programming/#polyglot-options) as other GraalVM languages and the same R options as [GNU R](https://cran.r-project.org/doc/manuals/r-release/R-intro.html#Invoking-R-from-the-command-line), e.g., `bin/R --vanilla`.
Use `--help` to print the list of supported options. The most important options include:
  - `--jvm` to enable Java interoperability
  - `--polyglot` to enable interoperability with other GraalVM languages
  - `--vm.Djava.net.useSystemProxies=true` to pass any options to the JVM, this will be translated to `-Djava.net.useSystemProxies=true`.

Note that unlike other GraalVM languages, R does not yet ship with a
[Native Image](https://www.graalvm.org/docs/reference-manual/native-image/) of its runtime.
Therefore the `--native` option, which is the default, will still start `Rscript` on top of JVM,
but for the sake of future compatibility the Java interoperability will not be available in such case.

You can optionally build the native image using:
```
gu rebuild-images R
```
The native image of FastR is intended only for curious users and experiments.
There are known issues and limitations. Once the native image was built, you can use
the `--jvm` flag to run FastR again in the JVM mode.

## GraalVM Integration

The R language integration with the GraalVM ecosystem includes:
   - seamless interoperability with other GraalVM languages and with Java
   - debugging with [Chrome DevTools](https://www.graalvm.org/docs/tools/chrome-debugger/)
   - [CPU and memory profiling](https://www.graalvm.org/docs/tools/profiling/)
   - [VisualVM integration](https://www.graalvm.org/docs/tools/visualvm/)

To start debugging the code start the R script with `--inspect` option
```
Rscript --inspect myScript.R
```
Note that GNU R compatible debugging using, for example, `debug(myFunction)` is also supported.
