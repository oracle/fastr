/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

/**
 * Extracts the salient information needed for a native call from the {@link RList} value provided
 * from R.
 */
public abstract class ExtractNativeCallInfoNode extends Node {
    @Child private ExtractVectorNode nameExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    @Child private ExtractVectorNode addressExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    @Child private ExtractVectorNode packageExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    @Child private ExtractVectorNode infoExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

    protected abstract Object execute(VirtualFrame frame, RList symbol);

    @Specialization
    protected Object extractNativeCallInfo(VirtualFrame frame, RList symbol) {
        if (nameExtract == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            nameExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        }
        if (addressExtract == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            addressExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        }
        if (packageExtract == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            packageExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        }
        String name = RRuntime.asString(nameExtract.applyAccessField(frame, symbol, "name"));
        SymbolHandle address = ((RExternalPtr) addressExtract.applyAccessField(frame, symbol, "address")).getAddr();
        // field name may be "package" or "dll", but always at (R) index 3
        RList packageList = (RList) packageExtract.apply(frame, symbol, new Object[]{3}, RLogical.valueOf(false), RMissing.instance);
        DLLInfo dllInfo = (DLLInfo) ((RExternalPtr) addressExtract.applyAccessField(frame, packageList, "info")).getExternalObject();
        return new NativeCallInfo(name, address, dllInfo);

    }

}
