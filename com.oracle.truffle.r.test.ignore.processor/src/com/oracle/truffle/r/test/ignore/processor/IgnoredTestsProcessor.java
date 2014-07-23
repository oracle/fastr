/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.ignore.processor;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("org.junit.Ignore")
/**
 * Audits the set of units tests, updating the {@code IgnoredTests} class so that each micro-test in a unit test is
 * represented as one {@code JUnit} test, allowing it to be un-ignored and debugged easily. It also creates a file
 * {@code AllTests} that contains every test as a single JUnit test, whether ignored or not.
 */
public class IgnoredTestsProcessor extends AbstractProcessor {
    private/* Trees */Object treesObj;
    private boolean inEclipse;

    private static class CallAndArg implements Comparable<CallAndArg> {
        final String call;
        final String arg;
        final boolean isIgnored;

        CallAndArg(String call, String arg, boolean isIgnored) {
            this.call = call;
            this.arg = arg;
            this.isIgnored = isIgnored;
        }

        @Override
        public String toString() {
            return call + "(\"" + arg + "\")";
        }

        public int compareTo(CallAndArg o) {
            int callCompare = call.compareTo(o.call);
            if (callCompare == 0) {
                return arg.compareTo(o.call);
            } else {
                return callCompare;
            }
        }
    }

    private static SortedMap<String, SortedSet<CallAndArg>> testMap = new TreeMap<>();
    private static final String IGNORED_TESTS = "FailingTests";
    private static final String ALL_TESTS = "AllTests";
    private static final String[] TEST_FILES = new String[]{IGNORED_TESTS, ALL_TESTS};

    @Override
    public synchronized void init(ProcessingEnvironment pe) {
        super.init(pe);
        try {
            Class<?> treesClass = Class.forName("com.sun.source.util.Trees");
            Method treesInstanceMethod = treesClass.getMethod("instance", ProcessingEnvironment.class);
            this.treesObj = treesInstanceMethod.invoke(null, pe);
        } catch (ClassNotFoundException ex) {
            note("Test output processor cannot run in Eclipse");
            inEclipse = true;
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException ex) {
            inEclipse = true;
            error("error creating/invoking Trees.instance");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (inEclipse) {
            return true;
        }
        if (roundEnv.processingOver()) {
            return true;
        }
        try {
            note("TestProcessor: analyzing tests");
            for (Element element : roundEnv.getElementsAnnotatedWith(org.junit.Test.class)) {
                Element classElement = element.getEnclosingElement();
                String testClassName = classElement.getSimpleName().toString();
                if (testClassName.startsWith(IGNORED_TESTS) || testClassName.startsWith(ALL_TESTS)) {
                    continue;
                }
                Trees trees = (Trees) this.treesObj;
                new TestAnalyzerTreeVisitor(element).scan(trees.getPath(element), trees);
            }
            File srcDir = getSrcDir(processingEnv.getFiler());
            for (String testFile : TEST_FILES) {
                File outputFileDir = outputFileDir(srcDir, testFile.replace("Tests", "").toLowerCase());
                String oldContent = readTestsFile(outputFileDir, testFile);
                generateTestsFile(outputFileDir, testFile, testFile.equals(IGNORED_TESTS), oldContent);
            }
        } catch (Exception ex) {
            error("error generating " + IGNORED_TESTS + ": " + ex);
            StackTraceElement[] elements = ex.getStackTrace();
            for (StackTraceElement element : elements) {
                error(element.toString());
            }
        }
        return false;
    }

    private static String readTestsFile(File outputFileDir, String className) throws IOException {
        File file = new File(outputFileDir, className + ".java");
        if (!file.exists()) {
            return null;
        }
        byte[] data = new byte[(int) file.length()];
        try (BufferedInputStream bs = new BufferedInputStream(new FileInputStream(file))) {
            bs.read(data);
        }
        return new String(data);
    }

    /**
     * Called twice, with {@code ignore === false} for included tests and {@code ignore === true}
     * for those annotated with {@code Ignore}. N.B. The duplicate checking currently does not work
     * across both variants, so when a test annotation is changed, duplicates may be reported.
     */
    private void generateTestsFile(File outputFileDir, String className, boolean ignore, String oldContent) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        StringWriter swr = new StringWriter();
        PrintWriter wr = new PrintWriter(swr);
        String packageName = className.replace("Tests", "").toLowerCase();
        String annKind = ignore ? "Ignore" : "Test";
        wr.println("// DO NOT EDIT, update using 'mx rignoredtests'");
        wr.printf("// This contains a copy of the @%s tests one micro-test per method%n", annKind);
        wr.printf("package com.oracle.truffle.r.test.%s;%n%n", packageName);
        wr.printf("import org.junit.%s;%n%n", annKind);
        wr.println("import com.oracle.truffle.r.test.*;\n");
        wr.println("//Checkstyle: stop");
        wr.printf("public class %s extends TestBase {%n", className);
        boolean duplicates = false;
        Map<String, String> methodTags = new HashMap<>();
        for (Map.Entry<String, SortedSet<CallAndArg>> entrySet : testMap.entrySet()) {
            String methodName = entrySet.getKey();
            String methodNameTrans = methodName.replace('.', '_');
            for (CallAndArg testCall : entrySet.getValue()) {
                if (ignore && !testCall.isIgnored) {
                    continue;
                }
                byte[] uniq = digest.digest(testCall.arg.getBytes());
                String tag = hexBytes(uniq);
                String previous = methodTags.get(tag);
                if (previous != null) {
                    String errMsg = String.format("duplicate test in %s: '%s', initially seen in %s", methodName, testCall.arg, previous);
                    error(errMsg);
                    duplicates = true;
                } else {
                    methodTags.put(tag, methodName);
                }
                wr.printf("    @%s%n", ignore ? "Ignore" : "Test");
                wr.printf("    public void %s_%s() {%n", methodNameTrans, tag);
                wr.printf("        %s;%n", testCall);
                wr.println("    }\n");
            }
        }
        wr.println("}\n");
        if (duplicates) {
            return;
        }
        String newContent = swr.getBuffer().toString();
        if (oldContent == null || !newContent.equals(oldContent)) {
            File file = new File(outputFileDir, className + ".java");
            try (BufferedWriter bwr = new BufferedWriter(new FileWriter(file))) {
                bwr.write(newContent);
            }
            note("updated " + file.getAbsolutePath());
        }
    }

