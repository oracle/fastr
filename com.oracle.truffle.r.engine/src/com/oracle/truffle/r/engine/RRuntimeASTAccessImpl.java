/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.engine.shell.RCommand;
import com.oracle.truffle.r.engine.shell.RscriptCommand;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
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
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionExpressionNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.Engine;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.Argument;
import com.oracle.truffle.r.runtime.nodes.RInstrumentableNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Implementation of {@link RRuntimeASTAccess}.
 *
 * A note on the "list" aspects of {@link RLanguage}, specified by {@link RAbstractContainer}. In
 * GnuR a language element (LANGSXP) is represented as a pairlist, so the length of the language
 * element is defined to be the length of the pairlist. The goal of this implementation is to
 * emulate the behavior of GnuR by walking the AST.
 *
 * The nodes we are interested in are {@link ReadVariableNode} (symbols), {@link ConstantNode}
 * (constants) and {@link RCallNode} etc., (calls). However, the nodes that are not (but should be)
 * represented as calls, e.g. {@link IfNode} have to be handled specially.
 *
 * Since the AST is a final field (and we assert) immutable in its syntactic essence, we can cache
 * information such as the length here. A Truffle AST has many nodes that are not part of the
 * syntactic essence and we ignore these.
 *
 * This implementation necessarily has to use a lot of {@code instanceof} checks on the node class.
 * However, it is not important enough to warrant refactoring as an {@link RNode} method, (cf
 * deparse). TODO reconsider this.
 *
 * Some examples:
 *
 * <pre>
 * length(quote(f()) == 1
 * length(quote(f(a)) == 2
 * length(quote(a + b)) == 3
 * length(quote(a + f(b))) == 3
 * </pre>
 *
 * Note the last example in particular which shows that the length is not computed from the
 * flattened tree. Rather indexing the third element would produce another language element of
 * length 2.
 */
class RRuntimeASTAccessImpl implements RRuntimeASTAccess {

    private static Object getIntrinsicValue(Object result) {
        if (result instanceof RSyntaxConstant) {
            return ((RSyntaxConstant) result).getValue();
        } else if (result instanceof RSyntaxLookup) {
            return RDataFactory.createSymbolInterned(((RSyntaxLookup) result).getIdentifier());
        } else {
            assert result instanceof RSyntaxCall || result instanceof RSyntaxFunction : result.getClass();
            return RDataFactory.createLanguage(((RSyntaxNode) result).asRNode());
        }
    }

    @TruffleBoundary
    @Override
    public RLanguage.RepType getRepType(RLanguage rl) {
        RSyntaxElement s = RASTUtils.unwrap(rl.getRep()).asRSyntaxNode();

        if (s instanceof RSyntaxCall) {
            return RLanguage.RepType.CALL;
        } else if (s instanceof RSyntaxFunction) {
            return RLanguage.RepType.FUNCTION;
        } else {
            throw RInternalError.shouldNotReachHere("unexpected type: " + s.getClass());
        }
    }

    @TruffleBoundary
    @Override
    public int getLength(RLanguage rl) {
        RSyntaxElement s = RASTUtils.unwrap(rl.getRep()).asRSyntaxNode();

        if (s instanceof RSyntaxCall) {
            return ((RSyntaxCall) s).getSyntaxSignature().getLength() + 1;
        } else if (s instanceof RSyntaxFunction) {
            return 4;
        } else {
            /*
             * We do not expect RSyntaxConstant and RSyntaxLookup here (see getDataAtAsObject).
             */
            throw RInternalError.shouldNotReachHere("unexpected type: " + s.getClass());
        }
    }

    @TruffleBoundary
    @Override
    public Object getDataAtAsObject(RLanguage rl, final int index) {
        // index has already been range checked based on getLength
        RSyntaxElement s = RASTUtils.unwrap(rl.getRep()).asRSyntaxNode();

        RSyntaxElement result;
        if (s instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) s;
            if (index == 0) {
                result = call.getSyntaxLHS();
            } else {
                result = call.getSyntaxArguments()[index - 1];
                if (result == null) {
                    result = RSyntaxLookup.createDummyLookup(RSyntaxNode.LAZY_DEPARSE, "", false);
                }
            }
        } else if (s instanceof RSyntaxFunction) {
            switch (index) {
                case 0:
                    result = RSyntaxLookup.createDummyLookup(null, "function", true);
                    break;
                case 1:
                    ArgumentsSignature sig = ((RSyntaxFunction) s).getSyntaxSignature();
                    RSyntaxElement[] defaults = ((RSyntaxFunction) s).getSyntaxArgumentDefaults();

                    Object list = RNull.instance;
                    for (int i = sig.getLength() - 1; i >= 0; i--) {
                        list = RDataFactory.createPairList(defaults[i] == null ? RSymbol.MISSING : getIntrinsicValue(defaults[i]), list,
                                        RDataFactory.createSymbolInterned(sig.getName(i)));
                    }
                    return list;
                case 2:
                    result = ((RSyntaxFunction) s).getSyntaxBody();
                    break;
                case 3:
                    // srcref
                    return RSource.createSrcRef(s.getLazySourceSection());
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        } else {
            /*
             * We do not expect RSyntaxConstant and RSyntaxLookup here: RSyntaxConstant should have
             * been converted to the constant value, and RSyntaxLookup should have been converted to
             * an RSymbol (see below).
             */
            throw RInternalError.shouldNotReachHere("unexpected type: " + s.getClass());
        }

        /*
         * Constants and lookups are converted to their intrinsic value:
         */
        return getIntrinsicValue(result);
    }

    @Override
    @TruffleBoundary
    public Object fromList(RList list, RLanguage.RepType repType) {
        int length = list.getLength();
        if (length == 0) {
            return RNull.instance;
        } else if (repType == RLanguage.RepType.CALL) {
            RStringVector formals = list.getNames();
            RSyntaxNode[] arguments = new RSyntaxNode[length - 1];
            String[] sigNames = new String[arguments.length];
            for (int i = 1; i < length; i++) {
                arguments[i - 1] = (RSyntaxNode) unwrapToRNode(list.getDataAtAsObject(i));
                String formal = formals == null ? null : formals.getDataAt(i);
                sigNames[i - 1] = formal != null && formal.length() > 0 ? formal : null;
            }
            RNode fn = unwrapToRNode(list.getDataAtAsObject(0));
            RLanguage result = RDataFactory.createLanguage(RASTUtils.createCall(fn, false, ArgumentsSignature.get(sigNames), arguments).asRNode());
            if (formals != null && formals.getLength() > 0 && formals.getDataAt(0).length() > 0) {
                result.setCallLHSName(formals.getDataAt(0));
            }
            return addAttributes(result, list);
        } else if (repType == RLanguage.RepType.FUNCTION) {
            RList argsList = (RList) list.getDataAt(1);
            RSyntaxNode body = (RSyntaxNode) unwrapToRNode(list.getDataAt(2));
            List<Argument<RSyntaxNode>> resArgs = new LinkedList<>();
            RStringVector argsNames = argsList.getNames();
            for (int i = 0; i < argsList.getLength(); i++) {
                String argName = argsNames == null ? null : argsNames.getDataAt(i);
                Object argVal = argsList.getDataAt(i);
                Argument<RSyntaxNode> arg = RCodeBuilder.argument(RSyntaxNode.LAZY_DEPARSE, argName, argVal == RSymbol.MISSING ? null : (RSyntaxNode) unwrapToRNode(argVal));
                resArgs.add(arg);
            }
            RootCallTarget rootCallTarget = new RASTBuilder().rootFunction(RSyntaxNode.LAZY_DEPARSE, resArgs, body, null);
            FunctionExpressionNode fnExprNode = FunctionExpressionNode.create(RSyntaxNode.LAZY_DEPARSE, rootCallTarget);
            RLanguage result = RDataFactory.createLanguage(fnExprNode);
            return addAttributes(result, list);
        } else {
            throw RInternalError.shouldNotReachHere("unexpected type");
        }
    }

    private static Object addAttributes(RAttributable result, RList list) {
        DynamicObject attrs = list.getAttributes();
        if (attrs != null && !attrs.isEmpty()) {
            result.initAttributes(RAttributesLayout.copy(attrs));
        }
        return result;
    }

    private static RNode unwrapToRNode(Object objArg) {
        Object obj = objArg;
        // obj is RSymbol or a primitive value.
        // A symbol needs to be converted back to a ReadVariableNode
        if (obj instanceof RLanguage) {
            return (RNode) RASTUtils.unwrap(((RLanguage) obj).getRep());
        } else if (obj instanceof RSymbol) {
            return ReadVariableNode.create(((RSymbol) obj).getName());
        } else {
            return ConstantNode.create(obj);
        }
    }

    @Override
    public RList asList(RLanguage rl) {
        Object[] data = new Object[getLength(rl)];
        for (int i = 0; i < data.length; i++) {
            data[i] = getDataAtAsObject(rl, i);
        }
        RStringVector names = getNames(rl);
        if (names == null) {
            return RDataFactory.createList(data);
        } else {
            return RDataFactory.createList(data, names);
        }
    }

    @Override
    @TruffleBoundary
    public RStringVector getNames(RLanguage rl) {
        RBaseNode node = rl.getRep();
        if (node instanceof RCallNode) {
            RCallNode call = (RCallNode) node;
            /*
             * If the function or any argument has a name, then all arguments (and the function) are
             * given names, with unnamed arguments getting "". However, if no arguments have names,
             * the result is NULL (null)
             */
            boolean hasName = false;
            String functionName = "";
            if (rl.getCallLHSName() != null) {
                hasName = true;
                functionName = rl.getCallLHSName();
            }
            ArgumentsSignature sig = call.getSyntaxSignature();
            if (!hasName) {
                for (int i = 0; i < sig.getLength(); i++) {
                    if (sig.getName(i) != null) {
                        hasName = true;
                        break;
                    }
                }
            }
            if (!hasName) {
                return null;
            }
            String[] data = new String[sig.getLength() + 1];
            data[0] = functionName; // function
            for (int i = 0; i < sig.getLength(); i++) {
                String name = sig.getName(i);
                data[i + 1] = name == null ? "" : name;
            }
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        } else {
            return null;
        }
    }

    @Override
    @TruffleBoundary
    public void setNames(RLanguage rl, RStringVector names) {
        RNode node = (RNode) rl.getRep();
        if (node instanceof RCallNode) {
            RCallNode call = (RCallNode) node;
            Arguments<RSyntaxNode> args = call.getArguments();
            ArgumentsSignature sig = args.getSignature();
            String[] newNames = new String[sig.getLength()];
            int argNamesLength = names.getLength() - 1;
            if (argNamesLength > sig.getLength()) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.ATTRIBUTE_VECTOR_SAME_LENGTH, "names", names.getLength(), sig.getLength() + 1);
            }
            for (int i = 0, j = 1; i < sig.getLength() && j <= argNamesLength; i++, j++) {
                newNames[i] = names.getDataAt(j);
            }
            // copying is already handled by RShareable
            rl.setRep(RCallNode.createCall(RSyntaxNode.INTERNAL, ((RCallNode) node).getFunction(), ArgumentsSignature.get(newNames), args.getArguments()));
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    public Object callback(RFunction f, Object[] args) {
        boolean gd = RContext.getInstance().stateInstrumentation.setDebugGloballyDisabled(true);
        try {
            return RContext.getEngine().evalFunction(f, null, null, null, args);
        } catch (ReturnException ex) {
            // cannot throw return exceptions further up.
            return ex.getResult();
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
    public RLanguage getSyntaxCaller(RCaller rl) {
        RCaller call = rl;
        while (call.isPromise()) {
            call = call.getParent();
        }
        RSyntaxElement syntaxNode = call.getSyntaxNode();
        return RDataFactory.createLanguage(((RSyntaxNode) syntaxNode).asRNode());
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
    public String getCallerSource(RLanguage rl) {
        // This checks for the specific structure of replacements
        RLanguage replacement = ReplacementDispatchNode.getRLanguage(rl);
        RLanguage elem = replacement == null ? rl : replacement;
        String string = RDeparse.deparse(elem, RDeparse.DEFAULT_Cutoff, true, RDeparse.KEEPINTEGER, -1);
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
                frame = Utils.getCallerFrame(frame, FrameAccess.READ_ONLY);
            }
            return findCallerFromFrame(frame);
        } else {
            RBaseNode originalCall = checkBuiltin(call);
            if (originalCall != null && originalCall.checkasRSyntaxNode() != null) {
                return RDataFactory.createLanguage(originalCall.asRSyntaxNode().asRNode());
            } else {
                // See checkBuiltin. Also some RBaseNode subclasses do not provide an RSyntaxNode.
                Frame frame = Utils.getActualCurrentFrame();
                return findCallerFromFrame(frame);
            }
        }
    }

    @Override
    public RSyntaxFunction getSyntaxFunction(RFunction f) {
        return (FunctionDefinitionNode) f.getTarget().getRootNode();
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
        if (!(node instanceof RSyntaxNode)) {
            return false;
        }
        if (isInternalChild(node)) {
            return false;
        }
        String className = tag.getSimpleName();
        switch (className) {
            case "CallTag":
                return node instanceof RCallNode;

            case "StatementTag": {
                Node parent = ((RInstrumentableNode) node).unwrapParent();
                if (node instanceof BlockNode) {
                    // TODO we may reconsider this
                    return false;
                }
                // Most likely
                if (parent instanceof BlockNode) {
                    return true;
                } else {
                    // single statement block, variable parent
                    return parent instanceof FunctionDefinitionNode || parent instanceof IfNode || parent instanceof AbstractLoopNode;
                }
            }

            case "RootTag": {
                Node parent = ((RInstrumentableNode) node).unwrapParent();
                return parent instanceof FunctionDefinitionNode;
            }

            case "LoopTag":
                return node instanceof AbstractLoopNode;

            default:
                return false;
        }
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
    public boolean enableDebug(RFunction func, boolean once) {
        return DebugHandling.enableDebug(func, "", RNull.instance, once, false);
    }

    @Override
    public boolean isDebugged(RFunction func) {
        return DebugHandling.isDebugged(func);
    }

    @Override
    public boolean disableDebug(RFunction func) {
        return DebugHandling.undebug(func);
    }

    @Override
    public Object rcommandMain(String[] args, String[] env, boolean intern) {
        IORedirect redirect = handleIORedirect(args, intern);
        Object result = RCommand.doMain(redirect.args, env, false, redirect.in, redirect.out);
        return redirect.getInternResult(result);
    }

    @Override
    public Object rscriptMain(String[] args, String[] env, boolean intern) {
        IORedirect redirect = handleIORedirect(args, intern);
        // TODO argument parsing can fail with ExitException, which needs to be handled correctly in
        // nested context
        Object result = RscriptCommand.doMain(redirect.args, env, false, redirect.in, redirect.out);
        return redirect.getInternResult(result);
    }

    private static final class IORedirect {
        private final InputStream in;
        private final OutputStream out;
        private final String[] args;
        private final boolean intern;

        private IORedirect(InputStream in, OutputStream out, String[] args, boolean intern) {
            this.in = in;
            this.out = out;
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
                arg = null;
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
        return new IORedirect(in, out, newArgs, intern);
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

    @SuppressWarnings("rawtypes")
    @Override
    public Class<? extends TruffleLanguage> getTruffleRLanguage() {
        return TruffleRLanguage.class;
    }
}
