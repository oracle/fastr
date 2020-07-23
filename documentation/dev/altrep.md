# ALTREP

ALTREP (alternative data representation) introduced in GNU-R version 3.5.0 enables package developers to provide an alternative data representation of R objects. The standard representation of data of an object requires the data to be resident at the memory, on the other hand with ALTREP the data might be dynamically fetched from an arbitrary source, eg. dynamically fetched from a database.

We expect that the reader of this document is familiar with the ALTREP, if you are not, see the references first. We will present examples of ALTREP and explain how they are represented in FastR.

------

Let us walk through an example of ALTREP usage. Bellow is a full snippet of C code that creates a *class descriptor*, registers some ALTREP methods, and finally, creates an instance. Note that the example contains altinteger vector, but the same principle would apply for all types of ALTREP vectors.

```C
// Declaration of ALTREP methods (native functions).
static R_xlen_t Length(SEXP instance);
static void * Dataptr(SEXP instance, Rboolean writeable);
static int Elt(SEXP instance, R_xlen_t index);

SEXP create_altrep_instance() {
    // Create class descriptor.
	R_altrep_class_t class_descriptor = R_make_altinteger_class("ClassName", "PackageName", dll_info);
    // Register methods.
	R_set_altrep_Length_method(class_descriptor, Length);
	R_set_altvec_Dataptr_method(class_descriptor, Dataptr);
    R_set_altinteger_Elt_method(class_descriptor, Elt);
    // ...
    // Create instance with no instance data.
    R_new_altrep(class_descriptor, R_NilValue, R_NilValue);
}
```

## Class descriptors

`R_altrep_class_t` corresponds to `AltRepClassDescriptor` hierarchy. A class descriptor holds references to ALTREP methods among other things. A reference to ALTREP method is represented with `AltrepMethodDescriptor` which holds some information about the corresponding native function, eg. whether the function was registered in NFI or LLVM backend. `AltrepMethodDescriptor` contains a `method` field that is in fact a `TruffleObject` and is executable at runtime via interop.

`AltRepClassDescriptor` also has `noMethodRedefined` assumption that holds as long as no ALTREP method is redefined for some class descriptor. Note that the initial method *registration* as done in the example is not considered a method *redefinition*. A method *redefinition* happens when there is already a method registered and we set a different function in place of the old one via eg. `R_set_altrep_Length_method`.

`className`, `packageName`, and `dllInfo` fields of `AltRepClassDescriptor` do not currently have any usage. The corresponding structures in GNU-R are used for serialization and deserialization of ALTREP instances and classes.

All the class descriptors are saved in `AltRepContext`, which does not currently have any usage. In the future it might be useful for implementation of serialization.

## Instances

This is roughly the structure of an ALTREP instance in FastR:

```
RIntVector
  |
  |--- RAltIntVectorData
          |
          |--- AltIntegerClassDescriptor
                     |
                     |----- AltrepMethodDescriptor
```

An ALTREP vector is represented as standard vector eg. `RIntVector`, `RStringVector`, etc. with `RAltrepVectorData` as `data`. Moreover, there is a method `RBaseObject.setAltrep()` which sets the ALTREP bit in a bitmask - similar to how `setS4()` works. Every vector with ALTREP data is always assumed to have ALTREP bit set.

The ALTREP vectors in FastR have exactly the same interface as standard vectors, as in GNU-R.  The key difference is in the behavior of exported messages of `VectorDataLibrary` - most of the exported messages do down calls which might be visible in performance.



## Down calls

Since all the ALTREP methods are in fact native functions, the down calling infrastructure is very important. Most of the exported messages in `RAltrepVectorData` class hierarchy do down calls. `AltrepRFFI` is an entry point for every ALTREP down call - it contains nodes that represent all the possible ALTREP methods eg. `DataptrNode`, `EltNode`, etc. `AltrepRFFI` resembles the pattern used for `BaseRFFI`, including the down call node factories, and references from `TruffleNFI_Context`, `TruffleLLVM_Context` and `TruffleMixed_Context` with some differences.

This is a simplified subtree of an AST representing statement `instance[[1]]`, where instance is an ALTREP vector.

```
RAltIntVectorData$GetIntAt
  |
  |--- AltrepRFFI$DataptrNode  (Or AltrepRFFI$EltNode)
          |
          |---- AltrepDownCallNode
```

`AltrepDownCallNode` does not inherit from existing `DownCallNode`, it rather implements the down calling mechanism from the scratch, because:

- `DownCallNode` uses uncached `InteropLibrary` for execution and therefore does not have sufficient performance when such down calls are made frequently.
- `DownCallNode` is more suitable for static targets ie. for a situation when we know which native function we want to call ahead of time. In ALTREP it is possible, although not very useful, to redefine certain method and therefor change the down call target at runtime.



## Implementation of rest of ALTREP API

Apart from functions to create class descriptors, registred methods, and create new instances, ALTREP consists also of a native API primarily designed for querying data from vectors. There are many functions, let us list only the most notable ones (for altintegers):

- `INTEGER_ELT`
- `INTEGER_GET_REGION`
- `INTEGER_NO_NA`
- `INTEGER_IS_SORTED`

These functions, represented as up calls in FastR, are designed to only dispatch to the corresponding method in `VectorDataLibrary`. For example `INTEGER_ELT` uses `getIntAt` message and `INTEGER_NO_NA` uses `isComplete` message. For `INTEGER_GET_REGION` we implemented `VectorDataLibrary.getIntRegion` with a reasonable default implementation.

In conclusion, the rest of the ALTREP API (API that does not directly create new classes and instances) is implemented entirely as a part of `VectorDataLibrary` with reasonable defaults. Therefore, this API may be used for all kinds of vectors.



## Tests

There are two native packages used to test ALTREP: `classtests` and `altreprffitests`. Both of them are integrated in our `pkgtest` infrastructure.

**TODO**: Mention unit tests?

### `classtests`

`classtests` package serves as a TCK (Test Compatibility Kit) for ALTREP API. It gets an ALTREP instance (more specifically a factory method, not an instance) as the input and it checks whether this instance satisfies various native code contracts - for example it tests whether the data pointer of the instance remains the same as long as it is not collected by GC, or whether the data gathered by `INTEGER_GET_REGION` are the same as the data gathered by `INTEGER_ELT`.

`classtests` is written in C++ and it contains as little R code as possible. It depends only on the standard library of C++. There is a very simple unit testing framework implemented.

### `altreprffitests`

`altreprffitests` tests whether ALTREP instances can be used transparently in R. This package contains some ALTREP class definitions in C++ but all the tests are written in R.

Key difference from `classtests` is that in `altreprffitests` we test more ALTREP instance with different registered methods at the same time, while in `classtests` we test just one instance.



## TODOs

- Serialization
- `Match` method implementation



## References

- Link to the ALTREP documentation in GNU-R SVN repository: [ALTREP.html](https://svn.r-project.org/R/branches/ALTREP/ALTREP.html)
- [Altrepisode repository](https://github.com/romainfrancois/altrepisode) by Romain Francois and corresponding [Blog posts](https://purrple.cat/blog/2018/10/14/altrep-and-cpp/).
- [Luke Tierny presentation](http://homepage.divms.uiowa.edu/~luke/talks/uiowa-2018.pdf).