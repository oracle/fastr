/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.instrumentation.StandardTags.CallTag;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.launcher.RCommand;
import com.oracle.truffle.r.launcher.RscriptCommand;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinRootNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.ComplexVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.helpers.DebugHandling;
import com.oracle.truffle.r.nodes.builtin.helpers.TraceHandling;
import com.oracle.truffle.r.nodes.control.AbstractLoopNode;
import com.oracle.truffle.r.nodes.control.BlockNode;
import com.oracle.truffle.r.nodes.control.IfNode;
import com.oracle.truffle.r.nodes.control.ReplacementDispatchNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags.FunctionBodyBlockTag;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags.LoopTag;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ShowCallerOf;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess;
import com.oracle.truffle.r.runtime.RootBodyNode;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.Engine;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RInstrumentableNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * This class contains functions that need access to actual implementation classes but which are
 * used in places where there is not dependency on the node project.
 */
class RRuntimeASTAccessImpl implements RRuntimeASTAccess {

    @Override
    public Object createLanguageElement(RSyntaxElement element) {
        return RASTUtils.createLanguageElement(element);
    }

    @Override
    public Object callback(RFunction f, Object[] args) {
        boolean gd = RContext.getInstance().stateInstrumentation.setDebugGloballyDisabled(true);
        try {
            return RContext.getEngine().evalFunction(f, null, null, true, null, args);
        } finally {
            RContext.getInstance().stateInstrumentation.setDebugGloballyDisabled(gd);
        }
    }

    @Override
    public Object forcePromise(String identifier, Object val) {
        if (val instanceof RPromise) {
            return ReadVariableNode.evalPromiseSlowPathWithName(identifier, null, (RPromise) val);
        } else {
            return val;
        }
    }

    @Override
    public ArgumentsSignature getArgumentsSignature(RFunction f) {
        return ((RRootNode) f.getRootNode()).getSignature();
    }

    @Override
    public Object[] getBuiltinDefaultParameterValues(RFunction f) {
        assert f.isBuiltin();
        return ((RBuiltinRootNode) f.getRootNode()).getBuiltinNode().getDefaultParameterValues();
    }

    @Override
    public void setFunctionName(RootNode node, String name) {
        ((FunctionDefinitionNode) node).setName(name);
    }

    @Override
    public Engine createEngine(RContext context) {
        return REngine.create(context);
    }

    @Override
    public RPairList getSyntaxCaller(RCaller rl) {
        RCaller call = rl;
        while (call.isPromise()) {
            call = call.getParent();
        }
        RSyntaxElement syntaxNode = call.getSyntaxNode();
        return RDataFactory.createLanguage(getOrCreateLanguageClosure(((RSyntaxNode) syntaxNode).asRNode()));
    }

    private static RBaseNode checkBuiltin(RBaseNode bn) {
        if (bn instanceof RBuiltinNode) {
            RSyntaxElement se = ((RBuiltinNode) bn).getOriginalCall();
            if (se == null) {
                /*
                 * TODO Ideally this never happens but do.call creates trees that make finding the
                 * original call impossible.
                 */
                return null;
            } else {
                return (RBaseNode) se;
            }
        } else {
            return bn;
        }
    }

    @Override
    public String getCallerSource(RPairList rl) {
        assert rl.isLanguage();
        // This checks for the specific structure of replacements
        RPairList replacement = ReplacementDispatchNode.getRLanguage(rl);
        RPairList elem = replacement == null ? rl : replacement;
        String string = RDeparse.deparse(elem, RDeparse.DEFAULT_CUTOFF, true, RDeparse.KEEPINTEGER, -1);
        return string.split("\n")[0];
    }

