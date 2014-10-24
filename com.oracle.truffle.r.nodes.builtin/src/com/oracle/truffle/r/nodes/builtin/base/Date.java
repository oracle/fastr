/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import java.text.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;

@RBuiltin(name = "date", kind = RBuiltinKind.INTERNAL, parameterNames = {})
public abstract class Date extends RBuiltinNode {

    @Specialization
    @TruffleBoundary
    protected String date() {
        return RRuntime.toString(new SimpleDateFormat(RRuntime.SYSTEM_DATE_FORMAT).format(Calendar.getInstance().getTime()));
    }
}
