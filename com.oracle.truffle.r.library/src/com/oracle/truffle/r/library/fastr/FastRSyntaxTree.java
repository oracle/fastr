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
package com.oracle.truffle.r.library.fastr;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.nodes.*;

public abstract class FastRSyntaxTree extends RExternalBuiltinNode.Arg3 {
    @Specialization
    @TruffleBoundary
    protected RNull printTree(RFunction function, byte source, byte visitAll) {
        boolean printSource = RRuntime.fromLogical(source);
        Node root = function.getTarget().getRootNode();
        if (RRuntime.fromLogical(visitAll)) {
            root.accept(new NodeVisitor() {

                public boolean visit(Node node) {
                    if (RBaseNode.isRSyntaxNode(node)) {
                        SourceSection ss = node.getSourceSection();
                        // All syntax nodes should have source sections
                        if (ss == null) {
                            System.out.printf("*** %s has null source section%n", node.getClass().getSimpleName());
                        }
                    }
                    return true;
                }

            });
        } else {
            RSyntaxNode.accept(root, 0, new RSyntaxNodeVisitor() {

                public boolean visit(RSyntaxNode node, int depth) {
                    for (int i = 0; i < depth; i++) {
                        System.out.print(' ');
                    }
                    System.out.print(node.getClass().getSimpleName());
                    SourceSection ss = ((Node) node).getSourceSection();
                    // All syntax nodes should have source sections
                    if (ss == null) {
                        System.out.print(" *** null source section");
                    } else {
                        if (printSource) {
                            printSourceCode(ss);
                        }
                    }
                    System.out.println();
                    return true;
                }
            }, true);
        }
        return RNull.instance;
    }

    private static void printSourceCode(SourceSection ss) {
        String code = ss.getCode();
        if (code.length() > 20) {
            code = code.substring(0, 20) + " ....";
        }
        code = code.replace("\n", "\\n ");
        System.out.print(" : ");
        System.out.print(code.length() == 0 ? "<EMPTY>" : code);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object fallback(Object a1, Object a2, Object a3) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

}
