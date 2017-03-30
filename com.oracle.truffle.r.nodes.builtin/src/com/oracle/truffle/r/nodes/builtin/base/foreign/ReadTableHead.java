/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public abstract class ReadTableHead extends RExternalBuiltinNode.Arg7 {

    static {
        Casts casts = new Casts(ReadTableHead.class);
        casts.arg(0).defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();
        casts.arg(1).mustNotBeNull().asIntegerVector().findFirst();
        casts.arg(2).mustNotBeMissing().mustBe(Predef.stringValue()).asStringVector().findFirst();
        casts.arg(3).mustNotBeMissing().mustBe(Predef.logicalValue()).asLogicalVector().findFirst().map(Predef.toBoolean());
        casts.arg(4).mustNotBeMissing().mustBe(Predef.stringValue()).asStringVector().findFirst();
        casts.arg(5).mustNotBeMissing().mustBe(Predef.stringValue()).asStringVector().findFirst();
        casts.arg(6).mustNotBeMissing().mustBe(Predef.logicalValue()).asLogicalVector().findFirst().map(Predef.toBoolean());
    }

    @Specialization
    @TruffleBoundary
    public RAbstractStringVector read(int con, int nlines, String commentChar, boolean blankLinesSkip,
                    String quote, String sep, boolean skipNull) {
        // TODO This is quite incomplete and just uses readLines, which works for some inputs
        try (RConnection openConn = RConnection.fromIndex(con).forceOpen("r")) {
            List<String> lines = new ArrayList<>(nlines);
            int totalLines = 0;
            while (totalLines < nlines) {
                String[] readLines = openConn.readLines(nlines - totalLines, true, skipNull);
                if (readLines.length == 0) {
                    break;
                }

                for (int i = 0; i < readLines.length; i++) {
                    postprocessLine(lines, readLines[i], commentChar, blankLinesSkip, quote, sep);
                }
                totalLines += lines.size();
            }

            return RDataFactory.createStringVector(lines.toArray(new String[0]), RDataFactory.COMPLETE_VECTOR);
        } catch (IOException ex) {
            throw error(RError.Message.ERROR_READING_CONNECTION, ex.getMessage());
        }
    }

    private static void postprocessLine(List<String> lines, String string, String commentChar, boolean blankLinesSkip, @SuppressWarnings("unused") String quote,
                    @SuppressWarnings("unused") String sep) {
        // TODO quote, sep
        if (blankLinesSkip && string.isEmpty()) {
            return;
        }

        if (commentChar != null && !commentChar.isEmpty() && string.startsWith(commentChar)) {
            return;
        }

        // no reason why not to add
        lines.add(string);
    }
}
