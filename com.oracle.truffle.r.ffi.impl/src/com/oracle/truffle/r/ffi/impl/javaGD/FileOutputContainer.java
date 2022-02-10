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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.rosuda.javaGD.GDContainer;

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.r.runtime.context.RContext;

public abstract class FileOutputContainer implements GDContainer {

    protected String fileNameTemplate;

    private int pageNumber = 0;

    protected FileOutputContainer(String fileNameTemplate) {
        this.fileNameTemplate = fileNameTemplate;
    }

    protected int getPageNumber() {
        return pageNumber;
    }

    public String getFileNameTemplate() {
        return fileNameTemplate;
    }

    public void setFileNameTemplate(String fileNameTemplate) {
        this.fileNameTemplate = fileNameTemplate;
    }

    @Override
    public void reset(int explicitPageNumber) {
        saveImage();
        if (explicitPageNumber >= 0) {
            pageNumber = explicitPageNumber;
        } else {
            pageNumber++;
        }
    }

    @Override
    public void closeDisplay() {
        saveImage();
    }

    static TruffleFile nextFile(int pageNumber, String fileNameTemplate) throws IOException {
        if (pageNumber == 0) {
            return null;
        }
        try {
            String fileName = String.format(fileNameTemplate, pageNumber);
            TruffleFile file = RContext.getInstance().getSafeTruffleFile(fileName);
            TruffleFile parent = file.getParent();
            if (isDevNull(file)) {
                return null;
            }
            if (parent != null && !parent.exists()) {
                parent.createDirectories();
            }

            return file;
        } catch (NullPointerException npe) {
            throw new FileNotFoundException("Path " + fileNameTemplate + " does not exist");
        }
    }

    private void saveImage() throws SaveImageException {
        try {
            TruffleFile file = nextFile(pageNumber, fileNameTemplate);
            if (file == null) {
                return;
            }
            saveImage(file);
        } catch (IOException e) {
            throw new SaveImageException(e);
        }
    }

    protected abstract void saveImage(TruffleFile file) throws IOException;

    static boolean isDevNull(TruffleFile file) {
        return file.getAbsoluteFile().getPath().equals("/dev/null");
    }

    public static class SaveImageException extends RuntimeException {

        private static final long serialVersionUID = 7928195763521729781L;

        public SaveImageException(Throwable cause) {
            super(cause);
        }

        @Override
        public String getMessage() {
            return getCause().getMessage();
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    public static class NotSupportedImageFormatException extends RuntimeException {

        private static final long serialVersionUID = -6539797720053321812L;

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

    }
}
