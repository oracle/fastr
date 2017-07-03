# FastR Java Interoperability

This tutorial shows how to take advantage of FastR`s Java interoperability features (and other Truffle implemented languages eventually).

All following examples are meant to be executed in the R Console, no additional Java dependencies are necessary.

# Setup
* download and unzip GraalVM/FastR. The bin directory contains the R and Rscript commands.
* or build from the [FastR Github repository](https://github.com/graalvm/fastr)
* to access third party java libraries, they have to be placed on FastR class path
```
> java.addToClasspath("/foo/bar.jar")
> java.addToClasspath(c("/foo/bar.jar", "/foo/bar2.jar"))
```

# Working with Java Classes and Objects
## Create a Java Class
By providing the fully qualified class name to the `new.java.class` function. 
```
> calendarClass <- new.java.class('java.util.GregorianCalendar')
```

(Every requested class has to be on FastR`s classpath. Java JDK classes, like GregorianCalendar used above, work out of the box.)

The returned value is an external object representing a Java Class.

## Create a new Java Object
By providing an external object representig a Java class to the `new.external` function.
```
> calendar <- new.external(calendarClass)
```

In addition to the class it is also possible to pass over additional constructor arguments.

```
> calendar <- new.external(calendarClass, year=2042L, moth=3L, day=1L)
```

And apart from the interop builtins, the `new` function can be used as well.

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

External objects returned from a field, method or created via `new` or `new.external` are either automatically converted into according R values or they live on as external objects in the FastR environment. If necessary, they can be passed over to java.

```
> cet <- new.java.class("java.util.TimeZone")$getTimeZone("CET")
> cetCalendar <- new.external(calendarClass, cet)
```

### Handling of Java primitives
Returned java primitives, primitive wrappers and String instances are automatically converted into according R values. 

### Passing Java specific primitives as arguments
R does not have primitives as e.g. java float or byte. As a result there are cases when it is necessary to designate a value passed over to Java to be converted into such a primitive type. 

```
> byteClass <- new.java.class('java.lang.Byte')
> new.external(byteClass, as.external.byte(1))
```

also 

```
> interopByte <- as.external.byte(1)
> interopChar <- as.external.char("a")
> interopFloat <- as.external.float(1.1)
> interopLong <- as.external.long(1)
> interopShort <- as.external.short(1)
```

R `integer` values map directly to Java `int`/`Integer`, R `numeric` to Java `double`/`Double`, R `logical` to Java `boolean`/`Boolean` and R `character` to Java `String`.

### Inspecting external objects
The `names` function can be used to obtain a list of instance and static members from an external Java Object or Java Class.
```
> names(calendar)
> names(calendarClass)
```

Code completion works as well

```
> calendar$a<TAB>
```

## Working with Java Arrays
The need for Java Arrays apears at the latest when they have to be passed over to java as arguments. 

### Create a Java Array 
By providing the component type and the array length or dimensions to the `new.java.array` function.
```
> intArray <- new.java.array('int', 3)
```

The component type names of primitive arrays are `boolean`, `byte`, `char`, `double`, `float`, `int`, `long` and `short`. (The same as in each particular primitive wrapper TYPE constant - see e.g. Integer.TYPE.getName().)
```
> integerArray <- new.java.array('java.lang.Integer', 3)
> stringArray <- new.java.array('java.lang.String', 3)
> string2DimArray <- new.java.array('java.lang.String', c(2, 3))
```

### Accessing array elements
Access to array elements is provided by the `[` operator
```
> stringArray[1] <- 'a'
> string2DimArray[1,1] <- 'a'
> element <- stringArray[1]
> element <- string2DimArray[1,1]
```

### Converting R objects to Java Arrays
Another way to create java arrays is to convert a vector or a list .
```
> intArray <- as.java.array(list(0L, 1L, 2L, 3L))
> intArray <- as.java.array(c(0L, 1L, 2L, 3L))
```

The resulting array component type is either automatically given by the according R type. Otherwise it has to be explicitly specified.
```
> as.java.array(c(1L,2L,3L), 'double')
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

Arrays where the component type is a Java primitive, a primitive wrapper or String are converted into a R vector, otherwise a list containing the array elements is created.

See also
```
> characterVector <- as.character(intArray)
> logicalVector <- as.logical(intArray)
> ...
```

### The Java Iterable Interface
When appropriate, Java objects implementing `java.lang.Iterable` are handled in the same way like Java Arrays when passed as arguments to functions.
```
> javaList <- new.external(new.java.class('java.util.ArrayList'))
> javaList$add(0); 
> javaList$add(1)
> length(javaList)
> as.integer(javaList)
> as.logical(javaList)
```

## Other useful Java Interop functions
To determine whether an object is an external object.
```
> is.external(calendar)
```

To determine whether an external object is executable.
```
> is.external.executable(calendar$getTime)
```

To determine whether an external object represents `null`.
```
> is.external.null(calendar)
```

To determine whether an external object represents an array-like structure.
```
> is.external.array(intArray)
```

To obtain the class name from an external Java Object.
```
> java.class(intArray)
```

## Compatibility with rJava 
FastR comes with a with a rJava compatibility layer based on FastR`s Java Interoperabily features. While currently only a subset of rJava functionality is supported, the ultimate future goal is to have a flawless execution of rJava based R code.
For more information see also the [rJava CRAN Page](https://cran.r-project.org/web/packages/rJava/index.html)

### Setup
* DO NOT try to install rJava via `install.packages`. The FastR\`s rJava package has to be installed instead: `bin/r CMD INSTALL com.oracle.truffle.r.pkgs/rjava`   
* any additional Java Libraries have to be added to FastR class path
```
> java.addToClasspath("/foo/bar.jar")
```

* as with any other R package, before executing any rJava functions, the package has to be loaded first.
```
> library(rJava)
```

### Supported rJava features
The `$` and `[` operators work the same as described above.

The following functions are supported in at least some aspects:
```
J
.jnew
.jcall
.jfield
.jarray
.jevalArray
.jbyte
.jchar
.jshort
.jlong
.jfloat
```

# FastR Interop Builtins
Bellow a list of available FastR Interoperability builtins. For more information see the FastR help pages.
```
> help(as.external.byte)
> ?as.external.byte
>```

* as.external.byte
* as.external.char
* as.external.float
* as.external.long
* as.external.short
* as.java.array
* is.external
* is.external.array
* is.external.executable
* is.external.null
* java.class
* new.external
* new.java.array
* new.java.class
* external.eval
* export
* import