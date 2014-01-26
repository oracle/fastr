# FastR

FastR is an implementation of the [R Language](http://www.r-project.org/) in Java atop [Truffle and Graal](http://openjdk.java.net/projects/graal/).
Truffle is a framework for building self-optimizing AST interpreters.
Graal is a dynamic compiler that is used to generate efficient machine code from partially evaluated Truffle ASTs.

FastR is an open-source effort of Purdue University, Johannes Kepler University Linz, and Oracle Labs.

## Quick Start

To build and run FastR, you need a [recent JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and a C/C++ compiler tool chain.
You also need to use the `mx` build tool, which is used in all projects built around Graal.
`mx` requires Python 2.7.

1. Clone `mx` (version 2): `hg clone https://hg.java.net/hg/mxtool2~code mxtool`
2. Append the `mxtool` directory to your `PATH`.

FastR requires Graal.
Both projects are hosted in different repositories.
To obtain both, use the split-repository support in `mx`:

1. Create a directory, e.g., `r`, that serves as the project root.
2. In the project root, issue `mx sclone https://bitbucket.org/allr/fastr`. This will clone both the FastR and Graal repositories.

After this, you should find two directories called `fastr` and `graal` in the project root.

`mx` manages projects in "suites".
For each project, there is a primary suite, which in this case is FastR.
To build a project and the projects it depends on, the `mx build` command is used:

1. Move to the primary suite directory: `cd fastr`
2. Build Graal and FastR: `mx build`

When building for the first time, the build script will prompt you for a VM configuration to build.
In general, choosing the `server` configuration is advised.
For details, see `README_GRAAL.txt` in the `graal` subdirectory of the project root.

The first build will also download various required libraries, so there should be a network connection.
If you are behind a firewall, make sure that you have the 'http_proxy' environment variable set appropriately.

After building, running the FastR console is done with `mx r` or 'mx R'.
FastR is supposed to support the same command line arguments as R, so running an R script is done with `mx R -f <file>`.

## IDE Usage

For IDE usage instructions, see the [corresponding page](https://wiki.openjdk.java.net/display/Graal/Eclipse) on the Graal wiki.

## Completeness

FastR is a work in progress: please expect many rough edges and non-working language features.
The tests in the `com.oracle.truffle.r.test` project can give you an idea of what is currently supported.

## Contributing

We would like to grow the FastR open-source community to provide a free R implementation atop the Truffle/Graal stack.
We encourage contributions, and invite interested developers to join in.
Prospective contributors need to sign the [Oracle Contributor Agreement (OCA)](http://www.oracle.com/technetwork/community/oca-486395.html).

## Contact

Please direct questions and comments to the [FastR mailing list](<<TBD>>).

## Authors

* Purdue University: Tomas Kalibera, Petr Maj, Jan Vitek
* Johannes Kepler University Linz: Christian Humer, Andreas Woess
* Oracle Labs: Michael Haupt, Mick Jordan, Adam Welc, Christian Wirth, Thomas Wuerthinger


