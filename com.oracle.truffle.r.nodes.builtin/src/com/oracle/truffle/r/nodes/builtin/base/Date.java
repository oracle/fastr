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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;

@RBuiltin(name = "date", kind = INTERNAL, parameterNames = {}, behavior = IO)
public abstract class Date extends RBuiltinNode {

    @Specialization
    @TruffleBoundary
    protected String date() {
        return new SimpleDateFormat(RRuntime.SYSTEM_DATE_FORMAT).format(Calendar.getInstance().getTime());
    }
}
