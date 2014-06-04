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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;

import java.util.*;
import java.text.*;

@RBuiltin(name = "date", kind = RBuiltinKind.INTERNAL)
public abstract class Date extends RBuiltinNode {

    @Specialization
    public String date() {
        return RRuntime.toString(new SimpleDateFormat(RRuntime.SYSTEM_DATE_FORMAT).format(Calendar.getInstance().getTime()));
    }
}
