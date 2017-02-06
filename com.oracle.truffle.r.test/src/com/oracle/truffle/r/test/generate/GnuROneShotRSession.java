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
package com.oracle.truffle.r.test.generate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.r.runtime.ProcessOutputManager;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RVersionNumber;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.test.TestBase;

/**
 * A non-interactive one-shot invocation of GnuR that is robust, if slow, in the face of
 * multiple-line output.
 *
 * By default, we use the version of GnuR internal to FastR to ensure version consistency. Which R
 * is used is controlled by the environment variable {@code FASTR_TESTGEN_GNUR}. If unset, or set to
 * 'internal', we take the default, otherwise the value is treated as a path to a directory assumed
 * to be an R HOME, i.e the executable used is {@code $FASTR_TESTGEN_GNUR/bin/R}.
 */
public class GnuROneShotRSession implements RSession {

    private static final String[] GNUR_COMMANDLINE = new String[]{"<R>", "--vanilla", "--slave", "--silent", "--no-restore"};
    private static final String FASTR_TESTGEN_GNUR = "FASTR_TESTGEN_GNUR";
    private static final String NATIVE_PROJECT = "com.oracle.truffle.r.native";
    private static final int DEFAULT_TIMEOUT_MINS = 5;
    private static int timeoutMins = DEFAULT_TIMEOUT_MINS;

    //@formatter:off
    protected static final String GNUR_OPTIONS =
                    "options(echo=FALSE)\n" +
                    "options(warn=FALSE)\n" +
                    "options(error=dump.frames)\n" +
                    "options(showErrorCalls=FALSE)\n" +
                    "options(keep.source=TRUE)\n" +
                    "Sys.setenv(LANGUAGE=\"EN\");";
    //@formatter:on

    protected static final byte[] NL = "\n".getBytes();
    protected static byte[] QUIT = "q()\n".getBytes();

    protected Process createGnuR() throws IOException {
        String timeout = System.getenv("FASTR_TESTGEN_TIMEOUT");
        if (timeout != null) {
            try {
                timeoutMins = Integer.parseInt(timeout);
            } catch (NumberFormatException ex) {
                System.err.println("ignoring invalid value for FASTR_TESTGEN_TIMEOUT");
            }
        }
        String testGenGnuR = System.getenv(FASTR_TESTGEN_GNUR);
        if (testGenGnuR == null || testGenGnuR.equals("internal")) {
            Path gnuRPath = FileSystems.getDefault().getPath(REnvVars.rHome(), NATIVE_PROJECT, "gnur", RVersionNumber.R_HYPHEN_FULL, "bin", "R");
            GNUR_COMMANDLINE[0] = gnuRPath.toString();
        } else {
            GNUR_COMMANDLINE[0] = FileSystems.getDefault().getPath(testGenGnuR, "bin", "R").toString();
        }

        ProcessBuilder pb = new ProcessBuilder(GNUR_COMMANDLINE);
        // fix time zone to "GMT" (to create consistent expected output)
        pb.environment().put("TZ", "GMT");
        pb.environment().remove("R_HOME"); // don't confuse GnuR with FastR!
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getOutputStream().write(GNUR_OPTIONS.getBytes());
        return p;
    }

    @Override
    public String eval(TestBase testBase, String expression, ContextInfo contextInfo, boolean longTimeout) throws Throwable {
        if (expression.contains("library(") && !TestBase.generatingExpected()) {
            System.out.println("==============================================");
            System.out.println("LIBRARY LOADING WHEN NOT GENERATING EXPECTED OUTPUT");
            System.out.println("creating expected output for these tests only works during test output");
            System.out.println("generation (mx rtestgen), and will otherwise create corrupted output.");
        }
        Process p = createGnuR();
        InputStream gnuRoutput = p.getInputStream();
        OutputStream gnuRinput = p.getOutputStream();
        ProcessOutputManager.OutputThreadVariable readThread = new ProcessOutputManager.OutputThreadVariable("gnur eval", gnuRoutput);
        readThread.start();
        send(gnuRinput, expression.getBytes(), NL, QUIT);
        int thisTimeout = longTimeout ? timeoutMins * 2 : timeoutMins;
        if (!p.waitFor(thisTimeout, TimeUnit.MINUTES)) {
            throw new RuntimeException(String.format("GNU R process timed out on: '%s'\n", expression));
        }
        readThread.join();
        return new String(readThread.getData(), 0, readThread.getTotalRead());

    }

    protected void send(OutputStream gnuRinput, byte[]... data) throws IOException {
        for (byte[] d : data) {
            gnuRinput.write(d);
        }
        gnuRinput.flush();
    }

    @Override
    public String name() {
        return "GnuR one-shot";
    }
}
