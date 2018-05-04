## S4 classes (jobjRef is re-defined in .First.lib to contain valid jobj)                                                                                                  
#' java object reference
setClass("jobjRef", representation(jobj="externalptr", jclass="character"), 
	prototype=list(jobj=NULL, jclass="java/lang/Object"))

#' rugged arrays
setClass("jarrayRef", representation("jobjRef", jsig="character"))

#' rectangular java arrays double[][] d = new double[m][n]
setClass("jrectRef", 
	representation("jarrayRef", dimension="integer" ) ) 



# we extend array here so that we can keep dimensions
# in the helper functions below, the storage mode is 
# set when the objects are built
# TODO: maybe an initialize method is needed here
# TODO: maybe a validate method is needed here as well
setClass("jfloat", representation("array" ) )
setClass("jlong", representation("array" )  )
setClass("jbyte", representation("array" )  )
setClass("jshort", representation("array" ) )
setClass("jchar", representation("array" )  )

# there is no way to distinguish between double and float in R, so we need to mark floats specifically
.jfloat <- function(x) {
	storage.mode( x ) <- "double"
	new("jfloat", x )
}
# the same applies to long
.jlong <- function(x) {
	storage.mode( x ) <- "double"
	new("jlong", x)
}
# and byte
.jbyte <- function(x) {
	storage.mode( x ) <- "integer"
	new("jbyte", x)
}
# and short
.jshort <- function(x){
	storage.mode( x ) <- "integer"
	new("jshort", x)
}
# and char (experimental)
.jchar <- function(x){
	storage.mode( x ) <- "integer"
	new("jchar", as.integer(x))
}

