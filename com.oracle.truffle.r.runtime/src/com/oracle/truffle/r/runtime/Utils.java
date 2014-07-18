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

import java.io.*;
import java.nio.charset.*;
import java.util.concurrent.atomic.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
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

        return gotSection ? s.createSection("<bounding box>", minLine, minLineColumn, minCharIndex, maxCharIndex - minCharIndex) : null;
    }

    public static void dumpFunction(String groupName, RFunction function) {
        GraphPrintVisitor graphPrinter = new GraphPrintVisitor();
        RootCallTarget callTarget = function.getTarget();
        if (callTarget != null) {
            graphPrinter.beginGroup(groupName);
            graphPrinter.beginGraph(RRuntime.toString(function)).visit(callTarget.getRootNode());
        }
        graphPrinter.printToNetwork(true);
    }

    public static String getResourceAsString(Class<?> clazz, String resourceName, boolean mustExist) {
        InputStream is = ResourceHandlerFactory.getHandler().getResourceAsStream(clazz, resourceName);
        if (is == null) {
            if (!mustExist) {
                return null;
            }
        } else {
            try {
                return Utils.getResourceAsString(is);
            } catch (IOException ex) {
            }
        }
        Utils.fail("resource " + resourceName + " not found");
        return null;
    }

    private static String getResourceAsString(InputStream is) throws IOException {
        try (BufferedReader bs = new BufferedReader(new InputStreamReader(is))) {
            char[] buf = new char[1024];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = bs.read(buf, 0, buf.length)) > 0) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
    }

    public static void warn(String msg) {
        // CheckStyle: stop system..print check
        System.err.println("FastR warning: " + msg);
    }

    /**
     * All terminations should go through this method.
     */
    public static void exit(int status) {
        RPerfAnalysis.report();
        System.exit(status);
    }

    public static void fail(String msg) {
        // CheckStyle: stop system..print check
        System.err.println("FastR internal error: " + msg);
        Utils.exit(2);
    }

    public static void fatalError(String msg) {
        System.err.println("Fatal error: " + msg);
        Utils.exit(2);
    }

    private static String userHome;

    private static String userHome() {
        if (userHome == null) {
            userHome = System.getProperty("user.home");
        }
        return userHome;
    }

    public static String tildeExpand(String path) {
        if (path.charAt(0) == '~') {
            return userHome() + path.substring(1);
        } else {
            return path;
        }
    }

    private static Charset UTF8;

    public static Charset getUTF8() {
        if (UTF8 == null) {
            UTF8 = Charset.forName("UTF-8");
        }
        return UTF8;
    }

    /**
     * Retrieve a frame from the call stack. The current frame is at depth 0, caller at depth 1,
     * etc.
     *
     * @param fa kind of access required to the frame
     * @param depth identifies which frame is required
     * @return {@link Frame} instance or {@code null} if {@code depth} is out of range
     */
    public static Frame getStackFrame(FrameAccess fa, int depth) {
        if (depth == 0) {
            return Truffle.getRuntime().getCurrentFrame().getFrame(fa, true);
        }

        LongAdder i = new LongAdder();
        return Truffle.getRuntime().iterateFrames(frameInstance -> {
            Frame f = null;
            i.increment();
            if (i.intValue() == depth) {
                f = frameInstance.getFrame(fa, false);
            }
            return f;
        });
    }

    /**
     * Return the depth of the stack, excluding the current frame and the pseudo-frame at the base.
     */
    public static int stackDepth() {
        LongAdder depth = new LongAdder();

        Truffle.getRuntime().iterateFrames(frameInstance -> {
            depth.increment();
            return null;
        });

        return depth.intValue() - 1;
    }

    /**
     * Retrieve the caller frame of the current frame.
     */
    @SlowPath
    public static Frame getCallerFrame(FrameAccess fa) {
        return getStackFrame(fa, 1);
    }

    /**
     * Generate a stack trace as a string.
     */
    @SlowPath
    public static String createStackTrace() {
        StringBuilder str = new StringBuilder();

        FrameInstance current = Truffle.getRuntime().getCurrentFrame();
        dumpFrame(str, current.getCallTarget(), current.getFrame(FrameAccess.READ_ONLY, true), current.isVirtualFrame());

        Truffle.getRuntime().iterateFrames(frameInstance -> {
            dumpFrame(str, frameInstance.getCallTarget(), frameInstance.getFrame(FrameAccess.READ_ONLY, true), frameInstance.isVirtualFrame());
            return null;
        });

// Iterable<FrameInstance> frames = Truffle.getRuntime().getStackTrace();
// if (frames != null) {
// for (FrameInstance frame : frames) {
// dumpFrame(str, frame.getCallTarget(), frame.getFrame(FrameAccess.READ_ONLY, true),
// frame.isVirtualFrame());
// }
// }
        return str.toString();
    }

    private static void dumpFrame(StringBuilder str, CallTarget callTarget, Frame frame, boolean isVirtual) {
        if (str.length() > 0) {
            str.append("\n");
        }
        str.append("Frame: ").append(callTarget).append(isVirtual ? " (virtual)" : "");
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        for (FrameSlot s : frameDescriptor.getSlots()) {
            str.append("\n  ").append(s.getIdentifier()).append("=").append(frame.getValue(s));
        }
    }

}
