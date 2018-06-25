
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
