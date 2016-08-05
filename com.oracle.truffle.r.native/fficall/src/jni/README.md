# Notes on the JNI implementation

## JNI References

Java object values are passed to native code using JNI local references that are valid for the duration of the call. The reference protects the object from garbage collection. Evidently if native code holds on to a local reference by storing it in a native variable,
that object might be collected, possibly causing incorrect behavior (at best) later in the execution. It is possible to convert a local reference to a global reference that preserves the object across multiple JNI calls but this risks preventing objects from being collected. The global variables defined in the R FFI, e.g. R_NilValue are necessarily handled as global references. However, by default, other values are left as local references, although this can be changed by setting the variable alwaysUseGlobal in rffiutils.c to a non-zero value.

## Vector Content Copying

The R FFI provides access to vector contents as raw C pointers, e.g., int *. This requires the use of the JNI functions to access/copy the underlying data. In addition it requires  that multiple calls on the same SEXP always return the same raw pointer.
Similar to the discussion on JNI references, the raw data is released at the end of the call. There is currently no provision to retain this data across multiple JNI calls.

