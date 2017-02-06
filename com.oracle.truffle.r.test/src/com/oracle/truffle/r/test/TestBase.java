/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;

import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.test.generate.FastRSession;
import com.oracle.truffle.r.test.generate.GnuROneShotRSession;
import com.oracle.truffle.r.test.generate.TestOutputManager;

/**
 * Base class for all unit tests. The unit tests are actually arranged as a collection of
 * micro-tests of the form {@code assertXXX(String test)}, organized into groups under one JUnit
 * test method, i.e., annotated with {@link Test}. Some of the micro-tests are generated dynamically
 * from templates.
 *
 * Given this two-level structure, it is important that a failing micro-test does not fail the
 * entire JUnit test, as this will prevent the subsequent micro-tests from running at all. Instead
 * failure is handled by setting the {@link #microTestFailed} field, and JUnit failure is indicated
 * in the {@link #afterTest()} method.
 */
public class TestBase {

    public static final boolean ProcessFailedTests = Boolean.getBoolean("ProcessFailedTests");

    /**
     * See {@link com.oracle.truffle.r.test.builtins.TestTestBase} for examples.
     */
    public enum Output implements TestTrait {
        IgnoreErrorContext, // the error context is ignored (e.g., "a+b" vs. "a + b")
        IgnoreErrorMessage, // the actual error message is ignored
        IgnoreWarningContext, // the warning context is ignored
        MayIgnoreErrorContext, // like IgnoreErrorContext, but no warning if the messages match
        MayIgnoreWarningContext,
        ContainsReferences, // replaces references in form of 0xbcdef1 for numbers
        IgnoreWhitespace, // removes all whitespace from the whole output
        IgnoreCase; // ignores upper/lower case differences

        @Override
        public String getName() {
            return name();
        }
    }

    public enum Ignored implements TestTrait {
        Unknown("failing tests that have not been classified yet"),
        Unstable("tests that produce inconsistent results in GNUR"),
        OutputFormatting("tests that fail because of problems with output formatting"),
        ParserErrorFormatting("tests that fail because of the formatting of parser error messages"),
        WrongCaller("tests that fail because the caller source is wrong in an error or warning"),
        ParserError("tests that fail because of bugs in the parser"),
        ImplementationError("tests that fail because of bugs in other parts of the runtime"),
        ReferenceError("tests that fail because of faulty behavior in the reference implementation that we don't want to emulate"),
        SideEffects("tests that are ignored because they would interfere with other tests"),
        MissingWarning("tests that fail because of missing warnings"),
        MissingBuiltin("tests that fail because of missing builtins"),
        Unimplemented("tests that fail because of missing functionality");

        private final String description;

        Ignored(String description) {
            this.description = description;
        }

        @Override
        public String getName() {
            return name();
        }

        public String getDescription() {
            return description;
        }
    }

    public enum Context implements TestTrait {
        NonShared, // Test requires a new non-shared {@link RContext}.
        LongTimeout; // Test requires a long timeout

        @Override
        public String getName() {
            return name();
        }
    }

    /**
     * Instantiated by the mx {@code JUnit} wrapper. The arguments are passed in the constructor and
     * must be a comma-separated list of strings, i.e.:
     * <ul>
     * <li>{@code expected=dir}: path to dir containing expected output file to be
     * read/generated/updated</li>
     * <li>{@code gen-expected}: causes the expected output file to be generated/updated</li>
     * <li>{@code gen-fastr=dir}: causes the FastR output to be generated in dir</li>
     * <li>{@code gen-diff=dir}: generates a difference file between FastR and expected output in
     * dir</li>
     * <li>{@code check-expected}: checks that the expected output file is consistent with the set
     * of tests but does not update</li>
     * <li>{@code keep-trailing-whitespace}: keep trailing whitespace when generating expected
     * <li>{@code test-methods}: pattern to match test methods in test classes output></li>
     * </ul>
     */
    public static class RunListener extends org.junit.runner.notification.RunListener {

        private static File diffsOutputFile;

        private static final String GEN_EXPECTED = "gen-expected";
        private static final String GEN_EXPECTED_QUIET = "gen-expected-quiet";
        private static final String CHECK_EXPECTED = "check-expected";
        private static final String EXPECTED = "expected=";
        private static final String GEN_FASTR = "gen-fastr=";
        private static final String GEN_DIFFS = "gen-diff=";
        private static final String KEEP_TRAILING_WHITESPACE = "keep-trailing-whitespace";
        private static final String TRACE_TESTS = "trace-tests";
        private static final String TEST_METHODS = "test-methods=";
        /**
         * The dir where 'mx' puts the output from building this project.
         */
        private static final String TEST_PROJECT_OUTPUT_DIR = "test-project-output-dir=";

