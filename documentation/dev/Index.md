# FastR Developer Documentation

* Introduction
  * [Building from Source](building.md)
  * [Basic Project Structure](structure.md)
* Implementation of R concepts
  * [R FFI Implementation](ffi.md): implementation of the native R extensions API
  * [Vector sharing](sharing.md) implementation of value semantics which avoids copying if possible
* Internal implementation details
  * [Build Process Documentation](build-process.md)
  * [Testing](testing.md): unit tests and R package tests
  * [Cast pipelines](casts.md): fluent API for configuration of coercion of builtins arguments
  * [Function calls](functions.md)
  * [Notes on upgrading R version](upgrading-r.md)

