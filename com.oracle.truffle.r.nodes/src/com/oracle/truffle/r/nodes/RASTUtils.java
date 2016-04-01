/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.ReadVariadicComponentNode;
import com.oracle.truffle.r.nodes.access.variables.NamedRNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentBaseNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RInstrumentableNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * A collection of useful methods for working with {@code AST} instances.
 */
public class RASTUtils {

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

    public static RSyntaxNode[] asSyntaxNodes(RNode[] nodes) {
        RSyntaxNode[] result = new RSyntaxNode[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            result[i] = nodes[i] == null ? null : nodes[i].asRSyntaxNode();
        }
        return result;
    }

    /**
     * Creates a standard {@link ReadVariableNode}.
     */
    @TruffleBoundary
    public static ReadVariableNode createReadVariableNode(String name) {
        return ReadVariableNode.create(name);
    }

    /**
     * Handles constants and symbols as special cases as required by R.
     */
    @TruffleBoundary
    public static Object createLanguageElement(RBaseNode argNode) {
        if (argNode == null) {
            return RSymbol.MISSING;
        } else if (argNode instanceof ConstantNode) {
            Object value = ((ConstantNode) argNode).getValue();
            if (value == RMissing.instance) {
                // special case which GnuR handles as an unnamed symbol
                return RSymbol.MISSING;
            }
            return value;
        } else if (argNode instanceof ReadVariableNode) {
            return RASTUtils.createRSymbol(argNode);
        } else if (argNode instanceof VarArgNode) {
            VarArgNode varArgNode = (VarArgNode) argNode;
            return RDataFactory.createSymbolInterned(varArgNode.getIdentifier());
        } else {
            assert !(argNode instanceof VarArgNode);
            return RDataFactory.createLanguage((RNode) argNode);
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
            assert id == id.intern();
            return RDataFactory.createSymbol(id);
        }
    }

    /**
     * Checks wheter {@code expr instanceof RSymbol} and, if so, wraps in an {@link RLanguage}
     * instance.
     */
    @TruffleBoundary
    public static Object checkForRSymbol(Object expr) {
        if (expr instanceof RSymbol) {
            String symbolName = ((RSymbol) expr).getName();
            return RDataFactory.createLanguage(ReadVariableNode.create(symbolName));
        } else {
            return expr;
        }
    }

