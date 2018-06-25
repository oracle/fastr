/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.RType;

/**
 * This object denotes a missing argument in a function call. The difference between
 * {@link RMissing} is that {@link REmpty} denotes explicitly missing argument, e.g., first argument
 * in call {@code foo(,42)}, but {@link RMissing} is produced inside FastR argument matching to
 * denote argument that was not passed at all, e.g., second dimension in {@code bar[3]}.
 * {@link RMissing} should never be produced by AST parser. If user attempts at setting
 * {@link RMissing} constant to a language object via subsetting, this constant is converted to
 * {@link REmpty}.
 *
 * Note: AST constant nodes with {@link REmpty} as value are not wrapped into promise nodes during
 * argument matching, therefore in the function prologue the {@link REmpty} gets directly saved into
 * corresponding frame slot, unlike other arguments' values which are typically wrapped in
 * {@link RPromise}.
 */
public final class REmpty extends RScalar {

    public static final REmpty instance = new REmpty();

    private REmpty() {
    }

    @Override
    public RType getRType() {
        return RType.Null;
    }

    @Override
    public String toString() {
        return "empty";
    }
}
