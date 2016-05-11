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
import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.builtin.CastUtils;
import com.oracle.truffle.r.nodes.builtin.CastUtils.Cast;
import com.oracle.truffle.r.nodes.builtin.CastUtils.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.ScanNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastNode.TypeExpr;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.nodes.RNode;

public class RBuiltinVerifier {

    public static void autoTestColSums(Class<?> builtinNodeClass) throws Exception {
        Class<?> builtinClass = builtinNodeClass.getSuperclass();
        RBuiltin annotation = builtinClass.getAnnotation(RBuiltin.class);
        int argLength = annotation.parameterNames().length;
        String[] parameterNames = annotation.parameterNames();
        parameterNames = Arrays.stream(parameterNames).map(n -> n.isEmpty() ? null : n).toArray(String[]::new);
        ArgumentsSignature signature = ArgumentsSignature.get(parameterNames);

        int total = signature.getLength();
        RNode[] args = new RNode[total];
        for (int i = 0; i < total; i++) {
            args[i] = AccessArgumentNode.create(i);
        }
        Method factoryMethod = builtinNodeClass.getMethod("create", RNode[].class);
        RBuiltinNode builtinNode = (RBuiltinNode) factoryMethod.invoke(null, (Object) args.clone());

        CastNode[] castNodes = builtinNode.getCasts();

        List<TypeExpr> argResultSets = Arrays.asList(castNodes).stream().map(cn -> cn == null ? TypeExpr.ANYTHING : cn.resultTypes()).collect(Collectors.toList());

        List<Method> specMethods = CastUtils.getAnnotatedMethods(builtinClass, Specialization.class);

        HashMap<Method, List<Set<Cast>>> convResultTypePerSpec = new HashMap<>();

        for (Method sm : specMethods) {
            Class<?>[] parTypes = sm.getParameterTypes();

            List<Set<Cast>> convResultTypesForSpec = new ArrayList<>();
            for (int i = 0; i < argLength; i++) {
                TypeExpr argResultSet = argResultSets.get(i);
                Class<?> argType = parTypes[i];
                Set<Cast> convArgResultCasts = CastUtils.Casts.findConvertibleActualType(argResultSet, argType, true);
                convResultTypesForSpec.add(convArgResultCasts);
            }
            convResultTypePerSpec.put(sm, convResultTypesForSpec);
        }

        Set<List<Type>> specPowerSetCombined = new HashSet<>();
        for (Map.Entry<Method, List<Set<Cast>>> entry : convResultTypePerSpec.entrySet()) {
            List<TypeExpr> actualArgTypeSets = entry.getValue().stream().
                            map(argCasts -> Casts.inputsAsTypeExpr(argCasts)).
                            collect(Collectors.toList());
            Set<List<Type>> specPowerSet = CastUtils.argumentPowerSet(actualArgTypeSets);
            specPowerSetCombined.addAll(specPowerSet);
        }

        Set<List<Type>> uncoveredArgsSet = CastUtils.argumentPowerSet(argResultSets);
        uncoveredArgsSet.removeAll(specPowerSetCombined);

        System.out.println("Argument cast pipelines binding:");
        for (int i = 0; i < argLength; i++) {
            TypeExpr argResultSet = argResultSets.get(i);
            System.out.println(" Pipeline for '" + annotation.parameterNames()[i] + "' (arg[" + i + "]):");
            System.out.println("  Result types union:");
            Set<Type> argSetNorm = argResultSet.normalize();
            System.out.println("   " + argSetNorm.stream().map(argType -> typeName(argType)).collect(Collectors.toSet()));
            System.out.println("  Bound result types:");
            Set<Type> unboundArgTypes = new HashSet<>(argSetNorm);
            for (Map.Entry<Method, List<Set<Cast>>> entry : convResultTypePerSpec.entrySet()) {
                Set<Cast> argCastInSpec = entry.getValue().get(i);
                argCastInSpec.stream().forEach(
                                partialCast -> {
                                    System.out.println("   " + partialCast.coverage() + " (" + typeName(partialCast.inputType()) + "->" + typeName(partialCast.resultType()) + ")" + " @ " +
                                                    methodName(entry.getKey()));
                                    unboundArgTypes.remove(partialCast.inputType());
                                });
            }
            System.out.println("  Unbound types:");
            System.out.println("   " + unboundArgTypes.stream().map(argType -> typeName(argType)).collect(Collectors.toSet()));
        }

        System.out.println("\nUnhandled argument combinations: " + uncoveredArgsSet.size());
// for (List<Type> uncoveredArgs : uncoveredArgsSet) {
// System.out.println(uncoveredArgs.stream().map(t -> typeName(t)).collect(Collectors.toList()));
// }
    }

    private static String typeName(Type t) {
        if (t instanceof Class) {
            return ((Class<?>) t).getSimpleName();
        } else {
            return t.getTypeName();
        }
    }

    private static String methodName(Method m) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> pt : m.getParameterTypes()) {
            if (sb.length() == 0) {
                sb.append(typeName(pt));
            } else {
                sb.append(",").append(typeName(pt));
            }
        }
        return typeName(m.getReturnType()) + " " + m.getName() + "(" + sb + ")";
    }

    public static void main(String[] args) throws Exception {
        autoTestColSums(ScanNodeGen.class);
    }

}
