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
package com.oracle.truffle.r.test.packages.analyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

public class FileLineIterator extends LineIterator {

    /** Maximum number of lines that will be analyzed. */
    public static final int MAX_LINES = 500000;

    private int cnt = 0;
    private BufferedReader reader;
    private String lookahead = null;

    public FileLineIterator(Path p) {
        try {
            this.reader = Files.newBufferedReader(p);
            nextLine();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    private String nextLine() {

        if (cnt++ >= MAX_LINES) {
            return null;
        }
        String line = lookahead;
        try {
            lookahead = reader.readLine();
        } catch (IOException e) {

        }
        return line;
    }

    @Override
    public boolean hasNext() {
        return cnt < MAX_LINES && lookahead != null;
    }

    @Override
    public String next() {
        if (hasNext()) {
            return nextLine();
        }
        throw new NoSuchElementException();
    }
}
