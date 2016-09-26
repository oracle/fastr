/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Cast nodes behave like unary nodes, but in many cases it is useful to have a specific type for
 * casts.
 */
public abstract class CastNode extends UnaryNode {

    @TruffleBoundary
    protected static void handleArgumentError(Object arg, RBaseNode callObj, RError.Message message, Object[] messageArgs) {
        if (RContext.getInstance() == null) {
            throw new IllegalArgumentException(String.format(message.message, substituteArgPlaceholder(arg, messageArgs)));
        } else {
            throw RError.error(callObj, message, substituteArgPlaceholder(arg, messageArgs));
        }
    }

    @TruffleBoundary
    protected static void handleArgumentWarning(Object arg, RBaseNode callObj, RError.Message message, Object[] messageArgs) {
        if (message == null) {
            return;
        }

        if (RContext.getInstance() == null) {
            System.err.println(String.format(message.message, substituteArgPlaceholder(arg,
                            messageArgs)));
        } else {
            RError.warning(callObj, message, substituteArgPlaceholder(arg, messageArgs));
        }
    }

    @SuppressWarnings({"unchecked"})
    public static Object[] substituteArgPlaceholder(Object arg, Object[] messageArgs) {
        Object[] newMsgArgs = Arrays.copyOf(messageArgs, messageArgs.length);

        for (int i = 0; i < messageArgs.length; i++) {
            final Object msgArg = messageArgs[i];
            if (msgArg instanceof Function) {
                newMsgArgs[i] = ((Function<Object, Object>) msgArg).apply(arg);
            }
        }

        return newMsgArgs;
    }
}
