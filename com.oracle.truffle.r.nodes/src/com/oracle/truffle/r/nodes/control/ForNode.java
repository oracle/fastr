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
package com.oracle.truffle.r.nodes.control;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@NodeChild(value = "range", type = RNode.class)
public abstract class ForNode extends LoopNode {

    @Child private WriteVariableNode cvar;
    @Child private RNode body;

    protected ForNode(WriteVariableNode cvar, RNode body) {
        this.cvar = cvar;
        this.body = body;
    }

    protected ForNode(ForNode prev) {
        this(prev.cvar, prev.body);
    }

    public static ForNode create(WriteVariableNode cvar, RNode range, RNode body) {
        return ForNodeFactory.create(cvar, body, range);
    }

    public static ForNode create(SourceSection src, WriteVariableNode cvar, RNode range, RNode body) {
        ForNode fn = create(cvar, range, body);
        fn.assignSourceSection(src);
        return fn;
    }

    @Specialization
    public Object doSequence(VirtualFrame frame, RIntSequence range) {
        int count = 0;
        try {
            for (int i = 0, pos = range.getStart(); i < range.getLength(); i++, pos += range.getStride()) {
                cvar.execute(frame, pos);
                try {
                    body.execute(frame);
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }
                } catch (NextException e) {
                }
            }
        } catch (BreakException e) {
        } finally {
            if (CompilerDirectives.inInterpreter()) {
                reportLoopCount(count);
            }
        }
        return RNull.instance;
    }

    @Specialization
    public Object doSequence(VirtualFrame frame, RAbstractVector vector) {
        int count = 0;
        try {
            for (int i = 0; i < vector.getLength(); ++i) {
                cvar.execute(frame, vector.getDataAtAsObject(i));
                try {
                    body.execute(frame);
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }
                } catch (NextException e) {
                }
            }
        } catch (BreakException e) {
        } finally {
            if (CompilerDirectives.inInterpreter()) {
                reportLoopCount(count);
            }
        }
        return RNull.instance;
    }

    @Specialization
    public Object doSequence(VirtualFrame frame, RExpression expr) {
        return doSequence(frame, expr.getList());
    }

    @Specialization
    public Object doSequence(VirtualFrame frame, int x) {
        int count = 0;
        try {
            cvar.execute(frame, x);
            try {
                body.execute(frame);
                if (CompilerDirectives.inInterpreter()) {
                    count++;
                }
            } catch (NextException e) {
            }
        } catch (BreakException e) {
        } finally {
            if (CompilerDirectives.inInterpreter()) {
                reportLoopCount(count);
            }
        }
        return RNull.instance;
    }

    @Specialization
    public Object doSequence(VirtualFrame frame, double x) {
        int count = 0;
        try {
            cvar.execute(frame, x);
            try {
                body.execute(frame);
                if (CompilerDirectives.inInterpreter()) {
                    count++;
                }
            } catch (NextException e) {
            }
        } catch (BreakException e) {
        } finally {
            if (CompilerDirectives.inInterpreter()) {
                reportLoopCount(count);
            }
        }
        return RNull.instance;
    }

    @Specialization
    public Object doSequence(VirtualFrame frame, String x) {
        int count = 0;
        try {
            cvar.execute(frame, x);
            try {
                body.execute(frame);
                if (CompilerDirectives.inInterpreter()) {
                    count++;
                }
            } catch (NextException e) {
            }
        } catch (BreakException e) {
        } finally {
            if (CompilerDirectives.inInterpreter()) {
                reportLoopCount(count);
            }
        }
        return RNull.instance;
    }
}
