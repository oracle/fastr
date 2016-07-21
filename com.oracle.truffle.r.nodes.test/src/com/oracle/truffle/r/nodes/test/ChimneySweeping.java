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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;
import com.oracle.truffle.r.nodes.builtin.RBuiltinFactory;
import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.nodes.test.RBuiltinDiagnostics.SingleBuiltinDiagnostics;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.generate.FastRSession;
import com.oracle.truffle.r.test.generate.GnuROneShotRSession;
import com.oracle.truffle.r.test.generate.TestOutputManager;

class ChimneySweeping extends SingleBuiltinDiagnostics {

    static class ChimneySweepingSuite extends RBuiltinDiagnostics {

        final FastRSession fastRSession;
        final GnuROneShotRSession gnuRSession;
        final TestOutputManager outputManager;

        ChimneySweepingSuite(RBuiltinDiagnostics.DiagConfig diagConfig) throws IOException {
            super(diagConfig);

            gnuRSession = new GnuROneShotRSession();

            fastRSession = FastRSession.create();
            outputManager = loadTestOutputManager();
        }

        @Override
        public SingleBuiltinDiagnostics createBuiltinDiagnostics(RBuiltinFactory bf) {
            return new ChimneySweeping(this, bf);
        }

        private static TestOutputManager loadTestOutputManager() throws IOException {
            TestOutputManager om;

            URL expectedTestOutputURL = ResourceHandlerFactory.getHandler().getResource(TestBase.class, TestOutputManager.TEST_EXPECTED_OUTPUT_FILE);
            if (expectedTestOutputURL == null) {
                throw new IOException("cannot find " + TestOutputManager.TEST_EXPECTED_OUTPUT_FILE + " resource");
            } else {
                om = new TestOutputManager(new File(expectedTestOutputURL.getPath()));
                om.readTestOutputFile();
            }

            return om;
        }
    }

    private final List<Samples<?>> argSamples;
    private final ChimneySweepingSuite diagSuite;

    private final Set<List<String>> printedOutputPairs = new HashSet<>();
    private final Set<String> printedErrors = new HashSet<>();
    private int sweepCounter = 0;

    ChimneySweeping(ChimneySweepingSuite diagSuite, RBuiltinFactory builtinFactory) {
        super(diagSuite, builtinFactory);
        this.diagSuite = diagSuite;
        this.argSamples = createSamples();
    }

    @Override
    public void diagnoseBuiltin() throws Exception {
        super.diagnoseBuiltin();

        sweepChimney();
    }

