/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid.device.awt;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.r.library.fastrGrid.device.FileGridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.NotSupportedImageFormatException;
import com.oracle.truffle.r.runtime.FileSystemUtils;
import com.oracle.truffle.r.runtime.context.RContext;

public final class BufferedImageDevice extends Graphics2DDevice implements FileGridDevice {
    private final BufferedImage image;
    private final String fileType;
    private final Env env;
    private String filename;

    private BufferedImageDevice(RContext context, String fileType, BufferedImage image, Graphics2D graphics, int width, int height, String filename) {
        super(graphics, width, height, true);
        this.env = context.getEnv();
        this.filename = filename;
        this.fileType = fileType;
        this.image = image;
        graphics.setBackground(new Color(255, 255, 255));
        graphics.clearRect(0, 0, width, height);
    }

    public static BufferedImageDevice open(RContext context, String filename, String fileType, int width, int height) throws NotSupportedImageFormatException {
        if (!isSupportedFormat(fileType)) {
            throw new NotSupportedImageFormatException();
        }
        BufferedImage image = new BufferedImage(width, height, TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        defaultInitGraphics(graphics);
        return new BufferedImageDevice(context, fileType, image, graphics, width, height, filename);
    }

    @Override
    public void openNewPage(String newFilename) throws DeviceCloseException {
        saveImage();
        filename = newFilename;
        openNewPage();
    }

    @Override
    public void close() throws DeviceCloseException {
        saveImage();
    }

    private void saveImage() throws DeviceCloseException {
        try {
            TruffleFile file = FileSystemUtils.getSafeTruffleFile(env, filename);
            TruffleFile parent = file.getParent();
            if (FileGridDevice.isDevNull(file)) {
                return;
            }
            if (parent != null && !parent.exists()) {
                // Bug in JDK? when the path contains directory that does not exist, the code throws
                // NPE and prints out to the standard output (!) stack trace of
                // FileNotFoundException. We still catch the exception, because this check and
                // following Image.write are not atomic.
                throw new DeviceCloseException(new FileNotFoundException("Path " + filename + " does not exist"));
            }
            ImageIO.write(image, fileType, file.newOutputStream());
        } catch (IOException e) {
            throw new DeviceCloseException(e);
        } catch (NullPointerException npe) {
            throw new DeviceCloseException(new FileNotFoundException("Path " + filename + " does not exist"));
        }
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

}
