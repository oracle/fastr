/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinFactory;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.BasePackage;
import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.CastUtils;
import com.oracle.truffle.r.nodes.casts.CastUtils.Cast;
import com.oracle.truffle.r.nodes.casts.CastUtils.Casts;
import com.oracle.truffle.r.nodes.casts.Not;
import com.oracle.truffle.r.nodes.casts.PredefFiltersSamplers;
import com.oracle.truffle.r.nodes.casts.PredefMappersSamplers;
import com.oracle.truffle.r.nodes.casts.TypeExpr;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;

public final class RBuiltinDiagnostics {

    public static void main(String[] args) throws Exception {
        boolean verbose = Arrays.stream(args).filter(arg -> "-v".equals(arg)).findFirst().isPresent();
        boolean ignoreRNull = Arrays.stream(args).filter(arg -> "-n".equals(arg)).findFirst().isPresent();
        boolean ignoreRMissing = Arrays.stream(args).filter(arg -> "-m".equals(arg)).findFirst().isPresent();
        List<String> bNames = Arrays.stream(args).filter(arg -> !arg.startsWith("-")).collect(Collectors.toList());

        Predef.setPredefFilters(new PredefFiltersSamplers());
        Predef.setPredefMappers(new PredefMappersSamplers());

        if (bNames.isEmpty()) {
            diagnoseAllBuiltins(verbose, ignoreRNull, ignoreRMissing);
        } else {
            for (String bName : bNames) {
                diagnoseSingleBuiltin(bName, verbose, ignoreRNull, ignoreRMissing);
            }
        }
    }

    private static void diagnoseSingleBuiltin(String builtinName, boolean verbose, boolean ignoreRNull, boolean ignoreRMissing) throws Exception {
        BasePackage bp = new BasePackage();
        RBuiltinFactory bf = bp.lookupByName(builtinName);
        diagnoseBuiltin(bf, verbose, ignoreRNull, ignoreRMissing);
    }

    private static void diagnoseAllBuiltins(boolean verbose, boolean ignoreRNull, boolean ignoreRMissing) {
        BasePackage bp = new BasePackage();
        for (RBuiltinFactory bf : bp.getBuiltins().values()) {
            try {
                diagnoseBuiltin(bf, verbose, ignoreRNull, ignoreRMissing);
            } catch (Exception e) {
                System.out.println(bf.getName() + " failed: " + e.getMessage());
            }
        }

        System.out.println("Finished");
        System.out.println("--------");
    }

    public static void diagnoseBuiltin(RBuiltinFactory builtinFactory, boolean verbose, boolean ignoreRNull, boolean ignoreRMissing) throws Exception {
        System.out.println("****************************************************************************");
        System.out.println("Builtin: " + builtinFactory.getName() + " (" + builtinFactory.getBuiltinNodeClass().getName() + ")");
        System.out.println("****************************************************************************");

        Class<?> builtinClass = builtinFactory.getBuiltinNodeClass();
        RBuiltin annotation = builtinClass.getAnnotation(RBuiltin.class);
        int argLength = annotation.parameterNames().length;
        String[] parameterNames = annotation.parameterNames();
        parameterNames = Arrays.stream(parameterNames).map(n -> n.isEmpty() ? null : n).toArray(String[]::new);

        CastNode[] castNodes = getCastNodesFromBuiltin(builtinFactory, parameterNames);

        List<TypeExpr> argResultSets = createArgResultSets(argLength, parameterNames, castNodes);
        argResultSets = argResultSets.stream().map(te -> te.filter(t -> {
            return !((ignoreRNull && t == RNull.class) || (ignoreRMissing && t == RMissing.class));
        })).collect(Collectors.toList());

        List<Method> specMethods = CastUtils.getAnnotatedMethods(builtinClass, Specialization.class);

        HashMap<Method, List<Set<Cast>>> convResultTypePerSpec = createConvResultTypePerSpecialization(argLength, argResultSets, specMethods);

        Set<List<Type>> nonCoveredArgsSet = combineArguments(argResultSets, convResultTypePerSpec);

        System.out.println("Argument cast pipelines binding:");
        for (int i = 0; i < argLength; i++) {
            TypeExpr argResultSet = argResultSets.get(i);
            System.out.println("\n Pipeline for '" + annotation.parameterNames()[i] + "' (arg[" + i + "]):");
            System.out.println("  Result types union:");
            Set<Type> argSetNorm = argResultSet.normalize();
            System.out.println("   " + argSetNorm.stream().map(argType -> typeName(argType)).collect(Collectors.toSet()));
            System.out.println("  Bound result types:");
            final int curParIndex = i;
            Set<Type> unboundArgTypes = new HashSet<>(argSetNorm);
            for (Map.Entry<Method, List<Set<Cast>>> entry : convResultTypePerSpec.entrySet()) {
                Set<Cast> argCastInSpec = entry.getValue().get(i);
                argCastInSpec.stream().forEach(
                                partialCast -> {
                                    System.out.println("   " + partialCast.coverage() + " (" + typeName(partialCast.inputType()) + "->" + typeName(partialCast.resultType()) + ")" + " in " +
                                                    methodName(entry.getKey(), curParIndex));
                                    unboundArgTypes.remove(partialCast.inputType());
                                });
            }
            System.out.println("  Unbound types:");
            System.out.println("   " + unboundArgTypes.stream().map(argType -> typeName(argType)).collect(Collectors.toSet()));
        }

        System.out.println("\nUnhandled argument combinations: " + nonCoveredArgsSet.size());
        System.out.println("");

        if (verbose) {
            for (List<Type> uncoveredArgs : nonCoveredArgsSet) {
                System.out.println(uncoveredArgs.stream().map(t -> typeName(t)).collect(Collectors.toList()));
            }
        }
    }

