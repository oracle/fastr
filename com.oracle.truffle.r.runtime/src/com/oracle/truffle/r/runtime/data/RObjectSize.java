/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

/**
 * Support for the sizing of the objects that flow through the interpreter, i.e., mostly
 * {@link RTypedValue}, but also including scalar types like {@code String} and dimension data for
 * arrays, i.e., {@code int[]}.
 *
 * The actually implementation is controlled by {@link ObjectSizeFactory} to finesse problems with
 * Java VMs that do not support reflection.
 *
 * Owing to the (implementation) complexity of some of the types, two levels of customization are
 * provided:
 * <ol>
 * <li>A completely custom sizing implementation can be provided for a specific type. This effects
 * all sizing computations.</li>
 * <li>In any given call to {@link #getObjectSize} an instance of {@IgnoreObjectHandler} can passed.
 * This allows some additional dynamic control over certain fields depending of the context of the
 * call. For example, when tracking the incremental memory allocation via {@link RDataFactory}, we
 * do not want to (double) count fields of type {@link RTypedValue}. However, when computing the
 * total size of the object, e.g. for the {@code utils::object.size} builtin, we do want to count
 * them.</li>
 * </ol>
 *
 */
public class RObjectSize {
    public static final int INT_SIZE = 32;
    public static final int DOUBLE_SIZE = 64;
    public static final int BYTE_SIZE = 8;

    public interface TypeCustomizer {
        /**
         * Allows complete control over sizing of a type registered with
         * {@link #registerTypeCustomizer}.
         */
        long getObjectSize(Object obj);
    }

    public interface IgnoreObjectHandler {
        /**
         * Controls which fields of an object passed to {@link #getObjectSize} will be ignored.
         * {@code rootObject} is the initiating object and {@code obj} is some field of that object
         * or one of its components. The return value should be {@code true} if {@code obj} should
         * be ignored.
         */
        boolean ignore(Object rootObject, Object obj);
    }

    /**
     * Returns an estimate of the size of the this object, including the size of any object-valued
     * fields, recursively. Evidently this is a snapshot and the size can change as, e.g.,
     * attributes are added/removed.
     *
     * If called immediately after creation by {@link RDataFactory}, with an
     * {@link IgnoreObjectHandler} that ignores objects created separately and, it provides an
     * approximation of the incremental memory usage of the system.
     *
     * @param ignoreObjectHandler An object that is called to decide whether to include the
     *            contribution a field of this object (and its sub-objects) in the result. Passing
     *            {@code null} includes everything. N.B. {@code obj} is typed as {@code Object} only
     *            to allow scalar typed such as {@code String} to be passed.
     *
     */
    public static long getObjectSize(Object obj, IgnoreObjectHandler ignoreObjectHandler) {
        return (int) ObjectSizeFactory.getInstance().getObjectSize(obj, ignoreObjectHandler);
    }

    /**
     * Register a {@link TypeCustomizer} for {@code klass} and its subclasses. I.e. and object
     * {@code obj} is customized iff {@code klass.isAssignableFrom(obj.getClass())}.
     */
    public static void registerTypeCustomizer(Class<?> klass, TypeCustomizer typeCustomizer) {
        ObjectSizeFactory.getInstance().registerTypeCustomizer(klass, typeCustomizer);
    }

    /**
     * This denotes a special customizer that completely ignores instances of the type and its
     * subclasses. It allows a more efficient implementation as the type can be suppressed
     * completely from the computation at the time fields of a containing type are analyzed.
     */
    public static final TypeCustomizer IGNORE = new TypeCustomizer() {

        @Override
        public long getObjectSize(Object obj) {
            return 0;
        }
    };

    // TODO construct proper customizers for some of these.
    static {
        registerTypeCustomizer(Frame.class, IGNORE);
        registerTypeCustomizer(FrameDescriptor.class, IGNORE);
        registerTypeCustomizer(Node.class, IGNORE);
        registerTypeCustomizer(CallTarget.class, IGNORE);
        registerTypeCustomizer(RBuiltinDescriptor.class, IGNORE);
        registerTypeCustomizer(RPromise.Closure.class, IGNORE);
        registerTypeCustomizer(Assumption.class, IGNORE);
        registerTypeCustomizer(RCaller.class, IGNORE);
        registerTypeCustomizer(SEXPTYPE.class, IGNORE);
    }
}
