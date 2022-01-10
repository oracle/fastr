---
layout: docs-experimental
toc_group: fastr
link_title: Interoperability with Java
permalink: /reference-manual/r/JavaInteroperability/
---
# Interoperability with Java

The GraalVM R runtime provides the built-in interoperability with Java.
Java class objects can be obtained via `java.type(...)`.
In order to run R with the Java interoperability features, the `R` or `Rscript` commands have to be started with the `--jvm` option.
```shell
R --jvm
```

Note: All of the following examples are meant to be executed in the R REPL; no additional Java dependencies are necessary.

- The standard `new` function interprets String arguments as a Java class if such class exists.
- `new` also accepts Java types returned from `java.type`.
- The fields and methods of Java objects can be accessed using the `$` operator.
- Additionally, you can use `awt(...)` to open an R drawing device
directly on a Java Graphics surface. For more details see [Java Graphics Interoperability](#java-graphics-interoperability).

The following example creates a new Java `BufferedImage` object, plots random data to it using R's `grid` package,
and shows an image in a window using Java's `AWT` framework.
Note that you must start the R script with `--jvm` to have access to Java interoperability.

```R
library(grid)
openJavaWindow <- function () {
   # create image and register graphics
   imageClass <- java.type('java.awt.image.BufferedImage')
   image <- new(imageClass, 450, 450, imageClass$TYPE_INT_RGB);
   graphics <- image$getGraphics()
   graphics$setBackground(java.type('java.awt.Color')$white);
   grDevices:::awt(image$getWidth(), image$getHeight(), graphics)

   # draw image
   grid.newpage()
   pushViewport(plotViewport(margins = c(5.1, 4.1, 4.1, 2.1)))
   grid.xaxis(); grid.yaxis()
   grid.points(x = runif(10, 0, 1), y = runif(10, 0, 1),
        size = unit(0.01, "npc"))

   # open frame with image
   imageIcon <- new("javax.swing.ImageIcon", image)
   label <- new("javax.swing.JLabel", imageIcon)
   panel <- new("javax.swing.JPanel")
   panel$add(label)
   frame <- new("javax.swing.JFrame")
   frame$setMinimumSize(new("java.awt.Dimension",
                image$getWidth(), image$getHeight()))
   frame$add(panel)
   frame$setVisible(T)
   while (frame$isVisible()) Sys.sleep(1)
}
openJavaWindow()
```

GraalVM's R runtime provides its own rJava-compatible replacement package available at [GitHub](https://github.com/oracle/fastr/tree/master/com.oracle.truffle.r.pkgs/rJava), which can be installed using:
```shell
R -e "install.fastr.packages('rJava')"
```

In order for third party Java libraries to be accessed, they have to be placed on R's class path:
```shell
> java.addToClasspath("/foo/bar.jar")
> java.addToClasspath(c("/foo/bar.jar", "/foo/bar2.jar"))
```

## Getting a Java Class

The access to a Java type is achieved by providing a fully qualified class name to the `java.type` function:
```shell
> calendarClass <- java.type('java.util.GregorianCalendar')
```
The returned value is a polyglot object representing a Java type.

The respective Java class is then available through the `class` property:
```shell
> calendarClass$class
```

The same works also for static class members:
```shell
> calendarClass$getInstance()
```

Every requested class has to be on the R classpath.
The JDK classes, like `GregorianCalendar` used above, work out of the box.

## Creating a New Java Object

A new Java object can be created by providing a Java type to the `new` function:
```shell
> calendar <- new(calendarClass)
```

It is also possible to pass over additional constructor arguments:
```shell
> calendar <- new(calendarClass, year=2042L, moth=3L, day=1L)
```

Alternately, you can use just a class name:
```shell
calendar <- new("java.util.GregorianCalendar")
calendar <- new("java.util.GregorianCalendar", year=2042L, moth=3L, day=1L)
```

## Accessing Fields and Methods

The access to static and instance fields and methods is provided by the `$` and `[` operators.

To access Java fields:
```shell
> calendarClass$SUNDAY
> calendarClass["SUNDAY"]
```

To invoke Java methods:
```shell
> currentTime <- calendar$getTime()
> currentTime["toString"]()
> calendar$setTime(currentTime)
```

Polyglot objects returned from a field or method, or created via `new`, are either automatically converted into corresponding R values or they live on as polyglot objects in the GraalVM R runtime.
If necessary, they can be passed over to Java:
```shell
> cet <- java.type("java.util.TimeZone")$getTimeZone("CET")
> cetCalendar <- new(calendarClass, cet)
```

## Handling of Java Primitives

The returned Java primitives, primitive wrappers, and String instances are automatically converted into corresponding R values and map as follows:

- R `integer` values map directly to Java `int`/`Integer`
- R `numeric` to Java `double`/`Double`
- R `logical` to Java `boolean`/`Boolean`
- R `character` to Java `String`
- If necessary R `integer` and `double` are converted to the expected Java type

## Inspecting Polyglot Objects

The `names` function can be used to obtain a list of instance and static members from a polyglot Java object or Java class:
```shell
> names(calendar)
> names(calendarClass)
```

Code completion works as well:
```shell
> calendar$a<TAB>
```

## Working with Java Arrays

The need for Java arrays appears when they have to be passed over to `java` as arguments.

You can create an array by creating an array class and instantiating an array from it:
```shell
> arrayClass <- java.type('int[]')
> intArray <- new(arrayClass, 3)
```

The component type names of primitive arrays are `boolean`, `byte`, `char`, `double`, `float`, `int`, `long`,
and `short` -- the same as in each particular primitive wrapper TYPE constant (see, e.g., `Integer.TYPE.getName()`).
Note that it is possible to pass an R vector into a Java method in case the expected Java array is of a primitive component type or String.
Then, the conversion happens automatically in the background.
```shell
> integerArray <- new(java.type('java.lang.Integer[]'), 3L)
> integer2DimArray <- new('java.lang.Integer[][]', c(2L, 3L))
> stringArray <- new(java.type('java.lang.String[]'), 3L)
```

The access to array elements is provided by the `[` operator:
```shell
> stringArray[1] <- 'a'
> string2DimArray[1,1] <- 'a'
> element <- stringArray[1]
> element <- string2DimArray[1,1]
```

## Converting Java Arrays into R Objects

Unlike Java primitives or their wrappers, Java arrays are not automatically converted into an R vector.
Nevertheless, when appropriate, they can be handled by R builtin functions the same way as native R objects:
```shell
> sapply(intArray, function(e) { e })
> length(stringArray)
> length(string2DimArray[1])
```

## Explicit Java Array Conversion

A Java array conversion can be done explicitly by providing a Java array to the `as.vector` function:
```shell
> intVec <- as.vector(intArray)
```

Arrays having a Java primitive component type are converted into an R vector.
Otherwise a list containing the array elements is created:
```shell
> characterVector <- as.character(intArray)
> logicalVector <- as.logical(intArray)
> ...
```

## Java Iterable Interface

When appropriate, Java objects implementing `java.lang.Iterable` are handled in the same way as Java arrays when passed as arguments to functions:
```shell
> javaList <- new(java.type('java.util.ArrayList'))
> javaList$add(0);
> javaList$add(1)
> length(javaList)
> as.integer(javaList)
> as.logical(javaList)
```

## Compatibility with rJava

The GraalVM R runtime comes with an rJava compatibility layer based on the Java interoperability features.
Most of the officially documented rJava functions are supported.
For more information, see the [rJava CRAN](https://cran.r-project.org/web/packages/rJava/index.html) documentation.

* You can install the GraalVM R runtime's `rJava` replacement using `install.packages("rJava")`.
The `install.packages` function in R has special handling for some packages, including `rJava`, and it downloads rJava from the source repository on GitHub instead of from MRAN.
* As with any other R package, before executing any rJava functions, the package has to be loaded first:
```shell
> library(rJava)
```

Supported rJava features:
* The `$` and `[` operators work the same as described above.

## Java Graphics Interoperability

The GraalVM R runtime includes its own Java-based implementation of the `grid` package and the following graphics devices: `png`, `jpeg`, `bmp`, `svg`, and `awt` (`X11` is aliased to `awt`).
The `graphics` package and most of its functions are not supported at the moment.

The `awt` device is based on the Java `Graphics2D` object and users can pass it to their own `Graphics2D` object instance when opening the device using the `awt` function, as shown in the Java interop example.
When the `Graphics2D` object is not provided to `awt`, it opens a new window similar to `X11`.

The `svg` device in GraalVM R runtime generates more lightweight SVG code than the `svg` implementation in GNU R.
Moreover, functions tailored to manipulate the SVG device are provided: `svg.off` and `svg.string`.
The SVG device is demonstrated in the following code sample:

```R
library(lattice)
svg()
mtcars$cars <- rownames(mtcars)
print(barchart(cars~mpg, data=mtcars))
svgCode <- svg.off()
cat(svgCode)
```
To learn more, see the `?functionName` syntax.
