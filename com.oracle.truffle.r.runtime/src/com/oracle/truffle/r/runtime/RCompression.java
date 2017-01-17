/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.tukaani.xz.LZMA2InputStream;

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
        XZ('Z');

        public final byte typeByte;

        Type(char typeChar) {
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

        private static final byte GZIP_MAGIC1 = GZIPInputStream.GZIP_MAGIC & 0xFF;
        private static final byte GZIP_MAGIC2 = (byte) ((GZIPInputStream.GZIP_MAGIC >> 8) & 0xFF);

        /**
         * Decode the compression type from the bytes in buf (which must be at least length 5).
         */
        private static Type decodeBuf(byte[] buf) {
            if (buf[0] == GZIP_MAGIC1 && buf[1] == GZIP_MAGIC2) {
                return RCompression.Type.GZIP;
            } else if (buf[0] == 'B' && buf[1] == 'Z' && buf[2] == 'h') {
                return RCompression.Type.BZIP2;
            } else if (buf[0] == (byte) 0xFD && buf[1] == '7' && buf[2] == 'z' && buf[3] == 'X' && buf[4] == 'Z') {
                return RCompression.Type.XZ;
            } else {
                return RCompression.Type.NONE;
            }
        }
    }

    public static Type getCompressionType(String path) throws IOException {
        try (InputStream is = new FileInputStream(path)) {
            byte[] buf = new byte[5];
            int count = is.read(buf);
            if (count == 5) {
                return RCompression.Type.decodeBuf(buf);
            }
        }
        return RCompression.Type.NONE;
    }

    /**
     * Uncompress for internal use in {@code LazyLoadDBFetch} where size of uncompressed data is
     * known.
     *
     * @param type compression type
     * @param udata where to store uncompressed data
     * @param cdata data to uncompress
     * @return {@code true} iff success
     */
    public static boolean uncompress(Type type, byte[] udata, byte[] cdata) {
        switch (type) {
            case NONE:
                System.arraycopy(cdata, 0, udata, 0, cdata.length);
                return true;
            case GZIP:
                return gzipUncompress(udata, cdata);
            case BZIP2:
                throw RInternalError.unimplemented("BZIP2 compression");
            case XZ:
                return lzmaUncompress(udata, cdata);
            default:
                assert false;
                return false;
        }
    }

    /**
     * Uncompress for internal use in {@code LazyLoadDBInsertValue} where size of uncompressed data
     * is known.
     *
     * @param type compression type
     * @param udata uncompressed data
     * @param cdata where to store compressed data
     * @return {@code true} iff success
     */
    public static boolean compress(Type type, byte[] udata, byte[] cdata) {
        switch (type) {
            case NONE:
                System.arraycopy(udata, 0, cdata, 0, udata.length);
                return true;
            case GZIP:
                return gzipCompress(udata, cdata);
            case BZIP2:
                throw RInternalError.unimplemented("BZIP2 compression");
            case XZ:
                return lzmaCompress(udata, cdata);
            default:
                assert false;
                return false;
        }
    }

    private static boolean gzipCompress(byte[] udata, byte[] cdata) {
        int rc = RFFIFactory.getRFFI().getZipRFFI().compress(cdata, udata);
        return rc == 0;
    }

    private static boolean gzipUncompress(byte[] udata, byte[] data) {
        int rc = RFFIFactory.getRFFI().getZipRFFI().uncompress(udata, data);
        return rc == 0;
    }

    /**
     * There is no obvious counterpart to {@link LZMA2InputStream} and according to the XZ forum it
     * is not implemented for Java, so have to use sub-process.
     */
    private static boolean lzmaCompress(byte[] udata, byte[] cdata) {
        int rc;
        ProcessBuilder pb = new ProcessBuilder("xz", "--compress", "--format=raw", "--lzma2", "--stdout");
        pb.redirectError(Redirect.INHERIT);
        try {
            Process p = pb.start();
            OutputStream os = p.getOutputStream();
            InputStream is = p.getInputStream();
            ProcessOutputManager.OutputThread readThread = new ProcessOutputManager.OutputThreadFixed("xz", is, cdata);
            readThread.start();
            os.write(udata);
            os.close();
            rc = p.waitFor();
            if (rc == 0) {
                readThread.join();
                return true;
            }
        } catch (InterruptedException | IOException ex) {
            return false;
        }
        return rc == 0;

    }

    private static boolean lzmaUncompress(byte[] udata, byte[] data) {
        int dictSize = udata.length < LZMA2InputStream.DICT_SIZE_MIN ? LZMA2InputStream.DICT_SIZE_MIN : udata.length;
        try (LZMA2InputStream lzmaStream = new LZMA2InputStream(new ByteArrayInputStream(data), dictSize)) {
            int totalRead = 0;
            int n;
            while ((n = lzmaStream.read(udata, totalRead, udata.length - totalRead)) > 0) {
                totalRead += n;
            }
            return totalRead == udata.length;
        } catch (IOException ex) {
            return false;
        }
    }

    public static byte[] bzipUncompressFromFile(String path) throws IOException {
        String[] command = new String[]{"bzip2", "-dc", path};
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(Redirect.INHERIT);
        Process p = pb.start();
        InputStream is = p.getInputStream();
        ProcessOutputManager.OutputThreadVariable readThread = new ProcessOutputManager.OutputThreadVariable(command[0], is);
        readThread.start();
        try {
            int rc = p.waitFor();
            if (rc == 0) {
                readThread.join();
                return Arrays.copyOf(readThread.getData(), readThread.getTotalRead());
            }
        } catch (InterruptedException ex) {
            // fall through
        }
        throw new IOException();
    }

    public static void bzipCompressToFile(byte[] data, String path, boolean append) throws IOException {
        String[] command = new String[]{"bzip2", "-zc"};
        int rc;
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(Redirect.INHERIT);
        Process p = pb.start();
        InputStream is = p.getInputStream();
        OutputStream os = p.getOutputStream();
        ProcessOutputManager.OutputThreadVariable readThread = new ProcessOutputManager.OutputThreadVariable(command[0], is);
        readThread.start();
        os.write(data);
        os.close();
        try {
            rc = p.waitFor();
            if (rc == 0) {
                readThread.join();
                byte[] cData = Arrays.copyOf(readThread.getData(), readThread.getTotalRead());
                OpenOption[] openOptions = append ? new OpenOption[]{StandardOpenOption.APPEND} : new OpenOption[0];
                Files.write(Paths.get(path), cData, openOptions);
                return;
            }
        } catch (InterruptedException ex) {
            // fall through
        }
        throw new IOException();
    }
}
