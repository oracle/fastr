/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;

/**
 * Support for the "internal"method of "utils::download.file". TODO take note of "quiet", "mode" and
 * "cacheOK".
 */
public final class Download extends RExternalBuiltinNode {

    @SuppressWarnings("unused")
    private static void download(String urlString, String destFile, boolean quiet, String mode, boolean cacheOK) throws IOException {
        URL url = new URL(urlString);
        byte[] buffer = new byte[8192];
        try (BufferedInputStream in = new BufferedInputStream(url.openStream()); BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destFile))) {
            int nread;
            while ((nread = in.read(buffer)) > 0) {
                out.write(buffer, 0, nread);
            }
        }
    }

    @Override
    public Integer call(RArgsValuesAndNames args) {
        Object[] argValues = args.getArguments();
        String url = isString(argValues[0]);
        String destFile = isString(argValues[1]);
        byte quiet = castLogical(castVector(argValues[2]));
        String mode = isString(argValues[3]);
        byte cacheOK = castLogical(castVector(argValues[4]));
        if (url == null || destFile == null || mode == null) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_UNNAMED_ARGUMENTS);
        }
        try {
            Download.download(url, destFile, RRuntime.fromLogical(quiet), mode, RRuntime.fromLogical(cacheOK));
            return 0;
        } catch (IOException ex) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
        }
    }
}
