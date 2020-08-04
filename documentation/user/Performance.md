# High Performance

GraalVM optimizes R code that runs for extended periods of time.
The speculative optimizations based on the runtime behaviour of the R code and dynamic compilation employed by the GraalVM Runtime are capable of removing most of the abstraction penalty incurred by the dynamism and complexity of the R language.

Examine the algorithm in the following example which calculates the mutual information of a large matrix:
```
x <- matrix(runif(1000000), 1000, 1000)
mutual_R <- function(joint_dist) {
 joint_dist <- joint_dist/sum(joint_dist)
 mutual_information <- 0
 num_rows <- nrow(joint_dist)
 num_cols <- ncol(joint_dist)
 colsums <- colSums(joint_dist)
 rowsums <- rowSums(joint_dist)
 for(i in seq_along(1:num_rows)){
  for(j in seq_along(1:num_cols)){
   temp <- log((joint_dist[i,j]/(colsums[j]*rowsums[i])))
   if(!is.finite(temp)){
    temp = 0
   }
   mutual_information <-
    mutual_information + joint_dist[i,j] * temp
  }
 }
 mutual_information
}
system.time(mutual_R(x))
#   user  system elapsed
#  1.321   0.010   1.279
```

Algorithms such as this one usually require C/C++ code to run efficiently:<a href="#note-2"><sup>2</sup></a>
```
if (!require('RcppArmadillo')) {
    install.packages('RcppArmadillo')
    library(RcppArmadillo)
}
library(Rcpp)
sourceCpp("r_mutual.cpp")
x <- matrix(runif(1000000), 1000, 1000)
system.time(mutual_cpp(x))
#   user  system elapsed
#  0.037   0.003   0.040
```
(Uses [r_mutual.cpp](http://graalvm.org/docs/examples/r_mutual.cpp).)
However, after a few iterations, GraalVM runs the R code efficiently enough to
make the performance advantage of C/C++ negligible:
```
system.time(mutual_R(x))
#   user  system elapsed
#  0.063   0.001   0.077
```

GraalVM implementation of R is primarily aimed at long-running applications. Therefore, the peak performance is usually only achieved after a warmup period. While startup time is currently slower than GNUR's, due to the overhead from Java class loading and compilation, future releases will contain a native image of R with improved startup.

<br/>
<br/>
<br/>
<sup id="note-2">2</sup> When this example is run for the first time, it installs the `RcppArmadillo` package,
which may take few minutes. Note that this example can be run in both R executed
with GraalVM and GNU R.
