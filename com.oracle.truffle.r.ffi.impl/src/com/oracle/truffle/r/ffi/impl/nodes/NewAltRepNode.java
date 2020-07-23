/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.altrep.AltComplexClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltLogicalClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRawClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRealClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;

@GenerateUncached
public abstract class NewAltRepNode extends FFIUpCallNode.Arg3 {
    public static NewAltRepNode create() {
        return NewAltRepNodeGen.create();
    }

    @Specialization
    protected Object newIntAltRep(AltIntegerClassDescriptor classDescriptor, Object data1, Object data2) {
        RAltRepData altRepData = new RAltRepData(data1, data2);
        RLogger.getLogger(RLogger.LOGGER_ALTREP).fine(
                        () -> "R_new_altrep: Returning vector with descriptor=" + classDescriptor.toString() + " to native.");
        return RDataFactory.createAltIntVector(classDescriptor, altRepData);
    }

    @Specialization
    protected Object newRealAltRep(AltRealClassDescriptor classDescriptor, Object data1, Object data2) {
        RAltRepData altRepData = new RAltRepData(data1, data2);
        RLogger.getLogger(RLogger.LOGGER_ALTREP).fine(
                        () -> "R_new_altrep: Returning vector with descriptor=" + classDescriptor.toString() + " to native.");
        return RDataFactory.createAltRealVector(classDescriptor, altRepData);
    }

    @Specialization
    protected Object newStringAltRep(AltStringClassDescriptor classDescriptor, Object data1, Object data2) {
        return RDataFactory.createAltStringVector(classDescriptor, new RAltRepData(data1, data2));
    }

    @Specialization
    protected Object newAltLogical(AltLogicalClassDescriptor descriptor, Object data1, Object data2) {
        return RDataFactory.createAltLogicalVector(descriptor, new RAltRepData(data1, data2));
    }

    @Specialization
    protected Object newAltRaw(AltRawClassDescriptor descriptor, Object data1, Object data2) {
        return RDataFactory.createAltRawVector(descriptor, new RAltRepData(data1, data2));
    }

    @Specialization
    protected Object newAltComplex(AltComplexClassDescriptor descriptor, Object data1, Object data2) {
        return RDataFactory.createAltComplexVector(descriptor, new RAltRepData(data1, data2));
    }

    @Fallback
    protected Object unknownAltrepType(@SuppressWarnings("unused") Object classDescriptor, @SuppressWarnings("unused") Object data1,
                    @SuppressWarnings("unused") Object data2) {
        throw RInternalError.shouldNotReachHere("Unknown class descriptor");
    }
}