        private final String arg;

        /**
         * Constructor with customization arguments.
         */
        public RunListener(String arg) {
            this.arg = arg;
        }

        @Override
        public void testRunStarted(Description description) {
            try {
                File expectedOutputFile = null;
                File fastROutputFile = null;
                boolean checkExpected = false;
                boolean genExpected = false;
                boolean genExpectedQuiet = false;
                if (arg != null) {
                    String[] args = arg.split(",");
                    for (String directive : args) {
                        if (directive.startsWith(EXPECTED)) {
                            expectedOutputFile = new File(new File(directive.replace(EXPECTED, "")), TestOutputManager.TEST_EXPECTED_OUTPUT_FILE);
                        } else if (directive.startsWith(GEN_FASTR)) {
                            fastROutputFile = new File(new File(directive.replace(GEN_FASTR, "")), TestOutputManager.TEST_FASTR_OUTPUT_FILE);
                        } else if (directive.startsWith(GEN_DIFFS)) {
                            diffsOutputFile = new File(new File(directive.replace(GEN_DIFFS, "")), TestOutputManager.TEST_DIFF_OUTPUT_FILE);
                        } else if (directive.equals(GEN_EXPECTED)) {
                            genExpected = true;
                        } else if (directive.equals(GEN_EXPECTED_QUIET)) {
                            genExpectedQuiet = true;
                        } else if (directive.equals(CHECK_EXPECTED)) {
                            checkExpected = true;
                        } else if (directive.equals(KEEP_TRAILING_WHITESPACE)) {
                            keepTrailingWhiteSpace = true;
                        } else if (directive.equals(TRACE_TESTS)) {
                            traceTests = true;
                        } else if (directive.startsWith(TEST_PROJECT_OUTPUT_DIR)) {
                            testProjectOutputDir = Paths.get(directive.replace(TEST_PROJECT_OUTPUT_DIR, ""));
                        } else if (directive.equals(TEST_METHODS)) {
                            testMethodsPattern = directive.replace(TEST_METHODS, "");
                        } else {
                            throw new RuntimeException("RunListener arg: " + arg + " invalid");
                        }
                    }
                }
                if (genExpected) {
                    System.setProperty("GenerateExpectedOutput", "true");
                }
                expectedOutputManager = new ExpectedTestOutputManager(expectedOutputFile, genExpected, checkExpected, genExpectedQuiet);
                fastROutputManager = new FastRTestOutputManager(fastROutputFile);
                addOutputHook();
            } catch (Throwable ex) {
                throw new AssertionError("R initialization failure", ex);
            }
        }

