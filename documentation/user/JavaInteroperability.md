# Interoperability with Java

GraalVM R runtime provides built-in interoperability with Java. Java class objects can be obtained via `java.type(...)`. In order to run FastR with Java interoperability features the `R` or `Rscript` commands have to be started
with the `--jvm` switch.
```
$ $GRAALVM_HOME/R --jvm
```

All the following examples are meant to be executed in the R Console, no additional Java dependencies are necessary.

The standard `new` function interprets string arguments as a Java class if such class exists. `new` also accepts Java types returned from `java.type`.
Fields and methods of Java objects can be accessed using the `$` operator.
Additionally, you can use `awt(...)` to open an R drawing device
directly on a Java Graphics surface, for more details see [Java Based Graphics](#java-based-graphics).

The following example creates a new Java `BufferedImage` object, plots random data to it using R's `grid` package,
and shows the image in a window using Java's `AWT` framework. Note that you must start the R script with `--jvm` to have access to Java interoperability.

```
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

GraalVM implementation of R provides its own rJava compatible replacement package available at [GitHub](https://github.com/oracle/fastr/tree/master/com.oracle.truffle.r.pkgs/rJava),
which can be installed using:

```shell
$ R -e "install.fastr.packages('rJava')"
```

To access third party Java libraries, they have to be placed on FastR class path.
```
> java.addToClasspath("/foo/bar.jar")
> java.addToClasspath(c("/foo/bar.jar", "/foo/bar2.jar"))
```

### Get a Java Class
The access to a Java type is done by providing the fully qualified class name to the `java.type` function:
```
> calendarClass <- java.type('java.util.GregorianCalendar')
```
The returned value is a polyglot object representing a Java type.
The respective Java class is then available through the `class` property:
```
> calendarClass$class
```

The same works also for static class members:
```
> calendarClass$getInstance()
```

Every requested class has to be on FastR classpath.
Java JDK classes, like `GregorianCalendar`, used above, work out of the box.

### Create a New Java Object
A new Java Object can be created by providing a Java type to the `new` function:
```
> calendar <- new(calendarClass)
```

It is also possible to pass over additional constructor arguments:
```
> calendar <- new(calendarClass, year=2042L, moth=3L, day=1L)
```

or use just the class name:

```
calendar <- new("java.util.GregorianCalendar")
calendar <- new("java.util.GregorianCalendar", year=2042L, moth=3L, day=1L)
```

### Accessing Fields and Methods
The access to static and instance fields and methods is provided by the `$` and `[` operators.

Access Java fields:
```
> calendarClass$SUNDAY
> calendarClass["SUNDAY"]
```

Invoke Java methods:
```
> currentTime <- calendar$getTime()
> currentTime["toString"]()
> calendar$setTime(currentTime)
```

Polyglot objects returned from a field, method or created via `new` are either
automatically converted into corresponding R values or they live on as polyglot
objects in the FastR environment. If necessary, they can be passed over to Java.

```
> cet <- java.type("java.util.TimeZone")$getTimeZone("CET")
> cetCalendar <- new(calendarClass, cet)
```

### Handling of Java Primitives
Returned java primitives, primitive wrappers and String instances are automatically converted into corresponding R values.

R `integer` values map directly to Java `int`/`Integer`, R `numeric` to Java `double`/`Double`, R `logical`
to Java `boolean`/`Boolean` and R `character` to Java `String`. If necessary R `integer` and `double`
are converted to the expected Java type.

### Inspecting Polyglot Objects
The `names` function can be used to obtain a list of instance and static members from
a polyglot Java Object or Java Class:
```
> names(calendar)
> names(calendarClass)
```

Code completion works as well:
```
> calendar$a<TAB>
```

### Working with Java Arrays
The need for Java Arrays appears at the latest when they have to be passed over to `java` as arguments.

You can create an array by creating an array class and instantiating an array from it:
```
> arrayClass <- java.type('int[]')
> intArray <- new(arrayClass, 3)
```

The component type names of primitive arrays are `boolean`, `byte`, `char`, `double`, `float`, `int`, `long`
and `short` -- the same as in each particular primitive wrapper TYPE constant (see, e.g., `Integer.TYPE.getName()`).
Note that it is possible to pass a R vector into a Java method in case the expected Java array is of a primitive
component type or String. The conversion happens then automatically on the background.
```
> integerArray <- new(java.type('java.lang.Integer[]'), 3L)
> integer2DimArray <- new('java.lang.Integer[][]', c(2L, 3L))
> stringArray <- new(java.type('java.lang.String[]'), 3L)
```

Access to array elements is provided by the `[` operator.
```
> stringArray[1] <- 'a'
> string2DimArray[1,1] <- 'a'
> element <- stringArray[1]
> element <- string2DimArray[1,1]
```

### Converting Java Arrays to R Objects
Unlike Java primitives or their wrappers, Java arrays are not automatically
converted into a R vector. Nevertheless, when appropriate they can be handled by
FastR builtin functions the same way as native R objects.
```
> sapply(intArray, function(e) { e })
> length(stringArray)
> length(string2DimArray[1])
```

### Explicit Java Array Conversion
A Java array conversion can be done explicitly by providing a Java array to the `as.vector` function:
```
> intVec <- as.vector(intArray)
```

Arrays having a Java primitive component type are converted into a R vector,
otherwise a list containing the array elements is created.
See also:
```
> characterVector <- as.character(intArray)
> logicalVector <- as.logical(intArray)
> ...
```

### The Java Iterable Interface
When appropriate, Java objects implementing `java.lang.Iterable` are handled in the same way like Java arrays when passed as arguments to functions.
```
> javaList <- new(java.type('java.util.ArrayList'))
> javaList$add(0);
> javaList$add(1)
> length(javaList)
> as.integer(javaList)
> as.logical(javaList)
```

### Compatibility with rJava
FastR comes with a with a rJava compatibility layer based on FastR Java
Interoperability features. Most of the officially documented rJava functions are
supported. For more information, see the [rJava CRAN](https://cran.r-project.org/web/packages/rJava/index.html) documentation.

* You can install FastR's `rJava` replacement using `install.packages("rJava")`.
The `install.packages` function in FastR has special handling for some packages including
`rJava` and it downloads it from FastR GitHub repository instead of from MRAN.
* as with any other R package, before executing any rJava functions, the package has to be loaded first.
```
> library(rJava)
```

Supported rJava features:
* The `$` and `[` operators work the same as described above.

## Java Graphics Interoperability
The GraalVM implementation of R includes its own Java based implementation of the `grid` package and the following graphics devices: `png`, `jpeg`, `bmp`, `svg` and `awt` (`X11` is aliased to `awt`). The `graphics` package and most of its functions are not supported at the moment.

The `awt` device is based on the Java `Graphics2D` object and users can pass it their own `Graphics2D` object instance when opening the device using the `awt` function, as shown in the Java interop example.
When the `Graphics2D` object is not provided to `awt`, it opens a new window similarly to `X11`.

The `svg` device in GraalVM implementation of R generates more lightweight SVG code than the `svg`
implementation in GNU R.
Moreover, functions tailored to manipulate the SVG device are provided: `svg.off` and `svg.string`.
The SVG device is demonstrated in the following code sample. Please use the `?functionName` syntax to learn more.

```
library(lattice)
svg()
mtcars$cars <- rownames(mtcars)
print(barchart(cars~mpg, data=mtcars))
svgCode <- svg.off()
cat(svgCode)
```
