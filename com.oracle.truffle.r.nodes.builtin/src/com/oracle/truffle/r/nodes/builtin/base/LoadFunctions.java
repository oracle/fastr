/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2013, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

public class LoadFunctions {

    @RBuiltin(name = "loadFromConn2", kind = RBuiltinKind.INTERNAL, parameterNames = {"con", "envir", "verbose"})
    public abstract static class LoadFromConn2 extends RInvisibleBuiltinNode {

        @Specialization
        protected RStringVector load(RConnection con, REnvironment envir, RAbstractLogicalVector verbose) {
            controlVisibility();
            return null;
        }
    }

}
