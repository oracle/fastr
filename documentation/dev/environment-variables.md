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
  - List of dependencies to be bundled with FastR, separated by comma.
  - Used for release builds.
  - See [Build modes](building.md#Build-modes).

## Run-time environment variables
- `FASTR_MRAN_MIRROR`
  - MRAN mirror used by FastR for installing packages.
  - If this env var is set, the package installation works as if calling `install.packages(..., repos=Sys.getenv('FASTR_PKGS_CACHE_OPT'))`.
- `FASTR_OPTION_<option>=<value>`
  - With this pattern, you can pass an option to FastR.
  - Equivalent to `$GRAALVM_HOME/bin/R --R.<option>=<value>`.
  - E.g. `FASTR_OPTION_PrintErrorStacktracesToFile=true`.
  - See `com.oracle.truffle.r.runtime.context.FastROptions`.
- `FASTR_PKGS_CACHE_OPT`
  - The location, and other properties of package cache directory used by `mx r-pkgcache`, and `mx pkgtest` commands.
  - Example: `export FASTR_PKGS_CACHE_OPT='dir=$HOME/fastr_pkgcache,vm=fastr,sync=TRUE'`.
    - `dir` points to the directory where the cache should exist, created if necessary.
    - `vm` is either `fastr` or `gnur`
    - `sync` is either `TRUE` or `FALSE`. If `TRUE`, the write access to the package cache is synchronized.
- `GDLOG`
  - See `com.oracle.truffle.r.ffi.impl.javaGD.LoggingGD.java`
- `JAVAGD_CLASS_NAME`
  - See `org.rosuda.javaGD/src/org/rosuda/javaGD/GDInterface.java`
