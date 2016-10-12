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
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinFactory;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.BasePackage;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineConfig;
import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.CastUtils;
import com.oracle.truffle.r.nodes.casts.CastUtils.Cast;
import com.oracle.truffle.r.nodes.casts.CastUtils.Casts;
import com.oracle.truffle.r.nodes.casts.FilterSamplerFactory;
import com.oracle.truffle.r.nodes.casts.MapperSamplerFactory;
import com.oracle.truffle.r.nodes.casts.Not;
import com.oracle.truffle.r.nodes.casts.TypeExpr;
import com.oracle.truffle.r.nodes.test.ChimneySweeping.ChimneySweepingSuite;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

public class RBuiltinDiagnostics {

    private static final String OUTPUT_MAX_LEVEL_ARG = "--outMaxLev=";

    static class DiagConfig {
        boolean verbose;
        boolean ignoreRNull;
        boolean ignoreRMissing;
        long maxTotalCombinations = 500L;
        int outputMaxLevel;
    }

    static {
        PipelineConfig.setFilterFactory(FilterSamplerFactory.INSTANCE);
        PipelineConfig.setMapperFactory(MapperSamplerFactory.INSTANCE);
    }

    private final DiagConfig diagConfig;

    RBuiltinDiagnostics(DiagConfig diagConfig) {
        this.diagConfig = diagConfig;
    }

    static RBuiltinDiagnostics createRBuiltinDiagnostics(String[] args) {
        return new RBuiltinDiagnostics(initDiagConfig(new DiagConfig(), args));
    }

    static <C extends DiagConfig> C initDiagConfig(C diagConfig, String[] args) {
        diagConfig.verbose = Arrays.stream(args).filter(arg -> "-v".equals(arg)).findFirst().isPresent();
        diagConfig.ignoreRNull = Arrays.stream(args).filter(arg -> "-n".equals(arg)).findFirst().isPresent();
        diagConfig.ignoreRMissing = Arrays.stream(args).filter(arg -> "-m".equals(arg)).findFirst().isPresent();
        diagConfig.outputMaxLevel = Arrays.stream(args).filter(arg -> arg.startsWith(OUTPUT_MAX_LEVEL_ARG)).map(x -> Integer.parseInt(x.split("=")[1])).findFirst().orElse(Integer.MAX_VALUE);
        return diagConfig;
    }

    public static void main(String[] args) throws Throwable {
        RBuiltinDiagnostics rbDiag = ChimneySweepingSuite.createChimneySweepingSuite(args).orElseGet(() -> createRBuiltinDiagnostics(args));

        List<String> bNames = Arrays.stream(args).filter(arg -> !arg.startsWith("-")).collect(Collectors.toList());
        if (bNames.isEmpty()) {
            rbDiag.diagnoseAllBuiltins();
        } else {
            for (String bName : bNames) {
                rbDiag.diagnoseSingleBuiltin(bName);
            }
        }
    }

    public SingleBuiltinDiagnostics createBuiltinDiagnostics(RBuiltinDiagFactory bf) {
        return new SingleBuiltinDiagnostics(this, bf);
    }

    public void diagnoseSingleBuiltin(String builtinName) throws Exception {
        BasePackage bp = new BasePackage();
        RBuiltinFactory bf = bp.lookupByName(builtinName);
        RBuiltinDiagFactory bdf;
        if (bf == null) {
            try {
                bdf = RExtBuiltinDiagFactory.create(builtinName);
            } catch (Exception e) {
                print(0, "No builtin '" + builtinName + "' found");
                return;
            }
        } else {
            bdf = new RIntBuiltinDiagFactory(bf);
        }

        createBuiltinDiagnostics(bdf).diagnoseBuiltin();

        print(0, "Finished");
        print(0, "--------");

        System.exit(0);
    }

    public void diagnoseAllBuiltins() {
        BasePackage bp = new BasePackage();
        for (RBuiltinFactory bf : bp.getBuiltins().values()) {
            try {
                createBuiltinDiagnostics(new RIntBuiltinDiagFactory((bf))).diagnoseBuiltin();
            } catch (Exception e) {
                e.printStackTrace();
                print(0, bf.getName() + " failed: " + e.getMessage());
            }
        }

        print(0, "Finished");
        print(0, "--------");

        System.exit(0);
    }

