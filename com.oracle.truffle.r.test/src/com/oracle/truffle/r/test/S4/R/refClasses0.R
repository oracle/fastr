#IgnoreErrorContext
# Create a new reference class, and an instance of it
gen <- setRefClass("myRefClass", fields=list(aa="integer", bb="ANY"))
x <- gen$new(aa=123L, bb=3.4)
# Extract and set a field
x$aa
x$aa <- 456L
## Not run: 
# Generate an error on trying to set the field to a non-integer
try(x$aa <- "notAnInteger")
## End(Not run)
# Can put any value in a field of type "ANY"
x$bb <- "foo"
# One way to create a new object:
# Call the "new" method on a refObjectGenerator object
xClass <- getRefClass("myRefClass")
x <- xClass$new(aa=123L, bb=3.4)

# A second way to create a new object:
# Use the refObjectGenerator as a function
xClass <- getRefClass("myRefClass")
x <- xClass(aa=123L, bb=3.4)

# A third way to create a new object:
# Call the "new" function with the class name
x <- new("myRefClass", aa=123L, bb=3.4)

# Make reference class inheriting from the above class
gen2 <- setRefClass("myRefClass2", contains="myRefClass",
    fields=list(cc="character"))
# Create instance specifying fields (including inherited ones)
x2 <- gen2$new(aa=1L, bb=2.3, cc="foo")

# Define reference class with a method
gen3 <- setRefClass("myRefClass3", fields=list(dd="numeric"),
    methods=list(getval=function() dd,
        setval=function(value) dd <<- value))
x3 <- gen3$new(dd=1.2)
# Call methods to extract/set value of "dd" field
x3$getval()     # returns 1.2
x3$setval(3.4)
x3$getval()     # now returns 3.4
## Not run: 
# Gives error if you try to set field to incorrect class
try(x3$setval("foo"))
## End(Not run)

# Define subclass of the above class with a method calling callSuper
gen4 <- setRefClass("myRefClass4", contains="myRefClass3",
    methods=list(getval=function() 2*callSuper()))
x4 <- gen4$new(dd=100)
x4$getval() # returns 200

# Define method with an "initialize" method modifying field values
#   field "x" is set to twice the specified value, defaulting to 100
#   field "y" is set to "none" if not specified explicitly
gen5 <- setRefClass("myRefClass5", fields=list(x="numeric",y="character"),
    methods=list(initialize=function(..., x=100L)
        {x <<- 2*x; y <<- "none"; callSuper(...)}))
gen5$new()        # initializes field "x" to 200, "y" to "none"
gen5$new(x=10)    # initializes field "x" to 20, "y" to "none"
gen5$new(y="abc") # initializes field "x" to 200, "y" to "abc"

# Define a field as a function used for an active binding
gen6 <- setRefClass("myRefClass6",
    methods=list(initialize=function(...)
        {saveVal <<- 0; numSet <<- 0; callSuper(...)}),
        fields=list(saveVal="numeric", numSet="numeric", value=function(value) {
            if(missing(value)) {
                saveVal
            } else {
                numSet <<- numSet+1
                saveVal <<- value
                value
      }}))
x6 <- gen6$new()
x6$value    # returns 0
x6$numSet   # returns 0
x6$value <- 123
x6$value    # returns 123
x6$numSet   # returns 1
x6$value <- 456
x6$value    # returns 456
x6$numSet   # returns 2