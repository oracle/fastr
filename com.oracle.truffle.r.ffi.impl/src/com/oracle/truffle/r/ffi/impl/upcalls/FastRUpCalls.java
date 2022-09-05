/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.upcalls;

import com.oracle.truffle.r.ffi.impl.nodes.FASTR_DATAPTRNode;
import com.oracle.truffle.r.ffi.impl.nodes.FASTR_serializeNode;
import com.oracle.truffle.r.ffi.processor.RFFICpointer;
import com.oracle.truffle.r.ffi.processor.RFFIInject;
import com.oracle.truffle.r.ffi.processor.RFFIUpCallNode;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * Up-calls specific to FastR used in FastR native code. Most of the functions are not exported in
 * any API, except for the global var API ({@code FASTR_GlobalVar*}, which is exported in
 * {@code Rinternals.h}.
 */
public interface FastRUpCalls {
    // Checkstyle: stop method name check

    Object R_MethodsNamespace();

    int FASTR_getConnectionChar(Object obj);

    int FASTR_getSerializeVersion();

    @RFFIUpCallNode(FASTR_serializeNode.class)
    void FASTR_serialize(Object object, int type, int version, @RFFICpointer Object stream, @RFFICpointer Object outBytesFunc);

    Object getSummaryDescription(Object x);

    Object getConnectionClassString(Object x);

    Object getOpenModeString(Object x);

    boolean isSeekable(Object x);

    void restoreHandlerStacks(Object savedHandlerStack, @RFFIInject RContext context);

    /**
     * Implements {@code DATAPTR} for types that do not have specialized API function for accessing
     * the underlying data such as {@link com.oracle.truffle.r.runtime.data.RStringVector}.
     */
    @RFFICpointer
    @RFFIUpCallNode(FASTR_DATAPTRNode.class)
    Object FASTR_DATAPTR(Object x);

    // Global Var API

    /**
     * Allocates a native global variable descriptor. Must be called at most once per DLL load.
     */
    Object FASTR_GlobalVarAlloc(@RFFIInject RContext context);

    /**
     * Initializes the {@code globVarDescr} with the information about the current context. i.e.
     * assign an index into the per-context array of global native variables. Must be called at most
     * once for every context.
     */
    void FASTR_GlobalVarInit(Object globVarDescr, @RFFIInject RContext context);

    /**
     * Same as {@link #FASTR_GlobalVarInit(Object, RContext)}, but also registers given destructor
     * to be called later during the context finalization.
     *
     * @param destructorNativeFunc Native function that will be called before the context finalizes.
     */
    void FASTR_GlobalVarInitWithDtor(Object globVarDescr, @RFFICpointer Object destructorNativeFunc, @RFFIInject RContext context);

    void FASTR_GlobalVarSetSEXP(Object globVarDescr, Object value, @RFFIInject RContext context);

    Object FASTR_GlobalVarGetSEXP(Object globVarDescr, @RFFIInject RContext context);

    void FASTR_GlobalVarSetPtr(Object globVarDescr, @RFFICpointer Object value, @RFFIInject RContext context);

    @RFFICpointer
    Object FASTR_GlobalVarGetPtr(Object globVarDescr, @RFFIInject RContext context);

    void FASTR_GlobalVarSetInt(Object globalVarDescr, int value, @RFFIInject RContext context);

    int FASTR_GlobalVarGetInt(Object globalVarDescr, @RFFIInject RContext context);

    void FASTR_GlobalVarSetDouble(Object globalVarDescr, double value, @RFFIInject RContext context);

    double FASTR_GlobalVarGetDouble(Object globalVarDescr, @RFFIInject RContext context);

    void FASTR_GlobalVarSetBool(Object globalVarDescr, boolean value, @RFFIInject RContext context);

    boolean FASTR_GlobalVarGetBool(Object globalVarDescr, @RFFIInject RContext context);

    /**
     * Prints all descriptors for all contexts. For debugging purposes.
     */
    void FASTR_GlobalVarPrintDescrs(@RFFIInject RContext context);
}
