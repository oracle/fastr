/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.packages.analyzer;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class FileLineListReader extends FileLineReader {

    private final List<String> l;

    public FileLineListReader(List<String> lines) {
        this.l = lines;
    }

    @Override
    public boolean isEmpty() {
        return l.isEmpty();
    }

    @Override
    public LineIterator iterator() {
        return new LineIterator() {

            private final Iterator<String> it = l.iterator();

            @Override
            public void close() throws IOException {
                // nothing to do
            }

            @Override
            public String next() {
                return it.next();
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
        };
    }

}
