/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.casts;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RInteropScalar;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;

/**
 * Cast pipelines internal. You probably want to use {@link ConvertForeignObjectNode}.
 */
public final class CastForeignNode extends CastNode {

    @Child private ConvertForeignObjectNode convertForeign;
    @Child private InteropLibrary interop;

    @CompilationFinal private ConditionProfile isForeign;
    @CompilationFinal private ConditionProfile isInteropScalar;

    @Override
    protected Object execute(Object obj) {
        if (!RRuntime.isForeignObject(obj)) {
            return obj;
        }
        if (convertForeign == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            convertForeign = insert(ConvertForeignObjectNode.create());
        }
        if (isForeign == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isForeign = ConditionProfile.createBinaryProfile();
        }
        if (isInteropScalar == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isInteropScalar = ConditionProfile.createBinaryProfile();
        }
        if (interop == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            interop = insert(InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize()));
        }
        if (isForeign.profile(ConvertForeignObjectNode.isForeignArray(obj, interop))) {
            return convertForeign.convert((TruffleObject) obj);
        } else if (isInteropScalar.profile(isInteropScalar(obj))) {
            return ((RInteropScalar) obj).getRValue();
        } else {
            return obj;
        }
    }

    protected static boolean isInteropScalar(Object obj) {
        return obj instanceof RInteropScalar;
    }
}
