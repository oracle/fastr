# FastR

FastR is an implementation of the [R Language](http://www.r-project.org/) in Java atop [Truffle and Graal](https://github.com/graalvm/).
Truffle is a framework for building self-optimizing AST interpreters.
Graal is a dynamic compiler that is used to generate efficient machine code from partially evaluated Truffle ASTs.

## Getting FastR

FastR is available in two forms:

1. As a [pre-built binary](http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html). N.B. This also includes (Truffle) implementations of Ruby and JavaScript. The pre-built binaries are available for Linux and Mac OS X. There is no Windows version available. The binary release is updated monthly.
2. As a source release on [GitHub](http://github.com/graalvm/fastr) for developers wishing to contribute to the project and/or study the implementation. N.B. This does not contain Ruby or JavaScript. The source release is updated regularly and always contains the latest tested version.

## Status and Limitations

FastR is intended eventually to be a drop-in replacement for GNU R. Currently, however, the implementation is incomplete. Notable limitations are:

1. Graphics support is mostly missing, limited to output to the "pdf" device.
2. Many packages either do not install, particularly those containing native (C/C++) code, or fail tests due to bugs and limitations in FastR. In particular popular packages such as `data.table` and `Rcpp` currently do not work with FastR.

## Running FastR

After downloading and unpacking the binary release, or compiling from source, the `bin` directory contains the `R` and `Rscript` commands and these can be used in a similar way to GNU R.

## Performance

FastR is primarily aimed at long-running applications. The runtime performance behavior is, like Java, based on runtime profiling and runtime compilation of the hot code paths. Therefore, there is an inevitable warm-up time before peak performance is achieved when evaluating a given expression. In addition, startup is slower than GNU R, due to the overhead from Java class loading and compilation.

# Building FastR from Source

Building FastR from source is supported on Mac OS X (El Capitan onwards), and various flavors of Linux.
FastR uses a build tool called `mx` (cf `maven`) which can be downloaded from [here](http://github.com/graalvm/mx).
`mx` manages software in _suites_, which are normally one-to-one with a `git` repository. FastR depends fundamentally on the [truffle](http://github.com/graalvm/truffle) suite. However, performance also depends on the [graal compiler](http://github.com/graalvm/graal-core) as without it, FastR operates in interpreted mode only. The conventional way to arrange the Git repos (suites) is as siblings in a parent directory, which we will call `FASTR_HOME`.

## Pre-Requisites
FastR shares some code with GnuR, for example, the default packages and the Blas library. Therefore, a version of GnuR (currently
R-3.3.2), is downloaded and built as part of the build. Both GNU R and FastR require access certain tools and packages that must be available
prior to the build. These are:

    A jvmci-enabled Java JDK which is available from [pre-built binary](http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html)
    Python version 2.7.x
    A Fortran compiler and libraries. Typically gfortran 4.8 or later
    The pcre package, version 8.38 or later
    The zlib package, version 1.2.8 or later
    The bzip2 package, version 1.0.6 or later
    The xz package, version 5.2.2 or later

If any of these are missing the GNU R build will fail which will cause the FastR build to fail also. Note that your system may have existing installations of these
packages, possibly in standard system locations, but older versions. These must either be upgraded or newer versions installed with the package manager on your system.
Since different systems use different package managers some of which install packages in directories that are not scanned by default by the C compiler and linker,
it may be necessary to inform the build of these locations using the following environment variables:

    PKG_INCLUDE_FLAGS_OVERRIDE
    PKG_LDFLAGS_OVERRIDE

For example, on Mac OS, the MacPorts installer places headers in /opt/local/include and libraries in /opt/local/lib, in which case, the above variables must be set to these
values prior to the build, e.g.:

    export PKG_INCLUDE_FLAGS_OVERRIDE=-I/opt/local/include
    export PKG_LDFLAGS_OVERRIDE=-L/opt/local/lib

 Note that if more than once location must be specified, the values must be quoted, e.g., as in `export PKG_LDFLAGS_OVERRIDE="\"-Lpath1 -Lpath2\""`.

 The environment variable `JAVA_HOME` must be set to the location of the jvmci-enabled Java JDK.

## Building FastR
Use the following sequence of commands to download and build an interpreted version of FastR.

    $ mkdir $FASTR_HOME
    $ cd $FASTR_HOME
    $ git clone http://github.com/graalvm/mx
	$ PATH=$PATH:$FASTR_HOME/mx
	$ git clone http://github.com/graalvm/fastr
	$ cd fastr
	$ mx build

The build will clone the Truffle repository and also download various required libraries.

After building, running the FastR console can be done either with `bin/R` or  with `mx r` or `mx R`. Using `mx` makes available some additional options that are of interest to FastR developers.
FastR supports the same command line arguments as R, so running an R script is done with `bin/R -f <file>` or `bin/Rscript <file>`.

## IDE Usage

`mx` supports IDE integration with Eclipse, Netbeans or IntelliJ and creates project metadata with the `ideinit` command (you can limit metadata creation to one IDE by setting the `MX_IDE` environment variable to, say, `eclipse`). After running this command you can import the `fastr` and `truffle` projects using the `File->Import` menu.

## Further Documentation

Further documentation on FastR, its limitations and additional functionality is [here](Index.md).

## Contributing

We would like to grow the FastR open-source community to provide a free R implementation atop the Truffle/Graal stack.
We encourage contributions, and invite interested developers to join in.
Prospective contributors need to sign the [Oracle Contributor Agreement (OCA)](http://www.oracle.com/technetwork/community/oca-486395.html).
The access point for contributions, issues and questions about FastR is the [GitHub repository ](https://github.com/graalvm/fastr)

