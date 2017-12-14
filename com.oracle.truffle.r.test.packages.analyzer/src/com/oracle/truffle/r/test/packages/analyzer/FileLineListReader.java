package com.oracle.truffle.r.test.packages.analyzer;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class FileLineListReader extends FileLineReader {

    private final Iterator<String> it;
    private final boolean empty;

    public FileLineListReader(List<String> lines) {
        Iterator<String> iterator = lines.iterator();
        this.it = iterator;
        this.empty = iterator.hasNext();
    }

    @Override
    public String readLine() throws IOException {
        if (!it.hasNext()) {
            return null;
        }
        return it.next();
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }

    @Override
    public Iterator<String> iterator() {
        return it;
    }

}
