# 23.0.0
* Removed deprecated FastR grid.
  * Removed `--R.UsetInternalGridGraphics` option.

# 22.3.0
* Implemented global native variable API, which allows the user to use some native package from two R contexts at the same time.
  * The API consists of a bunch of upcalls, named with a prefix of `FASTR_GlobalVar`, e.g., `FASTR_GlobalVarAlloc`.
  * The documentation is in `com.oracle.truffle.r.ffi.impl.upcalls.FastRUpCalls`.
  * Currently, only `grid` and `graphics` builtin packages are refactored to use the global native variable API.
* Add partial support for `dplyr` 1.0.3
  * Implement `SET_PRCODE`, `SET_PRENV` and `SET_PRVALUE` upcalls.

# 22.2.0
* `--R.DebugLLVMLibs` is no longer a stable option.
* Implemented `SET_GROWABLE_BIT` and `IS_GROWABLE` C API functions.
  * This fixes installation of the `cpp11` 0.2.6 package.
* Add `akima` package to the list of "native packages", so it is by default loaded by the native backend.

# 22.1.0
* Improved performance of the `order` and `rank` builtin functions
* FastR does not ship with its own copy of libz.so
  * System installation of this library becomes a requirement for FastR, but zlib is installed by default on MacOS and in most modern Linux distributions.
* Use JavaGD as the default graphical subsystem.
  * Deprecate `--R.UseInternalGridGraphics` option.
  * The FastR's graphical subsystem is now mostly compatible with GNU-R's, i.e., most functions from `graphics`, `grid`, and `grDevices` base packages are now supported.
  * Display lists are fully implemented.
  * Supported devices: SVG, PNG, JPEG, BMP, AWT.
  * See [graphics docs](./documentation/dev/graphics.md).
* Updated XZ library for compression to the version XZ-1.9.

# 22.0.0
* Adopted [NodeLibrary](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/interop/NodeLibrary.html).
* In unit tests, use single shared context rather than multiple inner contexts.
* Update recommended packages:
  * rpart to version 4.1-16 (2019-05-21)
  * cluster to version 2.1.2 (2021-04-17)

