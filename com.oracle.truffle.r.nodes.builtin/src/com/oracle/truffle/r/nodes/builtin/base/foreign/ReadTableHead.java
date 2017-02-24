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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
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
    }

    @Specialization
    @TruffleBoundary
    public RAbstractStringVector read(int con, int nlines, @SuppressWarnings("unused") Object commentChar, @SuppressWarnings("unused") Object blankLinesSkip,
                    @SuppressWarnings("unused") Object quote, @SuppressWarnings("unused") Object sep, @SuppressWarnings("unused") Object skipNull) {
        // TODO This is quite incomplete and just uses readLines, which works for some inputs
        try (RConnection openConn = RConnection.fromIndex(con).forceOpen("r")) {
            return RDataFactory.createStringVector(openConn.readLines(nlines, true, false), RDataFactory.COMPLETE_VECTOR);
        } catch (IOException ex) {
            throw error(RError.Message.ERROR_READING_CONNECTION, ex.getMessage());
        }
    }
}
