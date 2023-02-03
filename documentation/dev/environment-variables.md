# Environment variables
In this file, we enumerate all the fastr-related environment variables

## Build-time environment variables
- `GNUR_HOME_BINARY`
  - A path to prebuilt GNU-R
  - See [Using pre-built GNU-R binary](building.md#GNU-R)
- `FASTR_CC`
- `FASTR_CXX`
- `FASTR_FC`
  - FastR-specific C, C++, and Fortran compilers.
  - Used for building GNU-R from sources and/or when building native packages with FastR.
  - Do not use these env variables if you plan to use only one system-wide version of the toolchain.
- `FASTR_RELEASE`
  - If set to `true`, a release version of FastR is built
  - See [Build modes](building.md#Build-modes).
- `FASTR_NO_RECOMMENDED`
  - If set to `true`, no recommended packages are built
  - See [Build modes](building.md#Build-modes).
- `FASTR_RECOMMENDED_BINARY`
  - A path to prebuilt recommended packages.
  - See [Caching recommended packages](building.md#Caching-recommended-packages).
- `FASTR_CAPTURE_DEPENDENCIES`
  - Space-separated list of dependencies to be bundled with FastR.
  - Used for release builds.
  - For example: `export FASTR_CAPTURE_DEPENDENCIES="pcre2-8 gfortran quadmath gcc_s"`
  - See [Build modes](building.md#Build-modes).
- `PKG_LDFLAGS_OVERRIDE`
  - Where to find dependencies from `FASTR_CAPTURE_DEPENDENCIES`.
  - For example: `export PKG_LDFLAGS_OVERRIDE="-L/lib/x86_64-linux-gnu -L/usr/lib/x86_64-linux-gnu/"`
  - See [Build process](build-process.md#Release-build)

## Environment variables for development
- `ECLIPSE_EXE`
  - Path to eclipse executable used for `mx eclipseformat` or `mx checkstyle`.

## Run-time environment variables
- `FASTR_MRAN_MIRROR`
  - MRAN mirror used by FastR for installing packages.
  - If this env var is set, the package installation works as if
    calling `install.packages(..., repos=c('MRAN' = Sys.getenv('FASTR_MRAN_MIRROR')))`.
- `FASTR_REPOS`
  - name=value pairs for setting R repositories.
  - Will be used for something like `options(repos = ...)` in R.
  - Example: `FASTR_REPOS=file://home/pmarek/dev/fastr/com.oracle.truffle.r.test.native/packages/repo,CRAN=file://home/pmarek/minicran/2021-02-01`.
  - Used by `mx r-pkgtest` and `mx r-pkgcache` commands.
  - For more info run `mx r-pkgtest --help`.
- `FASTR_OPTION_<option>=<value>`
  - With this pattern, you can pass an option to FastR.
  - Equivalent to `$JAVA_HOME_HOME/bin/R --R.<option>=<value>`.
  - E.g. `FASTR_OPTION_PrintErrorStacktracesToFile=true`.
  - See `com.oracle.truffle.r.runtime.context.FastROptions`.
- `FASTR_PKGS_CACHE_OPT`
  - The location, and other properties of package cache directory used by `mx r-pkgcache`, and `mx pkgtest` commands.
  - Example: `export FASTR_PKGS_CACHE_OPT='dir=$HOME/fastr_pkgcache,vm=fastr,sync=TRUE'`.
    - `dir` points to the directory where the cache should exist, created if necessary.
    - `vm` is either `fastr` or `gnur`
    - `sync` is either `TRUE` or `FALSE`. If `TRUE`, the write access to the package cache is synchronized.
- `R_GCTORTURE=<steps>`/`FASTR_GCTORTURE=<steps>`
  - After `steps` NFI upcalls, GC will be invoked.
  - Can be used for debugging problems with "Unknown native references".
  - See `com.oracle.truffle.r.runtime.context.GCTortureState`.
- `GDLOG`
  - See `com.oracle.truffle.r.ffi.impl.javaGD.LoggingGD.java`
- `JAVAGD_CLASS_NAME`
  - See `org.rosuda.javaGD/src/org/rosuda/javaGD/GDInterface.java`
