/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.junit.internal.JUnitSystem;
import org.junit.internal.RealSystem;
import org.junit.internal.TextListener;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import junit.runner.Version;

public class FastRJUnitWrapper {
    // CheckStyle: stop system..print check

    /**
     * @param args args[0] is the path where to read the names of the testclasses.
     */
    public static void main(String[] args) {
        String testsFile = null;
        String runListenerClassName = null;
        String testClassName = null;
        List<Failure> missingClasses = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            final String arg = args[i];
            switch (arg) {
                case "--testsfile":
                    testsFile = getNextArg(args, ++i);
                    break;
                case "--runlistener":
                    runListenerClassName = getNextArg(args, ++i);
                    break;
                case "--testclass":
                    testClassName = getNextArg(args, ++i);
                    break;
                default:
                    usage();
            }
            i++;
        }
        RunListener runListener = null;
        String runListenerArg = null;
        if (runListenerClassName != null) {
            int cx = runListenerClassName.indexOf(':');
            if (cx > 0) {
                runListenerArg = runListenerClassName.substring(cx + 1);
                runListenerClassName = runListenerClassName.substring(0, cx);
            }
            try {
                Class<?> runListenerClass = Class.forName(runListenerClassName);
                if (runListenerArg == null) {
                    runListener = (RunListener) runListenerClass.newInstance();
                } else {
                    Constructor<?> cons = runListenerClass.getDeclaredConstructor(String.class);
                    runListener = (RunListener) cons.newInstance(runListenerArg);
                }
            } catch (Exception ex) {
                System.err.println("error instantiating: " + runListenerClassName + ": " + ex);
            }
        }
        if (testsFile == null && testClassName == null) {
            usage();
        }

        Class<?>[] classArgs = null;
        String[] stringArgs = null;
        try {
            if (testClassName != null) {
                classArgs = new Class<?>[1];
                classArgs[0] = Class.forName(testClassName);
                stringArgs = new String[1];
                stringArgs[0] = testClassName;
            } else if (testsFile != null) {

                ArrayList<Class<?>> tests = new ArrayList<>(1000);
                try (BufferedReader br = new BufferedReader(new FileReader(testsFile))) {
                    while ((testClassName = br.readLine()) != null) {
                        tests.add(Class.forName(testClassName));
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.exit(2);
                }

                classArgs = new Class<?>[tests.size()];
                tests.toArray(classArgs);
                stringArgs = new String[tests.size()];
                for (int j = 0; j < stringArgs.length; j++) {
                    stringArgs[j] = classArgs[j].getName();
                }
            }
        } catch (ClassNotFoundException ex) {
            System.out.println("Could not find class: " + testClassName);
            Description description = Description.createSuiteDescription(testClassName);
            Failure failure = new Failure(description, ex);
            missingClasses.add(failure);
        }
        if (classArgs.length == 1) {
            System.out.printf("executing junit test now... (%s)\n", classArgs[0]);
        } else {
            System.out.printf("executing junit tests now... (%d test classes)\n", classArgs.length);
        }
        // It is very strange that all this boilerplate is necessary to get the same effect as
        // JUnitCore.main
        JUnitSystem system = new RealSystem();
        System.out.println("JUnit version " + Version.id());
        JUnitCore core = new JUnitCore();
        core.addListener(new TextListener(system));
        core.addListener(runListener);
        Result result = core.run(classArgs);
        for (Failure each : missingClasses) {
            result.getFailures().add(each);
        }
        System.exit(result.wasSuccessful() ? 0 : 1);
    }

    private static String getNextArg(String[] args, int i) {
        if (i < args.length) {
            return args[i];
        } else {
            usage();
            return null;
        }
    }

    private static void usage() {
        System.err.println("usage: [--testsfile file] [--test testclass] [--runlistener runlistenerclass[:arg]]");
        System.exit(1);
    }
}
