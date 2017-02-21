package com.oracle.truffle.r.runtime.conn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.RConnection.SeekMode;
import com.oracle.truffle.r.runtime.conn.RConnection.SeekRWMode;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.sun.istack.internal.NotNull;

public class ReadWriteHelper {

    /**
     * {@code readLines} from an {@link InputStream}. It would be convenient to use a
     * {@link BufferedReader} but mixing binary and text operations, which is a requirement, would
     * then be difficult.
     *
     * @param warn TODO
     * @param skipNul TODO
     */
    public static String[] readLinesHelper(InputStream in, int n, boolean warn, boolean skipNul) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        int totalRead = 0;
        byte[] buffer = new byte[64];
        int pushBack = 0;
        while (true) {
            int ch;
            if (pushBack != 0) {
                ch = pushBack;
                pushBack = 0;
            } else {
                ch = in.read();
            }
            boolean lineEnd = false;
            if (ch < 0) {
                if (totalRead > 0) {
                    /*
                     * TODO GnuR says keep data and output a warning if blocking, otherwise silently
                     * push back. FastR doesn't support non-blocking yet, so we keep the data. Some
                     * refactoring is needed to be able to reliably access the "name" for the
                     * warning.
                     */
                    lines.add(new String(buffer, 0, totalRead));
                    if (warn) {
                        RError.warning(RError.SHOW_CALLER2, RError.Message.INCOMPLETE_FINAL_LINE, "TODO: connection path");
                    }
                }
                break;
            }
            if (ch == '\n') {
                lineEnd = true;
            } else if (ch == '\r') {
                lineEnd = true;
                ch = in.read();
                if (ch == '\n') {
                    // swallow the trailing lf
                } else {
                    pushBack = ch;
                }
            }
            if (lineEnd) {
                lines.add(new String(buffer, 0, totalRead));
                if (n > 0 && lines.size() == n) {
                    break;
                }
                totalRead = 0;
            } else {
                buffer = ConnectionSupport.checkBuffer(buffer, totalRead);
                buffer[totalRead++] = (byte) (ch & 0xFF);
            }
        }
        String[] result = new String[lines.size()];
        lines.toArray(result);
        return result;
    }

    public static void writeLinesHelper(WritableByteChannel out, RAbstractStringVector lines, String sep) throws IOException {
        for (int i = 0; i < lines.getLength(); i++) {
            final String line = lines.getDataAt(i);
            writeStringHelper(out, line, false);
            writeStringHelper(out, sep, false);
        }
    }

    public static void writeStringHelper(WritableByteChannel out, String s, boolean nl) throws IOException {
        final byte[] bytes = s.getBytes();
        final byte[] lineSepBytes = nl ? System.lineSeparator().getBytes() : null;

        ByteBuffer buf = ByteBuffer.allocate(bytes.length + (nl ? lineSepBytes.length : 0));
        buf.put(bytes);
        if (nl) {
            buf.put(lineSepBytes);
        }

        buf.rewind();
        out.write(buf);
    }

    /**
     * Writes characters in binary mode (without any re-encoding) to the provided channel.
     *
     * @param channel The writable byte channel to write to (must not be {@code null}).
     * @param s The character string to write (must not be {@code null}).
     * @param pad The number of null characters to append to the characters.
     * @param eos The end-of-string terminator (may be {@code null}).
     * @throws IOException
     */
    public static void writeCharHelper(@NotNull WritableByteChannel channel, @NotNull String s, int pad, String eos) throws IOException {

        final byte[] bytes = s.getBytes();
        final byte[] eosBytes = eos != null ? eos.getBytes() : null;

        final int bufLen = bytes.length + (pad > 0 ? pad : 0) + (eos != null ? eosBytes.length + 1 : 0);
        assert bufLen >= s.length();
        ByteBuffer buf = ByteBuffer.allocate(bufLen);
        buf.put(bytes);
        if (pad > 0) {
            for (int i = 0; i < pad; i++) {
                buf.put((byte) 0);
            }
        }
        if (eos != null) {
            if (eos.length() > 0) {
                buf.put(eos.getBytes());
            }
            // function writeChar is defined to append the null character if eos != null
            buf.put((byte) 0);
        }
        buf.rewind();
        channel.write(buf);
    }

    @Deprecated
    public static void writeBinHelper(ByteBuffer buffer, OutputStream outputStream) throws IOException {
        int n = buffer.remaining();
        byte[] b = new byte[n];
        buffer.get(b);
        outputStream.write(b);
    }

    /**
     * Reads null-terminated character strings from a {@link InputStream}.
     */
    public static byte[] readBinCharsHelper(InputStream in) throws IOException {
        int ch = in.read();
        if (ch < 0) {
            return null;
        }
        int totalRead = 0;
        byte[] buffer = new byte[64];
        while (true) {
            buffer = ConnectionSupport.checkBuffer(buffer, totalRead);
            buffer[totalRead++] = (byte) (ch & 0xFF);
            if (ch == 0) {
                break;
            }
            ch = in.read();
        }
        return buffer;
    }

    public static int readBinHelper(ByteBuffer buffer, InputStream inputStream) throws IOException {
        int bytesToRead = buffer.remaining();
        byte[] b = new byte[bytesToRead];
        int totalRead = 0;
        int thisRead = 0;
        while ((totalRead < bytesToRead) && ((thisRead = inputStream.read(b, totalRead, bytesToRead - totalRead)) > 0)) {
            totalRead += thisRead;
        }
        buffer.put(b, 0, totalRead);
        return totalRead;
    }

    public static String readCharHelper(int nchars, ReadableByteChannel channel, @SuppressWarnings("unused") boolean useBytes) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(nchars);
        channel.read(buf);
        int j = 0;
        for (; j < buf.position(); j++) {
            // strings end at 0
            if (buf.get(j) == 0) {
                break;
            }
        }

        return new String(buf.array(), 0, j);
    }

    /**
     * TODO probably, this method belongs to {@link ConnectionSupport}
     */
    public static long seek(SeekableByteChannel channel, long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
        long position = channel.position();
        switch (seekMode) {
            case ENQUIRE:
                break;
            case CURRENT:
                if (offset != 0) {
                    channel.position(position + offset);
                }
                break;
            case START:
                channel.position(offset);
                break;
            case END:
                throw RInternalError.unimplemented();

        }
        return position;
    }
}