    protected void print(int level, Object x) {
        if (level <= diagConfig.outputMaxLevel) {
            System.out.println(x);
        }
    }

    static class SingleBuiltinDiagnostics {
        private final RBuiltinDiagnostics diagSuite;
        final RBuiltinDiagFactory builtinFactory;
        final String builtinName;
        final int argLength;
        final String[] parameterNames;
        final CastNode[] castNodes;
        final List<Method> specMethods;
        final List<TypeExpr> argResultSets;
        final HashMap<Method, List<Set<Cast>>> convResultTypePerSpec;
        final Set<List<Type>> nonCoveredArgsSet;

        SingleBuiltinDiagnostics(RBuiltinDiagnostics diagSuite, RBuiltinDiagFactory builtinFactory) {
            this.diagSuite = diagSuite;
            this.builtinFactory = builtinFactory;
            this.builtinName = builtinFactory.getBuiltinName();

            String[] pn = builtinFactory.getParameterNames();
            this.argLength = pn.length;
            this.parameterNames = Arrays.stream(pn).map(n -> n == null || n.isEmpty() ? null : n).toArray(String[]::new);

            this.castNodes = getCastNodesFromBuiltin();

            List<TypeExpr> argResultSetsPreliminary = createArgResultSets();
            argResultSets = argResultSetsPreliminary.stream().map(te -> te.filter(t -> {
                return !((diagSuite.diagConfig.ignoreRNull && t == RNull.class) || (diagSuite.diagConfig.ignoreRMissing && t == RMissing.class));
            })).collect(Collectors.toList());

            this.specMethods = CastUtils.getAnnotatedMethods(builtinFactory.getBuiltinNodeClass(), Specialization.class);

            this.convResultTypePerSpec = createConvResultTypePerSpecialization();
            this.nonCoveredArgsSet = combineArguments();
        }

        protected void print(int level, Object x) {
            diagSuite.print(level, x);
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
            print(0, "****************************************************************************");
            print(0, "Builtin: " + builtinName + " (" + builtinFactory.getBuiltinNodeClass().getName() + ")");
            print(0, "****************************************************************************");

            print(0, "Argument cast pipelines binding:");
            for (int i = 0; i < argLength; i++) {
                diagnosePipeline(i);
            }

            print(0, "\nUnhandled argument combinations: " + nonCoveredArgsSet.size());
            print(0, "");

            printDeadSpecs();

            if (diagSuite.diagConfig.verbose) {
                for (List<Type> uncoveredArgs : nonCoveredArgsSet) {
                    print(0, uncoveredArgs.stream().map(t -> typeName(t)).collect(Collectors.toList()));
                }
            }
        }

        private void printDeadSpecs() {
            print(0, "Dead specializations: ");
            for (Map.Entry<Method, List<Set<Cast>>> resTpPerSpec : convResultTypePerSpec.entrySet()) {
                List<Set<Cast>> argsCasts = resTpPerSpec.getValue();
                List<Integer> missingCasts = new ArrayList<>();
                for (int i = 0; i < argsCasts.size(); i++) {
                    Set<Cast> argCasts = argsCasts.get(i);
                    if (argCasts.isEmpty()) {
                        missingCasts.add(i);
                    }
                }

                if (!missingCasts.isEmpty()) {
                    print(0, "   " + methodName(resTpPerSpec.getKey(), missingCasts));
                }
            }

            print(0, "");
        }

        protected void diagnosePipeline(int i) {
            TypeExpr argResultSet = argResultSets.get(i);
            print(0, "\n Pipeline for '" + parameterNames[i] + "' (arg[" + i + "]):");
            print(0, "  Result types union:");
            Set<Type> argSetNorm = argResultSet.normalize();
            print(0, "   " + argSetNorm.stream().map(argType -> typeName(argType)).collect(Collectors.toSet()));
            print(0, "  Bound result types:");
            final int curParIndex = i;
            Set<Type> unboundArgTypes = new HashSet<>(argSetNorm);
            for (Map.Entry<Method, List<Set<Cast>>> entry : convResultTypePerSpec.entrySet()) {
                Set<Cast> argCastInSpec = entry.getValue().get(i);
                argCastInSpec.stream().forEach(
                                partialCast -> {
                                    print(0, "   " + partialCast.coverage() + " (" + typeName(partialCast.inputType()) + "->" + typeName(partialCast.resultType()) + ")" + " in " +
                                                    methodName(entry.getKey(), Collections.singleton(curParIndex)));
                                    unboundArgTypes.remove(partialCast.inputType());
                                });
            }
            print(0, "  Unbound types:");
            print(0, "   " + unboundArgTypes.stream().map(argType -> typeName(argType)).collect(Collectors.toSet()));

        }

