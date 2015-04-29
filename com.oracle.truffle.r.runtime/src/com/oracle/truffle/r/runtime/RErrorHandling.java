/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.gnur.*;

/**
 * The details of error handling, including condition handling. Derived from GnUR src/main/errors.c.
 * The public methods in this class are primarily intended for use by the {@code .Internal}
 * functions in {@code ConditionFunctions}. Generally the {@link RError} class should be used for
 * error and warning reporting.
 *
 * FastR does not have access to the call that generated the error or warning as an AST (cf GnuR's
 * CLOSXP pairlist), only the {@link SourceSection} associated with the call. There is a need to
 * pass a value that denotes the call back out to R, e.g. as an argument to
 * {@code .handleSimpleError}. The R code mostly treats this as an opaque object, typically passing
 * it back into a {@code .Internal} that calls this class, but it sometimes calls {@code deparse} on
 * the value if it is not {@link RNull#instance}. Either way it must be a valid {@link RType} to be
 * passed as an argument. For better or worse, we use an {@link RPairList}. We handle the
 * {@code deparse} special case explicitly in our implementation of {@code deparse}.
 * <p>
 * TODO Consider using an {@link RLanguage} object to denote the call (somehow).
 */
public class RErrorHandling {
    /*
     * These values are either NULL or an RPairList.
     */
    private static Object restartStack = RNull.instance;
    private static Object handlerStack = RNull.instance;
    private static final Object RESTART_TOKEN = new Object();

    /**
     * The R error handling framework, mostly written in R, expects to be able to get hold of the
     * "current" error message in a context free manner through the {@code geterrmessae .Internal}.
     * There are some other state variables used during processing. To support future multi-threaded
     * behavior, we use a {@link ThreadLocal}.
     */

    private static class ErrorState {
        String errMsg;
        boolean inError;
        boolean inWarning;
        boolean immediateWarning;
        boolean inPrintWarning;
    }

    private static final ThreadLocal<ErrorState> errorState = new ThreadLocal<ErrorState>() {
        @Override
        protected ErrorState initialValue() {
            return new ErrorState();
        }
    };

    public static Object getHandlerStack() {
        return handlerStack;
    }

    public static Object getRestartStack() {
        return restartStack;
    }

    public static void restoreStacks(Object savedHandlerStack, Object savedRestartStack) {
        handlerStack = savedHandlerStack;
        restartStack = savedRestartStack;
    }

    public static Object createHandlers(RStringVector classes, RList handlers, REnvironment parentEnv, Object target, byte calling) {
        Object oldStack = handlerStack;
        Object newStack = oldStack;
        RList result = RDataFactory.createList(new Object[]{RNull.instance, RNull.instance, RNull.instance});
        int n = handlers.getLength();
        for (int i = n - 1; i >= 0; i--) {
            String klass = classes.getDataAt(i);
            Object handler = handlers.getDataAt(i);
            RList entry = mkHandlerEntry(klass, parentEnv, handler, target, result, calling);
            newStack = RDataFactory.createPairList(entry, newStack);
        }
        handlerStack = newStack;
        return oldStack;
    }

    private static final int ENTRY_CLASS = 0;
    private static final int ENTRY_CALLING_ENVIR = 1;
    private static final int ENTRY_HANDLER = 2;
    private static final int ENTRY_TARGET_ENVIR = 3;
    private static final int ENTRY_RETURN_RESULT = 4;

    private static final int RESULT_COND = 0;
    private static final int RESULT_CALL = 1;
    private static final int RESULT_HANDLER = 2;

    private static RList mkHandlerEntry(String klass, REnvironment parentEnv, Object handler, Object rho, RList result, byte calling) {
        Object[] data = new Object[5];
        data[ENTRY_CLASS] = klass;
        data[ENTRY_CALLING_ENVIR] = parentEnv;
        data[ENTRY_HANDLER] = handler;
        data[ENTRY_TARGET_ENVIR] = rho;
        data[ENTRY_RETURN_RESULT] = result;
        RList entry = RDataFactory.createList(data);
        entry.setGPBits(calling);
        return entry;

    }

    private static boolean isCallingEntry(RList entry) {
        return entry.getGPBits() != 0;
    }

    @TruffleBoundary
    public static String geterrmessage() {
        return errorState.get().errMsg;
    }

    @TruffleBoundary
    public static void seterrmessage(String msg) {
        errorState.get().errMsg = msg;
    }

