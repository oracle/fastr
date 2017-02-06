/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.RError.Message.CHAR_ARGUMENT;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

@RBuiltin(name = "setwd", visibility = OFF, kind = INTERNAL, parameterNames = "path", behavior = IO)
public abstract class Setwd extends RBuiltinNode {

    static {
        Casts casts = new Casts(Setwd.class);
        casts.arg("path").defaultError(SHOW_CALLER, CHAR_ARGUMENT).mustBe(stringValue()).asStringVector().mustBe(notEmpty()).findFirst();
    }

    @Specialization
    @TruffleBoundary
    protected Object setwd(String path,
                    @Cached("create()") BaseRFFI.GetwdNode getwdNode,
                    @Cached("create()") BaseRFFI.SetwdNode setwdNode) {
        String owd = getwdNode.execute();
        String nwd = Utils.tildeExpand(path);
        int rc = setwdNode.execute(nwd);
        if (rc != 0) {
            throw RError.error(this, RError.Message.CANNOT_CHANGE_DIRECTORY);
        } else {
            String nwdAbs = getwdNode.execute();
            Utils.updateCurwd(nwdAbs);
            return owd;
        }
    }
}
