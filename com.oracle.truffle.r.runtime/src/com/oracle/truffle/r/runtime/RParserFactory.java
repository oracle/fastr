/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.util.List;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Abstracts the implementation of the R parser.
 */
public abstract class RParserFactory {
    public interface Parser {
        List<RSyntaxNode> script(Source source, RCodeBuilder<RSyntaxNode> builder, TruffleRLanguage language) throws ParseException;

        List<RSyntaxNode> statements(Source source, Source fullSource, int startLine, RCodeBuilder<RSyntaxNode> builder, TruffleRLanguage language) throws ParseException;
    }

    static {
        final String prop = System.getProperty("fastr.parser.factory.class", "com.oracle.truffle.r.parser.DefaultRParserFactory");
        try {
            theInstance = (RParserFactory) Class.forName(prop).getDeclaredConstructor().newInstance();
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

    public static Parser getParser() {
        return getInstance().createParser();
    }

    protected abstract Parser createParser();

}
