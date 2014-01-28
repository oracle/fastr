/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import java.io.*;
import java.util.*;

import org.antlr.runtime.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.parser.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public class RLibraryLoader {

    private final File libFile;

    public RLibraryLoader(File libFile) {
        this.libFile = libFile;
    }

    public Map<String, FunctionExpressionNode.StaticFunctionExpressionNode> loadLibrary() {
        RNode library = parseLibrary();
        assert library instanceof SequenceNode;

        Map<String, FunctionExpressionNode.StaticFunctionExpressionNode> builtinDefs = new HashMap<>();
        for (Node libNode : library.getChildren()) {
            if (isFunctionDefinition(libNode)) {
                WriteVariableNode.UnresolvedWriteLocalVariableNode fnDef = (WriteVariableNode.UnresolvedWriteLocalVariableNode) libNode;
                String builtinName = fnDef.getSymbol().toString();
                FunctionExpressionNode.DynamicFunctionExpressionNode builtinExpr = (FunctionExpressionNode.DynamicFunctionExpressionNode) fnDef.getRhs();
                builtinDefs.put(builtinName, new FunctionExpressionNode.StaticFunctionExpressionNode(new RFunction("", builtinExpr.getCallTarget(), false)));
            }
        }
        return builtinDefs;
    }

    private static boolean isFunctionDefinition(Node node) {
        return (node instanceof WriteVariableNode.UnresolvedWriteLocalVariableNode) && (((WriteVariableNode) node).getRhs() instanceof FunctionExpressionNode);
    }

    private RNode parseLibrary() {
        ANTLRFileStream stream;
        try {
            stream = new ANTLRFileStream(libFile.getAbsolutePath());
            RTruffleVisitor transform = new RTruffleVisitor();
            RNode node = transform.transform(parseAST(stream, RContext.getInstance().getSourceManager().get(libFile.getAbsolutePath())));
            return node;
        } catch (RecognitionException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static ASTNode parseAST(ANTLRStringStream stream, Source source) throws RecognitionException {
        CommonTokenStream tokens = new CommonTokenStream();
        RLexer lexer = new RLexer(stream);
        tokens.setTokenSource(lexer);
        RParser parser = new RParser(tokens);
        parser.setSource(source);
        return parser.script().v;
    }

}