        @Override
        public void testRunFinished(Result result) {
            try {
                if (expectedOutputManager.generate) {
                    boolean updated = expectedOutputManager.writeTestOutputFile();
                    if (updated) {
                        if (expectedOutputManager.checkOnly) {
                            // fail fast
                            Utils.rSuicideDefault("Test file:" + expectedOutputManager.outputFile + " is out of sync with unit tests");
                        }
                        System.out.println("updating " + expectedOutputManager.outputFile);
                    }
                }
                if (fastROutputManager.outputFile != null) {
                    fastROutputManager.writeTestOutputFile(null, false);
                }
                if (diffsOutputFile != null) {
                    TestOutputManager.writeDiffsTestOutputFile(diffsOutputFile, expectedOutputManager, fastROutputManager);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void testStarted(Description description) {
            testElementName = description.getClassName() + "." + description.getMethodName();
            if (traceTests) {
                System.out.println(testElementName);
            }
            failedMicroTests = new ArrayList<>();
        }
    }

    @Before
    public void beforeTest() {
        checkOutputManagersInitialized();
    }

    private static void checkOutputManagersInitialized() {
        if (expectedOutputManager == null) {
            /*
             * Assume we are running a unit test in an IDE/non-JUnit setup and therefore the
             * RunListener was not invoked. In this case we can expect the test output file to exist
             * and open it as a resource.
             */
            URL expectedTestOutputURL = ResourceHandlerFactory.getHandler().getResource(TestBase.class, TestOutputManager.TEST_EXPECTED_OUTPUT_FILE);
            if (expectedTestOutputURL == null) {
                Assert.fail("cannot find " + TestOutputManager.TEST_EXPECTED_OUTPUT_FILE + " resource");
            } else {
                try {
                    expectedOutputManager = new ExpectedTestOutputManager(new File(expectedTestOutputURL.getPath()), false, false, false);
                    fastROutputManager = new FastRTestOutputManager(null);
                    addOutputHook();
                } catch (IOException ex) {
                    Assert.fail("error reading: " + expectedTestOutputURL.getPath() + ": " + ex);
                }
            }
        }
    }

    /**
     * Method for non-JUnit implementation to emulate important behavior of {@link RunListener}.
     */
    public static void emulateRunListener() {
        checkOutputManagersInitialized();
    }

    /**
     * Method for non-JUnit implementation to set test tracing.
     */
    public static void setTraceTests() {
        traceTests = true;
    }

    /**
     * Set the test context explicitly (for non-JUnit implementation). N.B. The {@code lineno} is
     * not the micro-test line, but that of the method declaration.
     */
    public void doBeforeTest(String className, int lineno, String methodName) {
        testElementName = className + "." + methodName;
        failedMicroTests = new ArrayList<>();
        explicitTestContext = String.format("%s:%d (%s)", className, lineno, methodName);
        if (traceTests) {
            System.out.println(testElementName);
        }
    }

    private static class ExpectedTestOutputManager extends TestOutputManager {
        private final boolean generate;

        /**
         * When {@code true}, indicates that test generation is in check only mode.
         */
        private final boolean checkOnly;
        /**
         * When running in generation mode, the original content of the expected output file.
         */
        private final String oldExpectedOutputFileContent;

        private boolean haveRSession;

        protected ExpectedTestOutputManager(File outputFile, boolean generate, boolean checkOnly, boolean genExpectedQuiet) throws IOException {
            super(outputFile);
            this.checkOnly = checkOnly;
            this.generate = generate;
            if (genExpectedQuiet) {
                localDiagnosticHandler.setQuiet();
            }
            oldExpectedOutputFileContent = readTestOutputFile();
            if (generate) {
                createRSession();
            }
        }

        void createRSession() {
            if (!haveRSession) {
                setRSession(new GnuROneShotRSession());
                haveRSession = true;
            }
        }

        boolean writeTestOutputFile() throws IOException {
            return writeTestOutputFile(oldExpectedOutputFileContent, checkOnly);
        }
    }

    private static class FastRTestOutputManager extends TestOutputManager {
        final FastRSession fastRSession;

        FastRTestOutputManager(File outputFile) {
            super(outputFile);
            setRSessionName("FastR");
            fastRSession = FastRSession.create();
        }
    }

    private static ExpectedTestOutputManager expectedOutputManager;
    private static FastRTestOutputManager fastROutputManager;

    private static class MicroTestInfo {
        /**
         * The expression currently being evaluated by FastR.
         */
        private String expression;

        /**
         * The result of FastR evaluating {@link #expression}.
         */
        private String fastROutput;

        /**
         * The expected output.
         */
        private String expectedOutput;

    }

    private static final MicroTestInfo microTestInfo = new MicroTestInfo();

    /**
     * Set to the JUnit test element name by the {@code RunListener}, i.e., {@code class.testMethod}
     * .
     */
    private static String testElementName;

    /**
     * Emptied at the start of a JUnit test, each failed micro test will be added to the list.
     */
    private static ArrayList<String> failedMicroTests = new ArrayList<>();

    private static ArrayList<String> unexpectedSuccessfulMicroTests = new ArrayList<>();

    private static SortedMap<String, Integer> exceptionCounts = new TreeMap<>();

    private static int successfulTestCount;
    private static int ignoredTestCount;
    private static int failedTestCount;
    private static int successfulInputCount;
    private static int ignoredInputCount;
    private static int failedInputCount;

    protected static String explicitTestContext;

    /**
     * A way to limit which tests are actually run. TODO requires more JUnit support for filtering
     * in the wrapper.
     *
     */
    @SuppressWarnings("unused") private static String testMethodsPattern;

    /**
     * {@code true} if expected output is not discarding trailing whitespace.
     */
    private static boolean keepTrailingWhiteSpace;

    /**
     * Trace the test methods as they are executed (debugging).
     */
    private static boolean traceTests;

    private static Path testProjectOutputDir;

    protected static final String ERROR = "Error";
    protected static final String WARNING = "Warning message";

    /**
     * If this is set to {@code true}, {@link Output#IgnoreErrorContext} will compare the full
     * output instead of truncating leading "Error" strings and such. This means it will behave like
     * {@link #assertEval}.
     */
    private static final boolean FULL_COMPARE_ERRORS = false;

    /**
     * To implement {@link Output#ContainsReferences}.
     **/
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("(?<id>(0x[0-9abcdefx]+))");

    /**
     * Test a given string with R source against expected output. This is (currently) an exact
     * match, so any warnings or errors will cause a failure until FastR matches GnuR in that
     * respect.
     */
    protected void assertEval(String... input) {
        evalAndCompare(input);
    }

    protected void assertEval(TestTrait trait1, String... input) {
        evalAndCompare(input, trait1);
    }

    protected void assertEval(TestTrait trait1, TestTrait trait2, String... input) {
        evalAndCompare(input, trait1, trait2);
    }

    protected void assertEval(TestTrait trait1, TestTrait trait2, TestTrait trait3, String... input) {
        evalAndCompare(input, trait1, trait2, trait3);
    }

    protected void assertEval(TestTrait trait1, TestTrait trait2, TestTrait trait3, TestTrait trait4, String... input) {
        evalAndCompare(input, trait1, trait2, trait3, trait4);
    }

    protected void assertEval(TestTrait trait1, TestTrait trait2, TestTrait trait3, TestTrait trait4, TestTrait trait5, String... input) {
        evalAndCompare(input, trait1, trait2, trait3, trait4, trait5);
    }

    protected void afterMicroTest() {
        // empty
    }

    // support testing of FastR-only functionality (equivalent GNU R output provided separately)
    protected void assertEvalFastR(String input, String gnuROutput) {
        evalAndCompare(new String[]{"if (length(grep(\"FastR\", R.Version()$version.string)) != 1) { " + gnuROutput + " } else { " + input + " }"});
    }

    /*
     * implementation support methods
     */

    /**
     * Check for micro-test failure and if so fail the entire test. N.B. Must do this using
     * {@code @After} and not in the {@code testFinished} listener method, because exceptions in the
     * listener prevent its subsequent invocation.
     */
    @After
    public void afterTest() {
        if (failedMicroTests != null && !failedMicroTests.isEmpty()) {
            fail(failedMicroTests.size() + " micro-test(s) failed: \n  " + new TreeSet<>(failedMicroTests));
        }
    }

    private static Path cwd;

    private static Path getCwd() {
        if (cwd == null) {
            cwd = Paths.get(System.getProperty("user.dir"));
        }
        return cwd;
    }

    public static void setTestProjectOutputDir(String path) {
        testProjectOutputDir = Paths.get(path);
    }

    private static final String TEST_OUTPUT = "tmptest";

    /**
     * Return a path that is relative to the 'cwd/testoutput' when running tests.
     */
    public static Path relativize(Path path) {
        return getCwd().relativize(path);
    }

    /**
     * Creates a directory with suffix {@code name} in the {@code testoutput} directory and returns
     * a relative path to it.
     */
    public static Path createTestDir(String name) {
        Path dir = Paths.get(getCwd().toString(), TEST_OUTPUT, name);
        if (!dir.toFile().exists()) {
            if (!dir.toFile().mkdirs()) {
                Assert.fail("failed to create dir: " + dir.toString());
            }
        }
        return relativize(dir);
    }

    private static final String TEST_PROJECT = "com.oracle.truffle.r.test";
    private static final String TEST_NATIVE_PROJECT = "com.oracle.truffle.r.test.native";

    /**
     * Returns a path to {@code baseName}, assumed to be nested in {@link #testProjectOutputDir}.
     * The path is return relativized to the cwd.
     */
    public static Path getProjectFile(Path baseName) {
        Path baseNamePath = Paths.get(TEST_PROJECT.replace('.', '/'), baseName.toString());
        Path result = relativize(testProjectOutputDir.resolve(baseNamePath));
        return result;
    }

    public static Path getNativeProjectFile(Path baseName) {
        Path path = Paths.get(TEST_NATIVE_PROJECT, baseName.toString());
        return path;
    }

    private static void microTestFailed() {
        if (!ProcessFailedTests) {
            System.err.printf("%nMicro-test failure: %s%n", getTestContext());
            System.err.printf("%16s %s%n", "Expression:", microTestInfo.expression);
            System.err.printf("%16s %s", "Expected output:", microTestInfo.expectedOutput);
            System.err.printf("%16s %s%n", "FastR output:", microTestInfo.fastROutput);

            failedMicroTests.add(getTestContext());
        }
    }

    private static String getTestContext() {
        if (explicitTestContext != null) {
            return explicitTestContext;
        }
        // We want the stack trace as if the JUnit test failed.
        RuntimeException ex = new RuntimeException();
        // The first method not in TestBase is the culprit
        StackTraceElement culprit = null;
        try {
            // N.B. This may not always be available (AOT).
            StackTraceElement[] stackTrace = ex.getStackTrace();
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement se = stackTrace[i];
                if (!se.getClassName().endsWith("TestBase")) {
                    culprit = se;
                    break;
                }
            }
            String context = String.format("%s:%d (%s)", culprit.getClassName(), culprit.getLineNumber(), culprit.getMethodName());
            return context;
        } catch (NullPointerException npe) {
            return "no test context available";
        }
    }

    /**
     * Wraps the traits, there are some meta-traits like {@link #isIgnored}, other traits can be
     * accessed through the corresponding enum-set.
     */
    private static class TestTraitsSet {
        EnumSet<Ignored> ignored = EnumSet.noneOf(Ignored.class);
        EnumSet<Output> output = EnumSet.noneOf(Output.class);
        EnumSet<Context> context = EnumSet.noneOf(Context.class);
        boolean isIgnored;
        boolean containsError;

        TestTraitsSet(TestTrait[] traits) {
            ignored.addAll(Arrays.asList(TestTrait.collect(traits, Ignored.class)));
            output.addAll(Arrays.asList(TestTrait.collect(traits, Output.class)));
            context.addAll(Arrays.asList(TestTrait.collect(traits, Context.class)));
            containsError = (!FULL_COMPARE_ERRORS && (output.contains(Output.IgnoreErrorContext) || output.contains(Output.IgnoreErrorMessage)));
            isIgnored = ignored.size() > 0 ^ (ProcessFailedTests && !(ignored.contains(Ignored.Unstable) || ignored.contains(Ignored.SideEffects)));
            assert !output.contains(Output.IgnoreWhitespace) || output.size() == 1 : "IgnoreWhitespace trait does not work with any other Output trait";

        }

        String preprocessOutput(String out) {
            if (output.contains(Output.IgnoreWhitespace)) {
                return out.replaceAll("\\s+", "");
            }
            if (output.contains(Output.IgnoreCase)) {
                return out.toLowerCase();
            }
            if (output.contains(Output.ContainsReferences)) {
                return convertReferencesInOutput(out);
            }
            return out;
        }
    }

    private void evalAndCompare(String[] inputs, TestTrait... traitsList) {
        WhiteList[] whiteLists = TestTrait.collect(traitsList, WhiteList.class);
        TestTraitsSet traits = new TestTraitsSet(traitsList);
        ContextInfo contextInfo = traits.context.contains(Context.NonShared) ? fastROutputManager.fastRSession.createContextInfo(ContextKind.SHARE_NOTHING) : null;
        int index = 1;
        boolean allOk = true;
        for (String input : inputs) {
            String expected = expectedEval(input, traitsList);
            if (traits.isIgnored || generatingExpected()) {
                ignoredInputCount++;
            } else {
                String result = fastREval(input, contextInfo, traits.context.contains(Context.LongTimeout));
                CheckResult checkResult = checkResult(whiteLists, input, traits.preprocessOutput(expected), traits.preprocessOutput(result), traits);

                result = checkResult.result;
                expected = checkResult.expected;
                boolean ok = checkResult.ok;

                if (ProcessFailedTests) {
                    if (ok) {
                        unexpectedSuccessfulMicroTests.add(getTestContext() + ": " + input);
                    } else if (expected.startsWith(ERROR) && result.startsWith(ERROR)) {
                        if (checkMessageStripped(expected, result)) {
                            unexpectedSuccessfulMicroTests.add("<error> " + getTestContext() + ": " + input);
                        }
                    } else if (expected.contains(WARNING) && result.contains(WARNING)) {
                        if (getOutputWithoutWarning(expected).equals(getOutputWithoutWarning(result)) && getWarningMessage(expected).equals(getWarningMessage(result))) {
                            unexpectedSuccessfulMicroTests.add("<warning> " + getTestContext() + ": " + input);
                        }
                    }
                }
                if (ok) {
                    successfulInputCount++;
                } else {
                    failedInputCount++;
                    microTestFailed();
                    if (inputs.length > 1) {
                        System.out.print('E');
                    }
                }
                allOk &= ok;
                afterMicroTest();
            }
            if ((index) % 100 == 0) {
                System.out.print('.');
            }
            index++;
        }
        if (traits.isIgnored) {
            ignoredTestCount++;
        } else if (allOk) {
            successfulTestCount++;
        } else {
            failedTestCount++;
        }
    }

    private static class CheckResult {

        public final boolean ok;
        public final String result;
        public final String expected;

        CheckResult(boolean ok, String result, String expected) {
            this.ok = ok;
            this.result = result;
            this.expected = expected;
        }
    }

    private CheckResult checkResult(WhiteList[] whiteLists, String input, String originalExpected, String originalResult, TestTraitsSet traits) {
        boolean ok;
        String result = originalResult;
        String expected = originalExpected;
        if (expected.equals(result) || searchWhiteLists(whiteLists, input, expected, result, traits)) {
            ok = true;
            if (traits.containsError && !traits.output.contains(Output.IgnoreErrorMessage)) {
                System.out.println("unexpected correct error message: " + getTestContext());
            }
            if (traits.output.contains(Output.IgnoreWarningContext)) {
                System.out.println("unexpected correct warning message: " + getTestContext());
            }
        } else {
            if (traits.output.contains(Output.IgnoreWarningContext) || (traits.output.contains(Output.MayIgnoreWarningContext) && expected.contains(WARNING))) {
                String resultWarning = getWarningMessage(result);
                String expectedWarning = getWarningMessage(expected);
                ok = resultWarning.equals(expectedWarning);
                result = getOutputWithoutWarning(result);
                expected = getOutputWithoutWarning(expected);
            } else {
                ok = true;
            }
            if (ok) {
                if (traits.containsError || (traits.output.contains(Output.MayIgnoreErrorContext) && expected.startsWith(ERROR))) {
                    ok = result.startsWith(ERROR) && (traits.output.contains(Output.IgnoreErrorMessage) || checkMessageStripped(expected, result) || checkMessageVectorInIndex(expected, result));
                } else {
                    ok = expected.equals(result);
                }
            }
        }
        return new CheckResult(ok, result, expected);
    }

    private static String convertReferencesInOutput(String input) {
        String result = input;
        Matcher matcher = REFERENCE_PATTERN.matcher(result);
        HashMap<String, Integer> idsMap = new HashMap<>();
        int currentId = 1;
        while (matcher.find()) {
            if (idsMap.putIfAbsent(matcher.group("id"), currentId) == null) {
                currentId++;
            }
        }
        for (Entry<String, Integer> item : idsMap.entrySet()) {
            result = result.replace(item.getKey(), item.getValue().toString());
        }
        return result;
    }

    private boolean searchWhiteLists(WhiteList[] whiteLists, String input, String expected, String result, TestTraitsSet testTraits) {
        if (whiteLists == null) {
            return false;
        }
        for (WhiteList list : whiteLists) {
            WhiteList.Results wlr = list.get(input);
            if (wlr != null) {
                // Sanity check that "expected" matches the entry in the WhiteList
                CheckResult checkedResult = checkResult(null, input, wlr.expected, expected, testTraits);
                if (!checkedResult.ok) {
                    System.out.println("expected output does not match: " + wlr.expected + " vs. " + expected);
                    return false;
                }
                // Substitute the FastR output and try to match that
                CheckResult fastRResult = checkResult(null, input, wlr.fastR, result, testTraits);
                if (fastRResult.ok) {
                    list.markUsed(input);
                    return true;
                }
            }
        }
        return false;
    }

    private static final Pattern warningPattern1 = Pattern.compile("^(?<pre>.*)Warning messages:\n1:(?<msg0>.*)\n2:(?<msg1>.*)\n3:(?<msg2>.*)\n4:(?<msg3>.*)5:(?<msg4>.*)$", Pattern.DOTALL);
    private static final Pattern warningPattern2 = Pattern.compile("^(?<pre>.*)Warning messages:\n1:(?<msg0>.*)\n2:(?<msg1>.*)\n3:(?<msg2>.*)\n4:(?<msg3>.*)$", Pattern.DOTALL);
    private static final Pattern warningPattern3 = Pattern.compile("^(?<pre>.*)Warning messages:\n1:(?<msg0>.*)\n2:(?<msg1>.*)\n3:(?<msg2>.*)$", Pattern.DOTALL);
    private static final Pattern warningPattern4 = Pattern.compile("^(?<pre>.*)Warning messages:\n1:(?<msg0>.*)\n2:(?<msg1>.*)$", Pattern.DOTALL);
    private static final Pattern warningPattern5 = Pattern.compile("^(?<pre>.*)Warning message:(?<msg0>.*)$", Pattern.DOTALL);

    private static final Pattern warningMessagePattern = Pattern.compile("^\n? ? ?(?:In .* :[ \n])?[ \n]*(?<m>[^\n]*)\n?$", Pattern.DOTALL);

    private static final Pattern[] warningPatterns = new Pattern[]{warningPattern1, warningPattern2, warningPattern3, warningPattern4, warningPattern5};

    private static Matcher getWarningMatcher(String output) {
        for (Pattern pattern : warningPatterns) {
            Matcher matcher = pattern.matcher(output);
            if (matcher.matches()) {
                return matcher;
            }
        }
        return null;
    }

    private static String getWarningMessage(String output) {
        Matcher matcher = getWarningMatcher(output);
        if (matcher == null) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < warningPatterns.length; i++) {
            try {
                String message = matcher.group("msg" + i);
                Matcher messageMatcher = warningMessagePattern.matcher(message);
                boolean messageMatches = messageMatcher.matches();
                assert messageMatches : "unexpected format in warning message: " + message;
                str.append(messageMatcher.group("m").trim()).append('|');
            } catch (IllegalArgumentException e) {
                break;
            }
        }
        return str.toString();
    }

