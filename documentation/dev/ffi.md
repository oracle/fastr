# The R FFI Implementation

## How R extensions work

* Loaded dynamic libraries are internally represented as `DllInfo` objects, both in FastR and GNU-R 
  * to the user code, they presented as R lists of class `DLLInfo`
  * one of the items in the R list is external pointer pointing to the internal representation (both in GNU-R and FastR)
  * loaded native libraries can be accessed by R function `getLoadedDLLs()`
* The entry point is R builtin function `dyn.load(path/to/mylibrary.so)`:
  * is it invoked, for example, from the `library` R function when loading a package, but can be invoked directly
  * it looks up native function `R_init_mylibrary` and executes it passing in the `DllInfo` internal object
  * `R_init_mylibrary` can do anything useful to initialize the package, but mainly it calls `R_registerRoutines`
    * `R_registerRoutines` takes the `DllInfo` object and then few arrays of C structs that describe the native functions to be registered. 
    * each array describes functions registered for different type of calling convention: `.C/.Fortran`, `.Call`, `.External`, etc.
    * each entry representing a function describes its name and number of arguments
    * `R_registerRoutines` puts the structures describing the native functions into the `DllInfo` object
    * in FastR: `R_registerRoutines` is a C function which calls back to our internal up-calls to do its job
      * implementing as a C function allows us to easily manipulate the C structures. In Java we would have to know their memory layout.
* R internal function `getRegisteredRoutines(myDllInfo)` gives native functions registered for given native library
  * `myDllInfo` is the external pointer to `DllInfo` internal object
  * the result is a list of lists
    * first dimension represents the type of calling convention: `.C/.Fortran`, `.Call`, `.External`, etc.
    * items of the first dimension are lists representing individual native functions, the lists have class `NativeSymbolInfo`
    * example: `.Internal(getRegisteredRoutines(stats:::C_dcauchy$dll[['info']]))`
    * `stats:::C_dcauchy` is an example of a list representing individual native function

Overall pseudocode:
```
library <- function(name, ...) {
   # ...
   dllInfo <- dyn.load(path_to_name_dot_so) # calls -> R_init_{name} -> R_registerRoutines
   routines <- getRegisteredRoutines(dllInfo)
   # add 'routines' to the namespace of package {name}
   # ...
}

# after calling library(pkg), in the namespace of pkg, accessible via `pkg:::`,
# there will an R list for each native function registered in `R_registerRoutines`
# the name of the variable holding that list is determined by the names passed to `R_registerRoutines`
.Call(pkg:::C_mynativefun, args)
```

### Calling by name

One can call native functions by name, in which case R searches all the loaded native libraries for given function. 
Note: it does not search its internal structures, but directly in the native library, i.e., bypassing the `R_registerRoutines` registration mechanism.

### Registering callables

A package can register native functions that can be retrieved and called from another package.
This is typically done in the "init" function (`R_init_mylibrary`) using the `R_RegisterCCallable` and `R_GetCCallable`.

## FastR implementation details

Note: this may need some updating (Jan 2020).

## Introduction
FastR can interface to native C and Fortran code in a number of ways, for example, access to C library APIs not supported by the Java JDK, access to LaPack functions, and the `.Call`, `.Fortran`, `.C` builtins. Each of these are defined by a Java interface, e.g. `CallRFFI` for the `.Call` builtin. To facilitate experimentation and different implementations, the implementation of these interfaces is defined by a factory class, `RFFIFactory`, that is chosen at run time via the `fastr.ffi.factory.class` system property, or the `FASTR_RFFI` environment variable.
The factory is responsible for creating an instance of the `RFFI` interface that in turn provides access to implementations of the underlying interfaces such as `CallRFFI`. This structure allows for each of the individual interfaces to be implemented by a different mechanism. Currently the default factory class is `TruffleNFI_RFFIFactory` which uses the Truffle NFI system to implement the transition to native code.

