/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ConstantNode.ConstantMissingNode;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgsPromiseNode;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.env.*;

/**
 * A collection of useful methods for working with {@code AST} instances.
 */
public class RASTUtils {

    /**
     * Removes any {@link WrapArgumentNode} or {@link WrapperNode}.
     */
    @TruffleBoundary
    public static Node unwrap(Object node) {
        if (node instanceof WrapArgumentNode) {
            return unwrap(((WrapArgumentNode) node).getOperand());
        } else if (node instanceof RInstrumentableNode) {
            return ((RInstrumentableNode) node).unwrap();
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
        return ReadVariableNode.createForced(name, RType.Any);
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
            if (argNode instanceof ConstantMissingNode) {
                // special case which GnuR handles as an unnamed symbol
                return RSymbol.MISSING;
            }
            return ((ConstantNode) argNode).getValue();
        } else if (argNode instanceof ReadVariableNode) {
            return RASTUtils.createRSymbol(argNode);
        } else if (argNode instanceof VarArgsPromiseNode) {
            /*
             * This is mighty tedious, but GnuR represents this as a pairlist and we do have to
             * convert it into either an RPairList or an RList for compatibility.
             */
            VarArgsPromiseNode vapn = ((VarArgsPromiseNode) argNode);
            Closure[] closures = vapn.getClosures();
            ArgumentsSignature signature = vapn.getSignature();
            RPairList prev = null;
            RPairList result = null;
            for (int i = 0; i < closures.length; i++) {
                RPairList pl = RDataFactory.createPairList(createLanguageElement(unwrap(closures[i].getExpr())), null, signature.getName(i));
                if (prev != null) {
                    prev.setCdr(pl);
                } else {
                    result = pl;
                }
                prev = pl;
            }
            return result;
        } else {
            assert !(argNode instanceof VarArgNode);
            return RDataFactory.createLanguage(argNode);
        }
    }

    /**
     * Creates an {@link RSymbol} from a {@link ReadVariableNode}.
     */
    @TruffleBoundary
    public static RSymbol createRSymbol(Node readVariableNode) {
        return RDataFactory.createSymbol(((ReadVariableNode) readVariableNode).getIdentifier());
    }

    /**
     * Checks wheter {@code expr instanceof RSymbol} and, if so, wraps in an {@link RLanguage}
     * instance.
     */
    @TruffleBoundary
    public static Object checkForRSymbol(Object expr) {
        if (expr instanceof RSymbol) {
            String symbolName = ((RSymbol) expr).getName();
            return RDataFactory.createLanguage(ReadVariableNode.create(symbolName, false));
        } else {
            return expr;
        }
    }

    /**
     * Create an {@link RNode} from a runtime value.
     */
    @TruffleBoundary
    public static RNode createNodeForValue(Object value) {
        if (value instanceof RNode) {
            return (RNode) value;
        } else if (value instanceof RSymbol) {
            return RASTUtils.createReadVariableNode(((RSymbol) value).getName());
        } else if (value instanceof RLanguage) {
            RLanguage l = (RLanguage) value;
            return (RNode) NodeUtil.cloneNode((Node) l.getRep());
        } else if (value instanceof RPromise) {
            // TODO: flatten nested promises?
            return NodeUtil.cloneNode(((RNode) ((RPromise) value).getRep()).unwrap());
        } else {
            return ConstantNode.create(value);
        }

    }