# 21.3.0
* Upgrade of [PCRE](https://www.pcre.org/) to PCRE2 version 10.37.
  * See [GNU-R changelog](https://cran.r-project.org/doc/manuals/r-devel/NEWS.html) (section MIGRATION TO PCRE2) for more details
     on potentially user visible differences between PCRE and PCRE2

Bug fixes:

* Fixed implicit make rule parameters used when building [R extensions](https://cran.r-project.org/doc/manuals/r-release/R-exts.html)
  * Fixes, e.g., installation of [maps](https://cran.r-project.org/web/packages/maps/index.html) package version 3.3.0.
  * Removed `-shared` flag from `LDFLAGS`, added `-shared` to `DYLIB_LDFLAGS`, added `SHLIB_LDGLAGS_R`
* Fixed unexpected garbage collection of `CHARSXP` objects in [R extensions](https://cran.r-project.org/doc/manuals/r-release/R-exts.html)
  * FastR did not materialize its internal representation of character vectors to GNU-R compatible CHARSXP objects,
      which caused unexpected collection of CHARSXP objects returned by STRING_ELT
* Option --no-init-file is not ignored anymore (do not read the user's profile at startup)
* Fixed functions `approx` and `approxfun` from the `stats` package
  * Previously they were always failing with error message "Incorrect number of arguments"

# 21.2.0
* Support for packages in 2021-02-01 CRAN snapshot:
  * testthat 3.0.1 is partially supported.
    * FastR does not support parallel tests run, i.e. run testthat only with `Sys.setenv(TESTTHAT_PARALLEL="false")`.
  * tibble 3.0.6 , vctrs 0.3.6, and data.table 1.13.6 are mostly supported.
  * Support for dplyr 1.0.3, ggplot 3.3.3, and knitr 1.31 is a work in progress.
  
Bug fixes:

* `read.dcf` does not ignore whitespaces in fields any more.
* `list.files` gives correct result in a subdirectory with the same prefix as its parent directory.
* Whitespaces in quantifiers in regular expressions are ignored.
  * GNU-R does not comply with PCRE with this behavior.
* `sys.frame` displays frames for `NextMethod` correctly.
* `parent.frame` is able to get the frame that is no longer on the stack.
  * Which is not recommended due to the documentation of `parent.frame`, but some packages do that nonetheless.

# 21.1.0

* Upgraded FastR to R 4.0.3
  * Made FastR runtime mostly compatible with R 4.0.3
  * Migrated to new versions of the base and recommended packages
  * Implemented some of the new features of R 4.0.3
    * All matrices have implicitly also the `array` class
    * Implemented `str2expression`, `str2lang` builtins
    * Pairlists cannot be coerced to language and vice versa
    * Deprecated `--slave` command line option, replaced by `--no-echo`
    * Added `recycle0` parameter to `paste` and `paste0`
    * Add expand parameter to `unlink`
    * `...elt` does no longer accept character vector as parameter
  * See [GNU-R 4.0.3 changelog](https://cran.r-project.org/doc/manuals/r-release/NEWS.pdf) for other changes in the base packages

* Upgraded the CRAN snapshot, used by default by `install.packages`, to 2021-02-01
  * Support of the new versions of most popular packages on FastR is work in progress
  * Packages with known issues: dplyr 1.0.3, ggplot 3.3.3, knitr 1.31

Bug fixes:

* `switch` builtin handles arguments properly (#171)
* `Rf_installTrChar` was giving wrong result
* S3 dispatch in `cbind` and `rbind`
* `.cache_class` with `NULL` parameter
* copy on write of S4 objects when passed into `class<-` function

Added missing R builtins and C APIs:

* `Rf_allocSExp` supports `CLOSXP`
* `R_Serialize` and `R_removeVarFromFrame`


# 21.0.0

Bug fixes:

* Fix `AssertionError` in `sprintf` (#169)

# 20.3.0

Bug fixes:

* Set internal generic dispatch for the lengths builtin, #164
* iconv handles NA values properly

New features:

* preliminary implementation of the ALTREP framework
  * support for registering custom ALTREP classes
  * ALTREP specific C API, e.g., `INTEGER_IS_SORTED`
  * serialization and deserialization of custom ALTREP objects is not supported yet

Added missing R builtins and C APIs:

* non-API C function match5, which is used by some packages
* non-API C function `match5`, which is used by some packages (#149)
* define dummy `XLENGTH` macro if `USE_RINTERNALS` is defined
  * non-existence of this macro is used by some packages to detect old R versions
* `IS_LONG_VEC` C API function
* when loading native symbol dynamically, FastR also checks the name with trailing underscode to be compatible with GNU-R

# 20.2.0

Bug fixes:

* FastR cleans-up the temporary directory created for internal implementation of the R input handlers
* Improve performance of `unique` #154

Added missing R builtins and C APIs

* Dummy implementations of `X_IS_SORTED` and `X_NO_NA` for `X = STRING,INTEGER,REAL` #156

# 20.1.0

New features:

* Preview of improved graphical support.
  * Use `--R.UseInternalGridGraphics=false` to activate this feature.
  * `graphics` and `grDevices` packages are implemented by incorporating the `JavaGD` package.
  * The standard `grid` package works out of the box as well as `lattice` and `ggplot2`.
  * Supported image formats: PDF, PNG, JPEG, BMP.
  * SVG support will be added in the future.
  * Display lists are fully implemented, which, e.g., makes Shiny work better in FastR.
  * Custom FastR `grid` package implementation, which is currently the default, will be deprecated and removed in future releases.
* FastR ships with GCC runtime libraries on all supported systems (Linux and MacOS).
  * GFortran is not a requirement anymore to run FastR, but it is still required to install R packages with Fortran code
  * The GFortran runtime libraries versions are 4.8.5 on Linux and 8.3.0 on MacOS. When compiling additional R packages with Fortran code, one must use GFortran of the same or higher version.
* GFortran is used as the default Fortran compiler even in the "LLVM" toolchain configuration.
  * R packages that contain Fortran code will not be runnable on
  the FastR's LLVM backend (`--R.BackEnd=llvm`). However, the default backend is
  the "native" (`--R.BackEnd=native`). One can also choose to run only specific packages
  via the LLVM backend (`--R.BackEndLLVM=mypackage`).
  * Users can switch back to F2C, which can produce libraries runnable on the LLVM backend,
  by editing variable `FC` in `{FASTR_HOME}/etc/Makeconf`.
* print stacktraces (in `traceback()`) for errors coming from C/C++ code when FastR is running in the LLVM mode (`--R.BackEnd=llvm`) and errors coming from other languages, e.g., GraalPython.
* `@` can be also used to access members of polyglot values originating from other languages. This was previously possible only with `$`, now both operators work.

Bug fixes:

* S3 dispatch with missing arguments, e.g., `as_tibble()` (reported by Michael Hall on Slack)
* `dyn.load` did not work with relative paths
* missing warnings on integer overflow #136
* the f2c script fixed to handle extra dotted file extensions #143
* `iconv` honors the `from` and `asRaw` parameters. Fixes, e.g.: `iconv('foo²²', 'UTF8', 'latin1')`

# 20.0.0

New features:

* clean-up of the `Makeconf` files shipped with FastR in the `etc` directory:
  * `Makeconf.llvm` makes use of the newly available tools in the Labs LLVM Toolchain (`ar` and `ranlib`)
  * `Makeconf.native` contains only generic configuration for Linux/MacOS (with GCC toolchain installed)
  * both files recognize following environment variables:
    * `PKG_LDFLAGS_OVERRIDE` intended for adding library directories for the linker, but may contain any linker flags, e.g., `-L/path/to/library/which/is/not/in/my/os/default/search/path`
    * `PKG_INCLUDE_FLAGS_OVERRIDE` intended for addding include directories, but may contain any compiler flags
    * Note: `Makeconf.llvm` is also used as the default `Makeconf` file, you can change that by using `fastr.setToolchain('native')` (not a new feature in this release)
    * Note: instead of exporting `PKG_LDFLAGS_OVERRIDE` or `PKG_INCLUDE_FLAGS_OVERRIDE`, you can also edit the Makeconf file
* implement the "parse with named arguments" Truffle API available to Truffle instruments
* use the `RootBodyTag` to tag function bodies for Truffle instrumentation

Added missing R builtins and C APIs

* subsetting an array by numeric/string matrix
* ported all external C functions used by `nlm` (#100) and `fisher.test` from the `stats` base package
* Rf_asS4 C API function #118
* `serializeInfoFromConn` and `loadInfoFromConn2` internal builtins

Bug fixes:

* fixed: `polyglot.value` is not an object #123
  * `polyglot.value` is now treated as explicit and not implicit class
  * one of the implications is that internal generic dispatch works with `polyglot.value` #122
* can't assign class to polyglot value #124
* incorrect formatting for sprintf %g and %G: trailing zeroes #126
* support for remote workers with fastRCluster package #113
* bug in variable lookup when a package removes one of its variables using `rm` during `.onLoad`
* Rf_eval: when the argument is a pair-list do not execute it like a language object
* fixed cross-product (`%*%`) for scalar complex values
* fixed `dirname('.')` and `dirname('/')` to give `'.'` and `'/'`
* `list2env` handles duplicated elements correctly
* properly propagate exit code from the launchers (`R` and `Rscript`)


# 19.3.0

New features:

* FastR is based on R 3.6.1
* GCC runtime libraries are not shipped with FastR, use the following commands to install the necessary dependencies:
    * Ubuntu 18.04 and 19.04: `apt-get install libgfortran3 libgomp1`
    * Oracle Linux 7: `yum install libgfortran libgomp`
    * Oracle Linux 8: `yum install compat-libgfortran-48`
    * MacOS: `brew install gcc@4.9`
* new Graal LLVM based back-end for running packages native code.
  * The default {FASTR_HOME}/etc/Makeconf is configured to use the Graal LLVM toolchain to build the native code of R packages.
    * The toolchain builds standard native binaries for a given plarform and also embeds the corresponding LLVM bitcode in them.
    * R builtin `fastr.setToolchain(name)` (`name` can be `llvm` or `native`) sets the compiler toolchain used for package building (modifies etc/Makeconf).
    * To switch back to the previous toolchain configuration that used `GCC`, execute `fastr.setToolchain("native")`.
  * Option `--R.BackEnd` specifies the default backend used to execute packages native code.
    * Different R packages can be run via different backends in one FastR context.
    * `--R.BackEnd=native`, the default, is JNI based backend that runs directly the actual native code.
    * `--R.BackEnd=llvm` is the new LLVM backend that loads the LLVM bitcode embedded in package libraries and runs it via Graal LLVM.
    * `--R.BackEndNative=pkg1,pkg2,...` enumerates packages whose native code will be executed by the native backend.
    * `--R.BackEndLLVM=pkg1,pkg2,...` enumerates packages whose native code will be executed by the LLVM backend.
    * Note: All `--R.BackEnd*` options are passed to R subprocesses.
  * Debugging of packages native code with LLVM backend.
    * Option `--R.DebugLLVMLibs` activates debugging of native code using the bundled LLVM bitcode.
    * Builtin `fastr.useDebugMakevars(use)` activates/deactivates a special `etc/Makevars.site` tailored for building the packages native code for debugging.
* `gc` attempts to invoke Java GC when FastR is run with `--R.EnableExplicitGC=true`
  * this is intended only for testing purposes and it is not recommended to run GC explicitly in FastR.
  * `gc` is not invoking Java GC by default because GNU-R GC and Java GC are fundamentally
    different and this may lead to unintended behavior.

Added missing R builtins and C APIs

* `gctorture` and `gctorture2` built-ins for testing purposes
* `grepRaw` (only for `fixed=T`)

Bug fixes:

* multiplication of a vector and matrix that has one of the dimensions equal to `0` #104
* `tibble` does not print properly when using `knitr` #68
* `type.convert` uses locale #88
* promise with the empty value is handled correctly in 'missing' #87
* `scan` handles non-default value of `nmax` argument
* `Rf_allocVector` fails gracefully when FastR runs out of memory
* bug in `DATAPTR` for vectors of size in bytes larger than 2^31
* provide correct daylight saving time info in `as.POSIXlt` #98
* `list.files` handles `no..` and `all` arguments correctly
* `R --version` does not enter the interactive mode #89
* `update.formula` with RHS that contains `NULL` #92
* failures when working with large vectors (>1GB)


# 19.2.0

New features:

* `fastRCluster` package that allows to run FastR inside GNU-R
  * install in GNU-R or RStudio with: `devtools::install_github('oracle/fastr/com.oracle.truffle.r.pkgs/fastRCluster')`
  * use `?fastRCluster` to learn more

Bug fixes:

* `SET_ATTRIB` does not validate the attributes (as expected by some packages)

# 19.1.0

New features:

* `is.function` returns `true` for foreign executables
* better error message when FastR cannot find its home directory

Bug fixes:

* strings '-Inf', '+NaN', and '-NaN' are correctly parsed to doubles including ignoring leading and trailing whitespace
* avoid unhandled exceptions in `dev.set` #76
* `for` loop creates and initializes the control variable even if there are no iterations #77
* update the output of the `capabilities` builtin #78

# 19.0.0

New features:

* the default `Renviron` file sets `R_LIBS_USER` to a directory inside the current user's home
  * running the `configure_fastr` will change this to a path that also contains GraalVM version and will create the directory

Added missing C APIs:

* `S_realloc`

Bug fixes:

* fatal error on Linux when pressing CTRL+C during long computation

# 1.0 RC 16

Bug fixes:

* `match` worked incorrectly with descending sequences (e.g., `3:1`)
* `match` did not handle descending sequences properly (e.g. `match(1:3, 3:1)`)
* lexer allows any "letter" in identifiers including, e.g., Japanese, which matches GNU-R behavior

# 1.0 RC 15

* `ActiveBinding` objects do not support the `UNBOX` message anymore
* new Truffle interop converts `double` values to `int` values if they fit in the integer range
  * see the changes in the [spec tests](https://github.com/oracle/fastr/commit/e08e2b19571479dddb6167d9a1d492a14cb4c7b2#diff-c842fa11097793b19bd410589c36af99)

Added missing R builtins and C APIs

* simple support for the weak reference API functions (`R_MakeWeakRef`, `R_MakeWeakRefC`, `R_WeakRefKey`, `R_WeakRefValue`)
* `Rf_i1mach`
* `gzcon` builtin for `url` connections, e.g., `load(url('protocol://path'))` should work now
* `Sys.localeconv` supports: `decimal_point`, `thousands_sep`, `grouping`, `int_curr_symbol`, `currency_symbol`, `mon_decimal_point`,
  `mon_thousands_sep`, `mon_grouping`, `int_frac_digits`, `p_cs_precedes`, other fields are fixed to some meaningful but not culture
  sensitive value for given field

Bug fixes:

* `rep.int` with value argument of length 0 just returns the value argument
* `tcrossprod` called from `apply` did not give correct result #60
* `Rf_lengthgets` can accept `NULL` argument
* FastR does not allow Java interop when not started with `--jvm`

# 1.0 RC 14

* all FastR specific options (NOT those GNU-R compatible like `--save`) are experimental except for `--R.PrintErrorStacktracesToFile`,
which is an option that enables logging of FastR internal errors for bug reporting purposes. Experimental options can be unlocked
using `--experimental-options` or with `ContextBuilder#allowExperimentalOptions`.
* the MRAN mirror used by FastR by default instead of CRAN was bumped to 2019-02-13
* options for the JVM or native image are now passed using `--vm.` prefix in both cases instead of `--jvm.` or `--native.`
(e.g., `--jvm.Dproperty=false` becomes `--vm.Dproperty=false`)

New features:

* whenever possible, errors are propagated to the FastR embedder

Added missing R builtins and C API

* `Rf_StringBlank`

Bug fixes:

* `C_numeric_deriv` gives wrong results of gradient #54
* `tcrossprod` with a single vector #56
* `length<-` would remove attributes from the target even if it was a shared value
* `length(x) <- N` should not strip attributes if `length(x) == N`, which is not in line with GNU-R documentation,
but relied upon in the `methods` package #55
* `as.Date` with invalid date string #56

# 1.0 RC 13

New features:

* new JUL-like logging infrastructure backed by Truffle
* FastR options backed by Truffle now. New command-line format - i.e. bin/r --R.PerformanceWarning="true". Also configurable via org.graal.polyglot.Context.Builder.
* script configure_fastr also regenerates etc/Renviron and etc/ldpaths
* FastR vectors are not writeable from other languages
  * in order to update FastR vectors from other languages:
    retrieve reference to the subset assign function `` `[<-` `` and execute it.
    Note that it will return the new vector with the updated values.

Added missing R builtins and C API

* `polyroot`
* dummy implementation of `pcre_config`

Bug fixes:

* when using GNU-R graphics (--R.UseInternalGridGraphics=false) FastR would still override the graphics package R functions
* cannot install RcppParallel #52
* visibility propagation in `tryCatch`

# 1.0 RC 12

New Features:

* the implementation of the `TruffleLanguage#toString` method uses R function `print`
  * for example: the console in Chrome DevTools will print data.frames formatted like R would

Added missing R builtins and C API

* FastR provides GNU-R compatible `parseData` for expressions parsed via `parse(...,keep.source=T)`
* `format.POSIXlt` supports following formats: %z, %Z, %x, %X.
* dummy implementation of the ALTREP framework to avoid linking problems. Most of the functions fail at runtime. #48

Bug fixes:

* `sys.calls` gives wrong result when `eval` with `envir` argument is on the call stack
* `is.na` was not correctly handling lists, for example: `is.na(list(function() 42))`
* transfer `srcref` attribute to the result of `.subset` and `[`
* `matrix(1,nrow=NULL,ncol=NULL)` caused internal FastR error instead of R user level error
* option `--polyglot` works with the native image of FastR
* added native functions optim() and optimness()
* fixed various race conditions in parallel package
* `strsplit(...,perl=T)` does not end up in an infinite loop if the pattern is not found by `pcre_exec`
* `as.character.factor` error for levels containing NAs
* `env2list` error for environments containing pairlists
* `body<-` error for non-scalar values
* `unlink` error for paths containing wildcard(s) but no path separator
*  dims attribute errorneously set to RDoubleVector; exception when retrieving the dims #49
* issues with the dplyr's `mutate` and `transmute`: #50 and #51
* fixed promises result visibility propagation eliminating extra `NULL` output of `tryCatch(cat('Hello\n'))`

# 1.0 RC 11

* upgraded the R version to R-3.5.1
  * base packages and other sources used directly from GNU-R upgraded to their R-3.5.1 versions
  * fixed differences between R-3.4.0 and R-3.5.1

* FastR does not print or log any details for internal errors unless it is run with `--jvm.DR:+PrintErrorStacktracesToFile`

Added missing R builtins and C API

* `Rf_duplicated`
* `Rf_setVar`
* `norm_rand`
* `exp_rand`

Bug fixes:

* internal error in `mapply` with empty arguments list
* `comment` and `comment<-` work with S4 objects
* `iconvlist()` was failing on argument error #43
* `range` works properly with lists
* the reference count of `dimnames` of the result of `==` was not handled properly leading to incorrect results #40
* `exists` did not work properly in all cases when used with the `mode` argument #44
* 'charIndex out of range' when parsing an incomplete source #39
* `no_proxy` environment variable was not parsed correctly
* `read.csv` treats empty value as `NA` of the same type as the rest of the values in the column #42
* `SET_NAMED` allows to decrease the reference count to support a pattern from `data.table`
* exception when writing into the result returned from `split`
* `switch` falls through only if the actual argument is empty constant: `switch('x',x=,y=42)` vs. `switch('x',x=quote(f(1,))[[3]],y=42)`
* `oldClass<-` works with external pointers and other less common R types
* C API function `Rf_setAttrib` coerces double vector to integer when setting "dim" attribute #46

# 1.0 RC 10

New features:

* interop and tooling: READ and WRITE of ActiveBinding may have side effects. This is communicated via `KEY_INFO` to the tools and other languages (e.g., a debugger may warn before evaluating an ActiveBinding)
* the MRAN mirror used by FastR as default repo was moved to https://mran.microsoft.com/snapshot/2018-06-20
* new function `install.fastr.packages` to install FastR rJava replacement and possibly other packages in the future
* print the whole guest language stacktrace if an exception occurs during an interop call into another language

Added missing R builtins and C API

* `pos.to.env` builtin
* private `do_fmin` external function from the stats packages used by public R function `optimize`
* `beta` #33

Bug fixes:

* tooling: top level statements are not marked as functions (e.g., a debugger will not treat them as such anymore)
* update rpath correctly for redistributed libraries when producing a release build. This issue caused linking problems for MacOS users #26
* UseMethod caused internal error under some specific circumstances (happens during installation of the R.oo package)
* fully support indirect use of .Internal, e.g. in `(get('.Internal'))(paste0(list(1,2),','))`
* `as.character(external-pointer)` does not crash, but prints the pointer address #28
* `file.path` with `NULL` as one of its arguments gives correct result (empty character vector)
* `format.POSIXlt` uses the same time zone database as rest of the system #29
* `dev.control(displaylist = 'inhibit')` caused `ClassCastException`
* `download.file` follows redirects.
* static members of Java interop objects are not ignored during printing and deparsing
* fixed internal error in `on.exit(NULL)`
* fixed `mget` to accept also non list values for `ifnotfound`
* updating dimensions of a vector always resets the dimnames. #34
* `env2list` used in, e.g., `as.list.environment` can handle `...` inside the environment

# 1.0 RC 9

New features

* various improvements in handling of foreign objects in R
  * [brief overview in the documentation](http://www.graalvm.org/docs/reference-manual/languages/r/#foreign)
  * [executable specification](https://github.com/oracle/fastr/blob/master/com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/library/fastr/R/interop-array-conversion-test.R#L158)

Added missing R builtins and C API

* eapply builtin
* rapply builtin

Bug fixes:

* colon builtin calculated length incorrectly in some circumstances
* `storage.mode<-` works with NULL
* Rf_coerceVector works with pairlists and language objects
* allow formal parameter names: '..1', '..1=default', '...=default'

# 1.0 RC 8

Bug fixes

* slot (`@`) was not working with foreign arrays
* memory leak when invoking some native functions
* one `symbol` was reported multiple times to the memory profiler
* maintain the same ownership relation between SEXPs as GNU-R does to prevent an unexpected collection of some SEXPs

# 1.0 RC 7

API changes

* eval.polyglot: the parameter `source` was renamed to `code`

New features

* AWT based graphics devices (jpg, png, X11, ...) supported in native image
* Seamless way to create R data frames from Polyglot objects
  * Handled by as.data.frame.polyglot.value
  * Expected structure: KEYS are used as column names, the values must be homogenous arrays (e.g. respond to HAS_SIZE)

Bug fixes

* S3 dispatch works correctly with NULL
* Paths in eval.polyglot are resolved relative to the current working directory
* Connections: sockets can always be read and written, raw connections are always binary
* Promises are evaluated in LazyLoadDBFetch (to support delayedAssign)
* Fixed broken `Rscript --version`
* Various fixes necessary to pass dplyr tests (GitHub version of dplyr)

Bugfixes

# 1.0 RC 6

New features

* Support for reading/writing graphical parameters via par
  * in preparation for full graphics package support

Added missing R builtins and C API

* 'get' builtin supports the 'envir' argument
* 'inspect' internal
* SETLEVELS
* Rf_isObject
* SET_ENCLOS
* R_nchar
* R_forceAndCall

Bugfixes

* support for formulas that include '...'
* updating attributes of NULL produces empty list with given attributes
* treat CR/LF in readLine like GNU-R
* fix in La_chol (incorrect pivot attribute in return)
* various fixes in vector coercion code to produce GNU-R compatible warnings and errors

# 1.0 RC 5

New features

* Script that configures FastR for the current system (jre/languages/R/bin/configure_fastr)
  * allows to pass additional arguments for the underlying configure script
  * by default uses the following arguments: --with-x=no --with-aqua=no --enable-memory-profiling FFLAGS=-O2

Updates in interop

* R code evaluated via interop never returns a Java primitive type, but always a vector
* Vectors of size 1 that do not contain NA can be unboxed
* Sending the READ message to an atomic R vector (array subscript in most languages) gives
  * Java primitive type as long as the value is not `NA`
  * if the value is `NA`, a special value that responds to `IS_NULL` with `true`. If this value is passed back to R it behaves as `NA` again
* Note that sending the READ message to a list, environment, or other heterogenous data structure never gives atomic Java type but a primitive R vector

Bug fixes

* Various smaller issues discovered during testing of CRAN packages.

# 1.0 RC 4

# 1.0 RC 3

Added missing R builtins and C API

* vmmin
* SETLENGTH, TRUELENGHT, SET_TRUELENGTH
* simplified version of LEVELS
* addInputHandler, removeInputHandler

Bug fixes

* The plotting window did not display anything after it was closed and reopened.
* Various smaller issues discovered during testing of CRAN packages.

New features

* Script that configures FastR for the current system (jre/languages/R/bin/configure_fastr) does not require Autotools anymore.
* Users can build a native image of the FastR runtime. The native image provides faster startup and slightly slower peak performance. Run jre/languages/R/bin/install_r_native_image to build the image.
