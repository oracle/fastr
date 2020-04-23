# Parallel Execution

GraalVM R engine adds a new cluster type `SHARED` for the `parallel` package. This cluster starts new jobs as new threads inside the same process. Example:

```
library(parallel)
cl0 <- makeCluster(7, 'SHARED')
clusterApply(cl0, seq_along(cl0), function(i) i)
```

Worker nodes inherit the attached packages from the parent node with copy-on-write semantics, but not the global environment.
This means that you do not need to load again R libraries on the worker nodes but values (including functions) from the global
environment have to be transferred to the worker nodes, e.g., using `clusterExport`.

Note that unlike with the `FORK` or `PSOCK` clusters the child nodes in `SHARED` cluster are running in
the same process, therefore, e.g., locking files with `lockfile` or `flock` will not work. Moreover,
the `SHARED` cluster is based on some assumptions about used R packages:

* The native code must not mutate shared vectors (which is a discouraged practice).
* The native code must be re-entrant (it can be executed from multiple threads,
but for compatibility FastR allows only one thread to be executing native code).
* If the package contains some global data other than R functions,
these must not be mutated after the package was loaded.

If the code that you want to parallelize does not match these expectations,
you can still use the `PSOCK` cluster with the GraalVM R engine.
The `FORK` cluster and functions depending solely on forking (e.g., `mcparallel`) are not supported at the moment.