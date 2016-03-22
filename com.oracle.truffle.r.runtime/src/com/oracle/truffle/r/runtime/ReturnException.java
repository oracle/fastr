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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.nodes.ControlFlowException;

@SuppressWarnings("serial")
public final class ReturnException extends ControlFlowException {

    private final Object result;
    private final int depth;

    /**
     * Support for the "return" builtin.
     *
     * @param result the value to return
     * @param depth if not -1, the depth of the frame of the function that the return should go to,
     *            skipping intermediate frames.
     */
    public ReturnException(Object result, int depth) {
        this.result = result;
        this.depth = depth;
    }

    /**
     * Support for the "return" builtin.
     *
     * @param result the value to return
     */
    public ReturnException(Object result) {
        this.result = result;
        this.depth = -1;
    }

    /**
     * @return the unexpected result
     */
    public Object getResult() {
        return result;
    }

    public int getDepth() {
        return depth;
    }
}
