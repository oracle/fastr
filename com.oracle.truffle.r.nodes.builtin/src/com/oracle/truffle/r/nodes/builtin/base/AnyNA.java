/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_SUMMARY;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.ops.na.NAOrNaNCheck;

@ImportStatic(DSLConfig.class)
@RBuiltin(name = "anyNA", kind = PRIMITIVE, parameterNames = {"x", "recursive"}, dispatch = INTERNAL_GENERIC, behavior = PURE_SUMMARY)
public abstract class AnyNA extends RBuiltinNode.Arg2 {

    // true if this is the first recursive level
    protected final boolean isRecursive;

    protected AnyNA() {
        this.isRecursive = false;
    }

    protected AnyNA(boolean isRecursive) {
        this.isRecursive = isRecursive;
    }

    public abstract byte execute(Object value, boolean recursive);

    static {
        Casts casts = new Casts(AnyNA.class);
        casts.arg("recursive").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RRuntime.LOGICAL_FALSE};
    }

    @Specialization
    protected byte isNA(byte value, @SuppressWarnings("unused") boolean recursive) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(int value, @SuppressWarnings("unused") boolean recursive) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(double value, @SuppressWarnings("unused") boolean recursive) {
        return RRuntime.asLogical(RRuntime.isNAorNaN(value));
    }

    @Specialization
    protected byte isNA(RComplex value, @SuppressWarnings("unused") boolean recursive) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(String value, @SuppressWarnings("unused") boolean recursive) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    @SuppressWarnings("unused")
    protected byte isNA(RRaw value, boolean recursive) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isNA(@SuppressWarnings("unused") RNull value, @SuppressWarnings("unused") boolean recursive) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(limit = "getVectorAccessCacheSize()")
    protected byte anyNACached(RAbstractAtomicVector x, @SuppressWarnings("unused") boolean recursive,
                    @Cached NAOrNaNCheck nanCheck,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        Object xData = x.getData();
        switch (xDataLib.getType(xData)) {
            case Logical:
            case Integer:
            case Character:
                // shortcut when we know there's no NAs
                if (!xDataLib.isComplete(xData)) {
                    SeqIterator iter = xDataLib.iterator(xData);
                    while (xDataLib.next(xData, iter)) {
                        if (xDataLib.isNextNA(xData, iter, NACheck.getEnabled())) {
                            return RRuntime.LOGICAL_TRUE;
                        }
                    }
                }
                break;
            case Raw:
                return RRuntime.LOGICAL_FALSE;
            case Double:
                SeqIterator iterDouble = xDataLib.iterator(xData);
                while (xDataLib.next(xData, iterDouble)) {
                    if (nanCheck.checkNAorNaN(xDataLib.getNextDouble(xData, iterDouble))) {
                        return RRuntime.LOGICAL_TRUE;
                    }
                }
                break;
            case Complex:
                SeqIterator iterCmplx = xDataLib.iterator(xData);
                while (xDataLib.next(xData, iterCmplx)) {
                    RComplex val = xDataLib.getNextComplex(xData, iterCmplx);
                    if (nanCheck.checkNAorNaN(val.getRealPart()) || nanCheck.checkNAorNaN(val.getImaginaryPart())) {
                        return RRuntime.LOGICAL_TRUE;
                    }
                }
                break;
        }
        return RRuntime.LOGICAL_FALSE;
    }

    protected AnyNA createRecursive() {
        return AnyNANodeGen.create(true);
    }

    @Specialization(guards = {"isRecursive", "recursive == cachedRecursive"})
    protected byte isNARecursive(RList list, boolean recursive,
                    @Cached("recursive") boolean cachedRecursive,
                    @Cached("createClassProfile()") ValueProfile elementProfile,
                    @Cached("create()") RLengthNode length) {
        if (cachedRecursive) {
            for (int i = 0; i < list.getLength(); i++) {
                Object value = elementProfile.profile(list.getDataAt(i));
                if (length.executeInteger(value) > 0) {
                    if (recursive(recursive, value) == RRuntime.LOGICAL_TRUE) {
                        return RRuntime.LOGICAL_TRUE;
                    }
                }
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @TruffleBoundary
    private byte recursive(boolean recursive, Object value) {
        return execute(value, recursive);
    }

    @Specialization(guards = {"!isRecursive", "recursive == cachedRecursive"})
    protected byte isNA(RList list, boolean recursive,
                    @Cached("recursive") boolean cachedRecursive,
                    @Cached("createRecursive()") AnyNA recursiveNode,
                    @Cached("createClassProfile()") ValueProfile elementProfile,
                    @Cached("create()") RLengthNode length) {
        for (int i = 0; i < list.getLength(); i++) {
            Object value = elementProfile.profile(list.getDataAt(i));
            if (cachedRecursive || length.executeInteger(value) == 1) {
                if (recursiveNode.execute(value, recursive) == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }
}
