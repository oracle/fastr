/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.codegen;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Optional;

public abstract class CodeGenBase {
    protected static final String COPYRIGHT = "/*\n" +
                    " * Copyright (c) 2018, " + Calendar.getInstance().get(Calendar.YEAR) + ", Oracle and/or its affiliates. All rights reserved.\n" +
                    " * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.\n" +
                    " *\n" +
                    " * This code is free software; you can redistribute it and/or modify it\n" +
                    " * under the terms of the GNU General Public License version 3 only, as\n" +
                    " * published by the Free Software Foundation.\n" +
                    " *\n" +
                    " * This code is distributed in the hope that it will be useful, but WITHOUT\n" +
                    " * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n" +
                    " * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License\n" +
                    " * version 3 for more details (a copy is included in the LICENSE file that\n" +
                    " * accompanied this code).\n" +
                    " *\n" +
                    " * You should have received a copy of the GNU General Public License version\n" +
                    " * 3 along with this work; if not, write to the Free Software Foundation,\n" +
                    " * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.\n" +
                    " *\n" +
                    " * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA\n" +
                    " * or visit www.oracle.com if you need additional information or have any\n" +
                    " * questions.\n" +
                    " */\n";

    protected PrintStream out;

    protected void initOutput(String[] args) {
        Optional<String> filename = Arrays.stream(args).filter(x -> !x.contains("-")).findFirst();
        if (filename.isPresent()) {
            try {
                out = new PrintStream(new FileOutputStream(filename.get()));
            } catch (FileNotFoundException e) {
                System.err.printf("Cannot open file '%s' for writing.\n", filename.get());
                System.exit(1);
            }
        } else {
            out = System.out;
        }
    }
}
