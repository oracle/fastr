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
package com.oracle.truffle.r.runtime.data;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RObjectSize.IgnoreObjectHandler;

/**
 * A debugging tool, logs all calls to {@link #getObjectSize(Object, IgnoreObjectHandler)} to a
 * file.
 *
 */
public class OutputAgentObjectSizeFactory extends AgentObjectSizeFactory {

    private PrintWriter printWriter;

    public OutputAgentObjectSizeFactory() {
        try {
            printWriter = new PrintWriter(new FileWriter(Utils.getLogPath("fastr_objectsize.log").toString()));
        } catch (IOException ex) {
            Utils.fail(ex.getMessage());
        }

    }

    @Override
    public long getObjectSize(Object obj, IgnoreObjectHandler ignoreObjectHandler) {
        long size = super.getObjectSize(obj, ignoreObjectHandler);
        printWriter.printf("%s: %d\n", obj.getClass().getSimpleName(), (int) size);
        printWriter.flush();
        return size;
    }
}
