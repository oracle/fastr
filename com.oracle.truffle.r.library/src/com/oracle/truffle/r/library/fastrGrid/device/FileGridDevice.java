/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid.device;

/**
 * Should be implemented by devices that save their output into a file. Such devices should only
 * accept complete filenames, i.e. without '%d' placeholder, and the handling of the placeholder is
 * left up to the calling code.
 */
public interface FileGridDevice extends GridDevice {
    /**
     * Each call to {@link #openNewPage()} should save the current image (into the current path) and
     * start drawing a new image into the given path, i.e. the given path becomes a new current
     * path.
     * 
     * @param filename tha path where to save the next image, will not contain '%d' symbol as its
     *            processing should be handled by the caller.
     * @throws DeviceCloseException
     */
    void openNewPage(String filename) throws DeviceCloseException;
}
