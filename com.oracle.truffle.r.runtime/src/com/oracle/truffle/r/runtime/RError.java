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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

/**
 * The error messages have been copied from GNU R.
 */
public final class RError extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * This exception should be subclassed by subsystems that need to throw subsystem-specific
     * exceptions to be caught by builtin implementations, which can then invoke
     * {@link RError#error(SourceSection, RErrorException)}, which access the stored {@link Message}
     * object and any arguments. E.g. see {@link PutException}.
     */
    public abstract static class RErrorException extends Exception {
        private static final long serialVersionUID = 1L;

        private final RError.Message msg;
        @CompilationFinal private final Object[] args;

        protected RErrorException(RError.Message msg, Object[] args) {
            super(RError.formatMessage(msg, args));
            this.msg = msg;
            this.args = args;
        }
    }

    private RError(String msg) {
        super(msg);
    }

    @Override
    public String toString() {
        return getMessage();
    }

    // use a ThreadLocal if multi-threaded evaluation
    /**
     * Are we (temporarily) ignore errors? Assumes single threaded.
     */
    private static boolean ignoreError;

    /**
     * Support for the temporary suppression of the normal error catch machinery. After a call to
     * {code ignoreError(true)}, the error will simply be thrown on the assumption that if will be
     * caught by the caller and some alternative course of action chosen. This should always be used
     * in a {@code try finally} block, e.g:
     *
     * <pre>
     * boolean prev = RError.ignoreError(true);
     * try {
     *     // do something that might throw an RError
     *     ...
     * } finally {
     *     RError.ignoreError(prev);
     * }
     * </pre>
     *
     * @param ignore
     * @return the previous setting
     */
    public static boolean ignoreError(boolean ignore) {
        boolean prev = ignoreError;
        ignoreError = ignore;
        return prev;
    }

    /**
     * Handles an R error with the most general argument signature. All other variants delegate to
     * this method. R allows an error to be caught and an arbitrary expression evaluated, often a
     * call to the {@code browser} function for interactive inspection of the environment where the
     * error occurred.
     *
     * Note that the method never actually returns a result, but the throws the error directly.
     * However, the signature has a return type of {@link RError} to allow callers to use the idiom
     * {@code throw error(...)} to indicate the control transfer.
     *
     * @param src source of the code throwing the error, or {@code null} if not available. If src is
     *            {@code null} an attempt is made to identify the call context from the currently
     *            active frame
     * @param msg a {@link Message} instance specifying the error
     * @param args arguments for format specifiers in the message string
     */
    @TruffleBoundary
    public static RError error(SourceSection src, Message msg, Object... args) {
        throw error0(src, msg, args);
    }

    @TruffleBoundary
    public static RError error(SourceSection src, Message msg) {
        throw error0(src, msg, (Object[]) null);
    }

    private static final RError MARKER_EXCEPTION = new RError("<marker exception>");

    private static RError error0(final SourceSection srcCandidate, Message msg, Object... args) {
        if (ignoreError) {
            throw MARKER_EXCEPTION;
        }

        VirtualFrame frame = null;
        SourceSection src = srcCandidate;
        if (src == null) {
            frame = Utils.getActualCurrentFrame();
            if (frame != null) {
                src = RArguments.getCallSourceSection(frame);
            }
        }
        String preamble = "Error";
        String formattedMsg = formatMessage(msg, args);
        String errorMsg = null;
        if (src == null) {
            // generally means top-level of shell or similar
            preamble += ": ";
            errorMsg = preamble + formattedMsg;
        } else {
            preamble += " in " + src.getCode() + " :";
            errorMsg = wrapMessage(preamble, formattedMsg);
        }
        RError rError = new RError(errorMsg);
        RInternalError.reportError(rError, src);

        Object errorExpr = ROptions.getValue("error");
        if (errorExpr != RNull.instance) {
            // Errors and warnings are output before the expression is evaluated
            RContext.getEngine().printRError(rError);
            // errorExpr can be anything, but not everything makes sense
            if (errorExpr instanceof RArgsValuesAndNames) {
                // TODO: worry about other potential ... values?
                errorExpr = ((RArgsValuesAndNames) errorExpr).getValues()[0];
            }
            if (errorExpr instanceof RLanguage || errorExpr instanceof RExpression) {
                if (frame == null) {
                    frame = Utils.getActualCurrentFrame();
                }
                MaterializedFrame materializedFrame = frame == null ? REnvironment.globalEnv().getFrame() : frame.materialize();
                if (errorExpr instanceof RLanguage) {
                    RContext.getEngine().eval((RLanguage) errorExpr, materializedFrame);
                } else if (errorExpr instanceof RExpression) {
                    RContext.getEngine().eval((RExpression) errorExpr, materializedFrame);
                }
            } else {
                // GnuR checks this earlier when the option is set
                throw new RError(Message.INVALID_ERROR.message);
            }
            // Control, transfer to top level, but suppress print
            throw new RError("");
        } else {
            throw rError;
        }
    }

    /**
     * Convenience variant of {@link #error(SourceSection, Message, Object...)} where no source
     * section can be provide. Ideally, this would never happen.
     */
    @TruffleBoundary
    public static RError error(Message msg, Object... args) {
        throw error(null, msg, args);
    }

    @TruffleBoundary
    public static RError error(Message msg) {
        throw error(null, msg, (Object[]) null);
    }

    /**
     * Convenience variant of {@link #error(SourceSection, Message, Object...)} where only one
     * argument to the message is given. This avoids object array creation in the (probably
     * fast-path) caller.
     */
    @TruffleBoundary
    public static RError error(SourceSection src, Message msg, Object arg) {
        throw error(src, msg, new Object[]{arg});
    }

    /**
     * Convenience variant of {@link #error(Message, Object...)} where only one argument to the
     * message is given. This avoids object array creation in the (probably fast-path) caller.
     */
    @TruffleBoundary
    public static RError error(Message msg, Object arg) {
        throw error(msg, new Object[]{arg});
    }

    /**
     * Variant for the case where the original error occurs in code where it is not appropriate to
     * report the error. The error information is propagated using the {@link RErrorException}.
     */
    @TruffleBoundary
    public static RError error(SourceSection src, RErrorException ex) {
        throw error(src, ex.msg, ex.args);
    }

    /**
     * A temporary error that indicates an unimplemented feature where terminating the VM using
     * {@link Utils#fatalError(String)} would be inappropriate.
     */
    @TruffleBoundary
    public static RError nyi(SourceSection src, String msg) {
        throw new RError("NYI: " + (src != null ? src.getCode() : "") + msg);
    }

    @TruffleBoundary
    public static void warning(Message msg, Object... args) {
        RContext.getInstance().setEvalWarning(formatMessage(msg, args));
    }

    @TruffleBoundary
    public static void warning(SourceSection src, Message msg, Object... args) {
        RContext.getInstance().setEvalWarning(wrapMessage("In " + src.getCode() + " :", formatMessage(msg, args)));
    }

    @TruffleBoundary
    public static String formatMessage(Message msg, Object... args) {
        return msg.hasArgs ? String.format(msg.message, args) : msg.message;
    }

    private static String wrapMessage(String preamble, String message) {
        // TODO find out about R's line-wrap policy
        // (is 74 a given percentage of console width?)
        if (preamble.length() + 1 + message.length() >= 74) {
            // +1 is for the extra space following the colon
            return preamble + "\n  " + message;
        } else {
            return preamble + " " + message;
        }
    }

    public static enum Message {
        /**
         * {@code GENERIC} should only be used in the rare case where a known error is not
         * available.
         */
        GENERIC("%s"),
        ARG_RECYCYLED("an argument will be fractionally recycled"),
        LENGTH_GT_1("the condition has length > 1 and only the first element will be used"),
        LENGTH_ZERO("argument is of length zero"),
        NA_UNEXP("missing value where TRUE/FALSE needed"),
        LENGTH_NOT_MULTI("longer object length is not a multiple of shorter object length"),
        INTEGER_OVERFLOW("NAs produced by integer overflow"),
        NA_OR_NAN("NA/NaN argument"),
        SUBSCRIPT_BOUNDS("subscript out of bounds"),
        SUBSCRIPT_BOUNDS_SUB("[[ ]] subscript out of bounds"),
        SELECT_LESS_1("attempt to select less than one element"),
        SELECT_MORE_1("attempt to select more than one element"),
        ONLY_0_MIXED("only 0's may be mixed with negative subscripts"),
        REPLACEMENT_0("replacement has length zero"),
        NOT_MULTIPLE_REPLACEMENT("number of items to replace is not a multiple of replacement length"),
        MORE_SUPPLIED_REPLACE("more elements supplied than there are to replace"),
        NA_SUBSCRIPTED("NAs are not allowed in subscripted assignments"),
        INVALID_ARG_TYPE("invalid argument type"),
        INVALID_ARG_TYPE_UNARY("invalid argument to unary operator"),
        VECTOR_SIZE_NEGATIVE("vector size cannot be negative"),
        NO_LOOP_FOR_BREAK_NEXT("no loop for break/next, jumping to top level"),
        INVALID_FOR_SEQUENCE("invalid for() loop sequence"),
        NO_NONMISSING_MAX("no non-missing arguments to max; returning -Inf"),
        NO_NONMISSING_MIN("no non-missing arguments to min; returning Inf"),
        LENGTH_NONNEGATIVE("length must be non-negative number"),
        MUST_BE_POSITIVE("%s must be a non-negative number"),
        INVALID_TFB("invalid (to - from)/by in seq(.)"),
        WRONG_SIGN_IN_BY("wrong sign in 'by' argument"),
        WRONG_TYPE("wrong type of argument"),
        BY_TOO_SMALL("'by' argument is much too small"),
        INCORRECT_SUBSCRIPTS("incorrect number of subscripts"),
        INCORRECT_SUBSCRIPTS_MATRIX("incorrect number of subscripts on matrix"),
        INVALID_SEP("invalid 'sep' specification"),
        INVALID_LENGTH("invalid '%s' length"),
        EMPTY_WHAT("empty 'what' specified"),
        LINE_ELEMENTS("line %d did not have %d elements"),
        ITEMS_NOT_MULTIPLE("number of items read is not a multiple of the number of columns"),
        // below: GNU R gives also expression for the argument
        NOT_FUNCTION("argument is not a function, character or symbol"),
        NON_NUMERIC_MATH("non-numeric argument to mathematical function"),
        NAN_PRODUCED("NaNs produced"),
        NUMERIC_COMPLEX_MATRIX_VECTOR("requires numeric/complex matrix/vector arguments"),
        NON_CONFORMABLE_ARGS("non-conformable arguments"),
        DATA_VECTOR("'data' must be of a vector type"),
        NON_NUMERIC_MATRIX_EXTENT("non-numeric matrix extent"),
        // below: also can mean empty
        INVALID_NCOL("invalid 'ncol' value (too large or NA)"),
        // below: also can mean empty
        INVALID_NROW("invalid 'nrow' value (too large or NA)"),
        NEGATIVE_NCOL("invalid 'ncol' value (< 0)"),
        NEGATIVE_NROW("invalid 'nrow' value (< 0)"),
        NON_CONFORMABLE_ARRAYS("non-conformable arrays"),
        UNKNOWN_UNNAMED_OBJECT("object not found"),
        ONLY_MATRIX_DIAGONALS("only matrix diagonals can be replaced"),
        REPLACEMENT_DIAGONAL_LENGTH("replacement diagonal has wrong length"),
        NA_INTRODUCED_COERCION("NAs introduced by coercion"),
        ARGUMENT_WHICH_NOT_LOGICAL("argument to 'which' is not logical"),
        X_NUMERIC("'x' must be numeric"),
        X_ARRAY_TWO("'x' must be an array of at least two dimensions"),
        ACCURACY_MODULUS("probable complete loss of accuracy in modulus"),
        INVALID_SEPARATOR("invalid separator"),
        INCORRECT_DIMENSIONS("incorrect number of dimensions"),
        LOGICAL_SUBSCRIPT_LONG("(subscript) logical subscript too long"),
        DECREASING_TRUE_FALSE("'decreasing' must be TRUE or FALSE"),
        ARGUMENT_LENGTHS_DIFFER("argument lengths differ"),
        ZERO_LENGTH_PATTERN("zero-length pattern"),
        ALL_CONNECTIONS_IN_USE("all connections are in use"),
        CANNOT_READ_CONNECTION("cannot read from this connection"),
        CANNOT_WRITE_CONNECTION("cannot write to this connection"),
        TOO_FEW_LINES_READ_LINES("too few lines read in readLines"),
        INVALID_CONNECTION("invalid connection"),
        OUT_OF_RANGE("out-of-range values treated as 0 in coercion to raw"),
        WRITE_ONLY_BINARY("can only write to a binary connection"),
        UNIMPLEMENTED_COMPLEX("unimplemented complex operation"),
        UNIMPLEMENTED_COMPLEX_FUN("unimplemented complex function"),
        COMPARISON_COMPLEX("invalid comparison with complex values"),
        NON_NUMERIC_BINARY("non-numeric argument to binary operator"),
        RAW_SORT("raw vectors cannot be sorted"),
        INVALID_UNNAMED_ARGUMENT("invalid argument"),
        INVALID_UNNAMED_VALUE("invalid value"),
        NAMES_NONVECTOR("names() applied to a non-vector"),
        ONLY_FIRST_VARIABLE_NAME("only the first element is used as variable name"),
        INVALID_FIRST_ARGUMENT("invalid first argument"),
        NO_ENCLOSING_ENVIRONMENT("no enclosing environment"),
        ASSIGN_EMPTY("cannot assign values in the empty environment"),
        ARGUMENT_NOT_MATRIX("argument is not a matrix"),
        DOLLAR_ATOMIC_VECTORS("$ operator is invalid for atomic vectors"),
        COERCING_LHS_TO_LIST("Coercing LHS to a list"),
        ARGUMENT_NOT_LIST("argument not a list"),
        DIMS_CONTAIN_NEGATIVE_VALUES("the dims contain negative values"),
        NEGATIVE_LENGTH_VECTORS_NOT_ALLOWED("negative length vectors are not allowed"),
        FIRST_ARG_MUST_BE_ARRAY("invalid first argument, must be an array"),
        IMAGINARY_PARTS_DISCARDED_IN_COERCION("imaginary parts discarded in coercion"),
        DIMS_CONTAIN_NA("the dims contain missing values"),
        LENGTH_ZERO_DIM_INVALID("length-0 dimension vector is invalid"),
        ATTRIBUTES_LIST_OR_NULL("attributes must be a list or NULL"),
        RECALL_CALLED_OUTSIDE_CLOSURE("'Recall' called from outside a closure"),
        NOT_NUMERIC_VECTOR("argument is not a numeric vector"),
        UNSUPPORTED_PARTIAL("unsupported options for partial sorting"),
        INDEX_RETURN_REMOVE_NA("'index.return' only for 'na.last(NA'"),
        SUPPLY_X_Y_MATRIX("supply both 'x' and 'y' or a matrix-like 'x'"),
        SD_ZERO("the standard deviation is zero"),
        INVALID_UNNAMED_ARGUMENTS("invalid arguments"),
        INVALID_INPUT("invalid input"),
        INVALID_INPUT_TYPE("invalid input type"),
        NA_PRODUCED("NAs produced"),
        DETERMINANT_COMPLEX("determinant not currently defined for complex matrices"),
        NON_NUMERIC_ARGUMENT("non-numeric argument"),
        FFT_FACTORIZATION("fft factorization error"),
        COMPLEX_NOT_PERMITTED("complex matrices not permitted at present"),
        FIRST_QR("first argument must be a QR decomposition"),
        ONLY_SQUARE_INVERTED("only square matrices can be inverted"),
        NON_NUMERIC_ARGUMENT_FUNCTION("non-numeric argument to function"),
        SEED_LENGTH(".Random.seed has wrong length"),
        // below: not exactly GNU-R message
        PROMISE_CYCLE("promise already under evaluation: recursive default argument reference or earlier problems?"),
        MISSING_ARGUMENTS("'missing' can only be used for arguments"),
        INVALID_ENVIRONMENT("invalid environment specified"),
        ENVIR_NOT_LENGTH_ONE("numeric 'envir' arg not of length one"),
        FMT_NOT_CHARACTER("'fmt' is not a character vector"),
        UNSUPPORTED_TYPE("unsupported type"),
        AT_MOST_ONE_ASTERISK("at most one asterisk '*' is supported in each conversion specification"),
        TOO_FEW_ARGUMENTS("too few arguments"),
        ARGUMENT_STAR_NUMBER("argument for '*' conversion specification must be a number"),
        EXACTLY_ONE_WHICH("exactly one attribute 'which' must be given"),
        ATTRIBUTES_NAMED("attributes must be named"),
        MISSING_INVALID("missing value is invalid"),
        CHARACTER_EXPECTED("character argument expected"),
        CANNOT_CHANGE_DIRECTORY("cannot change working directory"),
        FIRST_ARG_MUST_BE_STRING("first argument must be a character string"),
        ZERO_LENGTH_VARIABLE("attempt to use zero-length variable name"),
        ARGUMENT_NOT_INTERPRETABLE_LOGICAL("argument is not interpretable as logical"),
        OPERATIONS_NUMERIC_LOGICAL_COMPLEX("operations are possible only for numeric, logical or complex types"),
        MATCH_VECTOR_ARGS("'match' requires vector arguments"),
        DIMNAMES_NONARRAY("'dimnames' applied to non-array"),
        DIMNAMES_LIST("'dimnames' must be a list"),
        NO_ARRAY_DIMNAMES("no 'dimnames' attribute for array"),
        MISSING_SUBSCRIPT("[[ ]] with missing subscript"),
        IMPROPER_SUBSCRIPT("[[ ]] improper number of subscripts"),
        ROWNAMES_STRING_OR_INT("row names must be 'character' or 'integer', not '%s'"),
        ONLY_FIRST_USED("numerical expression has %d elements: only the first used"),
        NO_SUCH_INDEX("no such index at level %d"),
        LIST_COERCION("(list) object cannot be coerced to type '%s'"),
        CAT_ARGUMENT_LIST("argument %d (type 'list') cannot be handled by 'cat'"),
        DATA_NOT_MULTIPLE_ROWS("data length [%d] is not a sub-multiple or multiple of the number of rows [%d]"),
        ARGUMENT_NOT_MATCH("supplied argument name '%s' does not match '%s'"),
        ARGUMENT_MISSING("argument \"%s\" is missing, with no default"),
        UNKNOWN_FUNCTION("could not find function \"%s\""),
        UNKNOWN_FUNCTION_USE_METHOD("no applicable method for '%s' applied to an object of class '%s'"),
        UNKNOWN_OBJECT("object '%s' not found"),
        INVALID_ARGUMENT("invalid '%s' argument"),
        INVALID_POS_ARGUMENT("invalid 'pos' argument"),
        INVALID_VALUE("invalid '%s' value"),
        INVALID_ARGUMENTS_NO_QUOTE("invalid %s arguments"),
        INVALID_SUBSCRIPT_TYPE("invalid subscript type '%s'"),
        ARGUMENT_NOT_VECTOR("argument %d is not a vector"),
        CANNOT_COERCE("cannot coerce type '%s' to vector of type '%s'"),
        ARGUMENT_ONLY_FIRST("argument '%s' has length > 1 and only the first element will be used"),
        CANNOT_OPEN_FILE("cannot open file '%s': %s"),
        NOT_CONNECTION("'%s' is not a connection"),
        INCOMPLETE_FINAL_LINE("incomplete final line found on '%s'"),
        CANNOT_OPEN_PIPE("cannot open pipe() cmd '%s': %s"),
        INVALID_TYPE_ARGUMENT("invalid 'type' (%s) of argument"),
        ATTRIBUTE_VECTOR_SAME_LENGTH("'%s' attribute [%d] must be the same length as the vector [%d]"),
        SCAN_UNEXPECTED("scan() expected '%s', got '%s'"),
        MUST_BE_ENVIRON("'%s' must be an environment"),
        UNUSED_ARGUMENT("unused argument (%s)"),
        UNUSED_ARGUMENTS("unused arguments (%s)"),
        INFINITE_MISSING_VALUES("infinite or missing values in '%s'"),
        NON_SQUARE_MATRIX("non-square matrix in '%s'"),
        LAPACK_ERROR("error code %d from Lapack routine '%s'"),
        VALUE_OUT_OF_RANGE("value out of range in '%s'"),
        MUST_BE_STRING("'%s' must be a character string"),
        ARGUMENT_MUST_BE_STRING("argument '%s' must be a character string"),
        MUST_BE_NONNULL_STRING("'%s' must be non-null character string"),
        IS_OF_WRONG_LENGTH("'%s' is of wrong length %d (!= %d)"),
        IS_OF_WRONG_ARITY("'%d' argument passed to '%s' which requires '%d'"),
        OBJECT_NOT_SUBSETTABLE("object of type '%s' is not subsettable"),
        DIMS_DONT_MATCH_LENGTH("dims [product %d] do not match the length of object [%d]"),
        DIMNAMES_DONT_MATCH_DIMS("length of 'dimnames' [%d] must match that of 'dims' [%d]"),
        DIMNAMES_DONT_MATCH_EXTENT("length of 'dimnames' [%d] not equal to array extent"),
        MUST_BE_ATOMIC("'%s' must be atomic"),
        MUST_BE_NULL_OR_STRING("'%s' must be NULL or a character vector"),
        MUST_BE_SCALAR("'%s' must be of length 1"),
        ROWS_MUST_MATCH("number of rows of matrices must match (see arg %d)"),
        ROWS_NOT_MULTIPLE("number of rows of result is not a multiple of vector length (arg %d)"),
        ARG_ONE_OF("'%s' should be one of %s"),
        MUST_BE_SQUARE("'%s' must be a square matrix"),
        NON_MATRIX("non-matrix argument to '%s'"),
        NON_NUMERIC_ARGUMENT_TO("non-numeric argument to '%s'"),
        DIMS_GT_ZERO("'%s' must have dims > 0"),
        NOT_POSITIVE_DEFINITE("the leading minor of order %d is not positive definite"),
        LAPACK_INVALID_VALUE("argument %d of Lapack routine %s had invalid value"),
        RHS_SHOULD_HAVE_ROWS("right-hand side should have %d not %d rows"),
        SAME_NUMBER_ROWS("'%s' and '%s' must have the same number of rows"),
        EXACT_SINGULARITY("exact singularity in '%s'"),
        SINGULAR_SOLVE("singular matrix '%s' in solve"),
        SEED_TYPE(".Random.seed is not an integer vector but of type '%s'"),
        INVALID_USE("invalid use of '%s'"),
        FORMAL_MATCHED_MULTIPLE("formal argument \"%s\" matched by multiple actual arguments"),
        ARGUMENT_MATCHES_MULTIPLE("argument %d matches multiple formal arguments"),
        ARGUMENT_EMPTY("argument %d is empty"),
        REPEATED_FORMAL("repeated formal argument '%s'"),
        NOT_A_MATRIX_UPDATE_CLASS("invalid to set the class to matrix unless the dimension attribute is of length 2 (was %d)"),
        NOT_ARRAY_UPDATE_CLASS("cannot set class to \"array\" unless the dimension attribute has length > 0"),
        SET_INVALID_CLASS_ATTR("attempt to set invalid 'class' attribute"),
        NOT_LEN_ONE_LOGICAL_VECTOR("'%s' must be a length 1 logical vector"),
        TOO_LONG_CLASS_NAME("class name too long in '%s'"),
        NON_STRING_GENERIC("'generic' argument must be a character string"),
        OBJECT_NOT_SPECIFIED("object not specified"),
        NO_METHOD_FOUND("no method to invoke"),
        GEN_FUNCTION_NOT_SPECIFIED("generic function not specified"),
        DUPLICATE_SWITCH_DEFAULT("duplicate 'switch' defaults: '%s' and '%s'"),
        NO_ALTERNATIVE_IN_SWITCH("empty alternative in numeric switch"),
        EXPR_NOT_LENGTH_ONE("EXPR must be a length 1 vector"),
        EXPR_MISSING("'EXPR' is missing"),
        INVALID_STORAGE_MODE_UPDATE("invalid to change the storage mode of a factor"),
        NULL_VALUE("'value' must be non-null character string"),
        USE_DEFUNCT("use of '%s' is defunct: use %s instead"),
        NCOL_ZERO("nc(0 for non-null data"),
        NROW_ZERO("nr(0 for non-null data"),
        SAMPLE_LARGER_THAN_POPULATION("cannot take a sample larger than the population when 'replace(FALSE'\n"),
        SAMPLE_OBJECT_NOT_FOUND("object '%s' not found"),
        ERROR_IN_SAMPLE("Error in sample.int(x, size, replace, prob) :  "),
        INCORRECT_NUM_PROB("incorrect number of probabilities"),
        NA_IN_PROB_VECTOR("NA in probability vector"),
        NEGATIVE_PROBABILITY("non-positive probability"),
        MUST_BE_ONE_BYTE("invalid %s: must be one byte"),
        INVALID_DECIMAL_SEP("invalid decimal separator"),
        INVALID_QUOTE_SYMBOL("invalid quote symbol set"),
        // below: not exactly GNU-R message
        TOO_FEW_POSITIVE_PROBABILITY("too few positive probabilities"),
        DOTS_BOUNDS("The ... list does not contain %s elements"),
        REFERENCE_NONEXISTENT("reference to non-existent argument %d"),
        UNRECOGNIZED_FORMAT("unrecognized format specification '%s'"),
        INVALID_FORMAT_LOGICAL("invalid format '%s'; use format %%d or %%i for logical objects"),
        INVALID_FORMAT_INTEGER("invalid format '%s'; use format %%d, %%i, %%o, %%x or %%X for integer objects"),
        // the following list is incomplete (but like GNU-R)
        INVALID_FORMAT_DOUBLE("invalid format '%s'; use format %%f, %%e, %%g or %%a for numeric objects"),
        INVALID_LOGICAL("'%s' must be TRUE or FALSE"),
        INVALID_FORMAT_STRING("invalid format '%s'; use format %%s for character objects"),
        MUST_BE_CHARACTER("'%s' must be of mode character"),
        ALL_ATTRIBUTES_NAMES("all attributes must have names [%d does not]"),
        INVALID_REGEXP("invalid '%s' regular expression"),
        COERCING_ARGUMENT("coercing argument of type '%s' to %s"),
        MUST_BE_TRUE_FALSE_ENVIRONMENT("'%s' must be TRUE, FALSE or an environment"),
        UNKNOWN_OBJECT_MODE("object '%s' of mode '%s' was not found"),
        WRONG_LENGTH_ARG("wrong length for '%s' argument"),
        INVALID_TYPE_IN("invalid '%s' type in 'x %s y'"),
        DOT_DOT_MISSING("'..%d' is missing"),
        DOT_DOT_SHORT("the ... list does not contain %d elements"),
        NO_DOT_DOT("..%d used in an incorrect context, no ... to look in"),
        NO_LIST_FOR_CDR("'nthcdr' needs a list to CDR down"),
        INVALID_TYPE_LENGTH("invalid type/length (%s/%d) in vector allocation"),
        SUBASSIGN_TYPE_FIX("incompatible types (from %s to %s) in subassignment type fix"),
        SUBSCRIPT_TYPES("incompatible types (from %s to %s) in [[ assignment"),
        RECURSIVE_INDEXING_FAILED("recursive indexing failed at level %d"),
        ARGUMENTS_PASSED("%d arguments passed to '%s' which requires %d"),
        ARGUMENTS_PASSED_0_1("0 arguments passed to '%s' which requires 1"),
        NOT_CHARACTER_VECTOR("'%s' must be a character vector"),
        CANNOT_MAKE_VECTOR_OF_MODE("vector: cannot make a vector of mode '%s'"),
        SET_ROWNAMES_NO_DIMS("attempt to set 'rownames' on an object with no dimensions"),
        COLUMNS_NOT_MULTIPLE("number of columns of result is not a multiple of vector length (arg %d)"),
        DATA_FRAMES_SUBSET_ACCESS("data frames subset access not supported"),
        CANNOT_ASSIGN_IN_EMPTY_ENV("cannot assign values in the empty environment"),
        CANNOT_OPEN_CONNECTION("cannot open the connection"),
        ERROR_READING_CONNECTION("error reading connection: %s"),
        ERROR_WRITING_CONNECTION("error writing connection: %s"),
        ERROR_FLUSHING_CONNECTION("error flushing connection: %s"),
        ALREADY_OPEN_CONNECTION("connection is already open"),
        NO_ITEM_NAMED("no item named '%s' on the search list"),
        INVALID_OBJECT("invalid object for 'as.environment'"),
        EMPTY_NO_PARENT("the empty environment has no parent"),
        NOT_AN_ENVIRONMENT("argument is not an environment"),
        CANNOT_SET_PARENT("cannot set the parent of the empty environment"),
        INVALID_OR_UNIMPLEMENTED_ARGUMENTS("invalid or unimplemented arguments"),
        NOTHING_TO_LINK("nothing to link"),
        FROM_TO_DIFFERENT("'from' and 'to' are of different lengths"),
        NA_IN_FOREIGN_FUNCTION_CALL("NAs in foreign function call (arg %d)"),
        NA_NAN_INF_IN_FOREIGN_FUNCTION_CALL("NA/NaN/Inf in foreign function call (arg %d)"),
        INCORRECT_ARG("incorrect arguments to %s"),
        UNIMPLEMENTED_ARG_TYPE("unimplemented argument type (arg %d)"),
        NATIVE_CALL_FAILED("native call failed: %s"),
        C_SYMBOL_NOT_IN_TABLE("C symbol name \"%s\" not in load table"),
        FORTRAN_SYMBOL_NOT_IN_TABLE("Fortran symbol name \"%s\" not in load table"),
        NOT_THAT_MANY_FRAMES("not that many frames on the stack"),
        UNIMPLEMENTED_ARGUMENT_TYPE("unimplemented argument type"),
        MUST_BE_SQUARE_NUMERIC("'%s' must be a square numeric matrix"),
        MUST_BE_NUMERIC_MATRIX("'%s' must be a numeric matrix"),
        PARSE_ERROR("parse error"),
        SEED_NOT_VALID_INT("supplied seed is not a valid integer"),
        POSITIVE_CONTEXTS("number of contexts must be positive"),
        INVALID_TIMES_ARG("invalid 'times' value"),
        NORMALIZE_PATH_NOSUCH("path[%d]=\"%s\": No such file or directory"),
        ARGS_MUST_BE_NAMED("all arguments must be named"),
        INVALID_INTERNAL("invalid .Internal() argument"),
        NO_SUCH_INTERNAL("there is no .Internal function '%s'"),
        NO_SUCH_PRIMITIVE("no such primitive function"),
        INVALID_ERROR("invalid value for 'error'"),
        IMP_EXP_NAMES_MATCH("length of import and export names must match"),
        ENV_ADD_BINDINGS("cannot add bindings to a locked environment"),
        ENV_REMOVE_BINDINGS("cannot remove bindings from a locked environment"),
        ENV_REMOVE_VARIABLES("cannot remove variables from the %s environment"),
        ENV_CHANGE_BINDING("cannot change value of locked binding for '%s'"),
        ENV_ASSIGN_EMPTY("cannot assign values in the empty environment"),
        ENV_DETACH_BASE("detaching \"package:base\" is not allowed"),
        ENV_SUBSCRIPT("subscript out of range"),
        DLL_LOAD_ERROR("unable to load shared object '%s'\n  %s"),
        DLL_NOT_LOADED("shared object '%s' was not loaded"),
        RNG_BAD_KIND("RNG kind %s is not available"),
        RNG_NOT_IMPL_KIND("unimplemented RNG kind %d"),
        RNG_READ_SEEDS("cannot read seeds unless 'user_unif_nseed' is supplied"),
        RNG_SYMBOL("%s not found in user rng library"),
        CUMMAX_UNDEFINED_FOR_COMPLEX("'cummin' not defined for complex numbers"),
        CUMMIN_UNDEFINED_FOR_COMPLEX("'cummax' not defined for complex numbers"),
        NMAX_LESS_THAN_ONE("'nmax' must be positive"),
        CHAR_VEC_ARGUMENT("a character vector argument expected"),
        QUOTE_G_ONE("only the first character of 'quote' will be used"),
        UNEXPECTED("unexpected '%s' in \"%s\""),
        FIRST_ELEMENT_USED("first element used of '%s' argument"),
        MUST_BE_COERCIBLE_INTEGER("argument must be coercible to non-negative integer"),
        DEFAULT_METHOD_NOT_IMPLEMENTED_FOR_TYPE("default method not implemented for type '%s'"),
        ARG_MUST_BE_CLOSURE("argument must be a closure"),
        NOT_DEBUGGED("argument is not being debugged"),
        ADDING_INVALID_CLASS("adding class \"%s\" to an invalid object"),
        IS_NA_TO_NON_VECTOR("is.na() applied to non-(list or vector) of type '%s'"),
        NOT_MEANINGFUL_FOR_FACTORS("%s not meaningful for factors"),
        INPUTS_DIFFERENT_LENGTHS("inputs of different lengths"),
        MATRIX_LIKE_REQUIRED("a matrix-like object is required as argument to '%s'"),
        NOT_MEANINGFUL_FOR_ORDERED_FACTORS("'%s' is not meaningful for ordered factors"),
        UNSUPPORTED_URL_SCHEME("unsupported URL scheme"),
        CANNOT_CLOSE_STANDARD_CONNECTIONS("cannot close standard connections"),
        FULL_PRECISION("full precision may not have been achieved in '%s'"),
        ATTACH_BAD_TYPE("'attach' only works for lists, data frames and environments"),
        STRING_ARGUMENT_REQUIRED("string argument required"),
        NOT_VALID_NAMES("not a valid named list");

        public final String message;
        private final boolean hasArgs;

        private Message(String message) {
            this.message = message;
            hasArgs = message.indexOf('%') >= 0;
        }
    }
}
