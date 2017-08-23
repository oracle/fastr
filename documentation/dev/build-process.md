# FastR build process

This document describes the process of building FastR with a focus on the GNUR integration.
The description is organized in a top-down manner beginning with outlining the `mx build` command and
then delving into individual scripts that patch and build parts of GNUR.

See also [building](building.md), [release](../../com.oracle.truffle.r.release/README.md)

## `mx build`

 * locates the module definition in `$(FASTR_R_HOME)/mx.fastr`
 * possibly loads the `env` file from `mx.fastr` (there is none, by default)
 * sets up binary suites, if any
 * discovers suites (fastr as primary, truffle)
 * `post-init` (see function `mx_fastr_dist.mx_post_parse_cmd_line`)
 	* instantiation of distribution `FASTR_RELEASE<rffi>`
 		* NB: consult `mx.fastr/suite.py` for distributions definitions
		* `FASTR_RFFI` environment variable is checked to instantiate the `FASTR_RELEASE<rffi>` distribution
	 	* `<rffi>` is replaced by the `FASTR_RFFI` env var, which is by default `""`
	 	* various distribution properties are initialized, such as the license, dependencies, target OS,
	 	 build path etc. (see `mx._load_distribution`)
	 	* dependency resolution
	 	* distribution archives: `[FASTR, FASTR_UNIT_TESTS, FASTR_UNIT_TESTS_NATIVE, R_FFI_PROCESSOR, TRUFFLE_R_PARSER_PROCESSOR, FASTR_RELEASE]`
	 		* see `mx_fastr._fastr_suite.dists`
 * function `mx.build()`
 	* attempts to parallelize the build (controlled by the `parallelize` argument of `mx build`)
 		* currently 107 tasks
	* building native projects: an instance of `mx.NativeProject` creates an instance `mx.NativeBuildTask`, 
		* method `_build_run_args` creates the command line for `make`, such as:
			* `['make', '-f', '/Users/zslajchrt/work/tests/graal/truffle/src/com.oracle.truffle.nfi.native/Makefile', '-j', '8']`
			* `['make']` for the patched GNUR in `$(FASTR_R_HOME)/com.oracle.truffle.r.native`
		
## Integrating GNUR
 
 The original GNUR distribution is needed for a couple of reasons:

 * To compile the FastR specific and patched native sources with the original ones (e.g. header files, the built-in package sources etc.)
 * To arrange the FastR's directory structure according to the GNUR layout
 * To test the FastR functionality by comparing its output with that of GNUR
 
 In contrast to the previous build architecture, which extracted the necessary original
 sources needed by FastR from the GNUR distribution during the build, the current build 
 architecture maintains the required original sources as part of the project in the `com.oracle.truffle.r.native/gnur/patch`
 directory. The `patch` directory contains both the original and FastR specific native sources.
 Some of the original sources are patched, but the original ones are still available via the GIT history.
 
 Note: All the original (non-patched) files were added to GIT in a single commit (`a2d8b606a8f7a61c65ec96545644f28dd4af7a71`).
 Their modifications are made in subsequent commits.
 
 Thera are many original GNUR files that are just taken without any modification from GNUR
 and copied to their respective location in the FastR directory layout (See the section *Building `run`* for details).
 
 Contrary to the previous build process, the current one does not remove the GNUR build when cleaning the project,
 which makes the build much faster. GNUR typically needs to be re-built only occasionally.
 The default build behaviour downloads the corresponding GNUR distribution to the `libdownloads` directory and installs it there.
 The GNUR home directory is available through the `GNUR_HOME_BINARY` environment variable.
 
 Note: The `libdownloads` is not removed when cleaning the project.

 Note: The GNUR distribution must be built by the FastR build process that uses special compiler options.
 
 #### The outline of the native build process:
 
 * executes `make` in `com.oracle.truffle.r.native`
	* makes the subfolders: `gnur`, `include`, `fficall`, `gnur/patch-build/src/library`, `run`
	* updates file `version.built` to be the same as `version.source` (that is manually upgraded and stored in git)
	* `platform.mk`
		* created and populated during the gnur build (`gnur/Makefile.platform`)
		* configuration information extracted from GNUR used when building packages in the FastR environment (see [building](building.md))
	
