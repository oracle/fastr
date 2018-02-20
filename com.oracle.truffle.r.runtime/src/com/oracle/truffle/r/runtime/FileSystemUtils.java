/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError.Message;

public class FileSystemUtils {
    private static PosixFilePermission[] permissionValues = PosixFilePermission.values();

    private static Set<PosixFilePermission> permissionsFromMode(int mode) {
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        for (int i = 0; i < permissionValues.length; i++) {
            if ((mode & (1 << (permissionValues.length - i - 1))) != 0) {
                permissions.add(permissionValues[i]);
            }
        }
        return permissions;
    }

    @TruffleBoundary
    public static int chmod(String path, int mode) {
        try {
            Files.setPosixFilePermissions(Paths.get(path), permissionsFromMode(mode));
            return mode;
        } catch (IOException e) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Cannot change file permissions.");
        }
    }

    @TruffleBoundary
    public static void mkdir(String dir, int mode) throws IOException {
        Set<PosixFilePermission> permissions = permissionsFromMode(mode);
        Files.createDirectory(Paths.get(dir), PosixFilePermissions.asFileAttribute(permissions));
    }
}
