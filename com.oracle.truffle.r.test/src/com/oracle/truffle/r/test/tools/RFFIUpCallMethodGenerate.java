/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

import com.oracle.truffle.r.nodes.ffi.RFFIUpCallMethod;
import com.oracle.truffle.r.runtime.ffi.RFFICstring;
import com.oracle.truffle.r.runtime.ffi.UpCallsRFFI;

/**
 * Generates the entries for {@link RFFIUpCallMethod}.
 */
public class RFFIUpCallMethodGenerate {

    public static void main(String[] args) {
        Method[] methods = UpCallsRFFI.class.getMethods();

        Arrays.sort(methods, new Comparator<Method>() {

            @Override
            public int compare(Method a, Method b) {
                return a.getName().compareTo(b.getName());
            }

        });
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            String sig = getNFISignature(m);
            System.out.printf("%s(\"%s\")%s%n", m.getName(), sig, i == methods.length - 1 ? ";" : ",");
        }

    }

    private static String getNFISignature(Method m) {
        Class<?>[] paramTypes = m.getParameterTypes();
        Annotation[][] annotations = m.getParameterAnnotations();
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            String nfiParam = nfiParamName(paramType, annotations[i]);
            sb.append(nfiParam);
            if (i != paramTypes.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(')');
        sb.append(" : ");
        sb.append(nfiParamName(m.getReturnType(), new Annotation[0]));
        return sb.toString();
    }

    static String nfiParamName(Class<?> paramType, Annotation[] annotation) {
        String paramName = paramType.getSimpleName();
        Class<?> klass = annotation.length == 0 ? null : annotation[0].annotationType();
        switch (paramName) {
            case "Object":
                return klass == null ? "object" : "pointer";
            case "int":
                return "sint32";
            case "double":
                return "double";
            case "void":
                return "void";
            default:
                return "object";
        }

    }

}
