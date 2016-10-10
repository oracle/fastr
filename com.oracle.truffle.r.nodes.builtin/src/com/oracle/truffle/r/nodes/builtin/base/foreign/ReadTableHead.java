/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;

public final class ReadTableHead extends RExternalBuiltinNode {

    @Override
    @TruffleBoundary
    public Object call(RArgsValuesAndNames args) {
        // TODO This is quite incomplete and just uses readLines, which works for some inputs
        Object[] argValues = args.getArguments();
        RConnection conn = (RConnection) argValues[0];
        int nlines = castInt(castVector(argValues[1]));
        try (RConnection openConn = conn.forceOpen("r")) {
            return RDataFactory.createStringVector(openConn.readLines(nlines, true, false), RDataFactory.COMPLETE_VECTOR);
        } catch (IOException ex) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.ERROR_READING_CONNECTION, ex.getMessage());
        }
    }
}
