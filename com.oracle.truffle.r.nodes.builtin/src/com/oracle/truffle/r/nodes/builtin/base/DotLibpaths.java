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
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * {@code .libPaths} built in. Ultimately, this should revert to a snippet:
 *
 * <pre>
 * function (new)
 * {
 *     if (!missing(new)) {
 *         new <- Sys.glob(path.expand(new))
 *         paths <- unique(normalizePath(c(new, .Library.site, .Library), "/"))
 *        .lib.loc <<- paths[file.info(paths)$isdir %in% TRUE]
 *      }
 *      else .lib.loc
 * }
 * </pre>
 *
 */
@RBuiltin(name = ".libPaths", kind = SUBSTITUTE, parameterNames = {"new"})
public abstract class DotLibpaths extends RBuiltinNode {
    @Specialization
    protected Object libPathsVec(@SuppressWarnings("unused") RMissing missing) {
        controlVisibility();
        return RDataFactory.createStringVector(LibPaths.dotLibPaths(), RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected Object libPathsVec(RAbstractStringVector pathVec) {
        controlVisibility();
        ArrayList<String> resultsList = new ArrayList<>(pathVec.getLength());
        FileSystem fileSystem = FileSystems.getDefault();
        for (int i = 0; i < resultsList.size(); i++) {
            String path = Utils.tildeExpand(pathVec.getDataAt(i));
            try {
                resultsList.add(fileSystem.getPath(path).toRealPath().toString());
            } catch (IOException e) {
                // directory does not exist (or is inaccessible - same thing),
                // just ignore it
            }
        }
        String[] resultsArray = new String[resultsList.size()];
        resultsList.toArray(resultsArray);
        String[] uniqueArray = unique(resultsArray, LibPaths.dotLibrarySitePlusLibrary());
        return RDataFactory.createStringVector(uniqueArray, RDataFactory.COMPLETE_VECTOR);
    }

    private static String[] unique(String[]... args) {
        ArrayList<String> resultList = new ArrayList<>();
        for (String[] a : args) {
            for (String path : a) {
                if (!resultList.contains(path)) {
                    resultList.add(path);
                }
            }
        }
        String[] array = new String[resultList.size()];
        resultList.toArray(array);
        return array;
    }

    @Specialization
    protected Object libPathsGeneric(VirtualFrame frame, @SuppressWarnings("unused") Object x) {
        controlVisibility();
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "path");
    }

}
