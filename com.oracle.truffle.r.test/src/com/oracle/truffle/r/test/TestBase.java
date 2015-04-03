/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.regex.*;

import static org.junit.Assert.fail;

import org.junit.*;
import org.junit.runner.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.test.generate.*;

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
    public static enum Output implements TestTrait {
        ContainsError,
        ContainsWarning,
        MayContainError,
        MayContainWarning
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
    private static ArrayList<String> failedMicroTests;

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
        if (!failedMicroTests.isEmpty()) {
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

    /**
     * The method to call when a micro-test fails.
     */
    protected static boolean assertTrue(boolean truth) {
        if (!truth) {
            microTestFailed();
        }
        return true;
    }

    private static void microTestFailed() {
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
        String context = String.format("%s(%s:%d)", culprit.getMethodName(), culprit.getClassName(), culprit.getLineNumber());
        System.err.printf("%nMicro-test failute: %s%n", context);
        System.err.printf("%16s %s%n", "Expression:", microTestInfo.expression);
        System.err.printf("%16s %s", "Expected output:", microTestInfo.expectedOutput);
        System.err.printf("%16s %s%n", "FastR output:", microTestInfo.fastROutput);

        failedMicroTests.add(context);
    }

    protected static boolean assertFalse() {
        assertTrue(false);
        return false;
    }

    private void evalAndCompare(String[] inputs, TestTrait... traits) {
        WhiteList[] whiteLists = TestTrait.collect(traits, WhiteList.class);
        int index = 1;
        for (String input : inputs) {
            String expected = expectedEval(input);
            if (!generatingExpected()) {
                String result = fastREval(input);

                if (!expected.equals(result)) {
                    boolean foundInWhitelist = false;
                    for (WhiteList list : whiteLists) {
                        WhiteList.Results wlr = list.get(input);
                        if (wlr != null) {
                            assertTrue(wlr.expected.equals(expected));
                            if (wlr.fastR.equals(result)) {
                                list.markUsed(input);
                                foundInWhitelist = true;
                                break;
                            }
                        }
                    }
                    if (!foundInWhitelist) {
                        boolean containsError = (!FULL_COMPARE_ERRORS && TestTrait.contains(traits, Output.ContainsError)) ||
                                        (TestTrait.contains(traits, Output.MayContainError) && expected.startsWith(ERROR));
                        boolean containsWarning = TestTrait.contains(traits, Output.ContainsWarning) || (TestTrait.contains(traits, Output.MayContainWarning) && expected.contains(WARNING));

                        boolean ok;
                        if (containsError && !assertTrue(result.startsWith(ERROR))) {
                            ok = false;
                        } else {
                            if (containsWarning) {
                                String resultWarning = getWarningMessage(result);
                                String expectedWarning = getWarningMessage(expected);
                                ok = assertTrue(resultWarning.equals(expectedWarning));
                                result = getOutputWithoutWarning(result);
                                expected = getOutputWithoutWarning(expected);
                            }
                            if (containsError) {
                                ok = checkMessageStripped(expected, result);
                            } else {
                                ok = assertTrue(expected.equals(result));
                            }
                        }
                        if (!ok) {
                            System.out.print('E');
                        }
                    }
                }
                afterMicroTest();
            }
            if ((index) % 100 == 0) {
                System.out.print('.');
            }
            index++;
        }
        if (!generatingExpected()) {
            for (WhiteList list : whiteLists) {
                list.report();
            }
        }
    }

    private static final Pattern warningPattern1 = Pattern.compile("^(?<pre>.*)Warning messages:\n1:(?<msg0>.*)\n2:(?<msg1>.*)\n3:(?<msg2>.*)\n4:(?<msg3>.*)$", Pattern.DOTALL);
    private static final Pattern warningPattern2 = Pattern.compile("^(?<pre>.*)Warning messages:\n1:(?<msg0>.*)\n2:(?<msg1>.*)\n3:(?<msg2>.*)$", Pattern.DOTALL);
    private static final Pattern warningPattern3 = Pattern.compile("^(?<pre>.*)Warning messages:\n1:(?<msg0>.*)\n2:(?<msg1>.*)$", Pattern.DOTALL);
    private static final Pattern warningPattern4 = Pattern.compile("^(?<pre>.*)Warning message:(?<msg0>.*)$", Pattern.DOTALL);

    private static final Pattern warningMessagePattern = Pattern.compile("^\n? ? ?(?:In .* :[ \n])?(?<m>[^\n]*)\n?$", Pattern.DOTALL);

    private static final Pattern[] warningPatterns = new Pattern[]{warningPattern1, warningPattern2, warningPattern3, warningPattern4};

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
        for (int i = 0; i < 4; i++) {
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
        assertTrue(cxr > 0 && cxe > 0);
        String resultStripped = stripWhitespace(result, cxr + 1);
        String expectedStripped = stripWhitespace(expected, cxe + 1);
        return assertTrue(resultStripped.equals(expectedStripped));
    }

    private static String stripWhitespace(String r, int ix) {
        int x = ix;
        int rl = r.length();
        char ch = r.charAt(x);
        if (Character.isWhitespace(ch)) {
            while (Character.isWhitespace(ch) && x < rl) {
                ch = r.charAt(x++);
            }
            x--;
        }
        return r.substring(x);
    }

    /**
     * Evaluate {@code input} in FastR, returning all (virtual) console output that was produced.
     */
    protected static String fastREval(String input) {
        microTestInfo.expression = input;
        String result = fastROutputManager.fastRSession.eval(input);
        if (fastROutputManager.outputFile != null) {
            fastROutputManager.addTestResult(testElementName, input, result);
        }
        microTestInfo.fastROutput = result;
        return TestOutputManager.prepareResult(result, keepTrailingWhiteSpace);
    }

    protected static boolean generatingExpected() {
        return expectedOutputManager.generate;
    }

    /**
     * Evaluate expected output from {@code input}. By default the lookup is based on {@code input}
     * but can be overridden by providing a non-null {@code testIdOrNull}.
     */
    protected static String expectedEval(String input) {
        assert !input.contains("\n") : "test input cannot contain newlines - not supported by ExpectedTestOutput.test file format";
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
                    assertTrue(false);
                    expected = "NO EXPECTED OUTPUT";
                }
            }
            microTestInfo.expectedOutput = expected;
            return expected;
        }
    }

    private static String genTestResult(String input) {
        return expectedOutputManager.genTestResult(testElementName, input, localDiagnosticHandler, expectedOutputManager.checkOnly, keepTrailingWhiteSpace);
    }

    protected static String[] template(String template, String[]... parameters) {
        return TestOutputManager.template(template, parameters);
    }

    protected static String[] join(String[]... arrays) {
        return TestOutputManager.join(arrays);
    }

    private static final LocalDiagnosticHandler localDiagnosticHandler = new LocalDiagnosticHandler();

    private static class LocalDiagnosticHandler implements TestOutputManager.DiagnosticHandler {
        private boolean quiet;

        public void warning(String msg) {
            System.out.println("\nwarning: " + msg);
        }

        public void note(String msg) {
            if (!quiet) {
                System.out.println("\nnote: " + msg);
            }
        }

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
}