    private static String hexBytes(byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static File outputFileDir(File srcDir, String packageName) {
        return new File(srcDir, "com/oracle/truffle/r/test/" + packageName);
    }

    /**
     * Find out the src directory.
     */
    private static File getSrcDir(Filer filer) throws IOException {
        // The locator file is not actually created because we don't create a stream to it.
        FileObject locator = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "locator");
        return new File(new File(locator.toUri().getPath()).getParentFile().getParentFile(), "src");
    }

    private void diagnostic(Diagnostic.Kind kind, String msg) {
        processingEnv.getMessager().printMessage(kind, msg);
    }

    public void note(String msg) {
        diagnostic(Diagnostic.Kind.NOTE, msg);
    }

    private void error(String msg) {
        diagnostic(Diagnostic.Kind.ERROR, msg);
    }

    private static String getFullName(Element element) {
        Element classElement = element.getEnclosingElement();
        String testClassName = classElement.getSimpleName().toString();
        return testClassName + "." + element.getSimpleName().toString();
    }

    private static void addTest(Element element, String testCall, String testArg) {
        Map<String, SortedSet<CallAndArg>> map = testMap;
        String elementName = getFullName(element);
        SortedSet<CallAndArg> tests = map.get(elementName);
        if (tests == null) {
            tests = new TreeSet<>();
            map.put(elementName, tests);
        }

        tests.add(new CallAndArg(testCall, testArg.replace("\"", "\\\"").replace("\n", "\\n"), element.getAnnotation(org.junit.Ignore.class) != null));
    }

    private class TestAnalyzerTreeVisitor extends TreePathScanner<Object, Trees> {
        private final Element element;

        TestAnalyzerTreeVisitor(Element element) {
            this.element = element;
        }

        @Override
        public Object visitMethodInvocation(MethodInvocationTree methodInvocationTree, Trees trees) {
            if (methodInvocationTree.getMethodSelect() instanceof IdentifierTree) {
                String callName = getMethodCallName(methodInvocationTree);
                if (callName.startsWith("assertEval")) {
                    List<? extends ExpressionTree> args = methodInvocationTree.getArguments();
                    String test = getStringLiteral(args.get(0));
                    addTest(element, callName, test);
                }
            }
            return super.visitMethodInvocation(methodInvocationTree, trees);
        }

        String getMethodCallName(Tree methodInvocationTree) {
            IdentifierTree identifierTree = (IdentifierTree) ((MethodInvocationTree) methodInvocationTree).getMethodSelect();
            return identifierTree.getName().toString();
        }

        private String getStringLiteral(Tree t) {
            return (String) ((LiteralTree) t).getValue();
        }
    }

}
