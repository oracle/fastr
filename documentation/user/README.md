# GraalVM Implementation of R

GraalVM implementation of R, also known as **[FastR](https://github.com/oracle/fastr)**, is [compatible with GNU R](#graalvm-r-engine-compatibility),
can run R code at [unparalleled performance](#high-performance),
[integrates with the GraalVM](#graalvm-integration)
ecosystem and provides [additional R level features](#graalvm-r-engine-additional-features).

Note: The R language is currently [experimental](https://docs.oracle.com/en/graalvm/enterprise/20/guide/overview/license/licensing-information.html) and not recommended for production use at this time.

## Installing R

The R language runtime can be installed to a GraalVM build the [GraalVM Updater]({{ "/docs/reference-manual/install-components/" | relative_url }}) tool.
See `$GRAALVM_HOME/bin/gu --help` for more information.

### Requirements
GraalVM R engine requires the [OpenMP runtime library](https://www.openmprtl.org/) and [GFortran 3](https://gcc.gnu.org/wiki/GFortranBinaries) runtime libraries to be installed
on the target system. Following commands should install those dependencies.

* Ubuntu 18.04 and 19.10: `apt-get install libgfortran-8-dev libgomp1`
* Oracle Linux 8: `yum install libgfortran`
* MacOS: `brew install gcc@4.9`

On macOS it is necessary to run `$GRAALVM_HOME/bin/configure_fastr`.
This script will attempt to locate the necessary runtime libraries on your computer
and will fine-tune the the GraalVM R installation according to your system.
On Linux systems, this script will check that the necessary libraries are installed, and if not,
it will suggest how to install them.

Moreover, to install R packages that contain C/C++ or Fortran code, compilers
for those languages must be present on the target system. Following packages
satisfy the dependencies of the most common R packages:

* Ubuntu 18.04:
```
apt-get install build-essential gfortran-8 libxml2 libc++-dev
```
* Ubuntu 19.10:
```
apt-get install build-essential gfortran libxml2 libc++-dev
```
* Oracle Linux 8:
```
yum groupinstall 'Development Tools' && yum install libgfortran gcc-gfortran bzip2 libxml2-devel
```

### Search Paths for Packages
The default R library location is within the GraalVM installation directory.
In order to allow installation of additional packages for users that
do not have write access to the GraalVM installation directory,
edit the `R_LIBS_USER` variable in the `$GRAALVM_HOME/etc/Renviron` file.

## Running R Code

Run R code with the `R` and `Rscript` commands:
```shell
$ R [polyglot options] [R options] [filename]
```
```shell
$ Rscript [polyglot options] [R options] [filename]
```

GraalVM R engine uses the same [polyglot options]({{ "/docs/reference-manual/polyglot/#polyglot-options" | relative_url}}) as other GraalVM languages and the same R options as [GNU R](https://cran.r-project.org/doc/manuals/r-release/R-intro.html#Invoking-R-from-the-command-line), e.g., `bin/R --vanilla`.
Use `--help` to print the list of supported options. The most important options include:
  - `--jvm` to enable Java interoperability
  - `--polyglot` to enable interoperability with other GraalVM languages
  - `--vm.Djava.net.useSystemProxies=true` to pass any options to the JVM, this will be translated to `-Djava.net.useSystemProxies=true`.

Note: unlike other GraalVM languages, R does not yet ship with a
[Native Image]({{"/docs/reference-manual/aot-compilation/" | relative_url }}) of its runtime.
Therefore the `--native` option, which is the default, will still start Rscript on top of JVM,
but for the sake of future compatibility the Java interoperability will not be available in such case.

Users can optionally build the native image using:
```shell
jre/languages/R/bin/install_r_native_image
```

## Running R Extensions

The GraalVM R engine can run [R extensions](https://cran.r-project.org/doc/manuals/r-release/R-exts.html) in two modes:

* **native**: the native machine code is run directly on your CPU, this is the same as how GNU-R runs R extensions.
* **llvm**: if the LLVM bitcode is available, it can be interpreted by [GraalVM LLVM]({{ "/docs/reference-manual/languages/llvm" | relative_url }}).

The *native* mode is better suited for code that does not extensively interact with the R API, for example,
plain C or Fortran numerical computations working on primitive arrays. The *llvm* mode provides significantly
better performance for extensions that frequently call between R and the C/C++ code, because GraalVM LLVM
interpreter is also partially evaluated by the [Truffle library](https://github.com/oracle/graal/tree/master/truffle) like the R code, both can be inlined and optimized
as one compilation unit. Moreover, GraalVM LLVM is supported by
[GraalVM tools]({{ "/docs/reference-manual/tools/" | relative_url }}) which allows to, for instance,
debug R and C code together.

In one GraalVM R process, any R package can be loaded in either mode. That is, GraalVM R supports
mixing packages loaded in the *native* mode with packages loaded in the *llvm* mode in one process.

### Generating LLVM Bitcode

As of version 19.3.0, the GraalVM R engine is configured to use the
[LLVM toolchain]({{ "/docs/reference-manual/languages/llvm/#llvm-toolchain" | relative_url }})
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

### Choosing the Running Mode

Starting from the version 19.3.0, the GraalVM R engine uses the following defaults:
* **native** mode to load the packages
* **llvm** toolchain to build their sources

To enable the *llvm* mode for loading the packages, use `--R.BackEnd=llvm`.
You can also enable each mode selectively for given R packages by using:
* `--R.BackEndLLVM=package1,package2`
* `--R.BackEndNative=package1,package2`

## GraalVM R Engine Compatibility

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

You can use the [compatibility checker]({{"/docs/reference-manual/compatibility/" | relative_url}}) to find whether the CRAN packages you are interested in are tested on GraalVM and whether the tests pass successfully.

## High Performance
GraalVM runtime optimizes R code that runs for extended periods of time.
The speculative optimizations based on the runtime behavior of the R code and dynamic compilation employed by GraalVM runtime are capable of removing most of the abstraction penalty incurred by the dynamism and complexity of the R language.

Let us look at an algorithm in R code. The following example calculates the
mutual information of a large matrix:
```
x <- matrix(runif(1000000), 1000, 1000)
mutual_R <- function(joint_dist) {
 joint_dist <- joint_dist/sum(joint_dist)
 mutual_information <- 0
 num_rows <- nrow(joint_dist)
 num_cols <- ncol(joint_dist)
 colsums <- colSums(joint_dist)
 rowsums <- rowSums(joint_dist)
 for(i in seq_along(1:num_rows)){
  for(j in seq_along(1:num_cols)){
   temp <- log((joint_dist[i,j]/(colsums[j]*rowsums[i])))
   if(!is.finite(temp)){
    temp = 0
   }
   mutual_information <-
    mutual_information + joint_dist[i,j] * temp
  }
 }
 mutual_information
}
system.time(mutual_R(x))
#   user  system elapsed
#  1.321   0.010   1.279
```

Algorithms such as this one usually require C/C++ code to run efficiently:<a href="#note-2"><sup>2</sup></a>
```
if (!require('RcppArmadillo')) {
    install.packages('RcppArmadillo')
    library(RcppArmadillo)
}
library(Rcpp)
sourceCpp("r_mutual.cpp")
x <- matrix(runif(1000000), 1000, 1000)
system.time(mutual_cpp(x))
#   user  system elapsed
#  0.037   0.003   0.040
```
(Uses [r_mutual.cpp]({{"/docs/examples/r_mutual.cpp" | relative_url }}).)
However, after a few iterations, GraalVM runs the R code efficiently enough to
make the performance advantage of C/C++ negligible:
```
system.time(mutual_R(x))
#   user  system elapsed
#  0.063   0.001   0.077
```

GraalVM implementation of R is primarily aimed at long-running applications. Therefore, the peak performance is usually only achieved after a warmup period. While startup time is currently slower than GNUR's, due to the overhead from Java class loading and compilation, future releases will contain a native image of R with improved startup.

## GraalVM Integration

The R language integration with the GraalVM ecosystem includes:
   - seamless interop with other GraalVM languages and with Java
   - debugging with [Chrome DevTools]({{"/docs/reference-manual/tools/#debugger" | relative_url }})
   - [CPU and memory profiling]({{"/docs/reference-manual/tools/#profiler" | relative_url }})
   - [VisualVM integration]({{"/docs/reference-manual/tools/#heap-viewer" | relative_url }})

To start debugging the code start the R script with `--inspect` option
```shell
$ Rscript --inspect myScript.R
```
Note that GNU R compatible debugging using, for example, `debug(myFunction)` is also supported.

### Interoperability

GraalVM supports several other programming languages, including JavaScript, Ruby, Python, and LLVM.
GraalVM implementation of R also provides an API for programming language interoperability that lets you execute code from any other language that GraalVM supports. Note that you must start the R script with `--polyglot` to have access to other GraalVM languages.

GraalVM execution of R provides the following interoperability primitives:
 - `eval.polyglot('languageId', 'code')` evaluates code in some other language, the `languageId` can be, e.g., `js`.
 - `eval.polyglot(path = '/path/to/file.extension')` evaluates code loaded from a file. The language is recognized from the extension.
 - `export('polyglot-value-name', rObject)` exports an R object so that it can be imported by other languages.
 - `import('exported-polyglot-value-name')` imports a polyglot value exported by some other language.
---
layout: docs
title: Reference Manual for R
link_title: R Reference
permalink: /docs/reference-manual/languages/r/
toc_group: reference-manual
---

Please use the `?functionName` syntax to learn more. The following example demonstrates the interoperability features:
```
# get an array from Ruby
x <- eval.polyglot('ruby', '[1,2,3]')
print(x[[1]])
# [1] 1

# get a JavaScript object
x <- eval.polyglot(path='r_example.js')
print(x$a)
# [1] "value"

# use R vector in JavaScript
export('robj', c(1,2,3))
eval.polyglot('js', paste0(
    'rvalue = Polyglot.import("robj"); ',
    'console.log("JavaScript: " + rvalue.length);'))
# JavaScript: 3
# NULL -- the return value of eval.polyglot
```
(Uses [r_example.js]({{"/docs/examples/r_example.js" | relative_url }}).)

R vectors are presented as arrays to other languages. This includes single element vectors, e.g., `42L` or `NA`.
However, single element vectors that do not contain `NA` can be typically used in places where the other
languages expect a scalar value. Array subscript or similar operation can be used in other languages to access
individual elements of an R vector. If the element of the vector is not `NA`, the actual value
is returned as a scalar value. If the element is `NA`, then a special object that looks like `null`
is returned. The following Ruby code demonstrates this.

```ruby
vec = Polyglot.eval("R", "c(NA, 42)")
p vec[0].nil?
# true
p vec[1]
# 42

vec = Polyglot.eval("R", "42")
p vec.to_s
# "[42]"
p vec[0]
# 42
```

<p id='foreign'>The foreign objects passed to R are implicitly treated as specific R types.
The following table gives some examples.</p>

| Example of foreign object (Java) | Viewed 'as if' on the R side |
| -------------------------------- | ---------------------------- |
| int[] {1,2,3}                  | c(1L,2L,3L)                  |
| int[][] { {1, 2, 3}, {1, 2, 3} } | matrix(c(1:3,1:3),nrow=3)    |
| int[][] { {1, 2, 3}, {1, 3} }    | not supported: raises error  |
| Object[] {1, 'a', '1'}         | list(1L, 'a', '1')           |
| 42                             | 42L                        |

In the following code example, we can simply just pass the Ruby array to the R built-in function `sum`,
which will work with the Ruby array as if it was integer vector.

```
sum(eval.polyglot('ruby', '[1,2,3]'))
```

Foreign objects can be also explicitly wrapped into adapters that make them look like the desired R type.
In such a case, no data copying occurs if possible. The code snippet below shows the most common use cases.

```
# gives list instead of an integer vector
as.list(eval.polyglot('ruby', '[1,2,3]'))

# assume the following Java code:
# public class ClassWithArrays {
#   public boolean[] b = {true, false, true};
#   public int[] i = {1, 2, 3};
# }

x <- new('ClassWithArrays'); # see Java interop below
as.list(x)

# gives: list(c(T,F,T), c(1L,2L,3L))
```

For more details, please refer to
[the executable specification](https://github.com/oracle/fastr/blob/master/com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/library/fastr/R/interop-array-conversion-test.R#L158)
of the implicit and explicit foreign objects conversions.

Note that R contexts started from other languages or Java (as opposed to via the `bin/R` script) will default to non-interactive mode, similar to `bin/Rscript`.
This has implications on console output (results are not echoed) and graphics (output defaults to a file instead of a window), and some packages may behave differently in non-interactive mode.  

See the [Polyglot Reference]({{ "/docs/reference-manual/polyglot/" | relative_url }}) and the
[Embedding documentation]({{ "/docs/graalvm-as-a-platform/embed/" | relative_url }})
for more information about interoperability with other programming languages.


### Interoperability with Java
GraalVM R engine provides built-in interoperability with Java. Java class objects can be obtained via `java.type(...)`.
The standard `new` function interprets string arguments as a Java class if such class exists. `new` also accepts Java types returned from `java.type`.
Fields and methods of Java objects can be accessed using the `$` operator.
Additionally, you can use `awt(...)` to open an R drawing device
directly on a Java Graphics surface, for more details see [Java Based Graphics](#java-based-graphics).

The following example creates a new Java `BufferedImage` object, plots random data to it using R's `grid` package,
and shows the image in a window using Java's `AWT` framework. Note that you must start the R script with `--jvm` to have access to Java interoperability.

```
library(grid)
openJavaWindow <- function () {
   # create image and register graphics
   imageClass <- java.type('java.awt.image.BufferedImage')
   image <- new(imageClass, 450, 450, imageClass$TYPE_INT_RGB);
   graphics <- image$getGraphics()
   graphics$setBackground(java.type('java.awt.Color')$white);
   grDevices:::awt(image$getWidth(), image$getHeight(), graphics)

   # draw image
   grid.newpage()
   pushViewport(plotViewport(margins = c(5.1, 4.1, 4.1, 2.1)))
   grid.xaxis(); grid.yaxis()
   grid.points(x = runif(10, 0, 1), y = runif(10, 0, 1),
        size = unit(0.01, "npc"))

   # open frame with image
   imageIcon <- new("javax.swing.ImageIcon", image)
   label <- new("javax.swing.JLabel", imageIcon)
   panel <- new("javax.swing.JPanel")
   panel$add(label)
   frame <- new("javax.swing.JFrame")
   frame$setMinimumSize(new("java.awt.Dimension",
                image$getWidth(), image$getHeight()))
   frame$add(panel)
   frame$setVisible(T)
   while (frame$isVisible()) Sys.sleep(1)
}
openJavaWindow()
```

For more information on FastR interoperability with Java and other languages implemented with Truffle framework,
refer to the [Interoperability tutorial](https://github.com/oracle/fastr/blob/master/documentation/tutorials/interop/javaInteroperability.md).

GraalVM implementation of R provides its own rJava compatible replacement package available at [GitHub](https://github.com/oracle/fastr/tree/master/com.oracle.truffle.r.pkgs/rJava),
which can be installed using:

```shell
$ R -e "install.fastr.packages('rJava')"
```

## GraalVM R Engine Additional Features

##### Java Based Graphics
The GraalVM implementation of R includes its own Java based implementation of the `grid` package and the following graphics devices: `png`, `jpeg`, `bmp`, `svg` and `awt` (`X11` is aliased to `awt`). The `graphics` package and most of its functions are not supported at the moment.

The `awt` device is based on the Java `Graphics2D` object and users can pass it their own `Graphics2D` object instance when opening the device using the `awt` function, as shown in the Java interop example.
When the `Graphics2D` object is not provided to `awt`, it opens a new window similarly to `X11`.

The `svg` device in GraalVM implementation of R generates more lightweight SVG code than the `svg` implementation in GNU R.
Moreover, functions tailored to manipulate the SVG device are provided: `svg.off` and `svg.string`.
The SVG device is demonstrated in the following code sample. Please use the `?functionName` syntax to learn more.

```
library(lattice)
svg()
mtcars$cars <- rownames(mtcars)
print(barchart(cars~mpg, data=mtcars))
svgCode <- svg.off()
cat(svgCode)
```

##### In-Process Parallel Execution

GraalVM R engine adds a new cluster type `SHARED` for the `parallel` package. This cluster starts new jobs as new threads inside the same process. Example:

```
library(parallel)
cl0 <- makeCluster(7, 'SHARED')
clusterApply(cl0, seq_along(cl0), function(i) i)
```

Worker nodes inherit the attached packages from the parent node with copy-on-write semantics, but not the global environment.
This means that you do not need to load again R libraries on the worker nodes but values (including functions) from the global
environment have to be transfered to the worker nodes, e.g., using `clusterExport`.

Note that unlike with the `FORK` or `PSOCK` clusters the child nodes in `SHARED` cluster are running in the same process,
therefore, e.g., locking files with `lockfile` or `flock` will not work. Moreover, the `SHARED` cluster is based on
an assumption that packages' native code does not mutate shared vectors (which is a discouraged practice) and is thread
safe and re-entrant on the C level.

If the code that you want to parallelize does not match these expectations, you can still use the `PSOCK` cluster with the GraalVM R engine.
The `FORK` cluster and functions depending solely on forking (e.g., `mcparallel`) are not supported at the moment.

<br/>
<br/>
<br/>

<sup id="note-1">1</sup> More technically, GraalVM implementation of R uses a fixed MRAN URL from `$R_HOME/etc/DEFAULT_CRAN_MIRROR`, which is a snapshot of the
CRAN repository as it was visible at a given date from the URL string.

<sup id="note-2">2</sup> When this example is run for the first time, it installs the `RcppArmadillo` package,
which may take few minutes. Note that this example can be run in both R executed
with GraalVM and GNU R.
