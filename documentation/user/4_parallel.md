# Parallel Execution

The `FORK` cluster and functions depending solely on forking (e.g., `mcparallel`)
are not supported by the GraalVM R engine at the moment. One can use the `PSOCK`
cluster, which should work in the same way on GraalVM R engine as in GNU-R.

Moreover, FastR can be used as a worker node in a `PSOCK` cluster computation driven from GNU-R.
See [FastRCluter package](https://github.com/oracle/fastr/blob/master/com.oracle.truffle.r.pkgs/fastRCluster/DESCRIPTION)
for GNU-R, which provides helper functions to create `PSOCK` cluster nodes that run GraalVM R engine.
