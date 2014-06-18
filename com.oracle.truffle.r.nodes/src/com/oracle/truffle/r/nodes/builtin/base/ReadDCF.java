/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import java.io.*;
import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = ".readDCF", kind = INTERNAL)
public abstract class ReadDCF extends RBuiltinNode {

    @Specialization
    public RStringVector doReadDCF(RConnection conn, RAbstractStringVector fields, @SuppressWarnings("unused") RNull keepWhite) {
        try {
            DCF dcf = DCF.read(conn.readLines(0));
            if (dcf == null) {
                throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid connection");
            }
            String[] data = new String[fields.getLength()];
            String[] names = new String[data.length];
            boolean complete = RDataFactory.COMPLETE_VECTOR;
            for (int i = 0; i < data.length; i++) {
                List<DCF.Fields> records = dcf.getRecords();
                // not looking at multi-record files
                Map<String, String> fieldMap = records.get(0).getFields();
                String fieldName = fields.getDataAt(i);
                String value = fieldMap.get(fieldName);
                if (value == null) {
                    data[i] = RRuntime.STRING_NA;
                    complete = RDataFactory.INCOMPLETE_VECTOR;
                } else {
                    data[i] = value;
                }
                names[i] = fieldName;
            }
            return RDataFactory.createStringVector(data, complete, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR));
        } catch (IOException ex) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), "error reading connection");
        }

    }
}
