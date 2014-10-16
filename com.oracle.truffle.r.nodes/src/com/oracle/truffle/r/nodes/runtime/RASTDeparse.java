/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.runtime;

import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.UnresolvedWriteLocalVariableNode;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.binary.ColonNode.ColonCastNode;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Deparse support for AST instances. Helper class for {@link RASTHelperImpl}.
 *
 * N.B. We ignore the {@link SourceSection} for GnuR compatibility. E.g. in <a
 * href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/deparse.html">deparse
 * specification</a>: "To avoid the risk of a source attribute out of sync with the actual function
 * definition, the source attribute of a function will never be deparsed as an attribute."
 *
 * It would probably be a good idea to define a {@code deparse} method on {@link RNode} and move the
 * deparsing logic to the node itself.
 *
 * Parts transcribed from GnuR deparse.c
 */
public class RASTDeparse {
    public static void deparse(State state, RLanguage rl) {
        Node node = (Node) rl.getRep();
        deparseNode(state, node);
    }

    public static void deparse(State state, RFunction f) {
        deparseNode(state, f.getTarget().getRootNode());
    }

    /**
     * Deparse an AST node.
     */
    private static void deparseNode(State state, Node fnode) {
        // This would me much easier if everything really was a function
        Node node = RASTUtils.unwrap(fnode);
        if (node instanceof UnresolvedWriteLocalVariableNode) {
            UnresolvedWriteLocalVariableNode wvn = (UnresolvedWriteLocalVariableNode) node;
            state.append(wvn.getName());
            state.append(" <- ");
            deparseNodeOrValue(state, wvn.getRhs());
        } else if (node instanceof RCallNode || node instanceof DispatchedCallNode) {
            Object fname = RASTUtils.findFunctionName(node, false);
            Func func = isInfixOperator(fname);
            if (func != null) {
                deparseInfixOperator(state, node, func);
            } else {
                RDeparse.deparse2buff(state, fname);
                state.append('(');
                CallArgumentsNode args = RASTUtils.findCallArgumentsNode(node);
                String[] argNames = args.getNames();
                RNode[] argValues = args.getArguments();
                for (int i = 0; i < argValues.length; i++) {
                    if (argNames[i] != null) {
                        state.append(argNames[i]);
                        state.append(" = ");
                    }
                    deparseNodeOrValue(state, argValues[i]);
                    if (i == argValues.length - 1) {
                        continue;
                    }
                    state.append(", ");
                }
                state.append(')');
            }
        } else if (node instanceof IfNode) {
            /*
             * We have a problem with { }, since they do not exist as AST nodes (functions), so we
             * insert them routinely
             */
            IfNode ifNode = (IfNode) node;
            state.append("if (");
            deparseNodeOrValue(state, ifNode.getCondition());
            state.append(") ");
            state.writeOpenCurlyNLIncIndent();
            deparseNodeOrValue(state, ifNode.getThenPart());
            state.writeNLDecIndentCloseCurly();
            RNode elsePart = ifNode.getElsePart();
            if (elsePart != null) {
                state.append(" else ");
                state.writeOpenCurlyNLIncIndent();
                deparseNodeOrValue(state, elsePart);
                state.writeNLDecIndentCloseCurly();
            }
        } else if (node instanceof ConvertBooleanNode) {
            // if condition
            deparseNodeOrValue(state, RASTUtils.getChild(node, 0));
        } else if (node instanceof ColonNode) {
            // infix
            deparseNodeOrValue(state, RASTUtils.getChild(node, 0));
            state.append(':');
            deparseNodeOrValue(state, RASTUtils.getChild(node, 1));
        } else if (node instanceof ColonCastNode) {
            deparseNodeOrValue(state, RASTUtils.getChild(node, 0));
        } else if (node instanceof FunctionDefinitionNode) {
            ((RSyntaxNode) node).deparse(state);
        } else {
            if (FastROptions.Debug.getValue()) {
                Utils.debug("deparse: node type " + node.getClass().getSimpleName() + " unhandled, using source");
            }
            SourceSection ss = node.getSourceSection();
            if (ss == null) {
                state.append("<no source available>");
            } else {
                state.append(ss.getCode());
            }

        }
    }

