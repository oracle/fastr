package com.oracle.truffle.r.runtime.conn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

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

    public static void writeLinesHelper(OutputStream out, RAbstractStringVector lines, String sep) throws IOException {
        for (int i = 0; i < lines.getLength(); i++) {
            out.write(lines.getDataAt(i).getBytes());
            out.write(sep.getBytes());
        }
    }

    public static void writeStringHelper(ByteChannel out, String s, boolean nl) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(s.length() + 1);
        buf.put(s.getBytes());
        if (nl) {
            buf.putChar('\n');
        }
        buf.rewind();
        out.write(buf);
    }

    public static void writeCharHelper(ByteChannel channel, String s, int pad, String eos) throws IOException {

        ByteBuffer buf = ByteBuffer.allocate(s.length() + pad + eos.length() + 1);
        buf.put(s.getBytes());
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
     * Reads null-terminated character strings from an {@link InputStream}.
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

    public static String readCharHelper(int nchars, InputStream in, @SuppressWarnings("unused") boolean useBytes) throws IOException {
        byte[] bytes = new byte[nchars];
        in.read(bytes);
        int j = 0;
        for (; j < bytes.length; j++) {
            // strings end at 0
            if (bytes[j] == 0) {
                break;
            }
        }
        return new String(bytes, 0, j);
    }
}