        private CastNode[] getCastNodesFromBuiltin() {
            return builtinFactory.getCasts();
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

    private static String methodName(Method m, Collection<Integer> markedParamIndices) {
        final Set<Integer> markedParamRealIndices = markedParamIndices.stream().map(markedParamIndex -> getRealParamIndex(m.getParameterTypes(), markedParamIndex)).collect(Collectors.toSet());
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Class<?> pt : m.getParameterTypes()) {
            final String tn;
            if (markedParamRealIndices.contains(i)) {
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

    public interface RBuiltinDiagFactory {
        String getBuiltinName();

        Class<?> getBuiltinNodeClass();

        String[] getParameterNames();

        CastNode[] getCasts();

    }

    public static final class RIntBuiltinDiagFactory implements RBuiltinDiagFactory {

        private final RBuiltinFactory fact;

        public RIntBuiltinDiagFactory(RBuiltinFactory fact) {
            super();
            this.fact = fact;
        }

        @Override
        public String getBuiltinName() {
            return fact.getName();
        }

        @Override
        public Class<?> getBuiltinNodeClass() {
            return fact.getBuiltinNodeClass();
        }

        public RBuiltinKind getBuiltinKind() {
            return fact.getKind();
        }

        @Override
        public String[] getParameterNames() {
            RBuiltin annotation = fact.getBuiltinNodeClass().getAnnotation(RBuiltin.class);
            String[] pn = annotation.parameterNames();
            return Arrays.stream(pn).map(n -> n.isEmpty() ? null : n).toArray(String[]::new);
        }

        @Override
        public CastNode[] getCasts() {
            return fact.getConstructor().get().getCasts();
        }
    }

    public static final class RExtBuiltinDiagFactory implements RBuiltinDiagFactory {

        private final Class<? extends RExternalBuiltinNode> nodeClass;
        private final String[] parameterNames;

        RExtBuiltinDiagFactory(Class<? extends RExternalBuiltinNode> nodeClass, int arity) {
            this.nodeClass = nodeClass;
            this.parameterNames = new String[arity];
            for (int i = 0; i < arity; i++) {
                this.parameterNames[i] = "arg" + i;
            }
        }

        @SuppressWarnings("unchecked")
        public static RExtBuiltinDiagFactory create(String extBuiltinClsName) throws ClassNotFoundException {
            Class<?> nodeClass = Class.forName(extBuiltinClsName);

            if (!Modifier.isFinal(nodeClass.getModifiers())) {
                nodeClass = Class.forName(extBuiltinClsName + "NodeGen");
                if (!Modifier.isFinal(nodeClass.getModifiers())) {
                    throw new IllegalArgumentException("Invalid external builtin class name: " + extBuiltinClsName);
                }
            }

            if (!RExternalBuiltinNode.class.isAssignableFrom(nodeClass)) {
                throw new IllegalArgumentException(extBuiltinClsName + " is not a subclass of " + RExternalBuiltinNode.class.getName());
            }

            Optional<Method> execMethod = Arrays.stream(nodeClass.getMethods()).filter(
                            m -> m.getName().equals("execute") && Arrays.stream(m.getParameterTypes()).allMatch(t -> t == Object.class)).findFirst();
            if (execMethod.isPresent()) {
                return new RExtBuiltinDiagFactory((Class<RExternalBuiltinNode>) nodeClass, execMethod.get().getParameterCount());
            } else {
                throw new UnsupportedOperationException(extBuiltinClsName + " is not a supported external builtin class");
            }
        }

        @Override
        public String getBuiltinName() {
            return nodeClass.getSimpleName();
        }

        @Override
        public Class<?> getBuiltinNodeClass() {
            return nodeClass;
        }

        @Override
        public String[] getParameterNames() {
            return parameterNames;
        }

        @Override
        public CastNode[] getCasts() {
            try {
                return ((RExternalBuiltinNode) nodeClass.getMethod("create").invoke(null)).getCasts();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
