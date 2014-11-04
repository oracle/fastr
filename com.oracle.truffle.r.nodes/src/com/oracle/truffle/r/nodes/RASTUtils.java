/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ReadVariableNode.BuiltinFunctionVariableNode;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.instrument.RNodeWrapper;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * A collection of useful methods for working with {@code AST} instances.
 */
public class RASTUtils {

    /**
     * Removes any {@link WrapArgumentNode}.
     */
    @TruffleBoundary
    public static Node unwrap(Object node) {
        if (node instanceof WrapArgumentNode) {
            return unwrap(((WrapArgumentNode) node).getOperand());
        } else if (node instanceof RNodeWrapper) {
            return ((RNodeWrapper) node).getChild();
        } else {
            return (Node) node;
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

    /**
     * Creates a standard {@link ReadVariableNode}.
     */
    @TruffleBoundary
    public static ReadVariableNode createReadVariableNode(String name) {
        return ReadVariableNode.create(name, RType.Any, false, true, false, true);
    }

    /**
     * Creates a language element for the {@code index}'th element of {@code args}.
     */
    @TruffleBoundary
    public static Object createLanguageElement(CallArgumentsNode args, int index) {
        Node argNode = unwrap(args.getArguments()[index]);
        return RASTUtils.createLanguageElement(argNode);
    }

    /**
     * Handles constants and symbols as special cases as required by R.
     */
    @TruffleBoundary
    public static Object createLanguageElement(Node argNode) {
        if (argNode instanceof ConstantNode) {
            return ((ConstantNode) argNode).getValue();
        } else if (argNode instanceof ReadVariableNode) {
            return RASTUtils.createRSymbol(argNode);
        } else {
            return RDataFactory.createLanguage(argNode);
        }
    }

    /**
     * Creates an {@link RSymbol} from a {@link ReadVariableNode}.
     */
    @TruffleBoundary
    public static RSymbol createRSymbol(Node readVariableNode) {
        return RDataFactory.createSymbol(((ReadVariableNode) readVariableNode).getName());
    }

    @TruffleBoundary
    /**
     * Create an {@link RCallNode} where {@code fn} is either a:
     * <ul>
     * <li>{@link RFunction}\<li>
     * <li>{@link ConstantFunctioNode}</li>
     * <li>{@link ConstantStringNode}</li>
     * <li>{@link ReadVariableNode}</li>
     * <li>GroupDispatchNode</li>
     * </ul>
     */
    public static RNode createCall(Object fna, CallArgumentsNode callArgsNode) {
        Object fn = fna;
        if (fn instanceof ConstantNode) {
            fn = ((ConstantNode) fn).getValue();
        }
        if (fn instanceof String) {
            return RCallNode.createCall(null, RASTUtils.createReadVariableNode(((String) fn)), callArgsNode);
        } else if (fn instanceof ReadVariableNode) {
            return RCallNode.createCall(null, (ReadVariableNode) fn, callArgsNode);
        } else if (fn instanceof GroupDispatchNode) {
            GroupDispatchNode gdn = (GroupDispatchNode) fn;
            return DispatchedCallNode.create(gdn.getGenericName(), RGroupGenerics.RDotGroup, null, callArgsNode);
        } else {
            RFunction rfn = (RFunction) fn;
            return RCallNode.createStaticCall(null, rfn, callArgsNode);
        }
    }

    /**
     * Really should not be necessary, but things like '+' ({@link DispatchedCallNode}) have a
     * different AST structure from normal calls.
     */
    private static class CallArgsNodeFinder implements NodeVisitor {
        CallArgumentsNode callArgumentsNode;

        @TruffleBoundary
        public boolean visit(Node node) {
            if (node instanceof CallArgumentsNode) {
                callArgumentsNode = (CallArgumentsNode) node;
                return false;
            }
            return true;
        }

    }

    /**
     * Find the {@link CallArgumentsNode} that is the child of {@code node}. N.B. Does not copy.
     */
    public static CallArgumentsNode findCallArgumentsNode(Node node) {
        if (node instanceof RCallNode) {
            return ((RCallNode) node).getArgumentsNode();
        }
        node.accept(callArgsNodeFinder);
        assert callArgsNodeFinder.callArgumentsNode != null;
        return callArgsNodeFinder.callArgumentsNode;
    }

    private static final CallArgsNodeFinder callArgsNodeFinder = new CallArgsNodeFinder();

    /**
     * Returns the name (as an {@link RSymbol} or the function associated with an {@link RCallNode}
     * or {@link DispatchedCallNode}.
     *
     * @param quote TODO
     */
    public static Object findFunctionName(Node node, boolean quote) {
        RNode child = (RNode) unwrap(findFunctionNode(node));
        if (child instanceof ReadVariableNode) {
            if (child instanceof BuiltinFunctionVariableNode) {
                BuiltinFunctionVariableNode bvn = (BuiltinFunctionVariableNode) child;
                return bvn.getFunction();
            } else {
                return createRSymbol(child);
            }
        } else if (child instanceof GroupDispatchNode) {
            GroupDispatchNode groupDispatchNode = (GroupDispatchNode) child;
            String gname = groupDispatchNode.getGenericName();
            if (quote) {
                gname = "`" + gname + "`";
            }
            return RDataFactory.createSymbol(gname);
        } else if (child instanceof RCallNode) {
            return findFunctionName(child, quote);
        } else {
            // some more complicated expression, just deparse it
            RDeparse.State state = RDeparse.State.createPrintableState();
            child.deparse(state);
            return RDataFactory.createSymbol(state.toString());
        }
    }

    /**
     * Returns the {@link ReadVariableNode} associated with a {@link RCallNode} or the
     * {@link GroupDispatchNode} associated with a {@link DispatchedCallNode}.
     */
    public static RNode findFunctionNode(Node node) {
        if (node instanceof RCallNode) {
            return ((RCallNode) node).getFunctionNode();
        } else if (node instanceof DispatchedCallNode) {
            for (Node child : node.getChildren()) {
                if (child != null) {
                    if (child instanceof GroupDispatchNode) {
                        return (RNode) child;
                    }
                }
            }
        }
        assert false;
        return null;
    }

    /**
     * Get the {@code n}'th child of {@code node}.
     */
    public static Node getChild(Node node, int n) {
        int i = 0;
        for (Node child : node.getChildren()) {
            if (i++ == n) {
                return child;
            }
        }
        return null;
    }

}
