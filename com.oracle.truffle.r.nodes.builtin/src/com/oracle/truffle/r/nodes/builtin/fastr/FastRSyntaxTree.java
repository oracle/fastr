package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "fastr.syntaxtree", kind = PRIMITIVE, parameterNames = {"func"})
@RBuiltinComment("Prints the syntactic view of the Truffle tree of a function.")
public abstract class FastRSyntaxTree extends RInvisibleBuiltinNode {
    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected Object printTree(RFunction function) {
        controlVisibility();
        Node root = function.getTarget().getRootNode();
        RSyntaxNode.accept(root, 0, new RSyntaxNodeVisitor() {

            public boolean visit(RSyntaxNode node, int depth) {
                for (int i = 0; i < depth; i++) {
                    System.out.print(' ');
                }
                if (node instanceof FunctionDefinitionNode) {
                    System.out.println(((FunctionDefinitionNode) node).parentToString());
                } else {
                    System.out.println(node.toString());
                }
                return true;
            }
        });
        return RNull.instance;
    }

    @Specialization
    protected RNull printTree(@SuppressWarnings("unused") RMissing function) {
        controlVisibility();
        throw RError.error(RError.Message.ARGUMENTS_PASSED_0_1);
    }

    @Fallback
    protected RNull printTree(@SuppressWarnings("unused") Object function) {
        controlVisibility();
        throw RError.error(RError.Message.INVALID_ARGUMENT, "func");
    }

}
