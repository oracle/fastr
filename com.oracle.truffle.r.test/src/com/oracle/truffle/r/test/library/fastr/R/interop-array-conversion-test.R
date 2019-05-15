# Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

tmpDir <- tempdir()
errorsCount <- 0

checkIsVector <- function(x) {
    if(!is.vector(x)) {
        cat("Expected to be a vector but was:\n")
        print(x)
        cat("\n")
        errorsCount <<- errorsCount + 1L
    }
}

checkIsMatrix <- function(x) {
    if(!is.matrix(x)) {
        cat("Expected to be a matrix but was:\n");
        print(x); 
        cat("\n");
        errorsCount <<- errorsCount + 1L
    }
}

checkIsList <- function(x) {
    if(!is.list(x)) {
        cat("Expected to be a list but was:\n")
        print(x)
        cat("\n")
        errorsCount <<- errorsCount + 1L
    }
}

checkElements <- function(actual, expected, desc) {
    tryCatch(
        if(!all(actual == expected)) {
            cat("Different elements in : ", desc, "\n");
            cat(paste0("actual was:\n"))
            print(actual)
            cat("expected was:\n")
            print(expected)
            cat("\n")
            errorsCount <<- errorsCount + 1L
        },
        error = function(e) {
            printError(e=e, desc=paste0("Error in checkElements : ", desc), expected=expected, actual=actual)
            errorsCount <<- errorsCount + 1L
        }
    )
}

checkAllEqual <- function(actual, expected, desc) {
    tryCatch({
            allEqual <- all.equal(actual, expected)            
            if(!isTRUE(allEqual)) {
                cat(paste0("Found differences in : ", desc, "\n"))
                print(allEqual)
                cat("Expected was:\n")
                print(expected)
                cat("actuall was:\n")
                print(actual)
                cat("\n")
                errorsCount <<- errorsCount + 1L
            }
        },
        error = function(e) {
            printError(e=e, desc=paste0("Error in checkAllEqual : ", desc), expected=expected, actual=actual)
            errorsCount <<- errorsCount + 1L
        }
    )
}

checkResult <- function(fun, actual, expected, desc, ...) {    
    funName <- deparse(substitute(fun))
    tryCatch({                
            apermActual <- do.call(fun, list(actual, ...))
            apermExpected <- do.call(fun, list(expected, ...))
            if(!identical(apermActual, apermExpected)) {
                cat(paste0("Different '", funName, "' result in : ", desc, "\n "))
                cat("was\n")
                print(apermActual)
                cat("for actual:\n")
                print(actual)
                cat("and\n")
                print(apermExpected)
                cat("for expected:\n")
                print(expected)
                cat("\n")
                errorsCount <<- errorsCount + 1L
            }
        },
        error = function(e) {
            printError(e=e, desc=paste0("Error in checkResult(", funName, ", ...) : ", desc), expected=expected, actual=actual)
            errorsCount <<- errorsCount + 1L
        }
    )               
}


checkError <- function(fun, expectedMsg, desc) {
    tryCatch(fun(), error=function(e) actualMessage <<- e$message)
    if(expectedMsg != actualMessage) {
        cat("Different error message in : ", desc, "\n")
        cat(paste0("actual:\n"))
        cat(paste0(expectedMsg, "\n"))
        cat(paste0("expected:\n"))
        cat(actualMessage)
        cat("\n")
        errorsCount <<- errorsCount + 1L
    }
}

printError <- function(e, desc, actual, expected) {
    cat(paste0(desc, "\n"))        
    cat(paste0(deparse(e$call), " : ", e$message, "\n"))
    cat('expected was:\n')
    print(expected)
    cat('actual was:\n')
    print(actual)    
}

createJavaClass <- function(className, javaSource) {
    javaFileName <- paste0(tmpDir, "/", className, ".java")
    if (!file.exists(tmpDir)) {
        dir.create(tmpDir)
    }
    writeLines(javaSource, javaFileName)
    system(paste0("$JAVA_HOME/bin/javac ", javaFileName))
    java.addToClasspath(tmpDir)
}

