/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;

@ExportLibrary(VectorDataLibrary.class)
public class RAltStringVectorData extends RAltrepVectorData {
    private final AltStringClassDescriptor descriptor;

    public RAltStringVectorData(AltStringClassDescriptor descriptor, RAltRepData altRepData) {
        super(altRepData);
        assert hasDescriptorRegisteredNecessaryMethods(descriptor);
        this.descriptor = descriptor;
    }

    private static boolean hasDescriptorRegisteredNecessaryMethods(AltStringClassDescriptor descriptor) {
        return descriptor.isLengthMethodRegistered() && descriptor.isDataptrMethodRegistered() && descriptor.isEltMethodRegistered() && descriptor.isSetEltMethodRegistered();
    }

    public AltStringClassDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    @ExportMessage
    public RType getType() {
        return RType.Character;
    }

    @ExportMessage
    public String[] getStringDataCopy(@Shared("eltNode") @Cached AltrepRFFI.EltNode eltNode,
                    @Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        final int length = getLength(lengthNode);
        String[] stringData = new String[length];
        for (int i = 0; i < length; i++) {
            stringData[i] = (String) eltNode.execute(owner, i);
        }
        return stringData;
    }

    @ExportMessage
    public String getString(@SuppressWarnings("unused") RandomAccessIterator it, int index,
                    @Shared("eltNode") @Cached AltrepRFFI.EltNode eltNode) {
        return (String) eltNode.execute(owner, index);
    }

    @ExportMessage
    public String getStringAt(int index,
                    @Shared("eltNode") @Cached AltrepRFFI.EltNode eltNode) {
        return (String) eltNode.execute(owner, index);
    }

    @ExportMessage
    public String getNextString(SeqIterator it,
                    @Shared("eltNode") @Cached AltrepRFFI.EltNode eltNode) {
        return (String) eltNode.execute(owner, it.getIndex());
    }

    @ExportMessage
    public void setStringAt(int index, String value,
                    @Shared("setEltNode") @Cached AltrepRFFI.SetEltNode setEltNode) {
        setEltNode.execute((RStringVector) owner, index, value);
    }

    @ExportMessage
    public void setNextString(SeqWriteIterator it, String value,
                    @Shared("setEltNode") @Cached AltrepRFFI.SetEltNode setEltNode) {
        setEltNode.execute((RStringVector) owner, it.getIndex(), value);
    }

    @ExportMessage
    public void setString(@SuppressWarnings("unused") RandomAccessWriteIterator it, int index, String value,
                    @Shared("setEltNode") @Cached AltrepRFFI.SetEltNode setEltNode) {
        setEltNode.execute((RStringVector) owner, index, value);
    }
}
