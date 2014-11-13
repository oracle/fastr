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
