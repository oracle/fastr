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
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.junit.Assert;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;
import com.oracle.truffle.r.nodes.builtin.RBuiltinFactory;
import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.nodes.test.RBuiltinDiagnostics.DiagConfig;
import com.oracle.truffle.r.nodes.test.RBuiltinDiagnostics.SingleBuiltinDiagnostics;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.generate.FastRSession;
import com.oracle.truffle.r.test.generate.GnuROneShotRSession;
import com.oracle.truffle.r.test.generate.TestOutputManager;
import com.oracle.truffle.r.test.generate.TestOutputManager.TestInfo;

class ChimneySweeping extends SingleBuiltinDiagnostics {

    private static final String TEST_PREFIX = "com.oracle.truffle.r.test.builtins.TestBuiltin_";
    private static final String SWEEP_MODE_ARG = "--sweep";
    private static final String SWEEP_MODE_ARG_SPEC = SWEEP_MODE_ARG + "-";

    enum ChimneySweepingMode {
        auto,
        total,
        lite;

        static Optional<ChimneySweepingMode> fromArg(String arg) {
            if (SWEEP_MODE_ARG.equals(arg)) {
                return Optional.of(auto);
            } else if (arg.startsWith(SWEEP_MODE_ARG_SPEC)) {
                return Optional.of(valueOf(arg.substring(SWEEP_MODE_ARG_SPEC.length())));
            } else {
                return Optional.empty();
            }
        }
    }

    static class ChimneySweepingConfig extends DiagConfig {
        ChimneySweepingMode sweepingMode;
    }

    static class ChimneySweepingSuite extends RBuiltinDiagnostics {

        final ChimneySweepingConfig diagConfig;
        final FastRSession fastRSession;
        final GnuROneShotRSession gnuRSession;
        final TestOutputManager outputManager;

        ChimneySweepingSuite(ChimneySweepingConfig config) throws IOException {
            super(config);
            this.diagConfig = config;

            System.out.println("Loading GnuR ...");
            gnuRSession = new GnuROneShotRSession();

            System.out.println("Loading FastR ...");
            fastRSession = FastRSession.create();

            System.out.println("Loading test outputs ...");
            outputManager = loadTestOutputManager();
        }

        static Optional<RBuiltinDiagnostics> createChimneySweepingSuite(String[] args) throws IOException {
            if (getSweepMode(args).isPresent()) {
                ChimneySweepingConfig config = new ChimneySweepingConfig();
                return Optional.of(new ChimneySweepingSuite(initChimneySweepingConfig(config, args)));
            } else {
                return Optional.empty();
            }
        }

        static <C extends ChimneySweepingConfig> C initChimneySweepingConfig(C config, String[] args) {
            config.sweepingMode = getSweepMode(args).flatMap(ChimneySweepingMode::fromArg).orElse(ChimneySweepingMode.auto);
            return RBuiltinDiagnostics.initDiagConfig(config, args);
        }