    public static boolean isLanguageOrExpression(Object expr) {
        return expr instanceof RExpression || expr instanceof RLanguage;
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
            return RCallNode.createCall(null, RASTUtils.createReadVariableNode(((String) fn)), callArgsNode, null);
        } else if (fn instanceof ReadVariableNode) {
            return RCallNode.createCall(null, (ReadVariableNode) fn, callArgsNode, null);
        } else if (fn instanceof GroupDispatchCallNode) {
            GroupDispatchCallNode gdcn = (GroupDispatchCallNode) fn;
            return GroupDispatchCallNode.create(gdcn.getGenericName(), gdcn.getGroupName(), callArgsNode, gdcn.getCallSrc());
        } else {
            RFunction rfn = (RFunction) fn;
            return RCallNode.createCall(null, ConstantNode.create(rfn), callArgsNode, null);
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
     * @param escape Add escape characters to non-standard names
     */
    public static Object findFunctionName(Node node, boolean escape) {
        RNode child = (RNode) unwrap(findFunctionNode(node));
        if (child instanceof ConstantNode && ((ConstantNode) child).getValue() instanceof RFunction) {
            return ((ConstantNode) child).getValue();
        } else if (child instanceof ReadVariableNode) {
            String name = ((ReadVariableNode) child).getIdentifier();
            if (escape) {
                name = escapeName(name);
            }
            return RDataFactory.createSymbol(name);
        } else if (child instanceof GroupDispatchCallNode) {
            GroupDispatchCallNode groupDispatchNode = (GroupDispatchCallNode) child;
            String gname = groupDispatchNode.getGenericName();
            if (escape) {
                gname = escapeName(gname);
            }
            return RDataFactory.createSymbol(gname);
        } else if (child instanceof RBuiltinNode) {
            RBuiltinNode builtinNode = (RBuiltinNode) child;
            return RDataFactory.createSymbol((builtinNode.getBuiltin().getRBuiltin().name()));
        } else if (child instanceof RCallNode) {
            return findFunctionName(child, escape);
        } else {
            // some more complicated expression, just deparse it
            RDeparse.State state = RDeparse.State.createPrintableState();
            child.deparse(state);
            return RDataFactory.createSymbol(state.toString());
        }
    }

    private static String escapeName(String name) {
        return RDeparse.quotify(name, RDeparse.BACKTICK);
    }

    /**
     * Returns the {@link ReadVariableNode} associated with a {@link RCallNode} or the
     * {@link GroupDispatchCallNode} .
     */
    public static RNode findFunctionNode(Node node) {
        if (node instanceof RCallNode) {
            return ((RCallNode) node).getFunctionNode();
        } else if (node instanceof GroupDispatchCallNode) {
            return (RNode) node;
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

    @TruffleBoundary
    public static RNode substituteName(String name, REnvironment env) {
        Object val = env.get(name);
        if (val == null) {
            // not bound in env,
            return null;
        } else if (val instanceof RMissing) {
            // strange special case, mimics GnuR behavior
            return RASTUtils.createReadVariableNode("");
        } else if (val instanceof RPromise) {
            return (RNode) RASTUtils.unwrap(((RPromise) val).getRep());
        } else if (val instanceof RLanguage) {
            return (RNode) ((RLanguage) val).getRep();
        } else if (val instanceof RArgsValuesAndNames) {
            // this is '...'
            RArgsValuesAndNames rva = (RArgsValuesAndNames) val;
            if (rva.isEmpty()) {
                return new MissingDotsNode();
            }
            Object[] values = rva.getValues();
            RNode[] expandedNodes = new RNode[values.length];
            for (int i = 0; i < values.length; i++) {
                Object argval = values[i];
                while (argval instanceof RPromise) {
                    RPromise promise = (RPromise) argval;
                    Node unwrap = RASTUtils.unwrap(promise.getRep());
                    if (unwrap instanceof VarArgNode) {
                        VarArgNode varArgNode = (VarArgNode) unwrap;
                        try {
                            RArgsValuesAndNames v = (RArgsValuesAndNames) promise.getFrame().getObject(promise.getFrame().getFrameDescriptor().findFrameSlot("..."));
                            argval = v.getValues()[varArgNode.getIndex()];
                        } catch (FrameSlotTypeException e) {
                            throw RInternalError.shouldNotReachHere();
                        }
                    }
                    break;
                }
                if (argval instanceof RPromise) {
                    RPromise promise = (RPromise) argval;
                    expandedNodes[i] = (RNode) RASTUtils.unwrap(promise.getRep());
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
    public abstract static class DotsNode extends RNode {
    }

    /**
     * A temporary {@link RNode} type that exists only during substitution to hold the expanded
     * array of values from processing '...'. Allows {@link RSyntaxNode#substitute} to always return
     * a single node.
     */
    public static class ExpandedDotsNode extends DotsNode {

        public final RNode[] nodes;

        ExpandedDotsNode(RNode[] nodes) {
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
