/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.tools;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.stream.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ffi.*;

public abstract class DirChmod extends RExternalBuiltinNode.Arg2 {

    private static final int GRPWRITE_FILE_MASK = 0664;
    private static final int GRPWRITE_DIR_MASK = 0775;
    private static final int FILE_MASK = 0644;
    private static final int DIR_MASK = 0755;

    @TruffleBoundary
    @Specialization
    protected RNull dirChmod(RAbstractStringVector dir, Object gws) {
        if (dir.getLength() != 1) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "dir");
        }
        String pathName = ((RStringVector) dir).getDataAt(0);
        boolean setGroupWrite = RRuntime.fromLogical(castLogical(castVector(gws)));

        Path path = FileSystems.getDefault().getPath(pathName);
        int fileMask = setGroupWrite ? GRPWRITE_FILE_MASK : FILE_MASK;
        int dirMask = setGroupWrite ? GRPWRITE_DIR_MASK : DIR_MASK;
        assert path.isAbsolute();
        try (Stream<Path> stream = Files.walk(path, Integer.MAX_VALUE)) {
            Iterator<Path> iter = stream.iterator();
            while (iter.hasNext()) {
                Path element = iter.next();
                if (Files.isSameFile(path, element)) {
                    continue;
                }
                PosixFileAttributes pfa = Files.readAttributes(element, PosixFileAttributes.class);
                int elementMode = Utils.intFilePermissions(pfa.permissions());
                int newMode = Files.isDirectory(element) ? elementMode | dirMask : elementMode | fileMask;
                // System.out.printf("path %s: old %o, new %o%n", element, elementMode, newMode);
                RFFIFactory.getRFFI().getBaseRFFI().chmod(element.toString(), newMode);
            }
        } catch (IOException ex) {
            // ignore
        }
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object fallback(Object dir, Object gws) {
        throw RError.error(this, RError.Message.INVALID_ARGUMENT, "dir");
    }
}
