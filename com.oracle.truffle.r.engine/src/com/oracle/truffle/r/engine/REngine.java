/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.engine;

import java.io.*;
import java.util.*;

import org.antlr.runtime.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.parser.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.ConsoleHandler;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;
import com.oracle.truffle.r.runtime.rng.*;
import com.oracle.truffle.r.runtime.ffi.Load_RFFIFactory;

/**
 * The engine for the FastR implementation. Handles parsing and evaluation. There is exactly one
 * instance of this class, stored in {link #singleton}.
 */
public final class REngine implements RContext.Engine {

    private static final REngine singleton = new REngine();

    @CompilationFinal private boolean crashOnFatalError;
    @CompilationFinal private long startTime;
    @CompilationFinal private long[] childTimes;
    @CompilationFinal private RContext context;
    @CompilationFinal private RBuiltinLookup builtinLookup;
    @CompilationFinal private RFunction evalFunction;

    private REngine() {
    }

    /**
     * Initialize the engine.
     *
     * @param commandArgs
     * @param consoleHandler for console input/output
     * @param crashOnFatalErrorArg if {@code true} any unhandled exception will terminate the
     *            process.
     * @return a {@link VirtualFrame} that can be passed to
     *         {@link #parseAndEval(String, String, VirtualFrame, REnvironment, boolean, boolean)}
     */
    public static VirtualFrame initialize(String[] commandArgs, ConsoleHandler consoleHandler, boolean crashOnFatalErrorArg, boolean headless) {
        singleton.startTime = System.nanoTime();
        singleton.childTimes = new long[]{0, 0};
        Locale.setDefault(Locale.ROOT);
        FastROptions.initialize();
        Load_RFFIFactory.initialize();
        RPerfAnalysis.initialize();
        singleton.crashOnFatalError = crashOnFatalErrorArg;
        singleton.builtinLookup = RBuiltinPackages.getInstance();
        singleton.context = RContext.setRuntimeState(singleton, commandArgs, consoleHandler, headless);
        VirtualFrame globalFrame = RRuntime.createNonFunctionFrame();
        VirtualFrame baseFrame = RRuntime.createNonFunctionFrame();
        REnvironment.baseInitialize(globalFrame, baseFrame);
        singleton.evalFunction = singleton.lookupBuiltin("eval");
        RPackageVariables.initializeBase();
        RVersionInfo.initialize();
        RAccuracyInfo.initialize();
        RRNG.initialize();
        TempDirPath.initialize();
        LibPaths.initialize();
        ROptions.initialize();
        RProfile.initialize();
        // eval the system profile
        singleton.parseAndEval("<system_profile>", RProfile.systemProfile(), baseFrame, REnvironment.baseEnv(), false, false);
        REnvironment.packagesInitialize(RPackages.initialize());
        RPackageVariables.initialize(); // TODO replace with R code
        String siteProfile = RProfile.siteProfile();
        if (siteProfile != null) {
            singleton.parseAndEval("<site_profile>", siteProfile, baseFrame, REnvironment.baseEnv(), false, false);
        }
        String userProfile = RProfile.userProfile();
        if (userProfile != null) {
            singleton.parseAndEval("<user_profile>", userProfile, globalFrame, REnvironment.globalEnv(), false, false);
        }
        return globalFrame;
    }

    public static REngine getInstance() {
        return singleton;
    }

    public void loadDefaultPackage(String name, VirtualFrame frame, REnvironment envForFrame) {
        RBuiltinPackages.load(name, frame, envForFrame);
    }

    public RFunction lookupBuiltin(String name) {
        return builtinLookup.lookup(name);
    }

    public long elapsedTimeInNanos() {
        return System.nanoTime() - startTime;
    }

    public long[] childTimesInNanos() {
        return childTimes;
    }

    public Object parseAndEval(String sourceDesc, String rscript, VirtualFrame frame, REnvironment envForFrame, boolean printResult, boolean allowIncompleteSource) {
        return parseAndEvalImpl(new ANTLRStringStream(rscript), Source.asPseudoFile(rscript, sourceDesc), frame, printResult, allowIncompleteSource);
    }

    public Object parseAndEvalTest(String rscript, boolean printResult) {
        VirtualFrame frame = RRuntime.createNonFunctionFrame();
        REnvironment.resetForTest(frame);
        return parseAndEvalImpl(new ANTLRStringStream(rscript), Source.asPseudoFile(rscript, "<test_input>"), frame, printResult, false);
    }

