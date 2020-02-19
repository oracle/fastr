/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.javaGD;

import java.util.HashMap;
import java.util.Map;

import org.rosuda.javaGD.GDInterface;

public class SVGImageGD extends GDInterface {
    private final String fileNameTemplate;
    private final boolean onefile;
    private final String family;
    private final String bg;
    private final String antialias;

    public SVGImageGD(String fileNameTemplate, String params) {
        this.fileNameTemplate = fileNameTemplate;

        Map<String, String> paramsMap = new HashMap<>();
        if (params != null) {
            String[] pairs = params.split(",");
            for (String pair : pairs) {
                String[] nameValue = pair.split("=");
                paramsMap.put(nameValue[0], nameValue[1]);
            }
        }

        this.onefile = fromRLogical(paramsMap.getOrDefault("family", "FALSE"));
        this.family = paramsMap.getOrDefault("family", "sans");
        this.bg = paramsMap.getOrDefault("bg", "white");
        this.antialias = paramsMap.getOrDefault("antialias", "default");
    }

    private static boolean fromRLogical(String l) {
        return "TRUE".equals(l);
    }

    @Override
    public void gdOpen(double width, double height) {
        super.gdOpen(width, height);
        c = new SVGImageContainer((int) width, (int) height, fileNameTemplate, onefile, family, bg, antialias);
    }
}
