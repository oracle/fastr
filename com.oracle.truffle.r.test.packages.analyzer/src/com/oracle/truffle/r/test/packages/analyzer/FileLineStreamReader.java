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
import java.util.Iterator;
import java.util.NoSuchElementException;

public class FileLineStreamReader extends FileLineReader {
    /** Maximum number of lines that will be analyzed. */
    public static final int MAX_LINES = 50000;

    private final BufferedReader reader;
    private int cnt = 0;
    private String lookahead = null;
    private boolean empty;

    public FileLineStreamReader(BufferedReader in) {
        this.reader = in;
        empty = true;
        try {
            nextLine();
            empty = lookahead == null;
        } catch (IOException e) {
            // ignore
        }

    }

    private String nextLine() throws IOException {
        if (cnt++ >= MAX_LINES) {
            throw new IOException("File is too large for analysis.");
        }
        String line = lookahead;
        lookahead = reader.readLine();
        return line;
    }

    @Override
    public String readLine() throws IOException {
        return nextLine();
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {

            @Override
            public boolean hasNext() {
                return lookahead != null;
            }

            @Override
            public String next() {
                if (hasNext()) {
                    try {
                        return nextLine();
                    } catch (IOException e) {
                    }
                }
                throw new NoSuchElementException();
            }
        };
    }

}
