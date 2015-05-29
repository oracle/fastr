/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.library.tools;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

public abstract class C_ParseRd extends RExternalBuiltinNode.Arg7 {

    @SuppressWarnings("unused")
    @Specialization
    protected Object parseRd(RConnection con, REnvironment srcfile, String encoding, byte verbose, RAbstractStringVector basename, byte fragment, byte warningCalls) {
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Fallback
    public Object parseRd(Object con, Object srcfile, Object encoding, Object verbose, Object basename, Object fragment, Object warningCalls) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }
}
