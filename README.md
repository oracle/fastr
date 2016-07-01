# FastR

FastR is an implementation of the [R Language](http://www.r-project.org/) in Java atop [Truffle and Graal](https://github.com/graalvm/).
Truffle is a framework for building self-optimizing AST interpreters.
Graal is a dynamic compiler that is used to generate efficient machine code from partially evaluated Truffle ASTs.

## Getting FastR

FastR is available in two forms:

1. As a [pre-built binary](http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html). N.B. This also includes (Truffle) implementations of Ruby and JavaScript. The pre-built binaries are available for Linux and Mac OS X. There is no Windows version available. The binary release is updated monthly.
2. As a source release [GitHub](http://github.com/graalvm/fastr) for developers wishing to contribute to the project and/or study the implementation. N.B. This does not contain Ruby or JavaScript. The source release is updated regularly and always contains the latest tested version.

## Status and Limitations

FastR is intended eventually to be a drop-in replacement for GNU R. Currently, however, the implementation is incomplete. Notable limitations are:

1. Graphics support is mostly missing, limited to output to the "pdf" device.
2. Many packages either do not install, particularly those containing native (C/C++) code, or fail tests due to bugs and limitations in FastR. In particular popular packages such as `data.table` and `Rcpp` currently do not work with FastR.

## Running FastR

After downloading and unpacking the binary release, or compiling from source, the `bin` directory contains the `R` and `Rscript` commands and these can be used similar to GNU R. N.B. the binary release currently does not support the `R CMD` and related variants, so packages must be installed from the shell using `install.packages`.

## Performance

FastR is primarily aimed at long-running applications. The runtime performance behavior is, like Java, based on runtime profiling and runtime compilation of the hot code paths. Therefore, there is an inevitable warmup time before peak performance is achieved when evaluating a given expression. In addition, startup is slower than GNU R, due to the overhead from Java class loading and compilation.

# Building FastR from Source

FastR uses a build tool called `mx` (cf `maven`) which can be downloaded from [here](http://github.com/graalvm/mx).
`mx` manages software in _suites_, which are normally one-to-one with a `git` repository. FastR depends fundamentally on the [truffle] (http://github.com/graalvm/truffle) suite. However, performance also depends on the [graal compiler](http://github.com/graalvm/graal-core) as without it, FastR operates in intepreted mode only. The conventional way to arrange the Git repos (suites) is as siblings in a parent directory, which we will call `FASTR_HOME`. Use the following sequence of commands to download and build fastr.

    $ mkdir $FASTR_HOME
    $ git clone http://github.com/graalvm/mx
	$ PATH=$PATH:$FASTR_HOME/mx
    $ cd $FASTR_HOME
	$ git clone http://github.com/graalvm/fastr
	$ cd fastr
	$ mx sforceimports
	$ mx build

FastR shares some code with GnuR, for example, the default packages
and the Blas library. Therefore, a version of GnuR (currently
R-3.2.4), is downloaded and built as part of the first build. 

The first build will also download various required libraries.

After building, running the FastR console can be done either with `bin/R` or  with `mx r` or `mx R`.
FastR supports the same command line arguments as R, so running an R script is done with `mx R -f <file>`.

## IDE Usage

`mx` supports IDE integration with Eclipse, Netbeans or IntelliJ and creates project metadata with the `ideinit` command (you can limit metadata creation to one IDE by setting the `MX_IDE` environment variable to, say, `eclipse`). After running this command you can import the fastr and truffle projects using the `File->Import` menu.

## Contributing

We would like to grow the FastR open-source community to provide a free R implementation atop the Truffle/Graal stack.
We encourage contributions, and invite interested developers to join in.
Prospective contributors need to sign the [Oracle Contributor Agreement (OCA)](http://www.oracle.com/technetwork/community/oca-486395.html).

## Contact

Please direct questions and comments to the [FastR mailing list](http://groups.yahoo.com/group/fastr).
