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
package com.oracle.truffle.r.nodes.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.array.read.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgsPromiseNode;
import com.oracle.truffle.r.nodes.instrument.debug.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;
import com.oracle.truffle.r.runtime.gnur.*;

/**
 * Implementation of {@link RASTHelper}.
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
public class RASTHelperImpl implements RASTHelper {

    @TruffleBoundary
    @Override
    public int getLength(RLanguage rl) {
        RNode root = (RNode) rl.getRep();
        // NodeUtil.printTree(System.out, root);
        return computeLength(RASTUtils.unwrap(root));
    }

    @TruffleBoundary
    private static int computeLength(Node node) {
        int result = 1;
        if (node instanceof RCallNode || node instanceof GroupDispatchNode) {
            // 1 + number of args
            CallArgumentsNode args = RASTUtils.findCallArgumentsNode(node);
            result += args.getArguments().length;
        } else if (node instanceof IfNode) {
            // 3 or 4 with else part
            result = 3 + (((IfNode) node).getElsePart() != null ? 1 : 0);
        } else if (node instanceof FunctionBodyNode) {
            FunctionBodyNode fbn = (FunctionBodyNode) node;
            boolean hasBrace = fbn.getFunctionDefinitionNode().hasBraces();
            FunctionStatementsNode fsNode = fbn.getStatements();
            result = hasBrace ? 1 : 0;
            for (RNode s : fsNode.getSequence()) {
                if (s.unwrap().isSyntax()) {
                    result++;
                }
            }
        } else if (node instanceof ForNode) {
            return 4;
        } else if (node instanceof WhileNode) {
            return ((WhileNode) node).isRepeat() ? 2 : 3;
        } else if (node instanceof WriteVariableNode) {
            return 3;
        } else if (node instanceof AccessArrayNode) {
            AccessArrayNode accessArrayNode = (AccessArrayNode) node;
            int baseResult = 3;
            if (accessArrayNode.exactInSource) {
                baseResult++;
            }
            if (accessArrayNode.dropInSource) {
                baseResult++;
            }
            return baseResult;
        } else if (node instanceof AccessFieldNode) {
            return 3;
        } else if (node instanceof VarArgsPromiseNode) {
            return ((VarArgsPromiseNode) node).getClosures().length;
        } else {
            // TODO fill out
            assert false : node;
        }
        return result;
    }

    @TruffleBoundary
    @Override
    public Object getDataAtAsObject(RLanguage rl, final int indexArg) {
        // index has already been range checked based on computeLength
        Node node = RASTUtils.unwrap(rl.getRep());
        int index = indexArg;
        if (node instanceof RCallNode || node instanceof GroupDispatchNode) {
            if (index == 0) {
                if (RASTUtils.isNamedFunctionNode(node)) {
                    return RASTUtils.findFunctionName(node, true);
                } else {
                    return RDataFactory.createLanguage(RASTUtils.getFunctionNode(node));
                }
            } else {
                CallArgumentsNode args = RASTUtils.findCallArgumentsNode(node);
                return RASTUtils.createLanguageElement(args, index - 1);
            }
        } else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            switch (index) {
                case 0:
                    return RDataFactory.createSymbol("`if`");
                case 1:
                    return RASTUtils.createLanguageElement(ifNode.getCondition().getOperand());
                case 2:
                    return RASTUtils.createLanguageElement(ifNode.getThenPart());
                case 3:
                    return RASTUtils.createLanguageElement(ifNode.getElsePart());
                default:
                    assert false;
            }
        } else if (node instanceof FunctionBodyNode) {
            FunctionBodyNode fbn = (FunctionBodyNode) node;
            boolean hasBrace = fbn.getFunctionDefinitionNode().hasBraces();
            FunctionStatementsNode fsNode = fbn.getStatements();
            int sIndex = hasBrace ? index - 1 : index;
            if (hasBrace && index == 0) {
                return RDataFactory.createSymbol("`{`");
            } else {
                return RASTUtils.createLanguageElement(fsNode.getSequence()[sIndex].unwrap());
            }
        } else if (node instanceof ForNode) {
            ForNode forNode = (ForNode) node;
            switch (index) {
                case 0:
                    return RDataFactory.createSymbol("`for`");
                case 1:
                    return RASTUtils.createLanguageElement(forNode.getCvar());
                case 2:
                    return RASTUtils.createLanguageElement(forNode.getRange());
                case 3:
                    return RASTUtils.createLanguageElement(forNode.getBody());
                default:
                    assert false;
            }
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            if (whileNode.isRepeat() && index == 1) {
                index = 2;
            }
            switch (index) {
                case 0:
                    return RDataFactory.createSymbol(whileNode.isRepeat() ? "`repeat`" : "`while`");
                case 1:
                    return RASTUtils.createLanguageElement(whileNode.getCondition());
                case 2:
                    return RASTUtils.createLanguageElement(whileNode.getBody());
                default:
                    assert false;
            }
        } else if (node instanceof WriteVariableNode) {
            WriteVariableNode wvn = (WriteVariableNode) node;
            switch (index) {
                case 0:
                    return RDataFactory.createSymbol("`<-`");
                case 1:
                    return RDataFactory.createSymbol(wvn.getName());
                case 2:
                    return RASTUtils.createLanguageElement(wvn.getRhs());
                default:
                    assert false;
            }
        } else if (node instanceof AccessArrayNode) {
            AccessArrayNode accessArrayNode = (AccessArrayNode) node;
            switch (index) {
                case 0:
                    return RDataFactory.createSymbol(accessArrayNode.isSubset ? "`[`" : "`[[`");
                case 1:
                    return RASTUtils.createLanguageElement(accessArrayNode.getVector());
                default:
                    assert false;
            }
        } else if (node instanceof AccessFieldNode) {
            AccessFieldNode accessFieldNode = (AccessFieldNode) node;
            switch (index) {
                case 0:
                    return RDataFactory.createSymbol("`$`");
                case 1:
                    return RASTUtils.createLanguageElement(accessFieldNode.getObject());
                case 2:
                    return RDataFactory.createSymbol(accessFieldNode.getField());
                default:
                    assert false;
            }

        } else {
            // TODO fill out
            assert false;
        }
        return null;
    }

    public RList asList(RLanguage rl) {
        Object[] data = new Object[getLength(rl)];
        for (int i = 0; i < data.length; i++) {
            data[i] = getDataAtAsObject(rl, i);
        }
        return RDataFactory.createList(data);
    }

    @Override
    public void deparse(State state, RLanguage rl) {
        RASTDeparse.deparse(state, rl);
    }

    @Override
    public void deparse(State state, RFunction f) {
        RASTDeparse.deparse(state, f);
    }

    /*
     * Support for calling out to R functions from within FastR when not in a Truffle context.
     */

    private static RCallNode getCallNode(Source source) {
        try {
            return (RCallNode) ((RLanguage) RContext.getEngine().parse(source).getDataAt(0)).getRep();
        } catch (ParseException ex) {
            // most unexpected
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * TODO replace with more efficient implementation.
     */
    private static Object callOut(RCallNode callNode, int depth, Object... args) {
        RNode[] argNodes = new RNode[args.length];
        for (int i = 0; i < args.length; i++) {
            argNodes[i] = ConstantNode.create(args[i]);
        }
        RCallNode call = RCallNode.createCloneReplacingArgs(callNode, argNodes);
        /*
         * It's important to disable/enable debugging across this call as it isn't really part of
         * the user execution.
         */
        boolean gd = DebugHandling.globalDisable(true);
        try {
            return RContext.getEngine().eval(RDataFactory.createLanguage(call), REnvironment.globalEnv(), depth + 1);
        } catch (PutException ex) {
            throw RInternalError.shouldNotReachHere("callOut");
        } finally {
            DebugHandling.globalDisable(gd);
        }
    }

    private static final Source GET_NAMESPACE_SOURCE = Source.asPseudoFile("..getNamespace(name)", "<..getNamespace>");
    private static RCallNode getNamespaceCall;

    @Override
    public REnvironment findNamespace(RStringVector name, int depth) {
        if (getNamespaceCall == null) {
            getNamespaceCall = getCallNode(GET_NAMESPACE_SOURCE);
        }
        return (REnvironment) callOut(getNamespaceCall, depth, name);
    }

    private static final Source HANDLE_SIMPLE_ERROR_SOURCE = Source.asPseudoFile(".handleSimpleError(h, msg, call)", "<.handleSimpleError>");
    private static RCallNode handleSimpleErrorCall;

    @Override
    public void handleSimpleError(RFunction f, RStringVector msg, Object call, int depth) {
        if (handleSimpleErrorCall == null) {
            handleSimpleErrorCall = getCallNode(HANDLE_SIMPLE_ERROR_SOURCE);
        }
        callOut(handleSimpleErrorCall, depth, f, msg, call);
    }

    private static final Source SIGNAL_SIMPLE_WARNING_SOURCE = Source.asPseudoFile(".signalSimpleWarning(call, msg)", "<.signalSimpleWarning>");
    private static RCallNode signalSimpleWarningCall;

    public void signalSimpleWarning(RStringVector msg, Object call, int depth) {
        if (signalSimpleWarningCall == null) {
            signalSimpleWarningCall = getCallNode(SIGNAL_SIMPLE_WARNING_SOURCE);
        }
        callOut(signalSimpleWarningCall, depth, msg, call);
    }

    @Override
    public Object serialize(RSerialize.State state, Object obj) {
        if (obj instanceof RFunction) {
            RFunction f = (RFunction) obj;
            FunctionDefinitionNode fdn = (FunctionDefinitionNode) f.getRootNode();
            REnvironment env = REnvironment.frameToEnvironment(f.getEnclosingFrame());
            state.openPairList().setTag(env);
            fdn.serialize(state);
            return state.closePairList();
        } else if (obj instanceof RLanguage) {
            RLanguage lang = (RLanguage) obj;
            RNode node = (RNode) lang.getRep();
            state.openPairList(SEXPTYPE.LANGSXP);
            node.serialize(state);
            return state.closePairList();
        } else {
            throw RInternalError.unimplemented("serialize");
        }
    }

    @Override
    public void serializeNode(RSerialize.State state, Object node) {
        ((RNode) node).serialize(state);
    }

}
