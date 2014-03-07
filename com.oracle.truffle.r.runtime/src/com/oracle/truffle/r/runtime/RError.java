/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.*;

/*
 * The error messages are copy-pasted and/or hand re-written from GNU R.
 */
public abstract class RError extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String LENGTH_GT_1 = "the condition has length > 1 and only the first element will be used";
    public static final String LENGTH_ZERO = "argument is of length zero";
    public static final String NA_UNEXP = "missing value where TRUE/FALSE needed";
    public static final String LENGTH_NOT_MULTI = "longer object length is not a multiple of shorter object length";
    public static final String INTEGER_OVERFLOW = "NAs produced by integer overflow";
    public static final String NA_OR_NAN = "NA/NaN argument";
    public static final String SUBSCRIPT_BOUNDS = "subscript out of bounds";
    public static final String SELECT_LESS_1 = "attempt to select less than one element";
    public static final String SELECT_MORE_1 = "attempt to select more than one element";
    public static final String ONLY_0_MIXED = "only 0's may be mixed with negative subscripts";
    public static final String REPLACEMENT_0 = "replacement has length zero";
    public static final String NOT_MULTIPLE_REPLACEMENT = "number of items to replace is not a multiple of replacement length";
    public static final String MORE_SUPPLIED_REPLACE = "more elements supplied than there are to replace";
    public static final String NA_SUBSCRIPTED = "NAs are not allowed in subscripted assignments";
    public static final String INVALID_ARG_TYPE = "invalid argument type";
    public static final String INVALID_ARG_TYPE_UNARY = "invalid argument to unary operator";
    public static final String INVALID_LENGTH = "invalid 'length' argument";
    public static final String VECTOR_SIZE_NEGATIVE = "vector size cannot be negative";
    public static final String NO_LOOP_FOR_BREAK_NEXT = "no loop for break/next, jumping to top level";
    public static final String INVALID_FOR_SEQUENCE = "invalid for() loop sequence";
    public static final String NO_NONMISSING_MAX = "no non-missing arguments to max; returning -Inf";
    public static final String NO_NONMISSING_MIN = "no non-missing arguments to min; returning Inf";
    public static final String LENGTH_NONNEGATIVE = "length must be non-negative number";
    public static final String INVALID_TIMES = "invalid 'times' argument";
    public static final String INVALID_TFB = "invalid (to - from)/by in seq(.)";
    public static final String WRONG_SIGN_IN_BY = "wrong sign in 'by' argument";
    public static final String WRONG_TYPE = "wrong type of argument";
    public static final String BY_TOO_SMALL = "'by' argument is much too small";
    public static final String INCORRECT_SUBSCRIPTS = "incorrect number of subscripts";
    public static final String INCORRECT_SUBSCRIPTS_MATRIX = "incorrect number of subscripts on matrix";
    public static final String INVALID_TYPE_LIST = "invalid 'type' (list) of argument";
    public static final String INVALID_SEP = "invalid 'sep' specification";
    public static final String NOT_FUNCTION = "argument is not a function, character or symbol"; // GNU
    // R gives also expression for the argument
    public static final String NON_NUMERIC_MATH = "non-numeric argument to mathematical function";
    public static final String NAN_PRODUCED = "NaNs produced";
    public static final String NUMERIC_COMPLEX_MATRIX_VECTOR = "requires numeric/complex matrix/vector arguments";
    public static final String NON_CONFORMABLE_ARGS = "non-conformable arguments";
    public static final String INVALID_BYROW = "invalid 'byrow' argument";
    public static final String DATA_VECTOR = "'data' must be of a vector type";
    public static final String NON_NUMERIC_MATRIX_EXTENT = "non-numeric matrix extent";
    public static final String INVALID_NCOL = "invalid 'ncol' value (too large or NA)"; // also can
    // mean empty
    public static final String INVALID_NROW = "invalid 'nrow' value (too large or NA)"; // also can
    // mean empty
    public static final String NEGATIVE_NCOL = "invalid 'ncol' value (< 0)";
    public static final String NEGATIVE_NROW = "invalid 'nrow' value (< 0)";
    public static final String NON_CONFORMABLE_ARRAYS = "non-conformable arrays";
    public static final String INVALID_MODE = "invalid 'mode' argument";
    public static final String UNKNOWN_UNNAMED_OBJECT = "object not found";
    public static final String ONLY_MATRIX_DIAGONALS = "only matrix diagonals can be replaced";
    public static final String REPLACEMENT_DIAGONAL_LENGTH = "replacement diagonal has wrong length";
    public static final String NA_INTRODUCED_COERCION = "NAs introduced by coercion";
    public static final String ARGUMENT_WHICH_NOT_LOGICAL = "argument to 'which' is not logical";
    public static final String X_NUMERIC = "'x' must be numeric";
    public static final String X_ARRAY_TWO = "'x' must be an array of at least two dimensions";
    public static final String ACCURACY_MODULUS = "probable complete loss of accuracy in modulus";
    public static final String INVALID_SEPARATOR = "invalid separator";
    public static final String INCORRECT_DIMENSIONS = "incorrect number of dimensions";
    public static final String LOGICAL_SUBSCRIPT_LONG = "(subscript) logical subscript too long";
    public static final String DECREASING_TRUE_FALSE = "'decreasing' must be TRUE or FALSE";
    public static final String ARGUMENT_LENGTHS_DIFFER = "argument lengths differ";
    public static final String ZERO_LENGTH_PATTERN = "zero-length pattern";
    public static final String ALL_CONNECTIONS_IN_USE = "all connections are in use";
    public static final String CANNOT_READ_CONNECTION = "cannot read from this connection";
    public static final String CANNOT_WRITE_CONNECTION = "cannot write to this connection";
    public static final String TOO_FEW_LINES_READ_LINES = "too few lines read in readLines";
    public static final String INVALID_CONNECTION = "invalid connection";
    public static final String OUT_OF_RANGE = "out-of-range values treated as 0 in coercion to raw";
    public static final String WRITE_ONLY_BINARY = "can only write to a binary connection";
    public static final String UNIMPLEMENTED_COMPLEX = "unimplemented complex operation";
    public static final String COMPARISON_COMPLEX = "invalid comparison with complex values";
    public static final String NON_NUMERIC_BINARY = "non-numeric argument to binary operator";
    public static final String RAW_SORT = "raw vectors cannot be sorted";
    public static final String INVALID_UNNAMED_ARGUMENT = "invalid argument";
    public static final String INVALID_UNNAMED_VALUE = "invalid value";
    public static final String NAMES_NONVECTOR = "names() applied to a non-vector";
    public static final String ONLY_FIRST_VARIABLE_NAME = "only the first element is used as variable name";
    public static final String INVALID_FIRST_ARGUMENT = "invalid first argument";
    public static final String NO_ENCLOSING_ENVIRONMENT = "no enclosing environment";
    public static final String ASSIGN_EMPTY = "cannot assign values in the empty environment";
    public static final String ARGUMENT_NOT_MATRIX = "argument is not a matrix";
    public static final String DOLLAR_ATOMIC_VECTORS = "$ operator is invalid for atomic vectors";
    public static final String COERCING_LHS_TO_LIST = "Coercing LHS to a list";
    public static final String ARGUMENT_NOT_LIST = "argument not a list";
    public static final String DIMS_CONTAIN_NEGATIVE_VALUES = "the dims contain negative values";
    public static final String NEGATIVE_LENGTH_VECTORS_NOT_ALLOWED = "negative length vectors are not allowed";
    public static final String FIRST_ARG_MUST_BE_ARRAY = "invalid first argument, must be an array";
    public static final String IMAGINARY_PARTS_DISCARDED_IN_COERCION = "imaginary parts discarded in coercion";
    public static final String DIMS_CONTAIN_NA = "the dims contain missing values";
    public static final String LENGTH_ZERO_DIM_INVALID = "length-0 dimension vector is invalid";
    public static final String ATTRIBUTES_LIST_OR_NULL = "attributes must be a list or NULL";
    public static final String RECALL_CALLED_OUTSIDE_CLOSURE = "'Recall' called from outside a closure";
    public static final String NOT_NUMERIC_VECTOR = "argument is not a numeric vector";
    public static final String UNSUPPORTED_PARTIAL = "unsupported options for partial sorting";
    public static final String INDEX_RETURN_REMOVE_NA = "'index.return' only for 'na.last = NA'";
    public static final String SUPPLY_X_Y_MATRIX = "supply both 'x' and 'y' or a matrix-like 'x'";
    public static final String SD_ZERO = "the standard deviation is zero";
    public static final String INVALID_UNNAMED_ARGUMENTS = "invalid arguments";
    public static final String NA_PRODUCED = "NAs produced";
    public static final String DETERMINANT_COMPLEX = "determinant not currently defined for complex matrices";
    public static final String NON_NUMERIC_ARGUMENT = "non-numeric argument";
    public static final String FFT_FACTORIZATION = "fft factorization error";
    public static final String COMPLEX_NOT_PERMITTED = "complex matrices not permitted at present";
    public static final String FIRST_QR = "first argument must be a QR decomposition";
    public static final String ONLY_SQUARE_INVERTED = "only square matrices can be inverted";
    public static final String NON_NUMERIC_ARGUMENT_FUNCTION = "non-numeric argument to function";
    public static final String SEED_LENGTH = ".Random.seed has wrong length";
    public static final String PROMISE_CYCLE = "promise already under evaluation: recursive default argument reference?"; // not
    // exactly GNU-R message
    public static final String MISSING_ARGUMENTS = "'missing' can only be used for arguments";
    public static final String INVALID_ENVIRONMENT = "invalid environment specified";
    public static final String ENVIR_NOT_LENGTH_ONE = "numeric 'envir' arg not of length one";
    public static final String FMT_NOT_CHARACTER = "'fmt' is not a character vector";
    public static final String UNSUPPORTED_TYPE = "unsupported type";
    public static final String AT_MOST_ONE_ASTERISK = "at most one asterisk '*' is supported in each conversion specification";
    public static final String TOO_FEW_ARGUMENTS = "too few arguments";
    public static final String ARGUMENT_STAR_NUMBER = "argument for '*' conversion specification must be a number";
    public static final String EXACTLY_ONE_WHICH = "exactly one attribute 'which' must be given";
    public static final String ATTRIBUTES_NAMED = "attributes must be named";
    public static final String MISSING_INVALID = "missing value is invalid";
    public static final String CHARACTER_EXPECTED = "character argument expected";
    public static final String CANNOT_CHANGE_DIRECTORY = "cannot change working directory";
    public static final String FIRST_ARG_MUST_BE_STRING = "first argument must be a character string";
    public static final String ZERO_LENGTH_VARIABLE = "attempt to use zero-length variable name";
    public static final String ARGUMENT_NOT_INTERPRETABLE_LOGICAL = "argument is not interpretable as logical";
    public static final String OPERATIONS_NUMERIC_LOGICAL_COMPLEX = "operations are possible only for numeric, logical or complex types";
    public static final String MATCH_VECTOR_ARGS = "'match' requires vector arguments";
    public static final String DIMNAMES_NONARRAY = "'dimnames' applied to non-array";
    public static final String DIMNAMES_LIST = "'dimnames' must be a list";
    public static final String NO_ARRAY_DIMNAMES = "no 'dimnames' attribute for array";
    public static final String MISSING_SUBSCRIPT = "[[ ]] with missing subscript";

    public static final String ONLY_FIRST_USED = "numerical expression has %d elements: only the first used";
    public static final String NO_SUCH_INDEX = "no such index at level %d";
    public static final String LIST_COERCION = "(list) object cannot be coerced to type '%s'";
    public static final String CAT_ARGUMENT_LIST = "argument %d (type 'list') cannot be handled by 'cat'";
    public static final String DATA_NOT_MULTIPLE_ROWS = "data length [%d] is not a sub-multiple or multiple of the number of rows [%d]";
    public static final String ARGUMENT_NOT_MATCH = "supplied argument name '%s' does not match '%s'";
    public static final String ARGUMENT_MISSING = "argument '%s' is missing, with no default";
    public static final String UNKNOWN_FUNCTION = "could not find function '%s'";
    public static final String UNKNOWN_FUNCTION_USE_METHOD = "Error in UseMethod('%s') : \n no applicable method for '%s' applied to an object of class '%s'";
    public static final String UNKNOWN_OBJECT = "object '%s' not found";
    public static final String INVALID_ARGUMENT = "invalid '%s' argument";
    public static final String INVALID_SUBSCRIPT_TYPE = "invalid subscript type '%s'";
    public static final String ARGUMENT_NOT_VECTOR = "argument %d is not a vector";
    public static final String CANNOT_COERCE = "cannot coerce type '%s' to vector of type '%s'";
    public static final String ARGUMENT_ONLY_FIRST = "argument '%s' has length > 1 and only the first element will be used";
    public static final String CANNOT_OPEN_FILE = "cannot open file '%s': %s";
    public static final String NOT_CONNECTION = "'%s' is not a connection";
    public static final String INCOMPLETE_FINAL_LINE = "incomplete final line found on '%s'";
    public static final String CANNOT_OPEN_PIPE = "cannot open pipe() cmd '%s': %s";
    public static final String INVALID_TYPE_ARGUMENT = "invalid 'type' (%s) of argument";
    public static final String ATTRIBUTE_VECTOR_SAME_LENGTH = "'%s' attribute [%d] must be the same length as the vector [%d]";
    public static final String SCAN_UNEXPECTED = "scan() expected '%s', got '%s'";
    public static final String MUST_BE_ENVIRON = "'%s' must be an environment";
    public static final String UNUSED_ARGUMENT = "unused argument(s) (%s)"; // FIXME: GNU-R gives a
    // list of all unused arguments
    public static final String INFINITE_MISSING_VALUES = "infinite or missing values in '%s'";
    public static final String NON_SQUARE_MATRIX = "non-square matrix in '%s'";
    public static final String LAPACK_ERROR = "error code %d from Lapack routine '%s'";
    public static final String VALUE_OUT_OF_RANGE = "value out of range in '%s'";
    public static final String MUST_BE_NONNULL_STRING = "'%s' must be non-null character string";
    public static final String IS_OF_WRONG_LENGTH = "'%s' is of wrong length";
    public static final String IS_OF_WRONG_ARITY = "'%d' argument passed to '%s' which requires '%d'";
    public static final String OBJECT_NOT_SUBSETTABLE = "object of type '%s' is not subsettable";
    public static final String DIMS_DONT_MATCH_LENGTH = "dims [product %d] do not match the length of object [%d]";
    public static final String DIMNAMES_DONT_MATCH_DIMS = "length of 'dimnames' [%d] must match that of 'dims' [%d]";
    public static final String DIMNAMES_DONT_MATCH_EXTENT = "length of 'dimnames' [%d] not equal to array extent";
    public static final String MUST_BE_ATOMIC = "'%s' must be atomic";
    public static final String MUST_BE_NULL_OR_STRING = "'%s' must be NULL or a character vector";
    public static final String MUST_BE_SCALAR = "'%s' must be of length 1";
    public static final String ROWS_MUST_MATCH = "number of rows of matrices must match (see arg %d)";
    public static final String ROWS_NOT_MULTIPLE = "number of rows of result is not a multiple of vector length (arg %d)";
    public static final String ARG_ONE_OF = "'%s' should be one of %s";
    public static final String MUST_BE_SQUARE = "'%s' must be a square matrix";
    public static final String NON_MATRIX = "non-matrix argument to '%s'";
    public static final String NON_NUMERIC_ARGUMENT_TO = "non-numeric argument to '%s'";
    public static final String DIMS_GT_ZERO = "'%s' must have dims > 0";
    public static final String NOT_POSITIVE_DEFINITE = "the leading minor of order %d is not positive definite";
    public static final String LAPACK_INVALID_VALUE = "argument %d of Lapack routine %s had invalid value";
    public static final String RHS_SHOULD_HAVE_ROWS = "right-hand side should have %d not %d rows";
    public static final String SAME_NUMBER_ROWS = "'%s' and '%s' must have the same number of rows";
    public static final String EXACT_SINGULARITY = "exact singularity in '%s'";
    public static final String SINGULAR_SOLVE = "singular matrix '%s' in solve";
    public static final String SEED_TYPE = ".Random.seed is not an integer vector but of type '%s'";
    public static final String INVALID_USE = "invalid use of '%s'";
    public static final String FORMAL_MATCHED_MULTIPLE = "formal argument \"%s\" matched by multiple actual arguments";
    public static final String ARGUMENT_MATCHES_MULTIPLE = "argument %d matches multiple formal arguments";
    public static final String ARGUMENT_EMPTY = "argument %d is empty";
    public static final String REPEATED_FORMAL = "repeated formal argument '%s'";
    public static final String NOT_A_MATRIX_UPDATE_CLASS = "invalid to set the class to matrix unless the dimension attribute is of length 2 (was %d)";
    public static final String NOT_ARRAY_UPDATE_CLASS = "cannot set class to \"array\" unless the dimension attribute has length > 0";
    public static final String SET_INVALID_CLASS_ATTR = "attempt to set invalid 'class' attribute";
    public static final String NOT_LEN_ONE_LOGICAL_VECTOR = "'%s' must be a length 1 logical vector";
    private static final String TOO_LONG_CLASS_NAME = "class name too long in '%s'";
    private static final String NON_STRING_GENERIC = "'generic' argument must be a character string";
    // not exactly
    // GNU-R message
    public static final String DOTS_BOUNDS = "The ... list does not contain %s elements";
    public static final String REFERENCE_NONEXISTENT = "reference to non-existent argument %d";
    public static final String UNRECOGNIZED_FORMAT = "unrecognized format specification '%s'";
    public static final String INVALID_FORMAT_LOGICAL = "invalid format '%s'; use format %%d or %%i for logical objects";
    public static final String INVALID_FORMAT_INTEGER = "invalid format '%s'; use format %%d, %%i, %%o, %%x or %%X for integer objects";
    public static final String INVALID_FORMAT_DOUBLE = "invalid format '%s'; use format %%f, %%e, %%g or %%a for numeric objects"; // the
    // list is incomplete (but like GNU-R)
    public static final String INVALID_FORMAT_STRING = "invalid format '%s'; use format %%s for character objects";
    public static final String MUST_BE_CHARACTER = "'%s' must be of mode character";
    public static final String ALL_ATTRIBUTES_NAMES = "all attributes must have names [%d does not]";
    public static final String INVALID_REGEXP = "invalid '%s' regular expression";
    public static final String COERCING_ARGUMENT = "coercing argument of type '%s' to %s";
    public static final String MUST_BE_TRUE_FALSE_ENVIRONMENT = "'%s' must be TRUE, FALSE or an environment";
    public static final String UNKNOWN_OBJECT_MODE = "object '%s' of mode '%s' was not found";
    public static final String INVALID_TYPE_IN = "invalid '%s' type in 'x %s y'";
    public static final String DOT_DOT_MISSING = "'..%d' is missing";
    public static final String INVALID_TYPE_LENGTH = "invalid type/length (%s/%d) in vector allocation";
    public static final String SUBASSIGN_TYPE_FIX = "incompatible types (from %s to %s) in subassignment type fix";
    public static final String RECURSIVE_INDEXING_FAILED = "recursive indexing failed at level %d";

    private static final String NOT_CHARACTER_VECTOR = "'%s' must be a character vector";

    public static void warning(SourceSection source, String message) {
        RContext.getInstance().setEvalWarning("In " + source.getCode() + " : " + message);
    }

    public static void warning(SourceSection source, String message, Object... args) {
        RContext.getInstance().setEvalWarning("In " + source.getCode() + " : " + stringFormat(message, args));
    }

    public abstract static class RNYIError extends RError {

        private static final long serialVersionUID = -7296314309177604737L;
    }

    public static RError getNYI(final String msg) {
        return new RNYIError() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return msg == null ? "Not yet implemented ..." : msg;
            }
        };
    }

    public static RError getInternal(SourceSection expr, final String msg) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return msg == null ? "Internal implementation error ..." : msg;
            }
        };
    }

    public static RError getLengthZero(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return LENGTH_ZERO;
            }
        };
    }

    public static RError getNAorNaN(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return NA_OR_NAN;
            }
        };
    }

    public static RError getUnexpectedNA(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return NA_UNEXP;
            }
        };
    }

    public static RError getSubscriptBounds(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return SUBSCRIPT_BOUNDS;
            }
        };
    }

    public static RError getSelectLessThanOne(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return SELECT_LESS_1;
            }
        };
    }

    public static RError getSelectMoreThanOne(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return SELECT_MORE_1;
            }
        };
    }

    public static RError getOnlyZeroMixed(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return ONLY_0_MIXED;
            }
        };
    }

    public static RError getReplacementZero(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return REPLACEMENT_0;
            }
        };
    }

    public static RError getMoreElementsSupplied(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return MORE_SUPPLIED_REPLACE;
            }
        };
    }

    public static RError getNASubscripted(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return NA_SUBSCRIPTED;
            }
        };
    }

    public static RError getInvalidArgType(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return INVALID_ARG_TYPE;
            }
        };
    }

    public static RError getInvalidArgTypeUnary(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return INVALID_ARG_TYPE_UNARY;
            }
        };
    }

    public static RError getInvalidLength(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return INVALID_LENGTH;
            }
        };
    }

    public static RError getVectorSizeNegative(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return VECTOR_SIZE_NEGATIVE;
            }
        };
    }

    public static RError getNoLoopForBreakNext(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return NO_LOOP_FOR_BREAK_NEXT;
            }
        };
    }

    public static RError getInvalidForSequence(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_FOR_SEQUENCE;
            }
        };
    }

    public static RError getLengthNonnegative(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.LENGTH_NONNEGATIVE;
            }
        };
    }

    public static RError getInvalidTimes(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_TIMES;
            }
        };
    }

    public static RError getWrongSignInBy(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.WRONG_SIGN_IN_BY;
            }
        };
    }

    public static RError getWrongTypeOfArgument(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.WRONG_TYPE;
            }
        };
    }

    public static RError getByTooSmall(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.BY_TOO_SMALL;
            }
        };
    }

    public static RError getIncorrectSubscripts(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INCORRECT_SUBSCRIPTS;
            }
        };
    }

    public static RError getIncorrectSubscriptsMatrix(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INCORRECT_SUBSCRIPTS_MATRIX;
            }
        };
    }

    public static RError getInvalidTFB(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_TFB;
            }
        };
    }

    public static RError getInvalidTypeList(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_TYPE_LIST;
            }
        };
    }

    public static RError getInvalidSep(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_SEP;
            }
        };
    }

    public static RError getNotFunction(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NOT_FUNCTION;
            }
        };
    }

    public static RError getNonNumericMath(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NON_NUMERIC_MATH;
            }
        };
    }

    public static RError getNumericComplexMatrixVector(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NUMERIC_COMPLEX_MATRIX_VECTOR;
            }
        };
    }

    public static RError getNonConformableArgs(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NON_CONFORMABLE_ARGS;
            }
        };
    }

    public static RError getInvalidByRow(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_BYROW;
            }
        };
    }

    public static RError getDataVector(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.DATA_VECTOR;
            }
        };
    }

    public static RError getNonNumericMatrixExtent(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NON_NUMERIC_MATRIX_EXTENT;
            }
        };
    }

    public static RError getInvalidNCol(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_NCOL;
            }
        };
    }

    public static RError getInvalidNRow(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_NROW;
            }
        };
    }

    public static RError getNegativeNCol(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NEGATIVE_NCOL;
            }
        };
    }

    public static RError getNegativeNRow(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NEGATIVE_NROW;
            }
        };
    }

    public static RError getNonConformableArrays(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NON_CONFORMABLE_ARRAYS;
            }
        };
    }

    public static RError getInvalidMode(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_MODE;
            }
        };
    }

    public static RError getOnlyMatrixDiagonals(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ONLY_MATRIX_DIAGONALS;
            }
        };
    }

    public static RError getReplacementDiagonalLength(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.REPLACEMENT_DIAGONAL_LENGTH;
            }
        };
    }

    public static RError getArgumentWhichNotLogical(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ARGUMENT_WHICH_NOT_LOGICAL;
            }
        };
    }

    public static RError getXNumeric(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.X_NUMERIC;
            }
        };
    }

    public static RError getXArrayTwo(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.X_ARRAY_TWO;
            }
        };
    }

    public static RError getInvalidSeparator(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_SEPARATOR;
            }
        };
    }

    public static RError getIncorrectDimensions(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INCORRECT_DIMENSIONS;
            }
        };
    }

    public static RError getLogicalSubscriptLong(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.LOGICAL_SUBSCRIPT_LONG;
            }
        };
    }

    public static RError getDecreasingTrueFalse(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.DECREASING_TRUE_FALSE;
            }
        };
    }

    public static RError getArgumentLengthsDiffer(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ARGUMENT_LENGTHS_DIFFER;
            }
        };
    }

    public static RError getZeroLengthPattern(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ZERO_LENGTH_PATTERN;
            }
        };
    }

    public static RError getAllConnectionsInUse(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ALL_CONNECTIONS_IN_USE;
            }
        };
    }

    public static RError getCannotReadConnection(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.CANNOT_READ_CONNECTION;
            }
        };
    }

    public static RError getCannotWriteConnection(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.CANNOT_WRITE_CONNECTION;
            }
        };
    }

    public static RError getTooFewLinesReadLines(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.TOO_FEW_LINES_READ_LINES;
            }
        };
    }

    public static RError getInvalidConnection(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_CONNECTION;
            }
        };
    }

    public static RError getWriteOnlyBinary(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.WRITE_ONLY_BINARY;
            }
        };
    }

    public static RError getComparisonComplex(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.COMPARISON_COMPLEX;
            }
        };
    }

    public static RError getUnimplementedComplex(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.UNIMPLEMENTED_COMPLEX;
            }
        };
    }

    public static RError getNonNumericBinary(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NON_NUMERIC_BINARY;
            }
        };
    }

    public static RError getRawSort(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.RAW_SORT;
            }
        };
    }

    public static RError getInvalidUnnamedArgument(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_UNNAMED_ARGUMENT;
            }
        };
    }

    public static RError getInvalidUnnamedValue(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_UNNAMED_VALUE;
            }
        };
    }

    public static RError getNamesNonVector(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NAMES_NONVECTOR;
            }
        };
    }

    public static RError getInvalidFirstArgument(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_FIRST_ARGUMENT;
            }
        };
    }

    public static RError getNoEnclosingEnvironment(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NO_ENCLOSING_ENVIRONMENT;
            }
        };
    }

    public static RError getAssignEmpty(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ASSIGN_EMPTY;
            }
        };
    }

    public static RError getArgumentNotMatrix(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ARGUMENT_NOT_MATRIX;
            }
        };
    }

    public static RError getDimsContainNegativeValues(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.DIMS_CONTAIN_NEGATIVE_VALUES;
            }
        };
    }

    public static RError getNegativeLengthVectorsNotAllowed(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NEGATIVE_LENGTH_VECTORS_NOT_ALLOWED;
            }
        };
    }

    public static RError getFirstArgMustBeArray(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.FIRST_ARG_MUST_BE_ARRAY;
            }
        };
    }

    public static RError getImaginaryPartsDiscardedInCoercion(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION;
            }
        };
    }

    public static RError getNotMultipleReplacement(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NOT_MULTIPLE_REPLACEMENT;
            }
        };
    }

    public static RError getArgumentNotList(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ARGUMENT_NOT_LIST;
            }
        };
    }

    public static RError getUnknownObject(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return UNKNOWN_UNNAMED_OBJECT;
            }
        };
    }

    public static RError getDollarAtomicVectors(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.DOLLAR_ATOMIC_VECTORS;
            }
        };
    }

    public static RError getDimsContainNA(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.DIMS_CONTAIN_NA;
            }
        };
    }

    public static RError getLengthZeroDimInvalid(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.LENGTH_ZERO_DIM_INVALID;
            }
        };
    }

    public static RError getAttributesListOrNull(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ATTRIBUTES_LIST_OR_NULL;
            }
        };
    }

    public static RError getRecallCalledOutsideClosure(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.RECALL_CALLED_OUTSIDE_CLOSURE;
            }
        };
    }

    public static RError getNotNumericVector(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NOT_NUMERIC_VECTOR;
            }
        };
    }

    public static RError getUnsupportedPartial(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.UNSUPPORTED_PARTIAL;
            }
        };
    }

    public static RError getIndexReturnRemoveNA(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INDEX_RETURN_REMOVE_NA;
            }
        };
    }

    public static RError getSupplyXYMatrix(SourceSection source) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.SUPPLY_X_Y_MATRIX;
            }
        };
    }

    public static RError getInvalidUnnamedArguments(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_UNNAMED_ARGUMENTS;
            }
        };
    }

    public static RError getDeterminantComplex(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.DETERMINANT_COMPLEX;
            }
        };
    }

    public static RError getNonNumericArgument(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NON_NUMERIC_ARGUMENT;
            }
        };
    }

    public static RError getFFTFactorization(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.FFT_FACTORIZATION;
            }
        };
    }

    public static RError getComplexNotPermitted(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.COMPLEX_NOT_PERMITTED;
            }
        };
    }

    public static RError getFirstQR(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.FIRST_QR;
            }
        };
    }

    public static RError getOnlySquareInverted(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ONLY_SQUARE_INVERTED;
            }
        };
    }

    public static RError getNonNumericArgumentFunction(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NON_NUMERIC_ARGUMENT_FUNCTION;
            }
        };
    }

    public static RError getSeedLength(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.SEED_LENGTH;
            }
        };
    }

    public static RError getPromiseCycle(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.PROMISE_CYCLE;
            }
        };
    }

    public static RError getMissingArguments(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.MISSING_ARGUMENTS;
            }
        };
    }

    public static RError getCharacterExpected(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.CHARACTER_EXPECTED;
            }
        };
    }

    public static RError getCannotChangeDirectory(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.CANNOT_CHANGE_DIRECTORY;
            }
        };
    }

    public static RError getFirstArgMustBeString(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.FIRST_ARG_MUST_BE_STRING;
            }
        };
    }

    public static RError getZeroLengthVariable(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ZERO_LENGTH_VARIABLE;
            }
        };
    }

    public static RError getArgumentNotInterpretableLogical(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ARGUMENT_NOT_INTERPRETABLE_LOGICAL;
            }
        };
    }

    public static RError getOperationsNumericLogicalComplex(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.OPERATIONS_NUMERIC_LOGICAL_COMPLEX;
            }
        };
    }

    public static RError getInvalidEnvironment(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.INVALID_ENVIRONMENT;
            }
        };
    }

    public static RError getEnvirNotLengthOne(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ENVIR_NOT_LENGTH_ONE;
            }
        };
    }

    public static RError getFmtNotCharacter(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.FMT_NOT_CHARACTER;
            }
        };
    }

    public static RError getUnsupportedType(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.UNSUPPORTED_TYPE;
            }
        };
    }

    public static RError getAtMostOneAsterisk(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.AT_MOST_ONE_ASTERISK;
            }
        };
    }

    public static RError getTooFewArguments(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.TOO_FEW_ARGUMENTS;
            }
        };
    }

    public static RError getArgumentStarNumber(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ARGUMENT_STAR_NUMBER;
            }
        };
    }

    public static RError getExactlyOneWhich(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.EXACTLY_ONE_WHICH;
            }
        };
    }

    public static RError getAttributesNamed(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.ATTRIBUTES_NAMED;
            }
        };
    }

    public static RError getMissingInvalid(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.MISSING_INVALID;
            }
        };
    }

    public static RError getMatchVectorArgs(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.MATCH_VECTOR_ARGS;
            }
        };
    }

    public static RError getDimnamesNonarray(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.DIMNAMES_NONARRAY;
            }
        };
    }

    public static RError getDimnamesList(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.DIMNAMES_LIST;
            }
        };
    }

    public static RError getNoArrayDimnames(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.NO_ARRAY_DIMNAMES;
            }
        };
    }

    public static RError getMissingSubscript(SourceSection expr) {
        return new RErrorInExpr(expr) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return RError.MISSING_SUBSCRIPT;
            }
        };
    }

    public static RError getGenericError(SourceSection source, final String msg) {
        return new RErrorInExpr(source) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
                return msg;
            }
        };
    }

    static class RErrorInExpr extends RError {

        private SourceSection errorNode;
        private static final long serialVersionUID = 1L;

        public RErrorInExpr(SourceSection node) {
            errorNode = node;
        }

        public SourceSection getErrorNode() {
            return errorNode;
        }
    }

    public static RError getUnknownVariable(SourceSection ast, String variable) {
        return getGenericError(ast, stringFormat(RError.UNKNOWN_OBJECT, variable));
    }

    public static RError getArgumentMissing(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.ARGUMENT_MISSING, argName));
    }

    public static RError getUnknownFunction(SourceSection ast, String variable) {
        return getGenericError(ast, stringFormat(RError.UNKNOWN_FUNCTION, variable));
    }

    public static RError getUnknownFunctionUseMethod(SourceSection ast, String function, String classVector) {
        return getGenericError(ast, stringFormat(RError.UNKNOWN_FUNCTION_USE_METHOD, function, function, classVector));
    }

    public static RError getInvalidArgument(SourceSection ast, String str) {
        return getGenericError(ast, stringFormat(RError.INVALID_ARGUMENT, str));
    }

    public static RError getInvalidSubscriptType(SourceSection ast, String str) {
        return getGenericError(ast, stringFormat(RError.INVALID_SUBSCRIPT_TYPE, str));
    }

    public static RError getArgumentNotVector(SourceSection ast, int i) {
        return getGenericError(ast, stringFormat(RError.ARGUMENT_NOT_VECTOR, i));
    }

    public static RError getCannotCoerce(SourceSection ast, String srcType, String dstType) {
        return getGenericError(ast, stringFormat(RError.CANNOT_COERCE, srcType, dstType));
    }

    public static RError getCannotOpenFile(SourceSection ast, String fileName, String reason) {
        return getGenericError(ast, stringFormat(RError.CANNOT_OPEN_FILE, fileName, reason));
    }

    public static RError getCannotOpenPipe(SourceSection ast, String command, String reason) {
        return getGenericError(ast, stringFormat(RError.CANNOT_OPEN_PIPE, command, reason));
    }

    public static RError getNotConnection(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.NOT_CONNECTION, argName));
    }

    public static RError getInvalidTypeArgument(SourceSection ast, String typeName) {
        return getGenericError(ast, stringFormat(RError.INVALID_TYPE_ARGUMENT, typeName));
    }

    public static RError getAttributeVectorSameLength(SourceSection ast, String attr, int attrLen, int vectorLen) {
        return getGenericError(ast, stringFormat(RError.ATTRIBUTE_VECTOR_SAME_LENGTH, attr, attrLen, vectorLen));
    }

    public static RError getNoSuchIndexAtLevel(SourceSection ast, int level) {
        return getGenericError(ast, stringFormat(RError.NO_SUCH_INDEX, level));
    }

    public static RError getScanUnexpected(SourceSection ast, String expType, String gotValue) {
        return getGenericError(ast, stringFormat(RError.SCAN_UNEXPECTED, expType, gotValue));
    }

    public static RError getMustBeEnviron(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.MUST_BE_ENVIRON, argName));
    }

    public static RError getInfiniteMissingValues(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.INFINITE_MISSING_VALUES, argName));
    }

    public static RError getNonSquareMatrix(SourceSection ast, String builtinName) {
        return getGenericError(ast, stringFormat(RError.NON_SQUARE_MATRIX, builtinName));
    }

    public static RError getLapackError(SourceSection ast, int code, String routine) {
        return getGenericError(ast, stringFormat(RError.LAPACK_ERROR, code, routine));
    }

    public static RError getMustBeNonNullString(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.MUST_BE_NONNULL_STRING, argName));
    }

    public static RError getValueOutOfRange(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.VALUE_OUT_OF_RANGE, argName));
    }

    public static RError getValueIsOfWrongLength(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.IS_OF_WRONG_LENGTH, argName));
    }

    public static RError getWrongArity(SourceSection ast, String opName, int arity, int provided) {
        return getGenericError(ast, stringFormat(RError.IS_OF_WRONG_ARITY, arity, opName, provided));
    }

    public static RError getObjectNotSubsettable(SourceSection ast, String typeName) {
        return getGenericError(ast, stringFormat(RError.OBJECT_NOT_SUBSETTABLE, typeName));
    }

    public static RError getMustBeAtomic(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.MUST_BE_ATOMIC, argName));
    }

    public static RError getMustNullOrString(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.MUST_BE_NULL_OR_STRING, argName));
    }

    public static RError getMustBeScalar(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.MUST_BE_SCALAR, argName));
    }

    public static RError getRowsMustMatch(SourceSection ast, int argIndex) {
        return getGenericError(ast, stringFormat(RError.ROWS_MUST_MATCH, argIndex));
    }

    public static RError getNonMatrix(SourceSection ast, String builtinName) {
        return getGenericError(ast, stringFormat(RError.NON_MATRIX, builtinName));
    }

    public static RError getNonNumericArgumentTo(SourceSection ast, String builtinName) {
        return getGenericError(ast, stringFormat(RError.NON_NUMERIC_ARGUMENT_TO, builtinName));
    }

    public static RError getDimsGTZero(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.DIMS_GT_ZERO, argName));
    }

    public static RError getNotPositiveDefinite(SourceSection ast, int order) {
        return getGenericError(ast, stringFormat(RError.NOT_POSITIVE_DEFINITE, order));
    }

    public static RError getLapackInvalidValue(SourceSection ast, int argIndex, String routine) {
        return getGenericError(ast, stringFormat(RError.LAPACK_INVALID_VALUE, argIndex, routine));
    }

    public static RError getUnusedArgument(SourceSection ast, String msg) {
        return getGenericError(ast, stringFormat(RError.UNUSED_ARGUMENT, msg));
    }

    public static RError getDimsDontMatchLength(SourceSection ast, int dimsProduct, int objectLength) {
        return getGenericError(ast, stringFormat(RError.DIMS_DONT_MATCH_LENGTH, dimsProduct, objectLength));
    }

    public static RError getDimNamesDontMatchDims(SourceSection ast, int dimNamesLength, int dimsLength) {
        return getGenericError(ast, stringFormat(RError.DIMNAMES_DONT_MATCH_DIMS, dimNamesLength, dimsLength));
    }

    public static RError getDimNamesDontMatchExtent(SourceSection ast, int dimNamesVectorLength) {
        return getGenericError(ast, stringFormat(RError.DIMNAMES_DONT_MATCH_EXTENT, dimNamesVectorLength));
    }

    @SlowPath
    public static RError getArgOneOf(SourceSection ast, String argName, String[] allowed) {
        StringBuilder str = new StringBuilder();
        boolean first = true;
        for (String s : allowed) {
            if (first) {
                first = false;
            } else {
                str.append(", ");
            }
            str.append("\"");
            str.append(s);
            str.append("\"");
        }
        return getGenericError(ast, stringFormat(RError.ARG_ONE_OF, argName, RRuntime.toString(str)));
    }

    public static RError getMustBeSquare(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.MUST_BE_SQUARE, argName));
    }

    public static RError getRHSShouldHaveRows(SourceSection ast, int should, int has) {
        return getGenericError(ast, stringFormat(RError.RHS_SHOULD_HAVE_ROWS, should, has));
    }

    public static RError getSameNumberRows(SourceSection ast, String matA, String matB) {
        return getGenericError(ast, stringFormat(RError.SAME_NUMBER_ROWS, matA, matB));
    }

    public static RError getExactSingularity(SourceSection ast, String builtinName) {
        return getGenericError(ast, stringFormat(RError.EXACT_SINGULARITY, builtinName));
    }

    public static RError getSingularSolve(SourceSection ast, String matName) {
        return getGenericError(ast, stringFormat(RError.SINGULAR_SOLVE, matName));
    }

    public static RError getSeedType(SourceSection ast, String typeName) {
        return getGenericError(ast, stringFormat(RError.SEED_TYPE, typeName));
    }

    public static RError getInvalidUse(SourceSection ast, String builtinName) {
        return getGenericError(ast, stringFormat(RError.INVALID_USE, builtinName));
    }

    public static RError getFormalMatchedMultiple(SourceSection ast, String formalName) {
        return getGenericError(ast, stringFormat(RError.FORMAL_MATCHED_MULTIPLE, formalName));
    }

    public static RError getArgumentMatchesMultiple(SourceSection ast, int argIndex) {
        return getGenericError(ast, stringFormat(RError.ARGUMENT_MATCHES_MULTIPLE, argIndex));
    }

    public static RError getArgumentEmpty(SourceSection ast, int argIndex) {
        return getGenericError(ast, stringFormat(RError.ARGUMENT_EMPTY, argIndex));
    }

    public static RError getRepeatedFormal(SourceSection ast, String paramName) {
        return getGenericError(ast, stringFormat(RError.REPEATED_FORMAL, paramName));
    }

    public static RError getDotsBounds(SourceSection ast, int index) {
        return getGenericError(ast, stringFormat(RError.DOTS_BOUNDS, index));
    }

    public static RError getReferenceNonexistent(SourceSection ast, int argIndex) {
        return getGenericError(ast, stringFormat(RError.REFERENCE_NONEXISTENT, argIndex));
    }

    public static RError getUnrecognizedFormat(SourceSection ast, String formatString) {
        return getGenericError(ast, stringFormat(RError.UNRECOGNIZED_FORMAT, formatString));
    }

    public static RError getInvalidFormatLogical(SourceSection ast, String formatString) {
        return getGenericError(ast, stringFormat(RError.INVALID_FORMAT_LOGICAL, formatString));
    }

    public static RError getInvalidFormatInteger(SourceSection ast, String formatString) {
        return getGenericError(ast, stringFormat(RError.INVALID_FORMAT_INTEGER, formatString));
    }

    public static RError getInvalidFormatDouble(SourceSection ast, String formatString) {
        return getGenericError(ast, stringFormat(RError.INVALID_FORMAT_DOUBLE, formatString));
    }

    public static RError getInvalidFormatString(SourceSection ast, String formatString) {
        return getGenericError(ast, stringFormat(RError.INVALID_FORMAT_STRING, formatString));
    }

    public static RError getMustBeCharacter(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.MUST_BE_CHARACTER, argName));
    }

    public static RError getAllAttributesNames(SourceSection ast, int attrIndex) {
        return getGenericError(ast, stringFormat(RError.ALL_ATTRIBUTES_NAMES, attrIndex));
    }

    public static RError getListCoercion(SourceSection ast, String typeName) {
        return getGenericError(ast, stringFormat(RError.LIST_COERCION, typeName));
    }

    public static RError getInvalidRegexp(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.INVALID_REGEXP, argName));
    }

    public static RError getMustBeTrueFalseEnvironment(SourceSection ast, String argName) {
        return getGenericError(ast, stringFormat(RError.MUST_BE_TRUE_FALSE_ENVIRONMENT, argName));
    }

    public static RError getUnknownObjectMode(SourceSection ast, String symbol, String typeName) {
        return getGenericError(ast, stringFormat(RError.UNKNOWN_OBJECT_MODE, symbol, typeName));
    }

    public static RError getInvalidTypeIn(SourceSection ast, String operand, String operator) {
        return getGenericError(ast, stringFormat(RError.INVALID_TYPE_IN, operand, operator));
    }

    public static RError getDotDotMissing(SourceSection ast, int dotIndex) {
        return getGenericError(ast, stringFormat(RError.DOT_DOT_MISSING, dotIndex + 1));
    }

    public static RError getInvalidTypeLength(SourceSection ast, String typeName, int length) {
        return getGenericError(ast, stringFormat(RError.INVALID_TYPE_LENGTH, typeName, length));
    }

    public static RError getSubassignTypeFix(SourceSection ast, String fromType, String toType) {
        return getGenericError(ast, stringFormat(RError.SUBASSIGN_TYPE_FIX, fromType, toType));
    }

    public static RError getRecursiveIndexingFailed(SourceSection ast, int level) {
        return getGenericError(ast, stringFormat(RError.RECURSIVE_INDEXING_FAILED, level));
    }

    public static RError getNotMatixUpdateClass(SourceSection ast, int dim) {
        return getGenericError(ast, stringFormat(RError.NOT_A_MATRIX_UPDATE_CLASS, dim));
    }

    public static RError getNotArrayUpdateClass(SourceSection ast) {
        return getGenericError(ast, RError.NOT_ARRAY_UPDATE_CLASS);
    }

    public static RError getInvalidClassAttr(SourceSection ast) {
        return getGenericError(ast, RError.SET_INVALID_CLASS_ATTR);
    }

    @SlowPath
    private static String stringFormat(String format, Object... args) {
        return String.format(format, args);
    }

    public static RError getNotLengthOneLogicalVector(SourceSection sourceSection, final String arg) {
        return getGenericError(sourceSection, String.format(NOT_LEN_ONE_LOGICAL_VECTOR, arg));
    }

    public static RError getNotCharacterVector(SourceSection sourceSection, final String what) {
        return getGenericError(sourceSection, String.format(NOT_CHARACTER_VECTOR, what));
    }

    public static RError getTooLongClassName(SourceSection encapsulatingSourceSection, String generic) {
        return getGenericError(encapsulatingSourceSection, String.format(TOO_LONG_CLASS_NAME, generic));
    }

    public static RError getNonStringGeneric(SourceSection encapsulatingSourceSection) {
        return getGenericError(encapsulatingSourceSection, String.format(NON_STRING_GENERIC));
    }

}
