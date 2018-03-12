## This file is part of the rJava package - low-level R/Java interface
## (C)2006 Simon Urbanek <simon.urbanek@r-project.org>
## For license terms see DESCRIPTION and/or LICENSE
##
## $Id$

.check.JVM <- function() 
    .Call(RJava_checkJVM)
.need.init <- function()
    .Call(RJava_needs_init)

## initialization
.jinit <- function(classpath=NULL, parameters=getOption("java.parameters"), ..., silent=FALSE, force.init=FALSE) {
  running.classpath <- character()
  if (!.need.init()) {
    running.classpath <- .jclassPath()
    if (!force.init) {
      if (length(classpath)) {
        cpc <- unique(unlist(strsplit(classpath, .Platform$path.sep)))
        if (length(cpc)) .jaddClassPath(cpc)
      }
      return(0)
    }
  }

  ## determine path separator
  path.sep <- .Platform$path.sep

  if (!is.null(classpath)) {
    classpath <- as.character(classpath)
    if (length(classpath))
      classpath <- paste(classpath,collapse=path.sep)
  }
  
  # merge CLASSPATH environment variable if present
  cp<-Sys.getenv("CLASSPATH")
  if (!is.null(cp)) {
    if (is.null(classpath))
      classpath<-cp
    else
      classpath<-paste(classpath,cp,sep=path.sep)
  }
  
  # set rJava/java/boot for boostrap (so we can get RJavaClassLoader)
  boot.classpath <- file.path(.rJava.base.path,"java","boot")

  # if running in a sub-arch, append -Dr.arch in case someone gets the idea to start JRI
  if (is.character(.Platform$r_arch) && nzchar(.Platform$r_arch) && length(grep("-Dr.arch", parameters, fixed=TRUE)) == 0L)
    parameters <- c(paste("-Dr.arch=/", .Platform$r_arch, sep=''), as.character(parameters))

  ## unfortunately Sys/setlocale()/Sys.getlocale() have incompatible interfaces so there
  ## is no good way to get/set locales -- so we have to hack around it ...
  locale.list <- c("LC_COLLATE", "LC_CTYPE", "LC_MONETARY", "LC_NUMERIC", "LC_TIME", "LC_MESSAGES", "LC_PAPER", "LC_MEASUREMENT")
  locales <- sapply(locale.list, Sys.getlocale)
  loc.sig <- Sys.getlocale()

  #cat(">> init CLASSPATH =",classpath,"\n")
  #cat(">> boot class path: ", boot.classpath,"\n")
  # call the corresponding C routine to initialize JVM
  xr <- .External(RinitJVM, boot.classpath, parameters)

  ## we have to re-set the locales right away
  suppressWarnings(try(if (!identical(Sys.getlocale(), loc.sig)) for (i in names(locales)) try(Sys.setlocale(i, locales[i]), silent=TRUE),
      silent=TRUE))

  if (xr==-1) stop("Unable to initialize JVM.")
  if (xr==-2) stop("Another VM is already running and rJava was unable to attach to that VM.")
  # we'll handle xr==1 later because we need fully initialized rJava for that

  # this should remove any lingering .jclass objects from the global env
  # left there by previous versions of rJava
  pj <- grep("^\\.jclass",ls(1,all.names=TRUE),value=TRUE)
  if (length(pj)>0) { 
    rm(list=pj,pos=1)
    if (exists(".jniInitialized",1)) rm(list=".jniInitialized",pos=1)
    if (!silent) warning("rJava found hidden Java objects in your workspace. Internal objects from previous versions of rJava were deleted. Please note that Java objects cannot be saved in the workspace.")
  }

  ##--- HACK-WARNING: we're operating directly on the namespace environment
  ##                  this could be dangerous.
  for (x in .delayed.variables) unlockBinding(x, .env)
  assign(".jniInitialized", TRUE, .env)
  # get cached class objects for reflection
  assign(".jclassObject", .jcall("java/lang/Class","Ljava/lang/Class;","forName","java.lang.Object"), .env)
  assign(".jclassClass", .jcall("java/lang/Class","Ljava/lang/Class;","forName","java.lang.Class"), .env)
  assign(".jclassString", .jcall("java/lang/Class","Ljava/lang/Class;","forName","java.lang.String"), .env)

  ic <- .jcall("java/lang/Class","Ljava/lang/Class;","forName","java.lang.Integer")
  f<-.jcall(ic,"Ljava/lang/reflect/Field;","getField", "TYPE")
  assign(".jclass.int", .jcast(.jcall(f,"Ljava/lang/Object;","get",.jcast(ic,"java/lang/Object")),"java/lang/Class"), .env)
  ic <- .jcall("java/lang/Class","Ljava/lang/Class;","forName","java.lang.Double")
  f<-.jcall(ic,"Ljava/lang/reflect/Field;","getField", "TYPE")
  assign(".jclass.double", .jcast(.jcall(f,"Ljava/lang/Object;","get",.jcast(ic,"java/lang/Object")),"java/lang/Class"), .env)
  ic <- .jcall("java/lang/Class","Ljava/lang/Class;","forName","java.lang.Float")
  f<-.jcall(ic,"Ljava/lang/reflect/Field;","getField", "TYPE")
  assign(".jclass.float", .jcast(.jcall(f,"Ljava/lang/Object;","get",.jcast(ic,"java/lang/Object")),"java/lang/Class"), .env)
  ic <- .jcall("java/lang/Class","Ljava/lang/Class;","forName","java.lang.Boolean")
  f<-.jcall(ic,"Ljava/lang/reflect/Field;","getField", "TYPE")
  assign(".jclass.boolean", .jcast(.jcall(f,"Ljava/lang/Object;","get",.jcast(ic,"java/lang/Object")),"java/lang/Class"), .env)
  ic <- .jcall("java/lang/Class","Ljava/lang/Class;","forName","java.lang.Void")
  f<-.jcall(ic,"Ljava/lang/reflect/Field;","getField", "TYPE")
  assign(".jclass.void", .jcast(.jcall(f,"Ljava/lang/Object;","get",.jcast(ic,"java/lang/Object")),"java/lang/Class"), .env)

  ## if NOAWT is set, set AWT to headless
  if (nzchar(Sys.getenv("NOAWT"))) .jcall("java/lang/System","S","setProperty","java.awt.headless","true")

  lib <- "libs"
  if (nchar(.Platform$r_arch)) lib <- file.path("libs", .Platform$r_arch)

  rjcl <- NULL
  if (xr==1) { # && nchar(classpath)>0) {
    # ok, so we're attached to some other JVM - now we need to make sure that
    # we can load our class loader. If we can't then we have to use our bad hack
    # to be able to squeeze our loader in

    # first, see if this is actually JRIBootstrap so we have a loader already
    rjcl <- .Call(RJava_primary_class_loader)
    if (is.null(rjcl) || .jidenticalRef(rjcl,.jzeroRef)) rjcl <- NULL
    else rjcl <- new("jobjRef", jobj=rjcl, jclass="RJavaClassLoader")
    if (is.jnull(rjcl))
      rjcl <- .jnew("RJavaClassLoader", .rJava.base.path,
                                      file.path(.rJava.base.path, lib), check=FALSE)
    .jcheck(silent=TRUE)
    if (is.jnull(rjcl)) {
      ## it's a hack, so we run it in try(..) in case BadThings(TM) happen ...
      cpr <- try(.jmergeClassPath(boot.classpath), silent=TRUE)
      if (inherits(cpr, "try-error")) {
        .jcheck(silent=TRUE)
        if (!silent) warning("Another VM is running already and the VM did not allow me to append paths to the class path.")
        assign(".jinit.merge.error", cpr, .env)
      }
      if (length(parameters)>0 && any(parameters!=getOption("java.parameters")) && !silent)
        warning("Cannot set VM parameters, because VM is running already.")
    }
  }

  if (is.jnull(rjcl))
    rjcl <- .jnew("RJavaClassLoader", .rJava.base.path,
                  file.path(.rJava.base.path, lib), check=FALSE )

  if (!is.jnull(rjcl)) {
    ## init class loader
    assign(".rJava.class.loader", rjcl, .env)

    ##-- set the class for native code
    .Call(RJava_set_class_loader, .env$.rJava.class.loader@jobj)

    ## now it's time to add any additional class paths
    cpc <- unique(strsplit(classpath, .Platform$path.sep)[[1]])
    if (length(cpc)) .jaddClassPath(cpc)
  } else stop("Unable to create a Java class loader.")
  
  ##.Call(RJava_new_class_loader, .rJava.base.path, file.path(.rJava.base.path, lib))

  ## lock namespace bindings
  for (x in .delayed.variables) lockBinding(x, .env)
  
  ## now we need to update the attached namespace (package env)  as well
  m <- match(paste("package", getNamespaceName(.env), sep = ":"), search())[1]
  if (!is.na(m)) { ## only is it is attached
    pe <- as.environment(m)
    for (x in .delayed.export.variables) {
      unlockBinding(x, pe)
      pe[[x]] <- .env[[x]]
      lockBinding(x, pe)
    }
  }
  
  # FIXME: is this the best place or should this be done 
  #        internally right after the RJavaClassLoader is instanciated
  # init the cached RJavaTools class in the jni side
  .Call( "initRJavaTools", PACKAGE = "rJava" ) 
  
  # not yet
  # import( c( "java.lang", "java.util") )
  
  invisible(xr)
}

