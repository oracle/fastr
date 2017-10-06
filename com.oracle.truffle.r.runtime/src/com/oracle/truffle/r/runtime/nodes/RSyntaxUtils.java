/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RDeparse;

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
                    str.append("\n" + space() + "{");
                    level++;
                    for (int i = 0; i < arguments.length; i++) {
                        RSyntaxElement child = arguments[i];
                        str.append("\n" + space() + accept(child));
                    }
                    level--;
                    str.append("\n" + space() + "}\n" + space());
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
                    str.append((i > 0 ? ", " : "") + (callSignature.getName(i) == null ? "" : callSignature.getName(i) + "=") + (child == null ? "" : accept(child)));
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
        System.out.println("================= " + desc);
        System.out.println("==== arguments");
        for (int i = 0; i < formals.length; i++) {
            RSyntaxNode node = formals[i];
            System.out.print(signature.getName(i) + "=");
            System.out.println(node == null ? "" : visitor.accept(node));
        }
        System.out.println("==== body");
        System.out.println(visitor.accept(body));
    }
}
