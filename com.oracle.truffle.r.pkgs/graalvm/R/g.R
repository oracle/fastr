##
 # Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 # DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 #
 # This code is free software; you can redistribute it and/or modify it
 # under the terms of the GNU General Public License version 2 only, as
 # published by the Free Software Foundation.
 #
 # This code is distributed in the hope that it will be useful, but WITHOUT
 # ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 # FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 # version 2 for more details (a copy is included in the LICENSE file that
 # accompanied this code).
 #
 # You should have received a copy of the GNU General Public License version
 # 2 along with this work; if not, write to the Free Software Foundation,
 # Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 #
 # Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 # or visit www.oracle.com if you need additional information or have any
 # questions.
##

graalvmEnv <- new.env(parent = emptyenv())
graalvmEnv$status <- FALSE

graalvm.status <- function() {
	graalvmEnv$status
}

#' Set up the GraalVM agent
#'
#' @param home The home folder of the GraalVM installation
#' @param host The local host name at which the GraalVM agent is listening
#' @param port The port at which the GraalVM agent is listening
#' @param rlibs The value of the FastR R_LIBS environmental variable. The default 
#' value is calculated as paste0(graalvm.home, "/language/R/library").
#' @param javaOpts a character vector of Java options
#' @examples
#' graalvm.setup("~/work/graalvm-0.21")
#' # Running GraalVM in debug mode
#' graalvm.setup("~/work/graalvm-0.21", javaOpts = c("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=y"))
#' @export
graalvm.setup <- function(home, host = "localhost", port = 9876, rlibs = paste0(home, "/language/R/library"), 
							javaOpts = character(0)) {
	options(graalvm.home = home, graalvm.host = host, graalvm.port = port, graalvm.rlibs = rlibs,
	graalvm.javaOpts = paste(sapply(javaOpts, function(opt) { paste0("-J'", opt, "'")  }), collapse = " "))
}

commandURL <- function(cmd) {
	gHost <- getOption("graalvm.host");
	gPort <- getOption("graalvm.port");
	sprintf("http://%s:%d/%s", gHost, gPort, cmd)
}

ping <- function() {
	pingURL <- commandURL("ping")
	tryCatch({ suppressWarnings(readLines(pingURL, warn=FALSE)); TRUE }, error = function(e) FALSE)
}

#' Start the GraalVM agent. The agent is normally started automatically upon the first
#' code ecxecution.
#' @export
graalvm.start <- function() {
	if (!ping()) {
		graalvmEnv$status <- FALSE
		gHome <- getOption("graalvm.home");
		if (is.null(gHome)) stop("No GraalVM home configured. Use graalvm.setup() to specify it.")

		serverScriptPath <- attr(packageDescription("graalvm"), "file")
		serverScriptPath <-	substr(serverScriptPath, 1, nchar(serverScriptPath)-16)
		serverScriptPath <- paste0(serverScriptPath, "data/server.js")
	
		libEnvVar <- paste0("R_LIBS=", getOption("graalvm.rlibs"))
	
		gHost <- getOption("graalvm.host");
		gPort <- getOption("graalvm.port");
		javaOpts <- getOption("graalvm.javaOpts");
	
		nodeLaunchCmd <- paste0(libEnvVar, " ", gHome, "/bin/node ", javaOpts, " ", serverScriptPath, " ", gHost, " ", gPort, " &")
		system(nodeLaunchCmd, ignore.stdout = TRUE, ignore.stderr = TRUE)
	
		attempts <- 0L
		while(!ping()) {
			Sys.sleep(1)
			attempts <- attempts + 1L
			if (attempts >= 30) stop("Cannot launch GraalVM agent")
		}

		graalvmEnv$status <- TRUE
	} 
	
	# register the function stopping the agent on exit
	prevLast <- NULL
	if (exists(".Last")) {
		prevLast <- .Last
	}
	.Last <<- function() {
		tryCatch(graalvm::graalvm.stop(), error = function(e) print(e))
		# invoke the previous callback if any
		if (!is.null(prevLast)) prevLast()
	}
	TRUE
}

