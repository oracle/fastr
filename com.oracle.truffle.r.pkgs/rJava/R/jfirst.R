# this part is common to all platforms and must be invoked
# from .First.lib after library.dynam

# actual namespace environment of this package
.env <- environment()

# variables in the rJava environment that will be initialized *after* the package is loaded
# they need to be pre-created at load time and populated later by .jinit
.delayed.export.variables <- c(".jniInitialized", ".jclassObject", ".jclassString", ".jclassClass",
                               ".jclass.int", ".jclass.double", ".jclass.float", ".jclass.boolean",
                               ".jclass.void", ".jinit.merge.error")
# variables that are delayed but not exported are added here
.delayed.variables <- c(.delayed.export.variables, ".rJava.class.loader")

# C entry points to register
.register.addr <- c( # .Call
                    "PushToREXP", "RJava_checkJVM", "RJava_needs_init", "RJava_new_class_loader",
                    "RJava_primary_class_loader", "RJava_set_class_loader", "RJava_set_memprof", "RJavaCheckExceptions",
                    "RcreateArray", "RgetBoolArrayCont", "RgetByteArrayCont", "RgetCharArrayCont",
                    "RgetDoubleArrayCont", "RgetField", "RgetFloatArrayCont", "RgetIntArrayCont",
                    "RgetLongArrayCont", "RgetNullReference", "RgetObjectArrayCont",
                    "RgetShortArrayCont", "RgetStringArrayCont", "RidenticalRef",
                    "RisAssignableFrom", "RpollException", "RsetField", "RthrowException",
                    "javaObjectCache",
                     # .External
                    "RcreateObject", "RgetStringValue", "RinitJVM", "RtoString",
                     # .C
                    "RclearException", "RuseJNICache"
                    )

.jfirst <- function(libname, pkgname) {
  # register all C entry points
  addr <- getNativeSymbolInfo(.register.addr, pkgname)
  for (name in .register.addr)
     .env[[name]] <- addr[[name]]$address

  assign(".rJava.base.path", paste(libname, pkgname, sep=.Platform$file.sep), .env)
  assign(".jzeroRef", .Call(RgetNullReference), .env)

  for (x in .delayed.variables) assign(x, NULL, .env)
  assign(".jniInitialized", FALSE, .env)

  # default JVM initialization parameters
  if (is.null(getOption("java.parameters")))
    options("java.parameters"="-Xmx512m")
  
  ## S4 classes update - all classes are created earlier in classes.R, but jobjRef's prototype is only valid after the dylib is loaded
  setClass("jobjRef", representation(jobj="externalptr", jclass="character"), prototype=list(jobj=.jzeroRef, jclass="java/lang/Object"), where=.env)  
}
