/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "file.show", kind = RBuiltinKind.INTERNAL, parameterNames = {"files", "header", "title", "delete.file", "pager"}, behavior = RBehavior.IO)
public abstract class FileShow extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("files").asStringVector();
        casts.arg("header").asStringVector();
        casts.arg("title").asStringVector();
        casts.arg("delete.file").asLogicalVector().findFirst().map(toBoolean());
        casts.arg("pager").asStringVector().findFirst();
    }

    @Specialization
    @TruffleBoundary
    protected static RNull show(RAbstractStringVector files, RAbstractStringVector header, RAbstractStringVector title, boolean deleteFile, @SuppressWarnings("unused") String pager) {
        ConsoleHandler console = RContext.getInstance().getConsoleHandler();
        for (int i = 0; i < title.getLength(); i++) {
            console.println("==== " + title.getDataAt(i) + " ====");
        }
        for (int i = 0; i < files.getLength(); i++) {
            if (i < header.getLength() && !header.getDataAt(i).isEmpty()) {
                console.println("== " + header.getDataAt(i) + " ==");
            }
            try {
                Path path = Paths.get(files.getDataAt(i));
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    console.println(line);
                }
                if (deleteFile) {
                    path.toFile().delete();
                }
            } catch (IOException e) {
                throw RError.error(RError.SHOW_CALLER, Message.GENERIC, e.getMessage());
            }
        }
        return RNull.instance;
    }
}
