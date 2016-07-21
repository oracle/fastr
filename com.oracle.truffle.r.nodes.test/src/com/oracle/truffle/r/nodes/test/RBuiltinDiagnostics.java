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
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
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
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;

public class RBuiltinDiagnostics {

    static class DiagConfig {
        boolean verbose;
        boolean ignoreRNull;
        boolean ignoreRMissing;
        long maxTotalCombinations = 500L;
    }

    final DiagConfig diagConfig;

    public RBuiltinDiagnostics(DiagConfig diagConfig) {
        this.diagConfig = diagConfig;
    }

    public static void main(String[] args) throws Throwable {
        DiagConfig diagConfig = new DiagConfig();

        diagConfig.verbose = Arrays.stream(args).filter(arg -> "-v".equals(arg)).findFirst().isPresent();
        diagConfig.ignoreRNull = Arrays.stream(args).filter(arg -> "-n".equals(arg)).findFirst().isPresent();
        diagConfig.ignoreRMissing = Arrays.stream(args).filter(arg -> "-m".equals(arg)).findFirst().isPresent();
        List<String> bNames = Arrays.stream(args).filter(arg -> !arg.startsWith("-")).collect(Collectors.toList());

        Predef.setPredefFilters(new PredefFiltersSamplers());
        Predef.setPredefMappers(new PredefMappersSamplers());

        boolean chimneySweeping = Arrays.stream(args).filter(arg -> "--sweep".equals(arg)).findFirst().isPresent();

        RBuiltinDiagnostics rbDiag = chimneySweeping ? new ChimneySweeping.ChimneySweepingSuite(diagConfig) : new RBuiltinDiagnostics(diagConfig);

        if (bNames.isEmpty()) {
            rbDiag.diagnoseAllBuiltins();
        } else {
            for (String bName : bNames) {
                rbDiag.diagnoseSingleBuiltin(bName);
            }
        }
    }

    public SingleBuiltinDiagnostics createBuiltinDiagnostics(RBuiltinFactory bf) {
        return new SingleBuiltinDiagnostics(this, bf);
    }

    public void diagnoseSingleBuiltin(String builtinName) throws Exception {
        BasePackage bp = new BasePackage();
        RBuiltinFactory bf = bp.lookupByName(builtinName);
        if (bf == null) {
            System.out.println("No builtin '" + builtinName + "' found");
            return;
        }

        createBuiltinDiagnostics(bf).diagnoseBuiltin();
    }

    public void diagnoseAllBuiltins() {
        BasePackage bp = new BasePackage();
        for (RBuiltinFactory bf : bp.getBuiltins().values()) {
            try {
                createBuiltinDiagnostics(bf).diagnoseBuiltin();
            } catch (Exception e) {
                System.out.println(bf.getName() + " failed: " + e.getMessage());
            }
        }

        System.out.println("Finished");
        System.out.println("--------");
    }

    static class SingleBuiltinDiagnostics {
        private final RBuiltinDiagnostics diagSuite;
        final RBuiltinFactory builtinFactory;
        final String builtinName;
        final int argLength;
        final String[] parameterNames;
        final CastNode[] castNodes;
        final Class<?> builtinClass;
        final RBuiltin annotation;
        final List<Method> specMethods;
        final List<TypeExpr> argResultSets;
        final HashMap<Method, List<Set<Cast>>> convResultTypePerSpec;
        final Set<List<Type>> nonCoveredArgsSet;

