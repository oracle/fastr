---
name: "\U0001F41E Bug in R code execution"
about: Create a report for a wrong result or unexpected error during R code execution.
title: ''
labels: bug
assignees: ''

---
Thank you for reporting a bug in FastR. We will do our best to eventually address all open issues, however, you can also help us and increase the chances of your issue being fixed sooner by following these guidelines:

#### Can you reproduce with the latest development build?

The latest development build available at https://github.com/graalvm/graalvm-ce-dev-builds/releases. Let us know even if the issue is fixed in development build, so that we can make sure that there is no regression.

#### Include the following info

* If FastR produced `fastr_errors_pid{xyz}.log` or `fastr_errors.log`, attach them to the issue.
* GraalVM version or git revisions of FastR and GraalVM repositories if built from source.
  * Use `$GRAALVM_HOME/bin/R --vm.version` and include the full output.
  * Alternatively, attach file `$GRAALVM_HOME/release`.
* Output of R built-in function `sessionInfo()`.
  * The output contains a list loaded R packages and the order in which they were loaded.
  * If possible, call this function immediately after the issue appears,
  or as late as possible before the issue appears.
* OS name and version.

#### Optionally: try to reduce the example code as much as possible

* Try reproducing the issue with built-in data-sets (e.g., `mtcars`, run `help(package = "datasets")` to find out more).
* If not possible, reduce the data-sets and other necessary external resources as much as possible and attach/link them in the issue.
* Try reducing the set of R packages that need to be loaded in order to reproduce the issue.
* Most of the issues boil down to a single [primitive](https://adv-r.hadley.nz/functions.html#primitive-functions)
or [internal](https://www.rdocumentation.org/packages/base/versions/3.6.2/topics/Internal) R function, e.g., `sin`, not producing correct result, e.g.,
not preserving the `names` attribute. Finding the root cause usually takes most of the time and it often does not
require any FastR specific skills, however, it does require advanced knowledge of R itself. **It is** absolutely **fine to
not** attempt at finding the root cause. However, if you do find the root cause,
the fix from our side can be significantly faster. Here are some tips:  
  * Use `traceback()` to get the R stack trace of the last error.
  * Use the built-in interactive debugger (see `?debug`) alongside in both FastR and GNU-R.
  * Use `trace(xyz, edit=T)` to edit the source code of function `xyz` from some package in order to add debugging outputs.
  * When in interactive debugger (or in any R code) you can use `parent.frame(3)$abc` to access local variable `abc` in 3rd frame on the stack,
you can use that to inspect local variables of any function that is on the stack.
  * Non-public functions from R packages can be accessed using `:::`, e.g., `debug(tools:::.install_packages)`.
  * In the end, you should be able to get very simple R code snippet that demonstrates the faulty primitive/internal function behavior, e.g.: `sin(structure(3, names='a'))`.
* See [How to make a great R reproducible example](https://stackoverflow.com/questions/5963269/how-to-make-a-great-r-reproducible-example) for more details.  