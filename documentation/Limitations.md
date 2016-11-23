# Limitations

The long-term goal is for FastR to be a high-performance drop-in replacement for the reference implementation (open source GNU R). However, this is
a challenging goal for a language and ecosystem as rich as R's. At the time of writing FastR implements almost the entire language, but many packages
do not install or run correctly. For pure R packages this is generally due to incomplete implementations of the R builtin functions; either they are not
implemented at all or some argument types are not accepted. In due course we expect to provide a web page listing the status of all the CRAN packages
on FastR.

## Native Code

GNU R makes extensive use of native code, mostly C, to implement aspects of the default packages and, of course, package developers are encouraged to follow
the same strategy to finesse performance problems. The native extensions [interface](https://cran.r-project.org/doc/manuals/r-release/R-exts.html#System-and-foreign-language-interfaces)
is a difficult fit for a Java implementation such as FastR as it is strongly biased towards the existing GNU R implementation. In contrast Java
has a very abstract and portable native code interface, but it is difficult and inefficent to map this to the R interface. Especially problematic
are packages that actually depend on the internal details of GNUR, which can be enabled by setting `USE_RINTERNALS` in native code. FastR cannot support this
at all. Where this is being used by a package simply as a performance speedup (some function calls become C macros), the package will still build, but run more slowly.
However, where this is genuinely being used to access the GNU R internal data structures, it will fail to build. Note that whereas native C code produces
a performance speedup with GNU R it will likely run slower on FastR due to the overhead of the JNI interface. We are actively working on a solution to this problem.

In addition many of the default packages, e.g. `stats`, `graphics` contain a lot of C code that may use R internals. FastR has taken a mixed approach to
implementing these "package" builtins. Some have been translated to Java, some that do not depend on the GNU R internals are accessed through the
native interface, but many are not implemented. FastR is adopting "as-needed" approach to these functions.
