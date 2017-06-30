# FastR NetBeans Debugging Tutorial

## Getting started

Read the documentation in *documentation/debugging.md* in FastR's GitHub repository.
In order to suppress annoying stack traces when you (accidentally) enter values in the variables view, start FastR with option `--J @-Dtruffle.nbdebug.supressLangErrs=true`.

Let's start with a simple example.
```R
source("R/binSearch.r")
binSearch(1:100, 1)
binSearch(1:100, 100)
binSearch(1:100, 50)
binSearch(1:100, 67)
binSearch(1:100, 0)
binSearch(1:100, 101) # why does this not stop
```

Set a line breakpoint in function binSearch, step through the loop iterations and find the problem.

## Debugging Packages 1
Packages are usually loaded lazily from a binary file containing the serialized code and data of the package.
Therefore, you can usually only install a function breakpoint to package code.
However, FastR keeps the package source such that you can set a line breakpoint in the package's source files.

Determine the path where packages are installed:
```R
.libPaths()
```

To demonstrate this, start FastR in debug mode and attach the NetBeans debugger. 
Then enter `.libPaths()` to determine where your R packages are installed. 

For example, let's debug package *jsonlite* (so, if you haven't installed it, do so by typing `install.packages("jsonlite")`). 
This tutorial assumes *jsonlite* in version 1.5. 
Go to the installation directory from package *jsonlite* and open the file *jsonlite/R/toJSON.R*. 
Set a line breakpoint at line 32 which calls the generic `asJSON` function. 
Now, run our script *dump.r* using `source("R/dump.r")`. 
As soon as the debugging cursor stops at the line breakpoint, step into the call of function *asJSON* to find out which of the concrete implementations is actually executed.

## Debugging Packages 2
For some reason, it may be that packages do not have source code available.
In this case installing line breakpoints is not straigt forward.
Therefore, FastR provides a facility to query the source of an R function.
As in GnuR, if the source is not available for the function, the function's body is deparsed and a string representation is generated.
FastR then generates a temporary source file containing the deparsed source code.
This temporary source file can be queried using function `.fastr.srcinfo`.

Let's work through an example: 
```R
source("R/dummy.r")
fun
attributes(fun)
```

Actually, *fun* has just been sourced and we would expect that there is a *srcref* attribute.
Let's have a look what function *source* is actually doing:

```R
> .fastr.srcinfo(source)
[1] "/tmp/deparse/source-58f3b608a4.r#1"
```

This output means that FastR generated a temporary source file "/deparse/source-58f3b608a4.r" and function *source* starts at line 1.
Open the file in NetBeans (File -> Open File ...) and set a breakpoint at line 62.

```R
source("R/dummy.r")
```

Open the __Evaluate Expression__ view under menu Debug -> Evaluate Expression.
Type *isTRUE(keep.source)* and press <ctrl> + <enter>.
The result of the evaluation should be TRUE. So, why there are no source reference attributes?
Continue stepping until line 89 (*.Internal(parse(file, n = -1, NULL, "?", srcfile, encoding))*). 
This line calls the internal parse function which we unfortunately cannot step into because internal functions do not have R code.
They are implemented in Java or C.
Now, step over line 89 and evaluate the expression: *attributes(exprs)*
The results shows that the resulting expressions actually have source reference attributes.
Now, keep stepping until line 132 (*ei <- exprs[i]*) which copies one of the elements of the parsed expression.
Now, evaluate expression: *attributes(exprs)*
It turns out that the subexpression does not have any attributes.
The reason is that FastR does not create source reference attributes in the parser because the source information is stored differently.

