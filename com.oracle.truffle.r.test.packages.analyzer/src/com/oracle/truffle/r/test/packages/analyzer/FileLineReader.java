package com.oracle.truffle.r.test.packages.analyzer;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class FileLineReader implements Iterable<String> {

    public abstract String readLine() throws IOException;

    public abstract boolean isEmpty();

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {

            private String line = null;

            @Override
            public boolean hasNext() {
                try {
                    if (line == null) {
                        line = readLine();
                    }
                    return line != null;
                } catch (IOException e) {
                    // ignore
                }
                return false;
            }

            @Override
            public String next() {
                if (hasNext()) {
                    return line;
                }
                throw new NoSuchElementException();
            }
        };
    }

}