    private static String getOutputWithoutWarning(String output) {
        Matcher matcher = getWarningMatcher(output);
        return matcher != null ? matcher.group("pre") : output;
    }

    /**
     * Compares the actual error message, after removing any context before the ':' and after
     * removing whitespace.
     */
    private static boolean checkMessageStripped(String expected, String result) {
        String[] stripped = splitAndStripMessage(expected, result);
        if (stripped == null) {
            return false;
        }
        String expectedStripped = stripped[0];
        String resultStripped = stripped[1];
        return resultStripped.equals(expectedStripped);
    }

    private static final Pattern VECTOR_INDEX_PATTERN = Pattern.compile("(?<prefix>(attempt to select (more|less) than one element)).*");

    /**
     * Deal with R 3.3.x "selected more/less than one element in xxxIndex.
     */
    private static boolean checkMessageVectorInIndex(String expected, String result) {
        String[] stripped = splitAndStripMessage(expected, result);
        if (stripped == null) {
            return false;
        }
        String expectedStripped = stripped[0];
        String resultStripped = stripped[1];
        Matcher matcher = VECTOR_INDEX_PATTERN.matcher(expectedStripped);
        if (matcher.find()) {
            String prefix = matcher.group("prefix");
            return prefix.equals(resultStripped);
        } else {
            return false;
        }
    }

