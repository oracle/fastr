/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.context.AltRepContext;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;

@GenerateUncached
public abstract class MakeAltIntegerClassNode extends FFIUpCallNode.Arg3 {
    private static final TruffleLogger altrepLogger = RLogger.getLogger(RLogger.LOGGER_ALTREP);

    public static MakeAltIntegerClassNode create() {
        return MakeAltIntegerClassNodeGen.create();
    }

    @TruffleBoundary
    @Specialization
    public Object makeAltIntegerClass(String className, String packageName, Object dllInfo,
                    @CachedContext(TruffleRLanguage.class) RContext context) {
        AltRepContext altRepCtx = context.altRepContext;
        altrepLogger.fine(() -> "Making new altint class " + packageName + ":" + className);
        return altRepCtx.registerNewAltIntClass(className, packageName, dllInfo);
    }
}
