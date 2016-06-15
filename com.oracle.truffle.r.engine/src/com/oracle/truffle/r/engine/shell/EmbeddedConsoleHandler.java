/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.shell;

import com.oracle.truffle.r.runtime.RInterfaceCallbacks;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * In embedded mode the console functions as defined in {@code Rinterface.h} can be overridden. This
 * class supports that, delegating to a standard console handler if not redirected.
 *
 */
public class EmbeddedConsoleHandler implements ConsoleHandler {

    private ConsoleHandler delegate;
    private REmbedRFFI rEmbedRFFI;

    EmbeddedConsoleHandler(ConsoleHandler delegate) {
        this.delegate = delegate;
    }

    private REmbedRFFI getREmbedRFFI() {
        if (rEmbedRFFI == null) {
            rEmbedRFFI = RFFIFactory.getRFFI().getREmbedRFFI();
        }
        return rEmbedRFFI;
    }

    @Override
    public void println(String s) {
        if (RInterfaceCallbacks.R_WriteConsole.isOverridden()) {
            getREmbedRFFI().writeConsole(s);
            getREmbedRFFI().writeConsole("\n");
        } else {
            delegate.println(s);
        }

    }

    @Override
    public void print(String s) {
        if (RInterfaceCallbacks.R_WriteConsole.isOverridden()) {
            getREmbedRFFI().writeConsole(s);
        } else {
            delegate.print(s);
        }

    }

    @Override
    public void printErrorln(String s) {
        if (RInterfaceCallbacks.R_WriteConsole.isOverridden()) {
            getREmbedRFFI().writeErrConsole(s);
            getREmbedRFFI().writeErrConsole("\n");
        } else {
            delegate.printErrorln(s);
        }

    }

    @Override
    public void printError(String s) {
        if (RInterfaceCallbacks.R_WriteConsole.isOverridden()) {
            getREmbedRFFI().writeErrConsole(s);
        } else {
            delegate.printError(s);
        }

    }

    @Override
    public String readLine() {
        if (RInterfaceCallbacks.R_ReadConsole.isOverridden()) {
            return getREmbedRFFI().readConsole(delegate.getPrompt());
        } else {
            return delegate.readLine();
        }
    }

    @Override
    public boolean isInteractive() {
        return delegate.isInteractive();
    }

    @Override
    public String getPrompt() {
        return delegate.getPrompt();
    }

    @Override
    public void setPrompt(String prompt) {
        delegate.setPrompt(prompt);

    }

    @Override
    public int getWidth() {
        return delegate.getWidth();
    }

    @Override
    public String getInputDescription() {
        return delegate.getInputDescription();
    }

}