    @Override
    protected void diagnosePipeline(int i) {
        super.diagnosePipeline(i);

        System.out.println(" Samples:");
        System.out.println(argSamples.get(i));

        checkPipelines(i);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Samples<?>> createSamples() {
        DefaultArgsExtractor defArgExt = new DefaultArgsExtractor(diagSuite.fastRSession);
        Map<String, Samples<?>> defaultArgs = defArgExt.extractDefaultArgs(builtinName);

        List<Samples<?>> as = new ArrayList<>();
        for (int i = 0; i < argLength; i++) {
            CastNode cn;
            if (i < castNodes.length) {
                cn = castNodes[i];
            } else {
                cn = null;
            }
            Samples samples;
            try {
                samples = cn == null ? Samples.anything() : CastNodeSampler.createSampler(cn).collectSamples();
            } catch (Exception e) {
                throw new RuntimeException("Error in sample generation from argument " + i, e);
            }

            Samples defArgSamples = defaultArgs.get(parameterNames[i]);
            samples = defArgSamples == null ? samples : samples.and(defArgSamples);

            as.add(samples);

        }

        return as;
    }

    /**
     * Checks whether the argument samples are correct by passing them to the argument's pipeline.
     * The positive samples should pass without any error, while the negative ones should cause an
     * error.
     *
     * @param i
     */
    private void checkPipelines(int i) {
        CastNode cn;
        if (i < castNodes.length) {
            cn = castNodes[i];
        } else {
            cn = null;
        }
        if (cn != null) {
            Samples<?> samples = argSamples.get(i);
            if (samples.positiveSamples().isEmpty() && samples.negativeSamples().isEmpty()) {
                System.out.println("No samples");
            } else {
                testPipeline(cn, samples);
                System.out.println("Pipeline check OK (" + samples.positiveSamples().size() + "," + samples.negativeSamples().size() + ")");
            }
        }
    }

    private static void testPipeline(CastNode cn, Samples<?> samples) {
        NodeHandle<CastNode> argCastNodeHandle = TestUtilities.createHandle(cn, (node, args) -> {
            return node.execute(args[0]);
        });

        for (Object sample : samples.positiveSamples()) {
            try {
                argCastNodeHandle.call(sample);
            } catch (UnsupportedSpecializationException e) {
                System.out.println("Warning: No specialization to handle arg " + sample + " : " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                fail("Unexpectedly negative sample: " + sample);
            }
        }
        for (Object sample : samples.negativeSamples()) {
            try {
                argCastNodeHandle.call(sample);
                fail("Unexpectedly positive sample: " + sample);
            } catch (IllegalArgumentException e) {
                Assert.assertTrue(true);
                // ok
            } catch (RError e) {
                Assert.assertTrue(true);
                // ok
            }
        }
    }

    private void sweepChimney() throws IOException {
        Set<RList> validArgsList = extractValidArgsForBuiltin();
        sweepChimney(validArgsList);
    }

    private Set<RList> extractValidArgsForBuiltin() {
        final PolyglotEngine vm = diagSuite.fastRSession.createTestContext(null);

        try {
            Set<String> validArgs = diagSuite.outputManager.getTestMaps().entrySet().stream().filter(
                            e -> e.getKey().startsWith("com.oracle.truffle.r.test.builtins.TestBuiltin_" + builtinName)).flatMap(
                                            e -> e.getValue().keySet().stream()).filter(a -> a.contains(".Internal(" + builtinName)).map(ChimneySweeping::cutOffInternal).filter(
                                                            a -> a != null).collect(
                                                                            Collectors.toSet());
            Set<RList> args = validArgs.stream().map(a -> evalValidArgs(a, vm)).filter(a -> a != null).collect(Collectors.toSet());
            return args;
        } finally {
            vm.dispose();
        }
    }

    private static RList evalValidArgs(String argsExpr, PolyglotEngine vm) {
        try {
            Value eval = vm.eval(RSource.fromTextInternal(argsExpr, RSource.Internal.UNIT_TEST));
            RList args = (RList) eval.get();
            return args;
        } catch (IOException e) {
            // throw new RuntimeException(e);
            // todo: warning
            return null;
        }
    }

    private void sweepChimney(Set<RList> validArgsList) throws IOException {
        System.out.println("++++++++++++++++++++++");
        System.out.println("+  Chimney-sweeping  +");
        System.out.println("++++++++++++++++++++++");
        System.out.println();

        long totalCombinations = calculateNumOfSampleCombinations(argSamples);
        boolean useDiagonalGen = totalCombinations > diagSuite.diagConfig.maxTotalCombinations;

        List<List<Object>> generatedCombinations = generateSampleArgCombinations(argSamples, useDiagonalGen);

        System.out.print("Sweeping builtin '" + builtinName + "' by " + generatedCombinations.size() + " derived argument combinations per each of " + validArgsList.size() +
                        " valid argument samples, ");
        System.out.println("i.e. the total number of sweeps is " + generatedCombinations.size() * validArgsList.size() + ".");
        if (useDiagonalGen) {
            System.out.println("Using diagonal generation (total generation would generate " + totalCombinations + " combinations per valid argument sample)");
        } else {
            System.out.println("Using total generation yields " + totalCombinations + " combinations");
        }

        System.out.println();
        System.out.println();
        System.out.println("Press Enter to continue ...");
        System.in.read();

        evalArgsWithSampleCombinations(validArgsList, generatedCombinations);
    }

    private void evalBuiltin(RList validArgs, List<List<Object>> argSampleCombinations) {
        List<List<Object>> mergedSampleAndValidArgs = mergeValidAndSampleArgs(validArgs, argSampleCombinations);

        for (List<Object> evalArgs : mergedSampleAndValidArgs) {
            evalBuiltin(evalArgs);
        }

    }

    private void evalArgsWithSampleCombinations(Set<RList> validArgsList, List<List<Object>> argSampleCombinations) {
        validArgsList.forEach(validArgs -> evalBuiltin(validArgs, argSampleCombinations));
    }

    private void evalBuiltin(List<Object> args) {
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i < args.size(); i++) {
                Object validArg = args.get(i);
                String deparsedValidArg;
                try {
                    deparsedValidArg = RDeparse.deparse(validArg);
                } catch (Throwable e) {
                    throw new RuntimeException("ERROR: Cannot deparse " + validArg + ": " + e.getMessage(), e);
                }

                if (sb.length() != 0) {
                    sb.append(",");
                }
                sb.append(deparsedValidArg);
            }

            String call = ".Internal(" + builtinName + "(" + sb + "))";
            String output;
            try {
                output = diagSuite.fastRSession.eval(call, null, false);
            } catch (Throwable t) {
                output = "ERROR: " + t.getMessage();
            }
            String outputGnu = diagSuite.gnuRSession.eval(call, null, false);

            List<String> outputPair = Arrays.asList(output, outputGnu);

            if (outputGnu.equals(output)) {
                System.out.print('.');
            } else if (!printedOutputPairs.contains(outputPair)) {
                System.out.println("\n#" + sweepCounter + "> " + call);
                System.out.println("\n====== FastR output ======");
                System.out.println(output);

                System.out.println("====== GnuR output ======");
                System.out.println(outputGnu);
                System.out.println("==========================");

                printedOutputPairs.add(outputPair);
            } else {
                System.out.print('!');
            }

        } catch (Throwable e) {
            // throw new RuntimeException(e);
            // e.printStackTrace();
            if (!printedErrors.contains(e.getMessage())) {
                String call = ".Internal(" + builtinName + "(" + sb + "))";
                System.out.println("\n[" + sweepCounter + "]> " + call);
                System.out.println("ERROR: " + e.getMessage());
                printedErrors.add(e.getMessage());
            }
        } finally {
            sweepCounter++;
        }
    }

