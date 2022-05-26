/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Graphics;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import com.oracle.truffle.api.TruffleFile;

public class SVGImageContainer extends AbstractImageContainer {

    private static final String SVG_NS = "http://www.w3.org/2000/svg";

    private final DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

    // TODO: use these parameters
    @SuppressWarnings("unused") private final boolean onefile;
    @SuppressWarnings("unused") private final String family;
    @SuppressWarnings("unused") private final String bg;
    @SuppressWarnings("unused") private final String antialias;

    private SVGGraphics2D graphics;

    SVGImageContainer(int width, int height, String fileNameTemplate, boolean onefile, String family, String bg, String antialias) {
        super(width, height, fileNameTemplate);

        this.onefile = onefile;
        this.family = family;
        this.bg = bg;
        this.antialias = antialias;

        resetGraphics();
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

    @Override
    protected void resetGraphics() {
        Document document = domImpl.createDocument(SVG_NS, "svg", null);
        this.graphics = new SVGGraphics2D(document);
        this.graphics.setSVGCanvasSize(size);
        defaultInitGraphics(this.graphics);
    }

    @Override
    protected void dumpImage(TruffleFile file) throws IOException {
        try (BufferedWriter writer = file.newBufferedWriter()) {
            graphics.stream(writer, false);
        }
    }
}
