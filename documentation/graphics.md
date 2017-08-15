# Introduction

There are two main built-in R packages that provide basic graphical output 
support: *graphics* and *grid*. They both use some parts of even lower level
*grDevices* package, which allows users to change the "device" to which the 
graphical output will be drawn. The probably most popular graphical packages 
*lattice* and *ggplot2* are build on top of the *grid* package.

FastR has its own implementation of the *grid* package and it also emulates 
the most important R level functions from the *grDevices* package. This 
implementation is purely Java based and has been tested to work with 
the unmodified *lattice* package, support for unmodified *ggplot2* package 
is work in progress.

Any functions from the *graphics* package are not going to work with FastR, 
namely e.g. the `plot` function. R documentation, which can be displayed by 
typing `?functionName` can be used to determine if a function is from *graphics* 
or *grid* package or potentially from some *grid* based higher level package.

The longer term goal is to emulate R level *graphics* package functions by 
means of *grid* package drawing primitives.

# Examples

The *grid* package is distributed with FastR, but not loaded by default, 
therefore one has to load it using the `library` function. Following example 
draws a red rectangle in the center of the screen.

```
library(grid)
grid.rect(width=0.5, height=0.5, gp=gpar(col='red'))
```

The *lattice* package must be installed using:

```
install.packages('lattice');
```

With the *lattice* package installed, one can run the 
following example showing a barchart.

```
library(lattice)
mtcars$cars <- rownames(mtcars)
barchart(cars~mpg, data=mtcars)
```

# Devices Support

FastR supports the following devices:

* awt: opens a new AWT window for drawing, the window can be resized and 
its contents can be saved with the standard `savePlot` function. 
This is the default device for interactive sessions. 
In FastR, for compatibility with GNU R, *X11* device is alias for the *awt* device.

* svg: generates SVG image, which is saved into a specified file. 
The contents may be read into a character vector by FastR specific function 
`grDevices:::svg.off()`. FastR produces the SVG code directly from *grid* drawing commands, 
therefore the SVG code may look more SVG idiomatic and resembling the R code that created it, 
but may produce visually slightly different image compared to other devices. 
This device is default for non-interactive FastR sessions.

* jpg, png, bmp: these devices create an image file of specified format. Unlike the SVG device, 
these are also based on the AWT framework.

Devices that generate file take filename as an argument. The file will be saved on disk only 
once the standard `dev.off()` function is called. FastR honors the "device" option, which 
may override the default device. See the documentation of *grDevices* package for more details.

# Java Interoperability

The `grDevices::awt` function can accept `java.awt.Graphics2D` object and width and height in AWT units, 
in which case all the drawing will be done using that object. Example:

```
grDevices:::awt(420, 420, graphicsObj);
```

One possible use case is to create a Java based UI with a `JPanel` that will be 
used for visualizing some data from R. Override the `JPanel`'s `paint` 
method and pass the graphics object to R code using `PolyglotEngine`. 
The R code can do any *grid* based visualization and it will be directly 
displayed in the UI.

# Limitations

FastR's grid implementation does not yet support:

* expressions in `grid.text`
* `grid.xspline` function
* clipping

FastR does not plan to implement the R graphics engine display list
and related functions. However, the grid display list is implemented.

