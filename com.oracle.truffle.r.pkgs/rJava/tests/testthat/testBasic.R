# prerequisites:
# - 'testthat' package has to be installed: install.packages("testthat")
# - FastR`s rJava package has to be installed: bin/r CMD INSTALL com.oracle.truffle.r.pkgs/rjava
# - mxbuild/dists/fastr-unit-tests.jar has to be on FastR classpath

library(testthat)
library(rJava)

bo <- TRUE
bt <- 123
ch <- 'a'
d  <- 1.123456
f  <- 1.123
i  <- 12345L
l  <- 123456
sh <- 1234
st <- "a test string"

jbt <- .jbyte(bt)
jch <- .jchar(ch)
jf  <- .jfloat(f)
jl  <- .jlong(l)
jsh <- .jshort(sh)

primitiveTypeNames <- c("Boolean", "Byte", "Char", "Double", "Float", "Integer", "Long", "Short")
expectedValues <- list(bo, bt, ch, d, f, i, l, sh, st)

testClassName <- "com.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass"
t<-.jnew(class=testClassName, bo, jbt, jch, d, jf, i, jl, jsh, st)

testForMember <- function(valueProvider, memberNameProvider, typeNames) {
	for(idx in 1:length(typeNames)) {	
		typeName <- typeNames[idx]
		member <- memberNameProvider(typeName)
		expectedValue <- expectedValues[[idx]]
		value <- valueProvider(t, member)
		testValue(member, typeName, expectedValue, value)
	}
}

testValue <- function(memberName, typeName, expectedValue, value) {
	cat(paste0("  ", memberName, " returned value [", value, "] and is expected to be [", expectedValue, "]"), "\n")
	if(typeName != "Float") {
		expect_that(expectedValue, equals(value), info=memberName, label=memberName)
	} else {
		expect_true((expectedValue - value)^2 < 1e-8, info=memberName, label=memberName)
	}
}

testName <- "test if primitive field access works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	testForMember( valueProvider = function(t, member) { .jfield(t, , member) },
                   memberNameProvider = function(typeName) { paste0("field", typeName) },
                   primitiveTypeNames )
})

testName <- "test if primitive static field access works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	testForMember( valueProvider = function(t, member) { .jfield(t, , member) },
                   memberNameProvider = function(typeName) { paste0("fieldStatic", typeName) },
                   primitiveTypeNames )
})

testName <- "test if static field access works"
test_that("test if static field access works", {
	testForMember( valueProvider = function(t, member) { .jfield(t, , member) },
                   memberNameProvider = function(typeName) { paste0("fieldStatic", typeName, "Object") },
                   c(primitiveTypeNames, "String") )
})

testName <- "test if field access works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	testForMember( valueProvider = function(t, member) { .jfield(t, , member) },
                   memberNameProvider = function(typeName) { paste0("field", typeName, "Object")},
                   c(primitiveTypeNames, "String") )
})

testName <- "test if calling static method returning primitive values works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	testForMember( valueProvider = function(t, member) { .jcall(t, , member) },
                   memberNameProvider = function(typeName) { paste0("methodStatic", typeName) },
                   primitiveTypeNames )
})

testName <- "test if calling method returning primitive values works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	testForMember( valueProvider = function(t, member) { .jcall(t, , member) },
                   memberNameProvider = function(typeName) { paste0("method", typeName) },
                   primitiveTypeNames )
})

testName <- "test if calling static method returning object values works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	testForMember( valueProvider = function(t, member) { .jcall(t, , member) },
                   memberNameProvider = function(typeName) { paste0("methodStatic", typeName, "Object") },
                   c(primitiveTypeNames, "String") )
})

testName <- "test if calling method returning object values works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	testForMember( valueProvider = function(t, member) { .jcall(t, , member) },
                   memberNameProvider = function(typeName) { paste0("method", typeName, "Object") },
                   c(primitiveTypeNames, "String") )
})

testName <- "test if static access via class name works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	value <- .jfield(testClassName, , "fieldStaticInteger")
	testValue("fieldStaticInteger", "Integer", i, value)

	value <- .jcall(testClassName, , "methodStaticInteger")
	testValue("methodStaticInteger", "Integer", i, value)
})

testName <- "test if calling method with all primitive type parameters + string works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	value <- .jcall(t, , "allTypesMethod", bo, jbt, jch, d, jf, i, jl, jsh, st)
	expect_false(is.null(value))
})

testName <- "test if calling method with all primitive type parameters + string works"
test_that(testName, {
	cat(paste0(testName, "\n"))
	value <- .jcall(t, , "allTypesStaticMethod", bo, jbt, jch, d, jf, i, jl, jsh, st)
	expect_false(is.null(value))
})