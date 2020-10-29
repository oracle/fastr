# GraalVM R Runtime Compatibility

The GraalVM R runtime is based on GNU R and reuses the base packages.
It is currently based on GNU R 3.6.1, and moves to new major versions of R as they become available and stable.
GraalVM's R runtime developers maintain an extensive set of unit tests for all aspects of the R language and the builtin functionality, and these tests are available as part of the [R source code](https://github.com/oracle/fastr).

GraalVM's R runtime aims to be fully compatible with GNU R, including its native interface as used by [R extensions](https://cran.r-project.org/doc/manuals/r-release/R-exts.html).
It can install and run unmodified, complex R packages like `ggplot2`, `Shiny`, or `Rcpp`.
As some packages rely on unspecified behaviour or implementation details of GNU R, support for packages is a work in progress, and some packages might not install successfully or work as expected.

## Installing Packages

Packages can be installed using the `install.packages` function or the `R CMD INSTALL` shell command.
By default, R uses a fixed snapshot of the CRAN repository<a href="#note-1"><sup>1</sup></a>.
This behavior can be overridden by explicitly setting the `repos` argument of the `install.packages` function.
This functionality does not interfere with the `checkpoint` package. If you are behind a proxy server, make sure to configure the proxy either with environment variables or using the JVM options, e.g., `--vm.Djava.net.useSystemProxies=true`.

The versions of some packages specifically patched for GraalVM's R runtime can be installed using the `install.fastr.packages` function that downloads them from the [GitHub repository](https://github.com/oracle/fastr/tree/master/com.oracle.truffle.r.pkgs).
Currently, those are `rJava` and `data.table`.

## Limitations
There are some limitations of the GraalVM R runtime compared to GNU R:
   - Only small parts of the low-level `graphics` package are functional. However, the `grid` package is supported and R can install and run packages based on it, like `ggplot2`. Support for the `graphics` package in R is planned for future releases.
   - Encoding of character vectors: related builtins (e.g., `Encoding`) are available,
   but do not execute any useful code. Character vectors are represented as Java Strings and therefore encoded in the UTF-16 format. GraalVM's R runtime will add support for encoding in future releases.
   - Some parts of the native API (e.g., `DATAPTR`) expose implementation details that are hard to emulate for alternative implementations of R. These are implemented as needed while testing the GraalVM R runtime with various CRAN packages.

You can use the [Compatibility checker](https://www.graalvm.org/compatibility/) to find whether the CRAN packages you are interested in are tested on GraalVM and whether the tests pass successfully.
<br/>
<br/>
<br/>
<sup id="note-1">1</sup> More technically, GraalVM's R runtime uses a fixed MRAN URL from `$R_HOME/etc/DEFAULT_CRAN_MIRROR`, which is a snapshot of the CRAN repository as it was visible at a given date from the URL string.
