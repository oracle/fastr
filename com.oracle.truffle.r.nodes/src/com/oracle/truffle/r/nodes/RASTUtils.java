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
package com.oracle.truffle.r.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.ReadVariadicComponentNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.RCallSpecialNode;
import com.oracle.truffle.r.nodes.function.SaveArgumentsNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentBaseNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RInstrumentableNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * A collection of useful methods for working with {@code AST} instances.
 */
public final class RASTUtils {

    private RASTUtils() {
        // no instances
    }

    /**
     * Central location for all node cloning operations, in preference to {@link NodeUtil#cloneNode}
     * .
     */
    public static <T extends RBaseNode> T cloneNode(T node) {
        // TODO: use RASTBuilder here as well?
        return NodeUtil.cloneNode(node);
    }

    /**
     * Removes any {@link WrapArgumentNode} or {@link WrapperNode}.
     */
    @TruffleBoundary
    public static RBaseNode unwrap(Object node) {
        if (node instanceof WrapArgumentBaseNode) {
            return unwrap(((WrapArgumentBaseNode) node).getOperand());
        } else if (node instanceof com.oracle.truffle.r.runtime.nodes.RInstrumentableNode) {
            return ((RInstrumentableNode) node).unwrap();
        } else {
            return (RBaseNode) node;
        }
    }

    @TruffleBoundary
    public static Node unwrapParent(Node node) {
        Node parent = node.getParent();
        if (parent instanceof WrapperNode) {
            return parent.getParent();
        } else {
            return parent;
        }
    }

    @TruffleBoundary
    public static RSyntaxElement getOriginalCall(Node node) {
        Node p = node.getParent();
        while (p != null) {
            if (p instanceof RBuiltinNode) {
                return ((RBuiltinNode) p).getOriginalCall();
            }
            p = p.getParent();
        }
        return null;
    }

    public static RSyntaxNode[] asSyntaxNodes(RNode[] nodes) {
        RSyntaxNode[] result = new RSyntaxNode[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            result[i] = nodes[i] == null ? null : nodes[i].asRSyntaxNode();
        }
        return result;
    }

    public static boolean isLookup(RBaseNode node, String identifier) {
        RSyntaxNode element = node.asRSyntaxNode();
        return element instanceof RSyntaxLookup && identifier.equals(((RSyntaxLookup) element).getIdentifier());
    }

    /**
     * Handles constants and symbols as special cases as required by R: create symbols for simple
     * variables and actual values for constants.
     */
    @TruffleBoundary
    public static Object createLanguageElement(RSyntaxElement element) {
        assert element != null;
        if (element instanceof RSyntaxConstant) {
            Object value = ((RSyntaxConstant) element).getValue();
            if (value == RMissing.instance) {
                // special case which GnuR handles as an unnamed symbol
                return RSymbol.MISSING;
            }
            return value;
        } else if (element instanceof RSyntaxLookup) {
            String id = ((RSyntaxLookup) element).getIdentifier();
            assert Utils.isInterned(id);
            return RDataFactory.createSymbol(id);
        } else {
            assert element instanceof RSyntaxCall || element instanceof RSyntaxFunction;
            return RDataFactory.createLanguage(Closure.createLanguageClosure(((RSyntaxNode) element).asRNode()));
        }
    }

    /**
     * Creates an {@link RSymbol} from a {@link ReadVariableNode} o
     * {@link ReadVariadicComponentNode}.
     */
    @TruffleBoundary
    public static RSymbol createRSymbol(Node readVariableNode) {
        if (readVariableNode instanceof ReadVariadicComponentNode) {
            ReadVariadicComponentNode rvcn = (ReadVariadicComponentNode) readVariableNode;
            return RDataFactory.createSymbolInterned(rvcn.getPrintForm());
        } else {
            String id = ((ReadVariableNode) readVariableNode).getIdentifier();
            assert Utils.isInterned(id);
            return RDataFactory.createSymbol(id);
        }
    }

