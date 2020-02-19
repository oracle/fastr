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
        defaultInitGraphics(this.graphics);
    }

    @Override
    protected void dumpImage(TruffleFile file) throws IOException {
        try (BufferedWriter writer = file.newBufferedWriter()) {
            graphics.stream(writer, false);
        }
    }
}
