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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RForeignAccessFactory;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;

/**
 * A {@link ForeignAccess} instance captures the {@link Thread} that creates it and all uses are
 * checked against the current thread. Therefore, in a world with multiple {@link PolyglotEngine}s,
 * aka multiple {@link Thread} and {@link RContext} instances, it is not possible to use a simple
 * global constant value for the {@link ForeignAccess} instance that could be associated directly
 * with the {@link TruffleObject} class.
 *
 * This factory provides a generic solution for all FastR types (all of which are
 * {@link TruffleObject}s), at some cost in performance.
 *
 * The REALLY bad news is that we cannot use {@link RContext} to store the state because, although
 * that should be possible, at the time the call to {@link #getForeignAccess(RTruffleObject)}
 * happens the {@link RContext} may not have been associated with a thread, so
 * {@link RContext#getInstance()} will fail. In short the mapping has to be established using
 * {@link Thread} not {@link RContext} and there is no call that informs us ahead of a call to
 * {@link #getForeignAccess} that a new thread is in play. We use a Truffle {@link Assumption} to
 * provide a fast path in the normal, single-threaded, case.
 *
 * For most types we use the {@link MessageResolution} facility to automatically generate the
 * factory for creating the {@link ForeignAccess} instance. The exceptions are the (many) subclasses
 * of {@link RAbstractVector} as these have the same handling but the generator cannot handle
 * abstract classes.
 *
 */
public final class RForeignAccessFactoryImpl implements RForeignAccessFactory {
    /**
     * The subset of the full set of types that are supported for foreign access. N.B. This list
     * includes types that are not expected to travel between high-level languages, but may travel
     * into the native world (when implemented in Truffle).
     */
    private static final Class<?>[] FOREIGN_CLASSES = new Class<?>[]{
                    RRaw.class, RComplex.class, RIntSequence.class,
                    RDoubleSequence.class, RIntVector.class, RDoubleVector.class,
                    RRawVector.class, RComplexVector.class, RStringVector.class, RLogicalVector.class,
                    RFunction.class, RNull.class, REnvironment.class,
                    RList.class, RSymbol.class,
                    RPairList.class, RExternalPtr.class,
                    DLLInfo.class, DotSymbol.class};

    private static final class ForeignAccessState {

        private static final class TableEntry {
            private final Class<? extends RTruffleObject> clazz; // for sanity check
            private final ForeignAccess foreignAccess;

            private TableEntry(Class<? extends RTruffleObject> clazz, ForeignAccess foreignAccess) {
                this.clazz = clazz;
                this.foreignAccess = foreignAccess;
            }
        }

        /**
         * Table with a unique index for each class in {@link #FOREIGN_CLASSES}.
         */
        private static Class<?>[] classTable;
        /**
         * Isomorphic to {@link #classTable} but contains the {@link ForeignAccess} instance.
         */
        private final TableEntry[] table;
        /**
         * Mask to efficiently compute the table index.
         */
        @CompilationFinal private static int tableMask;
        /**
         * The thread that this state is generated for.
         */
        private final Thread thread;

        private ForeignAccessState() {
            this.thread = Thread.currentThread();
            table = new TableEntry[classTable.length];
            for (int i = 0; i < table.length; i++) {
                @SuppressWarnings("unchecked")
                Class<? extends RTruffleObject> checkedClass = (Class<? extends RTruffleObject>) classTable[i];
                if (checkedClass != null) {
                    table[i] = new TableEntry(checkedClass, createForeignAccess(checkedClass));
                }
            }
        }

        private ForeignAccess get(RTruffleObject obj) {
            Class<? extends RTruffleObject> clazz = obj.getClass();
            return get(clazz);
        }

        private ForeignAccess get(Class<? extends RTruffleObject> clazz) {
            int index = System.identityHashCode(clazz) & tableMask;
            assert table[index].clazz == clazz;
            return table[index].foreignAccess;
        }

        static {
            generatePrototypeTable();
        }

