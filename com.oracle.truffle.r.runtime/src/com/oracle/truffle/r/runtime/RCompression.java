/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * Abstracts the implementation of the various forms of compression used in R. Since the C API for
 * LZMA is very complex (as used by GnuR), we use an 'xz' subprocess to do the work.
 */
public class RCompression {
    public enum Type {
        NONE('0'),
        GZIP('1'),
        BZIP2('2'),
        LZMA('Z');

        public final byte typeByte;

        private Type(char typeChar) {
            this.typeByte = (byte) typeChar;
        }

        public static Type fromTypeChar(byte typeByte) {
            for (Type t : Type.values()) {
                if (t.typeByte == typeByte) {
                    return t;
                }
            }
            return null;
        }
    }

    public static boolean uncompress(Type type, byte[] udata, byte[] cdata) {
        switch (type) {
            case NONE:
                System.arraycopy(cdata, 0, udata, 0, cdata.length);
                return true;
            case GZIP:
                return gzipUncompress(udata, cdata);
            case BZIP2:
                throw RInternalError.unimplemented("BZIP2 compression");
            case LZMA:
                return lzmaUncompress(udata, cdata);
            default:
                assert false;
                return false;
        }
    }

    private static boolean gzipUncompress(byte[] udata, byte[] data) {
        long[] destlen = new long[1];
        destlen[0] = udata.length;
        int rc = RFFIFactory.getRFFI().getBaseRFFI().uncompress(udata, destlen, data);
        return rc == 0;
    }

    private static boolean lzmaUncompress(byte[] udata, byte[] data) {
        int rc;
        ProcessBuilder pb = new ProcessBuilder("xz", "--decompress", "--format=raw", "--lzma2", "--stdout");
        pb.redirectError(Redirect.INHERIT);
        try {
            Process p = pb.start();
            OutputStream os = p.getOutputStream();
            InputStream is = p.getInputStream();
            ProcessOutputThread readThread = new ProcessOutputThread(is, udata);
            readThread.start();
            os.write(data);
            os.close();
            rc = p.waitFor();
            if (rc == 0) {
                readThread.join();
                if (readThread.totalRead != udata.length) {
                    return false;
                }
            }
        } catch (InterruptedException | IOException ex) {
            rc = 127;
        }
        return rc == 0;
    }

    private static final class ProcessOutputThread extends Thread {
        private byte[] udata;
        private InputStream is;
        private int totalRead;

        private ProcessOutputThread(InputStream is, byte[] udata) {
            super("XZProcessOutputThread");
            this.is = is;
            this.udata = udata;
        }

        @Override
        public void run() {
            int n;
            try {
                while (totalRead < udata.length && (n = is.read(udata, totalRead, udata.length - totalRead)) != -1) {
                    totalRead += n;
                }
            } catch (IOException ex) {
                return;
            }

        }
    }

}
