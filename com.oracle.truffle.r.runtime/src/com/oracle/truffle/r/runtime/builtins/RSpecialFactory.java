/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.builtins;

import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * A builtin can have a node that can handle very simple cases for which (unlike with fast-path) no
 * argument matching has to be performed. Such simple case node, called special, is created for the
 * built-in by AST builder by default. If the special node cannot handle its arguments, it throws
 * {@link #FULL_CALL_NEEDED} and the it will be replaced with call to the full blown built-in.
 */
public interface RSpecialFactory {
    RuntimeException FULL_CALL_NEEDED = new FullCallNeededException();

    /**
     * Returns a 'special node' if the given arguments with their signature can be handled by it. If
     * if returns {@code null}, the full blown built-in node will be created.
     */
    RNode create(ArgumentsSignature argumentsSignature, RNode[] arguments);

    @SuppressWarnings("serial")
    final class FullCallNeededException extends RuntimeException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }
}
