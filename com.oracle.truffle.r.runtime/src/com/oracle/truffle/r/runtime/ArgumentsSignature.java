/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.data.RStringVector;

public final class ArgumentsSignature implements Iterable<String> {

    private static final ConcurrentHashMap<ArgumentsSignature, ArgumentsSignature> signatures = new ConcurrentHashMap<>();

    /**
     * Designates an element in a signature that was not found during argument matching. The
     * signature returned from argument matching must have a slot for each formal argument, formal
     * argument can be provided with a name or without a name, or it may have default value, but
     * without any actual value provided by the caller. This is the case for {@code UNMATCHED}. Use
     * {@link #isUnmatched(int)} for checking if argument is unmatched.
     */
    public static final String UNMATCHED = new String();
    public static final String VARARG_NAME = "...";
    public static final int[] EMPTY_VARARGS_INDEXES = new int[0];
    public static final int NO_VARARG = -1;

    @CompilationFinal(dimensions = 1) private static final ArgumentsSignature[] EMPTY_SIGNATURES = new ArgumentsSignature[32];
    public static final ArgumentsSignature INVALID_SIGNATURE = new ArgumentsSignature(new String[]{"<<invalid>>"}, false);

    static {
        for (int i = 0; i < EMPTY_SIGNATURES.length; i++) {
            EMPTY_SIGNATURES[i] = get(new String[i]);
        }
    }

    public static ArgumentsSignature get(String... names) {
        return get(names, false);
    }

    public static ArgumentsSignature empty(int length) {
        if (length < EMPTY_SIGNATURES.length) {
            assert length >= 0;
            return EMPTY_SIGNATURES[length];
        }
        CompilerDirectives.transferToInterpreter();
        RError.performanceWarning("argument list exceeds " + EMPTY_SIGNATURES + " elements");
        return get(new String[length]);
    }

    /**
     * Returns {@code null} if the the vector is {@code null}. Any empty string in the vector is
     * converted to {@code null} value.
     */
    public static ArgumentsSignature fromNamesAttribute(RStringVector names) {
        return names == null ? null : get(names.getDataWithoutCopying(), true);
    }

    @TruffleBoundary
    private static ArgumentsSignature get(String[] names, boolean convertEmpty) {
        assert names != null;
        ArgumentsSignature newSignature = new ArgumentsSignature(names, convertEmpty);
        ArgumentsSignature oldSignature = signatures.putIfAbsent(newSignature, newSignature);
        return oldSignature != null ? oldSignature : newSignature;
    }

    @CompilationFinal(dimensions = 1) private final String[] names;
    @CompilationFinal(dimensions = 1) private final int[] varArgIndexes;
    @CompilationFinal(dimensions = 1) private final boolean[] isVarArg;
    private final int length;
    private final int varArgIndex;
    private final int nonNullCount;

    private ArgumentsSignature(String[] names, boolean convertEmpty) {
        this.length = names.length;
        String[] localNames = null;
        int nonNull = 0;
        for (int i = 0; i < names.length; i++) {
            String s = names[i];
            if (s == null || (s.isEmpty() && convertEmpty)) {
                continue;
            } else if (localNames == null) {
                localNames = new String[names.length];
            }
            nonNull++;
            localNames[i] = s == UNMATCHED ? s : s.intern();
        }
        this.names = localNames;
        this.nonNullCount = nonNull;

        int varArgsCount = 0;
        boolean[] isVarArgsLocal = null;
        if (localNames != null) {
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                if (name != null) {
                    if (VARARG_NAME.equals(name)) {
                        if (isVarArgsLocal == null) {
                            isVarArgsLocal = new boolean[names.length];
                        }
                        isVarArgsLocal[i] = true;
                        varArgsCount++;
                    }
                }
            }
        }
        this.isVarArg = isVarArgsLocal;

        if (isVarArgsLocal == null) {
            this.varArgIndexes = EMPTY_VARARGS_INDEXES;
            this.varArgIndex = NO_VARARG;
        } else {
            int pos = 0;
            this.varArgIndexes = new int[varArgsCount];
            for (int i = 0; i < names.length; i++) {
                if (isVarArgsLocal[i]) {
                    varArgIndexes[pos++] = i;
                }
            }
            this.varArgIndex = varArgIndexes[0];
        }
    }

    public boolean isEmpty() {
        return this == EMPTY_SIGNATURES[0];
    }

    public int getLength() {
        return length;
    }

    public int getNonNullCount() {
        return nonNullCount;
    }

    public int getVarArgIndex() {
        assert varArgIndexes.length <= 1 : "cannot ask for _the_ vararg index if there are multiple varargs";
        return varArgIndex;
    }

    public int getVarArgCount() {
        return varArgIndexes.length;
    }

    public String[] getNames() {
        return names;
    }

    public String getName(int index) {
        if (names == null) {
            return null;
        }
        return names[index] == UNMATCHED ? null : names[index];
    }

    /**
     * Returns {@code true} if the given index represents an unmatched argument. This only makes
     * sense for signatures created by argument matching process, such signatures must contain a
     * slot for each formal parameter, even for formal parameters with default value that were not
     * supplied by the caller. Use this method in order to distinguish these from other parameters
     * that were not matched by name and therefore have {@code null} as their name.
     * {@link #getName(int)} returns {@code null} in either case.
     */
    public boolean isUnmatched(int index) {
        return names != null && names[index] == UNMATCHED;
    }

    public boolean isVarArg(int index) {
        return this.isVarArg != null && this.isVarArg[index];
    }

    /**
     * Returns the index of given name, {@code -1} if it is not present. The search key must be
     * interned string.
     */
    public int indexOfName(String find) {
        if (names == null) {
            return -1;
        }
        for (int i = 0; i < names.length; i++) {
            if (names[i] == find) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(names) ^ length;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ArgumentsSignature other = (ArgumentsSignature) obj;
        if (!Arrays.equals(names, other.names)) {
            return false;
        }
        return true;
    }

    @Override
    public Iterator<String> iterator() {
        CompilerAsserts.neverPartOfCompilation();
        String[] namesLocal = names == null ? new String[length] : names;
        return Arrays.asList(namesLocal).iterator();
    }

    @Override
    public String toString() {
        String value = names == null ? length + " times null" : Arrays.toString(names);
        return "Signature " + value;
    }
}
