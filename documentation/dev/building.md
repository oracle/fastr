# Building FastR from Source

Building FastR from source is supported on Mac OS X (El Capitan onwards), and various flavors of Linux.
FastR uses a build tool called `mx` (cf `maven`) which can be downloaded from [here](http://github.com/graalvm/mx).
`mx` manages software in _suites_, which are normally one-to-one with a `git` repository.
FastR depends fundamentally on the [Graal](http://github.com/graalvm/graal) suite and its sub-suites Truffle and Sulong.
However, performance also depends on the GraalVM compiler (also located in the Graal suite) as without it,
FastR operates in interpreted mode only. Suites must be arranged as siblings in a parent directory.
We will call the directory where FastR repository is cloned as `FASTR_HOME`.
The clone of the [Graal](http://github.com/graalvm/graal) repository will be in `FASTR_HOME/../graal`,
but it does not need to be cloned manually, `mx` will clone it automatically as it is a dependency of the FastR `mx` suite. 

## Build modes

There are some aspects of the build that can be configured:

* Whether to build and bundle recommended R packages.
* Whether to capture and bundle some dynamic libraries that FastR depends on so that when the FastR distribution
is used on another system it will work regardless of whether those libraries are installed on that system.

By default no recommended packages and no 3rd party dynamic libraries are bundled in FastR.
This can be controlled by the following environment variables:

* `FASTR_RELEASE` set to any value switches FastR to mode, where it builds and bundles recommended packages (may take very long unless they are cached), and captures and bundles some system libraries, which requires setting `PKG_LDFLAGS_OVERRIDE` environment variable (see [below](#non-standard-system-library-locations)). This mode is usedfor official FastR releases
and must be used when you are building a GraalVM distribution (from the `graal/vm` mx suite),
but it can be further configured by using the following environment variables:
* `FASTR_NO_RECOMMENDED` set to any value turns off the recommended packages build and bundling with FastR. Only applicable if `FASTR_RELEASE` is set.
* `FASTR_CAPTURE_DEPENDENCIES` if exported overrides the set of 3rd party libraries that are bundled with FastR, the value is space separated list of "base" names of the libraries, for example, use `gfortran`, for `libgfortran.so.5`. Libraries that can be bundled are: `libz`, `libpcre`, `libgfortran`, `libquadmath`, and `libgcc_s`. Only applicable if `FASTR_RELEASE` is set.

Note for maintainers of packages for package managers like apt or yum: it is recommended to **not bundle** any 3rd party libraries with FastR, but instead use the dependency management mechanisms of your package manager. To achieve this, export `FASTR_BUNDLE_DEPENDENCIES` set to an empty value (and build FastR with `FASTR_RELEASE`).

## Pre-Requisites

### GNU-R

FastR shares some code with GNU-R: the base packages and the Blas and Lapack libraries.
There are several options how to supply those.

#### Building GNU-R from sources 

Without any explicit configuration, FastR downloads GNU-R sources and attempts to built them. If the build fails, 
more details can be found in log files in the `libdownloads/R-{version}` directory, namely in files

* `libdownloads/R-{version}/gnur_configure.log`
* `libdownloads/R-{version}/gnur_make.log`

Please consult GNU-R documentation on the requirements for building GNU-R. Note: FastR builds GNU-R without the X support and with memory profiling. Simple way of satisfying GNU-R requirements is to install GNU-R via your system package manager or even better install only its build dependencies, for example:

```
apt-get build-dep r-base # for Debian based systems
yum-builddep R # for RedHat/CentOS based systems
``` 

Note: you may need to enable/add additional package sources.

If you are interested in the exact configure options we use for GNU-R,
take a look into: `com.oracle.truffle.r.native/gnur/Makefile.gnur`.

#### Using pre-built GNU-R binary

By exporting environment variable `GNUR_HOME_BINARY` with path to a home directory of existing GNU-R installation,
you can bypass the build of GUN-R from sources, however, FastR still downloads GNU-R sources during FastR build.

You can find the home directory of your system GNU-R by executing `R -e 'R.home()'`.

It is strongly recommended to use the same GNU-R version (major and minor) and in case you want to run tests that
compare output of FastR with GNU-R then also the same path version. You can find out version of an R engine by
executing `R --version`. This information is also encoded in `com.oracle.truffle.r.native/Makefile`
in FastR sources (variable `R_VERSION`).

#### Using different Blas/Lapack libraries

By exporting `BLAS_SOURCE` and/or `LAPACK_SOURCE` one can choose Blas/Lapack implementation independently of GNU-R. 
These variables must point to dynamic libraries that implement the Blas/Lapack interface. Another option is to export
`BLAS_LAPACK_DIR` that must point to a directory with subdirectory `lib` that must contain
`libRblas.so` and `libRlapack.so` (`*.dylib` on MacOS).

### FastR specific requirements

Requirements shared between FastR and GNU-R are:

    A Fortran compiler and libraries. Typically gfortran 4.8 or later
    The pcre package, version 8.38 or later
    The zlib package, version 1.2.8 or later
    The ed, sed, and make utilities (usually but not always available on modern *nix systems)

On top of the requirements of GNU-R, FastR also needs:

    A JVMCI-enabled Java JDK
    Python version 3.x

A JVMCI enabled Java builds are available in the [openjdk8-jvmci-builder GitHub repository](https://github.com/graalvm/openjdk8-jvmci-builder/releases).
The environment variable `JAVA_HOME` must be set to the location of the jvmci-enabled Java JDK.
    
Additional requirements in order to install and run some important R packages (required for for both GNU-R and FastR):

    The bzip2 package, version 1.0.6 or later
    The xz package, version 5.2.2 or later
    The curl package, version 7.50.1 or later

FastR uses the Labs LLVM Toolchain and `gfortran` to build its native sources.
The Labs LLVM Toolchain is provided by the Sulong suite in Graal repository and
MX handles that automatically. By default, FastR expects the `gfortran` executable
to be on the system path. Path to gfortran can be configured by exporting environment
variable `FASTR_FC`.

### Non standard system library locations

Since different systems use different package managers some of which install packages in directories that are not
scanned by default by the C compiler and linker, it may be necessary to inform the build of these locations using
the following environment variables:

    PKG_INCLUDE_FLAGS_OVERRIDE # additional include directories not scanned by your C compiler by default
    PKG_LDFLAGS_OVERRIDE # additional library directories not scanned by your linker by default

For example, on Mac OS, the MacPorts installer places headers in `/opt/local/include` and
libraries in `/opt/local/lib`, in which case, the above variables must be set to these
values prior to the build, e.g.:

    export PKG_INCLUDE_FLAGS_OVERRIDE=-I/opt/local/include
    export PKG_LDFLAGS_OVERRIDE=-L/opt/local/lib
    
Those variables are recognized by both FastR and GNU-R. If you wish to build FastR in a 
**mode where it bundles some of the dependencies**, you **have to provide `PKG_LDFLAGS_OVERRIDE`**
even if the those dependencies are on paths scanned by default by the system linker.
This is a limitation of the current Python script that handles the bundling and it
may be improved in the future. The `PKG_LDFLAGS_OVERRIDE` configuration for most Linux
systems where all the dependencies are in standard locations is following:

    export PKG_LDFLAGS_OVERRIDE="\"-L/lib/x86_64-linux-gnu/ -L/usr/lib/x86_64-linux-gnu/\""

Note that if more than one location must be specified, the values must be quoted.

### Caching recommended packages

Export environment variable `FASTR_RECOMMENDED_BINARY`, which must point to a directory with the following structure:

    FASTR_RECOMMENDED_BINARY
      ├── api-checksum.txt
      └── pkgs
          ├── codetools
          ├── MASS
          └── Matrix
          └── ...
        
subdirectory `pkgs` should contain the prebuilt R packages, and `api-checksum.txt` is the checksum of the
FastR API against which the cache was built. One can get the checksum by executing

    mx r-pkgcache --print-api-checksum --vm fastr

## Building FastR
Use the following sequence of commands to download and build FastR.

    $ mkdir $BUILD_DIR
    $ cd $BUILD_DIR
    $ git clone http://github.com/graalvm/mx
    $ PATH=$PATH:$BUILD_DIR/mx
    $ git clone http://github.com/oracle/fastr
    $ cd fastr
    $ mx build

The `mx build` command will clone the Graal repository and also download various required libraries, including GNU-R.
Any problems with the GNU-R configure step likely relate to dependent packages, so review the previous section.

## Running FastR

After building, running the FastR console can be done either with `bin/R` or  with `mx r` or `mx R`.
Using `mx` makes available some additional options that are of interest to FastR developers.
FastR supports the same command line arguments as R, so running an R script is done with `bin/R -f <file>` or `bin/Rscript <file>`.
When run via `mx`, FastR runs in interpreted mode, unless one "imports" the `compiler` suite (from Graal repository) like so

    mx --dynamicimports graal/compiler R

Another option is to run `mx graalvm-home`, which gives the full path to the directory with GraalVM distribution
that was built as part of the FastR build. In order to build this GraalVM distribution also with the GraalVM compiler,
alter the build command to:

    mx --dynamicimports graal/compiler build
    
you can then run the `R` command from within the GraalVM build using, e.g.: 

    $(mx graalvm-home)/bin/R --version
    
the GraalVM compiler is used by default in such case. You can check that by
enabling logging of Truffle compilations and then running some R code.

    $(mx graalvm-home)/bin/R --vm.Dgraal.TraceTruffleCompilation=true
    > foo <- function(i) i + sin(i)
    > r <- 0; for (i in 1:100000) r <- r + foo(i)
    [truffle] opt done         foo <opt> ...more details...

## Running FastR test

* `mx rutgen` runs set of basic fast automated tests
* `mx rtestgen` generates the expected output of the newly added automated tests
* `mx pkgtest --repos FASTR --run-tests testrffi` runs the tests of [R extensions C API](https://cran.r-project.org/doc/manuals/r-release/R-exts.html). 

## Useful MX options

* `mx -v {any command}` shows the full command line how MX invoked the `java` command. This may be useful to, for example, inspect the classpath.
* `mx --J @'-DanyJVMOptions -Xmx6g'` pass additional options to the `java` command.
* `mx -d {any command}` start `java` with debugging. You can then attach Java debugger to port `8000`.

## IDE Usage

`mx` supports IDE integration with Eclipse, Netbeans or IntelliJ and creates project metadata with the `ideinit` command. 
You can limit metadata creation to one IDE by setting the `MX_IDE` environment variable to, say, `eclipse`, 
or by using one of the specialized commands:

```
mx intellijinit
mx netbeansinit
mx eclipseinit
```

## Contributing

We would like to grow the FastR open-source community to provide a free R implementation atop the Truffle/Graal stack.
We encourage contributions, and invite interested developers to join in.
Prospective contributors need to sign the [Oracle Contributor Agreement (OCA)](http://www.oracle.com/technetwork/community/oca-486395.html).
The access point for contributions, issues and questions about FastR is the [GitHub repository](https://github.com/oracle/fastr).

## Troubleshooting

* if building GNU-R from sources (the default), check the following log files:
  * `libdownloads/R-{version}/gnur_configure.log`
  * `libdownloads/R-{version}/gnur_make.log`
* check that you have the right revision of the Graal repository
  * `mx sforceimports` run from `FASTR_HOME` (i.e., `$BUILD_DIR/fastr`) should checkout the desired revision of Graal

### Build fails when generating R grammar

This problem manifests by the following error message in the build output:

`Parser failed to execute command`

followed by a series of parser errors, such as:

`error(170): R.g:<LINE>:<COL>: the .. range operator isn't allowed in parser rules`

It seems to be an ANTLR issue occurring when `LANG`, `LC_ALL` and `LC_CTYPE` environment
variables are not set to the same value.

The solution is to set those variables to the same value, e.g.

```
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
export LC_CTYPE=en_US.UTF-8
```

Note: you may need to install `locale` and run the following before setting the above env variables:

```
locale en_US.UTF-8
```
