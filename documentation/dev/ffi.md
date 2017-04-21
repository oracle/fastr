# The R FFI Implementation

# Introduction
FastR can interface to native C and Fortran code in a number of ways, for example, access to C library APIs not supported by the Java JDK, access to LaPack functions, and the `.Call`, `.Fortran`, `.C` builtins. Each of these are defined by a Java interface,e.g. `CallRFFI` for the `.Call` builtin. To facilitate experimentation and different implementations, the implementation of these interfaces is defined by a factory class, `RFFIFactory`, that is chosen at run time via the `fastr.ffi.factory.class` system property, or the `FASTR_RFFI` environment variable.
The factory is responsible for creating an instance of the `RFFI` interface that in turn provides access to implementations of the underlying interfaces such as `CallRFFI`. This structure allows
for each of the individual interfaces to be implemented by a different mechanism. Currently the default factory class is `JNI_RFFIFactory` which uses the Java JNI system to implement the transition to native code.

# No native code mode
FastR can be configured to avoid running any unmanaged code coming from GNU R or packages. It is described in more detail [here](managed_ffi.md).

# Native Implementation
The native implementation of the [R FFI](https://cran.r-project.org/doc/manuals/r-release/R-exts.html) is contained in the `fficall` directory of
the `com.oracle/truffle.r.native` project`. It's actually a bit more than that as it also contains code copied from GNU R, for example that supports graphics or is sufficiently
simple that it is neither necessary nor desirable to implement in Java. As this has evolved a better name for `fficall` would probably be `main`
for compatibility with GNU R.

 There are five sub-directories in `fficall/src`:
 * `include`
 * `common`
 * `jni`
 * `truffle_nfi`
 * `truffle_llvm`

## The `fficall/include` directory

`include` should be thought as analgous to GNU R's `src/include`, i.e. internal headers needed by the code in `src/main`.
What we are trying to do by redefining them here is provide a boundary so that we don`t accidently capture code from GNU R that
is specific to the implementation of GNU R that is different in FastR, e.g., the representation of R objects. Evidently not every
piece of GNU R code or an internal header has that characteristic but this strategy allows us some control to draw the boundary as
tight as possible. Obviously we want to avoid duplicating (copying) code, as this requires validating the copy when migrating GNU R versions,
so there are three levels of implementation choice for the content of the header in this directory:

* Leave empty. This allows a `#include` to succeed and, if code does not actually use any symbols from the header, is ok.
* Indirect to the real GNU R header. This is potentially dangerous but a simple default for code that uses symbols from the header.
* Extract specific definitions from the GNU R header into a cut-down version. While this copies code it may be necessary to avoid unwanted aspects of the GNU R header. In principle this can be done by a "copy with sed" approach.

The indirection requires the use of the quote form of the `#include` directive. To avoid using a path that is GNU R version dependent,
the file ``gnurheaders.mk` provides a make variable `GNUR_HEADER_DEFS` with a set of appropriate -`D CFLAGS`.

Ideally, code is always compiled in such a way that headers are never implicitly read from GNU R, only via the `include` directory.
Unfortunately this cannot always be guaranteed as a directive of the form include "foo.h" (as opposed to include <foo.h>) in the
GNU R C code will always access a header in the same directory as the code being compiled. I.e., only the angle-bracket form can be controlled
by the `-I` compiler flag. If this is a problem, the only solution is to "copy with sed" the `.c` file and convert the quote form to the
angle bracket form.

## The `common` directory
`common` contains code that has no explicit JNI dependencies and has been extracted for reuse in other implementations. This code is mostly
copied/included from GNU R. N.B. Some modified files have a `_fastr` suffix to avoid a clash with an existing file in GNU R that would match
the Makefile rule for compiling directly from the GNU R file.

## The `jni` directory
`jni` contains the implementation that is based on and has explicit dependencies on Java JNI. It is described in more detail [here](jni_ffi.md). This is the default implementation.

## The `truffle_nfi` directory.
`truffle_nfi` contains the implementation that is based on the Truffle Native Function Interface. It is enabled by setting `FASTR_RFFIU=nfi` and doing a clean build.
The implementation is currently incomplete.

## The `truffle_llvm` directory

`truffle_llvm` contains the native side of the variant that is based on the Truffle LLVM implementation. It is described in more detail [here](truffle_llvm_ffi.md)

# RFFI Initialization
Not all of the individual interfaces need to be instantiated on startup. The `getXXXRFFI()` method of `RFFI` is responsible for instantiating the `XXX` interface (e.e.g `Call`).
However, the factory can choose to instantiate the interfaces eagerly if desired. The choice of factory class is made by `RFFIFactory.initialize()` which is called when the
initial `RContext` is being created by `PolyglotEngine`. Note that at this stage, very little code can be executed as the initial context has not yet been fully created and registered with `PolyglotEngine`.

In general, state maintained by the `RFFI` implementation classes is `RContext` specific and to facilitate this `RFFIFactory` defines a `newContextState` method that is called by `RContext`. Again, at the point this is called the context is not active and so any execution that requires an active context must be delayed until the `initialize` method is called on the `ContextState` instance. Typically special initialization may be required on the initialization of the initial context, such as loading native libraries, and also on the initialization of a `SHARED_PARENT_RW` context kind.
