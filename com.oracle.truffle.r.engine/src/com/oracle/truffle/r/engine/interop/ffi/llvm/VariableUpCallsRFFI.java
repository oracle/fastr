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
package com.oracle.truffle.r.engine.interop.ffi.llvm;

import com.oracle.truffle.api.interop.TruffleObject;

/**
 * This exists because {@link TruffleObject} instances cannot currently be stored in memory, so
 * upcall to get the values.
 *
 * TODO Some of these, e.g. {@link #R_NilValue}, are performance sensitive, but most are not. So we
 * could collapse those into a single upcall that returned all the values in one go and extract the
 * value with another upcall.
 */
public interface VariableUpCallsRFFI {
    // Checkstyle: stop method name check

    Object R_EmptyEnv();

    Object R_NilValue();

    Object R_UnboundValue();

    Object R_Srcref();

    Object R_MissingArg();

    Object R_BaseSymbol();

    Object R_BraceSymbol();

    Object R_Bracket2Symbol();

    Object R_BracketSymbol();

    Object R_ClassSymbol();

    Object R_DeviceSymbol();

    Object R_DimNamesSymbol();

    Object R_DimSymbol();

    Object R_DollarSymbol();

    Object R_DotsSymbol();

    Object R_DropSymbol();

    Object R_LastvalueSymbol();

    Object R_LevelsSymbol();

    Object R_ModeSymbol();

    Object R_NaRmSymbol();

    Object R_NameSymbol();

    Object R_NamesSymbol();

    Object R_NamespaceEnvSymbol();

    Object R_PackageSymbol();

    Object R_QuoteSymbol();

    Object R_RowNamesSymbol();

    Object R_SeedsSymbol();

    Object R_SourceSymbol();

    Object R_TspSymbol();

    Object R_dot_defined();

    Object R_dot_Method();

    Object R_dot_target();

    Object R_NaString();

    Object R_BlankString();

    Object R_BlankScalarString();

}
