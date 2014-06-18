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
