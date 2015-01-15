/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import java.io.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ops.na.*;

public class LoadFunctions {

    @RBuiltin(name = "loadFromConn2", kind = RBuiltinKind.INTERNAL, parameterNames = {"con", "envir", "verbose"})
    public abstract static class LoadFromConn2 extends RInvisibleBuiltinNode {

        private final NACheck naCheck = NACheck.create();

        // from src/main/saveload.c

        @Specialization
        protected RStringVector load(VirtualFrame frame, RConnection con, REnvironment envir, @SuppressWarnings("unused") RAbstractLogicalVector verbose) {
            controlVisibility();
            try {
                // TODO: go figure - the loadFromConn2 function is called from load.R where the
// magic numbers are already checked but the connection does not seem to be reset

// RStringVector v = con.readChar(RDataFactory.createIntVector(new int[]{5},
// RDataFactory.COMPLETE_VECTOR), true);
// assert v.getLength() == 1;
// String s = v.getDataAt(0);
// if (s.equals("RDA2\n") || s.equals("RDB2\n") || s.equals("RDX2\n")) {
                Object o = RSerialize.unserialize(con, RArguments.getDepth(frame));
                if (!(o instanceof RPairList)) {
                    throw RError.error(RError.Message.GENERIC, "loaded data is not in pair list form");
                }
                RPairList vars = (RPairList) o;

                String[] data = new String[vars.getLength()];
                int i = 0;
                naCheck.enable(true);
                while (true) {
                    String tag = vars.getTag().toString();
                    data[i] = tag;
                    naCheck.check(tag);

                    envir.put(tag, vars.car());

                    if (vars.cdr() == null || vars.cdr() == RNull.instance) {
                        break;
                    }
                    vars = (RPairList) vars.cdr();
                    i++;
                }

                return RDataFactory.createStringVector(data, naCheck.neverSeenNA());

                // TODO: see TODO comment above
// } else {
// throw RError.error(RError.Message.GENERIC,
// "the input does not start with a magic number compatible with loading from a connection");
// }

            } catch (IOException iox) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_READING_CONNECTION, iox.getMessage());
            } catch (PutException px) {
                throw RError.error(this.getEncapsulatingSourceSection(), px);
            }

        }
    }

}
