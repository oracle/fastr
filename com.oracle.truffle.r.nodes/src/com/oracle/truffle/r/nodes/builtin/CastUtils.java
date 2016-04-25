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
package com.oracle.truffle.r.nodes.builtin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class CastUtils {

    public static Class<?> elementType(Class<?> vectorType) {
        if (RAbstractIntVector.class.isAssignableFrom(vectorType)) {
            return Integer.class;
        }
        if (RAbstractDoubleVector.class.isAssignableFrom(vectorType)) {
            return Double.class;
        }
        if (RAbstractLogicalVector.class.isAssignableFrom(vectorType)) {
            return Byte.class;
        }
        if (RAbstractStringVector.class.isAssignableFrom(vectorType)) {
            return String.class;
        }
        if (RAbstractComplexVector.class.isAssignableFrom(vectorType)) {
            return RComplex.class;
        }
        if (RAbstractVector.class.isAssignableFrom(vectorType)) {
            return Object.class;
        }
        throw new IllegalArgumentException("Unsupported vector type " + vectorType);
    }

    public static Object emptyVector(Class<?> elementType) {
        if (Integer.class.isAssignableFrom(elementType)) {
            return RDataFactory.createEmptyIntVector();
        }
        if (Double.class.isAssignableFrom(elementType)) {
            return RDataFactory.createEmptyDoubleVector();
        }
        if (Byte.class.isAssignableFrom(elementType)) {
            return RDataFactory.createEmptyLogicalVector();
        }
        if (Boolean.class.isAssignableFrom(elementType)) {
            return RDataFactory.createEmptyLogicalVector();
        }
        if (String.class.isAssignableFrom(elementType)) {
            return RDataFactory.createEmptyStringVector();
        }
        if (RComplex.class.isAssignableFrom(elementType)) {
            return RDataFactory.createEmptyComplexVector();
        }
        return null;
    }

    public static Object naVector(Class<?> elementType) {
        if (Integer.class.isAssignableFrom(elementType)) {
            return RDataFactory.createIntVectorFromScalar(RRuntime.INT_NA);
        }
        if (Double.class.isAssignableFrom(elementType)) {
            return RDataFactory.createDoubleVectorFromScalar(RRuntime.DOUBLE_NA);
        }
        if (Byte.class.isAssignableFrom(elementType)) {
            return RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_NA);
        }
        if (Boolean.class.isAssignableFrom(elementType)) {
            return RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_NA);
        }
        if (String.class.isAssignableFrom(elementType)) {
            return RDataFactory.createStringVectorFromScalar(RRuntime.STRING_NA);
        }
        if (RComplex.class.isAssignableFrom(elementType)) {
            return RDataFactory.createComplexVectorFromScalar(RComplex.createNA());
        }
        return null;
    }

    public static Object naValue(Class<?> elementType) {
        if (Integer.class.isAssignableFrom(elementType)) {
            return RRuntime.INT_NA;
        }
        if (Double.class.isAssignableFrom(elementType)) {
            return RRuntime.DOUBLE_NA;
        }
        if (Byte.class.isAssignableFrom(elementType)) {
            return RRuntime.LOGICAL_NA;
        }
        if (Boolean.class.isAssignableFrom(elementType)) {
            return null;
        }
        if (String.class.isAssignableFrom(elementType)) {
            return RRuntime.STRING_NA;
        }
        if (RComplex.class.isAssignableFrom(elementType)) {
            return RComplex.createNA();
        }
        return null;
    }

    public static Object singletonVector(Object element) {
        if (element == RNull.instance) {
            return RNull.instance;
        }
        if (element instanceof Integer) {
            return RDataFactory.createIntVectorFromScalar((Integer) element);
        }
        if (element instanceof Double) {
            return RDataFactory.createDoubleVectorFromScalar((Double) element);
        }
        if (element instanceof Byte) {
            return RDataFactory.createLogicalVectorFromScalar((Byte) element);
        }
        if (element instanceof Boolean) {
            return RDataFactory.createLogicalVectorFromScalar((Boolean) element);
        }
        if (element instanceof String) {
            return RDataFactory.createStringVectorFromScalar((String) element);
        }
        if (element instanceof RComplex) {
            return RDataFactory.createComplexVectorFromScalar((RComplex) element);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> sampleValuesForClass(Class<T> cls) {
        HashSet<Object> samples = new HashSet<>();

        samples.add(naValue(cls));

        if (cls == Object.class || Byte.class.isAssignableFrom(cls)) {
            samples.add(RRuntime.LOGICAL_TRUE);
            samples.add(RRuntime.LOGICAL_FALSE);
        }

        if (cls == Object.class || RAbstractLogicalVector.class.isAssignableFrom(cls)) {
            samples.add(RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_TRUE));
            samples.add(RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_FALSE));
            samples.add(RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_TRUE}, true));
            samples.add(RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_NA}, false));
        }

        if (cls == Object.class || Boolean.class.isAssignableFrom(cls)) {
            samples.add(Boolean.TRUE);
            samples.add(Boolean.FALSE);
        }

        if (cls == Object.class || Integer.class.isAssignableFrom(cls)) {
            samples.add(0);
            samples.add(1);
            samples.add(-1);
        }

        if (cls == Object.class || RAbstractIntVector.class.isAssignableFrom(cls)) {
            samples.add(RDataFactory.createIntVectorFromScalar(0));
            samples.add(RDataFactory.createIntVectorFromScalar(1));
            samples.add(RDataFactory.createIntVectorFromScalar(-1));
            samples.add(RDataFactory.createIntVector(new int[]{-1, 0, 1}, true));
            samples.add(RDataFactory.createIntVector(new int[]{-1, 0, 1, RRuntime.INT_NA}, false));
        }

        if (cls == Object.class || Double.class.isAssignableFrom(cls)) {
            samples.add(0d);
            samples.add(Math.PI);
            samples.add(-Math.PI);
        }

        if (cls == Object.class || RAbstractDoubleVector.class.isAssignableFrom(cls)) {
            samples.add(RDataFactory.createDoubleVectorFromScalar(0));
            samples.add(RDataFactory.createDoubleVectorFromScalar(1));
            samples.add(RDataFactory.createDoubleVectorFromScalar(-1));
            samples.add(RDataFactory.createDoubleVector(new double[]{-1, 0, 1}, true));
            samples.add(RDataFactory.createDoubleVector(new double[]{-Math.PI, 0, Math.PI, RRuntime.DOUBLE_NA}, false));
        }

        if (cls == Object.class || RComplex.class.isAssignableFrom(cls)) {
            samples.add(RComplex.valueOf(0, 0));
            samples.add(RComplex.valueOf(Math.PI, Math.PI));
            samples.add(RComplex.valueOf(-Math.PI, -Math.PI));
        }

        if (cls == Object.class || RAbstractComplexVector.class.isAssignableFrom(cls)) {
            samples.add(RDataFactory.createComplexVectorFromScalar(RComplex.valueOf(0, 0)));
            samples.add(RDataFactory.createComplexVectorFromScalar(RComplex.valueOf(1, 1)));
            samples.add(RDataFactory.createComplexVectorFromScalar(RComplex.valueOf(-1, 1)));
            samples.add(RDataFactory.createComplexVector(new double[]{-Math.PI, 0, Math.PI, RRuntime.DOUBLE_NA, -Math.PI, 0, Math.PI, RRuntime.DOUBLE_NA}, true));
            samples.add(RDataFactory.createComplexVector(new double[]{-Math.PI, 0, Math.PI, RRuntime.DOUBLE_NA, -Math.PI, 0, Math.PI, RRuntime.DOUBLE_NA}, false));
        }

        if (cls == Object.class || String.class.isAssignableFrom(cls)) {
            samples.add("");
        }

        if (cls == Object.class || RAbstractStringVector.class.isAssignableFrom(cls)) {
            samples.add(RDataFactory.createStringVectorFromScalar(""));
            samples.add(RDataFactory.createStringVectorFromScalar("abc"));
            samples.add(RDataFactory.createStringVector(new String[]{"", "abc"}, true));
            samples.add(RDataFactory.createStringVector(new String[]{"", "abc", RRuntime.STRING_NA}, false));
        }

        samples.remove(null);

        return (Set<T>) samples;
    }

    public static Set<Object> sampleValuesForClassesOtherThan(Class<?> cls) {
        return CastUtils.sampledClasses.stream().filter(c -> c != cls).flatMap(c -> sampleValuesForClass(c).stream()).collect(Collectors.toSet());
    }

    public static final Set<Class<?>> sampledClasses = Collections.unmodifiableSet(CastBuilder.samples(Integer.class, Double.class, Byte.class, RComplex.class, String.class));

    public static Set<Object> pseudoValuesForClass(Class<?> cls) {
        HashSet<Object> samples = new HashSet<>();
        samples.add(RNull.instance);
        samples.add(emptyVector(cls));
        samples.add(naValue(cls));
        samples.add(naVector(cls));

        samples.remove(null);

        return samples;
    }

}
