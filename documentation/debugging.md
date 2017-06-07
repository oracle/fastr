# Introduction

Since FastR is implemented atop the Truffle framework, it supports debugging of applications using NetBeans.
This is a very convenient and more powerful way for debugging compared to R's browser function.
However, FastR also implements the internal debugging functions `browse`, `trace`, and so on.
Unfortunately, FastR cannot support the RStudio debugger since it depends on GnuR's implementation.

This section describes how to debug R applications using NetBeans.

# Pre-Requisites

* A recent NetBeans IDE (e.g. nightly build from http://bits.netbeans.org/download/trunk/nightly/latest/zip/).
* The _Truffle Debugging Support_ Plugin for NetBeans.  
  Install this by starting NetBeans and selecting `Tools > Plugins > Available Plugins > Truffle Debugging Support`

# Using the NetBeans Debugger

1. Start NetBeans and install a breakpoint.  
   Unfortunately, NetBeans does currently not have a suitable R source code editor.  
   But you can define a breakpoint as you would do for JavaScript.  
   Open the _Breakpoints_ view: `Window > Debugging > Breakpoints`  
   Then, create a new breakpoint.  
   Specify `Debugger: JavaScript` and `Breakpoint Type: Line`.  
   Enter the path to the R source file in field `File` and the line number for the breakpoint.  

2. Start FastR in debugging mode such that a remote debugger can attach.  
`bin/R --J @-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000`

3. Run your application.  
   There are many ways to run your R application. The simplest one is using `source("file.R")`.

# Additional Features

The NetBeans debugger is source-location-based meaning that it assumes every source of an application resides somewhere in a source file.  
However, R applications often run deserialized code or the code is created in some other way.

In order to be still able to use the NetBeans debugger for such source, you first need to generate a temporary source file using a FastR builtin function.

For example, assume we want to debug function `print`, first run following command:  
	`.fastr.srcinfo(print)`

It will output something similar to  
	`[1] "/tmp/deparse/print-fca37561e0.r#1"`  
which is the file containing the deparsed source code of `print`.  
The generated source file is now associated with function print and a breakpoint can be installed.

# Disclaimer

Since FastR is still in its development phase, debugging may not be possible in all cases.

