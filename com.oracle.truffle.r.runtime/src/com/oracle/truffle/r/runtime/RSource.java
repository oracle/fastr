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
package com.oracle.truffle.r.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.oracle.truffle.api.source.Source;

public class RSource {
    public static Source fromFileName(String text, String path) {
        File file = new File(path).getAbsoluteFile();
        try {
            URI uri = new URI("file://" + file.getAbsolutePath());
            return Source.newBuilder(text).name(path).uri(uri).mimeType(RRuntime.R_APP_MIME).build();
        } catch (URISyntaxException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    public static Source fromText(String text, String name) {
        return fromText(text, name, RRuntime.R_APP_MIME);
    }

    public static Source fromText(String text, String name, String mimeType) {
        return Source.newBuilder(text).name(name).mimeType(mimeType).build();
    }

    public static Source fromFileName(String path) throws IOException {
        return Source.newBuilder(new File(path)).name(path).mimeType(RRuntime.R_APP_MIME).build();
    }

    public static Source fromFile(File file) throws IOException {
        return Source.newBuilder(file).name(file.getName()).mimeType(RRuntime.R_APP_MIME).build();
    }

    public static Source fromURL(URL url, String name) throws IOException {
        return Source.newBuilder(url).name(name).mimeType(RRuntime.R_APP_MIME).build();
    }
}