### Building GNUR

 * patches GNUR unpacked in the subdirectory using `ed` scripts
 * done in three stages: `Makefile.gnur`, `Makefile.platform`, `Makefile.libs`
	
#### `Makefile.gnur`

 * copies `com.oracle.truffle.r.native/gnur/patch` to `com.oracle.truffle.r.native/gnur/patch-build`
 * unpacks `libdownloads/R-$(R_VERSION).tar.gz`
 * optionally (Solaris) unpacks `libdownloads/$(ICONV).tar.gz`
	* Note: `libdownloads` must not contain multiple GNUR binary distributions, otherwise `R_VERSION` contains all version numbers extracted from those distribution files
 * `GNUR_CONFIG_FLAGS` constructed and passed over to the configure utility that generates the `Makeconf` file for GNUR
	 * the output in `$(GNUR_HOME_BINARY)/gnur_configure.log`
 * optionally (Linux, SunOS) patches the generated `$(GNUR_HOME_BINARY)/Makeconf` by `$(GNUR_HOME_BINARY)/Makeconf < edMakeconf` (adds `-fPIC` to `CFLAGS` and `FFLAGS`, i.e. enables Position Independent Code)
 * builds GNUR in `libdownloads/R-$(R_VERSION)` using special compiler options
 * A special configuration for Solaris:
  1. the default `iconv` utility is inadequate and has to be replaced by GNU `iconv`
  2. the solaris studio compilers must be used, assumed to be on the `PATH`
  3. Solaris runs on x64 and Sparc and the configure options are different
 * makes `com.oracle.truffle.r.native/gnur/patch-build/src/include` creating `com.oracle.truffle.r.native/gnur/patch-build/include`. 
 Some headers are already patched to conform the FastR needs (in the previous system, the patching was done by `mx.fastr/mx_fastr_edinclude.py`).
 The header files in the resulting `include` directory are later linked from `com.oracle.truffle.r.native/include`. See *Building `include`*.

_Patched files_:

 * The generated `$(GNUR_HOME_BINARY)/Makeconf` using `edMakeconf`
 
#### `Makefile.platform`

It extracts relevant parts of the generated GnuR `Makeconf` file into FastR's `platform.mk`.
The `platform.mk` file is included in many places such as the `Makefile`s for the standard packages built for FastR.

* `sedMakeconf` extracts various flags from `$(GNUR_HOME_BINARY)/Makeconf`
* `sedEtcMakeconf` extracts `LPACBLAS_LIBS` and `LAPACK_LIBS` from `$(GNUR_HOME_BINARY)/etc/Makeconf`
* `edAddFASTR` adds `-DFASTR` to `CFLAGS` and replaces `R_HOME` by `FASTR_R_HOME`

The code extracted by all scripts and some other stuff is stored into `../platform.mk`

#### `Makefile.libs`

* Copies the `Blas` and `lapack` libraries from `$(GNUR_HOME_BINARY)/src`. Also copies: `pcre`, `z`, `gfortran`, `quadmath` and `gcc_s` (from various system locations)
* Invokes `mx rcopylib` to copy individual libraries. (The `rcopylib` function is in `mx.fastr/mx_copylib.py`).
* On OS X it uses `install_name_tool` to set the library paths
* Note: `rcopylib.done`, a "sentinel" file, indicates that dependencies were checked (and possibly copied)
* Note: `FASTR_LIB_DIR` contains binary dependencies (dynamic libraries) that are not found in the standard system locations

### Building `include`
	
The `include` directory contains the patched version of the GNUR `include` directory with the standard R header files.

Note: The header files in this directory and its subdirectory are not in git: The relevant `.gitignore` snippet:
```
	/com.oracle.truffle.r.native/include/*.h
	/com.oracle.truffle.r.native/include/R_ext/*.h
	/com.oracle.truffle.r.native/include/linked
```

