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

import java.io.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class ConnectionFunctions {

    public static class FileReadRConnection extends RConnection {
        private final BufferedReader bufferedReader;

        FileReadRConnection(String path) throws IOException {
            bufferedReader = new BufferedReader(new FileReader(path));
        }

        @SlowPath
        @Override
        public String[] readLines(int n) throws IOException {
            ArrayList<String> lines = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
                if (n > 0 && lines.size() == n) {
                    break;
                }
            }
            String[] result = new String[lines.size()];
            lines.toArray(result);
            return result;
        }
    }

    @RBuiltin(".Internal.file")
    public abstract static class File extends RBuiltinNode {
        @Override
        public final boolean getVisibility() {
            return false;
        }

        @Specialization
        @SuppressWarnings("unused")
        public Object file(String description, String open, byte blocking, RAbstractStringVector encoding, byte raw) {
            controlVisibility();
            if (!open.equals("r")) {
                throw RError.getGenericError(getEncapsulatingSourceSection(), "unimplemented open mode:" + open);
            }
            String ePath = Utils.tildeExpand(description);
            try {
                // temporarily return invisible to avoid missing print support
                return new FileReadRConnection(ePath);
            } catch (IOException ex) {
                RContext.getInstance().setEvalWarning("cannot open file '" + description + "': " + ex.getMessage());
                throw RError.getGenericError(getEncapsulatingSourceSection(), "cannot open connection");
            }
        }

        @SuppressWarnings("unused")
        @Specialization(order = 100)
        public Object file(Object description, Object open, Object blocking, Object encoding, Object raw) {
            controlVisibility();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid arguments");
        }
    }

    @RBuiltin("close")
    public abstract static class Close extends RBuiltinNode {
        @Override
        public final boolean getVisibility() {
            return false;
        }

        @Specialization
        public Object close(@SuppressWarnings("unused") Object con) {
            controlVisibility();
            // TODO implement when on.exit doesn't evaluate it's argument
            return RNull.instance;
        }
    }

    @RBuiltin(".Internal.readLines")
    public abstract static class ReadLines extends RBuiltinNode {
        @Specialization
        public Object readLines(RConnection con, int n, byte ok, @SuppressWarnings("unused") byte warn, @SuppressWarnings("unused") String encoding) {
            controlVisibility();
            try {
                String[] lines = con.readLines(n);
                if (n > 0 && lines.length < n && ok == RRuntime.LOGICAL_FALSE) {
                    throw RError.getGenericError(getEncapsulatingSourceSection(), "too few lines read in readLines");
                }
                return RDataFactory.createStringVector(lines, RDataFactory.COMPLETE_VECTOR);
            } catch (IOException x) {
                throw RError.getGenericError(getEncapsulatingSourceSection(), "error reading connection: " + x.getMessage());
            }
        }

        @SuppressWarnings("unused")
        @Specialization(order = 100)
        public Object readLines(Object con, Object n, Object ok, Object warn, Object encoding) {
            controlVisibility();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid arguments");
        }
    }

}
