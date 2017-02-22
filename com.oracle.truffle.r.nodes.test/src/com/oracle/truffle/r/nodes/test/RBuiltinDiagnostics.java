/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinFactory;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.BasePackage;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineBuilder;
import com.oracle.truffle.r.nodes.casts.CastUtils;
import com.oracle.truffle.r.nodes.casts.CastUtils.Cast;
import com.oracle.truffle.r.nodes.casts.CastUtils.Casts;
import com.oracle.truffle.r.nodes.casts.Not;
import com.oracle.truffle.r.nodes.casts.ResultTypesAnalyser;
import com.oracle.truffle.r.nodes.casts.TypeExpr;
import com.oracle.truffle.r.nodes.test.ChimneySweeping.ChimneySweepingSuite;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public class RBuiltinDiagnostics {

    private static final String OUTPUT_MAX_LEVEL_ARG = "--outMaxLev=";

    private static final TypeExpr rmissingType = TypeExpr.atom(RMissing.class);

    static class DiagConfig {
        boolean verbose;
        boolean ignoreRNull;
        boolean ignoreRMissing;
        long maxTotalCombinations = 500L;
        int outputMaxLevel;
    }

    final DiagConfig diagConfig;
    int warningCounter = 0;
    int reportedBuiltinsCounter = 0;

    RBuiltinDiagnostics(DiagConfig diagConfig) {
        this.diagConfig = diagConfig;
    }

    static RBuiltinDiagnostics createRBuiltinDiagnostics(String[] args, boolean batchDiag) {
        return new RBuiltinDiagnostics(initDiagConfig(new DiagConfig(), args, batchDiag));
    }

    static <C extends DiagConfig> C initDiagConfig(C diagConfig, String[] args, boolean batchDiag) {
        diagConfig.verbose = Arrays.stream(args).filter(arg -> "-v".equals(arg)).findFirst().isPresent();
        diagConfig.ignoreRNull = Arrays.stream(args).filter(arg -> "-n".equals(arg)).findFirst().isPresent();
        diagConfig.ignoreRMissing = Arrays.stream(args).filter(arg -> "-m".equals(arg)).findFirst().isPresent();
        diagConfig.outputMaxLevel = Arrays.stream(args).filter(arg -> arg.startsWith(OUTPUT_MAX_LEVEL_ARG)).map(x -> Integer.parseInt(x.split("=")[1])).findFirst().orElse(
                        batchDiag ? 0 : Integer.MAX_VALUE);
        return diagConfig;
    }

    public static void main(String[] args) throws Throwable {
        List<String> bNames = Arrays.stream(args).filter(arg -> !arg.startsWith("-")).collect(Collectors.toList());

        RBuiltinDiagnostics rbDiag = ChimneySweepingSuite.createChimneySweepingSuite(args).orElseGet(() -> createRBuiltinDiagnostics(args, bNames.isEmpty()));
        if (bNames.isEmpty()) {
            rbDiag.diagnoseAllBuiltins();
        } else {
            boolean ok = true;
            for (String bName : bNames) {
                ok &= rbDiag.diagnoseSingleBuiltin(bName);
            }

            System.exit(ok ? 0 : 1);
        }
    }

    public SingleBuiltinDiagnostics createBuiltinDiagnostics(RBuiltinDiagFactory bf) {
        return new SingleBuiltinDiagnostics(this, bf);
    }

    public boolean diagnoseSingleBuiltin(String builtinName) throws Exception {
        SingleBuiltinDiagnostics diag;
        try {
            print(0, "Diagnosing '" + builtinName + "' ...");

            BasePackage bp = new BasePackage();
            RBuiltinFactory bf = bp.lookupByName(builtinName);
            RBuiltinDiagFactory bdf;

            if (bf == null) {
                Class<?> bltnCls = loadBuiltinClass(toReflClassName(builtinName));
                if (RExternalBuiltinNode.class.isAssignableFrom(bltnCls)) {
                    bdf = RExtBuiltinDiagFactory.create(bltnCls);
                } else {
                    bdf = new RIntBuiltinDiagFactory(findBuiltInFactory(bltnCls, bp));
                }
            } else {
                bdf = new RIntBuiltinDiagFactory(bf);
            }

            diag = createBuiltinDiagnostics(bdf);
        } catch (Throwable t) {
            print(0, "Error in initialization of builtin " + builtinName);
            t.printStackTrace();
            return false;
        }

        boolean ok = true;
        try {
            diag.init().diagnoseBuiltin();

            print(1, "Finished");
            print(1, "--------");
        } catch (WarningException e) {
            diag.print(0, "Warning: " + e.getMessage());
        } catch (InfoException e) {
            print(0, e.getMessage());
        } catch (Throwable e) {
            ok = false;
            e.printStackTrace();
        }
        return ok;
    }

    public void diagnoseAllBuiltins() {
        BasePackage bp = new BasePackage();

        List<Class<? extends RExternalBuiltinNode>> extBltn = ExtBuiltinsList.getBuiltins();
        Collection<RBuiltinFactory> intBltn = bp.getBuiltins().values();
        int nBltn = intBltn.size() + extBltn.size();
        System.out.println("Diagnosing " + nBltn + " builtins (" + intBltn.size() + " internal, " + extBltn.size() + " external)");

        boolean ok = true;
        int errCounter = 0;

        for (RBuiltinFactory bf : intBltn) {
            System.out.print(".");
            SingleBuiltinDiagnostics diag;
            try {
                diag = createBuiltinDiagnostics(new RIntBuiltinDiagFactory((bf)));
            } catch (WarningException e) {
                print(0, "Warning: " + e.getMessage());
                continue;
            } catch (InfoException e) {
                print(1, e.getMessage());
                continue;
            } catch (Throwable t) {
                errCounter++;
                print(0, "Error in initialization of builtin " + bf.getName());
                t.printStackTrace();
                continue;
            }
            try {
                diag.init().diagnoseBuiltin();
            } catch (WarningException e) {
                diag.print(0, "Warning: " + e.getMessage());
            } catch (InfoException e) {
                diag.print(1, e.getMessage());
            } catch (Throwable t) {
                errCounter++;
                ok = false;
                diag.print(0, "");
                t.printStackTrace();
            }
        }

        for (Class<? extends RExternalBuiltinNode> extBltCls : extBltn) {
            System.out.print(".");
            SingleBuiltinDiagnostics diag;
            try {
                diag = createBuiltinDiagnostics(RExtBuiltinDiagFactory.create(extBltCls));
            } catch (WarningException e) {
                print(0, "Warning: " + e.getMessage());
                continue;
            } catch (InfoException e) {
                print(1, e.getMessage());
                continue;
            } catch (Throwable t) {
                errCounter++;
                print(0, "Error in initialization of " + extBltCls.getName() + " builtin");
                t.printStackTrace();
                continue;
            }
            try {
                diag.init().diagnoseBuiltin();
            } catch (WarningException e) {
                diag.print(0, "Warning: " + e.getMessage());
            } catch (InfoException e) {
                diag.print(1, e.getMessage());
            } catch (Throwable t) {
                errCounter++;
                ok = false;
                diag.print(0, "");
                t.printStackTrace();
            }
        }

        print(0, "\n\nFinished:");
        print(0, " Total builtins: " + nBltn);
        print(0, " Dubious builtins: " + reportedBuiltinsCounter);
        print(0, " Clean builtins: " + (nBltn - reportedBuiltinsCounter));
        print(0, " Errors: " + errCounter);
        print(0, " Warnings: " + warningCounter);

        System.exit(ok ? 0 : 1);
    }

    private static RBuiltinFactory findBuiltInFactory(Class<?> bltnCls, BasePackage bp) {
        Optional<RBuiltinFactory> bltnFact = bp.getBuiltins().values().stream().filter(bf -> bf.getBuiltinNodeClass().isAssignableFrom(bltnCls)).findFirst();
        if (bltnFact.isPresent()) {
            return bltnFact.get();
        } else {
            throw new IllegalArgumentException("No builtin found for class " + bltnCls.getName());
        }
    }

    public static Class<?> loadBuiltinClass(String builtinClsName) throws ClassNotFoundException {
        Class<?> nodeClass = Class.forName(builtinClsName);
        if (!Modifier.isFinal(nodeClass.getModifiers())) {
            nodeClass = toNodeGenClass(nodeClass);
            if (!Modifier.isFinal(nodeClass.getModifiers())) {
                throw new IllegalArgumentException("Invalid external builtin class name: " + builtinClsName);
            }
        }

        return nodeClass;
    }

    private static String toReflClassName(String qualified) {
        String[] split = qualified.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            sb.append(s);
            if (i < split.length - 1) {
                if (Character.isUpperCase(s.charAt(0))) {
                    sb.append("$");
                } else {
                    sb.append(".");
                }
            }
        }
        return sb.toString();
    }

    private static String toGenNodeName(String name) {
        if (name.endsWith("Node")) {
            return name + "Gen";
        } else {
            return name + "NodeGen";
        }
    }

    private static Class<?> toNodeGenClass(Class<?> nodeCls) throws ClassNotFoundException {
        String nodeGenClsName;
        if (nodeCls.getEnclosingClass() == null) {
            nodeGenClsName = toGenNodeName(nodeCls.getName());
        } else {
            String enclClsName = nodeCls.getEnclosingClass().getName();
            String enclosingClsSuffix = RBaseNode.class.isAssignableFrom(nodeCls.getEnclosingClass()) ? (enclClsName.endsWith("Node") ? "Gen" : "NodeGen") : "Factory";
            String[] split = nodeCls.getName().split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                if (i == split.length - 1) {
                    String[] lastSplit = s.split("\\$");
                    sb.append(lastSplit[0] + enclosingClsSuffix + "$");
                    sb.append(toGenNodeName(lastSplit[1]));
                } else {
                    sb.append(s);
                }
                if (i < split.length - 1) {
                    sb.append(".");
                }
            }
            nodeGenClsName = sb.toString();
        }

        return Class.forName(nodeGenClsName);
    }

    protected void print(int level, Object x) {
        String msg = "" + x;
        if (msg.startsWith("Warning:")) {
            warningCounter++;
        }

        if (level <= diagConfig.outputMaxLevel) {
            System.out.println(msg);
        }
    }

    static class SingleBuiltinDiagnostics {
        private final RBuiltinDiagnostics diagSuite;
        final RBuiltinDiagFactory builtinFactory;
        final String builtinName;
        final int argLength;
        final String[] parameterNames;

        NodeWithArgumentCasts.Casts casts;
        List<Method> specMethods;
        List<TypeExpr> argResultSets;
        HashMap<Method, List<Set<Cast>>> convResultTypePerSpec;
        Set<List<Type>> nonCoveredArgsSet;

        SingleBuiltinDiagnostics(RBuiltinDiagnostics diagSuite, RBuiltinDiagFactory builtinFactory) {
            this.diagSuite = diagSuite;
            this.builtinFactory = builtinFactory;
            this.builtinName = builtinFactory.getBuiltinName();
            String[] pn = builtinFactory.getParameterNames();
            this.argLength = pn.length;
            this.parameterNames = Arrays.stream(pn).map(n -> n == null || n.isEmpty() ? null : n).toArray(String[]::new);
        }

        SingleBuiltinDiagnostics init() throws Throwable {
            String builtinClassName = builtinFactory.getBuiltinNodeClass().getName();
            // causes the invocation of the static initializer in the builtin node class
            Class<?> bltnCls = NodeWithArgumentCasts.Casts.getBuiltinClass(Class.forName(builtinClassName));

            try {
                this.casts = builtinFactory.getCasts();
            } catch (RInternalError e) {
                // It will be converted into an error after all builtins are fixed
                throw new WarningException("Builtin " + builtinClassName + " should declare argument casts or use Casts.noCasts(" + bltnCls.getSimpleName() + ".class)");
            }

            if (this.casts == null || this.casts.declaresNoCasts()) {
                throw new InfoException("Builtin " + builtinClassName + " has no-casts");
            }

            argResultSets = createArgResultSets();

            List<Method> specs = CastUtils.getAnnotatedMethods(builtinFactory.getBuiltinNodeClass(), Specialization.class);
            this.specMethods = new ArrayList<>(specs);
            // N.B. The fallback method cannot be found by the Fallback annotation since
            // this annotation has the CLASS retention policy. Nonetheless, the fallback method can
            // be determined throught the fallback node in the generated class.
            Optional<Method> fallback = findFallbackMethod(toNodeGenClass(bltnCls));
            if (fallback.isPresent()) {
                this.specMethods.add(fallback.get());
            }

            this.convResultTypePerSpec = createConvResultTypePerSpecialization();
            this.nonCoveredArgsSet = combineArguments();

            return this;
        }

        private boolean headerPrinted;

        protected void print(int level, Object x) {
            printBuiltinHeader(level);
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
            print(1, "Argument cast pipelines binding:");
            for (int i = 0; i < argLength; i++) {
                diagnosePipeline(i);
            }

            print(1, "\nUnhandled argument combinations: " + nonCoveredArgsSet.size());
            print(1, "");

            printDeadSpecs();

            if (diagSuite.diagConfig.verbose) {
                for (List<Type> uncoveredArgs : nonCoveredArgsSet) {
                    print(1, uncoveredArgs.stream().map(t -> typeName(t)).collect(Collectors.toList()));
                }
            }
        }

        private void printBuiltinHeader(int level) {
            if (!headerPrinted && level <= diagSuite.diagConfig.outputMaxLevel) {
                diagSuite.print(level, "\n");
                diagSuite.print(level, "****************************************************************************");
                diagSuite.print(level, "Builtin: " + builtinName + " (" + builtinFactory.getBuiltinNodeClass().getCanonicalName() + ")");
                diagSuite.print(level, "****************************************************************************");
                headerPrinted = true;
                diagSuite.reportedBuiltinsCounter++;
            }
        }

        private void printDeadSpecs() {
            StringBuilder sb = new StringBuilder();
            int deadSpecCnt = 0;
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
                    sb.append("   " + methodName(resTpPerSpec.getKey(), missingCasts) + "\n");
                    deadSpecCnt++;
                }
            }

            int logLev = deadSpecCnt == 0 ? 1 : 0;
            String msg = deadSpecCnt == 0 ? "Dead specializations: " + deadSpecCnt : "Warning: Dead specializations: " + deadSpecCnt;
            print(logLev, msg);
            print(logLev, sb.toString());
        }

        protected void diagnosePipeline(int i) {
            TypeExpr argResultSet = argResultSets.get(i);

            String pipelineHeader = "Pipeline for '" + parameterNames[i] + "' (arg[" + i + "])";

            StringBuilder sb = new StringBuilder();
            sb.append("  Result types union:").append('\n');
            Set<Type> argSetNorm = argResultSet.toNormalizedConjunctionSet();
            sb.append("   " + argSetNorm.stream().map(argType -> typeName(argType)).collect(Collectors.toSet())).append('\n');
            sb.append("  Bound result types:").append('\n');
            final int curParIndex = i;
            Set<Type> unboundArgTypes = new HashSet<>(argSetNorm);
            for (Map.Entry<Method, List<Set<Cast>>> entry : convResultTypePerSpec.entrySet()) {
                Set<Cast> argCastInSpec = entry.getValue().get(i);
                argCastInSpec.stream().forEach(
                                partialCast -> {
                                    sb.append("   " + partialCast.coverage() + " (" + typeName(partialCast.inputType()) + "->" + typeName(partialCast.resultType()) + ")" + " in " +
                                                    methodName(entry.getKey(), Collections.singleton(curParIndex))).append('\n');
                                    unboundArgTypes.remove(partialCast.inputType());
                                });
            }
            if (unboundArgTypes.isEmpty()) {
                print(1, pipelineHeader);
            } else {
                print(0, "Warning: " + pipelineHeader);
                print(0, "   Unbound types: " + unboundArgTypes.stream().map(argType -> typeName(argType)).collect(Collectors.toSet()));
            }
            print(1, sb.toString());

        }

        private List<TypeExpr> createArgResultSets() {
            Object[] defParams = null;
            try {
                defParams = builtinFactory.getDefaultParameterValues();
            } catch (Throwable t) {
                print(0, "Warning: Cannot obtain default parameter values. Cause: " + t.getMessage());
                if (diagSuite.diagConfig.outputMaxLevel > 0) {
                    t.printStackTrace();
                }
            }

            if (defParams != null && defParams.length > 0 && defParams.length < argLength) {
                throw new RInternalError("Builtin " + builtinName + " provides invalid default parameter values");
            }

            List<TypeExpr> as = new ArrayList<>();
            PipelineBuilder[] plBuilders = casts.getPipelineBuilders();
            for (int i = 0; i < argLength; i++) {

                PipelineBuilder plBuilder;
                if (i < plBuilders.length) {
                    plBuilder = plBuilders[i];
                } else {
                    plBuilder = null;
                }
                TypeExpr te;
                try {
                    if (plBuilder == null) {
                        te = TypeExpr.ANYTHING;
                    } else {
                        te = ResultTypesAnalyser.analyse(plBuilder.getFirstStep()).removeWildcards();

                        if (!te.and(rmissingType).isNothing()) {
                            // try to find a replacement for RMissing
                            if ((defParams != null && defParams.length > i) && defParams[i] != RMissing.instance) {
                                // Cancel RMissing in the result type if there is a
                                // substitution for it
                                te = te.and(rmissingType.not());
                            }
                        }

                    }
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

    private static Optional<Method> findFallbackMethod(Class<?> genBltnClass) {
        Optional<Class<?>> fallbackNodeCls = Arrays.stream(genBltnClass.getDeclaredClasses()).filter(c -> "FallbackNode_".equals(c.getSimpleName())).findFirst();
        return fallbackNodeCls.flatMap(fc -> findFallbackMethodFromAnnot(fc, genBltnClass.getSuperclass()));
    }

    private static Optional<Method> findFallbackMethodFromAnnot(Class<?> fallbackNodeClass, Class<?> bltnCls) {
        GeneratedBy genByAnnot = fallbackNodeClass.getAnnotation(GeneratedBy.class);
        assert genByAnnot != null;
        String fallbackName = genByAnnot.methodName();

        return findMethod(bltnCls, dm -> {
            return dm.getAnnotation(Specialization.class) == null && fallbackName.startsWith(dm.getName() + "(");
        });
    }

    private static Optional<Method> findMethod(Class<?> clazz, Predicate<Method> filter) {
        Optional<Method> res = Arrays.asList(clazz.getDeclaredMethods()).stream().filter(filter).findFirst();
        if (res.isPresent()) {
            return res;
        }

        if (clazz.getSuperclass() != Object.class) {
            return findMethod(clazz.getSuperclass(), filter);
        } else {
            return Optional.empty();
        }
    }

    public interface RBuiltinDiagFactory {
        String getBuiltinName();

        Class<?> getBuiltinNodeClass();

        String[] getParameterNames();

        Object[] getDefaultParameterValues();

        CastNode[] getCastNodes();

        NodeWithArgumentCasts.Casts getCasts();
    }

    public static final class RIntBuiltinDiagFactory implements RBuiltinDiagFactory {

        private final RBuiltinFactory fact;

        public RIntBuiltinDiagFactory(RBuiltinFactory fact) {
            super();

            if (!RBuiltinNode.class.isAssignableFrom(fact.getBuiltinNodeClass())) {
                throw new InfoException("A 'fake' builtin");
            }

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
        public CastNode[] getCastNodes() {
            return fact.getConstructor().get().getCasts();
        }

        @Override
        public NodeWithArgumentCasts.Casts getCasts() {
            return NodeWithArgumentCasts.Casts.getCasts(fact.getBuiltinNodeClass());
        }

        @Override
        public Object[] getDefaultParameterValues() {
            switch (getBuiltinKind()) {
                case SUBSTITUTE:
                case PRIMITIVE:
                    return fact.getConstructor().get().getDefaultParameterValues();
                default:
                    return null;
            }
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
        public static RExtBuiltinDiagFactory create(Class<?> nodeClass) {

            if (!RExternalBuiltinNode.class.isAssignableFrom(nodeClass)) {
                throw new IllegalArgumentException(nodeClass.getName() + " is not a subclass of " + RExternalBuiltinNode.class.getName());
            }

            Optional<Method> execMethod = Arrays.stream(nodeClass.getMethods()).filter(
                            m -> m.getName().equals("execute") && Arrays.stream(m.getParameterTypes()).allMatch(t -> t == Object.class)).findFirst();
            if (execMethod.isPresent()) {
                return new RExtBuiltinDiagFactory((Class<RExternalBuiltinNode>) nodeClass, execMethod.get().getParameterCount());
            } else {
                throw new InfoException("no-args builtin '" + nodeClass.getName());
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
        public CastNode[] getCastNodes() {
            try {
                return ((RExternalBuiltinNode) nodeClass.getMethod("create").invoke(null)).getCasts();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public NodeWithArgumentCasts.Casts getCasts() {
            return NodeWithArgumentCasts.Casts.getCasts(getBuiltinNodeClass());
        }

        @Override
        public Object[] getDefaultParameterValues() {
            return null;
        }
    }

    @SuppressWarnings("serial")
    private static final class InfoException extends RuntimeException {
        InfoException(String msg) {
            super(msg);
        }
    }

    @SuppressWarnings("serial")
    private static final class WarningException extends RuntimeException {
        WarningException(String msg) {
            super(msg);
        }
    }
}
