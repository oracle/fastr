/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.launcher;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;

import com.oracle.truffle.r.launcher.RCmdOptions.Client;
import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;

public abstract class RAbstractLauncher extends AbstractLanguageLauncher implements Closeable {

    private final Client client;
    protected final InputStream inStream;
    protected final OutputStream outStream;
    protected final OutputStream errStream;
    protected RCmdOptions options;
    private boolean useJVM;
    protected ConsoleHandler consoleHandler;
    protected Context context;

    RAbstractLauncher(Client client, String[] env, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        this.client = client;
        assert env == null : "re-enble environment variables";
        this.inStream = inStream;
        this.outStream = outStream;
        this.errStream = errStream;
    }

    @Override
    protected List<String> preprocessArguments(List<String> arguments, Map<String, String> polyglotOptions) {
        boolean[] recognizedArgsIndices = new boolean[arguments.size()];
        this.options = RCmdOptions.parseArguments(client, arguments.toArray(new String[arguments.size()]), true, recognizedArgsIndices);
        List<String> unrecognizedArgs = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            if ("--jvm".equals(arguments.get(i))) {
                useJVM = true;
            } else if (!recognizedArgsIndices[i]) {
                unrecognizedArgs.add(arguments.get(i));
            }
        }

        return unrecognizedArgs;
    }

    protected abstract String[] getArguments();

    @Override
    protected void launch(Builder contextBuilder) {
        this.consoleHandler = RCommand.createConsoleHandler(options, null, inStream, outStream);
        this.context = contextBuilder.allowAllAccess(true).allowHostAccess(useJVM).arguments("R", getArguments()).in(consoleHandler.createInputStream()).out(
                        outStream).err(errStream).build();
        this.consoleHandler.setContext(context);
    }

    @Override
    protected String getLanguageId() {
        return "R";
    }

    @Override
    protected void printHelp(OptionCategory maxCategory) {
        RCmdOptions.printHelp(client);
    }

    @Override
    protected void collectArguments(Set<String> opts) {
        for (RCmdOption option : RCmdOption.values()) {
            if (option.shortName != null) {
                opts.add(option.shortName);
            }
            if (option.plainName != null) {
                opts.add(option.plainName);
            }
        }
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
        }
    }
}
