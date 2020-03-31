/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tags an upcall argument or the return type as being (on the native side) a C array.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface RFFICarray {

    enum Type {
        Any(null, null),
        Byte("com.oracle.truffle.r.runtime.ffi", "FFINativeByteArrayUnwrapNode"),
        Int("com.oracle.truffle.r.runtime.ffi", "FFINativeIntArrayUnwrapNode"),
        Double("com.oracle.truffle.r.runtime.ffi", "FFINativeDoubleArrayUnwrapNode");

        public final String wrapperClassPackage;
        public final String wrapperSimpleClassName;

        Type(String wrapperClassPackage, String wrapperSimpleClassName) {
            this.wrapperClassPackage = wrapperClassPackage;
            this.wrapperSimpleClassName = wrapperSimpleClassName;
        }
    }

    /**
     * @return the element type of the array. It must be specified for the parameters only, not for
     *         the return type.
     */
    Type element() default Type.Any;

    /**
     * @return An expression yielding the length of the array. The expression can refer by name or
     *         index to the parameters preceding the annotated parameter. The parameter name in the
     *         expression must be enclosed in curly braces, such as <code>4 * {x} * {y}</code>.
     */
    String length() default "";

}
