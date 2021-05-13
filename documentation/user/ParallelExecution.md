---
layout: docs-experimental
toc_group: fastr
link_title: Parallel Execution
permalink: /reference-manual/r/ParallelExecution/
redirect_from: /docs/reference-manual/r/ParallelExecution/
---
# Parallel Execution

The `FORK` cluster and functions depending solely on forking (e.g., `mcparallel`) are not supported by the GraalVM R runtime at the moment.
However, users can use the `PSOCK` cluster, which should work in the same way on the GraalVM R runtime as on GNU R.

Moreover, R can be used as a worker node in a `PSOCK` cluster computation driven from GNU R.
See [FastRCluster package](https://github.com/oracle/fastr/blob/master/com.oracle.truffle.r.pkgs/fastRCluster/DESCRIPTION) for GNU R, which provides helper functions to create `PSOCK` cluster nodes that run the GraalVM R runtime.
