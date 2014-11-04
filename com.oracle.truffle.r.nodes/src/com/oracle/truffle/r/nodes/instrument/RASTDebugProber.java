package com.oracle.truffle.r.nodes.instrument;

import static com.oracle.truffle.api.instrument.StandardSyntaxTag.*;

import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.control.SequenceNode;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.RInternalError;

/**
 * A visitor which traverses a completely parsed R AST (presumed not yet executed) and attaches
 * {@linkplain Probe Probes} at some of them for use by the debugging framework.
 *
 * Syntax nodes in {@link SequenceNode}s are tagged with {@link StandardSyntaxTag#STATEMENT}.
 * Function calls are tagged with {@link StandardSyntaxTag#CALL}. {@link FunctionStatementsNode}s
 * are tagged with {@link StandardSyntaxTag#START_METHOD} which allows the debugger to pause after
 * the arguments have been saved in the frame.
 *
 * N.B. The calls to {@code probe()} insert a {@link WrapperNode} as the parent of the associated
 * node.
 */
public final class RASTDebugProber implements NodeVisitor, ASTProber {

    private static final RASTDebugProber singleton = new RASTDebugProber();

    public static RASTDebugProber getRASTProber() {
        return singleton;
    }

    public void probeAST(Node node) {
        node.accept(this);
    }

    @Override
    public boolean visit(Node node) {
        if (node instanceof RInstrumentableNode) {
            RInstrumentableNode iNode = (RInstrumentableNode) node;
            if (iNode.isInstrumentable()) {
                Probe probe = iNode.probe();
                if (iNode instanceof RCallNode) {
                    probe.tagAs(CALL, RASTUtils.findFunctionName(node, false));
                } else if (iNode instanceof FunctionStatementsNode) {
                    probe.tagAs(START_METHOD, getFunctionDefinitionNode((Node) iNode).getUUID());
                } else if (iNode instanceof FunctionBodyNode) {
                    probe.tagAs(RSyntaxTag.FUNCTION_BODY, getFunctionDefinitionNode((Node) iNode).getUUID());
                }
                if (iNode instanceof SequenceNode) {
                    RNode[] sequence = ((SequenceNode) iNode).getSequence();
                    for (RNode n : sequence) {
                        if (n.isSyntax()) {
                            n.probe().tagAs(STATEMENT, null);
                        }
                    }

                }
            }
        }
        return true;
    }

    private static FunctionDefinitionNode getFunctionDefinitionNode(Node node) {
        if (node instanceof FunctionDefinitionNode) {
            return (FunctionDefinitionNode) node;
        } else if (node == null) {
            throw RInternalError.shouldNotReachHere();
        } else {
            return getFunctionDefinitionNode(node.getParent());
        }
    }
}