        SingleBuiltinDiagnostics(RBuiltinDiagnostics diagSuite, RBuiltinFactory builtinFactory) {
            this.diagSuite = diagSuite;
            this.builtinFactory = builtinFactory;
            this.builtinName = builtinFactory.getName();

            this.builtinClass = builtinFactory.getBuiltinNodeClass();
            this.annotation = builtinClass.getAnnotation(RBuiltin.class);
            this.argLength = annotation.parameterNames().length;
            String[] pn = annotation.parameterNames();
            this.parameterNames = Arrays.stream(pn).map(n -> n.isEmpty() ? null : n).toArray(String[]::new);

            this.castNodes = getCastNodesFromBuiltin();

            List<TypeExpr> argResultSetsPreliminary = createArgResultSets();
            argResultSets = argResultSetsPreliminary.stream().map(te -> te.filter(t -> {
                return !((diagSuite.diagConfig.ignoreRNull && t == RNull.class) || (diagSuite.diagConfig.ignoreRMissing && t == RMissing.class));
            })).collect(Collectors.toList());

            this.specMethods = CastUtils.getAnnotatedMethods(builtinClass, Specialization.class);

            this.convResultTypePerSpec = createConvResultTypePerSpecialization();
            this.nonCoveredArgsSet = combineArguments();
        }

        private HashMap<Method, List<Set<Cast>>> createConvResultTypePerSpecialization() {
            HashMap<Method, List<Set<Cast>>> convResultTypes = new HashMap<>();

            for (Method sm : specMethods) {
                Class<?>[] parTypes = sm.getParameterTypes();

                List<Set<Cast>> convResultTypesForSpec = new ArrayList<>();
                for (int i = 0; i < argLength; i++) {
                    TypeExpr argResultSet = argResultSets.get(i);
                    Type argType = getParamType(parTypes, i);
                    Set<Cast> convArgResultCasts = CastUtils.Casts.findConvertibleActualType(argResultSet, argType, true);
                    convResultTypesForSpec.add(convArgResultCasts);
                }
                convResultTypes.put(sm, convResultTypesForSpec);
            }
            return convResultTypes;
        }

        private Set<List<Type>> combineArguments() {
            Set<List<Type>> specPowerSetCombined = new HashSet<>();
            for (Map.Entry<Method, List<Set<Cast>>> entry : convResultTypePerSpec.entrySet()) {
                List<TypeExpr> actualArgTypeSets = entry.getValue().stream().map(argCasts -> Casts.inputsAsTypeExpr(argCasts)).collect(Collectors.toList());
                Set<List<Type>> specPowerSet = CastUtils.argumentProductSet(actualArgTypeSets);
                specPowerSetCombined.addAll(specPowerSet);
            }

            Set<List<Type>> nonCovered = CastUtils.argumentProductSet(argResultSets);
            nonCovered.removeAll(specPowerSetCombined);
            return nonCovered;
        }

        public void diagnoseBuiltin() throws Exception {
            System.out.println("****************************************************************************");
            System.out.println("Builtin: " + builtinName + " (" + builtinFactory.getBuiltinNodeClass().getName() + ")");
            System.out.println("****************************************************************************");

            System.out.println("Argument cast pipelines binding:");
            for (int i = 0; i < argLength; i++) {
                diagnosePipeline(i);
            }

            System.out.println("\nUnhandled argument combinations: " + nonCoveredArgsSet.size());
            System.out.println("");

            if (diagSuite.diagConfig.verbose) {
                for (List<Type> uncoveredArgs : nonCoveredArgsSet) {
                    System.out.println(uncoveredArgs.stream().map(t -> typeName(t)).collect(Collectors.toList()));
                }
            }
        }

        protected void diagnosePipeline(int i) {
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

        private CastNode[] getCastNodesFromBuiltin() {
            ArgumentsSignature signature = ArgumentsSignature.get(parameterNames);

            int total = signature.getLength();
            RNode[] args = new RNode[total];
            for (int i = 0; i < total; i++) {
                args[i] = ReadVariableNode.create("dummy");
            }
            RBuiltinNode builtinNode = builtinFactory.getConstructor().apply(args.clone());

            CastNode[] cn = builtinNode.getCasts();
            return cn;
        }

        private List<TypeExpr> createArgResultSets() {
            List<TypeExpr> as = new ArrayList<>();
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
                as.add(te);
            }
            return as;
        }

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