#' Stop the GraalVM agent.
#' @export
graalvm.stop <- function() {
	tryCatch({ 
		suppressWarnings(readLines(commandURL("stop"), warn=FALSE))
		graalvmEnv$status <- FALSE
		TRUE
	}, error = function(e) FALSE)
}

#' Execute code by GraalVM using the language interpreter that corresponds
#' to the language mimetype.
#'
#' @param code the code to be executed. It must be a language element as long as the target
#' language is R, otherwise it must be a string.
#' @param echo controls whether this function returns the result of the interpreted code.
#' The default value is TRUE.
#' @param mimetype The mimetype of the target language. Currently supported values are
#' "application/x-r", "text/javascript" and "application/x-ruby".
#' @family execution functions
#' @examples
#' g(runif(10^3))
#' g(runif(10^8), echo = FALSE) # We do not want that the result is returned due to its size
#' g("1 < 2", mimetype = "text/javascript")
#' @export
g <- function(code, echo = TRUE, mimetype = "application/x-r") {
	if (mimetype == "application/x-r") {
		code <- deparse(substitute(code))
	} else {
		if (!is.character(code)) stop("The code argument must a character vector")
	}
	send(code, echo, mimetype)
}

#' Execute R code.
#' @examples
#' g.r("runif(10^3)")
#' @family execution functions
#' @export
g.r <- function(code, echo = TRUE) {
	if (!is.character(code)) stop("The code argument must a character vector")
	send(code, echo, "application/x-r")
}

#' Execute JavaScript code.
#' @examples
#' g.js("1 < 2")
#' @family execution functions
#' @export
g.js <- function(code, echo = TRUE) {
	if (!is.character(code)) stop("The code argument must a character vector")
	send(code, echo, "text/javascript")
}

#' Execute Ruby code.
#' @examples
#' g.rb("1 < 2")
#' @family execution functions
#' @export
g.rb <- function(code, echo = TRUE) {
	if (!is.character(code)) stop("The code argument must a character vector")
	send(code, echo, "application/x-ruby")
}

#' Assign a value to a paired variable. The value is assigned both locally and remotely.
#' The local variable must have been initialized by one of the language specific gget.* or
#' ggset.* functions.
#' 
#' @family paired variables
#' @examples
#' # Create and initialize a variable in JS
#' g.js("a = 1")
#' # Pick up the a variable from JS and define its counterpart in GNUR
#' gget.js(a)
#' a
#' # Increment the variable both locally and in JS 
#' gset(a, a + 1)
#' a
#' g.js("a[0]")
#' g.js("a[1] = 10")
#' gget(a)
#' a
#' @export
gset <- function(var, value = var) {
	varName <- deparse(substitute(var))
	if (exists(varName)) {
		meta <- attr(var, "graalvm")
		if (is.null(meta)) {
			stop(paste("Unpaired variable ", varName))
		}
		deparsedValue = NULL
		if (meta$mimetype == "application/x-r") {
			deparsedValue <- paste(deparse(substitute(value)), collapse="\n")
		}
		setVar(varName, value, deparsedValue, meta$mimetype)
	} else {
		stop(paste("Undefined variable ", varName))
	}
}

#' Assign the value to the paired variable in Graal FastR and locally.
#'
#' @family paired variables
#' @export
gset.r <- function(var, value) setVar(deparse(substitute(var)), value, paste(deparse(substitute(value)), collapse="\n"), "application/x-r")

#' Assign the value to the paired variable in Graal JS and locally.
#'
#' @family paired variables
#' @export
gset.js <- function(var, value) setVar(deparse(substitute(var)), value, NULL, "text/javascript")

#' Assign the value to the paired variable in Graal Ruby and locally.
#'
#' @family paired variables
#' @export
gset.rb <- function(var, value) setVar(deparse(substitute(var)), value, NULL, "application/x-ruby")

