# rJava
R/Java interface allowing the use of Java from R as well as embedding
R into Java (via JRI)

Please visit the [main rJava project page on
RForge.net](http://rforge.net) for details.

### Installation

Recommended installation of the latest development version is via

    install.packages("rJava",,"http://rforge.net")

in R. The RForge.net repository is updated automatically on each
commit. On OS X you may need to add `type='source'`.

### Sources

When checking out the sources, you *must* use

    git clone --recursive https://github.com/s-u/rJava.git

since rJava includes REngine as a submodule. If you want to create a
package from the source checkout, you *must* use `sh mkdist` to do so
since the checkout is not the acutal package.

### Mailing list and bug reports

Please use
[stats-rosuda-devel](https://mailman.rz.uni-augsburg.de/mailman/listinfo/stats-rosuda-devel)
mailing list for questions about rJava and [rJava GitHub issues
page](https://github.com/s-u/rJava/issues) to report bugs.

