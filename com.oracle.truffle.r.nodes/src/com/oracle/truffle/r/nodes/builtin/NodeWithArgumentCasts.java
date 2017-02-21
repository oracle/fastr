/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ConcurrentHashMap;

import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PreinitialPhaseBuilder;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticBuiltinNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;

public interface NodeWithArgumentCasts {

    default Casts getCastHolder() {
        return getStaticCasts(getClass());
    }

    static Casts getStaticCasts(Class<?> builtinClass) {
        Casts casts = Casts.getCasts(builtinClass);
        return casts == null ? Casts.empty : casts;
    }

    default CastNode[] getCasts() {
        CastNode[] casts = getCastHolder().getCastNodes();
        return casts;
    }

    final class Casts {
        private static final ConcurrentHashMap<Class<?>, Casts> castsMap = new ConcurrentHashMap<>();
        private static final Casts empty = new Casts(false);

        protected final CastBuilder casts;
        private final boolean declaresNoCasts;

        private Casts(boolean noCasts) {
            casts = new CastBuilder();
            this.declaresNoCasts = noCasts;
        }

        private Casts(Class<? extends NodeWithArgumentCasts> cls, boolean noCasts) {
            castsMap.put(cls, this);
            casts = new CastBuilder(cls.getAnnotation(RBuiltin.class));
            this.declaresNoCasts = noCasts;
        }

        public Casts(Class<? extends NodeWithArgumentCasts> cls) {
            this(cls, false);
        }

        public static void noCasts(Class<? extends NodeWithArgumentCasts> cls) {
            castsMap.put(cls, new Casts(cls, true));
        }

        /**
         * Indicates that a builtin uses the {@link Casts#noCasts(Class)} to declare that it uses no
         * arguments casts although it has one or more arguments.
         */
        public boolean declaresNoCasts() {
            return declaresNoCasts;
        }

        public static Casts forClass(Class<?> cls) {
            return castsMap.get(cls);
        }

        public CastNode[] getCastNodes() {
            return casts.getCasts();
        }

        public PipelineBuilder[] getPipelineBuilders() {
            return casts.getPipelineBuilders();
        }

        public PreinitialPhaseBuilder arg(String argumentName) {
            return casts.arg(argumentName);
        }

        public PreinitialPhaseBuilder arg(int argumentIndex, String argumentName) {
            return casts.arg(argumentIndex, argumentName);
        }

        public PreinitialPhaseBuilder arg(int argumentIndex) {
            return casts.arg(argumentIndex);
        }

        public static Casts getCasts(Class<?> builtinClass) {
            Class<?> cls = Casts.getBuiltinClass(builtinClass);
            Casts casts = Casts.forClass(cls);
            if (casts == null && !noCastsAllowed(cls)) {
                throw RInternalError.shouldNotReachHere("No casts associated with builtin " + cls);
            }
            return casts;
        }

        private static boolean noCastsAllowed(Class<?> cls) {
            boolean res = RExternalBuiltinNode.Arg0.class.isAssignableFrom(cls) || UnaryArithmeticBuiltinNode.class.isAssignableFrom(cls) ||
                            (RBuiltinNode.class.isAssignableFrom(cls) && hasNoArguments(cls));
            return res;
        }

        private static boolean hasNoArguments(Class<?> cls) {
            RBuiltin a = cls.getAnnotation(RBuiltin.class);
            return a != null && a.parameterNames().length == 0;
        }

        public static Class<?> getBuiltinClass(Class<?> cls) {
            if (cls.getAnnotation(GeneratedBy.class) != null) {
                return cls.getSuperclass();
            } else {
                return cls;
            }
        }
    }
}
