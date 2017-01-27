The C code in this directory is never compiled by the standard C compiler to create compiled object code.
It is compiled solely to create LLVM IR which is interpreted at runtime. This is controlled by the -DFASTR_LLVM "compiler" flag.
