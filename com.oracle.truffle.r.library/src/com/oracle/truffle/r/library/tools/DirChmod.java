/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.library.tools;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Iterator;
import java.util.stream.Stream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.FileSystemUtils;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RNull;

public abstract class DirChmod extends RExternalBuiltinNode.Arg2 {

    private static final int GRPWRITE_FILE_MASK = 0664;
    private static final int GRPWRITE_DIR_MASK = 0775;
    private static final int FILE_MASK = 0644;
    private static final int DIR_MASK = 0755;

    static {
        Casts casts = new Casts(DirChmod.class);
        casts.arg(0, "dir").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        casts.arg(1).asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    @Specialization
    @TruffleBoundary
    protected RNull dirChmod(String pathName, boolean setGroupWrite) {
        Path path = FileSystems.getDefault().getPath(pathName);
        int fileMask = setGroupWrite ? GRPWRITE_FILE_MASK : FILE_MASK;
        int dirMask = setGroupWrite ? GRPWRITE_DIR_MASK : DIR_MASK;
        if (!path.toFile().exists()) {
            return RNull.instance;
        }
        assert path.isAbsolute() : path;
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
                FileSystemUtils.chmod(element.toString(), newMode);
            }
        } catch (IOException ex) {
            // ignore
        }
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object fallback(Object dir, Object gws) {
        throw error(RError.Message.INVALID_ARGUMENT, "dir");
    }
}
