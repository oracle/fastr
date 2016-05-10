/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "readRenviron", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = "path")
public abstract class ReadREnviron extends RBuiltinNode {

    @Specialization(guards = "lengthOneCVector(vec)")
    @TruffleBoundary
    protected Object doReadEnviron(RAbstractStringVector vec) {
        controlVisibility();
        String path = Utils.tildeExpand(vec.getDataAt(0));
        byte result = RRuntime.LOGICAL_TRUE;
        try {
            RContext.getInstance().stateREnvVars.readEnvironFile(path);
        } catch (FileNotFoundException ex) {
            RError.warning(this, RError.Message.GENERIC, ex.getMessage());
            result = RRuntime.LOGICAL_FALSE;
        } catch (IOException ex) {
            throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
        }
        return result;
    }

    @Fallback
    protected Object doReadEnviron(@SuppressWarnings("unused") Object vec) {
        throw RError.error(this, RError.Message.ARGUMENT_MUST_BE_STRING, "x");
    }

    protected static boolean lengthOneCVector(RAbstractStringVector vec) {
        return vec.getLength() == 1;
    }
}