All header files are symbolically linked to their sources in `com.oracle.truffle.r.native/gnur/patch-build/include`.

The file `linked` is just a sentinel file indicating that the links have been made.

The contents of the patched `include` directory is copied later by `run/Makefile` to `$(FASTR_R_HOME)/include`.

_Patched files_:

 * `Rinternals.h`, `Rinterface.h`, `Rconfig.h` (generated) and `R_ext/GraphicsEngine.h` using `mx.fastr/mx_fastr_edinclude.py`

_Other required sources_:

 * `com.oracle.truffle.r.native/gnur/patch-build/include/*.h`, `com.oracle.truffle.r.native/gnur/patch-build/include/R_ext/*.h`
		
### Building `fficall`

It builds `libR` and optionally (JNI) `libjniboot`.

See:

 * [ffi](ffi.md)
 * [managed ffi](managed_ffi.md)
 * [truffle llvm ffi](truffle_llvm_ffi.md)
 * [truffle nfi](truffle_nfi.md)

The `FASTR_RFFI` variable controls which version of FFI is build: `managed` (i.e. no native), `llvm` and `nfi`.

The `common` part is built (see `common/Makefile`) prior to handing over the control 
to the corresponding FFI subdirectory (except the `managed` FFI).

Then the dynamic library `libR` is built from the object files made in the previous step, which are stored into `lib`.

In the end, on Darwin, the installation paths of `libRblas.dylib`, `libRlapack.dylib` and `libR.dylib` are updated 
using `install_name_tool`. Also the paths of `libpcre` and `libz` are updated using `mx rupdatelib` (defined in `mx.fastr/mx_copylib.py`).

#### Building `common` FFI

This builds selected GNUR files and local overrides (`*.c` and `*.f`):

* compiles the selected `main` and `appl` C sources in `com.oracle.truffle.r.native/gnur/patch-build/src/main` and `com.oracle.truffle.r.native/gnur/patch-build/src/appl`
	* main: `colors.c devices.c engine.c format.c graphics.c plot.c plot3d.c plotmath.c rlocale.c sort.c`
	* appl: `pretty.c interv.c`
	
* compiles the Fortran sources in `com.oracle.truffle.r.native/gnur/patch-build/src/appl`
* compiles the local overrides (`*.c, *.f`)
	* `../include/gnurheaders.mk` is included to define `GNUR_HEADER_DEFS` consisting of headers that we refer to indirectly
* all objects are compiled into `../../lib` (i.e. `com.oracle.truffle.r.native/fficall/lib`)

_Other required sources_:

 * From `$(GNUR_HOME)/src/main`: `colors.c, devices.c, engine.c, format.c, graphics.c, plot.c, plot3d.c, plotmath.c, rlocale.c, sort.c`
 * From `$(GNUR_HOME)/src/appl`: `pretty.c, interv.c, d*.f`

#### Building `llvm` FFI

It does pretty much the same thing as the `common` FFI build. In addition to that it:

* includes the common truffle sources from `../truffle_common`
* includes Sulong headers from `$(SULONG_DIR)/include` (`SULONG_DIR = $(abspath $(FASTR_R_HOME)/../sulong)`)
* includes `../common/rffi_upcallsindex.h`

#### Building `nfi` FFI
	
Analogous to the `llvm` build, except it:

* includes headers from `NFI_INCLUDES`, which is set in environment (by `mx`)

### Building `library` (built-in packages)

All sources needed to build the built-in packages are maintained as part of the project and are
stored in GIT. The source location corresponds to the standard GNUR location for the built-in packages,
which is `src/library` relative to the root of the distribution, i.e. `com.oracle.truffle.r.native/gnur/patch`
in the case of the FastR project.

However, the packages are built in the working copy of the `patch` directory, i.e. `patch-build`.

