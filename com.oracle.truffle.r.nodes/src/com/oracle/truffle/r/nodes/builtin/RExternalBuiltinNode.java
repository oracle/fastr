/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.builtin.RBuiltinBaseNode;

@TypeSystemReference(RTypes.class)
public abstract class RExternalBuiltinNode extends RBuiltinBaseNode implements NodeWithArgumentCasts {

    public Object call(@SuppressWarnings("unused") VirtualFrame frame, RArgsValuesAndNames args) {
        return call(args);
    }

    protected abstract Object call(RArgsValuesAndNames args);

    @Override
    protected RBaseNode getErrorContext() {
        return RError.NO_CALLER;
    }

    @Children private final CastNode[] argumentCasts;

    public RExternalBuiltinNode() {
        this.argumentCasts = getCasts();
    }

    protected void checkLength(RArgsValuesAndNames args, int expectedLength) {
        if (args.getLength() != expectedLength) {
            CompilerDirectives.transferToInterpreter();
            String name = this.getClass().getSimpleName();
            if (name.endsWith("NodeGen")) {
                name = name.substring(0, name.length() - 7);
            }
            // this error always shows the .Call itself
            throw RError.error(this, Message.INCORRECT_NOF_ARGS, args.getLength(), expectedLength, name.toLowerCase());
        }
    }

    protected Object castArg(RArgsValuesAndNames args, int index) {
        return index < argumentCasts.length && argumentCasts[index] != null ? argumentCasts[index].doCast(args.getArgument(index)) : args.getArgument(index);
    }

    public abstract static class Arg0 extends RExternalBuiltinNode {
        public abstract Object execute();

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 0);
            return execute();
        }
    }

    public abstract static class Arg1 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 1);
            return execute(castArg(args, 0));
        }
    }

    public abstract static class Arg2 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 2);
            return execute(castArg(args, 0), castArg(args, 1));
        }
    }

    public abstract static class Arg3 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2, Object arg3);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 3);
            return execute(castArg(args, 0), castArg(args, 1), castArg(args, 2));
        }
    }

    public abstract static class Arg4 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 4);
            return execute(castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3));
        }
    }

    public abstract static class Arg5 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 5);
            return execute(castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4));
        }
    }

    public abstract static class Arg6 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 6);
            return execute(castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5));
        }
    }

    public abstract static class Arg7 extends RExternalBuiltinNode {

        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 7);
            return execute(castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6));
        }
    }

    public abstract static class Arg8 extends RExternalBuiltinNode {

        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 8);
            return execute(castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6), castArg(args, 7));
        }
    }

    public abstract static class Arg9 extends RExternalBuiltinNode {

        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 9);
            return execute(castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6), castArg(args, 7), castArg(args, 8));
        }
    }

    public abstract static class Arg10 extends RExternalBuiltinNode {

        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 10);
            return execute(castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6), castArg(args, 7), castArg(args, 8),
                            castArg(args, 9));
        }
    }

    public abstract static class Arg11 extends RExternalBuiltinNode {

        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 11);
            return execute(castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6), castArg(args, 7), castArg(args, 8),
                            castArg(args, 9), castArg(args, 10));
        }
    }
}
