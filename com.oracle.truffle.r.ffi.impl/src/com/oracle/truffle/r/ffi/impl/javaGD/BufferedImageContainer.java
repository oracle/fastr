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

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

import com.oracle.truffle.api.TruffleFile;

public class BufferedImageContainer extends AbstractImageContainer {

    private final BufferedImage image;
    private final Graphics2D graphics;
    private final String fileType;
    private final float quality;

    public BufferedImageContainer(int width, int height, String fileType, String fileNameTemplate, float quality) {
        super(width, height, fileNameTemplate);

        if (!isSupportedFormat(fileType)) {
            throw new NotSupportedImageFormatException();
        }

        this.fileType = fileType;
        this.quality = quality;

        this.image = new BufferedImage(width, height, TYPE_INT_RGB);
        this.graphics = (Graphics2D) image.getGraphics();
        defaultInitGraphics(this.graphics);
    }

    private static boolean isSupportedFormat(String formatName) {
        String[] formatNames = ImageIO.getWriterFormatNames();
        for (String n : formatNames) {
            if (n.equals(formatName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void resetGraphics() {
        graphics.clearRect(0, 0, size.width, size.height);
    }

    @Override
    protected void dumpImage(TruffleFile file) throws FileNotFoundException, IOException {
        if ("jpeg".equalsIgnoreCase(fileType) || "jpg".equalsIgnoreCase(fileType)) {
            saveJPEG(file);
        } else {
            ImageIO.write(image, fileType, file.newOutputStream());
        }
    }

    private void saveJPEG(TruffleFile file) throws FileNotFoundException, IOException {
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(new FileImageOutputStream(new File(file.getPath())));

        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpegParams.setCompressionQuality(quality);
        writer.write(null, new IIOImage(image, null, null), jpegParams);
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

}
