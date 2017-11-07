
# Quick start
FastR supports a 'managed' mode, in which it does not execute any native code directly, especially code coming from GNU R and packages,
and tries to avoid other potentially security sensitive code. To enable this mode, clean build and run
FastR with environment variable `FASTR_RFFI` set to `managed`.

# Details
FastR has an 'implementation' of RFFI that does not use any native code directly (e.g. through JNI) and implements only small subset of the API.
Any usage of the unimplemented parts will cause error at runtime. To enable this RFFI implementation clean build FastR with environment variable
`FASTR_RFFI` set to *managed* and when running FastR set java property named *fastr.rffi.factory.type* to `managed`.

There are additional options that can restrict other usages of native code in FastR:

* When FastR option `LoadPackagesNativeCode=false`, then FastR does not load builtin packages (graphics and base) native code.
Note that loading of their native code is going to fail with *managed* RFFI implementation.
* When FastR option `LoadProfiles=false`, then FastR does not load user profile, machine profile etc. Those scripts typically use
some R code that ends up trying to call native code, which is again going to fail with *managed* RFFI implementation.
* Set `FastRConfig#InternalGridAwtSupport` to `false` before building FastR. This should remove usages of AWT from FastR's
bytecode and thus reduce the amount of native code that can be invoked by running arbitrary R code in FastR.

Note that boolean FastR options are passed using syntax R:+/-OptionName. Command line to run FastR with all the
aforementioned options:

```
mx --J @'-DR:-LoadPackagesNativeCode -DR:-LoadProfiles -Dfastr.rffi.factory.type=managed' r
```
