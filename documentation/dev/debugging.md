# Debugging
By default, `mx -d r` starts a normal Java waiting for a debugger to be attached.
This is generally useful when hacking FastR sources.

- Evaluate `Utils.createStackTrace(false)` statement in the debugger to see the R stack trace.
- Put a breakpoint into `BrowserNode` and add `browser()` statement to the R code.
- There is a bunch of FastR-specific functions solely for debugging purposes, like `.fastr.inspect`, or `.fastr.rcallertrace`.

## R code debugging
The simplest possible way to debug R code is to use the builtin debugging facilities, like these functions:
- `trace`
  - `trace(func_to_trace, tracer=quote(print(func_arg)))`
- `debugonce`
- `browser`

When the return value of a specific statement is different in FastR and in GNU-R, it is most convenient to run both FastR and GNU-R alongside each other and inject, e.g., a `browser` statement on the same place on both engines, and step over statements simultaneously.
Note that the output generated during debugging (like current source and line) may not be the same on GNU-R and in FastR.

## Native code debugging
- In LLVM mode (`mx r --R.BackEnd=llvm`), LLVM bytecode is interpreted, and each `LLVMFunctionStartNode` in the stack trace corresponds to a function call.
- There is no equivalent to `.fastr.inspect` in native code.
  We can, however use some not frequently used upcall, like `ALTREP`, and put a breakpoint into this upcall.
  Possibly, you can add calls to `Rprintf` where appropriate.
  This method has the disadvantage that you have to rebuild the native package every time you modify its sources, as R will not warn you when the binary is out of date.

### Debugging with GDB
Debugging with GDB is mostly useful for debugging a package with native code.
Before we can do that, we should compile the native sources of the package with debug symbols, and conveniently without any optimizations.
This can be done, for example, by defining user specified Makevars file, like this:
```
echo "CFLAGS = -ggdb -O0" > ~/.R/Makevars
echo "CXXFLAGS = -ggdb -O0" >> ~/.R/Makevars
```

This command attaches GDB to a running FastR process, assuming there is just one FastR process running on the machine:
```
gdb -p `jps | grep RMain | cut -d " " -f 1`
```
Once GDB is started, we should immediately run `handle SIGSEGV nostop`, because JVM uses SIGSEGV for its internal stuff.
This is a short list of GDB commands that are useful:
```
info sources  # List all loaded sources to check we have what we want to debug
signal 0  # Continue execution of the JVM (CTRL^C in JVM to break into GDB)
directory /path/to/sources
break sourceFile.c:100
break functionName
info break
delete [n]  # Delete n-th breakpoint
p variableName
p *arrayName@length
```