    private static Set<List<Type>> combineArguments(List<TypeExpr> argResultSets, HashMap<Method, List<Set<Cast>>> convResultTypePerSpec) {
        Set<List<Type>> specPowerSetCombined = new HashSet<>();
        for (Map.Entry<Method, List<Set<Cast>>> entry : convResultTypePerSpec.entrySet()) {
            List<TypeExpr> actualArgTypeSets = entry.getValue().stream().map(argCasts -> Casts.inputsAsTypeExpr(argCasts)).collect(Collectors.toList());
            Set<List<Type>> specPowerSet = CastUtils.argumentProductSet(actualArgTypeSets);
            specPowerSetCombined.addAll(specPowerSet);
        }

        Set<List<Type>> nonCoveredArgsSet = CastUtils.argumentProductSet(argResultSets);
        nonCoveredArgsSet.removeAll(specPowerSetCombined);
        return nonCoveredArgsSet;
    }

    private static HashMap<Method, List<Set<Cast>>> createConvResultTypePerSpecialization(int argLength, List<TypeExpr> argResultSets, List<Method> specMethods) {
        HashMap<Method, List<Set<Cast>>> convResultTypePerSpec = new HashMap<>();

        for (Method sm : specMethods) {
            Class<?>[] parTypes = sm.getParameterTypes();

            List<Set<Cast>> convResultTypesForSpec = new ArrayList<>();
            for (int i = 0; i < argLength; i++) {
                TypeExpr argResultSet = argResultSets.get(i);
                Type argType = getParamType(parTypes, i);
                Set<Cast> convArgResultCasts = CastUtils.Casts.findConvertibleActualType(argResultSet, argType, true);
                convResultTypesForSpec.add(convArgResultCasts);
            }
            convResultTypePerSpec.put(sm, convResultTypesForSpec);
        }
        return convResultTypePerSpec;
    }

    private static List<TypeExpr> createArgResultSets(int argLength, String[] parameterNames, CastNode[] castNodes) {
        List<TypeExpr> argResultSets = new ArrayList<>();
        for (int i = 0; i < argLength; i++) {
            CastNode cn;
            if (i < castNodes.length) {
                cn = castNodes[i];
            } else {
                cn = null;
            }
            TypeExpr te;
            try {
                te = cn == null ? TypeExpr.ANYTHING : CastNodeSampler.createSampler(cn).resultTypes();
            } catch (Exception e) {
                throw new RuntimeException("Cannot create sampler for argument " + parameterNames[i], e);
            }
            argResultSets.add(te);
        }
        return argResultSets;
    }

    private static CastNode[] getCastNodesFromBuiltin(RBuiltinFactory builtinFactory, String[] parameterNames) {
        ArgumentsSignature signature = ArgumentsSignature.get(parameterNames);

        int total = signature.getLength();
        RNode[] args = new RNode[total];
        for (int i = 0; i < total; i++) {
            args[i] = AccessArgumentNode.create(i);
        }
        RBuiltinNode builtinNode = builtinFactory.getConstructor().apply(args.clone());

        CastNode[] castNodes = builtinNode.getCasts();
        return castNodes;
    }

    private static int getRealParamIndex(Class<?>[] parTypes, int i) {
        return parTypes.length > 0 && Frame.class.isAssignableFrom(parTypes[0]) ? i + 1 : i;
    }

    private static Type getParamType(Class<?>[] parTypes, int i) {
        final int j = getRealParamIndex(parTypes, i);
        Type argType = j < parTypes.length ? parTypes[j] : Not.NOTHING;
        return argType;
    }

    private static String typeName(Type t) {
        if (t instanceof Class) {
            return ((Class<?>) t).getSimpleName();
        } else {
            return t.getTypeName();
        }
    }

    private static String methodName(Method m, int markedParamIndex) {
        final int markedParamRealIndex = getRealParamIndex(m.getParameterTypes(), markedParamIndex);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Class<?> pt : m.getParameterTypes()) {
            final String tn;
            if (i == markedParamRealIndex) {
                tn = "*" + typeName(pt) + "*";
            } else {
                tn = typeName(pt);
            }
            if (sb.length() == 0) {
                sb.append(tn);
            } else {
                sb.append(",").append(tn);
            }
            i++;
        }
        return typeName(m.getReturnType()) + " " + m.getName() + "(" + sb + ")";
    }

}
