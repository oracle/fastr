/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.DCF;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "readDCF", kind = INTERNAL, parameterNames = {"conn", "fields", "keepwhite"}, behavior = IO)
public abstract class ReadDCF extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("conn").mustBe(RConnection.class);
        casts.arg("fields").mapNull(emptyStringVector()).asStringVector();
        casts.arg("keepwhite").mapNull(emptyStringVector()).asStringVector();
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector doReadDCF(RConnection conn, RAbstractStringVector fields, RAbstractStringVector keepWhite) {
        DCF dcf = null;
        try (RConnection openConn = conn.forceOpen("r")) {
            Set<String> keepWhiteSet = null;
            if (keepWhite.getLength() > 0) {
                keepWhiteSet = new HashSet<>(keepWhite.getLength());
                for (int i = 0; i < keepWhite.getLength(); i++) {
                    keepWhiteSet.add(keepWhite.getDataAt(i));
                }
            }
            dcf = DCF.read(openConn.readLines(0, true, false), keepWhiteSet);
        } catch (IOException ex) {
            throw RError.error(this, RError.Message.ERROR_READING_CONNECTION, ex.getMessage());
        }
        if (dcf == null) {
            throw RError.error(this, RError.Message.INVALID_CONNECTION);
        }
        List<DCF.Fields> records = dcf.getRecords();
        int nRecords = records.size();
        // Maximum number of columns is the number of distinct fields (tags)
        // possibly modulated by "fields" argument
        LinkedHashMap<String, String> allFields = new LinkedHashMap<>();
        for (DCF.Fields record : records) {
            LinkedHashMap<String, String> fieldMap = record.getFields();
            for (String key : fieldMap.keySet()) {
                if (needField(key, fields)) {
                    allFields.put(key, key);
                }
            }
        }
        int nColumns = allFields.size();
        String[] data = new String[nRecords * nColumns];
        String[] columnNames = new String[nColumns];
        allFields.keySet().toArray(columnNames);
        boolean complete = RDataFactory.COMPLETE_VECTOR;

        // now scan the records and fill in the matrix
        for (int r = 0; r < nRecords; r++) {
            DCF.Fields record = records.get(r);
            Map<String, String> fieldMap = record.getFields();
            for (int c = 0; c < columnNames.length; c++) {
                int index = c * nRecords + r;
                String columnName = columnNames[c];
                String value = fieldMap.get(columnName);
                if (value == null) {
                    // this record did not have this field
                    data[index] = RRuntime.STRING_NA;
                    complete = RDataFactory.INCOMPLETE_VECTOR;
                } else {
                    data[index] = value;
                }
            }
        }
        int[] dims = new int[]{nRecords, nColumns};
        RList dimnames = RDataFactory.createList(new Object[]{RNull.instance, RDataFactory.createStringVector(columnNames, RDataFactory.COMPLETE_VECTOR)});
        RStringVector result = RDataFactory.createStringVector(data, complete, dims);
        result.setDimNames(dimnames);
        return result;

    }

    private static boolean needField(String fieldName, RAbstractStringVector fields) {
        if (fields.getLength() == 0) {
            return true;
        }
        for (int i = 0; i < fields.getLength(); i++) {
            if (fieldName.equals(fields.getDataAt(i))) {
                return true;
            }
        }
        return false;
    }

}
