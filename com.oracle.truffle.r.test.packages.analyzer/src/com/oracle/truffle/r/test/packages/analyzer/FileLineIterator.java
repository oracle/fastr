/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.packages.analyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

public class FileLineIterator extends LineIterator {

    private static final Logger LOGGER = Logger.getLogger(FileLineIterator.class.getName());

    public static final int MAX_FILE_SIZE = 200 * 1024 * 1024;

    private BufferedReader reader;
    private String lookahead = null;

    public FileLineIterator(Path p) {
        try {
            long size = Files.size(p);
            if (size < MAX_FILE_SIZE) {
                this.reader = Files.newBufferedReader(p);
                nextLine();
            } else {
                this.reader = new BufferedReader(new InputStreamReader(new LimitSizeInputStreamReader(Files.newInputStream(p), MAX_FILE_SIZE)));
                LOGGER.fine(String.format("Will read at most %d bytes from file %s.", MAX_FILE_SIZE, p));
                this.reader = null;
            }
        } catch (IOException e) {
            LOGGER.severe(String.format("I/O error occurred when reading %s: %s", p, e.getMessage()));
        }
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    private String nextLine() {

        String line = lookahead;
        try {
            lookahead = reader.readLine();
        } catch (OutOfMemoryError e) {
            // If a single line is just too large, abort.
            lookahead = null;
        } catch (IOException e) {
            lookahead = null;
        }
        return line;
    }

    @Override
    public boolean hasNext() {
        return lookahead != null;
    }

    @Override
    public String next() {
        if (hasNext()) {
            return nextLine();
        }
        throw new NoSuchElementException();
    }

    private static class LimitSizeInputStreamReader extends InputStream {

        private final long limit;
        private final InputStream source;
        private long consumed;

        protected LimitSizeInputStreamReader(InputStream source, long limit) {
            this.source = source;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            if (consumed < limit) {
                consumed++;
                return source.read();
            }
            return -1;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, (int) Math.min(limit - consumed, len));
            consumed += read;
            return read;
        }

        @Override
        public int available() throws IOException {
            return Math.min((int) (limit - consumed), super.available());
        }

    }

}
