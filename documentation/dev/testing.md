# Introduction

Testing of FastR has two aspects: unit tests and package tests. Unit tests are small snippets of R code intended to test a specific aspects of R, typically one of the builtin functions. These are managed using the Java JUnit framework and all tests must pass before a change is pushed to external repository. Package testing uses the standard GNU R package test machinery and, currently, no packages
are tested before a change is pushed to external repository. Note, however, that the unit tests do test the installation of the "recommended" packages that are distributed with GNU R, plus some internal packages, e.g. `testrffi` for testing the R FFI interface.

The unit testing works by executing the R test and the comparing the output with that from GNU R, which is cached in the file `ExpectedTestOutput.test` in the `com.oracle.truffle.r.test` project. The tests are typically organized as "micro-tests" within a single JUnit test method. Exact matching of the context of error message and warnings is not required (but must be declared in a `TestTrait` argument to the micro-test).

## Unit Tests

The unit tests reside mainly in the `com.oracle.truffle.r.test` project, with a smaller number in the and `com.oracle.truffle.r.nodes.test` project. To execute the unit tests use the `mx junit` command. The standard set of unit tests is available via the `mx junitdefault` command and the following additional variants are available:

1. `mx junitsimple`: everything except the package tests and the `com.oracle.truffle.r.nodes.test`
2. `mx junit --tests list`: `list` is a comma-separated list of test patterns, where a pattern is a package or class. For example to just run the "builtin" tests run `mx junit --tests com.oracle.truffle.r.test.builtins`.

As with most FastR `mx` commands, additional parameters can be passed to the underlying FastR process using the `--J` option. For example to debug a unit test under an IDE, it is important to disable the internal timeout mechanism that detects looping tests, vis:

    mx -d junit --tests sometestclass --J @-DDisableTestTimeout

### Regenerating ExpectedTestOutput.test

After adding, removing or altering units tests (including the `TestTrait` argument), it is necessary to regenerate the GNU R output, vis:

    mx rtestgen

## Package Tests

### Introduction

The R ecosystem is heavily based on packages contributed by the user community, and the standard CRAN repository contains over 6000 such packages. Naturally, many of these serve rather obscure areas but there is a small subset that are extremely popular and widely used, for example the "top 100" most popular packages cited here.

Using a package in R is actually a two step process. First a package must be "installed" and then it can be "loaded" into an R session. The installation step takes the package as a gzipped tar file from the repository, unpacks it and then does some processing before installing the result in a "library" directory. The processing may involve C or Fortran compilation if the package uses the R foreign function interface, as many do.The default installation library location is the system library, which is where the packages included with GnuR are stored, e.g. `base`, `stats`. However, additional directories can be specified through the `R_LIBS_USER` or `R_LIBS` environment variables.

### Installation

A package can be installed in three ways:

    using the command line tool R CMD INSTALL  package.tar.gz
    using utils::install.packages(pkgname) from within an R session
    using the lower level tools:::.install_packages(args) from within an R session

A final step in both these approaches is to test that the package can be loaded (see below). The virtue of the second approach is that it automatically handles the download of the package from the repository. The third approach works when you have access to package tar file, vis:

    tools:::.install_packages(c("-d", "digest_0.6.9.tar.gz"))

The `-d` outputs additional tracing of the package installation process. The argument values are the same as for `R CMD INSTALL`.

### Loading

A package is loaded into an R session using `library(pkgname)` or `require(pkgname)`, which adds it to the search path. `pkgname` can be a quoted string or just the package name, e.g. `library(digest)`. The unquoted form takes advantage of R's lazy evaluation mechanism and the `substitute` builtin.

### Testing

Package developers can provide tests in several ways. To enable the full set of tests a package must be installed with the `--install-tests` option. The `mx pkgtest` command described below always sets this option. Once installed a package can be tested with the `tools::testInstalledPackage` function. The `mx pkgtest` command provides a standard way to do this.

### Package Installation and Testing

Package installation and testing is partly handled by a R script `r/install.packages.R` in the `com.oracle.truffle.r.test.packages` project and partly by an `mx` script. There are two relevant `mx` commands, `installpkgs` and `pkgtest`. The former is simply a wrapper to `install.packages.R`, whereas `pkgtest` contains additional code to gather and compare test outputs.

#### The install.packages.R script

While normally run with FastR using the `mx installpkgs` wrapper, this script can be used standalone using `Rscript`, thereby allowing to to be used by GNU R also.
The command has a rather daunting set of options but, for normal use, most of these do not need to be set.

