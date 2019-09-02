# 19.3.0

New features:

* `gc` attempts to invoke Java GC when FastR is run with `--R.EnableExplicitGC=true`
  * this is intended only for testing purposes and it is not recommended to run GC explicitly in FastR.
  * `gc` is not invoking Java GC by default because GNU-R GC and Java GC are fundamentally
    different and this may lead to unintended behavior.

Added missing R builtins and C APIs

* `gctorture` and `gctorture2` built-ins for testing purposes
* `grepRaw` (only for `fixed=T`)

Bug fixes:

* `tibble` does not print properly when using `knitr` #68
* `type.convert` uses locale #88
* promise with the empty value is handled correctly in 'missing' #87
* `scan` handles non-default value of `nmax` argument
* `Rf_allocVector` fails gracefully when FastR runs out of memory
* bug in `DATAPTR` for vectors of size in bytes larger than 2^31

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