    private static String[] splitAndStripMessage(String expected, String result) {
        int cxr = result.lastIndexOf(':');
        int cxe = expected.lastIndexOf(':');
        if (cxr < 0 || cxe < 0) {
            return null;
        }
        String resultStripped = result.substring(cxr + 1).trim();
        String expectedStripped = expected.substring(cxe + 1).trim();
        return new String[]{expectedStripped, resultStripped};
    }

    /**
     * Evaluate {@code input} in FastR, returning all (virtual) console output that was produced. If
     * {@code nonShared} then this must evaluate in a new, non-shared, {@link RContext}.
     */
    protected String fastREval(String input, ContextInfo contextInfo, boolean longTimeout) {
        microTestInfo.expression = input;
        String result;
        try {
            beforeEval();
            result = fastROutputManager.fastRSession.eval(this, input, contextInfo, longTimeout);
        } catch (Throwable e) {
            String clazz;
            if (e instanceof RInternalError && e.getCause() != null) {
                clazz = e.getCause().getClass().getSimpleName();
            } else {
                clazz = e.getClass().getSimpleName();
            }
            Integer count = exceptionCounts.get(clazz);
            exceptionCounts.put(clazz, count == null ? 1 : count + 1);
            result = e.toString();
            if (!ProcessFailedTests) {
                e.printStackTrace();
            }
        }
        if (fastROutputManager.outputFile != null) {
            fastROutputManager.addTestResult(testElementName, input, result, keepTrailingWhiteSpace);
        }
        microTestInfo.fastROutput = result;
        return TestOutputManager.prepareResult(result, keepTrailingWhiteSpace);
    }

