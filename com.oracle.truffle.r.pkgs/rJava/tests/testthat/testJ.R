# prerequisites:
# - 'testthat' package has to be installed: install.packages("testthat")
# - FastR`s rJava package has to be installed: bin/r CMD INSTALL com.oracle.truffle.r.pkgs/rjava

library(testthat)
library(rJava)

.jaddClassPath(paste0(Sys.getenv("R_HOME"), "/mxbuild/dists/fastr-unit-tests.jar"))

testName <- "test J function"
test_that(testName, {
    cat(paste0(testName, "\n"))
    tc <- J("com.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass")
    expect_equal(2147483647L, tc$fieldStaticInteger)

    tc <- J("com/oracle/truffle/r/test/library/fastr/TestJavaInterop$TestClass")
    expect_equal(2147483647L, tc$fieldStaticInteger)

    expect_equal(2147483647L, J("com.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass", "methodStaticInteger"))
    expect_equal(2147483647L, J("com/oracle/truffle/r/test/library/fastr/TestJavaInterop$TestClass", "methodStaticInteger"))
})
