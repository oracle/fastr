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

import com.oracle.truffle.r.runtime.data.RAttributeStorage;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import java.io.PrintWriter;
import java.io.Writer;

public class PrettyPrintWriter extends PrintWriter implements PrettyWriter {
    
    public PrettyPrintWriter(Writer out) {
        super(out);
    }
    @Override
    public void begin(Object value) {
        if (out instanceof PrettyWriter) {
            ((PrettyWriter)out).begin(value);
        }
    }

    @Override
    public void end(Object value) {
        if (out instanceof PrettyWriter) {
            ((PrettyWriter)out).end(value);
        }
    }
    
    @Override
    public void beginAttributes(RAttributeStorage value) {
        if (out instanceof PrettyWriter) {
            ((PrettyWriter)out).beginAttributes(value);
        }
    }

    @Override
    public void endAttributes(RAttributeStorage value) {
        if (out instanceof PrettyWriter) {
            ((PrettyWriter)out).endAttributes(value);
        }
    }

    @Override
    public void beginValue(Object value) {
        if (out instanceof PrettyWriter) {
            ((PrettyWriter)out).beginValue(value);
        }
    }

    public Writer getUnderlyingWriter() {
        return out;
    }
    
    @Override
    public void endValue(Object value) {
        if (out instanceof PrettyWriter) {
            ((PrettyWriter)out).endValue(value);
        }
    }

    @Override
    public void beginElement(RAbstractVector vector, int index, VectorPrinter.FormatMetrics fm) {
        if (out instanceof PrettyWriter) {
            ((PrettyWriter)out).beginElement(vector, index, fm);
        }
    }

    @Override
    public void endElement(RAbstractVector vector, int index, VectorPrinter.FormatMetrics fm) {
        if (out instanceof PrettyWriter) {
            ((PrettyWriter)out).endElement(vector, index, fm);
        }
    }

    @Override
    public Object getPrintReport() {
        if (out instanceof PrettyWriter) {
            return ((PrettyWriter)out).getPrintReport();
        } else {
            return null;
        }
    }
    
    
}