#' Retrieve the variable defined in a GraalVM language. The local variable must have been 
#' initialized by one of the language specific gget.* or ggset.* functions.
#' 
#' @family paired variables
#' @export
gget <- function(var) {
	varName <- deparse(substitute(var))
	if (exists(varName)) {
		meta <- attr(var, "graalvm")
		if (is.null(meta)) {
			stop(paste("Unpaired variable ", varName))
		}
		getVar(varName, meta$mimetype) 
	} else {
		stop(paste("Undefined variable ", varName))
	}
}

#' Retrieve the variable defined in GraalVM FastR.
#'
#' @family paired variables
#' @export
gget.r <- function(var) getVar(deparse(substitute(var)), "application/x-r")

#' Retrieve the variable defined in GraalVM JS.
#'
#' @family paired variables
#' @export
gget.js <- function(var) getVar(deparse(substitute(var)), "text/javascript")

#' Retrieve the variable defined in GraalVM Ruby.
#'
#' @family paired variables
#' @export
gget.rb <- function(var) getVar(deparse(substitute(var)), "application/x-ruby")

setVar <- function(varName, value, deparsedValue, mimetype="application/x-r") {
	localValue <- value
	meta <- NULL
	if (exists(varName)) {
		meta <- attr(var, "graalvm")
	} 
	if (is.null(meta)) {
		meta <- list(varName = varName, mimetype = mimetype)
		attr(localValue, "graalvm") <- meta
	}
	if (meta$mimetype == "application/x-r") {
		code <- paste0(meta$varName, "<-", deparsedValue) 
	} else if (meta$mimetype == "text/javascript") {
		code <- paste0(meta$varName, "=", toJSON(value)) 
	} else if (meta$mimetype == "application/x-ruby") {
		code <- paste0("$", meta$varName, "=", toJSON(value))
	} else {
		stop(paste("Unsupported language mimetype:", mimetype))
	}
	send(code, FALSE, meta$mimetype)
	
	assign(varName, localValue, inherits = TRUE)
}

getVar <- function(varName, mimetype="application/x-r") {
	meta <- NULL
	if (exists(varName)) {
		meta <- attr(var, "graalvm")
	} 
	if (is.null(meta)) {
		meta <- list(varName = varName, mimetype = mimetype)
	}
	if (meta$mimetype == "application/x-r") {
		code <- meta$varName 
	} else if (meta$mimetype == "text/javascript") {
		code <- meta$varName 
	} else if (meta$mimetype == "application/x-ruby") {
		code <- paste0("$", meta$varName)
	} else {
		stop(paste("Unsupported language mimetype:", mimetype))
	}
	value <- send(code, TRUE, meta$mimetype)
	
	if (!is.null(value)) {
		meta <- list("varName" = varName, "mimetype" = mimetype)
		attr(value, "graalvm") <- meta
	}
	
	assign(varName, value, inherits = TRUE)
}

send <- function(code, echo, mimetype) {
	tryCatch(sendAttempt(code, echo, mimetype), error = function(e) {
		# try to restart the agent and invoke it agains
		graalvm.start()
		sendAttempt(code, echo, mimetype)
	})	
}

sendAttempt <- function(code, echo, mimetype) {
	code <- paste(code, collapse="\n")
	if (!graalvmEnv$status) {
		graalvm.start()
	}
	h <- new_handle(failonerror = FALSE)
	handle_setform(h, code=code, echo=as.character(echo), mimetype=mimetype)
	url <- commandURL("")
	resp <- curl_fetch_memory(url, handle = h)
	respData <- rawToChar(resp$content)
	respData <- strsplit(respData, "\r\n")[[1]]
	if (mimetype == "application/x-r") {
		respObj <- eval(parse(text=respData))
	} else if (mimetype == "text/javascript") {
		respObj <- fromJSON(respData)
	} else if (mimetype == "application/x-ruby") {
		respObj <- fromJSON(respData)
	}
	
	if (resp$status_code >= 400) {
		stop(respObj)
	} else {
		respObj
	}
}
