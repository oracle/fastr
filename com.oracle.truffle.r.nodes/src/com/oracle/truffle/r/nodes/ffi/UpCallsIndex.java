/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.ffi;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.r.nodes.ffi.ListAccessNodesFactory.CADDRNodeGen;
import com.oracle.truffle.r.nodes.ffi.ListAccessNodesFactory.CADRNodeGen;
import com.oracle.truffle.r.nodes.ffi.ListAccessNodesFactory.CARNodeGen;
import com.oracle.truffle.r.nodes.ffi.ListAccessNodesFactory.CDDRNodeGen;
import com.oracle.truffle.r.nodes.ffi.ListAccessNodesFactory.CDRNodeGen;
import com.oracle.truffle.r.nodes.ffi.MiscNodesFactory.LENGTHNodeGen;
import com.oracle.truffle.r.runtime.ffi.UpCallsRFFI;

/**
 * Provides indices for indexing into table of {@link RootCallTarget}s for any upcall implemented in
 * Truffle, and a {@link #register} method to associate exactly those calls which are implemented in
 * Truffle. The indices were initially generated automatically from {@link UpCallsRFFI}.
 */
final class UpCallsIndex {
    static final int TABLE_LENGTH = 110;

    static final int CADDR = 0;
    static final int CADR = 1;
    static final int CAR = 2;
    static final int CDDR = 3;
    static final int CDR = 4;
    static final int DUPLICATE_ATTRIB = 5;
    static final int ENCLOS = 6;
    static final int GetRNGstate = 7;
    static final int INTEGER = 8;
    static final int LENGTH = 9;
    static final int LOGICAL = 10;
    static final int NAMED = 11;
    static final int OBJECT = 12;
    static final int PRINTNAME = 13;
    static final int PRVALUE = 14;
    static final int PutRNGstate = 15;
    static final int RAW = 16;
    static final int RDEBUG = 17;
    static final int REAL = 18;
    static final int RSTEP = 19;
    static final int R_BaseEnv = 20;
    static final int R_BaseNamespace = 21;
    static final int R_BindingIsLocked = 22;
    static final int R_CleanUp = 23;
    static final int R_ExternalPtrAddr = 24;
    static final int R_ExternalPtrProt = 25;
    static final int R_ExternalPtrTag = 26;
    static final int R_FindNamespace = 27;
    static final int R_GlobalContext = 28;
    static final int R_GlobalEnv = 29;
    static final int R_HomeDir = 30;
    static final int R_MakeExternalPtr = 31;
    static final int R_NamespaceRegistry = 32;
    static final int R_NewHashedEnv = 33;
    static final int R_ParseVector = 34;
    static final int R_SetExternalPtrAddr = 35;
    static final int R_SetExternalPtrProt = 36;
    static final int R_SetExternalPtrTag = 37;
    static final int R_ToplevelExec = 38;
    static final int R_computeIdentical = 39;
    static final int R_do_MAKE_CLASS = 40;
    static final int R_getContextCall = 41;
    static final int R_getContextEnv = 42;
    static final int R_getContextFun = 43;
    static final int R_getContextSrcRef = 44;
    static final int R_getGlobalFunctionContext = 45;
    static final int R_getParentFunctionContext = 46;
    static final int R_insideBrowser = 47;
    static final int R_isEqual = 48;
    static final int R_isGlobal = 49;
    static final int R_lsInternal3 = 50;
    static final int R_tryEval = 51;
    static final int Rf_GetOption1 = 52;
    static final int Rf_PairToVectorList = 53;
    static final int Rf_ScalarDouble = 54;
    static final int Rf_ScalarInteger = 55;
    static final int Rf_ScalarLogical = 56;
    static final int Rf_ScalarString = 57;
    static final int Rf_allocateArray = 58;
    static final int Rf_allocateMatrix = 59;
    static final int Rf_allocateVector = 60;
    static final int Rf_anyDuplicated = 61;
    static final int Rf_asChar = 62;
    static final int Rf_asInteger = 63;
    static final int Rf_asLogical = 64;
    static final int Rf_asReal = 65;
    static final int Rf_classgets = 66;
    static final int Rf_cons = 67;
    static final int Rf_copyListMatrix = 68;
    static final int Rf_copyMatrix = 69;
    static final int Rf_defineVar = 70;
    static final int Rf_duplicate = 71;
    static final int Rf_error = 72;
    static final int Rf_eval = 73;
    static final int Rf_findVar = 74;
    static final int Rf_findVarInFrame = 75;
    static final int Rf_findVarInFrame3 = 76;
    static final int Rf_findfun = 77;
    static final int Rf_getAttrib = 78;
    static final int Rf_gsetVar = 79;
    static final int Rf_inherits = 80;
    static final int Rf_install = 81;
    static final int Rf_isNull = 82;
    static final int Rf_isString = 83;
    static final int Rf_lengthgets = 84;
    static final int Rf_mkCharLenCE = 85;
    static final int Rf_ncols = 86;
    static final int Rf_nrows = 87;
    static final int Rf_setAttrib = 88;
    static final int Rf_warning = 89;
    static final int Rf_warningcall = 90;
    static final int Rprintf = 91;
    static final int SETCADR = 92;
    static final int SETCAR = 93;
    static final int SETCDR = 94;
    static final int SET_RDEBUG = 95;
    static final int SET_RSTEP = 96;
    static final int SET_STRING_ELT = 97;
    static final int SET_SYMVALUE = 98;
    static final int SET_TAG = 99;
    static final int SET_TYPEOF_FASTR = 100;
    static final int SET_VECTOR_ELT = 101;
    static final int STRING_ELT = 102;
    static final int SYMVALUE = 103;
    static final int TAG = 104;
    static final int TYPEOF = 105;
    static final int VECTOR_ELT = 106;
    static final int R_Interactive = 107;
    static final int isS4Object = 108;
    static final int unif_rand = 109;

    static void register() {
        FFIUpCallRootNode.add(Rf_asReal, AsRealNodeGen::create);
        FFIUpCallRootNode.add(Rf_asLogical, AsLogicalNodeGen::create);
        FFIUpCallRootNode.add(Rf_asInteger, AsIntegerNodeGen::create);
        FFIUpCallRootNode.add(Rf_asChar, AsCharNodeGen::create);
        FFIUpCallRootNode.add(CAR, CARNodeGen::create);
        FFIUpCallRootNode.add(CDR, CDRNodeGen::create);
        FFIUpCallRootNode.add(CADR, CADRNodeGen::create);
        FFIUpCallRootNode.add(CADDR, CADDRNodeGen::create);
        FFIUpCallRootNode.add(CDDR, CDDRNodeGen::create);
        FFIUpCallRootNode.add(LENGTH, LENGTHNodeGen::create);
    }
}
