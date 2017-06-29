# prerequisites:
# - 'testthat' package has to be installed: install.packages("testthat")
# - FastR`s rJava package has to be installed: bin/r CMD INSTALL com.oracle.truffle.r.pkgs/rjava
# - mxbuild/dists/fastr-unit-tests.jar has to be on FastR classpath

library(testthat)
library(rJava)

testName <- "test .jarray"
test_that(testName, {
    cat(paste0(testName, "\n"))
	
    a <- .jarray(c(1.1, 2.1, 3.1))
    expect_true(is.external.array(a))
    expect_equal(length(a), 3)
    expect_equal(a[1], c(1.1))
    expect_equal(a[2], c(2.1))
    expect_equal(a[3], c(3.1))

    a <- .jarray(c(1L, 2L, 3L))
    expect_true(is.external.array(a))
    expect_equal(length(a), 3)
    expect_equal(a[1], c(1))
    expect_equal(a[2], c(2))
    expect_equal(a[3], c(3))

    a <- .jarray(c(TRUE, FALSE))
    expect_true(is.external.array(a))
    expect_equal(length(a), 2)
    expect_equal(a[1], TRUE)
    expect_equal(a[2], FALSE)

    a <- .jarray(c(.jbyte(1), .jchar("a"), .jfloat(1.1), .jlong(2), .jshort(123)))
    expect_true(is.external.array(a))
    expect_equal(length(a), 5)
    expect_equal(a[1], 1)
    expect_equal(a[2], "a")
    expect_true((a[3] - 1.1)^2 < 1e-8)
    expect_equal(a[4], 2)
    expect_equal(a[5], 123)

    to <- .jnew('java.util.ArrayList')
    a <- .jarray(to)
    expect_true(is.external.array(a))
    expect_equal(length(a), 1)
    expect_equal(a[1], to)

    to <- .jnew('java.util.ArrayList')
    a <- .jarray(c(to, to))
    expect_true(is.external.array(a))
    # fails at the moment    
    # expect_equal(length(a), 2)
    # expect_equal(a[1], to)
    # expect_equal(a[2], to)

    a <- .jarray(list(1, 2, 3))
    expect_true(is.external.array(a))
    expect_equal(length(a), 3)
    expect_equal(a[1], 1)
    expect_equal(a[2], 2)
    expect_equal(a[3], 3)
})

testName <- "test .jevalArray"
test_that(testName, {
    cat(paste0(testName, "\n"))

    expectedValues <- list(
        Boolean=list("logical", TRUE, FALSE, TRUE), 
        Byte=list("integer", 1,2,3), 
        Char=list("character", "a", "b", "c"), 
        Double=list("double",1.1, 2.1, 3.1), 
        Float=list("double", 1.1, 2.1, 3.1), 
        Integer=list("integer",1,2,3), 
        Long=list("double", 1,2,3), 
        Short=list("integer",1,2,3), 
        String=list("character", "a", "b", "c"))
    testClassName <- "com.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass"
    t<-.jnew(class=testClassName)

    for(expectedName in names(expectedValues)) {                
        fieldName <- paste0("fieldStatic", expectedName, "Array")
        ev<-expectedValues[expectedName][[1]]
        arrayType <- ev[[1]]
        arrayLength <- length(ev) - 1
        a<-t[fieldName]
        expect_true(is.external.array(a), info=paste0("the array was returned for ", fieldName), label="is.external.array")
        ae<-.jevalArray(a)
        expect_true(is.vector(ae), info=paste0("the array was returned for ", fieldName), label="is.vector")
        expect_equal(typeof(ae), arrayType, info=paste0("the array was returned for ", fieldName), label="typeof")
        expect_equal(length(ae), arrayLength, info=paste0("the array was returned for ", fieldName), label="array length")
        for(i in 1:arrayLength) {
            if(expectedName != "Float") {
                expect_equal(a[i], ev[[i+1]], info=paste0("the array was returned for ", fieldName), label=paste0("value at", i))
            } else {
                expect_true((ev[[i+1]] - a[i])^2 < 1e-8, info=paste0("the array was returned for ", fieldName), label=paste0("value at", i))
            }
        }
    }

})