/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RForeignAccessFactory;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class RForeignAccessFactoryImpl implements RForeignAccessFactory {

    private static final class TableEntry {
        private final Class<? extends RTruffleObject> clazz;
        private final ForeignAccess foreignAccess;
        /**
         * {@link PolyglotEngine} checks the thread on a {@link ForeignAccess}.
         */
        private final Thread thread;

        private TableEntry(Class<? extends RTruffleObject> clazz, ForeignAccess foreignAccess) {
            this.clazz = clazz;
            this.thread = Thread.currentThread();
            this.foreignAccess = foreignAccess;
        }
    }

    TableEntry[] table = new TableEntry[32];
    int tableIndex;

    @Override
    public ForeignAccess getForeignAccess(RTruffleObject obj) {
        return get(obj);
    }

    private synchronized ForeignAccess get(RTruffleObject obj) {
        Class<? extends RTruffleObject> objclazz = obj.getClass();
        Thread thread = Thread.currentThread();
        for (int i = 0; i < tableIndex; i++) {
            TableEntry te = table[i];
            if (te.clazz == objclazz && te.thread == thread) {
                return te.foreignAccess;
            }
        }
        return createForeignAccess(objclazz);
    }

    @TruffleBoundary
    private ForeignAccess createForeignAccess(Class<? extends RTruffleObject> clazz) {
        ForeignAccess foreignAccess = null;
        String name = clazz.getSimpleName();
        switch (name) {
            case "RList":
                foreignAccess = RListMRForeign.createAccess();
                break;
            case "RFunction":
                foreignAccess = RFunctionMRForeign.createAccess();
                break;
            default:
                if (RAbstractVector.class.isAssignableFrom(clazz)) {
                    foreignAccess = ForeignAccess.create(RAbstractVector.class, new RAbstractVectorAccessFactory());
                } else {
                    throw RInternalError.unimplemented("foreignAccess: " + name);
                }

        }
        TableEntry te = new TableEntry(clazz, foreignAccess);
        table[tableIndex++] = te;
        return te.foreignAccess;

    }

    @Override
    public Class<? extends TruffleLanguage<RContext>> getTruffleLanguage() {
        return TruffleRLanguage.class;
    }
}
