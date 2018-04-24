## methods for jobjRef class
##
## additional methods ($ and $<-) are defined in reflection.R

# show method
# FIXME: this should show the class of the object instead of Java-Object
setMethod("show", c(object="jobjRef"), function(object) {
  if (is.jnull(object)) show("Java-Object<null>") else show(paste("Java-Object{", .jstrVal(object), "}", sep=''))
  invisible(NULL)
})

setMethod("show", c(object="jarrayRef"), function(object) {
  show(paste("Java-Array-Object",object@jsig,":", .jstrVal(object), sep=''))
  invisible(NULL)
})

# map R comparison operators to .jequals
setMethod("==", c(e1="jobjRef",e2="jobjRef"), function(e1,e2) .jequals(e1,e2))
setMethod("==", c(e1="jobjRef"), function(e1,e2) .jequals(e1,e2))
setMethod("==", c(e2="jobjRef"), function(e1,e2) .jequals(e1,e2))

setMethod("!=", c(e1="jobjRef",e2="jobjRef"), function(e1,e2) !.jequals(e1,e2))
setMethod("!=", c(e1="jobjRef"), function(e1,e2) !.jequals(e1,e2))
setMethod("!=", c(e2="jobjRef"), function(e1,e2) !.jequals(e1,e2))

# other operators such as <,> are defined in comparison.R