    private static void deparseNodeOrValue(State state, Node arg) {
        Object value = unwrapValue(arg);
        if (value instanceof RNode) {
            deparseNode(state, (RNode) value);
        } else {
            RDeparse.deparse2buff(state, value);
        }
    }

    private static Func isInfixOperator(Object fname) {
        if (fname instanceof RSymbol) {
            return RDeparse.getFunc(((RSymbol) fname).getName());
        }
        return null;
    }

    private static Func isInfixOperatorNode(Node node) {
        if (node instanceof RCallNode || node instanceof DispatchedCallNode) {
            Object fname = RASTUtils.findFunctionName(node, false);
            return isInfixOperator(fname);
        } else {
            return null;
        }
    }

    public static void deparseInfixOperator(RDeparse.State state, Node node, RDeparse.Func func) {
        CallArgumentsNode args = RASTUtils.findCallArgumentsNode(node);
        RNode[] argValues = args.getArguments();
        PP kind = func.info.kind;
        if (kind == PP.BINARY && argValues.length == 1) {
            kind = PP.UNARY;
        }
        switch (kind) {
            case UNARY:
                state.append(func.op);
                deparseNodeOrValue(state, argValues[0]);
                break;

            case BINARY:
            case BINARY2:
                // TODO lbreak
                boolean parens = needsParens(func.info, argValues[0], true);
                if (parens) {
                    state.append('(');
                }
                deparseNodeOrValue(state, argValues[0]);
                if (parens) {
                    state.append(')');
                }
                state.append(' ');
                state.append(func.op);
                state.append(' ');
                parens = needsParens(func.info, argValues[1], false);
                if (parens) {
                    state.append('(');
                }
                deparseNodeOrValue(state, argValues[1]);
                if (parens) {
                    state.append(')');
                }
                break;

            default:
                assert false;
        }
    }

    public static void deparseInfixOperator2(RDeparse.State state, Node node, RDeparse.Func func) {
        CallArgumentsNode args = RASTUtils.findCallArgumentsNode(node);
        RNode[] argValues = args.getArguments();
        PP kind = func.info.kind;
        if (kind == PP.BINARY && argValues.length == 1) {
            kind = PP.UNARY;
        }
        switch (kind) {
            case UNARY:
                state.append(func.op);
                argValues[0].deparse(state);
                break;

            case BINARY:
            case BINARY2:
                // TODO lbreak
                boolean parens = needsParens(func.info, argValues[0], true);
                if (parens) {
                    state.append('(');
                }
                argValues[0].deparse(state);
                if (parens) {
                    state.append(')');
                }
                state.append(' ');
                state.append(func.op);
                state.append(' ');
                parens = needsParens(func.info, argValues[1], false);
                if (parens) {
                    state.append('(');
                }
                argValues[1].deparse(state);
                if (parens) {
                    state.append(')');
                }
                break;

            default:
                assert false;
        }
    }

    private static boolean needsParens(PPInfo mainop, Node arg, boolean isLeft) {
        Node node = RASTUtils.unwrap(arg);
        Func func = isInfixOperatorNode(node);
        if (func != null) {
            CallArgumentsNode args = RASTUtils.findCallArgumentsNode(node);
            PPInfo arginfo = func.info;
            switch (arginfo.kind) {
                case BINARY:
                case BINARY2:
                    switch (args.getArguments().length) {
                        case 1:
                            if (!isLeft) {
                                return false;
                            }
                            if (arginfo.prec == RDeparse.PREC_SUM) {
                                arginfo = new PPInfo(arginfo.kind, RDeparse.PREC_SIGN, arginfo.rightassoc);
                            }
                            // drop through
                        case 2:
                            break;
                        default:
                            return false;
                    }
                    // drop through

                case UNARY:
                    if (mainop.prec > arginfo.prec || (mainop.prec == arginfo.prec && isLeft == mainop.rightassoc)) {
                        return true;
                    }
            }
        } else {
            // TODO complex
        }
        return false;
    }

    private static Object unwrapValue(Node node) {
        Node unode = RASTUtils.unwrap(node);
        if (unode instanceof ConstantNode) {
            return ((ConstantNode) unode).getValue();
        } else if (unode instanceof ReadVariableNode) {
            return RDataFactory.createSymbol(((ReadVariableNode) unode).getSymbol().getName());
        } else {
            return unode;
        }
    }

}
