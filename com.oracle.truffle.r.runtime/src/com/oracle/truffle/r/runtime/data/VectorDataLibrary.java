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

import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.GenerateLibrary.DefaultExport;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import static com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.notWriteableError;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@GenerateLibrary
@DefaultExport(DefaultRVectorDataLibrary.class)
public abstract class VectorDataLibrary extends Library {

    static final LibraryFactory<VectorDataLibrary> FACTORY = LibraryFactory.resolve(VectorDataLibrary.class);

    public static LibraryFactory<VectorDataLibrary> getFactory() {
        return FACTORY;
    }

    @SuppressWarnings("unused")
    public boolean isComplete(Object data) {
        return false;
    }

    @SuppressWarnings("unused")
    public boolean isMaterialized(Object data) {
        return false;
    }

    @SuppressWarnings("unused")
    public int getLength(Object data) {
        return 0;
    }

    @SuppressWarnings("unused")
    public void setDataAtAsObject(Object data, int idx, Object value, NACheck naCheck) {
        throw notWriteableError(RDoubleVectorData.class, "setDataAtAsObject");
    }
}