    /**
     * Create an {@link RNode} from a runtime value.
     */
    @TruffleBoundary
    public static RBaseNode createNodeForValue(Object value) {
        if (value instanceof RNode) {
            return (RNode) value;
        } else if (value instanceof RPromise) {
            RPromise promise = (RPromise) value;
            RNode promiseRep = (RNode) unwrap(((RPromise) value).getRep());
            if (promiseRep instanceof VarArgNode) {
                VarArgNode varArgNode = (VarArgNode) promiseRep;
                RPromise varArgPromise = (RPromise) varArgNode.execute(promise.getFrame());
                Node unwrappedRep = unwrap(varArgPromise.getRep());
                if (unwrappedRep instanceof ConstantNode) {
                    return (ConstantNode) unwrappedRep;
                } else {
                    // this is for the return value is supposed to be of the form "..N" to represent
                    // unexpanded component of ..., as for example in:
                    // f1<-function(...) match.call(expand.dots=FALSE);
                    // f2<-function(...) f1(...); f2(a)
                    return null;
                }
            }
            return RASTUtils.cloneNode(promiseRep);
        } else {
            return createNodeForRValue(value);
        }
    }

    @TruffleBoundary
    public static RBaseNode createNodeForRValue(Object value) {
        if (value instanceof RSymbol) {
            RSymbol symbol = (RSymbol) value;
            if (symbol.isMissing()) {
                return RContext.getASTBuilder().constant(RSyntaxNode.SOURCE_UNAVAILABLE, REmpty.instance).asRNode();
            } else {
                return RContext.getASTBuilder().lookup(RSyntaxNode.SOURCE_UNAVAILABLE, symbol.getName(), false).asRNode();
            }
        } else if (value instanceof RLanguage) {
            return RASTUtils.cloneNode(((RLanguage) value).getRep());
        } else {
            assert value instanceof String || value instanceof Integer || value instanceof Double || value instanceof Byte || value instanceof TruffleObject;
            return ConstantNode.create(value);
        }
    }

    @TruffleBoundary
    public static RSyntaxNode createSyntaxNodeForRValue(Object value) {
        if (value instanceof RSymbol) {
            RSymbol symbol = (RSymbol) value;
            if (symbol.isMissing()) {
                return RContext.getASTBuilder().constant(RSyntaxNode.SOURCE_UNAVAILABLE, REmpty.instance);
            } else {
                return RContext.getASTBuilder().lookup(RSyntaxNode.SOURCE_UNAVAILABLE, symbol.getName(), false);
            }
        } else if (value instanceof RLanguage) {
            return RContext.getASTBuilder().process(((RLanguage) value).getSyntaxElement());
        } else {
            assert value instanceof String || value instanceof Integer || value instanceof Double || value instanceof Byte || value instanceof TruffleObject;
            return ConstantNode.create(value);
        }
    }

    /**
     * Create an {@link RCallNode}. Where {@code fn} is either a:
     * <ul>
     * <li>{@link RFunction}\
     * <li>{@code RNode}</li>
     * </ul>
     */
    @TruffleBoundary
    public static RSyntaxNode createCall(Object fn, boolean sourceUnavailable, ArgumentsSignature signature, RSyntaxNode... arguments) {
        RNode fnNode;
        if (fn instanceof RFunction) {
            fnNode = ConstantNode.create(fn);
        } else {
            fnNode = (RNode) unwrap(fn);
        }
        SourceSection sourceSection = sourceUnavailable ? RSyntaxNode.SOURCE_UNAVAILABLE : RSyntaxNode.LAZY_DEPARSE;
        return RCallSpecialNode.createCall(sourceSection, fnNode, signature, arguments);
    }