The following packages are currently patched: `base, compiler, datasets, utils, grDevices, graphics, grid, parallel, splines, stats, stats4, methods, tools`.

The `Makefile` just delegates the process to the individual subdirectories.

The `lib.mk` file is included into the package makefiles. It contains the common logic for
all subordinate package builds. This common logic consists of copying the original 
GNUR library (binary) files to the FastR library directory. It also defines a couple of extension targets
 `LIB_PKG_PRE`, `LIB_PKG` and `LIB_PKG_POST`, `CLEAN_PKG` that are overridden by the package makefiles.
The package sources are compiled and linked into the corresponding dynamic library (`<package>.so`).
Finally and optionally (Darwin, non-LLVM), the library is installed using the system tools.

#### Package `base`

In the pre-build stage, it changes GnuR's build script `$(GNUR_HOME_BINARY)/src/library/base/makebasedb.R`
so that it does not compress the lazy load database, then it (re)builds GnuR. The original `makebasedb.R` is
saved to `$(GNUR_HOME_BINARY)/src/library/base/makebasedb.R.tmp`, which is reused in the post-build stage.

In the post-build stage, the R script `$(FASTR_R_HOME)R/base` is patched and `$(GNUR_HOME_BINARY)/src/library/base/makebasedb.R`
is restored from the copy. Then, the GNUR is rebuilt to undo the changes made by the auxiliary 
GNUR build in the pre-build stage.

_Patched files_:

 * `$(GNUR_HOME_BINARY)/src/library/base/makebasedb.R` using `sed 's|compress = TRUE|compress = FALSE|g'`
 * the generated file `R/base.R`

#### Package `graphics`

_Other required sources_:

 * The headers reachable from `$(GNUR_HOME)/src/library/graphics`
 * C sources from `$(GNUR_HOME)/src/library/graphics/src`: `base.c, graphics.c, init.c, par.c, plot.c, plot3d.c, stem.c`
 * The headers defined in `fficall/src/include/gnurheaders.mk`

#### Package `grDevices`

_Other required sources_:

 * The header files reachable from `$(GNUR_HOME)/src/library/grDevices`
 * `$(GNUR_HOME)/src/main/gzio.h`
 * All Cairo C sources: `$(GNUR_HOME)/src/library/grDevices/src/cairo/*.c`
 * Other C sources from `$(GNUR_HOME)/src/library/grDevices/src`: `axis_scales.c, chull.c, colors.c, devCairo.c, devPS.c, devPicTeX.c, devQuartz.c, devices.c, init.c, stubs.c`
 * The headers defined in `fficall/src/include/gnurheaders.mk`

#### Package `grid`

_Patched files_:

 * `grid.c`, `state.c` using sed (`sed_grid`, `sed_state`)

_Other required sources_:

 * `grid.h`
 * `gpar.c, just.c, layout.c, matrix.c, register.c, unit.c, util.c, viewport.c`

#### Package `methods`

_Other required sources_:

 * `init.c`
 * `methods.h`

#### Package `parallel`

_Patched files_:

 * `glpi.h`, `rngstream.c`

_Other required sources_:

 * `init.c`
 * `parallel.h`

#### Package `splines`

_Patched files_:

 * `splines.c`

#### Package `stats`

_Patched files_:

 * `fft.c` using `ed_fft`
 * `modreg.h`, `nls.h`, `port.h`, `stats.h`, `ts.h`

_Other required sources_:

 * Fortan sources: `bsplvd.f, bvalue.f, bvalus.f, eureka.f, hclust.f, kmns.f, lminfl.f, loessf.f, ppr.f, qsbart.f, sgram.f, sinerp.f, sslvrg.f, stl.f, stxwx.f`
 * C sources: `init.c, isoreg.c, kmeans.c, loessc.c, monoSpl.c, sbart.c` 
 * All headers

#### Package `tools`

_Patched files_:

 * `gramRd.c`

_Other required sources_:

 * `init.c`
 * `tools.h`

#### Package `utils`

_Other required sources_:

 * `init.c`
 * `utils.h`

