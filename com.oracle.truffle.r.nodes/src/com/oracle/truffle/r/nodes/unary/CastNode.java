/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import java.util.Arrays;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Cast nodes behave like unary nodes, but in many cases it is useful to have a specific type for
 * casts.
 */
public abstract class CastNode extends UnaryNode {

    private static boolean isTesting = false;
    private static String lastWarning;

    public static void testingMode() {
        isTesting = true;
    }

    /**
     * For testing purposes only, returns the last warning message (only when {@link #testingMode()}
     * was invoked before).
     */
    public static String getLastWarning() {
        return lastWarning;
    }

    public static void clearLastWarning() {
        lastWarning = null;
    }

    @SuppressWarnings({"unchecked"})
    private static Object[] substituteArgs(Object arg, MessageData message) {
        Object[] messageArgs = message.getMessageArgs();
        Object[] newMsgArgs = Arrays.copyOf(messageArgs, messageArgs.length);

        for (int i = 0; i < messageArgs.length; i++) {
            final Object msgArg = messageArgs[i];
            if (msgArg instanceof Function) {
                newMsgArgs[i] = ((Function<Object, Object>) msgArg).apply(arg);
            }
        }
        return newMsgArgs;
    }

    private RBaseNode getCallObj(MessageData message) {
        return message.getCallObj() == null ? this : message.getCallObj();
    }

    @TruffleBoundary
    protected RuntimeException handleArgumentError(Object arg, MessageData message) {
        if (isTesting) {
            throw new IllegalArgumentException(String.format(message.getMessage().message, substituteArgs(arg, message)));
        } else {
            throw RError.error(getCallObj(message), message.getMessage(), substituteArgs(arg, message));
        }
    }

    @TruffleBoundary
    protected void handleArgumentWarning(Object arg, MessageData message) {
        if (message == null) {
            return;
        }
        if (isTesting) {
            lastWarning = String.format(message.getMessage().message, substituteArgs(arg, message));
        } else {
            RError.warning(getCallObj(message), message.getMessage(), substituteArgs(arg, message));
        }
    }
}
