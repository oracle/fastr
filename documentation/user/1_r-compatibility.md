# GraalVM R Engine Compatibility

GraalVM implementation of R, known as **FastR**, is based on GNU R and reuses
the base packages. It is currently based on GNU-R 3.6.1, and moves to new major
versions of R as they become available and stable. The [FastR project](https://github.com/oracle/fastr), maintains an extensive set of unit
tests for all aspects of the R language and the builtin functionality, and these
tests are available as part of the R source code. GraalVM R engine aims to be
fully compatible with GNU R, including its native interface as used by [R extensions](https://cran.r-project.org/doc/manuals/r-release/R-exts.html). It
can install and run unmodified complex R packages like `ggplot2`, `Shiny`, or
`Rcpp`. As some packages rely on unspecified behavior or implementation details
of GNU-R, support for packages is work in progress, and some packages might not
install successfully or work as expected.

Packages can be installed using the `install.packages` function or the `R CMD INSTALL` shell command.
By default, R uses fixed snapshot of the CRAN repository<a href="#note-1"><sup>1</sup></a>.
This behavior can be overridden by explicitly setting the `repos` argument of the `install.packages` function.
This functionality does not interfere with the `checkpoint` package. If you are behind a proxy server, make
sure to configure the proxy either with environment variables or using the JVM options,
e.g., `--vm.Djava.net.useSystemProxies=true`.

Versions of some packages specifically patched for GraalVM implementation of R can be installed using the `install.fastr.packages`
function that downloads them from the [GitHub repository](https://github.com/oracle/fastr/tree/master/com.oracle.truffle.r.pkgs).
Currently, those are `rJava` and `data.table`.

Known limitations of GraalVM implementation of R compared to GNU R:
   - Only small parts of the low-level `graphics` package are functional. However, the `grid` package is supported and R can install and run packages based on it like `ggplot2`.
   Support for the `graphics` package in R is planned for future releases.
   - Encoding of character vectors. Related builtins (e.g., `Encoding`) are available,
   but do not execute any useful code. Character vectors are represented as Java Strings and therefore encoded in UTF-16 format. GraalVM implementation of R will add support for encoding in future releases.
   - Some parts of the native API (e.g., `DATAPTR`) expose implementation details that are hard to emulate for alternative implementations of R. These are implemented as needed while testing the GraalVM implementation of R with various CRAN packages.

You can use the [compatibility checker](http://graalvm.org/docs/reference-manual/compatibility) to find whether the CRAN packages you are interested in are tested on GraalVM and whether the tests pass successfully.

<br/>
<br/>
<br/>
<sup id="note-1">1</sup> More technically, GraalVM implementation of R uses a fixed MRAN URL from `$R_HOME/etc/DEFAULT_CRAN_MIRROR`, which is a snapshot of the
CRAN repository as it was visible at a given date from the URL string.
