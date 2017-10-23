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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.builtin.RBuiltinBaseNode;

@TypeSystemReference(RTypes.class)
public abstract class RBuiltinNode extends RBuiltinBaseNode implements NodeWithArgumentCasts {

    public abstract Object call(VirtualFrame frame, Object... args);

    /**
     * Return the default values of the builtin's formal arguments. This is only valid for builtins
     * of {@link RBuiltinKind kind} PRIMITIVE or SUBSTITUTE. Only simple scalar constants and
     * {@link RMissing#instance}, {@link RNull#instance} and {@link RArgsValuesAndNames#EMPTY} are
     * allowed.
     */
    public Object[] getDefaultParameterValues() {
        return EMPTY_OBJECT_ARRAY;
    }

    public static final RBuiltinNode inline(RBuiltinDescriptor factory) {
        // static number of arguments
        return ((RBuiltinFactory) factory).getConstructor().get();
    }

    protected final RBuiltin getRBuiltin() {
        return getRBuiltin(getClass());
    }

    private static RBuiltin getRBuiltin(Class<?> klass) {
        GeneratedBy generatedBy = klass.getAnnotation(GeneratedBy.class);
        if (generatedBy != null) {
            return generatedBy.value().getAnnotation(RBuiltin.class);
        } else {
            return null;
        }
    }

    /**
     * Generally, {@link RBuiltinNode} instances are created as child nodes of a private class in
     * {@link RCallNode} that can return the original {@link RCallNode} which has all the pertinent
     * information as initially parsed. However, currently, builtins called via
     * {@code do.call("func", )} have a {@link RBuiltinRootNode} as a parent, which carries no
     * context about the original call, so we return {@code null}.
     */
    public RSyntaxElement getOriginalCall() {
        Node p = getParent();
        while (p != null) {
            if (p instanceof RSyntaxCall) {
                RSyntaxCall call = (RSyntaxCall) p;
                if (call.getSyntaxArguments().length > 0 && call.getSyntaxLHS() instanceof RSyntaxLookup && ((RSyntaxLookup) call.getSyntaxLHS()).getIdentifier().equals(".Internal")) {
                    // unwrap .Internal calls
                    return call.getSyntaxArguments()[0];
                }
                return call;
            }
            p = p.getParent();
        }
        return null;
    }

    @Override
    public String toString() {
        return (getRBuiltin() == null ? getClass().getSimpleName() : getRBuiltin().name());
    }

    @Override
    @TruffleBoundary
    public RBaseNode getErrorContext() {
        ErrorContext context = RError.contextForBuiltin(getRBuiltin());
        return context == null ? this : context;
    }

    @Children private final CastNode[] argumentCasts;
    @CompilationFinal(dimensions = 1) private final Class<?>[] argumentClasses;

    protected RBuiltinNode(int argCount) {
        argumentCasts = getCasts();
        argumentClasses = new Class<?>[argCount];
    }

    protected Object castArg(Object[] args, int index) {
        Object value;
        if (index < argumentCasts.length && argumentCasts[index] != null) {
            value = argumentCasts[index].doCast(args[index]);
        } else {
            value = args[index];
        }
        Class<?> clazz = argumentClasses[index];
        if (clazz == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            argumentClasses[index] = clazz = value.getClass();
            return value;
        } else if (clazz == Object.class) {
            return value;
        } else if (value.getClass() == clazz) {
            if (CompilerDirectives.inInterpreter()) {
                return value;
            }
            return clazz.cast(value);
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            argumentClasses[index] = Object.class;
            return value;
        }
    }

    public abstract static class Arg0 extends RBuiltinNode {

        public abstract Object execute(VirtualFrame frame);

        protected Arg0() {
            super(0);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame);
        }
    }

    public abstract static class Arg1 extends RBuiltinNode {
        public abstract Object execute(VirtualFrame frame, Object arg);

        protected Arg1() {
            super(1);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0));
        }
    }

    public abstract static class Arg2 extends RBuiltinNode {
        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2);

        protected Arg2() {
            super(2);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1));
        }
    }

    public abstract static class Arg3 extends RBuiltinNode {
        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3);

        protected Arg3() {
            super(3);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2));
        }
    }

    public abstract static class Arg4 extends RBuiltinNode {
        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4);

        protected Arg4() {
            super(4);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3));
        }
    }

    public abstract static class Arg5 extends RBuiltinNode {
        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

        protected Arg5() {
            super(5);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4));
        }
    }

    public abstract static class Arg6 extends RBuiltinNode {
        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

        protected Arg6() {
            super(6);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5));
        }
    }

    public abstract static class Arg7 extends RBuiltinNode {

        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

        protected Arg7() {
            super(7);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6));
        }
    }

    public abstract static class Arg8 extends RBuiltinNode {

        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8);

        protected Arg8() {
            super(8);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6), castArg(args, 7));
        }
    }

    public abstract static class Arg9 extends RBuiltinNode {

        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9);

        protected Arg9() {
            super(9);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6), castArg(args, 7), castArg(args, 8));
        }
    }

    public abstract static class Arg10 extends RBuiltinNode {

        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10);

        protected Arg10() {
            super(10);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6), castArg(args, 7), castArg(args, 8),
                            castArg(args, 9));
        }
    }

    public abstract static class Arg11 extends RBuiltinNode {

        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
                        Object arg11);

        protected Arg11() {
            super(11);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6), castArg(args, 7), castArg(args, 8),
                            castArg(args, 9), castArg(args, 10));
        }
    }

    public abstract static class Arg19 extends RBuiltinNode {

        public abstract Object execute(VirtualFrame frame, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
                        Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18, Object arg19);

        protected Arg19() {
            super(19);
        }

        @Override
        public final Object call(VirtualFrame frame, Object... args) {
            return execute(frame, castArg(args, 0), castArg(args, 1), castArg(args, 2), castArg(args, 3), castArg(args, 4), castArg(args, 5), castArg(args, 6), castArg(args, 7), castArg(args, 8),
                            castArg(args, 9), castArg(args, 10), castArg(args, 11), castArg(args, 12), castArg(args, 13), castArg(args, 14), castArg(args, 15), castArg(args, 16), castArg(args, 17),
                            castArg(args, 18));
        }
    }
}