    @TruffleBoundary
    public static void addRestart(RList restart) {
        restartStack = RDataFactory.createPairList(restart, restartStack, RNull.instance);
    }

    @TruffleBoundary
    public static void signalCondition(RList cond, String msg, Object call) {
        Object oldStack = handlerStack;
        RPairList pList;
        while ((pList = findConditionHandler(cond)) != null) {
            RList entry = (RList) pList.car();
            handlerStack = pList.cdr();
            if (isCallingEntry(entry)) {
                Object h = entry.getDataAt(ENTRY_HANDLER);
                if (h == RESTART_TOKEN) {
                    errorcallDflt(fromCall(call), Message.GENERIC, msg);
                } else {
                    // TODO: temporary workaround just to prevent suppressMessages from failing -
                    // what we really need to do is to evaluate the handler with proper arguments
                    Utils.warn("condition signalling not fully supported");
                    break;
                }
            } else {
                throw gotoExitingHandler(cond, call, entry);
            }
        }
        handlerStack = oldStack;
    }

    /**
     * Called from {@link RError} to initiate the condition handling logic.
     */
    static void signalError(SourceSection callSrc, Message msg, Object... args) {
        String fMsg = formatMessage(msg, args);
        Object oldStack = handlerStack;
        RPairList pList;
        while ((pList = findSimpleErrorHandler()) != null) {
            RList entry = (RList) pList.car();
            handlerStack = pList.cdr();
            errorState.get().errMsg = fMsg;
            if (isCallingEntry(entry)) {
                if (entry.getDataAt(ENTRY_HANDLER) == RESTART_TOKEN) {
                    return;
                } else {
                    RFunction handler = (RFunction) entry.getDataAt(2);
                    RStringVector errorMsgVec = RDataFactory.createStringVectorFromScalar(fMsg);
                    RContext.getRASTHelper().handleSimpleError(handler, errorMsgVec, createCall(callSrc), RArguments.getDepth(safeCurrentFrame()));
                }
            } else {
                throw gotoExitingHandler(RNull.instance, createCall(callSrc), entry);
            }
        }
        handlerStack = oldStack;
    }

    private static ReturnException gotoExitingHandler(Object cond, Object call, RList entry) throws ReturnException {
        REnvironment rho = (REnvironment) entry.getDataAt(ENTRY_TARGET_ENVIR);
        RList result = (RList) entry.getDataAt(ENTRY_RETURN_RESULT);
        Object[] resultData = result.getDataWithoutCopying();
        resultData[RESULT_COND] = cond;
        resultData[RESULT_CALL] = call;
        resultData[RESULT_HANDLER] = entry.getDataAt(ENTRY_HANDLER);
        throw new ReturnException(result, rho.getFrame());
    }

    private static RPairList findSimpleErrorHandler() {
        Object list = handlerStack;
        while (list != RNull.instance) {
            RPairList pList = (RPairList) list;
            RList entry = (RList) pList.car();
            String klass = (String) entry.getDataAt(0);
            if (klass.equals("simpleError") || klass.equals("error") || klass.equals("condition")) {
                return pList;
            }
            list = pList.cdr();
        }
        return null;
    }

    private static RPairList findConditionHandler(RList cond) {
        // GnuR checks whether this is a string vector - in FastR it's statically typed to be
        RStringVector classes = cond.getClassHierarchy();
        Object list = handlerStack;
        while (list != RNull.instance) {
            RPairList pList = (RPairList) list;
            RList entry = (RList) pList.car();
            String klass = (String) entry.getDataAt(0);
            for (int i = 0; i < classes.getLength(); i++) {
                if (klass.equals(classes.getDataAt(i))) {
                    return pList;
                }
            }
            list = pList.cdr();
        }
        return null;

    }

    @TruffleBoundary
    public static void dfltStop(String msg, Object call) {
        errorcallDflt(fromCall(call), Message.GENERIC, msg);
    }

    @TruffleBoundary
    public static void dfltWarn(String msg, Object call) {
        warningcallDflt(fromCall(call), Message.GENERIC, msg);
    }