    public class ParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public ParseException(String msg) {
            super(msg);
        }
    }

    public RExpression parse(String rscript) throws RContext.Engine.ParseException {
        try {
            Sequence seq = (Sequence) ParseUtil.parseAST(new ANTLRStringStream(rscript), Source.asPseudoFile(rscript, "<parse_input>"));
            ASTNode[] exprs = seq.getExpressions();
            Object[] data = new Object[exprs.length];
            for (int i = 0; i < exprs.length; i++) {
                data[i] = RDataFactory.createLanguage(transform(exprs[i], REnvironment.emptyEnv()));
            }
            return RDataFactory.createExpression(RDataFactory.createList(data));
        } catch (RecognitionException ex) {
            throw new RContext.Engine.ParseException(ex.getMessage());
        }
    }

    public Object eval(RFunction function, RExpression expr, REnvironment envir, REnvironment enclos) throws PutException {
        Object result = null;
        RFunction ffunction = function;
        if (ffunction == null) {
            ffunction = evalFunction;
        }
        for (int i = 0; i < expr.getLength(); i++) {
            RLanguage lang = (RLanguage) expr.getDataAt(i);
            result = eval(ffunction, (RNode) lang.getRep(), envir, enclos);
        }
        return result;
    }

    public Object eval(RFunction function, RLanguage expr, REnvironment envir, REnvironment enclos) throws PutException {
        RFunction ffunction = function;
        if (ffunction == null) {
            ffunction = evalFunction;
        }
        return eval(ffunction, (RNode) expr.getRep(), envir, enclos);
    }

    public Object eval(RExpression expr, VirtualFrame frame) {
        Object result = null;
        for (int i = 0; i < expr.getLength(); i++) {
            RLanguage lang = (RLanguage) expr.getDataAt(i);
            result = eval(lang, frame);
        }
        return result;
    }

    public Object eval(RLanguage expr, VirtualFrame frame) {
        RootCallTarget callTarget = makeCallTarget((RNode) expr.getRep());
        return runCall(callTarget, frame, false, false);

    }

    /**
     * This is tricky because the {@link Frame} "f" associated with {@code envir} has been
     * materialized so we can't evaluate in it directly. Instead we create a new
     * {@link VirtualFrame}, that is a logical clone of "f", evaluate in that, and then update "f"
     * on return.
     *
     * N.B. The implementation should do its utmost to avoid calling this method as it is inherently
     * inefficient. In particular, in the case where a {@link VirtualFrame} is available, then the
     * {@code eval} methods that take such a {@link VirtualFrame} should be used in preference.
     *
     * TODO The check to patch the enclosing frame for {@code RFunctions} defined during the eval is
     * painful and perhaps inadequate (should we be deep analysis of the result?). Can we find a way
     * to avoid the patch? and get the enclosing frame correct on definition?
     *
     */
    private static Object eval(RFunction function, RNode exprRep, REnvironment envir, @SuppressWarnings("unused") REnvironment enclos) throws PutException {
        RootCallTarget callTarget = makeCallTarget(exprRep);
        MaterializedFrame envFrame = envir.getFrame();
        VirtualFrame vFrame = RRuntime.createFunctionFrame(function);
        RArguments.setEnclosingFrame(vFrame, RArguments.getEnclosingFrame(envFrame));
        // We make the new frame look like it was a real call to "function" (why?)
        RArguments.setFunction(vFrame, function);
        FrameDescriptor envfd = envFrame.getFrameDescriptor();
        FrameDescriptor vfd = vFrame.getFrameDescriptor();
        // Copy existing bindings. Logically we want to clone the existing frame contents.
        // N.B. Since FrameDescriptors can be shared between frames, the descriptor may
        // contain slots that do not have values in the frame.
        int i = 0;
        for (; i < envfd.getSlots().size(); i++) {
            FrameSlot slot = envfd.getSlots().get(i);
            FrameSlotKind slotKind = slot.getKind();
            FrameSlot vFrameSlot = vfd.addFrameSlot(slot.getIdentifier(), slotKind);
            Object slotValue = envFrame.getValue(slot);
            if (slotValue != null) {
                try {
                    switch (slotKind) {
                        case Byte:
                            vFrame.setByte(vFrameSlot, (byte) slotValue);
                            break;
                        case Int:
                            vFrame.setInt(vFrameSlot, (int) slotValue);
                            break;
                        case Double:
                            vFrame.setDouble(vFrameSlot, (double) slotValue);
                            break;
                        case Object:
                            vFrame.setObject(vFrameSlot, slotValue);
                            break;
                        case Illegal:
                            break;
                        default:
                            throw new FrameSlotTypeException();
                    }
                } catch (FrameSlotTypeException ex) {
                    throw new RuntimeException("unexpected FrameSlot exception", ex);
                }
            }

        }
        Object result = runCall(callTarget, vFrame, false, false);
        if (result != null) {
            FrameDescriptor fd = vFrame.getFrameDescriptor();
            for (FrameSlot slot : fd.getSlots()) {
                if (slot.getKind() != FrameSlotKind.Illegal) {
                    // the put will take care of checking the slot type, so getValue is ok
                    Object value = vFrame.getValue(slot);
                    if (value != null) {
                        if (value instanceof RFunction) {
                            checkPatchRFunctionEnclosingFrame((RFunction) value, vFrame, envFrame);
                        }
                        envir.put(slot.getIdentifier().toString(), value);
                    }
                }
            }
        }
        if (result instanceof RFunction) {
            checkPatchRFunctionEnclosingFrame((RFunction) result, vFrame, envFrame);
        }
        return result;
    }

    private static void checkPatchRFunctionEnclosingFrame(RFunction func, Frame vFrame, MaterializedFrame envFrame) {
        if (func.getEnclosingFrame() == vFrame) {
            // this function's enclosing environment should be envFrame
            func.setEnclosingFrame(envFrame);
        }
    }

    public Object evalPromise(RPromise promise, VirtualFrame frame) throws RError {
        RootCallTarget callTarget = makeCallTarget((RNode) promise.getRep());
        return runCall(callTarget, frame, false, false);
    }

    public Object evalPromise(RPromise promise) throws RError {
        // have to do the full out eval
        try {
            return eval(lookupBuiltin("eval"), (RNode) promise.getRep(), promise.getEnv(), null);
        } catch (PutException ex) {
            // TODO a new, rather unlikely, error
            assert false;
            return null;
        }
    }

    private static Object parseAndEvalImpl(ANTLRStringStream stream, Source source, VirtualFrame frame, boolean printResult, boolean allowIncompleteSource) {
        try {
            RootCallTarget callTarget = makeCallTarget(parseToRNode(stream, source));
            Object result = runCall(callTarget, frame, printResult, true);
            return result;
        } catch (NoViableAltException | MismatchedTokenException e) {
            if (e.token.getType() == Token.EOF && allowIncompleteSource) {
                // the parser got stuck at the eof, request another line
                return INCOMPLETE_SOURCE;
            }
            String line = source.getCode(e.line);
            String message = "Error: unexpected '" + e.token.getText() + "' in \"" + line.substring(0, e.charPositionInLine + 1) + "\"";
            singleton.context.getConsoleHandler().println(source.getLineCount() == 1 ? message : (message + " (line " + e.line + ")"));
            return null;
        } catch (RError e) {
            singleton.context.getConsoleHandler().println(e.getMessage());
            return null;
        } catch (RecognitionException | RuntimeException e) {
            singleton.context.getConsoleHandler().println("Exception while parsing: " + e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parses a text stream into a Truffle AST.
     *
     * @param stream
     * @param source
     * @return the root node of the Truffle AST
     * @throws RecognitionException on parse error
     */
    private static RNode parseToRNode(ANTLRStringStream stream, Source source) throws RecognitionException {
        return transform(ParseUtil.parseAST(stream, source), REnvironment.globalEnv());
    }

    /**
     * Transforms an AST produced by the parser into a Truffle AST.
     *
     * @param astNode parser AST instance
     * @param environment the lexically enclosing environment that will be associated with top-level
     *            function definitions in {@code astNode}
     * @return the root node of the Truffle AST
     */
    private static RNode transform(ASTNode astNode, REnvironment environment) {
        RTruffleVisitor transform = new RTruffleVisitor(environment);
        RNode result = transform.transform(astNode);
        return result;
    }

    private static boolean traceMakeCallTarget;

    /**
     * Wraps the Truffle AST in {@code node} in an anonymous function and returns a
     * {@link RootCallTarget} for it. We define the
     * {@link com.oracle.truffle.r.runtime.env.REnvironment.FunctionDefinition} environment to have
     * the {@link REnvironment#emptyEnv()} as parent, so it is note scoped relative to any existing
     * environments, i.e. is truly anonymous.
     *
     * N.B. For certain expressions, there might be some value in enclosing the wrapper function in
     * a specific lexical scope. E.g., as a way to access names in the expression known to be
     * defined in that scope.
     *
     * @param body The AST for the body of the wrapper, i.e., the expression being evaluated.
     */
    @SlowPath
    private static RootCallTarget makeCallTarget(RNode body) {
        if (traceMakeCallTarget) {
            doTraceMakeCallTarget(body);
        }
        REnvironment.FunctionDefinition rootNodeEnvironment = new REnvironment.FunctionDefinition(REnvironment.emptyEnv());
        FunctionDefinitionNode rootNode = new FunctionDefinitionNode(null, rootNodeEnvironment, body, FormalArguments.NO_ARGS, "<wrapper>", true, true);
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        return callTarget;
    }

    private static void doTraceMakeCallTarget(RNode body) {
        String nodeClassName = body.getClass().getSimpleName();
        SourceSection ss = body.getSourceSection();
        String trace;
        if (ss == null) {
            if (body instanceof ConstantNode) {
                trace = ((ConstantNode) body).getValue().toString();
            } else {
                trace = "not constant/no source";
            }
        } else {
            trace = ss.toString();
        }
        RContext.getInstance().getConsoleHandler().printf("makeCallTarget: node: %s, %s%n", nodeClassName, trace);

    }

    /**
     * Execute {@code callTarget} in {@code frame}, optionally printing any result. N.B.
     * {@code callTarget.call} will create a new {@link VirtualFrame} called, say, {@code newFrame},
     * in which to execute the (anonymous) {@link FunctionDefinitionNode} associated with
     * {@code callTarget}. When execution reaches {@link FunctionDefinitionNode#execute},
     * {@code frame} will be accessible via {@code newFrame.getArguments()[0]}, and the execution
     * will continue using {@code frame}.
     */
    private static Object runCall(RootCallTarget callTarget, VirtualFrame frame, boolean printResult, boolean topLevel) {
        Object result = null;
        try {
            try {
                // FIXME: callTargets should only be called via Direct/IndirectCallNode
                result = callTarget.call(frame.materialize());
            } catch (ControlFlowException cfe) {
                throw RError.error(RError.Message.NO_LOOP_FOR_BREAK_NEXT);
            }
            if (printResult) {
                printResult(result);
            }
            reportWarnings(false);
        } catch (RError e) {
            if (topLevel) {
                singleton.printRError(e);
            } else {
                throw e;
            }
        } catch (Throwable e) {
            reportImplementationError(e);
        }
        return result;
    }

    @SlowPath
    private static void printResult(Object result) {
        if (RContext.isVisible()) {
            // TODO cache this
            RFunction function = (RFunction) REnvironment.baseEnv().get("print");
            function.getTarget().call(RArguments.create(function, new Object[]{result, RRuntime.asLogical(true)}));
        }
    }

    @SlowPath
    public void printRError(RError e) {
        String es = e.toString();
        if (!es.isEmpty()) {
            context.getConsoleHandler().printErrorln(e.toString());
        }
        reportWarnings(true);
    }

    @SlowPath
    private static void reportImplementationError(Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(out));
        singleton.context.getConsoleHandler().printErrorln(RRuntime.toString(out));
        // R suicide, unless, e.g., we are running units tests.
        // We don't call quit as the system is broken.
        if (singleton.crashOnFatalError) {
            Utils.exit(2);
        }
    }

    @SlowPath
    private static void reportWarnings(boolean inAddition) {
        List<String> evalWarnings = singleton.context.extractEvalWarnings();
        ConsoleHandler consoleHandler = singleton.context.getConsoleHandler();
        // GnuR outputs warnings to the stderr, so we do too
        if (evalWarnings != null && evalWarnings.size() > 0) {
            if (inAddition) {
                consoleHandler.printError("In addition: ");
            }
            if (evalWarnings.size() == 1) {
                consoleHandler.printErrorln("Warning message:");
                consoleHandler.printErrorln(evalWarnings.get(0));
            } else {
                consoleHandler.printErrorln("Warning messages:");
                for (int i = 0; i < evalWarnings.size(); i++) {
                    consoleHandler.printErrorln((i + 1) + ":");
                    consoleHandler.printErrorln("  " + evalWarnings.get(i));
                }
            }
        }
    }

}