    static long calculateNumOfSampleCombinations(List<Samples<?>> argSamples) {
        Long total = argSamples.stream().reduce(1L, (num, samples) -> num * (samples.allSamples().size() + 1), (n1, n2) -> n1 * n2);
        return total;
    }

    static List<List<Object>> generateSampleArgCombinations(List<Samples<?>> argSamples, boolean diagonalMethod) {
        if (argSamples.isEmpty()) {
            return Collections.emptyList();
        } else if (argSamples.size() == 1) {
            Samples<?> as = argSamples.get(0);
            List<Object> samples = new ArrayList<>(as.allSamples());
            samples.add(0, null);
            return samples.stream().map(a -> Collections.singletonList(a)).collect(Collectors.toList());
        } else {
            List<Samples<?>> subList = argSamples.subList(1, argSamples.size());
            List<List<Object>> subArgs = generateSampleArgCombinations(subList, diagonalMethod);

            Samples<?> as = argSamples.get(0);
            List<Object> samples = new ArrayList<>(as.allSamples());
            samples.add(0, null);
            return samples.stream().flatMap(a -> {
                if (a == null || !diagonalMethod) {
                    return subArgs.stream().map(sa -> {
                        List<Object> saExt = new ArrayList<>();
                        saExt.add(a);
                        saExt.addAll(sa);
                        return saExt;
                    });
                } else {
                    List<Object> saExt = new ArrayList<>();
                    saExt.add(a);
                    saExt.addAll(subArgs.get(0)); // the null vector
                    return Collections.singletonList(saExt).stream();
                }
            }).collect(Collectors.toList());
        }

    }

    static List<List<Object>> mergeValidAndSampleArgs(RList validArgs, List<List<Object>> sampleArgsList) {
        return sampleArgsList.stream().map(sampleArgs -> {
            List<Object> newSampleArgs = new ArrayList<>();
            for (int i = 0; i < validArgs.getLength(); i++) {
                if (sampleArgs.get(i) == null) {
                    newSampleArgs.add(validArgs.getDataAt(i));
                } else {
                    newSampleArgs.add(sampleArgs.get(i));
                }
            }
            return newSampleArgs;
        }).collect(Collectors.toList());
    }

    private static String cutOffInternal(String a) {
        int i = a.indexOf(".Internal(");
        if (i >= 0) {
            return a.substring(0, i);
        } else {
            return null;
        }
    }
}
