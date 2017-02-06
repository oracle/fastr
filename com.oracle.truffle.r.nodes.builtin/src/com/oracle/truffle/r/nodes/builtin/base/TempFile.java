/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.TempPathName;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "tempfile", kind = INTERNAL, parameterNames = {"pattern", "tempdir", "fileext"}, behavior = COMPLEX)
public abstract class TempFile extends RBuiltinNode {

    static {
        Casts casts = new Casts(TempFile.class);
        casts.arg("pattern").asVector().mustBe(stringValue(), RError.SHOW_CALLER, RError.Message.INVALID_FILENAME_PATTERN).mustBe(notEmpty(), RError.SHOW_CALLER, RError.Message.NO, "pattern");
        casts.arg("tempdir").asVector().mustBe(stringValue(), RError.SHOW_CALLER, RError.Message.INVALID_VALUE, "tempdir").findFirst(RError.SHOW_CALLER, RError.Message.NO, "tempdir");
        casts.arg("fileext").asVector().mustBe(stringValue(), RError.SHOW_CALLER, RError.Message.INVALID_FILE_EXT).mustBe(notEmpty(), RError.SHOW_CALLER, RError.Message.NO, "fileext");
    }

    @TruffleBoundary
    @Specialization
    protected RStringVector tempfile(RAbstractStringVector pattern, String tempDir, RAbstractStringVector fileExt) {
        int patternLength = pattern.getLength();
        int fileExtLength = fileExt.getLength();
        int maxLength = Math.max(patternLength, fileExtLength);
        String[] data = new String[maxLength];
        int patternInd = 0;
        int fileExtInd = 0;
        for (int i = 0; i < maxLength; i++) {
            data[i] = TempPathName.createNonExistingFilePath(pattern.getDataAt(patternInd), tempDir, fileExt.getDataAt(fileExtInd));
            patternInd = Utils.incMod(patternInd, patternLength);
            fileExtInd = Utils.incMod(fileExtInd, fileExtLength);
        }
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }
}
