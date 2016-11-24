/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.nodes.function.signature;

import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;

/**
 * Represents the information about varargs contained in {@link ArgumentsSignature} and its argument
 * values and provides helper functions related to flattening the varargs.
 */
public final class VarArgsHelper {

    /**
     * Array of the same size as the original signature with {@code null} in places where there are
     * regular arguments and with {@link ArgumentsSignature} instances under the same indexes of
     * their corresponding {@link RArgsValuesAndNames}.
     */
    private final ArgumentsSignature[] varArgs;

    /**
     * The total number of arguments including those in varargs, and excluding unmatched ones.
     */
    private final int argListSize;

    private VarArgsHelper(ArgumentsSignature[] varArgs, int argListSize) {
        this.varArgs = varArgs;
        this.argListSize = argListSize;
    }

    public static VarArgsHelper create(ArgumentsSignature signature, Object[] suppliedArguments) {
        assert signature.getLength() == suppliedArguments.length;
        ArgumentsSignature[] varArgs = null;
        int argListSize = signature.getLength();
        for (int i = 0; i < suppliedArguments.length; i++) {
            Object arg = suppliedArguments[i];
            if (arg instanceof RArgsValuesAndNames) {
                if (varArgs == null) {
                    varArgs = new ArgumentsSignature[suppliedArguments.length];
                }
                varArgs[i] = ((RArgsValuesAndNames) arg).getSignature();
                argListSize += ((RArgsValuesAndNames) arg).getLength() - 1;
            } else if (signature.isUnmatched(i)) {
                argListSize--;
            }
        }
        return new VarArgsHelper(varArgs, argListSize);
    }

    public boolean hasVarArgs() {
        return varArgs != null;
    }

    public ArgumentsSignature[] getVarArgsSignatures() {
        return varArgs;
    }

    public int getArgListSize() {
        return argListSize;
    }

    /**
     * Returns an array where each index is either index into the variables array (positive number)
     * or it is a packed representation of two indices: one into the variables array pointing to
     * varargs instance and the other is index into this varargs' arguments array. Use static
     * methods {@link #isVarArgsIndex(long)}, {@link #extractVarArgsArgumentIndex(long)} and
     * {@link #extractVarArgsArgumentIndex(long)} to access the data packed in the {@code long}
     * value. This method also removes arguments that are marked as 'unmatched' in the signature.
     *
     * Note: where {@link #hasVarArgs()} returns {@code false}, then the flattening may not be
     * necessary. This optimization is left to the caller.
     */
    public long[] flattenIndexes(ArgumentsSignature signature) {
        long[] preparePermutation = new long[argListSize];
        int index = 0;
        for (int i = 0; i < varArgs.length; i++) {
            ArgumentsSignature varArgSignature = varArgs[i];
            if (varArgSignature != null) {
                for (int j = 0; j < varArgSignature.getLength(); j++) {
                    preparePermutation[index++] = -((((long) i) << 32) + j) - 1;
                }
            } else if (!signature.isUnmatched(i)) {
                preparePermutation[index++] = i;
            }
        }
        return preparePermutation;
    }

    /** @see #flattenIndexes(ArgumentsSignature) */
    public Object[] flattenValues(ArgumentsSignature signature, Object[] values) {
        Object[] result = new Object[argListSize];
        int resultIdx = 0;
        for (int valuesIdx = 0; valuesIdx < values.length; valuesIdx++) {
            if (varArgs[valuesIdx] != null) {
                assert values[valuesIdx] instanceof RArgsValuesAndNames;
                assert ((RArgsValuesAndNames) values[valuesIdx]).getSignature() == varArgs[valuesIdx];
                RArgsValuesAndNames varArgsValues = (RArgsValuesAndNames) values[valuesIdx];
                for (int i = 0; i < varArgsValues.getLength(); i++) {
                    result[resultIdx++] = varArgsValues.getArgument(i);
                }
            } else if (!signature.isUnmatched(valuesIdx)) {
                result[resultIdx++] = values[valuesIdx];
            }
        }
        return result;
    }

    /** @see #flattenIndexes(ArgumentsSignature) */
    public ArgumentsSignature flattenNames(ArgumentsSignature signature) {
        String[] argNames = new String[argListSize];
        int index = 0;
        for (int i = 0; i < varArgs.length; i++) {
            ArgumentsSignature varArgSignature = varArgs[i];
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

    /*
     * Utility functions for interpreting the result of flattenIndexes.
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
}
