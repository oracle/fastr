/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;
import com.oracle.truffle.r.runtime.ffi.DLL.NativeSymbolType;
import com.oracle.truffle.r.runtime.ffi.DLL.RegisteredNativeSymbol;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;

/**
 * Fake registration of a RFunction as if it was a native function callable by .C, .Call, .External,
 * .Fortran.
 */
@RBuiltin(name = ".fastr.register.functions", kind = PRIMITIVE, parameterNames = {"library", "env", "nstOrd", "functions"}, behavior = COMPLEX)
public abstract class FastRRegisterFunctions extends RBuiltinNode.Arg4 {

    static {
        NodeWithArgumentCasts.Casts.noCasts(FastRRegisterFunctions.class);
    }

    /**
     * @param library library under which to register the given function
     * @param env the environment to use
     * @param nstOrd see {@link NativeSymbolType} , .C - 0, .Call - 1, .Fortran - 2, .External 3
     * @param functions named list of RFunction-s
     */
    @Specialization
    protected Object register(String library, REnvironment env, int nstOrd, RList functions) {
        try {
            RStringVector names = functions.getNames();

            DLLInfo dllInfo = DLL.findLibrary(library);
            if (dllInfo == null) {
                dllInfo = DLL.createSyntheticLib(RContext.getInstance(), library);
            }

            DotSymbol[] symbols = new DotSymbol[names.getLength()];
            SymbolHandle[] symbolHandles = new SymbolHandle[names.getLength()];

            for (int i = 0; i < names.getLength(); i++) {
                assert functions.getDataAt(i) instanceof RFunction : " only RFunction elements are allowed in the functions list: " + functions.getDataAt(i);
                RFunction fun = (RFunction) functions.getDataAt(i);
                String name = names.getDataAt(i);
                assert !name.isEmpty() : "each element in  functions list has to be named";

                symbolHandles[i] = new SymbolHandle(fun);
                symbols[i] = new DotSymbol(name, symbolHandles[i], 0);
            }

            NativeSymbolType nst = NativeSymbolType.values()[nstOrd];
            DotSymbol[] oldSymbols = dllInfo.getNativeSymbols(nst);
            DotSymbol[] newSymbols;
            if (oldSymbols == null) {
                newSymbols = symbols;
            } else {
                newSymbols = new DotSymbol[oldSymbols.length + symbols.length];
                System.arraycopy(oldSymbols, 0, newSymbols, 0, oldSymbols.length);
                System.arraycopy(symbols, 0, newSymbols, oldSymbols.length, symbols.length);
            }
            dllInfo.setNativeSymbols(nstOrd, newSymbols);

            assign(symbols, dllInfo, symbolHandles, nst, env);
        } catch (REnvironment.PutException ex) {
            throw error(ex);
        }
        return RNull.instance;
    }

    @TruffleBoundary
    private static void assign(DotSymbol[] symbols, DLLInfo dllInfo, SymbolHandle[] symbolHandles, NativeSymbolType nst, REnvironment env) throws PutException {
        for (int i = 0; i < symbols.length; i++) {
            SymbolInfo si = new SymbolInfo(dllInfo, symbols[i].name, symbolHandles[i]);
            RList symbolObject = si.createRSymbolObject(new RegisteredNativeSymbol(nst, symbols[i], dllInfo), true);
            env.put(symbols[i].name, symbolObject);
        }
    }

    @Specialization
    protected Object register(String library, REnvironment env, double nstOrd, RList functions) {
        return register(library, env, (int) nstOrd, functions);
    }

}
