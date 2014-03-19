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
import java.nio.file.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(".Internal.normalizePath")
public abstract class NormalizePath extends RBuiltinNode {

    @Specialization
    public RStringVector doNormalizePath(RAbstractStringVector pathVec, @SuppressWarnings("unused") String winslash, byte mustWork) {
        String[] results = new String[pathVec.getLength()];
        FileSystem fileSystem = FileSystems.getDefault();
        for (int i = 0; i < results.length; i++) {
            String path = pathVec.getDataAt(i);
            String normPath = Utils.tildeExpand(path);
            try {
                normPath = fileSystem.getPath(path).toRealPath().toString();
            } catch (IOException e) {
                if (mustWork != RRuntime.LOGICAL_FALSE) {
                    String msg = e instanceof NoSuchFileException ? "No such file or directory: " + path : e.toString();
                    if (mustWork == RRuntime.LOGICAL_TRUE) {
                        throw RError.getGenericError(getEncapsulatingSourceSection(), msg);
                    } else {
                        RContext.getInstance().setEvalWarning(msg);
                    }
                }
            }
            results[i] = normPath;
        }
        return RDataFactory.createStringVector(results, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Generic
    public Object doNormalizePath(Object path, Object winslash, Object mustWork) {
        throw RError.getWrongTypeOfArgument(getEncapsulatingSourceSection());
    }
}
