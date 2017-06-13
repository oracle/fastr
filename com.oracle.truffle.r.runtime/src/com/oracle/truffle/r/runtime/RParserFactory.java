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
package com.oracle.truffle.r.runtime;

import java.util.List;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;

/**
 * Abstracts the implementation of the R parser.
 */
public abstract class RParserFactory {
    public interface Parser<T> {
        List<T> script(Source source, RCodeBuilder<T> builder, TruffleRLanguage language) throws ParseException;

        RootCallTarget rootFunction(Source source, String name, RCodeBuilder<T> builder, TruffleRLanguage language) throws ParseException;

        boolean isRecognitionException(Throwable t);

        int line(Throwable t);

        int charPositionInLine(Throwable t);
    }

    static {
        final String prop = System.getProperty("fastr.parser.factory.class", "com.oracle.truffle.r.parser.DefaultRParserFactory");
        try {
            theInstance = (RParserFactory) Class.forName(prop).newInstance();
        } catch (Exception ex) {
            // CheckStyle: stop system..print check
            System.err.println("Failed to instantiate class: " + prop);
            System.exit(1);
        }
    }

    private static RParserFactory theInstance;

    private static RParserFactory getInstance() {
        return theInstance;
    }

    public static <T> Parser<T> getParser() {
        return getInstance().createParser();
    }

    protected abstract <T> Parser<T> createParser();

}