## Native Implementation
The native implementation of the [R FFI](https://cran.r-project.org/doc/manuals/r-release/R-exts.html) is contained in the `fficall` directory of
the `com.oracle/truffle.r.native` project. It's actually a bit more than that as it historically also contains code taken over and adapted from GNU R,
for example code that is sufficiently simple that it is neither necessary nor desirable to implement in Java.
The new approach to reusing code from GNU-R is to put it into the `gnur/patch` subdirectory and also maintain vanilla state of those files in branch `gnur`.
See [build process documentation](build-process.md) for more details.

 There are five sub-directories in `fficall/src`:
 * `include`
 * `common`
 * `truffle_nfi`
 * `truffle_llvm`

### The `fficall/include` directory

`include` should be thought as analogous to GNU R's `src/include`, i.e. internal headers needed by the code in `src/main`.
What we are trying to do by redefining them here is to provide a boundary so that we don`t accidently capture code from GNU R that
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

### The `common` directory
Historically, `common` contains code that has no explicit dependencies specific to Truffle NFI or Truffle LLVM and has been extracted for reuse in other implementations. 
This code is mostly copied/included from GNU R. N.B. Some modified files have a `_fastr` suffix to avoid a clash with an existing file in GNU R that would match
the Makefile rule for compiling directly from the GNU R file.

The new approach to reusing code from GNU-R is to put it into the `gnur/patch` subdirectory and also maintain vanilla state of those files in branch `gnur`.
See [build process documentation](build-process.md) for more details.

### The `truffle_nfi` directory.
`truffle_nfi` contains the implementation that is based on the Truffle Native Function Interface.

### The `truffle_llvm` directory

`truffle_llvm` contains the native side of the variant that is based on the Truffle LLVM implementation. It is described in more detail [here](truffle_llvm_ffi.md)

## RFFI Initialization
Not all of the individual interfaces need to be instantiated on startup. The `getXXXRFFI()` method of `RFFI` is responsible for instantiating the `XXX` interface (e.e.g `Call`).
However, the factory can choose to instantiate the interfaces eagerly if desired. The choice of factory class is made by `RFFIFactory.initialize()` which is called when the
initial `RContext` is being created by `PolyglotEngine`. Note that at this stage, very little code can be executed as the initial context has not yet been fully created and registered with `PolyglotEngine`.

In general, state maintained by the `RFFI` implementation classes is `RContext` specific and to facilitate this `RFFIFactory` defines a `newContextState` method that is called by `RContext`.
Again, at the point this is called the context is not active and so any execution that requires an active context must be delayed until the `initialize` method is called on the `ContextState` instance.
Typically special initialization may be required on the initialization of the initial context, such as loading native libraries, and also on the initialization of a `SHARED_PARENT_RW` context kind.

## Sharing data structures from Java with the native code

Every subclass of the `RBaseObject` abstract class, notably `RVector<ArrayT>` subclasses, may have a so called `NativeMirror` object associated with it. This object is created once the `RBaseObject` is passed to native and then used as a representation for the original `RBaseObject`. Initially FastR assigns a unique number (ID) to such `RBaseObject` and keeps this ID in the `NativeMirror` object, the ID is then passed to the native code as opaque pointer. The native code then may call a R-API function, e.g. `Rf_eval` passing it an opaque pointer, this transitions back to Java and FastR finds the `RBaseObject` corresponding to the value stored in the opaque pointer and passes this `RBaseObject` to the FastR implementation of `Rf_eval` in `JavaUpCallsRFFIImpl#Rf_eval`. Note that any opaque pointer can only be obtained using R-API function, e.g. `allocVec`, which up-calls to Java and FastR creates `RBaseObject`, corresponding `NativeMirror` with ID and passes that as the opaque pointer back to the native code.

Every subclass of `RMaterializedVector` can be materialized into native memory. In such case, its `data` field that normally holds a reference to
the managed data, e.g. `int[]`, is set to `null` and its `NativeMirror` object will hold address to the off-heap data as a `long` value.
All the operations, e.g. `getDataAt`, on such vector will now reach to the native memory instead to the managed array.
This materialization happens, for example, when the native code calls `INTEGER` R-API function, which is supposed to return a
pointer to the backing (native) array. The finalizer of the `NativeMirror` object is responsible for freeing the native memory.

Two steps are necessary, when a `RBaseObject` is about to be send to native code:
First, FastR needs to materialize any vectors sent to the native code, because the native code assumes
that it can change them in-place via this mechanism. Any object that is send to the native
code (either as an argument in a downcall or as a return value from an upcall) must be first processed through `FFIMaterializeNode` (this is done automatically in the code generated by `FFIProcessor`).

For the time being, sequences cache their materialized version to avoid having to recreate it and
to avoid issues with GC described below. In future FastR vectors may support in-place changes of
the internal representation. 

Next, once it is ensured the `RBaseObject` is materialised, the `NativeMirror` representation for the materialized version has to be created and passed over to native.

## Garbage collection

The native extensions can create R objects (e.g., via `Rf_alloc`). However, those objects are not referenced and would be subject of garbage collection.
GNU-R provides API for protecting such objects (e.g., `PROTECT`). Unfortunately, GNU-R does garbage collection only during certain up-calls to the R engine,
namely during calls that are known to allocate new R objects. This knowledge is used by extensions authors and it is common that R objects are not properly
protected when there are no allocating up-calls. This is in contrast with JVM, where GC can run at (almost) any point.
FastR has to manually protect R objects leaked to the native code for the time until next GC cycle would be executed by GNU-R,
i.e., until the next allocating up-call is invoked.

Moreover, extension authors use knowledge of relationships between R objects, which let them avoid GC protection for
R objects that are known to be referenced by other R objects that are known to be reachable from a GC root.
For example, R objects retrieved from an environment that is on a library search path do not need to be protected.
The problem is that FastR needs to materialize vectors before sending them to the native code as described above,
but the materialized vector would not be referenced by the environment. For this reason, any up-call that returns
an object (A) that the user may assume is referenced by another R object (B) must materialize (A) via `FFIMaterializeNode`
and then fixup the value inside (B) to actually create the same reference relationship as GNU-R.

This approach does not work for situations where (B) cannot hold (A) because of different internal
representation of some data structures in FastR. For example, in FastR attributes are not represented as a pair-list
but the function `ATTRIB` returns them as such. In these cases we either introduce additional
artificial field in (A) to hold materialized (B) or use a weak hash map stored in `RFFI` context.
This helps to protect such objects from GC and tie their life-cycle with what would be their
referencing object in GNU-R. What this approach doesn't solve is that the extension authors
may assume that changes done in such objects (e.g., attributes pair-list) will be visible in
their referencing object. At this point, we ignore this potential problem.

Additionally, in in case of a downcall, the materialized version of a `RBaseObject` has to be kept alive until the downcall returns, so that it is ensured that an eventual upcall might still get access to the original downcalls argument object.