        /**
         * Create a table that has a unique index for every member of {@link #FOREIGN_CLASSES} for
         * efficient access. Since {@link System#identityHashCode(Object)} can vary from run to run,
         * the size of the table may vary, but is typically in the range 64-4096.
         */
        private static void generatePrototypeTable() {
            int ts = 32;
            while (true) {
                classTable = new Class<?>[ts];
                boolean collision = false;
                for (int i = 0; i < FOREIGN_CLASSES.length; i++) {
                    Class<?> clazz = FOREIGN_CLASSES[i];
                    int h = System.identityHashCode(clazz);
                    int hc = h % ts;
                    if (classTable[hc] == null) {
                        classTable[hc] = clazz;
                    } else {
                        collision = true;
                        break;
                    }
                }
                if (!collision) {
                    break;
                } else {
                    ts = ts * 2;
                }
            }
            tableMask = ts - 1;
        }

        @TruffleBoundary
        private static ForeignAccess createForeignAccess(Class<? extends RTruffleObject> clazz) {
            ForeignAccess foreignAccess = null;
            String name = clazz.getSimpleName();
            if (RNull.class.isAssignableFrom(clazz)) {
                foreignAccess = RNullMRForeign.createAccess();
            } else if (RList.class.isAssignableFrom(clazz)) {
                foreignAccess = RListMRForeign.createAccess();
            } else if (REnvironment.class.isAssignableFrom(clazz)) {
                foreignAccess = REnvironmentMRForeign.createAccess();
            } else if (RPairList.class.isAssignableFrom(clazz)) {
                foreignAccess = RPairListMRForeign.createAccess();
            } else if (RFunction.class.isAssignableFrom(clazz)) {
                foreignAccess = RFunctionMRForeign.createAccess();
            } else if (DLL.DLLInfo.class.isAssignableFrom(clazz)) {
                foreignAccess = DLLInfoMRForeign.createAccess();
            } else if (DLL.DotSymbol.class.isAssignableFrom(clazz)) {
                foreignAccess = DLLDotSymbolMRForeign.createAccess();
            } else if (RSymbol.class.isAssignableFrom(clazz)) {
                foreignAccess = RSymbolMRForeign.createAccess();
            } else if (RExternalPtr.class.isAssignableFrom(clazz)) {
                foreignAccess = RExternalPtrMRForeign.createAccess();
            } else {
                if (RAbstractVector.class.isAssignableFrom(clazz)) {
                    foreignAccess = ForeignAccess.create(RAbstractVector.class, new RAbstractVectorAccessFactory());
                } else {
                    throw RInternalError.unimplemented("foreignAccess: " + name);
                }
            }
            return foreignAccess;
        }

    }

    /**
     * In normal execution, there is only one thread.
     */
    private static final Assumption singleStateAssumption = Truffle.getRuntime().createAssumption("single ForeignAccessState");
    /**
     * In case of multiple threads, the per-thread state.
     */
    private static final ThreadLocal<ForeignAccessState> threadLocalState = new ThreadLocal<>();
    /**
     * In single thread mode, a fast path to the state. In multi-thread mode set to {@code null}.
     */
    @CompilationFinal private static ForeignAccessState singleForeignAccessState;

    @Override
    public ForeignAccess getForeignAccess(RTruffleObject obj) {
        ForeignAccessState foreignAccessState;
        if (singleStateAssumption.isValid()) {
            foreignAccessState = singleForeignAccessState;
            if (foreignAccessState == null) {
                // very first call
                foreignAccessState = new ForeignAccessState();
                singleForeignAccessState = foreignAccessState;
                threadLocalState.set(foreignAccessState);
            } else {
                // check thread
                if (Thread.currentThread() != foreignAccessState.thread) {
                    singleStateAssumption.invalidate();
                    singleForeignAccessState = null;
                    foreignAccessState = new ForeignAccessState();
                }
            }
        } else {
            // use the threadLocal
            foreignAccessState = threadLocalState.get();
        }
        ForeignAccess result = foreignAccessState.get(obj);
        return result;
    }

    @Override
    public Class<? extends TruffleLanguage<RContext>> getTruffleLanguage() {
        return TruffleRLanguage.class;
    }

}
