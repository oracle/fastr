package com.oracle.truffle.r.test.packages.analyzer;

import java.io.BufferedReader;
import java.io.IOException;

public class FileLineStreamReader extends FileLineReader {
    /** Maximum number of lines that will be analyzed. */
    public static final int MAX_LINES = 50000;

    private final BufferedReader reader;
    private int cnt = 0;

    public FileLineStreamReader(BufferedReader in) {
        this.reader = in;
    }

    @Override
    public String readLine() throws IOException {
        if (cnt++ >= MAX_LINES) {
            throw new IOException("File is too large for analysis.");
        }
        return reader.readLine();
    }

    @Override
    public boolean isEmpty() {
        try {
            return reader.ready();
        } catch (IOException e) {
            return false;
        }
    }
}