        private static Optional<String> getSweepMode(String[] args) {
            return Arrays.stream(args).filter(arg -> arg.startsWith(SWEEP_MODE_ARG)).findFirst();
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
    private final Set<RList> validArgsList;

    private final Set<List<String>> printedOutputPairs = new HashSet<>();
    private final Set<String> printedErrors = new HashSet<>();
    private int sweepCounter = 0;

    ChimneySweeping(ChimneySweepingSuite diagSuite, RBuiltinFactory builtinFactory) {
        super(diagSuite, builtinFactory);
        this.diagSuite = diagSuite;
        this.validArgsList = extractValidArgsForBuiltin();
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
                if (cn == null) {
                    samples = Samples.anything();
                } else {
                    CastNodeSampler<CastNode> sampler = CastNodeSampler.createSampler(cn);
                    samples = sampler.collectSamples();
                }
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

    private Set<RList> extractValidArgsForBuiltin() {
        final PolyglotEngine vm = diagSuite.fastRSession.createTestContext(null);

        try {
            String snippetAnchor;
            switch (annotation.kind()) {
                case INTERNAL:
                    snippetAnchor = ".Internal(" + builtinName + "(";
                    break;
                default:
                    snippetAnchor = builtinName + "(";
                    break;
            }

            String builtinNameSimple = builtinName.replace(".", "");
            Map<String, SortedMap<String, TestInfo>> snippets = diagSuite.outputManager.getTestMaps().entrySet().stream().filter(
                            e -> e.getKey().startsWith(TEST_PREFIX + builtinName) || e.getKey().startsWith(TEST_PREFIX + builtinNameSimple)).collect(
                                            Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            Set<String> flatSnippets = snippets.entrySet().stream().flatMap(
                            e -> e.getValue().keySet().stream()).collect(Collectors.toSet());
            Set<String> filteredSnippets = flatSnippets.stream().filter(a -> a.contains(snippetAnchor)).collect(Collectors.toSet());
            Set<String> validArgs = filteredSnippets.stream().map(a -> cutOffInvocation(a, snippetAnchor)).filter(
                            a -> a != null && !"".equals(a)).collect(Collectors.toSet());
            Set<RList> args = validArgs.stream().map(a -> evalValidArgs(a, vm)).filter(a -> a != null).collect(Collectors.toSet());

            if (args.isEmpty()) {
                Object[] nullArgs = new Object[this.argLength];
                Arrays.fill(nullArgs, RNull.instance);
                args = Collections.singleton(RDataFactory.createList(nullArgs));
                System.out.println("No suitable test snippets found. Using the default RNull argument list");
            }

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
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            // throw new RuntimeException(e);
            // todo: warning
            return null;
        }
    }

    private void sweepChimney() throws IOException {
        System.out.println("++++++++++++++++++++++");
        System.out.println("+  Chimney-sweeping  +");
        System.out.println("++++++++++++++++++++++");
        System.out.println();

        boolean useDiagonalGen;

        long totalCombinations = calculateNumOfSampleCombinations(argSamples);

        switch (diagSuite.diagConfig.sweepingMode) {
            case lite:
                useDiagonalGen = true;
                break;

            case total:
                useDiagonalGen = false;
                break;

            case auto:
            default:
                useDiagonalGen = totalCombinations > diagSuite.diagConfig.maxTotalCombinations;
                break;
        }

        List<List<Object>> generatedCombinations = generateSampleArgCombinations(argSamples, useDiagonalGen);

        System.out.println("Springboard argument lists: " + validArgsList.size());
        System.out.println("Used sample combinations: " + generatedCombinations.size() + " (from total " + totalCombinations + ")");
        System.out.println("Sweeps to perform: " + generatedCombinations.size() * validArgsList.size());

        System.out.println();
        System.out.println();
        System.out.println("Press Enter to continue ...");
        System.in.read();

        evalArgsWithSampleCombinations(generatedCombinations);
    }

    private void evalBuiltin(RList validArgs, List<List<Object>> argSampleCombinations) {
        List<List<Object>> mergedSampleAndValidArgs = mergeValidAndSampleArgs(validArgs, argSampleCombinations);

        for (List<Object> evalArgs : mergedSampleAndValidArgs) {
            evalBuiltin(evalArgs);
        }

    }

    private void evalArgsWithSampleCombinations(List<List<Object>> argSampleCombinations) {
        sweepCounter = 0;
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

            String call;
            switch (annotation.kind()) {
                case INTERNAL:
                    call = ".Internal(" + builtinName + "(" + sb + "))";
                    break;
                default:
                    call = builtinName + "(" + sb + ")";
                    break;
            }

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
                if (i >= sampleArgs.size() || sampleArgs.get(i) == null) {
                    newSampleArgs.add(validArgs.getDataAt(i));
                } else {
                    newSampleArgs.add(sampleArgs.get(i));
                }
            }
            return newSampleArgs;
        }).collect(Collectors.toList());
    }

    private static String cutOffInvocation(String a, String anchor) {
        int i = a.indexOf(anchor);
        if (i >= 0) {
            return a.substring(0, i);
        } else {
            return null;
        }
    }
}
