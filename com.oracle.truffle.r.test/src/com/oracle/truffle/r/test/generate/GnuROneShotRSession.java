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
package com.oracle.truffle.r.test.generate;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RVersionNumber;
import com.oracle.truffle.r.test.TestBase;

/**
 * A non-interactive one-shot invocation of GnuR that is robust, if slow, in the face of
 * multiple-line output.
 *
 * Ideally we would use the version of GnuR internal to FastR to ensure consistency, but there are
 * currently some differences in behavior (TBD). Which R is used is controlled by the environment
 * variable {@code FASTR_TESTGEN_GNUR}. If unset, we take the default, which is currently the system
 * installed version (more precisely whatever "R" resolves to on the PATH). If
 * {@code FASTR_TESTGEN_GNUR} is set to {@code internal}, we use the internally built GnuR. Any
 * other value behaves as if it was unset. {@code PATH}.
 */
public class GnuROneShotRSession implements RSession {

    private static final String[] GNUR_COMMANDLINE = new String[]{"R", "--vanilla", "--slave", "--silent"};
    private static final String SYSTEM_GNUR_ENV = "FASTR_TESTGEN_GNUR";
    private static final String NATIVE_PROJECT = "com.oracle.truffle.r.native";

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
        String testGenGnuR = System.getenv(SYSTEM_GNUR_ENV);
        if (testGenGnuR != null && testGenGnuR.equals("internal")) {
            Path gnuRPath = FileSystems.getDefault().getPath(REnvVars.rHome(), NATIVE_PROJECT, "gnur", "R-" + RVersionNumber.FULL, "bin", "R");
            GNUR_COMMANDLINE[0] = gnuRPath.toString();
        }
        ProcessBuilder pb = new ProcessBuilder(GNUR_COMMANDLINE);
        // fix time zone to "CET" (to create consistent expected output)
        pb.environment().put("TZ", "CET");
        pb.environment().remove("R_HOME"); // don't confuse GnuR with FastR!
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getOutputStream().write(GNUR_OPTIONS.getBytes());
        return p;
    }

    protected String readAvailable(InputStream gnuRoutput) throws IOException {
        int n = gnuRoutput.available();
        byte[] data = new byte[n];
        gnuRoutput.read(data);
        return new String(data);
    }

    public String eval(String expression) {
        if (expression.contains("library(") && !TestBase.generatingExpected()) {
            System.out.println("==============================================");
            System.out.println("LIBRARY LOADING WHILE CREATING EXPECTED OUTPUT");
            System.out.println("creating expected output for these tests only works during test output");
            System.out.println("generation (mx rtestgen), and will otherwise create corrupted output.");
        }
        try {
            Process p = createGnuR();
            InputStream gnuRoutput = p.getInputStream();
            OutputStream gnuRinput = p.getOutputStream();
            send(gnuRinput, expression.getBytes(), NL, QUIT);
            p.waitFor();
            return readAvailable(gnuRoutput);
        } catch (IOException | InterruptedException ex) {
            System.err.print("exception: " + ex);
            return null;
        }

    }

    protected void send(OutputStream gnuRinput, byte[]... data) throws IOException {
        for (byte[] d : data) {
            gnuRinput.write(d);
        }
        gnuRinput.flush();
    }

    public String name() {
        return "GnuR one-shot";
    }
}