    public static boolean generatingExpected() {
        return expectedOutputManager.generate;
    }

    protected static boolean checkOnly() {
        return expectedOutputManager.checkOnly;
    }

    /**
     * Used only for package installation to avoid explicitly using {@link ProcessBuilder}. Instead
     * we go via the {@code system2} R function (which may call {@link ProcessBuilder} internally).
     *
     */
    protected static String evalInstallPackage(String system2Command) throws Throwable {
        if (generatingExpected()) {
            return expectedOutputManager.getRSession().eval(null, system2Command, null, true);
        } else {
            return fastROutputManager.fastRSession.eval(null, system2Command, null, true);
        }
    }

    /**
     * Evaluate expected output from {@code input}. By default the lookup is based on {@code input}
     * but can be overridden by providing a non-null {@code testIdOrNull}.
     */
    protected String expectedEval(String input, TestTrait... traits) {
        if (generatingExpected()) {
            // generation mode
            return genTestResult(input, traits);
        } else {
            // unit test mode
            String expected = expectedOutputManager.getOutput(input);
            if (expected == null) {
                // get the expected output dynamically (but do not update the file)
                expectedOutputManager.createRSession();
                expected = genTestResult(input);
                if (expected == null) {
                    expected = "<<NO EXPECTED OUTPUT>>";
                }
            }
            microTestInfo.expectedOutput = expected;
            return expected;
        }
    }