## Inspecting Promises
Promises are in particular difficult to handle during debugging.
The maxime of debugging is to not modify the program as it could change its behavior and make any debugging difficult.
However, this means that you will often not be able to inspect a promise until it is too late since a promise is evaluated at the time it is used.
FastR allows to inspect promises in the variables view.
Every promise (a function parameter) has three fields: `value`, `isEvaluated`, and `isEager`
If `isEager` is `true`, then you will immediately see the value of the promise. An eager promise is a special kind where FastR eagerly evaluated the value for performance reasons.
In this case it is safe to have the value already available without changing the semantics of the program.
If `isEager` is `FALSE`, then `isEvaluated` will initially also be `FALSE` and `value` will be `NULL`.
As soon as the executed function uses the parameter, the promise will be evaluated and `isEvaluated` becomes `TRUE`.
Since the function may never use the value, it is possible to inspect the promise's value by manually setting `isEvaluated` to `TRUE` in the variables view. 
The promise is now evaluated and its value can be inspected. 
In order to reset the promise to its state before, you can simply set `isEvaluated` to `FALSE` again.
  
## GraalVM-featured
FastR is part of the Graal/Truffle world and it is therefore easily possible to write R applications that interact with other programming languages like Java. 
FastR has its dedicated Java interoperability API that allows to create and use Java objects. 
The NetBeans debugger is also capable of stepping over language boundaries. 

### Preparation

1. Download GraalVM from [Oracle Technology Network (OTN)](http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html) and extract the archive.
2. Open NetBeans and add GraalVM as Java Platform:
  1. Tools -> Java Platforms
  2. Add Platform ...
  3. Java Standard Edition -> Next
  4. Navigate to the extracted folder of GraalVM and select the __jdk__ subfolder and click *Next*.
  5. Specify an appropriate platform name like __GraalVM JDK__ and click finish. 
3. Open the NetBeans project *InteropDebugging* and ensure that it uses __GraalVM JDK__ as platform:
  1. Right click on the project and select *Properties*.
  2. Select *Libraries* and choose __GraalVM JDK__ in the dropdown menu labeled with *Java Platform:*. 
4. To be able to build the project, ensure that the library *truffle-api.jar* is imported correctly.
  * The easiest way is to copy or link the wohle GraalVM into the project's root folder using the folder name `graalvm`.
  * Otherwise: 
    1. Right click on the project and select *Properties*.
    2. Then select entry *Libraries*, select the *Compile* tab and look for *Classpath*.
    3. Click on *...* and add file *graalvm/lib/truffle/truffle-api.jar*, where *graalvm* is the folder where you extracted the downloaded GraalVM into.
5. Clean and build project *InteropDebugging*.

### Inter-language Debugging 

File `Main.java` creates a `PolyglotEngine` object that can execute R code. This is basically the FastR engine. 
The engine object can now run R code by creating a source object (representing R code) and submitting the source to the engine. 
The expression `fromString("print('Hello, World! (from string)')")` creates a source code from a string. 
Expression `fromFile("R/main.r")` creates source code from file *R/main.r*. 

Now, set a line breakpoint at line 46 (the second eval expression), build the project and run the Java application using GraalVM on the command line:
`graalvm-<version>/bin/graalvm -J:-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -cp build/classes com.oracle.truffe.r.Main` 
Attach the NetBeans debugger as described in *documentation/debugging.md* via Debug -> Attach Debugger. 
Once the debugger breaks at line 46, you can step into the R application (Debug -> Step Into). 
The debugging cursor will next arrive in file *R/main.r*. 

File *R/main.r* also uses language interoperability to create a Java object and to run JavaScript code.
We can easily debug the executed Java code by setting a breakpoint in method `java.util.Date.toString`. 
During stepping through the R program, you will also step into the Java code. 

Next, lines 31 to 35 in *R/main.r* instantiate an object of a class in our NetBeans Java project. 
Before we can use our class *JavaMessage*, we need to add this project to the class path for the Java interoperability. 
This is done by statement `java.addToClasspath("build/classes")`. 
You can now also set a breakpoint in the `getMessage()` method and the debugger will halt on this breakpoint if the R expression `obj$getMessage()` is evaluated. 

Lines 38 and 39 further evaluate code of a different language, namely JavaScript.
If you have stepped to this call, you will be able to step into the JavaScript program.
You can then continue your debugging activities in the JavaScript program and you will return to the origin.