# FIXME: this is not always true: osgi, eclipse etc use a different
#        class loader strategy, we should add some sort of hook to let people
#        define how they want this to be done
.jmergeClassPath <- function(cp) {
  ccp <- .jcall("java/lang/System","S","getProperty","java.class.path")
  ccpc <- strsplit(ccp, .Platform$path.sep)[[1]]
  cpc <- strsplit(cp, .Platform$path.sep)[[1]]
  rcp <- unique(cpc[!(cpc %in% ccpc)])
  if (length(rcp) > 0) {
    # the loader requires directories to include trailing slash
    # Windows: need / or \ ? (untested)
    dirs <- which(file.info(rcp)$isdir)
    for (i in dirs)
      if (substr(rcp[i],nchar(rcp[i]),nchar(rcp[i]))!=.Platform$file.sep)
        rcp[i]<-paste(rcp[i], .Platform$file.sep, sep='')

    ## this is a hack, really, that exploits the fact that the system class loader
    ## is in fact a subclass of URLClassLoader and it also subverts protection
    ## of the addURL class using reflection - yes, bad hack, but we use it
    ## only if the boot class path doesn't contain our own class loader so
    ## we cannot replace the system loader with our own (this will happen when we
    ## need to attach to an existing VM)
    ## The original discussion and code for this hack was at:
    ## http://forum.java.sun.com/thread.jspa?threadID=300557&start=15&tstart=0

    ## it should probably be run in try(..) because chances are that it will
    ## break if Sun changes something...
    cl <- .jcall("java/lang/ClassLoader", "Ljava/lang/ClassLoader;", "getSystemClassLoader")
    urlc <- .jcall("java/lang/Class", "Ljava/lang/Class;", "forName", "java.net.URL")
    clc <- .jcall("java/lang/Class", "Ljava/lang/Class;", "forName", "java.net.URLClassLoader")
    ar <- .jcall("java/lang/reflect/Array", "Ljava/lang/Object;",
                         "newInstance", .jclassClass, 1:1)
    .jcall("java/lang/reflect/Array", "V", "set",
                  .jcast(ar, "java/lang/Object"), 0:0,
                  .jcast(urlc, "java/lang/Object"))
    m<-.jcall(clc, "Ljava/lang/reflect/Method;", "getDeclaredMethod", "addURL", .jcast(ar,"[Ljava/lang/Class;"))
    .jcall(m, "V", "setAccessible", TRUE)

    ar <- .jcall("java/lang/reflect/Array", "Ljava/lang/Object;",
                 "newInstance", .jclassObject, 1:1)
    
    for (fn in rcp) {
      f <- .jnew("java/io/File", fn)
      url <- .jcall(f, "Ljava/net/URL;", "toURL")
      .jcall("java/lang/reflect/Array", "V", "set",
             .jcast(ar, "java/lang/Object"), 0:0,
             .jcast(url, "java/lang/Object"))
      .jcall(m, "Ljava/lang/Object;", "invoke",
             .jcast(cl, "java/lang/Object"), .jcast(ar, "[Ljava/lang/Object;"))
    }

    # also adjust the java.class.path property to not confuse others
    if (length(ccp)>1 || (length(ccp)==1 && nchar(ccp[1])>0))
      rcp <- c(ccp, rcp)
    acp <- paste(rcp, collapse=.Platform$path.sep)
    .jcall("java/lang/System","S","setProperty","java.class.path",as.character(acp))
  } # if #rcp>0
  invisible(.jcall("java/lang/System","S","getProperty","java.class.path"))
}
