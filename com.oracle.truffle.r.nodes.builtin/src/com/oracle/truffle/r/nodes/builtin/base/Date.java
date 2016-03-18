/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RRuntime;

@RBuiltin(name = "date", kind = RBuiltinKind.INTERNAL, parameterNames = {})
public abstract class Date extends RBuiltinNode {

    @Specialization
    @TruffleBoundary
    protected String date() {
        return new SimpleDateFormat(RRuntime.SYSTEM_DATE_FORMAT).format(Calendar.getInstance().getTime());
    }
}