    private String genTestResult(String input, TestTrait... traits) {
        return expectedOutputManager.genTestResult(this, testElementName, input, localDiagnosticHandler, expectedOutputManager.checkOnly, keepTrailingWhiteSpace, traits);
    }

    /**
     * Creates array with all the combinations of parameters substituted in template. Substitution
     * is done via '%NUMBER', i.e. '%0' is replaced with all the values from the first array.
     */
    protected static String[] template(String template, String[]... parameters) {
        return TestOutputManager.template(template, parameters);
    }

    protected static String[] join(String[]... arrays) {
        return TestOutputManager.join(arrays);
    }

    /**
     * Tests that require additional {@link PolyglotEngine} global symbols should override this,
     * which will be called just prior to the evaluation.
     */
    public void addPolyglotSymbols(@SuppressWarnings("unused") PolyglotEngine.Builder builder) {
    }

    private static final LocalDiagnosticHandler localDiagnosticHandler = new LocalDiagnosticHandler();

    private static class LocalDiagnosticHandler implements TestOutputManager.DiagnosticHandler {
        private boolean quiet;

        @Override
        public void warning(String msg) {
            System.out.println("\nwarning: " + msg);
        }

        @Override
        public void note(String msg) {
            if (!quiet) {
                System.out.println("\nnote: " + msg);
            }
        }

