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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.data.*;

public final class Utils {

    public static Error nyi() {
        throw new UnsupportedOperationException();
    }

    public static Error nyi(String reason) {
        throw new UnsupportedOperationException(reason);
    }

    public static String getProperty(String key, String dfltValue) {
        return System.getProperty(key, dfltValue);
    }

    public static boolean getProperty(String key, boolean dfltValue) {
        return Boolean.parseBoolean(getProperty(key, dfltValue ? "true" : "false"));
    }

    public static final boolean DEBUG = true;

    public static void debug(String msg) {
        if (DEBUG) {
            // CheckStyle: stop system..print check
            System.err.println(msg);
            // CheckStyle: resume system..print check
        }
    }

    public static int incMod(int value, int mod) {
        int result = (value + 1);
        if (result == mod) {
            return 0;
        }
        return result;
    }

    public static SourceSection sourceBoundingBox(Node[] nodes) {
        if (nodes == null || nodes.length == 0) {
            return null;
        }

        int minLine = Integer.MAX_VALUE;
        int minLineColumn = Integer.MAX_VALUE;
        int minCharIndex = Integer.MAX_VALUE;
        int maxCharIndex = Integer.MIN_VALUE;
        boolean gotSection = false;
        Source s = null;

        for (Node n : nodes) {
            if (n == null) {
                continue;
            }
            SourceSection src = n.getSourceSection();
            if (src == null) {
                continue;
            }

            gotSection = true;
            if (s == null) {
                s = src.getSource();
            } else {
                assert s == src.getSource();
            }

            if (src.getStartLine() < minLine) {
                minLine = src.getStartLine();
                if (src.getStartColumn() < minLineColumn) {
                    minLineColumn = src.getStartColumn();
                }
            }
            if (src.getCharIndex() < minCharIndex) {
                minCharIndex = src.getCharIndex();
            }
            if (src.getCharEndIndex() > maxCharIndex) {
                maxCharIndex = src.getCharEndIndex();
            }
        }

        return gotSection ? new DefaultSourceSection(s, "<bounding box>", minLine, minLineColumn, minCharIndex, maxCharIndex - minCharIndex) : null;
    }

    public static void dumpFunction(String groupName, RFunction function) {
        GraphPrintVisitor graphPrinter = new GraphPrintVisitor();
        DefaultCallTarget callTarget = (DefaultCallTarget) function.getTarget();
        if (callTarget != null) {
            graphPrinter.beginGroup(groupName);
            graphPrinter.beginGraph(function.toString()).visit(callTarget.getRootNode());
        }
        graphPrinter.printToNetwork(true);
    }

}
