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

import com.oracle.truffle.r.test.TestBase;

/**
 * A non-interactive one-shot invocation of GnuR that is robust, if slow, in the face of
 * multiple-line output.
 */
public class GnuROneShotRSession implements RSession {

    protected static final String[] GNUR_COMMANDLINE = new String[]{"R", "--vanilla", "--slave", "--silent"};
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
