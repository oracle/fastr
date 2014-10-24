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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ReadVariableNode.ResolvePromiseNode;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.binary.ColonNode.ColonCastNode;
import com.oracle.truffle.r.nodes.binary.ColonNodeFactory.ColonCastNodeFactory;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.FunctionExpressionNode.DynamicFunctionExpressionNode;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

@RBuiltin(name = "substitute", kind = PRIMITIVE, parameterNames = {"expr", "env"}, nonEvalArgs = {0})
public abstract class Substitute extends RBuiltinNode {

    @Child private Quote quote;

    protected abstract Object executeObject(VirtualFrame frame, RPromise promise, Object env);

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    private Quote checkQuote() {
        if (quote == null) {
            quote = insert(QuoteFactory.create(new RNode[1], getBuiltin(), getSuppliedArgsNames()));
        }
        return quote;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object doSubstitute(Object expr, Object x) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ENVIRONMENT);
    }

    @Specialization
    protected Object doSubstitute(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") RMissing envMissing) {
        controlVisibility();
        return doSubstituteWithEnv(frame, expr, null);
    }

    @Specialization
    protected Object doSubstitute(VirtualFrame frame, RPromise expr, REnvironment env) {
        controlVisibility();
        return doSubstituteWithEnv(frame, expr, env);
    }

    @Specialization
    protected Object doSubstitute(VirtualFrame frame, RPromise expr, RList list) {
        controlVisibility();
        return doSubstituteWithEnv(frame, expr, REnvironment.createFromList(list, REnvironment.baseEnv()));
    }

    /**
     * Handles all above specializations. Transforms an AST into another AST, with the appropriate
     * substitutions. The incoming AST will either denote a symbol, constant or function call
     * (because in R everything else is a call). So in general, both the input and output is a call(
     * language element). E.g. {@link IfNode} is a special case because it is not (currently)
     * represented as a function, as are several other nodes.
     *
     * We clear the {@link SourceSection} on the substituted tree as it would need to be modified
     * anyway and GnuR appears to. E.g. {@code c(z+2)} when substituted prints as {@code z + 2}.
     *
     * As per {@code deparse}, it might make sense to have each node handle its own substitution
     * through an appropriate interface.
     *
     * @param frame
     * @param expr
     * @param envArg {@code null} if the {@code env} argument was {@code RMissing} to avoid always
     *            materializing the current frame.
     * @return in general an {@link RLanguage} instance, but simple cases could be a constant value
     *         or {@link RSymbol}
     */
    @TruffleBoundary
    private Object doSubstituteWithEnv(VirtualFrame frame, RPromise expr, REnvironment envArg) {
        // In the global environment, substitute behaves like quote
        // TODO It may be too early to do this check, GnuR doesn't work this way (re promises)
        if (envArg == null && REnvironment.isGlobalEnvFrame(frame) || envArg == REnvironment.globalEnv()) {
            return checkQuote().execute(frame, expr);
        }
        // Check for missing env, which means current
        REnvironment env = envArg != null ? envArg : REnvironment.frameToEnvironment(frame.materialize());

        // We have to examine all the names in the expression:
        // 1. Ordinary variable, replace by value (if bound), else unchanged
        // 2. promise (aka function argument): replace by expression associated with the promise
        // 3. ..., replace by contents of ... (if bound)

        // The "expr" promise comes from the no-evalarg aspect of the builtin,
        // so get the actual expression (AST) from that
        Node node = RASTUtils.unwrap(expr.getRep());
        // substitution is destructive so clone the tree
        node = NodeUtil.cloneNode(node);
        // NodeUtil.printTree(System.out, node);
        return RASTUtils.createLanguageElement(clearSourceSection(substituteAST(node, env)));
    }

    /**
     * Visits the tree rooted at {@link Node}, performing substitutions. The visit is similar to
     * that provided by {@link NodeVisitor}, but processes call arguments specially.
     *
     * The result is always a subclass of {@link Node}. One possible optimization would be to
     * special-case a top level call and avoid creating nodes for constants and symbols, as this
     * wrapping is undone in the caller.
     *
     * {@link WrapArgumentNode}s are not unwrapped by the substitution. It is not clear whether this
     * is correct in all cases.
     *
     * It is also possible that hidden (non-syntactic) nodes may need to be transformed.
     *
     */
    @TruffleBoundary
    private static Node substituteAST(Node node, REnvironment env) {
        if (node instanceof ConstantNode) {
            return node;
        } else if (node instanceof ReadVariableNode) {
            return substituteVariable((ReadVariableNode) node, env);
        } else if (node instanceof RCallNode || node instanceof DispatchedCallNode) {
            RNode funNode = RASTUtils.findFunctionNode(node);
            Node funNodeSubs;
            if (node instanceof DispatchedCallNode) {
                /*
                 * TODO support substitution of the function name, which would require a new
                 * OpsDispatchNode.
                 */
                funNodeSubs = funNode;
            } else {
                // ReadVariableNode
                funNodeSubs = substituteAST(funNode, env);
            }
            boolean changed = funNodeSubs != funNode;
            CallArgumentsNode arguments = RASTUtils.findCallArgumentsNode(node);
            CallArgumentsNode argumentsSubs = substituteArgs(arguments, env);
            changed = changed || (arguments != argumentsSubs);
            if (!changed) {
                return node;
            } else {
                return RASTUtils.createCall(funNodeSubs, argumentsSubs);
            }
        } else if (node instanceof WrapArgumentNode) {
            Node unode = RASTUtils.unwrap(node);
            Node subsNode = substituteAST(unode, env);
            if (subsNode != unode) {
                if (subsNode instanceof DotsNode) {
                    return subsNode;
                } else {
                    return WrapArgumentNode.create((RNode) subsNode, false);
                }
            } else {
                return node;
            }
        } else if (node instanceof WriteVariableNode) {
            // nothing to do, formals do not get substituted, so neither do these (artificial) nodes
            return node;
        } else {
            return substituteOther(node, env);
        }
    }

    @TruffleBoundary
    private static Node substituteVariable(ReadVariableNode node, REnvironment env) {
        String name = node.getSymbol().getName();
        Object val = env.get(name);
        if (val == null) {
            // not bound in env,
            if (node instanceof ResolvePromiseNode) {
                return ((ResolvePromiseNode) node).getReadNode();
            } else {
                return node;
            }
        } else if (val instanceof RMissing) {
            if (name.equals("...")) {
                return new MissingDotsNode();
            } else {
                // strange special case, mimics GnuR behavior
                return RASTUtils.createReadVariableNode("");
            }
        } else if (val instanceof RPromise) {
            return RASTUtils.unwrap(((RPromise) val).getRep());
        } else if (val instanceof RLanguage) {
            return (Node) ((RLanguage) val).getRep();
        } else if (val instanceof RArgsValuesAndNames) {
            // this is '...'
            RArgsValuesAndNames rva = (RArgsValuesAndNames) val;
            Object[] values = rva.getValues();
            RNode[] expandedNodes = new RNode[values.length];
            for (int i = 0; i < values.length; i++) {
                Object argval = values[i];
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

    /**
     * Substitute the arguments, handling the special case of '...', which can grow or shrink the
     * array.
     */
    @TruffleBoundary
    private static CallArgumentsNode substituteArgs(CallArgumentsNode arguments, REnvironment env) {
        boolean changed = false;
        String[] names = arguments.getNames();
        RNode[] argNodes = arguments.getArguments();
        RNode[] argNodesNew = new RNode[argNodes.length];
        int missingCount = 0;
        int j = 0;
        for (int i = 0; i < argNodes.length; i++) {
            RNode argNode = argNodes[i];
            RNode argNodeSubs = (RNode) substituteAST(argNode, env);
            if (argNodeSubs instanceof MissingDotsNode) {
                // in this case we remove the argument altogether, leave slot as null
                missingCount++;
                changed = true;
            } else if (argNodeSubs instanceof ExpandedDotsNode) {
                // 2 or more
                ExpandedDotsNode expandedDotsNode = (ExpandedDotsNode) argNodeSubs;
                RNode[] argNodesNewer = new RNode[argNodesNew.length + expandedDotsNode.nodes.length - 1];
                if (i > 0) {
                    System.arraycopy(argNodesNew, 0, argNodesNewer, 0, j);
                }
                System.arraycopy(expandedDotsNode.nodes, 0, argNodesNewer, j, expandedDotsNode.nodes.length);
                argNodesNew = argNodesNewer;
                changed = true;
                j += expandedDotsNode.nodes.length - 1;
            } else {
                argNodesNew[j] = argNodeSubs;
                changed = changed || argNode != argNodeSubs;
            }
            j++;
        }
        if (!changed) {
            return arguments;
        } else {
            if (missingCount > 0) {
                // Strip out the Missing ... instances
                RNode[] argNodesNewer = new RNode[argNodesNew.length - missingCount];
                j = 0;
                for (int i = 0; i < argNodesNew.length; i++) {
                    RNode argNode = argNodesNew[i];
                    if (argNode == null) {
                        continue;
                    }
                    argNodesNewer[j++] = argNode;
                }
                argNodesNew = argNodesNewer;
            }
            return CallArgumentsNode.create(false, false, argNodesNew, names);
        }

    }

    /**
     * Semi-generic substitution of nodes not explicitly handled in {@link #substituteAST}. It
     * cannot be truly generic because we cannot use reflection.
     */
    @TruffleBoundary
    private static Node substituteOther(Node node, REnvironment env) {
        if (FastROptions.Debug.getValue()) {
            Utils.debug("substituteOther: " + node.getClass().getSimpleName());
        }
        ArrayList<RNode> childSubs = new ArrayList<>();
        for (Node child : node.getChildren()) {
            if (child != null) {
                childSubs.add((RNode) substituteAST(child, env));
            }
        }
        // This isn't robust unless getChildren matches declaration order.
        if (node instanceof ColonNode) {
            return ColonNodeFactory.create(childSubs.get(0), childSubs.get(1));
        } else if (node instanceof ColonCastNode) {
            return ColonCastNodeFactory.create(childSubs.get(0));
        } else if (node instanceof IfNode) {
            return IfNode.create(childSubs.get(0), childSubs.get(1), childSubs.get(2));
        } else if (node instanceof ConvertBooleanNode) {
            return ConvertBooleanNode.create(childSubs.get(0));
        } else if (node instanceof DynamicFunctionExpressionNode) {
            DynamicFunctionExpressionNode dfe = (DynamicFunctionExpressionNode) node;
            return substituteFunction((FunctionDefinitionNode) dfe.getCallTarget().getRootNode(), env);
        } else {
            assert false;
            return null;
        }
    }

    @TruffleBoundary
    private static Node substituteFunction(FunctionDefinitionNode node, REnvironment env) {
        SequenceNode sn = (SequenceNode) RASTUtils.getChild(node, 0);
        for (Node child : sn.getChildren()) {
            RNode rchild = (RNode) child;
            if (rchild != null) {
                RNode newChild = (RNode) substituteAST(rchild, env);
                if (child != newChild) {
                    child.replace(newChild);
                }
            }
        }
        return node;
    }

    private static Node clearSourceSection(Node node) {
        node.accept(n -> {
            n.clearSourceSection();
            return true;
        });
        return node;
    }

    /**
     * Marker class for special '...' handling.
     */
    private static abstract class DotsNode extends RNode {
    }

    /**
     * A temporary {@link RNode} type that exists only during substitution to hold the expanded
     * array of values from processing '...'. Allows {@link #substituteAST} to always return a
     * single node.
     */
    private static class ExpandedDotsNode extends DotsNode {

        RNode[] nodes;

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
    private static class MissingDotsNode extends DotsNode {
        @Override
        public Object execute(VirtualFrame frame) {
            assert false;
            return null;
        }

    }

}
