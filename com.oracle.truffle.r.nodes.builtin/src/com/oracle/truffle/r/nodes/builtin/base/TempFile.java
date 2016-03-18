/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.TempPathName;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "tempfile", kind = INTERNAL, parameterNames = {"pattern", "tempdir", "fileext"})
public abstract class TempFile extends RBuiltinNode {
    @CompilationFinal private int stringVectorsAmount;

    private static final String INVALID_PATTERN = mkErrorMsg("filename");
    private static final String INVALID_TEMPDIR = mkErrorMsg("tempdir");
    private static final String INVALID_FILEEXT = mkErrorMsg("file extension");

    private static String mkErrorMsg(String msg) {
        return "invalid '" + msg + "'";
    }

    @Specialization(guards = "tempDir.getLength() == 1")
    @TruffleBoundary
    protected RStringVector tempfile(String pattern, RAbstractStringVector tempDir, String fileExt) {
        controlVisibility();
        return RDataFactory.createStringVector(TempPathName.createNonExistingFilePath(pattern, tempDir.getDataAt(0), fileExt));
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector tempfileGeneric(Object pattern, Object tempDir, Object fileExt) {
        controlVisibility();
        // Now we have RStringVectors of at least length 1
        RStringVector[] argVecs = new RStringVector[]{checkVector(pattern, INVALID_PATTERN), checkVector(tempDir, INVALID_TEMPDIR), checkVector(fileExt, INVALID_FILEEXT)};
        stringVectorsAmount = argVecs.length;
        int maxL = findMaxLengthIn(argVecs);
        extendVectorsToSameLength(argVecs, maxL);   // Now all vectors are same length
        return RDataFactory.createStringVector(createTempFilesPaths(argVecs, maxL), true);
    }

    @TruffleBoundary
    private int findMaxLengthIn(RStringVector[] stringVectors) {
        int maxLength = 0;
        for (int i = 0; i < stringVectorsAmount; i++) {
            int length = stringVectors[i].getLength();
            if (length > maxLength) {
                maxLength = length;
            }
        }
        return maxLength;
    }

    @TruffleBoundary
    private void extendVectorsToSameLength(RStringVector[] stringVectors, int desiredLength) {
        for (int i = 0; i < stringVectorsAmount; i++) {
            RStringVector stringVector = stringVectors[i];
            int length = stringVector.getLength();
            if (length < desiredLength) {
                stringVectors[i] = extendVector(stringVector, length, desiredLength);
            }
        }
    }

    @TruffleBoundary
    private static RStringVector extendVector(RStringVector vec, int vecL, int maxL) {
        String[] data = new String[maxL];
        int i = 0;
        while (i < vecL) {
            data[i] = vec.getDataAt(i);
            i++;
        }
        while (i < maxL) {
            data[i] = vec.getDataAt(i % vecL);
            i++;
        }
        return RDataFactory.createStringVector(data, true);
    }

    @TruffleBoundary
    private static String[] createTempFilesPaths(RStringVector[] stringVectors, int pathsAmount) {
        // pathsAmount must be equals to length of vector. All vectors must be same length
        String[] paths = new String[pathsAmount];
        for (int i = 0; i < pathsAmount; i++) {
            paths[i] = TempPathName.createNonExistingFilePath(stringVectors[0].getDataAt(i), stringVectors[1].getDataAt(i), stringVectors[2].getDataAt(i));
        }
        return paths;
    }

    @TruffleBoundary
    private RStringVector checkVector(Object obj, String msg) {
        if (obj instanceof RStringVector) {
            RStringVector result = (RStringVector) obj;
            if (result.getLength() > 0) {
                return result;
            }
        } else if (obj instanceof String) {
            return RDataFactory.createStringVector((String) obj);
        }
        throw RError.error(this, RError.Message.GENERIC, msg);
    }
}
