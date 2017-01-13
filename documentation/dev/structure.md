# Introduction

The FastR codebase is structured around IDE `projects`, which are contained in directories beginning with `com.oracle.truffle.r`.
The expectation is that source code will be viewed and edited in an IDE (we will use Eclipse as the example) and the `mx` tool
has support for automatically generating the IDE project metadata via the `ideinit` command. N.B. if you run this before you have built the system with `mx build`
do not be surprised that it will compile some Java classes. It does this to gather information about Java annotation processors that is necessary for
correct rebuilding within the IDE.

The majority of the projects are "Java" projects, but any project with `native` in its name contains native code, e.g. C code, and is (ultimately) built
using `make`. `mx` handles this transparently. Note, however, that editing and building the native code in an IDE requires support for C development to have
been installed. E.g. for Eclipse, the CDE plugin.
