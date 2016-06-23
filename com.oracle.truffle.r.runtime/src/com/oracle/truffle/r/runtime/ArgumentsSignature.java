/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
    public static final int NO_VARARG = -1;

    private static final String VARARG_GETTER_PREFIX = "..";

    @CompilationFinal private static final ArgumentsSignature[] EMPTY_SIGNATURES = new ArgumentsSignature[32];
    public static final ArgumentsSignature INVALID_SIGNATURE = new ArgumentsSignature(new String[]{"<<invalid>>"});

    static {
        for (int i = 0; i < EMPTY_SIGNATURES.length; i++) {
            EMPTY_SIGNATURES[i] = get(new String[i]);
        }
    }

    @TruffleBoundary
    public static ArgumentsSignature get(String... names) {
        assert names != null;
        ArgumentsSignature newSignature = new ArgumentsSignature(names);
        ArgumentsSignature oldSignature = signatures.putIfAbsent(newSignature, newSignature);
        return oldSignature != null ? oldSignature : newSignature;
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

    @CompilationFinal private final String[] names;
    @CompilationFinal private final int[] varArgIndexes;
    @CompilationFinal private final boolean[] isVarArg;
    @CompilationFinal private final boolean[] isVarArgGetter;
    private final int nonNullCount;

    private ArgumentsSignature(String[] names) {
        this.names = Arrays.stream(names).map(s -> s == null || s == UNMATCHED ? s : s.intern()).toArray(String[]::new);
        this.nonNullCount = (int) Arrays.stream(names).filter(s -> s != null).count();

        int index = NO_VARARG;
        int count = 0;
        this.isVarArg = new boolean[names.length];
        this.isVarArgGetter = new boolean[names.length];
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (name != null) {
                if (VARARG_NAME.equals(name)) {
                    this.isVarArg[i] = true;
                    count++;
                    if (index != NO_VARARG) {
                        index = i;
                    }
                } else if (name.startsWith(VARARG_GETTER_PREFIX)) {
                    this.isVarArgGetter[i] = true;
                }
            }
        }
        int pos = 0;
        this.varArgIndexes = new int[count];
        for (int i = 0; i < names.length; i++) {
            if (isVarArg[i]) {
                varArgIndexes[pos++] = i;
            }
        }
    }

    public boolean isEmpty() {
        return this == EMPTY_SIGNATURES[0];
    }

    public int getLength() {
        return names.length;
    }

    public int getNonNullCount() {
        return nonNullCount;
    }

    public int getVarArgIndex() {
        assert varArgIndexes.length <= 1 : "cannot ask for _the_ vararg index if there are multiple varargs";
        return varArgIndexes.length == 0 ? NO_VARARG : varArgIndexes[0];
    }

    public int getVarArgCount() {
        return varArgIndexes.length;
    }

    public String[] getNames() {
        return names;
    }

    public String getName(int index) {
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
        return names[index] == UNMATCHED;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(names);
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
        return Arrays.asList(names).iterator();
    }

    @Override
    public String toString() {
        return "Signature " + Arrays.toString(names);
    }

    /*
     * Utility functions
     */

    public static boolean isVarArgsIndex(long idx) {
        return idx < 0;
    }

    public static int extractVarArgsIndex(long idx) {
        return (int) ((-idx - 1) >> 32);
    }

    public static int extractVarArgsArgumentIndex(long idx) {
        return (int) (-idx - 1);
    }

    /**
     * Returns an array where each index is either index into the variables array (positive number)
     * or it is a packed representation of two indices: one into the variables array pointing to
     * varargs instance and the other is index into this varargs' arguments array. Use static
     * methods {@link #isVarArgsIndex(long)}, {@link #extractVarArgsArgumentIndex(long)} and
     * {@link #extractVarArgsArgumentIndex(long)} to access the data packed in the {@code long}
     * value. This method also removes arguments that are marked as 'unmatched' in the signature.
     * 
     * @param argListSize length of the result -- sum of lengths of all varargs contained within
     *            varArgSignatures minus any unmatched arguments.
     */
    public static long[] flattenIndexes(ArgumentsSignature[] varArgSignatures, ArgumentsSignature suppliedSignature, int argListSize) {
        long[] preparePermutation = new long[argListSize];
        int index = 0;
        for (int i = 0; i < varArgSignatures.length; i++) {
            ArgumentsSignature varArgSignature = varArgSignatures[i];
            if (varArgSignature != null) {
                for (int j = 0; j < varArgSignature.getLength(); j++) {
                    preparePermutation[index++] = -((((long) i) << 32) + j) - 1;
                }
            } else if (!suppliedSignature.isUnmatched(i)) {
                preparePermutation[index++] = i;
            }
        }
        return preparePermutation;
    }

    /** {@link #flattenIndexes(ArgumentsSignature[], ArgumentsSignature, int)}. */
    public static ArgumentsSignature flattenNames(ArgumentsSignature signature, ArgumentsSignature[] varArgSignatures, int argListSize) {
        String[] argNames = new String[argListSize];
        int index = 0;
        for (int i = 0; i < varArgSignatures.length; i++) {
            ArgumentsSignature varArgSignature = varArgSignatures[i];
            if (varArgSignature != null) {
                for (int j = 0; j < varArgSignature.getLength(); j++) {
                    argNames[index++] = varArgSignature.getName(j);
                }
            } else if (!signature.isUnmatched(i)) {
                argNames[index++] = signature.getName(i);
            }
        }
        return ArgumentsSignature.get(argNames);
    }
}