##### Usage

    mx installpkgs [--repos list] [--cran-mirror url]
                   [--verbose | -v] [-V]
                   [--dryrun]
                   [--no-install | -n]
                   [--create-blacklist]
                   [--blacklist-file file]
                   [--ignore-blacklist]
                   [--initial-blacklist-file file]
                   [--install-dependents-first]
                   [--run-mode mode]
                   [--pkg-filelist file]
                   [--testdir dir]
                   [--pkg-list-installed]
                   [--print-ok-installs]
                   [--list-versions]
                   [--use-installed-pkgs]
                   [--invert-pkgset]
                   [--alpha-daily]
                   [–count-daily count]
                   [--random count]
                   [–pkg-pattern regexp]
                   [--run-tests]
                   [pattern]

A single unkeyworded argument, i.e. `pattern` is interpreted as if it were `-pkg-pattern pattern`.

Key concepts are discussed below.

##### Package Blacklist

There are many packages that cannot be installed due to either missing functionality or fundamental limitations in FastR and this set is seeded from a a DCF file, `initial.package.blacklist`, in the `com.oracle.truffle.r.test.packages` project. `install.packages` operates in two modes, either creating a complete blacklist from an initial blacklist or reading a previously created blacklist file. In the latter case, if the blacklist file does not exist, it will be created. The complete blacklist file can be specified in three ways:

1. using the command line argument `--blacklist-file`; if omitted defaults to the file `package.blacklist`
2. TODO
3. TODO

##### CRAN Mirror
Packages are downloaded and installed from the repos given by the `repos` argument, a comma-separated list, that defaults to `CRAN`. CRAN packages are downloaded from a CRAN mirror. When the standard `utils::install_packages` function is run interactively, the user is prompted for a mirror. To avoid such interaction, `install.packages` has two ways for specifying a mirror. The default CRAN mirror is `http://cran.cnr.berkeley.edu/` but this can be changed either with the command line argument `--cran-mirror` or the environment variable `CRAN_MIRROR`.  The `FASTR` repo is internal to the source base and contains FastR-specific test packages. The BioConductor repo can be added by setting `--repos BIOC`. It also implies `CRAN`.

##### Installation Directory
The directory in which to install the package can be specified either by setting the `R_LIBS_USER` environment variable or with the `--lib` command line argument. The former is recommended and indeed required for running tests after installation (the testing system does not honor the `--lib` argument).

##### Specifying packages to Install
If the `--pkg-filelist` argument is provided then the associated file should contain a list of packages to install, one per line. Otherwise if a package pattern argument is given, then all packages matching the (R) regular expression are candidates for installation, otherwise all available packages are candidates, computed by invoking the `available.packages()` function. The candidate set can be adjusted with additional options.  The `--use-installed.pkgs` option will cause `install.packages` to analyze the package installation directory for existing successfully installed packages and remove those from the candidate set. Some convenience options implicitly set `--pkg-filelist`, namely:

    --ok-only: sets it to the file `com.oracle.truffle.r.test.packages/ok.packages`. This file is a list of packages that are known to install.

N.B. This file is updated only occasionally. Regressions, bug fixes, can render it inaccurate.

Two options are designed to be used for a daily package testing run. These are based on the day of the year and install/test a rolling set of packages:

    --alpha-daily: set package list to those starting with the letter computed as yday %% 26. E.g., yday 0 is ^[Aa], yday 1 is ^[Bb]
    --count-daily count: Install "count" packages starting at an index computed from yday. The set of packages repeats every N days where N is the total number of packages divided by count.

Finally, the `--invert-pkgset` option starts with the set from `available.packages()` and then subtracts the candidate set computed as described above and sets the candidate set to the result.

N.B. By default the candidate set is always reduced by omitting any package in the package blacklist set, but this can be turned off by setting `--ignore-blacklist`. N.B. also that `--pkg-filelist` and `--pkg-pattern` are mutually exclusive.

##### Installing Dependent Packages:
`install.packages` installs the list of requested packages one by one. By default `utils::install.packages` always installs dependent packages, even if the dependent package has already been installed. This can be particularly wasteful if the package fails to install. Setting `--install-dependents-first` causes `install.packages` to analyse the dependents and install them one by one first, aborting the installation of the depending package if any fail.

##### Run Mode
GNU R uses R/Rscript sub-processes in the internals of package installation and testing, but multiple package installations (e.g. using `--pkg-filelist`) would normally be initiated from a single top-level R process. This assumes that the package installation process itself is robust. This mode is defined as the `internal` mode variant of the `--run-mode` option. Since FastR is still under development, in `internal` mode a failure of FastR during a single package installation would abort the entire `install.packages` execution. Therefore by default `install.packages` runs each installation in  a separate FastR sub-process, referred to as `system` mode (because the R `system` function is used to launch the sub-process).

