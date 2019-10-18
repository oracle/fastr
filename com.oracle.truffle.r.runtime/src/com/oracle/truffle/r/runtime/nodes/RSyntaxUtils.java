/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.Argument;

import java.util.ArrayList;

public abstract class RSyntaxUtils {

    private RSyntaxUtils() {
        // empty
    }

    private static final String SPACE = "                                                                                                  ";

    /**
     * This is a simple example usage of the {@link RSyntaxVisitor} class that prints the given
     * formals and body to the console.
     */
    public static void verifyFunction(String desc, ArgumentsSignature signature, RSyntaxNode[] formals, RSyntaxNode body) {
        RSyntaxVisitor<String> visitor = new RSyntaxVisitor<String>() {
            int level = 0;

            private String space() {
                return SPACE.substring(0, level * 4);
            }

            @Override
            protected String visit(RSyntaxCall element) {
                StringBuilder str = new StringBuilder();
                RSyntaxElement lhs = element.getSyntaxLHS();
                RSyntaxElement[] arguments = element.getSyntaxArguments();
                ArgumentsSignature callSignature = element.getSyntaxSignature();
                if (lhs instanceof RSyntaxLookup && "{".equals(((RSyntaxLookup) lhs).getIdentifier())) {
                    str.append("\n").append(space()).append("{");
                    level++;
                    for (int i = 0; i < arguments.length; i++) {
                        RSyntaxElement child = arguments[i];
                        str.append("\n").append(space()).append(accept(child));
                    }
                    level--;
                    str.append("\n").append(space()).append("}\n").append(space());
                } else {
                    str.append(accept(lhs));
                    printArguments(str, arguments, callSignature);
                }
                return str.toString();
            }

            private void printArguments(StringBuilder str, RSyntaxElement[] arguments, ArgumentsSignature callSignature) {
                str.append('(');
                for (int i = 0; i < arguments.length; i++) {
                    RSyntaxElement child = arguments[i];
                    str.append(i > 0 ? ", " : "").append(callSignature.getName(i) == null ? "" : callSignature.getName(i) + "=").append(child == null ? "" : accept(child));
                }
                str.append(')');
            }

            @Override
            protected String visit(RSyntaxConstant element) {
                return "<" + element.getValue().getClass().getSimpleName() + " " + element.getValue() + ">";
            }

            @Override
            protected String visit(RSyntaxLookup element) {
                String name = element.getIdentifier();
                return RDeparse.isValidName(name) ? name : "`" + name + "`";
            }

            @Override
            protected String visit(RSyntaxFunction element) {
                StringBuilder str = new StringBuilder("function");
                printArguments(str, element.getSyntaxArgumentDefaults(), element.getSyntaxSignature());
                str.append(' ').append(accept(element.getSyntaxBody()));
                return str.toString();
            }
        };
        TruffleLogger logger = RLogger.getLogger(RSyntaxUtils.class.getName());
        logger.info("================= " + desc);
        logger.info("==== arguments");
        for (int i = 0; i < formals.length; i++) {
            RSyntaxNode node = formals[i];
            logger.info(signature.getName(i) + "=" + (node == null ? "" : visitor.accept(node)));
        }
        logger.info("==== body");
        logger.info(visitor.accept(body));
    }

    public static ArrayList<Argument<RSyntaxNode>> createArgumentsList(RSyntaxElement[] arguments, ArgumentsSignature signature) {
        ArrayList<Argument<RSyntaxNode>> result = new ArrayList<>(arguments.length);
        createArgumentsList(0, arguments, signature, result);
        return result;
    }

    public static void createArgumentsList(int offset, RSyntaxElement[] arguments, ArgumentsSignature signature, ArrayList<Argument<RSyntaxNode>> result) {
        RCodeBuilder<RSyntaxNode> builder = RContext.getASTBuilder();
        for (int j = offset; j < arguments.length; j++) {
            result.add(RCodeBuilder.argument(arguments[j] == null ? null : arguments[j].getLazySourceSection(), signature.getName(j),
                            arguments[j] == null ? null : builder.process(arguments[j])));
        }
    }
}