    /**
     * This is where all the complexity in locating the caller for an error/warning is located. If a
     * specific node is provided as {@code call}, then this node is used as the caller context for
     * the error message. Other cases are represented by {@link RError#NO_CALLER},
     * {@link RError#SHOW_CALLER} and {@link RError#SHOW_CALLER2}.
     */
    @Override
    public Object findCaller(RBaseNode call) {
        if (call == RError.NO_CALLER) {
            return RNull.instance;
        } else if (call == RError.SHOW_CALLER) {
            Frame frame = Utils.getActualCurrentFrame();
            return findCallerFromFrame(frame);
        } else if (call == RError.SHOW_CALLER2) {
            Frame frame = Utils.getActualCurrentFrame();
            if (frame != null && RArguments.isRFrame(frame)) {
                RCaller parent = RArguments.getCall(frame);
                frame = Utils.getCallerFrame(parent, FrameAccess.READ_ONLY);
            }
            return findCallerFromFrame(frame);
        } else if (call instanceof ShowCallerOf) {
            Frame frame = Utils.getActualCurrentFrame();

            boolean match = false;
            String name = ((ShowCallerOf) call).getCallerOf();
            Frame f = frame;
            while (f != null && RArguments.isRFrame(f)) {
                RCaller parent = RArguments.getCall(f);
                if (parent.isValidCaller()) {
                    RSyntaxElement syntaxNode = parent.getSyntaxNode();
                    if (syntaxNode instanceof RSyntaxCall) {
                        RSyntaxElement syntaxElement = ((RSyntaxCall) syntaxNode).getSyntaxLHS();
                        if (syntaxElement instanceof RSyntaxLookup) {
                            if (match) {
                                return findCallerFromFrame(f);
                            }
                            if (name.equals(((RSyntaxLookup) syntaxElement).getIdentifier())) {
                                match = true;
                            }
                        }
                    }
                }
                f = Utils.getCallerFrame(parent, FrameAccess.READ_ONLY);
            }

            return findCallerFromFrame(frame);
        } else {
            RBaseNode originalCall = checkBuiltin(call);
            if (originalCall != null && originalCall.checkasRSyntaxNode() != null) {
                return RDataFactory.createLanguage(getOrCreateLanguageClosure(originalCall.asRSyntaxNode().asRNode()));
            } else {
                // See checkBuiltin. Also some RBaseNode subclasses do not provide an RSyntaxNode.
                Frame frame = Utils.getActualCurrentFrame();
                return findCallerFromFrame(frame);
            }
        }
    }

    private Object findCallerFromFrame(Frame frame) {
        if (frame != null && RArguments.isRFrame(frame)) {
            RCaller caller = RArguments.getCall(frame);
            while (caller.isPromise()) {
                caller = caller.getParent();
            }
            if (caller != null && caller.isValidCaller()) {
                /*
                 * This is where we need to ensure that we have an RLanguage object with a rep that
                 * is an RSyntaxNode.
                 */
                return getSyntaxCaller(caller);
            }
        }
        return RNull.instance;
    }

    @Override
    public boolean isFunctionDefinitionNode(Node node) {
        return node instanceof FunctionDefinitionNode;
    }

    @Override
    public void traceAllFunctions() {
        TraceHandling.traceAllFunctions();
    }

    @Override
    public RSyntaxNode unwrapPromiseRep(RPromise promise) {
        return RASTUtils.unwrap(promise.getRep()).asRSyntaxNode();
    }

    @Override
    public boolean isTaggedWith(Node node, Class<?> tag) {
        if (node instanceof RootBodyNode) {
            return (tag == RootTag.class);
        }
        if (node instanceof RootNode) {
            // roots don't have any tags
            return false;
        }
        if (!(node instanceof RSyntaxNode)) {
            return false;
        }
        if (isInternalChild(node)) {
            return false;
        }
        if (tag == CallTag.class) {
            return node instanceof RCallNode;
        }
        if (tag == FunctionBodyBlockTag.class) {
            return node instanceof BlockNode && ((BlockNode) node).unwrapParent() instanceof RootBodyNode;
        }
        if (tag == LoopTag.class) {
            return node instanceof AbstractLoopNode;
        }
        if (tag == StatementTag.class) {
            if (node instanceof BlockNode) {
                // so that the stepping location is not the block itself, but the first statement in
                // the block, note that the FastR's own debugging and tracing mechanism uses
                // FunctionBodyBlockTag to recognize function bodies.
                return false;
            }
            // How to recognize statement from some node inside a statement (e.g. expression)?
            Node parent = ((RInstrumentableNode) node).unwrapParent();
            if (parent instanceof BlockNode) {
                // It's in a block of statements
                return true;
            } else {
                // single statement block: as function body, if/else body, loop body
                // note: RepeatingNode is not a RSyntaxElement but the body of a loop is
                // under the repeating node !
                return parent instanceof RootBodyNode || parent instanceof IfNode || AbstractLoopNode.isLoopBody(node);
            }
        }
        // TODO: ExpressionTag: (!statement && !loop && !if && !call && !root)??
        return false;
    }

