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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.r.runtime.*;

/**
 * A simple wrapper class that represents a symbol that might me read or written to.
 */
public final class Symbol {
    /**
     * The {@link Symbol}s identifier.
     */
    private final String name;

    /**
     * Whether this is "...".
     */
    private final boolean isVarArg;

    /**
     * Whether this is "..n".
     */
    private final boolean isVarArgGetter;

    /**
     * @param symbolStr
     * @return A fresh instance of {@link Symbol} containing the given {@link #name}
     */
    public static Symbol create(String symbolStr) {
        return new Symbol(symbolStr);
    }

    /**
     * @param symbolObj
     * @return A fresh instance of {@link Symbol} containing the given Object's {@link #toString()}
     */
    public static Symbol create(Object symbolObj) {
        return new Symbol(RRuntime.toString(symbolObj));
    }

    /**
     * @param name {@link #name}
     * @see Symbol
     */
    private Symbol(String name) {
        this.name = name;

        this.isVarArg = name.equals("...");
        this.isVarArgGetter = !isVarArg && name.startsWith("..");
    }

    /**
     * @return {@link #isVarArg}
     */
    public boolean isVarArg() {
        return isVarArg;
    }

    /**
     * @return {@link #isVarArgGetter}
     */
    public boolean isVarArgGetter() {
        return isVarArgGetter;
    }

    /**
     * @return {@link #name}
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
