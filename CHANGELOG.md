
# 1.0 RC 5

Updates in interop:

* R code evaluated via interop never returns a Java primitive type, but always a vector
* Vectors of size 1 that do not contain NA can be unboxed
* Sending the READ message to an atomic R vector (array subscript in most languages) gives
  * Java primitive type as long as the value is not `NA`
  * a special value that responds to `IS_NULL` with `true`. If this value is passed back to R it behaves as `NA` again
* Note that sending the READ message to a list, environment, or other heterogenous data structure never gives atomic Java type but a primitive R vector

# 1.0 RC 4

# 1.0 RC 3

Added missing R builtins and C API

* vmmin
* SETLENGTH, TRUELENGHT, SET_TRUELENGTH
* simplified version of LEVELS
* addInputHandler, removeInputHandler

Bug fixes

* The plotting window did not display anything after it was closed and reopened.
* Various smaller issues discovered during testing of CRAN packages.

New features

* Script that configures FastR for the current system (jre/languages/R/bin/configure_fastr) does not require Autotools anymore.
* Users can build a native image of the FastR runtime. The native image provides faster startup and slightly slower peak performance. Run jre/languages/R/bin/install_r_native_image to build the image.
