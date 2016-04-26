/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
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

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RPerfStats;
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

    public enum Output implements TestTrait {
        ContainsError, // the error context is ignored (e.g., "a+b" vs. "a + b")
        ContainsAmbiguousError, // the actual error message is ignored
        ContainsWarning, // the warning context is ignored
        MayContainError,
        MayContainWarning;
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

        public String getDescription() {
            return description;
        }
    }

    public enum Context implements TestTrait {
        NonShared; // Test requires a new non-shared {@link RContext}.
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
        private static final String KEEP_TRAILING_WHITESPACEG = "keep-trailing-whitespace";
        private static final String TEST_METHODS = "test-methods=";

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
                        } else if (directive.equals(KEEP_TRAILING_WHITESPACEG)) {
                            keepTrailingWhiteSpace = true;
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
                            System.err.println("Test file:" + expectedOutputManager.outputFile + " is out of sync with unit tests");
                            Utils.exit(1);
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
                RPerfStats.report();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }

        @Override
        public void testStarted(Description description) {
            testElementName = description.getClassName() + "." + description.getMethodName();
            failedMicroTests = new ArrayList<>();
        }
    }

    @Before
    public void beforeTest() {
        if (expectedOutputManager == null) {
            // assume we are running a unit test in an IDE and the RunListener was not invoked.
            // In this case we can expect the test output file to exist and open it as a resource
            URL expectedTestOutputURL = ResourceHandlerFactory.getHandler().getResource(TestBase.class, TestOutputManager.TEST_EXPECTED_OUTPUT_FILE);
            if (expectedTestOutputURL == null) {
                Assert.fail("cannot find " + TestOutputManager.TEST_EXPECTED_OUTPUT_FILE + " resource");
            } else {
                try {
                    expectedOutputManager = new ExpectedTestOutputManager(new File(expectedTestOutputURL.getPath()), false, false, false);
                    fastROutputManager = new FastRTestOutputManager(null);
                } catch (IOException ex) {
                    Assert.fail("error reading: " + expectedTestOutputURL.getPath() + ": " + ex);
                }
            }
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

    protected static final String ERROR = "Error";
    protected static final String WARNING = "Warning message";

    /**
     * If this is set to {@code true}, {@link Output#ContainsError} will compare the full output
     * instead of truncating leading "Error" strings and such. This means it will behave like
     * {@link #assertEval}.
     */
    private static final boolean FULL_COMPARE_ERRORS = false;

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
        evalAndCompare(new String[]{"if (length(grep(\"FastR\", R.Version()$version.string)) != 1) { " + gnuROutput + " } else " + input});
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

    /**
     * Return a path that is relative to the cwd when running tests.
     */
    public static Path relativize(Path path) {
        if (cwd == null) {
            cwd = Paths.get(System.getProperty("user.dir"));
        }
        return cwd.relativize(path);
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
        // We want the stack trace as if the JUnit test failed
        RuntimeException ex = new RuntimeException();
        // The first method not in TestBase is the culprit
        StackTraceElement culprit = null;
        for (StackTraceElement se : ex.getStackTrace()) {
            if (!se.getClassName().endsWith("TestBase")) {
                culprit = se;
                break;
            }
        }
        String context = String.format("%s:%d (%s)", culprit.getClassName(), culprit.getLineNumber(), culprit.getMethodName());
        return context;
    }

    private void evalAndCompare(String[] inputs, TestTrait... traits) {
        WhiteList[] whiteLists = TestTrait.collect(traits, WhiteList.class);

        boolean ignored = TestTrait.contains(traits, Ignored.class) ^ (ProcessFailedTests && !(TestTrait.contains(traits, Ignored.Unstable) || TestTrait.contains(traits, Ignored.SideEffects)));

        boolean containsWarning = TestTrait.contains(traits, Output.ContainsWarning);
        boolean containsError = (!FULL_COMPARE_ERRORS && (TestTrait.contains(traits, Output.ContainsError) || TestTrait.contains(traits, Output.ContainsAmbiguousError)));
        boolean mayContainWarning = TestTrait.contains(traits, Output.MayContainWarning);
        boolean mayContainError = TestTrait.contains(traits, Output.MayContainError);
        boolean ambiguousError = TestTrait.contains(traits, Output.ContainsAmbiguousError);
        boolean nonSharedContext = TestTrait.contains(traits, Context.NonShared);

        ContextInfo contextInfo = nonSharedContext ? fastROutputManager.fastRSession.createContextInfo(ContextKind.SHARE_NOTHING) : null;

        int index = 1;
        boolean allOk = true;
        for (String input : inputs) {
            String expected = expectedEval(input);
            if (ignored || generatingExpected()) {
                ignoredInputCount++;
            } else {
                String result = fastREval(input, contextInfo);

                CheckResult checkResult = checkResult(whiteLists, input, expected, result, containsWarning, mayContainWarning, containsError, mayContainError, ambiguousError);

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
                    System.out.print('E');
                }
                allOk &= ok;
                afterMicroTest();
            }
            if ((index) % 100 == 0) {
                System.out.print('.');
            }
            index++;
        }
        if (ignored) {
            ignoredTestCount++;
        } else if (allOk) {
            successfulTestCount++;
        } else {
            failedTestCount++;
        }
        if (!generatingExpected()) {
            for (WhiteList list : whiteLists) {
                list.report();
            }
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

    private CheckResult checkResult(WhiteList[] whiteLists, String input, String originalExpected, String originalResult, boolean containsWarning, boolean mayContainWarning, boolean containsError,
                    boolean mayContainError, boolean ambiguousError) {
        boolean ok;
        String result = originalResult;
        String expected = originalExpected;
        if (input.equals("c(1i,1i,1i)/(-(1/0))")) {
            System.console();
        }
        if (expected.equals(result) || searchWhiteLists(whiteLists, input, expected, result, containsWarning, mayContainWarning, containsError, mayContainError, ambiguousError)) {
            ok = true;
            if (containsError && !ambiguousError) {
                System.out.println("unexpected correct error message: " + getTestContext());
            }
            if (containsWarning) {
                System.out.println("unexpected correct warning message: " + getTestContext());
            }
        } else {
            if (containsWarning || (mayContainWarning && expected.contains(WARNING))) {
                String resultWarning = getWarningMessage(result);
                String expectedWarning = getWarningMessage(expected);
                ok = resultWarning.equals(expectedWarning);
                result = getOutputWithoutWarning(result);
                expected = getOutputWithoutWarning(expected);
            } else {
                ok = true;
            }
            if (ok) {
                if (containsError || (mayContainError && expected.startsWith(ERROR))) {
                    ok = result.startsWith(ERROR) && (ambiguousError || checkMessageStripped(expected, result));
                } else {
                    ok = expected.equals(result);
                }
            }
        }
        return new CheckResult(ok, result, expected);
    }

    private boolean searchWhiteLists(WhiteList[] whiteLists, String input, String expected, String result, boolean containsWarning, boolean mayContainWarning, boolean containsError,
                    boolean mayContainError, boolean ambiguousError) {
        if (whiteLists == null) {
            return false;
        }
        for (WhiteList list : whiteLists) {
            WhiteList.Results wlr = list.get(input);
            if (wlr != null) {
                // Sanity check that "expected" matches the entry in the WhiteList
                CheckResult checkedResult = checkResult(null, input, wlr.expected, expected, containsWarning, mayContainWarning, containsError, mayContainError, ambiguousError);
                if (!checkedResult.ok) {
                    System.out.println("expected output does not match: " + wlr.expected + " vs. " + expected);
                    return false;
                }
                // Substitute the FastR output and try to match that
                CheckResult fastRResult = checkResult(null, input, wlr.fastR, result, containsWarning, mayContainWarning, containsError, mayContainError, ambiguousError);
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

    private static final Pattern warningMessagePattern = Pattern.compile("^\n? ? ?(?:In .* :[ \n])?(?<m>[^\n]*)\n?$", Pattern.DOTALL);

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
                assert messageMatcher.matches() : "unexpected format in warning message: " + message;
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
        int cxr = result.lastIndexOf(':');
        int cxe = expected.lastIndexOf(':');
        if (cxr < 0 || cxe < 0) {
            return false;
        }
        String resultStripped = result.substring(cxr + 1).trim();
        String expectedStripped = expected.substring(cxe + 1).trim();
        return resultStripped.equals(expectedStripped);
    }

    /**
     * Evaluate {@code input} in FastR, returning all (virtual) console output that was produced. If
     * {@code nonShared} then this must evaluate in a new, non-shared, {@link RContext}.
     */
    protected static String fastREval(String input, ContextInfo contextInfo) {
        microTestInfo.expression = input;
        String result;
        try {
            result = fastROutputManager.fastRSession.eval(input, contextInfo);
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
            fastROutputManager.addTestResult(testElementName, input, result);
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
     * Evaluate expected output from {@code input}. By default the lookup is based on {@code input}
     * but can be overridden by providing a non-null {@code testIdOrNull}.
     */
    protected static String expectedEval(String input) {
        if (generatingExpected()) {
            // generation mode
            return genTestResult(input);
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

    private static String genTestResult(String input) {
        return expectedOutputManager.genTestResult(testElementName, input, localDiagnosticHandler, expectedOutputManager.checkOnly, keepTrailingWhiteSpace);
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
        } catch (Exception e) {
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

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
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
}
