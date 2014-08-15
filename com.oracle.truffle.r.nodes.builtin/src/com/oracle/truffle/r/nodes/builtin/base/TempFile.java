/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;
import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "tempfile", kind = INTERNAL, parameterNames = {"pattern", "tempdir", "fileext"})
public abstract class TempFile extends RBuiltinNode {

    private static Random rand = new Random();

    private static final String RANDOM_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int RANDOM_CHARACTERS_LENGTH = RANDOM_CHARACTERS.length();
    private static final int RANDOM_LENGTH = 12; // as per GnuR
    private static final String INVALID_PATTERN = mkErrorMsg("filename");
    private static final String INVALID_TEMPDIR = mkErrorMsg("tempdir");
    private static final String INVALID_FILEEXT = mkErrorMsg("file extension");

    private static String mkErrorMsg(String msg) {
        return "invalid '" + msg + "'";
    }

    @Specialization(order = 0, guards = "tempDirL1")
    public RStringVector tempfile(String pattern, RAbstractStringVector tempDir, String fileExt) {
        controlVisibility();
        return RDataFactory.createStringVector(createFile(pattern, tempDir.getDataAt(0), fileExt));
    }

    @SuppressWarnings("unused")
    public static boolean tempDirL1(String pattern, RAbstractStringVector tempDir, String fileExt) {
        return tempDir.getLength() == 1;
    }

    @Specialization(order = 100)
    public RStringVector tempfileGeneric(VirtualFrame frame, Object pattern, Object tempDir, Object fileExt) throws RError {
        controlVisibility();
        RStringVector[] argVecs = new RStringVector[]{checkVector(frame, pattern, INVALID_PATTERN), checkVector(frame, tempDir, INVALID_TEMPDIR), checkVector(frame, fileExt, INVALID_FILEEXT)};
        // Now we have RStringVectors of at least length 1
        int maxL = 0;
        for (int i = 0; i < argVecs.length; i++) {
            int l = argVecs[i].getLength();
            if (i > maxL) {
                maxL = l;
            }
        }
        for (int i = 0; i < argVecs.length; i++) {
            int l = argVecs[i].getLength();
            if (l < maxL) {
                argVecs[i] = extendVector(argVecs[i], l, maxL);
            }
        }
        // Now all vectors are same length
        String[] data = new String[maxL];
        for (int i = 0; i < data.length; i++) {
            data[i] = createFile(argVecs[0].getDataAt(i), argVecs[1].getDataAt(i), argVecs[2].getDataAt(i));
        }
        return RDataFactory.createStringVector(data, true);
    }

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

    private RStringVector checkVector(VirtualFrame frame, Object obj, String msg) throws RError {
        if (obj instanceof RStringVector) {
            RStringVector result = (RStringVector) obj;
            if (result.getLength() > 0) {
                return result;
            }
        } else if (obj instanceof String) {
            return RDataFactory.createStringVector((String) obj);
        }
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.GENERIC, msg);
    }

    private static String createFile(String pattern, String tempDir, String fileExt) {
        while (true) {
            StringBuilder sb = new StringBuilder(tempDir);
            sb.append(File.separatorChar);
            sb.append(pattern);
            appendRandomString(sb);
            if (fileExt.length() > 0) {
                sb.append(fileExt);
            }
            String path = sb.toString();
            if (!new File(path).exists()) {
                return path;
            }
        }
    }

    private static void appendRandomString(StringBuilder sb) {
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(RANDOM_CHARACTERS.charAt(rand.nextInt(RANDOM_CHARACTERS_LENGTH)));
        }
    }

}