### Building `run`

This build prepares the FastR directory structure mimicking that of GNUR. It creates and
populates the following directories: `bin, doc, etc, share, include`.

* Overrides `bin/Rscript` and `bin/R`
* Adds `Rclasspath.sh` to `bin/execRextras`
* Updates `R_HOME_DIR` to FastR
* Overrides `examples-header.R` and `examples-footer.R` in `share/R`
* Records the FFI version to `etc/ffibuildtype`

See `run/Makefile` for more info.

_Patched files_:

 * `$(GNUR_HOME)/bin/R`, `$(GNUR_HOME)/etc/Renviron`, `$(GNUR_HOME)/etc/Makeconf` (all generated)

_Other required sources_:
 
 * `$(GNUR_HOME)/etc/Makeconf` (generated by `configure`)  (becomes local `Makeconf.etc`)
 * `$(GNUR_HOME)/etc/javaconf` (generated by `configure`)
 * `$(GNUR_HOME)/etc/repositories` (generated by `configure`)
 * `$(GNUR_HOME)/etc/ldpaths` (generated by `configure`)
 * `$(GNUR_HOME)/doc/*` (processed by `configure`)
 * From `$(GNUR_HOME)/share/`: directories `R, Rd, make, java, encodings`

## Installing recommended packages		

Note: This build resides in a separate project: `com.oracle.truffle.r.native.recommended`.

It installs the `recommended` packages that are bundled with GNU R. It has to be built separately 
from the native project that contains the packages because that is built first and before
FastR is completely built.

The tar balls of packages sources are taken from the GNUR distribution (i.e. `$(GNUR_HOME_BINARY)/src/library/Recommended`).

The command used to install a package: `$(FASTR_R_HOME)/bin/R CMD INSTALL --library=$(FASTR_R_HOME)/library $$pkgtar;`

As this takes quite a while the building is conditional on the `FASTR_RELEASE` environment variable.

N.B. this flag is not set for "normal" FastR gate builds defined in `ci.hocon`. It is set only in the post-merge "stories" build defined in `ci.overlays/fastr.hocon`

* It always installs `codetools`, as it is required by `S4`
* If `FASTR_RELEASE` is `true`, the following packages are also installed: `MASS boot class cluster lattice nnet spatial Matrix survival KernSmooth foreign nlme rpart`
* `$(NATIVE_PROJECT_DIR)/platform.mk` is included
* Weak symbol refs used (i.e. `-undefined dynamic_lookup`) so that `libR.dylib` (which loads the package libraries) does not have to be specified when building the package

### Appendix 1: Making the built-in packages separately (i.e. out of the main build process)

#### Prerequisites

```
export FASTR_HOME=~/work/fastr
```

To make a package `pkg` the working directory must be changed to `$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build/src/library/pkg`.

#### Making base
```
make PACKAGE=base TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0
```

#### Making grDevices
```
make PACKAGE=grDevices TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0
```

#### building graphics
```
make PACKAGE=graphics TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0
```

#### Making grid
```
make PACKAGE=grid TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0
```

#### Making methods
```
make PACKAGE=methods TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0
```

#### Making parallel
```
make PACKAGE=parallel TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0
```

#### Makining splines
```
make PACKAGE=splines TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0
```

#### Making stats
```
make PACKAGE=stats TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library FASTR_LIB_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0 FASTR_R_HOME=$FASTR_HOME
```

#### Making stats4
```
make PACKAGE=stats4 TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0
```

#### Making tools
```
make PACKAGE=tools TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0 FASTR_RFFI=nfi
```

#### Making utils
```
make PACKAGE=utils TOPDIR=$FASTR_HOME/com.oracle.truffle.r.native GNUR_HOME=$FASTR_HOME/com.oracle.truffle.r.native/gnur/patch-build FASTR_LIBRARY_DIR=$FASTR_HOME/library GNUR_HOME_BINARY=$FASTR_HOME/libdownloads/R-3.4.0
```
