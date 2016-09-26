/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

/**
 * Managing output from sub-processes, which is needed in a several places, e.g.
 * {@link RCompression}.
 */
public class ProcessOutputManager {
    public abstract static class OutputThread extends Thread {
        protected final InputStream is;
        protected int totalRead;
        protected final Semaphore entry = new Semaphore(0);
        protected final Semaphore exit = new Semaphore(0);

        protected OutputThread(String name, InputStream is) {
            super(name);
            this.is = is;
        }

        public int getTotalRead() {
            return totalRead;
        }
    }

    /**
     * Reads until the expected length or EOF (which is an error).
     */
    public static final class OutputThreadFixed extends OutputThread {
        protected byte[] data;

        public OutputThreadFixed(String name, InputStream is, byte[] data) {
            super(name, is);
            this.data = data;
        }

        @Override
        public void run() {
            int n;
            try {
                while (totalRead < data.length && (n = is.read(data, totalRead, data.length - totalRead)) != -1) {
                    totalRead += n;
                }
            } catch (IOException ex) {
                return;
            }
        }
    }

    /**
     * Reads a variable sized amount of data into a growing array.
     *
     */
    public static final class OutputThreadVariable extends OutputThread {
        private byte[] data;

        public OutputThreadVariable(String name, InputStream is) {
            super(name, is);
            this.data = new byte[8192];
        }

        @Override
        public void run() {
            int n;
            try {
                while ((n = is.read(data, totalRead, data.length - totalRead)) != -1) {
                    totalRead += n;
                    if (totalRead == data.length) {
                        byte[] udataNew = new byte[data.length * 2];
                        System.arraycopy(data, 0, udataNew, 0, data.length);
                        data = udataNew;
                    }
                }
            } catch (IOException ex) {
                // unexpected, we will just return what we have read so far
            } finally {
                exit.release();
            }
        }

        /**
         * Waits for the reads to complete and then returns the internal byte array used to store
         * the data, which may be larger than {@link #totalRead}. Use {@link #getTotalRead} to
         * access the subarray containing the read data.
         */
        public byte[] getData() {
            try {
                exit.acquire();
            } catch (InterruptedException e) {

            }
            return data;
        }
    }
}
