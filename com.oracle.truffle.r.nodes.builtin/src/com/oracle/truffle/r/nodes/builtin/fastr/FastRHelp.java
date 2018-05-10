/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.fastr;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.fastr.FastRHelpFactory.FastRAddHelpPathNodeGen;
import com.oracle.truffle.r.runtime.RError;
import static com.oracle.truffle.r.runtime.RVisibility.ON;

import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class FastRHelp {

    private static ArrayList<String> paths;

    private static synchronized void initPaths() {
        if (paths == null) {
            paths = new ArrayList<>(3);
        }
    }

    private static synchronized void addPath(String path) {
        initPaths();
        paths.add(path);
    }

    private static synchronized String[] getPaths() {
        initPaths();
        return paths.toArray(new String[paths.size()]);
    }

    @RBuiltin(name = ".fastr.addHelpPath", visibility = ON, kind = PRIMITIVE, parameterNames = {"path"}, behavior = COMPLEX)
    public abstract static class FastRAddHelpPath extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(FastRAddHelpPath.class);
            casts.arg("path").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization()
        @TruffleBoundary
        public Object helpPath(String path) {
            addPath(path);
            return RNull.instance;
        }

        public static FastRAddHelpPath create() {
            return FastRAddHelpPathNodeGen.create();
        }
    }

    @RBuiltin(name = ".fastr.helpPath", visibility = ON, kind = PRIMITIVE, parameterNames = {"builtinName"}, behavior = COMPLEX)
    public abstract static class FastRHelpPath extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(FastRHelpPath.class);
            casts.arg("builtinName").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization()
        @TruffleBoundary
        public Object helpPath(String builtinName) {
            for (String path : getPaths()) {
                String filename = path + '/' + builtinName + ".Rd";
                try (InputStream in = ResourceHandlerFactory.getHandler().getResourceAsStream(getClass(), filename)) {
                    if (in != null) {
                        return filename;
                    }
                } catch (IOException ex) {
                }
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.helpRd", visibility = ON, kind = PRIMITIVE, parameterNames = {"path"}, behavior = COMPLEX)
    public abstract static class FastRHelpRd extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(FastRHelpRd.class);
            casts.arg("path").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization()
        @TruffleBoundary
        public Object getHelpRdPath(String path) {
            try (InputStream in = ResourceHandlerFactory.getHandler().getResourceAsStream(getClass(), path)) {
                if (in != null) {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        return sb.toString();
                    }
                }
            } catch (IOException ex) {
                RError.warning(this, RError.Message.GENERIC, "problems while reading " + path, ex.getMessage());
            }
            return RNull.instance;
        }
    }
}