    /**
     * Convert a {@code call} value back into a {@link SourceSection}.
     *
     * @param call Either {@link RNull#instance} or an {@link RPairList}.
     * @return a {@link SourceSection} which may be null iff {@code call == RNull.instance}.
     */
    private static SourceSection fromCall(Object call) {
        if (call == RNull.instance) {
            return null;
        } else if (call instanceof RPairList) {
            RPairList pl = (RPairList) call;
            return (SourceSection) pl.getTag();
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * Create an (opaque) value to carry a {@link SourceSection} for callback to R. The input value
     * may be {@code null}. We assert that no R code ever accesses the content of the result.
     *
     * @return Either {@link RNull#instance} or an {@link RPairList} with tag set to {@code src}.
     *         The {@code type} tag helps {@code deparse} distinguish this value from a standard
     *         {@link RPairList}.
     */
    private static Object createCall(SourceSection src) {
        return src == null ? RNull.instance : RDataFactory.createPairList(null, null, src, SEXPTYPE.FASTR_SOURCESECTION);
    }

    /**
     * The default error handler. This is where all the error message formatting is done and the
     * output.
     */
    static RError errorcallDflt(SourceSection callArg, Message msg, Object... objects) throws RError {
        SourceSection call = callArg;
        String fmsg = formatMessage(msg, objects);
        if (call == null) {
            Frame frame = Utils.getActualCurrentFrame();
            if (frame != null) {
                call = RArguments.getCallSourceSection(frame);
            }
        }

        String errorMessage = createErrorMessage(call, fmsg);
        Utils.writeStderr(errorMessage, true);

        if (warnings.size() > 0) {
            Utils.writeStderr("In addition: ", false);
            printWarnings(false);
        }

        // we are not quite done - need to check for options(error=expr)
        Object errorExpr = ROptions.getValue("error");
        if (errorExpr != RNull.instance) {
            MaterializedFrame materializedFrame = safeCurrentFrame();
            // type already checked in ROptions
            if (errorExpr instanceof RFunction) {
                // Called with no arguments, but defaults will be applied
                RFunction errorFunction = (RFunction) errorExpr;
                ArgumentsSignature argsSig = RContext.getRASTHelper().getArgumentsSignature(errorFunction);
                Object[] evaluatedArgs;
                if (errorFunction.isBuiltin()) {
                    evaluatedArgs = RContext.getRASTHelper().getBuiltinDefaultParameterValues(errorFunction);
                } else {
                    evaluatedArgs = new Object[argsSig.getLength()];
                    for (int i = 0; i < evaluatedArgs.length; i++) {
                        evaluatedArgs[i] = RMissing.instance;
                    }
                }
                errorFunction.getTarget().call(RArguments.create(errorFunction, call, materializedFrame, RArguments.getDepth(materializedFrame) + 1, evaluatedArgs, argsSig));
            } else if (errorExpr instanceof RLanguage || errorExpr instanceof RExpression) {
                if (errorExpr instanceof RLanguage) {
                    RContext.getEngine().eval((RLanguage) errorExpr, materializedFrame);
                } else if (errorExpr instanceof RExpression) {
                    RContext.getEngine().eval((RExpression) errorExpr, materializedFrame);
                }
            } else {
                // Checked when set
                throw RInternalError.shouldNotReachHere();
            }
        }
        throw new RError(errorMessage);
    }

    private static MaterializedFrame safeCurrentFrame() {
        Frame frame = Utils.getActualCurrentFrame();
        return frame == null ? REnvironment.globalEnv().getFrame() : frame.materialize();
    }

    // TODO ? GnuR uses a vector with names
    private static class Warning {
        final String message;
        final Object call;

        Warning(String message, Object call) {
            this.message = message;
            this.call = call;
        }
    }

    private static final LinkedList<Warning> warnings = new LinkedList<>();
    private static int maxWarnings = 50;

    static void warningcall(SourceSection src, Message msg, Object... args) {
        Object call = createCall(src);
        RStringVector warningMessage = RDataFactory.createStringVectorFromScalar(formatMessage(msg, args));
        /*
         * Warnings generally do not prevent results being printed. However, this call into R will
         * destroy any visibility setting made by the calling builtin prior to this call. So we save
         * and restore it across the call.
         */
        boolean visibility = RContext.isVisible();
        try {
            RContext.getRASTHelper().signalSimpleWarning(warningMessage, call, RArguments.getDepth(safeCurrentFrame()));
        } finally {
            RContext.setVisible(visibility);
        }
    }

    static void warningcallDflt(SourceSection call, Message msg, Object... args) {
        vwarningcallDflt(call, msg, args);
    }

    static void vwarningcallDflt(SourceSection call, Message msg, Object... args) {
        ErrorState myErrorState = errorState.get();
        if (myErrorState.inWarning) {
            return;
        }
        Object s = ROptions.getValue("warning.expression");
        if (s != RNull.instance) {
            if (!(s instanceof RLanguage || s instanceof RExpression)) {
                // TODO
            }
            throw RInternalError.unimplemented();
        }

        // ensured in ROptions
        int w = ((RIntVector) ROptions.getValue("warn")).getDataAt(0);
        if (w == RRuntime.INT_NA) {
            w = 0;
        }
        if (w <= 0 && myErrorState.immediateWarning) {
            w = 1;
        }

        if (w < 0 || myErrorState.inWarning || errorState.get().inError) {
            /*
             * ignore if w<0 or already in here
             */
            return;
        }

        try {
            myErrorState.inWarning = true;
            String fmsg = formatMessage(msg, args);
            String message = createWarningMessage(call, fmsg);
            if (w >= 2) {
                throw RInternalError.unimplemented();
            } else if (w == 1) {
                Utils.writeStderr(message, true);
            } else if (w == 0) {
                warnings.add(new Warning(fmsg, call));
            }
        } finally {
            myErrorState.inWarning = false;
        }
    }

    @TruffleBoundary
    public static void printWarnings(boolean suppress) {
        if (suppress) {
            warnings.clear();
            return;
        }
        int nWarnings = warnings.size();
        if (nWarnings == 0) {
            return;
        }
        if (errorState.get().inPrintWarning) {
            if (nWarnings > 0) {
                warnings.clear();
                Utils.writeStderr("Lost warning messages", true);
            }
            return;
        }
        try {
            errorState.get().inPrintWarning = true;
            if (nWarnings == 1) {
                Utils.writeStderr("Warning message:", true);
                Warning warning = warnings.get(0);
                if (warning.call == null) {
                    Utils.writeStderr(warning.message, true);
                } else {
                    Utils.writeStderr(String.format("In %s : %s", warning.call, warning.message), true);
                }
            } else if (nWarnings <= 10) {
                Utils.writeStderr("Warning messages:", true);
                for (int i = 0; i < nWarnings; i++) {
                    Utils.writeStderr((i + 1) + ":", true);
                    Utils.writeStderr("  " + warnings.get(i).message, true);
                }
            } else {
                if (nWarnings < maxWarnings) {
                    Utils.writeStderr(String.format("There were %d warnings (use warnings() to see them)", nWarnings), true);
                } else {
                    Utils.writeStderr(String.format("There were %d or more warnings (use warnings() to see the first %d)", nWarnings), true);
                }
            }
            Object[] wData = new Object[nWarnings];
            // TODO names
            for (int i = 0; i < nWarnings; i++) {
                wData[i] = warnings.get(i);
            }
            RList lw = RDataFactory.createList(wData);
            REnvironment.baseEnv().safePut("last.warning", lw);
        } finally {
            errorState.get().inPrintWarning = false;
            warnings.clear();
        }
    }

    /**
     * Converts a {@link RError.Message}, that possibly requires arguments into a {@link String}.
     */
    static String formatMessage(RError.Message msg, Object... args) {
        return msg.hasArgs ? String.format(msg.message, args) : msg.message;
    }

    static String wrapMessage(String preamble, String message) {
        // TODO find out about R's line-wrap policy
        // (is 74 a given percentage of console width?)
        if (preamble.length() + 1 + message.length() >= 74) {
            // +1 is for the extra space following the colon
            return preamble + "\n  " + message;
        } else {
            return preamble + " " + message;
        }
    }

    static String createErrorMessage(SourceSection src, String formattedMsg) {
        return createKindMessage("Error", src, formattedMsg);
    }

    static String createWarningMessage(SourceSection src, String formattedMsg) {
        return createKindMessage("Warning", src, formattedMsg);
    }

    /**
     * Creates an error message suitable for output to the user, taking into account {@code src},
     * which may be {@code null}.
     */
    static String createKindMessage(String kind, SourceSection src, String formattedMsg) {
        String preamble = kind;
        String errorMsg = null;
        if (src == null) {
            // generally means top-level of shell or similar
            preamble += ": ";
            errorMsg = preamble + formattedMsg;
        } else {
            preamble += " in " + src.getCode() + " :";
            errorMsg = wrapMessage(preamble, formattedMsg);
        }
        return errorMsg;
    }

}
