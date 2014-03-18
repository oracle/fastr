/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import java.io.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(".Internal.file.remove")
public abstract class FileRemove extends RBuiltinNode {

    @Specialization
    public Object doFileRemove(RAbstractStringVector vec) {
        byte[] status = new byte[vec.getLength()];
        for (int i = 0; i < status.length; i++) {
            String path = vec.getDataAt(i);
            if (path == RRuntime.STRING_NA) {
                status[i] = RRuntime.LOGICAL_FALSE;
            } else {
                File f = new File(Utils.tildeExpand(path));
                boolean ok = f.delete();
                status[i] = ok ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
                if (!ok) {
                    RContext.getInstance().setEvalWarning("  cannot remove file '" + path + "'");
                }
            }
        }
        return RDataFactory.createLogicalVector(status, RDataFactory.COMPLETE_VECTOR);
    }

    @Generic
    public Object doFileRemoveGeneric(@SuppressWarnings("unused") Object x) {
        throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid filename");
    }
}
