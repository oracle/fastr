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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

import com.oracle.truffle.r.runtime.conn.GZIPConnections.GZIPRConnection;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.tukaani.xz.LZMA2InputStream;
import org.tukaani.xz.XZInputStream;

/**
 * Abstracts the implementation of the various forms of compression used in R.
 */
public class RCompression {
    public enum Type {
        NONE('0'),
        GZIP('1'),
        BZIP2('2'),
        LZMA('Z');

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
                return RCompression.Type.LZMA;
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
                return lzmaUncompressInternal(udata, cdata);
            default:
                assert false;
                return false;
        }
    }

    public static boolean compress(Type type, byte[] udata, byte[] cdata) {
        switch (type) {
            case NONE:
                System.arraycopy(udata, 0, cdata, 0, udata.length);
                return true;
            case GZIP:
                return gzipCompress(udata, cdata);
            case BZIP2:
                throw RInternalError.unimplemented("BZIP2 compression");
            case LZMA:
                return lzmaCompress(udata, cdata);
            default:
                assert false;
                return false;
        }
    }

    private static boolean gzipCompress(byte[] udata, byte[] cdata) {
        long[] cdatalen = new long[1];
        cdatalen[0] = cdata.length;
        int rc = RFFIFactory.getRFFI().getZipRFFI().compress(cdata, cdatalen, udata);
        return rc == 0;
    }

    private static boolean gzipUncompress(byte[] udata, byte[] data) {
        long[] destlen = new long[1];
        destlen[0] = udata.length;
        int rc = RFFIFactory.getRFFI().getZipRFFI().uncompress(udata, destlen, data);
        return rc == 0;
    }

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

    private static boolean lzmaUncompressInternal(byte[] udata, byte[] data) {
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

    @FunctionalInterface
    private interface CreateInStream<T extends InputStream> {
        InputStream create(InputStream base) throws IOException;
    }

    public static byte[] lzmaUncompressFromFile(String path) {
        try {
            return genericUncompressFromFile(path, (is) -> new XZInputStream(is));
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    public static byte[] bzipUncompressFromFile(String path) {
        try {
            return genericUncompressFromFile(path, (is) -> new BZip2CompressorInputStream(is));
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    private static <T extends InputStream> byte[] genericUncompressFromFile(String path, CreateInStream<T> creator) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(path));
        byte[] buffer = new byte[data.length * 4];
        try (InputStream in = creator.create(new ByteArrayInputStream(data))) {
            int totalRead = 0;
            int n;
            while ((n = in.read(buffer, totalRead, buffer.length - totalRead)) > 0) {
                totalRead += n;
                if (totalRead == buffer.length) {
                    byte[] newbuffer = new byte[buffer.length * 2];
                    System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
                    buffer = newbuffer;
                }
            }
            byte[] result = new byte[totalRead];
            System.arraycopy(buffer, 0, result, 0, totalRead);
            return result;
        }
    }
}