When running `install.packages` under GNU R, it makes sense to set `--run-mode internal`.

##### Use with GNU R

Basic usage is:

    $ Rscript $FASTR_HOME/fastr/com.oracle.truffle.r.test.packages/r/install.packages.R --run-mode internal [options]

where `FASTR_HOME` is the location of the FastR source.

##### Testing
Testing packages requires that they are first installed, so all of the above is relevant. Testing is enabled by the `--run-tests` option and all successfully installed packages are tested.

##### Additional Options

    --verbose | -v: output tracing on basic steps
    -V: more verbose tracing
    --dry-run: output what would be installed but don't actually install
    --no-install | -n: suppress installation phase (useful for --create blacklist and --use-installed-packages/--run-tests)
    --random count: install count packages randomly chosen from the candidate set
    --testdir dir: store test output in dir (defaults to "test").
    --print-ok-installs: print the successfully installed packages
    --list-versions: for the candidate set of packages to install list the name and version in format: name,version,
    --run-tests: run packages tests on the successfully installed packages (not including dependents)

#### Examples

    $ export R_LIBS_USER=`pwd`/lib.install.packages

    $ mx installpkgs --pkg-pattern '^A3$'

Install the `A3` package (and its dependents) in `$R_LIBS_USER`, creating the `package.blacklist` file first if it does not exist. The dependents (`xtable`, `pbapply`) will be installed implicitly by the  underlying R install.packages function

    $ mx installpkgs --install-dependents-first--pkg-pattern '^A3$'

Similar to the above but the dependents of A3 are explicitly installed first. This is equivalent to using `--pkg-filelist` file, where file would contain xtable, pbapply and A3 in that order.

    $ mx installpkgs --create-blacklist --no-install

Just (re)create the package blacklist file.

    $ mx installpkgs --pkg-filelist specific

Install exactly those packages (and their dependents) specified, one per line, in the file `specific`.

    $ mx installpkgs --ok-only --invert-pkgset --random-count 100

Install 100 randomly chosen packages that are not in the file `com.oracle.truffle.r.test.packages/ok.packages`.

    $ mx installpkgs --ignore-blacklist '^Rcpp$'

Override the blacklist and attempt to install the `Rcpp` package. N.B. The regular expression prevents the installation of other packages beginning with `Rcpp`.

#### The mx pkgtest command

The `mx pkgtest` command is a wrapper on `mx installpkgs` that forces the `--run-tests` option and also executes the same tests under GnuR and compares the results. In order to run the tests under GnuR, the `gnur` suite must be installed as a sibling to `fastr`.

#### Running/Debugging Tests Locally

To debug why a test fails requires first that the package is installed locally plus some understanding about how the test process operates. The R code that performs installation and testing makes use of R sub-processes, so simply running the main process under the Java debugger will not work. To demonstrate this we will use the `digest` package as an example.The following command will install the `digest` package in the directory specified by the `R_LIBS_USER` environment variable:

    $ FASTR_LOG_SYSTEM=1 mx installpkgs '^digest$'

First, note that, by default,  the `installpkgs` command itself introduces an extra level on sub-process in order to avoid a failure from aborting the entire install command when installing/testing multiple packages. You can see this by setting the environment variable `FASTR_LOG_SYSTEM` to any value. The first sub-process logged will be running the command `com.oracle.truffle.r.test.packages/r/install.package.R` and the second will be the one running `R CMD INSTALL --install-tests` of the digest package. For ease of debugging you can set the `--run-mode` option to `internal`, which executes the first phase of the install in the process running `installpkgs`. Similar considerations apply to the testing phase. By default a sub-process is used to run the `com.oracle.truffle.r.test.packages/r/test.package.R script`, which then runs the actual test using a sub-process to invoke `R CMD BATCH`. Again the first sub-process can be avoided using `--run-mode internal`. N.B. If you run the tests for `digest` you will see that there are four separate sub-processes used to run different tests. The latter three are the specific tests for digest that were made available by installing with `--install-tests`. Not all packages have such additional tests. Note that there is no way to avoid the tests being run in sub-processes so setting the `-d` option to the `installpkgs` command will have no effect on those. Instead set the environment variable `MX_R_GLOBAL_ARGS=-d` which will cause the sub-processes to run under the debugger. Note that you will not (initially) see the `Listening for transport dt_socket at address: 8000` message on the console, but activating the debug launch from the IDE will connect to the sub-process.


