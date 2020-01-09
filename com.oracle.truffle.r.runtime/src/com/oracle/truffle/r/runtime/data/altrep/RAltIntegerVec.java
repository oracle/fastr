/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

@ExportLibrary(InteropLibrary.class)
public class RAltIntegerVec extends RAbstractIntVector implements RMaterializedVector {
    private RAltRepData data;
    private final AltIntegerClassDescriptor descriptor;
    private final FastPathAccess vectorAccess;
    private static final TruffleLogger logger = RLogger.getLogger("altrep");
    // TODO: quickfix
    private final int cachedLength;

    public RAltIntegerVec(AltIntegerClassDescriptor descriptor, RAltRepData data, boolean complete) {
        // TODO: Should complete = true?
        super(complete);
        setAltRep();
        this.data = data;
        this.descriptor = descriptor;
        assert hasDescriptorRegisteredNecessaryMethods(descriptor):
                "Descriptor " + descriptor.toString() + " does not have registered all necessary methods";
        this.cachedLength = descriptor.invokeLengthMethod(this, null);
        this.vectorAccess = new FastPathAccess(this);
    }

    private boolean hasDescriptorRegisteredNecessaryMethods(AltIntegerClassDescriptor descriptor) {
        return descriptor.isLengthMethodRegistered() && descriptor.isDataptrMethodRegistered();
                /* TODO: && descriptor.isUnserializeMethodRegistered(); */
    }

    public AltIntegerClassDescriptor getDescriptor() {
        return descriptor;
    }

    public RAltRepData getData() {
        return data;
    }

    public Object getData1() {
        return data.getData1();
    }

    public Object getData2() {
        return data.getData2();
    }

    public void setData1(Object data1) {
        data.setData1(data1);
    }

    public void setData2(Object data2) {
        data.setData2(data2);
    }

    /**
     * This message overrides the default message in @link RAbstractNumericVector - becuase the aforementioned
     * message calls getLength which in turn invokes length native function which again calls getLength.
     * This problem occurs only with LLVM backend.
     */
    @ExportMessage
    final boolean isNull() {
        return false;
    }

    @Override
    public String toString() {
        return "AltIntegerVector: data={" + data.toString() + "}";
    }

    // Should not be called frequently
    @Override
    public int getDataAt(int index) {
        if (descriptor.isEltMethodRegistered()) {
            return descriptor.invokeEltMethod(this, null, index);
        } else {
            // Invoke uncached dataptr method
            long address = descriptor.invokeDataptrMethod(this, true, null, null);
            return NativeDataAccess.getData(this, index, address);
        }
    }

    @Override
    public void setDataAt(Object store, int index, int value) {
        NativeDataAccess.setData(this, index, value);
    }

    @Override
    public int getLength() {
        return cachedLength;
    }

    @Override
    public RIntVector materialize() {
        // TODO: Implement iteration with FastPath access
        int[] newData = new int[getLength()];
        for (int i = 0; i < getLength(); i++) {
            newData[i] = getDataAt(i);
        }
        return RDataFactory.createIntVector(newData, true);
    }

    @Override
    public Object getInternalStore() {
        // Vratit deskriptor a neco jako this
        return this;
    }

    @Override
    public VectorAccess access() {
        return vectorAccess;
    }

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }

    // Should not be called frequently.
    public long invokeDataptr() {
        // TODO: writeabble = true?
        return descriptor.invokeDataptrMethod(this, true, null, null);
    }

    private final SlowPathFromIntAccess SLOW_PATH_ACCESS = new SlowPathFromIntAccess() {
        @Override
        protected int getIntImpl(AccessIterator accessIter, int index) {
            RAltIntegerVec vector = (RAltIntegerVec) accessIter.getStore();
            return vector.getDataAt(index);
        }

        @Override
        protected void setIntImpl(AccessIterator accessIter, int index, int value) {
            RAltIntegerVec vector = (RAltIntegerVec) accessIter.getStore();
            vector.setDataAt(null, index, value);
        }
    };

    private static final class FastPathAccess extends FastPathFromIntAccess {
        private final boolean hasEltMethod;
        @Child private InteropLibrary eltMethodInterop;
        //TODO: Cachovat si rovnou adresu?
        @Child private InteropLibrary dataptrMethodInterop;
        @Child private InteropLibrary dataptrInterop;

        FastPathAccess(RAltIntegerVec value) {
            super(value);
            this.hasEltMethod = value.getDescriptor().isEltMethodRegistered();
            this.eltMethodInterop = hasEltMethod ? InteropLibrary.getFactory().create(value.getDescriptor().getEltMethod()) : null;
            this.dataptrMethodInterop = hasEltMethod ? null : InteropLibrary.getFactory().create(value.getDescriptor().getDataptrMethod());
            this.dataptrInterop = hasEltMethod ? null : createDataptrInterop(value);
        }

        @CompilerDirectives.TruffleBoundary
        private static InteropLibrary createDataptrInterop(RAltIntegerVec value) {
            AltIntegerClassDescriptor descriptor = value.getDescriptor();
            // Note that dataptrMethodInterop is not adopted here yet.
            InteropLibrary slowDataptrMethodInterop = InteropLibrary.getFactory().getUncached(descriptor.getDataptrMethod());
            Object dataptr = AltRepClassDescriptor.invokeNativeFunction(slowDataptrMethodInterop, descriptor.getDataptrMethod(),
                    descriptor.getDataptrMethodSignature(), descriptor.getDataptrMethodArgCount(), value, true);
            return InteropLibrary.getFactory().create(dataptr);
        }

        // TODO: Fix this (there should be more conditions).
        @Override
        public boolean supports(Object value) {
            if (!(value instanceof RAltIntegerVec)) {
                return false;
            }

            if (hasEltMethod) {
                return super.supports(value) && ((RAltIntegerVec) value).getDescriptor().isEltMethodRegistered();
            } else {
                // Note that dataptrMethod must be registered.
                return super.supports(value);
            }
        }

        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            RAltIntegerVec instance = getInstanceFromIterator(accessIter);

            if (hasEltMethod) {
                return instance.getDescriptor().invokeEltMethod(instance, eltMethodInterop, index);
            } else {
                long address = instance.getDescriptor().invokeDataptrMethod(instance, true, dataptrMethodInterop, dataptrInterop);
                return NativeDataAccess.getData(instance, index, address);
            }
        }

        @Override
        protected void setIntImpl(AccessIterator accessIter, int index, int value) {
            RAltIntegerVec instance = getInstanceFromIterator(accessIter);
            NativeDataAccess.setData(instance, index, value);
        }

        private RAltIntegerVec getInstanceFromIterator(AccessIterator accessIterator) {
            Object store = accessIterator.getStore();
            assert store instanceof RAltIntegerVec;
            return (RAltIntegerVec) store;
        }
    }
}
