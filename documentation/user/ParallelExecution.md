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

Note that unlike with the `FORK` or `PSOCK` clusters the child nodes in `SHARED` cluster are running in the same process,
therefore, e.g., locking files with `lockfile` or `flock` will not work. Moreover, the `SHARED` cluster is based on
an assumption that packages' native code does not mutate shared vectors (which is a discouraged practice) and is thread
safe and re-entrant on the C level.

If the code that you want to parallelize does not match these expectations, you can still use the `PSOCK` cluster with the GraalVM R engine.
The `FORK` cluster and functions depending solely on forking (e.g., `mcparallel`) are not supported at the moment.

<br/>
<br/>
<br/>

<sup id="note-1">1</sup> More technically, GraalVM implementation of R uses a fixed MRAN URL from `$R_HOME/etc/DEFAULT_CRAN_MIRROR`, which is a snapshot of the
CRAN repository as it was visible at a given date from the URL string.

<sup id="note-2">2</sup> When this example is run for the first time, it installs the `RcppArmadillo` package,
which may take few minutes. Note that this example can be run in both R executed
with GraalVM and GNU R.