    /**
     * Create an {@link RNode} from a runtime value.
     */
    @TruffleBoundary
    public static RBaseNode createNodeForValue(Object value) {
        if (value instanceof RNode) {
            return (RNode) value;
        } else if (value instanceof RSymbol) {
            return RASTUtils.createReadVariableNode(((RSymbol) value).getName());
        } else if (value instanceof RLanguage) {
            RLanguage l = (RLanguage) value;
            return RASTUtils.cloneNode(l.getRep());
        } else if (value instanceof RPromise) {
            RPromise promise = (RPromise) value;
            RNode promiseRep = (RNode) unwrap(((RPromise) value).getRep());
            if (promiseRep instanceof VarArgNode) {
                VarArgNode varArgNode = (VarArgNode) promiseRep;
                RPromise varArgPromise = (RPromise) varArgNode.execute((VirtualFrame) promise.getFrame());
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
            return ConstantNode.create(value);
        }
    }

    /**
     * Create an {@link RCallNode}. Where {@code fn} is either a:
     * <ul>
     * <li>{@link RFunction}\
     * <li>{@code ConstantFunctionNode}</li>
     * <li>{@code ConstantStringNode}</li>
     * <li>{@link ReadVariableNode}</li>
     * <li>{@link RCallNode}</li>
     * <li>GroupDispatchNode</li>
     * </ul>
     */
    @TruffleBoundary
    public static RSyntaxNode createCall(Object fna, boolean sourceUnavailable, ArgumentsSignature signature, RSyntaxNode... arguments) {
        Object fn = fna;
        if (fn instanceof Node) {
            fn = unwrap(fn);
        }
        if (fn instanceof ConstantNode) {
            fn = ((ConstantNode) fn).getValue();
        }
        SourceSection sourceSection = sourceUnavailable ? RSyntaxNode.SOURCE_UNAVAILABLE : RSyntaxNode.EAGER_DEPARSE;
        if (fn instanceof String) {
            return RCallNode.createCall(sourceSection, RASTUtils.createReadVariableNode(((String) fn)), signature, arguments);
        } else if (fn instanceof ReadVariableNode) {
            return RCallNode.createCall(sourceSection, (ReadVariableNode) fn, signature, arguments);
        } else if (fn instanceof NamedRNode) {
            return RCallNode.createCall(RSyntaxNode.SOURCE_UNAVAILABLE, (NamedRNode) fn, signature, arguments);
        } else if (fn instanceof RFunction) {
            RFunction rfn = (RFunction) fn;
            return RCallNode.createCall(sourceSection, ConstantNode.create(rfn), signature, arguments);
        } else if (fn instanceof RCallNode) {
            return RCallNode.createCall(sourceSection, (RCallNode) fn, signature, arguments);
        } else {
            // this of course would not make much sense if trying to evaluate this call, yet it's
            // syntactically possible, for example as a result of:
            // f<-function(x,y) sys.call(); x<-f(7, 42); x[c(2,3)]
            return RCallNode.createCall(sourceSection, ConstantNode.create(fn), signature, arguments);
        }
    }

    @TruffleBoundary
    /**
     * The heart of the {@code substitute} function, where we look up the value of {@code name} in
     * {@code env} and, if bound, return whatever value it had (as an {@link RSyntaxNode},or
     * {@code null} if not bound.
     *
     * N.B. It is <b>very</b> important that the result is cloned or created as otherwise we risk
     * accidental sharing between ASTs.
     */
    public static RSyntaxNode substituteName(String name, REnvironment env) {
        Object val = env.get(name);
        if (val == null) {
            // not bound in env,
            return null;
        } else if (val instanceof RMissing) {
            // strange special case, mimics GnuR behavior
            return RASTUtils.createReadVariableNode("");
        } else if (val instanceof RPromise) {
            return (RSyntaxNode) RASTUtils.cloneNode(RASTUtils.unwrap(((RPromise) val).getRep()));
        } else if (val instanceof RLanguage) {
            return (RSyntaxNode) RASTUtils.cloneNode(((RLanguage) val).getRep());
        } else if (val instanceof RSymbol) {
            return RASTUtils.createReadVariableNode(((RSymbol) val).getName());
        } else if (val instanceof RArgsValuesAndNames) {
            // this is '...'
            RArgsValuesAndNames rva = (RArgsValuesAndNames) val;
            if (rva.isEmpty()) {
                return new MissingDotsNode();
            }
            Object[] values = rva.getArguments();
            RSyntaxNode[] expandedNodes = new RSyntaxNode[values.length];
            for (int i = 0; i < values.length; i++) {
                Object argval = values[i];
                while (argval instanceof RPromise) {
                    RPromise promise = (RPromise) argval;
                    Node unwrap = RASTUtils.unwrap(promise.getRep());
                    if (unwrap instanceof VarArgNode) {
                        VarArgNode varArgNode = (VarArgNode) unwrap;
                        try {
                            RArgsValuesAndNames v = (RArgsValuesAndNames) promise.getFrame().getObject(promise.getFrame().getFrameDescriptor().findFrameSlot(ArgumentsSignature.VARARG_NAME));
                            argval = v.getArguments()[varArgNode.getIndex()];
                        } catch (FrameSlotTypeException e) {
                            throw RInternalError.shouldNotReachHere();
                        }
                    }
                    break;
                }
                if (argval instanceof RPromise) {
                    RPromise promise = (RPromise) argval;
                    expandedNodes[i] = (RSyntaxNode) RASTUtils.cloneNode(RASTUtils.unwrap(promise.getRep()));
                } else {
                    expandedNodes[i] = ConstantNode.create(argval);
                }
            }
            return values.length > 1 ? new ExpandedDotsNode(expandedNodes) : expandedNodes[0];
        } else {
            // An actual value
            return ConstantNode.create(val);
        }
    }

    @TruffleBoundary
    public static String expectName(RNode node) {
        if (node instanceof ConstantNode) {
            Object c = ((ConstantNode) node).getValue();
            if (c instanceof String) {
                return (String) c;
            } else if (c instanceof Double) {
                return ((Double) c).toString();
            } else {
                throw RInternalError.unimplemented();
            }
        } else if (node instanceof ReadVariableNode) {
            return ((ReadVariableNode) node).getIdentifier();
        } else {
            throw RInternalError.unimplemented();
        }
    }

    /**
     * Marker class for special '...' handling.
     */
    private abstract static class DotsNode extends RNode implements RSyntaxNode {

        @Override
        public RSyntaxNode substituteImpl(REnvironment env) {
            throw RInternalError.unimplemented();
        }

        @Override
        public void serializeImpl(com.oracle.truffle.r.runtime.RSerialize.State state) {
            throw RInternalError.unimplemented();
        }

        @Override
        public void setSourceSection(SourceSection sourceSection) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public SourceSection getSourceSection() {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * A temporary {@link RNode} type that exists only during substitution to hold the expanded
     * array of values from processing '...'. Allows {@link RSyntaxNode#substituteImpl} to always
     * return a single node.
     */
    public static final class ExpandedDotsNode extends DotsNode {

        public final RSyntaxNode[] nodes;

        private ExpandedDotsNode(RSyntaxNode[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            assert false;
            return null;
        }
    }

    /**
     * Denotes a '...' usage that was "missing".
     */
    public static class MissingDotsNode extends DotsNode {
        @Override
        public Object execute(VirtualFrame frame) {
            assert false;
            return null;
        }
    }
}
