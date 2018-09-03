/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid.device.remote;

import com.oracle.truffle.r.runtime.RInternalError;
import java.nio.charset.StandardCharsets;

public class RemoteDeviceDataExchange {

    public static String bytesToString(String title, byte[] buf, int limit) {
        StringBuilder sb = new StringBuilder(title.length() + (limit << 2));
        sb.append(title).append("0x");
        for (int i = 0; i < limit; i++) {
            int b = buf[i] & 0xFF;
            sb.append("0123456789ABCDEF".charAt(b >>> 4));
            sb.append("0123456789ABCDEF".charAt(b & 0x0F));
        }
        return sb.toString();
    }

    private static final int DEFAULT_BUF_SIZE = 64;

    private byte[] buf;

    private int index;

    private int limit;

    public RemoteDeviceDataExchange() {
        this.buf = new byte[DEFAULT_BUF_SIZE];
    }

    public RemoteDeviceDataExchange(byte[] readBuf, int limit) {
        this.buf = readBuf;
        this.limit = limit;
    }

    public void writeInt(int value) {
        ensureCapacity(4);
        buf[index++] = (byte) (value >>> 24);
        buf[index++] = (byte) (value >> 16);
        buf[index++] = (byte) (value >> 8);
        buf[index++] = (byte) value;
    }

    public int readInt() {
        ensureData(4);
        return ((buf[index++] & 0xff) << 24 |
                        (buf[index++] & 0xff) << 16 |
                        (buf[index++] & 0xff) << 8 |
                        (buf[index++] & 0xff));
    }

    public void writeIntArray(int[] value) {
        if (value != null) {
            int len = value.length;
            ensureCapacity(4 + (len << 2));
            writeInt(len);
            for (int i = 0; i < len; i++) {
                writeInt(value[i]);
            }
        } else {
            writeInt(-1);
        }
    }

    public int[] readIntArray() {
        int len = readInt();
        if (len == -1) {
            return null;
        }
        ensureData(len << 2);
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) {
            arr[i] = readInt();
        }
        return arr;
    }

    public void writeByte(byte value) {
        ensureCapacity(1);
        buf[index++] = value;
    }

    public byte readByte() {
        ensureData(1);
        return buf[index++];
    }

    public void writeByteArray(byte[] value) {
        if (value != null) {
            int len = value.length;
            ensureCapacity(4 + len);
            writeInt(len);
            System.arraycopy(value, 0, buf, index, len);
            index += len;
        } else {
            writeInt(-1);
        }
    }

    public byte[] readByteArray() {
        int len = readInt();
        if (len == -1) {
            return null;
        }
        ensureData(len);
        byte[] arr = new byte[len];
        System.arraycopy(buf, index, arr, 0, len);
        index += len;
        return arr;
    }

    public void writeDouble(double value) {
        ensureCapacity(8);
        long valueBits = Double.doubleToRawLongBits(value);
        buf[index++] = (byte) (valueBits >>> 56);
        buf[index++] = (byte) ((valueBits >> 48) & 0xff);
        buf[index++] = (byte) ((valueBits >> 40) & 0xff);
        buf[index++] = (byte) ((valueBits >> 32) & 0xff);
        buf[index++] = (byte) ((valueBits >> 24) & 0xff);
        buf[index++] = (byte) ((valueBits >> 16) & 0xff);
        buf[index++] = (byte) ((valueBits >> 8) & 0xff);
        buf[index++] = (byte) (valueBits & 0xff);
    }

    public double readDouble() {
        long bits = ((long) (buf[index++] & 0xff) << 56 | (long) (buf[index++] & 0xff) << 48 | (long) (buf[index++] & 0xff) << 40 | (long) (buf[index++] & 0xff) << 32 |
                        (long) (buf[index++] & 0xff) << 24 | (long) (buf[index++] & 0xff) << 16 | (long) (buf[index++] & 0xff) << 8 | buf[index++] & 0xff);
        return Double.longBitsToDouble(bits);
    }

    public void writeDoubleArray(double[] value) {
        if (value != null) {
            int len = value.length;
            ensureCapacity(4 + (len << 3));
            writeInt(len);
            for (int i = 0; i < len; i++) {
                writeDouble(value[i]);
            }
        } else {
            writeInt(-1);
        }
    }

    public double[] readDoubleArray() {
        int len = readInt();
        if (len == -1) {
            return null;
        }
        ensureData(len << 3);
        double[] arr = new double[len];
        for (int i = 0; i < len; i++) {
            arr[i] = readDouble();
        }
        return arr;
    }

    public void writeString(String value) {
        if (value != null) {
            boolean simple = true;
            for (int i = 0; i < value.length(); i++) {
                if (value.charAt(i) >= 0x80) {
                    simple = false;
                    break;
                }
            }
            if (simple && value.length() <= buf.length) {
                writeInt(value.length());
                ensureCapacity(value.length());
                for (int i = 0; i < value.length(); i++) {
                    buf[index++] = (byte) value.charAt(i);
                }
            } else {
                byte[] bytes = value.getBytes();
                int bytesLen = bytes.length;
                ensureCapacity(bytesLen + 4);
                writeInt(bytesLen);
                System.arraycopy(bytes, 0, buf, index, bytesLen);
                index += bytesLen;
            }
        } else { // value == null
            writeInt(-1);
        }
    }

    public String readString() {
        int strLen = readInt();
        if (strLen == -1) {
            return null;
        }
        boolean simple = true;
        for (int i = 0; i < strLen; i++) {
            byte b = buf[index + i];
            if (b < 0) {
                simple = false;
                break;
            }
        }
        String result;
        if (simple) {
            @SuppressWarnings("deprecation")
            String s = new String(buf, 0, index, strLen);
            result = s;
        } else {
            result = new String(buf, index, strLen, StandardCharsets.UTF_8);
        }
        index += strLen;
        return result;
    }

    /**
     * Grab all bytes written so far and return them as byte array and then reset write index to
     * zero for fresh writing.
     * 
     * @return bytes written prior call to this method.
     */
    public byte[] resetWrite() {
        byte[] result = new byte[index];
        System.arraycopy(buf, 0, result, 0, index);
        index = 0;
        return result;
    }

    public boolean isEmpty() {
        return (index == 0);
    }

    public boolean isReadFinished() {
        return (index == buf.length);
    }

    private void ensureCapacity(int nBytes) {
        int requireLen = index + nBytes;
        if (requireLen > buf.length) {
            byte[] newBuf = new byte[Math.max(requireLen, buf.length << 1)];
            System.arraycopy(buf, 0, newBuf, 0, index);
            buf = newBuf;
        }
    }

    private void ensureData(int nBytes) {
        if (index + nBytes > limit) {
            throw RInternalError.unimplemented("Unexpected EOF: " + (nBytes - limit) + " more bytes expected.");
        }
    }

}