    private static boolean isInternalChild(Node node) {
        Node parent = node.getParent();
        while (parent != null) {
            if (parent instanceof InternalRSyntaxNodeChildren) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    @Override
    @TruffleBoundary
    public boolean enableDebug(RFunction func, boolean once) {
        return DebugHandling.enableDebug(func, "", RNull.instance, once, false);
    }

    @Override
    @TruffleBoundary
    public boolean isDebugged(RFunction func) {
        return DebugHandling.isDebugged(func);
    }

    @Override
    @TruffleBoundary
    public boolean disableDebug(RFunction func) {
        return DebugHandling.undebug(func);
    }

    @Override
    public Object rcommandMain(String[] args, String[] env, boolean intern) {
        IORedirect redirect = handleIORedirect(args, intern);
        Object result = RCommand.doMain(redirect.args, env, redirect.in, redirect.out, redirect.err);
        return redirect.getInternResult(result);
    }

    @Override
    public Object rscriptMain(String[] args, String[] env, boolean intern) {
        IORedirect redirect = handleIORedirect(args, intern);
        // TODO argument parsing can fail with ExitException, which needs to be handled correctly in
        // nested context
        Object result = RscriptCommand.doMain(redirect.args, env, redirect.in, redirect.out, redirect.err);
        return redirect.getInternResult(result);
    }

    private static final class IORedirect {
        private final InputStream in;
        private final OutputStream err;
        private final OutputStream out;
        private final String[] args;
        private final boolean intern;

        private IORedirect(InputStream in, OutputStream out, OutputStream err, String[] args, boolean intern) {
            this.in = in;
            this.out = out;
            this.err = err;
            this.args = args;
            this.intern = intern;
        }

        private Object getInternResult(Object result) {
            if (intern) {
                int status = (int) result;
                ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
                String s = new String(bos.toByteArray());
                RStringVector sresult;
                if (s.length() == 0) {
                    sresult = RDataFactory.createEmptyStringVector();
                } else {
                    String[] lines = s.split("\n");
                    sresult = RDataFactory.createStringVector(lines, RDataFactory.COMPLETE_VECTOR);
                }
                if (status != 0) {
                    setResultAttr(status, sresult);
                }
                return sresult;
            } else {
                return result;
            }
        }

        @TruffleBoundary
        private static void setResultAttr(int status, RStringVector sresult) {
            sresult.setAttr("status", RDataFactory.createIntVectorFromScalar(status));
        }
    }

    private static IORedirect handleIORedirect(String[] args, boolean intern) {
        /*
         * This code is primarily intended to handle the "system" .Internal so the possible I/O
         * redirects are taken from the system/system2 R code. N.B. stdout redirection is never set
         * if "intern == true. Both input and output can be redirected to /dev/null.
         */
        InputStream in = System.in;
        OutputStream out = System.out;
        OutputStream err = System.err;
        ArrayList<String> newArgsList = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("<")) {
                String file;
                if (i < args.length - 1) {
                    file = Utils.tildeExpand(Utils.unShQuote(args[i + 1]));
                } else {
                    throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, "redirect missing");
                }
                try {
                    in = new FileInputStream(file);
                } catch (IOException ex) {
                    throw RError.error(RError.NO_CALLER, RError.Message.NO_SUCH_FILE, file);
                }
                arg = null;
                i++;
            } else if (arg.startsWith("2>")) {
                if (arg.equals("2>&1")) {
                    // happens anyway
                } else {
                    assert !intern;
                    throw RError.nyi(RError.NO_CALLER, "stderr redirect");
                }
                arg = null;
            } else if (arg.startsWith(">")) {
                assert !intern;
                throw RError.nyi(RError.NO_CALLER, "stdout redirect");
            }
            if (arg != null) {
                newArgsList.add(arg);
            }
            i++;
        }
        String[] newArgs;
        if (newArgsList.size() == args.length) {
            newArgs = args;
        } else {
            newArgs = new String[newArgsList.size()];
            newArgsList.toArray(newArgs);
        }
        // to implement intern, we create a ByteArryOutputStream to capture the output
        if (intern) {
            out = new ByteArrayOutputStream();
        }
        return new IORedirect(in, out, err, newArgs, intern);
    }

    @Override
    public String encodeDouble(double x) {
        return DoubleVectorPrinter.encodeReal(x);
    }

    @Override
    public String encodeDouble(double x, int digits) {
        return DoubleVectorPrinter.encodeReal(x, digits);
    }

    @Override
    public String encodeComplex(RComplex x) {
        return ComplexVectorPrinter.encodeComplex(x);
    }

    @Override
    public String encodeComplex(RComplex x, int digits) {
        return ComplexVectorPrinter.encodeComplex(x, digits);
    }

    @Override
    public void checkDebugRequest(RFunction func) {
        RInstrumentation.checkDebugRequested(func);
    }

    @Override
    public Class<? extends TruffleRLanguage> getTruffleRLanguage() {
        return TruffleRLanguageImpl.class;
    }

    @Override
    public RAbstractStringVector getClassHierarchy(RAttributable value) {
        return ClassHierarchyNode.getClassHierarchy(value);
    }

    @Override
    public RContext getCurrentContext() {
        return TruffleRLanguageImpl.getCurrentContext();
    }

    private static Closure getOrCreateLanguageClosure(RNode expr) {
        CompilerAsserts.neverPartOfCompilation();
        return RContext.getInstance().languageClosureCache.getOrCreateLanguageClosure(expr);
    }
}
