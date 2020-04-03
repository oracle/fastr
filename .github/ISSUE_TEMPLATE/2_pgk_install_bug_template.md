---
name: "\U0001F41E Bug during R package installation"
about: Create a report for an error during installation of an R package.
title: ''
labels: bug
assignees: ''

---
Thank you for reporting a bug in FastR. We will do our best to eventually address all open issues, however, you can also help us and increase the chances of your issue being fixed sooner by following these guidelines:

#### Can you reproduce with the latest development build?

The latest development build available at https://github.com/graalvm/graalvm-ce-dev-builds/releases. Let us know even if the issue is fixed in development build, so that we can make sure that there is no regression.

#### Include the following info

* If you explicitly used different CRAN mirror than the default, please include its URL.
* If you were installing the package from sources, include the sources or a link to them.
* If FastR produced `fastr_errors_pid{xyz}.log` or `fastr_errors.log`, attach them to the issue.
* GraalVM version or git revisions of FastR and GraalVM repositories if built from source.
  * Use `$GRAALVM_HOME/bin/R --vm.version` and include the full output.
  * Alternatively, attach file `$GRAALVM_HOME/release`.
* Output of R built-in function `sessionInfo()`.
  * The output contains a list loaded R packages and the order in which they were loaded.
  * If possible, call this function immediately after the issue appears,
  or as late as possible before the issue appears.
* OS name and version.

#### Optionally: try to reduce the error

Package installation consists of several steps. Run the installation with the 
following options to turn all those steps off and then try removing the
`--no-{XYZ}` options one by one to determine, which step causes the issue.

```
install.packages('Rcpp', INSTALL_opts='--no-R --no-libs --no-help --no-data --no-demo --no-exec --no-test-load')
```

Here are additional instructions for some of those steps: 

*R*

For this step, FastR (and GNU-R too) loads the R package and as a side effect it executes any global code, 
which may fail due to incompatibility bug in R code execution. Please try the suggestions from the 
"Bug in R code execution" issue template.

*libs*

  This is when the compilation of package's native sources (C/C++ and Fotran) takes place. 
The configuration of compilers and other required tools, e.g., `ar`, is stored in `$GRAALVM_HOME/jre/languages/R/etc/Makeconf`. 
By default, FastR is configured to use the GraalVM LLVM Toolchain for C/C++ and F2C for Fotran 
(the transpiled C is compiled with the GraalVM LLVM Toolchain). 
You can switch to `gcc` by calling `fastr.setToolchain('native')`. 
Does the package install correctly when you switch the toolchain?

*docs*

During this step, the `*.Rd` help files are parsed. You can try removing some of the `*.Rd`
files from the package source to determine which one causes the issue.