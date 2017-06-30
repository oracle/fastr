# FastR NetBeans Debugging Tutorial

## Getting started

Read the documentation in *documentation/debugging.md* in FastR's GitHub repository.  

This tutorial is based on GraalVM (and not on standalone FastR, which has a slightly different command line interface).  
Therefore, download the latest GraalVM from [Oracle Technology Network (OTN)](http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html) and extract the archive.  
  
First, start the FastR shell by running following command:  

    graalvm/bin/R -J:-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -J:-Dtruffle.ndebug.supressLangErrs=true

An argument prefixed with `-J:` will be passed to the *Java Virtual Machine*.  
The first argument `-J:-Xrunjdwp:...` enables a debugging server such that we are able to attach a debugger.  
The second argument is to suppress annoying error output when you (accidentally) enter values in the variables view. 
Once you run the command, it will print following line:  

    Listening for transport dt_socket at address: 8000

This means that the VM is ready and waits for a remote debugger to connect. Therefore, start NetBeans and attach the debugger as described in *documentation/debugging.md*.  

Let's start with a simple example:  

    source("R/binSearch.r")  
    binSearch(1:100, 1)  
    binSearch(1:100, 100)  
    binSearch(1:100, 50)  
    binSearch(1:100, 67)  
    binSearch(1:100, 0)   
    binSearch(1:100, 101) # why does this not stop  

Set a line breakpoint in function `binSearch`, step through the loop iterations and find the problem.

## Debugging Packages 1
Packages are loaded lazily from a binary file containing the serialized code and data of the package.
Therefore, you can usually only install a function breakpoint to package code.
However, FastR keeps the package source such that you can set a line breakpoint in the package's source files.

Determine the path where packages are installed:

    .libPaths()  

To demonstrate this, start FastR in debug mode and attach the NetBeans debugger. 
Then enter `.libPaths()` to determine where your R packages are installed. 

For example, let's debug package *jsonlite* (so, if you haven't installed it, do so by typing `install.packages("jsonlite")`).  
This tutorial assumes *jsonlite* in version 1.5.  
Go to the installation directory from package *jsonlite* and open the file *jsonlite/R/toJSON.R*.  
Set a line breakpoint at line 31 which calls the generic `asJSON` function.  
Now, run our script *dump.r* using `source("R/dump.r")`.  
As soon as the debugging cursor stops at the line breakpoint, step into the call of function *asJSON* to find out which of the concrete implementations is actually executed.

## Debugging Packages 2
For some reason, it may be that packages do not have source code available.
In this case installing line breakpoints is not straight forward.
Therefore, FastR provides a facility to query the source of an R function.
As in GnuR, if the source is not available for the function, the function's body is deparsed and a string representation is generated.
FastR then generates a temporary source file containing the deparsed source code.
This temporary source file can be queried using function `.fastr.srcinfo`.

Let's work through an example: 
Load the provided file `dummy.r` which defines a function named *fun*.

    source("R/dummy.r")  
    fun  
    attributes(fun)  

Actually, *fun* has just been sourced and we would expect that there is a *srcref* attribute.
Let's have a look what function *source* is actually doing:

    .fastr.srcinfo(source)  
    [1] "/tmp/deparse/source-58f3b608a4.r#1"  

This output means that FastR generated a temporary source file "/deparse/source-58f3b608a4.r" and function *source* starts at line 1.
Open the file in NetBeans (File -> Open File ...) and set a breakpoint at line 62.

    source("R/dummy.r")  

Open the __Evaluate Expression__ view under menu Debug -> Evaluate Expression.  
Type `isTRUE(keep.source)` and press __CTRL + ENTER__.  
The result of the evaluation should be `TRUE`. So, why there are no source reference attributes?  
Continue stepping until line 90 (`.Internal(parse(file, n = -1, NULL, "?", srcfile, encoding))`).  
This line calls the internal parse function which we unfortunately cannot step into because internal functions do not have R code.  
They are implemented in Java or C.  
Now, step over line 89 and evaluate the expression: `attributes(exprs)`  
The results shows that the resulting expressions actually have source reference attributes.  
Now, keep stepping until line 132 (`ei <- exprs[i]`) which copies one of the elements of the parsed expression.  
Now, evaluate expression: `attributes(exprs[i])`  
It turns out that the subexpression does not have any attributes.  
The reason is that FastR does not create source reference attributes in the parser because the source information is stored differently.  

## Inspecting Promises
Promises are in particular difficult to handle during debugging.  
The maxim of debugging is to not modify the program as it could change its behavior and make any debugging difficult.  
However, this means that you will often not be able to inspect a promise until it is too late since a promise is evaluated at the time it is used.  
FastR allows to inspect promises in the variables view.  
Every promise (function parameter) has three fields: `value`, `isEvaluated`, and `expression`  
If `isEvaluated` cannot be modified, then you will immediately see the value of the promise.   
If `isEvaluated` can be modified, then it will initially be `FALSE` and `value` will be `NULL`.  
As soon as the executed function uses the parameter, the promise will be evaluated and `isEvaluated` becomes `TRUE`.  
Since the function may never use the value, it is possible to inspect the promise's value by manually setting `isEvaluated` to `TRUE` in the variables view.   
The promise is now evaluated and its value can be inspected.   
In order to reset the promise to its state before, you can simply set `isEvaluated` to `FALSE` again.  
The field `expression` shows the actual expression of the promise, e.g., as it has been passed in a function call.  

For example, load the source of file *dummy.r* (if not already done).  

    source("R/dummy.r")

Set a line breakpoint somewhere in function `fun` and call the function as following:  

    fun({ s0 <- "Hello"; s1 <- "World"; paste0(s0, ", ", s1) })

When the debugger stops at the breakpoint in function `fun`, you will notice that variable `x` is of type *promise*.  
The field `value` is `NULL` and field `isEvaluated` is `FALSE`.  
Now, change the value of field `isEvaluated` to `TRUE`.  
Unfortunately, the variables view does not recognize that other values might also have changed.  
But we can use the __Evaluate Expression__ view to inspect the updated value of `x`.  
Enter `x` and press __CTRL + ENTER__.  
You will see the final string the promise evaluates to.  
Now, reset the promise to not alter the programs behavior by setting field `isEvaluated` to `FALSE` in the variables view.  
  
## GraalVM-featured
FastR is part of the Graal/Truffle world and it is therefore easily possible to write R applications that interact with other programming languages like Java. 
FastR has its dedicated Java interoperability API that allows to create and use Java objects. 
It is also possible to cross language boundaries during stepping when using the NetBeans debugger with GraalVM.  

### Preparation

1. Open NetBeans and add GraalVM as Java Platform:
  1. Tools -> Java Platforms
  2. Add Platform ...
  3. Java Standard Edition -> Next
  4. Navigate to the extracted folder of GraalVM and select the __jdk__ subfolder and click *Next*.
  5. Specify an appropriate platform name like __GraalVM JDK__ and click finish. 
2. Open the NetBeans project *InteropDebugging* and ensure that it uses __GraalVM JDK__ as platform:
3. Open the NetBeans project *InteropDebugging*
  1. File -> New Project ...
  2. Java Project with Existing Sources
  3. Specify name *InteropDebugging*, select folder *InteropDebugging* and click Next
  4. Add folder *src* to *Source Package Folders* and click Next
  5. There should be two files *JavaMessage.java* and *Main.java*
  6. Click Finish
4. To be able to build the project, ensure that the library *truffle-api.jar* is imported correctly.
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

