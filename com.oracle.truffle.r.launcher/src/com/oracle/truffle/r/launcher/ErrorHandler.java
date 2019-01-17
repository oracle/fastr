/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.PolyglotException.StackFrame;

abstract class ErrorHandler {
    private ErrorHandler() {
        // no instances
    }

    static void handleError(PolyglotException e, OutputStream output) throws IOException {
        output.write(getMessage(e).getBytes());
        output.flush();
    }

    private static String getMessage(PolyglotException eIn) {
        PolyglotException e = eIn;
        if (eIn.getCause() instanceof PolyglotException) {
            e = (PolyglotException) eIn.getCause();
        }

        List<StackFrame> stackTrace = new ArrayList<>();
        for (StackFrame s : e.getPolyglotStackTrace()) {
            stackTrace.add(s);
        }

        // remove trailing host frames
        for (ListIterator<StackFrame> iterator = stackTrace.listIterator(stackTrace.size()); iterator.hasPrevious();) {
            StackFrame s = iterator.previous();
            if (s.isHostFrame()) {
                iterator.remove();
            } else {
                break;
            }
        }

        // remove trailing <R> frames
        for (ListIterator<StackFrame> iterator = stackTrace.listIterator(stackTrace.size()); iterator.hasPrevious();) {
            StackFrame s = iterator.previous();
            if (s.getLanguage().getId().equals("R")) {
                iterator.remove();
            } else {
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        if (e.isHostException()) {
            sb.append(e.asHostException().toString());
        } else if (e.getMessage() != null) {
            sb.append(e.getMessage()).append('\n');
        }
        sb.append('\n');
        for (StackFrame s : stackTrace) {
            sb.append("\tat ");
            sb.append(s);
            sb.append('\n');
        }
        return sb.toString();
    }
}
