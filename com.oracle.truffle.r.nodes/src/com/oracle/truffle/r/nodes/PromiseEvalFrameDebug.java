/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.PromiseEvalFrame;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Debugging support to trace the evaluation of promises that require a {@link PromiseEvalFrame} and
 * the frame depth analysis. Output, when enabled is sent to "fastr_pefd.log".
 *
 */
public class PromiseEvalFrameDebug {

    /**
     * For use in calling code to check whether to trace. Set {@code true} for tracing to take
     * effect.
     */
    @CompilationFinal public static boolean enabled/* = true */;

    private static final String DEBUG_TAG = "pefd";

    private static PrintStream pstream;

    private static PrintStream out() {
        if (pstream == null) {
            try {
                pstream = new PrintStream(new FileOutputStream("fastr_pefd.log"));
            } catch (IOException ex) {
                throw RInternalError.shouldNotReachHere();
            }
        }
        return pstream;
    }

    private static boolean enabled() {
        return enabled && FastROptions.debugMatches(DEBUG_TAG);
    }

    private static int level;

    public static void doPromiseEval(boolean start, Frame frame, Frame promiseFrame, RPromise promise) {
        if (enabled()) {
            if (!start) {
                level--;
            }
            out().printf("**%s promise eval(%d): depth %d, pfd %d, '%s'%n", (start ? "starting" : "ending"), level, frame == null ? -1 : RArguments.getDepth(frame), RArguments.getDepth(promiseFrame),
                            getPromiseSrc(promise));
            if (start) {
                level++;
            }
        }

    }

    public static void log(String s) {
        if (enabled()) {
            out().println(s);
        }
    }

    private static String getPromiseSrc(RPromise promise) {
        RBaseNode node = RASTUtils.unwrap(promise.getRep());
        if (node.isRSyntaxNode()) {
            RSyntaxNode sn = node.asRSyntaxNode();
            return sn.getSourceSection().getCode();
        } else {
            return "<no source>";
        }
    }

    public static void dumpStack(String id) {
        if (enabled()) {
            out().printf("%s: %s%n", id, Utils.createStackTrace(false));
        }

    }

    public static void noPromise(RBuiltinNode node, int depth) {
        if (enabled()) {
            out().printf("getEffectiveDepth[%s](%d): no promise eval in progress%n", getCode(node.getOriginalCall()), depth);
        }
    }

    public static void match(RBuiltinNode node, boolean match, PromiseEvalFrame pf, int depth) {
        if (enabled()) {
            out().printf("getEffectiveDepth[%s](%d) match=%b on: %s%n", getCode(node.getOriginalCall()), depth, match, getCode(pf.getPromise().getRep().asRSyntaxNode()));
        }
    }

    private static String getCode(RSyntaxNode node) {
        return node.getSourceSection().getCode();
    }

}
