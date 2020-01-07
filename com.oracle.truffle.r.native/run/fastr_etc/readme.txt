Contains FastR specific hand-written versions of configuration files from R_HOME/etc.
All the files are copied manually in the com.oracle.truffle.r.native/run/Makefile.
The conventions used:
  * Each OS has its own subdirectory, files shared across OSes are in "Shared" subdirectory.
  * Suffixes "llvm" and "native" distinguish versions for LLVM and NFI backend.
  * The "llvm" version is used as the default.
  * When adding a file that has "llvm" and "native" specific versions, consider updating the implementation of the `fastr.setToolchain` builtin.