if(any(R.version$engine == "FastR")) {

    ###############################################################
    #
    # And this is were the actual tests start ...
    #
    ###############################################################

    # First init the foreign arrays

    # new int[] {1, 2, 3}
    intArray <- new(java.type('int[]'), 3)
    intArray[1] <- 1; intArray[2] <- 2; intArray[3] <- 3

    # new byte[] {Byte.MIN_VALUE, 0, Byte.MAX_VALUE}
    byteArray <- new(java.type('byte[]'), 3)
    byteArray[1] <- java.type('java.lang.Byte')$MIN_VALUE; byteArray[2] <- 0; byteArray[3] <- java.type('java.lang.Byte')$MAX_VALUE

    # new int[] {{1, 2, 3}, {1, 2, 3}}
    int2DArray <- new(java.type('int[][]'), c(2L, 3L))
    int2DArray[1][1] <- 1; int2DArray[1][2] <- 2; int2DArray[1][3] <- 3;
    int2DArray[2][1] <- 1; int2DArray[2][2] <- 2; int2DArray[2][3] <- 3;

    # new int[] {{1, 2}, {1, 2, 3}}
    int2DNotRectArray <- new(java.type('int[][]'), 2)
    ja <- new(java.type('int[]'), 2); 
    ja[1] <- 1; ja[2] <- 2
    int2DNotRectArray[1] <- ja
    ja <- new(java.type('int[]'), 3); 
    ja[1] <- 1; ja[2] <- 2; ja[3] <- 3
    int2DNotRectArray[2] <- ja

    # new Object[] {1, 'a', '1'}
    mixedArray <- new(java.type('java.lang.Object[]'), 3)
    mixedArray[1] <- 1; mixedArray[2] <- 'a'; mixedArray[3] <- '1'

    # new Object[][] {{1, 2, 3}, {'a', 'b', 'c'}}}
    mixed2DArray <- new(java.type('java.lang.Object[][]'), c(2L, 3L))
    mixed2DArray[1][1] <-  1 ; mixed2DArray[1][2] <-  2 ; mixed2DArray[1][3] <-  3
    mixed2DArray[2][1] <- 'a'; mixed2DArray[2][2] <- 'b'; mixed2DArray[2][3] <- 'c'

    # new Object[][] {{1, 'b', 3}, {'a', 2, 'c'}}}
    intermixed2DArray <- new(java.type('java.lang.Object[][]'), c(2L, 3L))
    intermixed2DArray[1][1] <-  1 ; intermixed2DArray[1][2] <- 'b'; intermixed2DArray[1][3] <-  3
    intermixed2DArray[2][1] <- 'a'; intermixed2DArray[2][2] <-  2 ; intermixed2DArray[2][3] <- 'c'

    # new Object[][] {{1, 2}, {'a', 'b', 'c'}}}
    mixed2DNotRectArray <- new(java.type('java.lang.Object[][]'), 2)
    ja <- new(java.type('java.lang.Object[]'), 2)
    ja[1] <- 1; ja[2] <- 2
    mixed2DNotRectArray[1] <- ja
    ja <- new(java.type('java.lang.Object[]'), 3)
    ja[1] <- 'a'; ja[2] <- 'b'; ja[3] <- 'c'
    mixed2DNotRectArray[2] <- ja

    # new Object[][] {{1, 'b'}, {'a', 2, 'c'}}}
    intermixed2DNotRectArray <- new(java.type('java.lang.Object[][]'), 2)
    ja <- new(java.type('java.lang.Object[]'), 2)
    ja[1] <- 1; ja[2] <- 'b'
    intermixed2DNotRectArray[1] <- ja
    ja <- new(java.type('java.lang.Object[]'), 3)
    ja[1] <- 'a'; ja[2] <- 2; ja[3] <- 'c'
    intermixed2DNotRectArray[2] <- ja

    ###############################################################
    # IMPLICIT CONVERSIONS
    # - happen when a foreign array is passed to a builtin
    # - the array is then, according to its values and dimensions,
    # cast to a vector, matrix, array, list, or composed list
    ###############################################################

    ###############################################################
    # IMPLICIT CONVERSIONS - HOMOGENOUS ARRAY

    # when passing a homogenous one-dimensional array to a builtin 
    # it is implicitly converted to a vector of the corresponging atomic type
    # e.g. sum(int[] {1, 2, 3}) == sum(c(1, 2, 3))
    checkElements(
        actual=intArray,
        expected=1L:3L,
        desc='implicit conversion of int[] {1, 2, 3}')

    checkResult(
        fun=sum,
        actual=intArray, 
        expected=1L:3L,
        desc='implicit conversion of int[] {1, 2, 3}')

    # when passing a homogenous multi-dimensional rectangular array to a builtin
    # it is implicitly converted to a matrix of the corresponding atomic type
    # e.g aperm(int[][] {{1, 2, 3}, {1, 2, 3}}) == aperm(matrix(c(1L, 1L, 2L, 2L, 3L, 3L), c(2, 3)))
    checkElements(
        actual=int2DArray, 
        expected=matrix(c(1L, 1L, 2L, 2L, 3L, 3L), c(2, 3)), 
        desc='implicit conversion of int[][] {{1, 2, 3}, {1, 2, 3}}')

    checkResult(
        fun=aperm,
        actual=int2DArray, 
        expected=matrix(c(1L, 1L, 2L, 2L, 3L, 3L), c(2, 3)), 
        desc='implicit conversion of int[][] {{1, 2, 3}, {1, 2, 3}}')

    # a homogenous multi-dimensional non rectangular array 
    # can't be implicitly converted to a vector
    # e.g sum(int[] {{1, 2}, {1, 2, 3}}) => error
    checkError(
        fun=function() sum(int2DNotRectArray), 
        expectedMsg="A non rectangular array cannot be converted to a vector, only to a list.",
        desc="int[][] {{1, 2}, {1, 2, 3}}")

    ###############################################################
    # IMPLICIT CONVERSIONS - HETEROGENOUS ARRAY

    # when passing a heterogenous one-dimensional array to a builtin
    # is implicitly converted to a list
    # e.g. grep('a', Object[] {1, 'a', '1'}) == grep('a', list(1, 'a', '1'))
    checkResult(
        fun=grep,
        actual=mixedArray, 
        expected=list(1, 'a', '1'), 
        desc="implicit conversion of Object[] {1, 'a', '1'}",
        pattern='a')

    # when passing a heterogenous multi-dimensional array to a builtin
    # it is implicitly converted to a composed list
    # e.g. grep('a', Object[][] {{"a", "b", "c"}, {1, 2, 3}}) == grep('a', list(c(1, 2, 3), c('a', 'b', 'c')))
    expectedList <- list(c(1, 2, 3), c('a', 'b', 'c'))
    checkElements(
        actual=mixed2DArray[1],
        expected=expectedList[[1]],
        desc='implicit conversion of Object[][] {{1, 2, 3}, {"a", "b", "c"}} - first subarray')
    checkElements(
        actual=mixed2DArray[2],
        expected=expectedList[[2]],
        desc='implicit conversion of Object[][] {{1, 2, 3}, {"a", "b", "c"}} - second subarray')
    checkResult(
        fun=grep,
        actual=mixed2DArray, 
        expected=expectedList, 
        desc='implicit conversion of Object[][] {{1, 2, 3}, {"a", "b", "c"}}',
        pattern='a')

    expectedList <- list(list(1, 'b', 3), list('a', 2, 'c'))    
    checkResult(
        fun=grep,
        actual=intermixed2DArray, 
        expected=expectedList, 
        desc='implicit conversion of Object[][] {{1, 2, 3}, {"a", "b", "c"}}',
        pattern='b')    

    # when passing a heterogenous non rectangular multi-dimensional array to a builtin 
    # is implicitly converted to a composed list
    # e.g. grep('a', Object[][] {{1, 2}, {"a", "b", "c"}}) == grep('a', list(c(1, 2), c("a", "b", "c"))
    expectedList <- list(c(1, 2), c('a', 'b', 'c'))
    checkElements(
        actual=mixed2DNotRectArray[1],
        expected=expectedList[[1]],
        desc='implicit conversion of Object[][] {{1, 2}, {"a", "b", "c"}} - first subarray')
    checkElements(
        actual=mixed2DNotRectArray[2],
        expected=expectedList[[2]],
        desc='implicit conversion of Object[][] {{1, 2}, {"a", "b", "c"}} - second subarray')
    checkResult(
        fun=grep,
        actual=mixed2DNotRectArray, 
        expected=expectedList, 
        desc='implicit conversion of Object[][] {{1, 2}, {"a", "b", "c"}}',
        pattern='a')

    expectedList <- list(list(1, 'b'), list('a', 2, 'c'))
    checkResult(
        fun=grep,
        actual=intermixed2DNotRectArray, 
        expected=expectedList, 
        desc='implicit conversion of Object[][] {{1, 2, 3}, {"a", "b", "c"}}',
        pattern='b')

    ###############################################################
    # EXPLICIT CONVERSIONS TO A VECTOR
    ###############################################################

    ###############################################################
    # EXPLICIT CONVERSIONS TO A VECTOR - HOMOGENOUS ARRAY

    # homogenous one-dimensional array
    # e.g. as.vector(int[] {1, 2, 3}) == c(1, 2, 3)
    checkIsVector(as.vector(intArray))
    
    # if not given, the resulting vector type is determined automaticaly 
    # e.g. an int[] array results in an 'integer' vector
    checkAllEqual(
        actual=as.vector(intArray), 
        expected=1L:3L,
        desc='as.vector(int[] {1, 2, 3})')

    # 'mode' works as well
    checkAllEqual(
        actual=as.vector(intArray, 'character'),
        expected=c('1', '2', '3'),
        desc='as.vector(int[] {1, 2, 3}, "character")')

    # a homogenous multi-dimensional rectangular array
    # is converted into an atomic flat vector of a corresponding R type 
    # and the dimensions are ignored.
    # Elements positions in the vector is set 'by column'
    # e.g. as.vector(int[][] {{1, 2, 3}, {1, 2, 3}}) == c(1, 1, 2, 2, 3, 3)
    checkIsVector(as.vector(int2DArray))
    checkAllEqual(
        actual=as.vector(int2DArray),
        expected=c(1, 1, 2, 2, 3, 3),
        desc='as.vector(int[][] {{1, 2, 3}, {1, 2, 3}})')    

    # a homogenous multi-dimensional non rectangular array
    # can't be implicitly converted to a vector
    # e.g. as.vector(int[] {{1, 2}, {1, 2, 3}}) => error
    checkError(
        fun=function() as.vector(int2DNotRectArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='int[][] {{1, 2}, {1, 2, 3}}')

    ###############################################################
    # EXPLICIT CONVERSIONS TO A VECTOR - java byte array and as.vector vs as.raw

    # java byte arrays are by default converted to an integer vector - this can happen
    # either in scope of an explicit as.vector call or when passed to a builtin.
    # In difference to that - in case of as.raw it is obvious that 
    # the attempt is made to get the according byte value.

    checkAllEqual(
        actual=as.raw(byteArray),
        expected=as.raw(c(0x80, 0x00, 0x7f)),
        desc='as.raw(byte[] {1, 2, 3})')
    
    # and compare with     
    checkAllEqual(
        actual=as.vector(byteArray),
        expected=c(-128L, 0L, 127L),
        desc='as.vector(byte[] {1, 2, 3})')

    ###############################################################
    # EXPLICIT CONVERSIONS TO A VECTOR - HETEROGENOUS ARRAY

    # if 'mode' is not set in 'as.vector' then 
    # a heterogenous one-dimensional array is converted to a list 
    # e.g. as.vector(new Object[] {1, 'a', '1'}) == list(1, 'a', '1')
    mixedArrayAsVector <- as.vector(mixedArray)
    checkIsList(mixedArrayAsVector)
    checkAllEqual(
        actual=mixedArrayAsVector,
        expected=list(1, 'a', '1'),
        desc='as.vector(new Object[] {1, "a", "1"}))')

    # if 'mode' is not set in 'as.vector' then 
    # a heterogenous multi-dimensional array is converted to a composed list
    # e.g. as.vector(new Object[][] {{"a", "b", "c"}, {1, 2, 3}})) == list(c(1, 2, 3), c('a', 'b', 'c'))
    mixed2DArrayAsVector <- as.vector(mixed2DArray)
    checkIsList(mixed2DArrayAsVector)
    checkAllEqual(
        actual=mixed2DArrayAsVector,
        expected=list(c(1, 2, 3), c('a', 'b', 'c')),
        desc='as.vector(Object[][] {{1, 2, 3}, {"a", "b", "c"}})')

    # if 'mode' is not set in 'as.vector' then 
    # a heterogenous non rectangular multi-dimensional array is converted to a composed list
    # e.g. as.vector(Object[][] {{1, 2}, {"a", "b", "c"}}) == list(c(1, 2), c('a', 'b', 'c'))
    mixed2DNotRectArrayAsVector <- as.vector(mixed2DNotRectArray)
    checkIsList(mixed2DNotRectArrayAsVector)
    checkAllEqual(
        actual=mixed2DNotRectArrayAsVector,
        expected=list(c(1, 2), c('a', 'b', 'c')),
        desc='as.vector(Object[][] {{1, 2}, {"a", "b", "c"}}))')

    # when 'mode' set in 'as.vector' then 
    # a heterogenous array is converted as if the equivalent list was passed to as.vector(..., mode)
    # e.g. as.vector(Object [] {1, 'a', '1'}, 'character') == as.vector(list(1, 'a', '1'), 'character')
    checkAllEqual(
        actual=as.vector(mixedArray, 'character'),
        expected=as.vector(list(1, 'a', '1'), 'character'),
        desc='as.vector(Object[] {1, "a", "1"}, "character"')
    checkAllEqual(
        actual=as.vector(mixed2DArray, 'character'),
        expected=as.vector(list(c(1, 2, 3), c('a', 'b', 'c')), 'character'),
        desc='as.vector(Object[][] {{1, 2, 3}, {"a", "b", "c"}}, "character"')

    ###############################################################
    # EXPLICIT CONVERSIONS TO A VECTOR - NON ARRAY FOREIGN OBJECT
    # results in an error
    checkError(
        fun=function() as.vector(new(java.type('java.lang.Object'))), 
        expectedMsg='no method for coercing this polyglot value to a vector',
        desc='as.vector(java.lang.Object)')

    ###############################################################
    # EXPLICIT CONVERSIONS TO A LIST        
    ###############################################################

    # a one-dimensional array is converted to a plain list    
    checkAllEqual(
        actual=as.list(intArray),
        expected=list(1L, 2L, 3L),
        desc='as.list(int[] {1, 2, 3})')

    checkAllEqual(
        actual=as.list(mixedArray),
        expected=list(1, 'a', '1'),
        desc='as.list(Object[] {1, "a", "1"})')

    ###############################################################
    # a multi-dimensional array is converted to a composed list       
    checkAllEqual(
        actual=as.list(int2DArray),
        expected=list(c(1, 2, 3), c(1, 2, 3)),
        desc='as.list(int[][] {{1, 2, 3}, {1, 2, 3}})')

    checkAllEqual(
        actual=as.list(int2DNotRectArray),
        expected=list(c(1, 2), c(1, 2, 3)),
        desc='as.list(int[][] {{1, 2}, {1, 2, 3}})')

    checkAllEqual(
        actual=as.list(mixed2DArray),
        expected=list(c(1, 2, 3), c('a', 'b', 'c')),
        desc='as.list(Object[][] {{1, 2, 3}, {"a", "b", "c"}})')

    checkAllEqual(
        actual=as.list(mixed2DNotRectArray),
        expected=list(c(1, 2), c('a', 'b', 'c')),
        desc='as.list(Object[][] {{1, 2}, {"a", "b", "c"}})')

    checkAllEqual(
        actual=as.list(intermixed2DArray),
        expected=list(list(1, 'b', 3), list('a', 2, 'c')),
        desc='as.list(Object[][] {{1, "b", 3}, {"a", 2, "c"}})')

    checkAllEqual(
        actual=as.list(intermixed2DNotRectArray),
        expected=list(list(1, 'b'), list('a', 2, 'c')),
        desc='as.list(Object[][] {{1, "b"}, {"a", 2, "c"}})')

    ###############################################################
    # a foreign object can be converted to a named list, where:
    # - array keys are recursively resolved as in implicit conversions.
    # - non array keys are unboxed or simply added to the list 
    #   as what they are (polyglot values).

    javaSource <-
    'public class ClassWithArrays {
        public boolean[] b = {true, false, true};
        public int[] i = {1, 2, 3};        
    }'
    createJavaClass('ClassWithArrays', javaSource)
    foreignObject <- new('ClassWithArrays')
    checkAllEqual(
        actual=as.list(foreignObject),
        expected=list(b=c(T, F, T), i=c(1, 2, 3)),
        desc='as.list(ClassWithArrays)')

    javaSource <-
    'public class ClassWith2DimArrays {
        public boolean[][] b = {{true, false, true}, {true, false, true}};
        public int[][] i = {{1, 2, 3}, {1, 2, 3}};
    }'
    createJavaClass('ClassWith2DimArrays', javaSource)
    foreignObject <- new('ClassWith2DimArrays')
    checkAllEqual(
        actual=as.list(foreignObject),
        expected=list(b=matrix(c(T, T, F, F, T, T), 2, 3), i=matrix(c(1, 1, 2, 2, 3, 3), 2, 3)),
        desc='as.list(ClassWith2DimArrays)')

    javaSource <-
    'public class ClassWithMixedArrays {
         public boolean[][] b = {{true, false, true}, {true, false, true}};
         public int[] i = {1, 2, 3};
         public Object[][] oa = {{true, false, true}, {"a", "b", "c"}};
         public Object n = null;
    }'
    createJavaClass('ClassWithMixedArrays', javaSource)
    foreignObject <- new('ClassWithMixedArrays')
    checkAllEqual(
        actual=as.list(foreignObject),
        expected=list(b=matrix(c(T, T, F, F, T, T), 2, 3), i=c(1, 2, 3), oa=list(c(T, F, T), c('a', 'b', 'c')), n=NULL),
        desc='as.list(TestAsListClassMixed)')

    ###############################################################
    # EXPLICIT CONVERSIONS TO A MATRIX
    # array is first implicitly converted to a vector or list and 
    # then passed over to as.matrix
    ###############################################################
    checkAllEqual(
        actual=as.matrix(intArray),
        expected=as.matrix(c(1L, 2L, 3L)),
        desc='as.matrix(int[] {1, 2, 3})')

    checkAllEqual(
        actual=as.matrix(int2DArray),
        expected=as.matrix(matrix(c(1, 1, 2, 2, 3, 3), c(2,3))),
        desc='as.matrix(int[][] {{1, 2, 3}, {1, 2, 3}})')

    checkAllEqual(
        actual=as.matrix(mixedArray),
        expected=as.matrix(list(1, 'a', '1')),
        desc='as.matrix(Object[] {1, "a", "1"})')

    checkAllEqual(
        actual=as.matrix(mixed2DArray),
        expected=as.matrix(list(c(1, 2, 3), c('a', 'b', 'c'))),
        desc='as.matrix(Object[][] {{1, 2, 3}, {"a", "b", "c"}})')

    checkError(
        fun=function() as.matrix(int2DNotRectArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='as.matrix(int[][]{{1, 2}, {1, 2, 3}})')
    checkError(
        fun=function() as.matrix(mixed2DNotRectArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='as.matrix(int[][]{{1, 2}, {"a", "b", "c"}})')
    checkError(
        fun=function() as.matrix(intermixed2DArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='as.matrix(int[][]{{1, "b", 3}, {"a", 2, "c"}})')
    checkError(
        fun=function() as.matrix(intermixed2DNotRectArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='as.matrix(int[][]{{1, "b"}, {"a", 2, "c"}})')

    ###############################################################
    # EXPLICIT CONVERSIONS TO AN ARRAY
    # array is first implicitly converted to a vector or list and 
    # then passed over to as.array
    ###############################################################
    checkAllEqual(
        actual=as.array(intArray),
        expected=as.array(c(1L, 2L, 3L)),
        desc='as.array(int[] {1, 2, 3})')

    checkAllEqual(
        actual=as.array(int2DArray),
        expected=as.array(matrix(c(1, 1, 2, 2, 3, 3), c(2,3))),
        desc='as.array(int[][] {{1, 2, 3}, {1, 2, 3}})')

    checkAllEqual(
        actual=as.array(mixedArray),
        expected=as.array(list(1, 'a', '1')),
        desc='as.array(Object[] {1, "a", "1"})')

    checkAllEqual(
        actual=as.array(mixed2DArray),
        expected=as.array(list(c(1, 2, 3), c('a', 'b', 'c'))),
        desc='as.array(Object[][] {{1, 2, 3}, {"a", "b", "c"}})')

    checkError(
        fun=function() as.array(int2DNotRectArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='as.array(int[][]{{1, 2}, {1, 2, 3}})')
    checkError(
        fun=function() as.array(mixed2DNotRectArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='as.array(int[][]{{1, 2}, {"a", "b", "c"}})')
    checkError(
        fun=function() as.array(intermixed2DArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='as.array(int[][]{{1, "b", 3}, {"a", 2, "c"}})')
    checkError(
        fun=function() as.array(intermixed2DNotRectArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='as.array(int[][]{{1, "b"}, {"a", 2, "c"}})')

    ###############################################################
    # EXPLICIT CONVERSIONS TO A DATA FRAME
    # array is first implicitly converted to a vector or list and 
    # then passed over to as.array
    ###############################################################
    checkAllEqual(
        actual=as.data.frame(intArray, nm='cl'), # nm='cl' - ensure same column name
        expected=as.data.frame(c(1L, 2L, 3L), nm='cl'),
        desc='as.data.frame(int[] {1, 2, 3})')

    checkAllEqual(
        actual=as.data.frame(int2DArray),
        expected=as.data.frame(matrix(c(1, 1, 2, 2, 3, 3), c(2,3))),
        desc='as.data.frame(int[][] {{1, 2, 3}, {1, 2, 3}})')

    checkAllEqual(
        actual=as.data.frame(mixedArray),
        expected=as.data.frame(list(1L, 'a', '1')),
        desc='as.data.frame(Object[] {1, "a", "1"})')

    checkAllEqual(
        actual=as.data.frame(mixed2DArray),
        expected=as.data.frame(list(c(1L, 2L, 3L), c('a', 'b', 'c'))),
        desc='as.data.frame(Object[][] {{1, 2, 3}, {"a", "b", "c"}})')

    checkError(
        fun=function() as.data.frame(int2DNotRectArray), 
        expectedMsg='A non rectangular array cannot be converted to a vector, only to a list.',
        desc='as.data.frame(int[][]{{1, 2}, {1, 2, 3}})')
    checkError(
        fun=function() as.data.frame(mixed2DNotRectArray), 
        expectedMsg='arguments imply differing number of rows: 2, 3',
        desc='as.data.frame(int[][]{{1, 2}, {"a", "b", "c"}})')
    checkError(
        fun=function() as.data.frame(intermixed2DArray), 
        expectedMsg='arguments imply differing number of rows: 2, 3',
        desc='as.data.frame(int[][]{{1, "b", 3}, {"a", 2, "c"}})')
    checkError(
        fun=function() as.data.frame(intermixed2DNotRectArray), 
        expectedMsg='arguments imply differing number of rows: 2, 3',
        desc='as.data.frame(int[][]{{1, "b"}, {"a", 2, "c"}})')

    ###############################################################
    # a foreign object can be converted to a data.frame 
    # if all keys are arrays. The object is the first implicitely 
    # converted to a named list where the array keys are recursively 
    # resolved as is in implicit conversions.

    javaSource <- 
    'public class ClassWithArrays {
        public boolean[] b = {true, false, true};
        public int[] i = {1, 2, 3};        
    }'
    createJavaClass('ClassWithArrays', javaSource)
    foreignObject <- new('ClassWithArrays')    
    checkAllEqual(
        actual=as.data.frame(foreignObject),
        expected=as.data.frame(list(b=c(T, F, T), i=c(1, 2, 3))),
        desc='as.data.frame(ClassWithArrays)')

    javaSource <- 
    'public class ClassWith2DimArrays {
        public boolean[][] b = {{true, false, true}, {true, false, true}};
        public int[][] i = {{1, 2, 3}, {1, 2, 3}};
    }'
    createJavaClass('ClassWith2DimArrays', javaSource)
    foreignObject <- new('ClassWith2DimArrays')
    checkAllEqual(
        actual=as.data.frame(foreignObject),
        expected=as.data.frame(list(b=matrix(c(T, T, F, F, T, T), 2, 3), i=matrix(c(1, 1, 2, 2, 3, 3), 2, 3))),
        desc='as.data.frame(ClassWith2DimArrays)')

    # converting a foreign object with non array keys results in an error
    javaSource <-
    'public class ClassWithMixedArrays {
        public boolean[][] b = {{true, false, true}, {true, false, true}};
        public int[] i = {1, 2, 3};
        public Object[][] oa = {{true, false, true}, {"a", "b", "c"}};
        public Object n = null;
    }'
    createJavaClass('ClassWithMixedArrays', javaSource)
    foreignObject <- new('ClassWithMixedArrays')
    checkError(
        function() as.data.frame(foreignObject), 
        expectedMsg='arguments imply differing number of rows: 2, 3, 0', 
        desc='as.data.frame(ClassWithMixedArrays)')

    if (errorsCount > 0) {
        cat("FAILED TESTS: ", errorsCount)
    }
}