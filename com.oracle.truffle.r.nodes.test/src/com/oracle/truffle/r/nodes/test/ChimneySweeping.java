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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineBuilder;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.nodes.casts.SamplesCollector;
import com.oracle.truffle.r.nodes.test.RBuiltinDiagnostics.DiagConfig;
import com.oracle.truffle.r.nodes.test.RBuiltinDiagnostics.RIntBuiltinDiagFactory;
import com.oracle.truffle.r.nodes.test.RBuiltinDiagnostics.SingleBuiltinDiagnostics;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.generate.FastRSession;
import com.oracle.truffle.r.test.generate.GnuROneShotRSession;
import com.oracle.truffle.r.test.generate.TestOutputManager;
import com.oracle.truffle.r.test.generate.TestOutputManager.TestInfo;

/**
 * Use the following command to sweep all builtins
 *
 * <pre>
 * mx rbdiag --sweep --mnonly --matchLevel=error --maxSweeps=30 --outMaxLev=0
 * </pre>
 *
 * .
 *
 */
class ChimneySweeping extends SingleBuiltinDiagnostics {

    private final Set<String> blacklistedBuiltins = new HashSet<>();
    {
        blacklistedBuiltins.add(".dfltWarn");
        blacklistedBuiltins.add("browser");
        blacklistedBuiltins.add(".fastr.context.r");
    }

    private static final String TEST_PREFIX = "com.oracle.truffle.r.test.builtins.TestBuiltin_";
    private static final String SWEEP_MODE_ARG = "--sweep";
    private static final String SWEEP_MODE_ARG_SPEC = SWEEP_MODE_ARG + "=";
    private static final String NO_SELF_TEST_ARG = "--noSelfTest";
    private static final String MISSING_AND_NULL_SAMPLES_ONLY_ARG = "--mnonly";
    private static final String OUTPUT_MATCH_LEVEL = "--matchLevel";
    private static final String OUTPUT_MATCH_LEVEL_SPEC = OUTPUT_MATCH_LEVEL + "=";
    private static final String MAX_SWEEPS_ARG = "--maxSweeps=";

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

    enum OutputMatchLevel {
        same,
        error;

        static Optional<OutputMatchLevel> fromArg(String arg) {
            if (arg.startsWith(OUTPUT_MATCH_LEVEL_SPEC)) {
                return Optional.of(valueOf(arg.substring(OUTPUT_MATCH_LEVEL_SPEC.length())));
            } else {
                return Optional.empty();
            }
        }
    }

    static class ChimneySweepingConfig extends DiagConfig {
        ChimneySweepingMode sweepingMode;
        OutputMatchLevel outputMatchLevel;
        boolean missingAndNullSamplesOnly;
        boolean performPipelineSelfTest;
        int maxSweeps;
    }

    static class ChimneySweepingSuite extends RBuiltinDiagnostics {

        final ChimneySweepingConfig diagConfig;
        final FastRSession fastRSession;
        final GnuROneShotRSession gnuRSession;
        final TestOutputManager outputManager;

        ChimneySweepingSuite(ChimneySweepingConfig config) throws IOException {
            super(config);
            this.diagConfig = config;

            print(1, "Loading GnuR ...");
            gnuRSession = new GnuROneShotRSession();

            print(1, "Loading FastR ...");
            fastRSession = FastRSession.create();

            print(1, "Loading test outputs ...");
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
            config.outputMatchLevel = getOutputMatchLevel(args).flatMap(OutputMatchLevel::fromArg).orElse(OutputMatchLevel.same);
            config.missingAndNullSamplesOnly = Arrays.stream(args).filter(arg -> MISSING_AND_NULL_SAMPLES_ONLY_ARG.equals(arg)).findFirst().isPresent();
            // The pipeline self-test is disabled when only RMissing and RNull samples are used as
            // these values are not determined via the pipeline static type analysis
            config.performPipelineSelfTest = config.missingAndNullSamplesOnly ? false : !Arrays.stream(args).filter(arg -> NO_SELF_TEST_ARG.equals(arg)).findFirst().isPresent();
            config.maxSweeps = Arrays.stream(args).filter(arg -> arg.startsWith(MAX_SWEEPS_ARG)).map(x -> Integer.parseInt(x.split("=")[1])).findFirst().orElse(Integer.MAX_VALUE);
            return RBuiltinDiagnostics.initDiagConfig(config, args, false);
        }

        private static Optional<String> getSweepMode(String[] args) {
            return Arrays.stream(args).filter(arg -> arg.startsWith(SWEEP_MODE_ARG)).findFirst();
        }

