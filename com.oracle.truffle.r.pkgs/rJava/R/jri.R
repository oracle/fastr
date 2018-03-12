## bindings into JRI

## warning: JRI REXP class has currently no finalizers! (RReleaseREXP must be used manually for now)
## warning: this produces JRI-API pbjects - that should go away! use toJava below
.r2j <- function(x, engine = NULL, convert = TRUE) {
  if (is.null(engine)) engine <- .jcall("org/rosuda/JRI/Rengine","Lorg/rosuda/JRI/Rengine;","getMainEngine")
  if (!is(engine, "jobjRef")) stop("invalid or non-existent engine")
  new("jobjRef",jobj=.Call(PushToREXP,"org/rosuda/JRI/REXP",engine@jobj,engine@jclass,x,convert),jclass="org/rosuda/JRI/REXP")
}

toJava <- function(x, engine = NULL) {
  ## this is really the wrong place for all this REngine checking stuff, but so far .jengine uses JRI API only and legacy code may rely on that
  ## so this is the only place that assumes REngine API and thus will load it ...
  ec <- .jfindClass("org.rosuda.JRI.Rengine", silent=TRUE)
  if (is.jnull(ec)) {
    .jcheck(TRUE)
    stop("JRI is not loaded. Please start JRI first - see ?.jengine")
  }
  ec <- .jfindClass("org.rosuda.REngine.REngine", silent=TRUE)
  if (is.jnull(ec)) {
    .jcheck(TRUE)
    fn <- system.file("jri","REngine.jar",package="rJava")
    if (nzchar(fn)) .jaddClassPath(fn)
    fn <- system.file("jri","JRIEngine.jar",package="rJava")
    if (nzchar(fn)) .jaddClassPath(fn)
    ec <- .jfindClass("org.rosuda.REngine.REngine", silent=TRUE)
    if (is.jnull(ec)) {
      .jcheck(TRUE)
      stop("Cannot find REngine API classes. Please make sure you have installed and loaded the REngine API")
    }
  }
  if (is.null(engine)) engine <- .jcall("org/rosuda/REngine/REngine","Lorg/rosuda/REngine/REngine;","getLastEngine")
  if (is.jnull(engine)) { # no last engine, but there may be JRI engine already running ...
    me <- .jcall("org/rosuda/JRI/Rengine","Lorg/rosuda/JRI/Rengine;","getMainEngine", check=FALSE)
    .jcheck(TRUE)
    if (is.jnull(me)) stop("JRI is not running. Please start JRI first - see ?.jengine")
    engine <- .jnew("org/rosuda/REngine/JRI/JRIEngine", me)
    .jcheck(TRUE)
  }
  .jcheck(TRUE)
  if (!is(engine, "jobjRef")) stop("invalid or non-existent engine")
  new("jobjRef",jobj=.Call(PushToREXP,"org/rosuda/REngine/REXPReference",engine@jobj,"org/rosuda/REngine/REngine",x,NULL),jclass="org/rosuda/REngine/REXPReference")
}

.setupJRI <- function(new=TRUE) {
  ec <- .jfindClass("org.rosuda.JRI.Rengine", silent=TRUE)
  if (is.jnull(ec)) {
    .jcheck(TRUE)
    .jaddClassPath(system.file("jri","JRI.jar",package="rJava"))
    ec <- .jfindClass("org.rosuda.JRI.Rengine", silent=TRUE)
    .jcheck(TRUE)
    if (is.jnull(ec))
      stop("Cannot find JRI classes")
  }
  me <- .jcall("org/rosuda/JRI/Rengine","Lorg/rosuda/JRI/Rengine;","getMainEngine", check=FALSE)
  .jcheck(TRUE)
  if (!is.jnull(me)) {
    if (!new) return(TRUE)
    warning("JRI engine is already running.")
    return(FALSE)
  }
  e <- .jnew("org/rosuda/JRI/Rengine")
  !is.jnull(e)
}

.jengine <- function(start=FALSE, silent=FALSE) {
  me <- NULL
  ec <- .jfindClass("org.rosuda.JRI.Rengine", silent=TRUE)
  .jcheck(TRUE)
  if (!is.jnull(ec)) {
    me <- .jcall("org/rosuda/JRI/Rengine","Lorg/rosuda/JRI/Rengine;","getMainEngine", check=FALSE)
    .jcheck(TRUE)
  }
  if (is.jnull(me)) {
    if (!start) {
      if (silent) return(NULL)
      stop("JRI engine is not running.")
    }
    .setupJRI(FALSE)
    me <- .jcall("org/rosuda/JRI/Rengine","Lorg/rosuda/JRI/Rengine;","getMainEngine", check=FALSE)
    .jcheck(TRUE)
  }
  if (is.jnull(me) && !silent)
    stop("JRI engine is not running.")
  me
}
