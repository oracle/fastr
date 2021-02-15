/**
* Just a dummy source so we force compilation for base library, therefore linking of shared library that
* contains a section with LLVM bitcode.
* Otherwise, we would only copy base.so from GNU-R, which does not contain section with LLVM bitcode.
* Note that we need a section with LLVM bitcode so that we can run FastR with LLVM backend.
*/