    @TruffleBoundary
    public static Object createFormals(RFunction fun) {
        if (fun.isBuiltin()) {
            return RNull.instance;
        }
        FunctionDefinitionNode fdNode = (FunctionDefinitionNode) fun.getTarget().getRootNode();
        FormalArguments formalArgs = fdNode.getFormalArguments();
        Object succ = RNull.instance;
        for (int i = formalArgs.getSignature().getLength() - 1; i >= 0; i--) {
            RNode argument = formalArgs.getDefaultArgument(i);
            Object lang = argument == null ? RSymbol.MISSING : RASTUtils.createLanguageElement(argument.asRSyntaxNode());
            RSymbol name = RDataFactory.createSymbol(formalArgs.getSignature().getName(i));
            succ = RDataFactory.createPairList(lang, succ, name);
        }
        return succ;
    }

    @TruffleBoundary
    public static void modifyFunction(RFunction fun, Object newBody, Object newFormals, MaterializedFrame newEnv) {
        RootCallTarget target = fun.getTarget();
        FunctionDefinitionNode root = (FunctionDefinitionNode) target.getRootNode();

        SaveArgumentsNode saveArguments;
        FormalArguments formals;
        int formalsLength = newFormals == RNull.instance ? 0 : ((RPairList) newFormals).getLength();
        String[] argumentNames = new String[formalsLength];
        RNode[] defaultValues = new RNode[formalsLength];
        AccessArgumentNode[] argAccessNodes = new AccessArgumentNode[formalsLength];
        RNode[] init = new RNode[formalsLength];

        Object current = newFormals;
        int i = 0;
        while (current != RNull.instance) {

            final RSyntaxNode defaultValue;
            Object arg = ((RPairList) current).car();
            if (arg == RMissing.instance) {
                defaultValue = null;
            } else if (arg == RNull.instance) {
                defaultValue = RContext.getASTBuilder().constant(RSyntaxNode.LAZY_DEPARSE, RNull.instance);
            } else if (arg instanceof RLanguage) {
                defaultValue = RASTUtils.createSyntaxNodeForRValue(arg);
            } else if (arg instanceof RSymbol) {
                RSymbol symbol = (RSymbol) arg;
                if (symbol.isMissing()) {
                    defaultValue = null;
                } else {
                    defaultValue = RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, symbol.getName(), false);
                }
            } else if (RRuntime.convertScalarVectors(arg) instanceof RAttributable) {
                defaultValue = RContext.getASTBuilder().constant(RSyntaxNode.LAZY_DEPARSE, arg);
            } else {
                throw RInternalError.unimplemented();
            }
            AccessArgumentNode accessArg = AccessArgumentNode.create(i);
            argAccessNodes[i] = accessArg;
            Object tag = ((RPairList) current).getTag();
            String argName = ((RSymbol) tag).getName();
            init[i] = WriteVariableNode.createArgSave(argName, accessArg);

            // Store formal arguments
            argumentNames[i] = argName;
            defaultValues[i] = defaultValue == null ? null : defaultValue.asRNode();
            current = ((RPairList) current).cdr();
            i++;
        }
        saveArguments = new SaveArgumentsNode(init);
        formals = FormalArguments.createForFunction(defaultValues, ArgumentsSignature.get(argumentNames));
        for (AccessArgumentNode access : argAccessNodes) {
            access.setFormals(formals);
        }

        RSyntaxNode bodyNode = root.getBody();
        if (newBody != null) {
            bodyNode = RASTUtils.createSyntaxNodeForRValue(newBody);
        }

        FrameDescriptor descriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("<SET_BODY/SET_FORMALS/SET_CLOENV>", descriptor);
        FrameSlotChangeMonitor.initializeEnclosingFrame(descriptor, newEnv);
        FunctionDefinitionNode rootNode = FunctionDefinitionNode.create(RContext.getInstance().getLanguage(), RSyntaxNode.LAZY_DEPARSE, descriptor, null, saveArguments, bodyNode, formals,
                        root.getName(), null);
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        fun.reassignTarget(callTarget);
        fun.reassignEnclosingFrame(newEnv);
    }
}
