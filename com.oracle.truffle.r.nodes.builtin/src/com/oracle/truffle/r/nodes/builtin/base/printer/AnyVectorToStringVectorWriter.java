/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.printer;

import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

public class AnyVectorToStringVectorWriter extends Writer implements PrettyWriter {
    private RAbstractVector vector;
    private StringBuilder sb = new StringBuilder();
    private String[] stringElements = null;
    private int levelCounter = 0;
    private boolean addSpaces;

    private static final RAttributeProfiles dummyAttrProfiles = RAttributeProfiles.create();

    @Override
    public void begin(Object value) {
        levelCounter++;
    }

    @Override
    public void end(Object value) {
        levelCounter--;
    }

    @Override
    public void beginAttributes(RAttributeStorage value) {
    }

    @Override
    public void endAttributes(RAttributeStorage value) {
    }

    @Override
    public void beginValue(Object value) {
        if (levelCounter == 1 && value instanceof RAbstractVector) {
            this.vector = (RAbstractVector) value;
            this.stringElements = new String[vector.getLength()];
        }
    }

    @Override
    public void endValue(Object value) {
    }

    @Override
    public void beginElement(RAbstractVector vector, int index, FormatMetrics fm) {
        if (levelCounter == 1) {
            sb = new StringBuilder();
        }
    }

    @Override
    public void endElement(RAbstractVector vector, int index, FormatMetrics fm) {
        if (levelCounter == 1) {
            String s = sb.toString().trim();

            if (index == 0) {
                addSpaces = (vector instanceof RAbstractLogicalVector) ||
                                s.length() < fm.originalMaxWidth;
            }

            if (addSpaces) {
                int ns = fm.getOriginalMaxWidth() - s.length();
                char[] spaces = new char[ns];
                Arrays.fill(spaces, ' ');
                stringElements[index] = new StringBuilder().append(spaces).append(s).toString();
            } else {
                stringElements[index] = sb.toString().trim();
            }
            sb = null;
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (sb != null) {
            sb.append(cbuf, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public RStringVector getPrintReport() {
        RStringVector sv = RDataFactory.createStringVector(stringElements, vector.isComplete());
        if (vector.getDimensions() != null) {
            sv.setDimensions(vector.getDimensions());
            sv.setDimNames(vector.getDimNames(dummyAttrProfiles));
        } else {
            sv.setNames(vector.getNames(dummyAttrProfiles));
        }
        return sv;
    }

}