        private static Optional<String> getOutputMatchLevel(String[] args) {
            return Arrays.stream(args).filter(arg -> arg.startsWith(OUTPUT_MATCH_LEVEL_SPEC)).findFirst();
        }

        @Override
        public SingleBuiltinDiagnostics createBuiltinDiagnostics(RBuiltinDiagFactory bf) {
            if (bf instanceof RIntBuiltinDiagFactory) {
                return new ChimneySweeping(this, (RIntBuiltinDiagFactory) bf);
            } else {
                throw new UnsupportedOperationException("Only non-external builtins supported for chimney-sweeping atm");
            }
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

    private final ChimneySweepingSuite diagSuite;
    private final RBuiltinKind kind;

    private List<Samples<?>> argSamples;
    private Set<RList> validArgsList;
    private CastNode[] castNodes;

    private final Set<List<String>> printedOutputPairs = new HashSet<>();
    private final Set<String> printedErrors = new HashSet<>();
    private int sweepCounter = 0;

    ChimneySweeping(ChimneySweepingSuite diagSuite, RIntBuiltinDiagFactory builtinFactory) {
        super(diagSuite, builtinFactory);
        this.diagSuite = diagSuite;
        this.kind = builtinFactory.getBuiltinKind();
    }

    @Override
    SingleBuiltinDiagnostics init() throws Throwable {
        super.init();

        this.castNodes = builtinFactory.getCastNodes();

        print(0, "\n*** Chimney-sweeping of '" + builtinName + "' (" + builtinFactory.getBuiltinMetaClass().getName() + ") ***");

        this.validArgsList = extractValidArgsForBuiltin();
        this.argSamples = createSamples();

        return this;
    }

    @Override
    public boolean diagnoseBuiltin() throws Exception {
        // super.diagnoseBuiltin();

        if (blacklistedBuiltins.contains(builtinName)) {
            print(1, "Builtin '" + builtinName + "' blacklisted for chimney-sweeping");
        } else {
            sweepChimney();
        }

        return true;
    }

    @Override
    protected boolean diagnosePipeline(int i) {
        super.diagnosePipeline(i);

        print(1, " Samples:");
        print(1, argSamples.get(i));

        if (diagSuite.diagConfig.performPipelineSelfTest) {
            checkPipelines(i);
        }

        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Samples<?>> createSamples() {
        DefaultArgsExtractor defArgExt = new DefaultArgsExtractor(diagSuite.fastRSession, msg -> print(1, msg));
        Map<String, Samples<?>> defaultArgs = defArgExt.extractDefaultArgs(builtinName);

        PipelineBuilder[] plBuilders = casts.getPipelineBuilders();

        List<Samples<?>> as = new ArrayList<>();
        for (int i = 0; i < argLength; i++) {
            Samples samples;

            if (diagSuite.diagConfig.missingAndNullSamplesOnly) {
                samples = Samples.anything(RNull.instance).or(Samples.anything(RMissing.instance));
            } else {
                PipelineBuilder plBuilder;
                if (i < plBuilders.length) {
                    plBuilder = plBuilders[i];
                } else {
                    plBuilder = null;
                }
                try {
                    if (plBuilder == null) {
                        samples = Samples.anything();
                    } else {
                        Set<Object> sampleSet = SamplesCollector.collect(plBuilder.getFirstStep());
                        samples = new Samples<>("", sampleSet, Collections.emptySet(), x -> sampleSet.contains(x));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error in sample generation from argument " + i, e);
                }

                Samples defArgSamples = defaultArgs.get(parameterNames[i]);
                samples = defArgSamples == null ? samples : samples.and(defArgSamples);
            }

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
                print(1, "No samples");
            } else {
                testPipeline(cn, samples);
                print(1, "Pipeline check OK (" + samples.positiveSamples().size() + "," + samples.negativeSamples().size() + ")");
            }
        }
    }

    private void testPipeline(CastNode cn, Samples<?> samples) {
        NodeHandle<CastNode> argCastNodeHandle = TestUtilities.createHandle(cn, (node, args) -> {
            return node.doCast(args[0]);
        });

        for (Object sample : samples.positiveSamples()) {
            try {
                argCastNodeHandle.call(sample);
            } catch (UnsupportedSpecializationException e) {
                print(1, "Warning: No specialization to handle arg " + sample + " : " + e.getMessage());
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
        final PolyglotEngine vm = diagSuite.fastRSession.checkContext(null).createVM();

        try {
            String snippetAnchor;
            switch (kind) {
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

            return args;
        } finally {
            vm.dispose();
        }
    }

    private RList evalValidArgs(String argsExpr, PolyglotEngine vm) {
        try {
            Value eval = vm.eval(RSource.fromTextInternal(argsExpr, RSource.Internal.UNIT_TEST));
            Object res = eval.get();
            // TODO: do not use reflection here
            Method getter = res.getClass().getDeclaredMethod("getDelegate");
            getter.setAccessible(true);
            RList args = (RList) getter.invoke(res);
            return args;
        } catch (Exception e) {
            print(1, "Warning: Cannot parse arguments: " + argsExpr);
            return null;
        }
    }

    private void sweepChimney() {
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

        print(1, "Springboard argument lists: " + validArgsList.size());
        print(1, "Used sample combinations: " + generatedCombinations.size() + " (from total " + totalCombinations + ")");

        int sweepsToPerform = Math.min(generatedCombinations.size() * validArgsList.size(), diagSuite.diagConfig.maxSweeps);
        print(0, "Sweeps to perform: " + sweepsToPerform);

        evalArgsWithSampleCombinations(generatedCombinations);
    }

    private void evalBuiltin(RList validArgs, List<List<Object>> argSampleCombinations) {
        List<List<Object>> mergedSampleAndValidArgs = mergeValidAndSampleArgs(validArgs, argSampleCombinations);

        boolean isOriginal = true;
        for (List<Object> evalArgs : mergedSampleAndValidArgs) {
            if (!evalBuiltin(evalArgs, isOriginal)) {
                return;
            }
            isOriginal = false;
        }
    }

    private void evalArgsWithSampleCombinations(List<List<Object>> argSampleCombinations) {
        sweepCounter = 0;
        validArgsList.forEach(validArgs -> evalBuiltin(validArgs, argSampleCombinations));
    }

    private boolean evalBuiltin(List<Object> args, boolean isOriginal) {
        if (sweepCounter > diagSuite.diagConfig.maxSweeps) {
            return false;
        }

        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i < args.size(); i++) {
                Object validArg = args.get(i);
                if (validArg == RMissing.instance) {
                    continue;
                }
                String deparsedValidArg;
                try {
                    deparsedValidArg = RDeparse.deparse(validArg);
                } catch (Throwable e) {
                    throw new RuntimeException("ERROR: Cannot deparse " + validArg + ": " + e.getMessage(), e);
                }

                if (sb.length() != 0) {
                    sb.append(",");
                }
                sb.append(parameterNames[i]).append('=').append(deparsedValidArg);
            }

            String call;
            if ("(".equals(builtinName)) {
                call = "(" + sb + ")";
            } else {
                switch (kind) {
                    case INTERNAL:
                        call = ".Internal(" + builtinName + "(" + sb + "))";
                        break;
                    default:
                        call = builtinName + "(" + sb + ")";
                        break;
                }
            }

            String output;
            try {
                output = diagSuite.fastRSession.eval(null, call, null, false);
            } catch (Throwable t) {
                output = "ERROR: " + t.getMessage();
            }
            String outputGnu = diagSuite.gnuRSession.eval(null, call, null, false);

            List<String> outputPair = Arrays.asList(output, outputGnu);

            if (isOriginal && !output.equals(outputGnu)) {
                // The original test may not be passing (e.g. marked by Ignored.Inknown etc.).
                // Skip these arguments.
                System.out.print('I');
                print(1, "Ignoring original test arguments: " + sb);
                return false;
            }

            if (compareOutputs(output, outputGnu)) {
                System.out.print('.');
            } else if (!printedOutputPairs.contains(outputPair)) {
                print(0, "\n#" + sweepCounter + "> " + call);
                print(0, "\n====== FastR output ======");
                print(0, output);

                print(0, "====== GnuR output ======");
                print(0, outputGnu);
                print(0, "==========================");

                printedOutputPairs.add(outputPair);
            } else {
                System.out.print('!');
            }
        } catch (Throwable e) {
            if (!printedErrors.contains(e.getMessage())) {
                String call = ".Internal(" + builtinName + "(" + sb + "))";
                print(0, "\n[" + sweepCounter + "]> " + call);
                print(0, "ERROR: " + e.getMessage());
                printedErrors.add(e.getMessage());
            }
        } finally {
            sweepCounter++;
        }

        return true;
    }

    private boolean compareOutputs(String output, String outputGnu) {
        switch (diagSuite.diagConfig.outputMatchLevel) {
            case same:
                return outputGnu.equals(output);
            case error:
                if (output.contains("ERROR:")) {
                    // FastR error
                    return false;
                }
                if (output.contains("Error") && outputGnu.contains("Error")) {
                    return true;
                }
                if (!output.contains("Error") && !outputGnu.contains("Error")) {
                    return true;
                }
                return false;
            default:
                throw new UnsupportedOperationException("Unsupported output match level: " + diagSuite.diagConfig.outputMatchLevel);
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