        @Override
        public void error(String msg) {
            System.err.println("\nerror: " + msg);
        }

        void setQuiet() {
            quiet = true;
        }
    }

    protected static boolean deleteDir(Path dir) {
        try {
            Files.walkFileTree(dir, DELETE_VISITOR);
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    private static final class DeleteVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return del(file);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return del(dir);
        }

        private static FileVisitResult del(Path p) throws IOException {
            Files.delete(p);
            return FileVisitResult.CONTINUE;
        }
    }

    private static final DeleteVisitor DELETE_VISITOR = new DeleteVisitor();

    private static void addOutputHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (!generatingExpected()) {
                    WhiteList.report();
                }
                if (!unexpectedSuccessfulMicroTests.isEmpty()) {
                    System.out.println("Unexpectedly successful tests:");
                    for (String test : new TreeSet<>(unexpectedSuccessfulMicroTests)) {
                        System.out.println(test);
                    }
                }
                if (!exceptionCounts.isEmpty()) {
                    System.out.println("Exceptions encountered during test runs:");
                    for (Entry<String, Integer> entry : exceptionCounts.entrySet()) {
                        System.out.println(entry);
                    }
                }
                System.out.println("            tests | inputs");
                System.out.printf("successful: %6d | %6d%n", successfulTestCount, successfulInputCount);
                double successfulTestPercentage = 100 * successfulTestCount / (double) (successfulTestCount + failedTestCount + ignoredTestCount);
                double successfulInputPercentage = 100 * successfulInputCount / (double) (successfulInputCount + failedInputCount + ignoredInputCount);
                System.out.printf("            %5.1f%% | %5.1f%%%n", successfulTestPercentage, successfulInputPercentage);
                System.out.printf("   ignored: %6d | %6d%n", ignoredTestCount, ignoredInputCount);
                System.out.printf("    failed: %6d | %6d%n", failedTestCount, failedInputCount);
            }
        });
    }

    /**
     * Called before an actual evaluation happens.
     */
    public void beforeEval() {
        // empty
    }
}
