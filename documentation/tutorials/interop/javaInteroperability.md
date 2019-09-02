# FastR Java Interoperability

This tutorial shows how to take advantage of FastR`s Java interoperability features (and other Truffle implemented languages eventually).

All following examples are meant to be executed in the R Console, no additional Java dependencies are necessary.

# Setup
To install and setup GraalVM and FastR follow the Getting Started instructions in FastR [README](../../../README.md#getting_started).

Note that:
* in order to run FastR with Java interoperability features the R and Rscript commands have to be executed with the --jvm switch.
```
$bin/R --jvm
```
* to access third party java libraries, they have to be placed on FastR class path.
```
> java.addToClasspath("/foo/bar.jar")
> java.addToClasspath(c("/foo/bar.jar", "/foo/bar2.jar"))
```

# Working with Java Classes and Objects
## Get a Java Class
Access to a java type is done by providing the fully qualified class name to the `java.type` function. 
```
> calendarClass <- java.type('java.util.GregorianCalendar')
```
The returned value is a polyglot object representing a Java type.

the respective java class is then available through the `class` property.
```
> calendarClass$class
```

the same works also for static class members
```
> calendarClass$getInstance()
```

(Every requested class has to be on FastR`s classpath. Java JDK classes, like GregorianCalendar used above, work out of the box.)

## Create a new Java Object
By providing a java type to the `new` function.
```
> calendar <- new(calendarClass)
```

It is also possible to pass over additional constructor arguments.

```
> calendar <- new(calendarClass, year=2042L, moth=3L, day=1L)
```

Using just the class name works as well.

```
calendar <- new("java.util.GregorianCalendar")
calendar <- new("java.util.GregorianCalendar", year=2042L, moth=3L, day=1L)
```

## Accessing Fields and Methods
Access to static and instance fields and methods is provided by the `$` and `[` operators.

### Accessing Java Fields
```
> calendarClass$SUNDAY
> calendarClass["SUNDAY"]
```
### Invoking Java Methods
```
> currentTime <- calendar$getTime()
> currentTime["toString"]()
> calendar$setTime(currentTime)
```

Polyglot objects returned from a field, method or created via `new` are either automatically converted into according R values or they live on as polyglot objects in the FastR environment. If necessary, they can be passed over to java.

```
> cet <- java.type("java.util.TimeZone")$getTimeZone("CET")
> cetCalendar <- new(calendarClass, cet)
```

### Handling of Java primitives
Returned java primitives, primitive wrappers and String instances are automatically converted into according R values. 

R `integer` values map directly to Java `int`/`Integer`, R `numeric` to Java `double`/`Double`, R `logical` to Java `boolean`/`Boolean` and R `character` to Java `String`. If necessary R `integer` and `double` are converted to the expected Java type.

### Inspecting polyglot objects
The `names` function can be used to obtain a list of instance and static members from an polyglot Java Object or Java Class.
```
> names(calendar)
> names(calendarClass)
```

Code completion works as well

```
> calendar$a<TAB>
```

## Working with Java Arrays
The need for Java Arrays appears at the latest when they have to be passed over to java as arguments. 

### Create a Java Array 
By creating an array class and instantiating an array from it.
```
> arrayClass <- java.type('int[]')
> intArray <- new(arrayClass, 3)
```

The component type names of primitive arrays are `boolean`, `byte`, `char`, `double`, `float`, `int`, `long` and `short`. (The same as in each particular primitive wrapper TYPE constant - see e.g. Integer.TYPE.getName().)
Note that it is possible to pass a R vector into a Java method in case the expected java array is of a primitive component type or String. The conversion happens then automatically on the background.

```
> integerArray <- new(java.type('java.lang.Integer[]'), 3L)
> integer2DimArray <- new('java.lang.Integer[][]', c(2L, 3L))
> stringArray <- new(java.type('java.lang.String[]'), 3L)

```

### Accessing array elements
Access to array elements is provided by the `[` operator
```
> stringArray[1] <- 'a'
> string2DimArray[1,1] <- 'a'
> element <- stringArray[1]
> element <- string2DimArray[1,1]
```

### Converting Java Arrays to R objects 
Unlike Java primitives or their wrappers, java arrays aren't on access automatically converted into a R vector. Nevertheless, when appropriate they can be handled by FastR builtin functions the same way as native R objects.
```
> sapply(intArray, function(e) { e })
> length(stringArray)
> length(string2DimArray[1])
```

### Explicit Java Array conversion
By providing a Java Array to the `as.vector` function.
```
> intVec <- as.vector(intArray)
```

Arrays having a Java primitive component type are converted into a R vector, otherwise a list containing the array elements is created.

See also
```
> characterVector <- as.character(intArray)
> logicalVector <- as.logical(intArray)
> ...
```

### The Java Iterable Interface
When appropriate, Java objects implementing `java.lang.Iterable` are handled in the same way like Java Arrays when passed as arguments to functions.
```
> javaList <- new(java.type('java.util.ArrayList'))
> javaList$add(0); 
> javaList$add(1)
> length(javaList)
> as.integer(javaList)
> as.logical(javaList)
```

## Compatibility with rJava 
FastR comes with a with a rJava compatibility layer based on FastR`s Java Interoperabily features. Most of the officially documented rJava functions are supported.
For more information see also the [rJava CRAN Page](https://cran.r-project.org/web/packages/rJava/index.html)

### Setup
* DO NOT try to install rJava via `install.packages`. The FastR\`s rJava package has to be installed instead: `bin/r CMD INSTALL com.oracle.truffle.r.pkgs/rjava`

* as with any other R package, before executing any rJava functions, the package has to be loaded first.
```
> library(rJava)
```

### Supported rJava features
The `$` and `[` operators work the same as described above.

# FastR Interop Builtins
Bellow a list of available FastR Interoperability builtins. For more information see the FastR help pages or try the examples.
```
> help(java.type)
> ?java.type
> example(java.type)
```

* java.type
* java.addToClasspath
* is.polyglot.value
* eval.polyglot
* export
* import