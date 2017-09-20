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
package com.oracle.truffle.r.nodes.test;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;

/**
 * This class contains a list of external builtins.
 */
public class ExtBuiltinsList {

    @SuppressWarnings("rawtypes") private static final Class[] builtins = {
                    com.oracle.truffle.r.nodes.builtin.RInternalCodeBuiltinNode.class,
                    com.oracle.truffle.r.nodes.objects.NewObjectNodeGen.class,
                    com.oracle.truffle.r.nodes.objects.GetPrimNameNodeGen.class,
                    com.oracle.truffle.r.library.utils.TypeConvertNodeGen.class,
                    com.oracle.truffle.r.library.utils.RprofNodeGen.class,
                    com.oracle.truffle.r.library.utils.RprofmemNodeGen.class,
                    com.oracle.truffle.r.library.utils.ObjectSizeNodeGen.class,
                    com.oracle.truffle.r.library.utils.MenuNodeGen.class,
                    com.oracle.truffle.r.library.utils.DownloadNodeGen.class,
                    com.oracle.truffle.r.library.utils.Crc64NodeGen.class,
                    com.oracle.truffle.r.library.utils.CountFields.class,
                    com.oracle.truffle.r.library.parallel.ParallelFunctionsFactory.MCIsChildNodeGen.class,
                    com.oracle.truffle.r.nodes.builtin.base.foreign.WriteTableNodeGen.class,
                    com.oracle.truffle.r.nodes.builtin.base.foreign.ReadTableHeadNodeGen.class,
                    com.oracle.truffle.r.nodes.builtin.base.foreign.MakeQuartzDefault.class,
                    com.oracle.truffle.r.nodes.builtin.base.foreign.Flushconsole.class,
                    com.oracle.truffle.r.nodes.builtin.base.foreign.FftNodeGen.class,
                    com.oracle.truffle.r.nodes.builtin.base.foreign.CairoPropsNodeGen.class,
                    com.oracle.truffle.r.library.tools.ToolsTextFactory.DoTabExpandNodeGen.class,
                    com.oracle.truffle.r.library.tools.ToolsTextFactory.CodeFilesAppendNodeGen.class,
                    com.oracle.truffle.r.library.tools.Rmd5NodeGen.class,
                    com.oracle.truffle.r.library.tools.DirChmodNodeGen.class,
                    com.oracle.truffle.r.library.tools.C_ParseRdNodeGen.class,
                    com.oracle.truffle.r.library.stats.WilcoxFreeNode.class,
                    com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function3_2NodeGen.class,
                    com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function4_1NodeGen.class,
                    com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function4_2NodeGen.class,
                    com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function3_1NodeGen.class,
                    com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function2_1NodeGen.class,
                    com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function2_2NodeGen.class,
                    com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.ApproxTestNodeGen.class,
                    com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.ApproxNodeGen.class,
                    com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineCoefNodeGen.class,
                    com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineEvalNodeGen.class,
                    com.oracle.truffle.r.library.stats.SignrankFreeNode.class,
                    com.oracle.truffle.r.library.stats.RMultinomNodeGen.class,
                    com.oracle.truffle.r.library.stats.RandFunctionsNodesFactory.RandFunction3NodeGen.class,
                    com.oracle.truffle.r.library.stats.RandFunctionsNodesFactory.RandFunction2NodeGen.class,
                    com.oracle.truffle.r.library.stats.RandFunctionsNodesFactory.RandFunction1NodeGen.class,
                    com.oracle.truffle.r.library.stats.DoubleCentreNodeGen.class,
                    com.oracle.truffle.r.library.stats.CutreeNodeGen.class,
                    com.oracle.truffle.r.library.stats.CovcorNodeGen.class,
                    com.oracle.truffle.r.library.stats.CompleteCases.class,
                    com.oracle.truffle.r.library.stats.CdistNodeGen.class,
                    com.oracle.truffle.r.library.stats.BinDistNodeGen.class,
                    com.oracle.truffle.r.library.methods.SubstituteDirectNodeGen.class,
                    com.oracle.truffle.r.library.methods.SlotFactory.R_getSlotNodeGen.class,
                    com.oracle.truffle.r.library.methods.SlotFactory.R_setSlotNodeGen.class,
                    com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_initMethodDispatchNodeGen.class,
                    com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_methodsPackageMetaNameNodeGen.class,
                    com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_getClassFromCacheNodeGen.class,
                    com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_set_method_dispatchNodeGen.class,
                    com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_M_setPrimitiveMethodsNodeGen.class,
                    com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_identCNodeGen.class,
                    com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_getGenericNodeGen.class,
                    com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_nextMethodCallNodeGen.class,
                    com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_externalPtrPrototypeObjectNodeGen.class,
                    com.oracle.truffle.r.library.stats.deriv.DerivNodeGen.class,
    };

    @SuppressWarnings("unchecked")
    public static List<Class<? extends RExternalBuiltinNode>> getBuiltins() {
        return Arrays.asList(builtins);
    }
}
