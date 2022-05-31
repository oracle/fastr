/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.runtime.parsermetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

public final class FunctionScope {
    private String functionName;

    public static final FunctionScope EMPTY_SCOPE = new FunctionScope();

    private static final int INITIAL_LOC_VARS_CAPACITY = 12;

    private final List<FrameSlotKind> localVariableKinds = new ArrayList<>(INITIAL_LOC_VARS_CAPACITY);
    private final List<String> localVariableNames = new ArrayList<>(INITIAL_LOC_VARS_CAPACITY);

    /**
     * Maps identifiers to indexes into lists used in this class. Note that these indexes are not
     * {@link com.oracle.truffle.r.runtime.env.frame.FrameIndex frame indexes}.
     */
    private final Map<String, Integer> localVariableArrayIndexes = new HashMap<>(INITIAL_LOC_VARS_CAPACITY);

    private static final TruffleLogger logger = RLogger.getLogger(RLogger.LOGGER_AST);

    public FunctionScope() {
        this(null);
    }

    public FunctionScope(String functionName) {
        this.functionName = functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void addLocalVariable(String identifier, FrameSlotKind slotKind) {
        Integer arrayIdx = localVariableArrayIndexes.get(identifier);
        if (arrayIdx == null) {
            logger.fine(() -> String.format("Adding local variable %s to function scope '%s'", identifier, functionName));
            localVariableArrayIndexes.put(identifier, localVariableNames.size());
            localVariableNames.add(identifier);
            localVariableKinds.add(slotKind);
        } else {
            // Because of the implementation of FrameIndex, frameIndex can also index an array.
            // In this case, the local variable is redefined, so we replace its old slot kind with
            // the newly defined slot kind.
            FrameSlotKind oldKind = localVariableKinds.get(arrayIdx);
            localVariableKinds.set(arrayIdx, replaceKind(oldKind, slotKind));
        }
    }

    /**
     * Returns a frame slot kind that should replace the given {@code oldKind} by {@code newKind}.
     * For example {@code replaceKind(Integer, Double) => Object}, or
     * {@code replaceKind(Illegal, Object) => Object}, etc.
     */
    private static FrameSlotKind replaceKind(FrameSlotKind oldKind, FrameSlotKind newKind) {
        // TODO: Add if clause that returns Illegal.
        if (oldKind == newKind) {
            return oldKind;
        } else {
            return FrameSlotKind.Object;
        }
    }

    public int getLocalVariableCount() {
        return localVariableNames.size();
    }

    /**
     * Returns {@link com.oracle.truffle.r.runtime.env.frame.FrameIndex frame index} of the given
     * local variable.
     *
     * Note that this method uses knowledge of internal implementation of
     * {@link com.oracle.truffle.r.runtime.env.frame.FrameIndex} - indexes into normal slots (not
     * auxiliary) are 0-based positive integers.
     */
    public Integer getLocalVariableFrameIndex(String symbol) {
        Integer locVarArrayIdx = localVariableArrayIndexes.get(symbol);
        return locVarArrayIdx != null ? locVarArrayIdx + FrameSlotChangeMonitor.INTERNAL_INDEXED_SLOT_COUNT : null;
    }

    public String getLocalVariableName(int locVarIdx) {
        return localVariableNames.get(locVarIdx);
    }

    public FrameSlotKind getLocalVariableKind(int locVarIdx) {
        return localVariableKinds.get(locVarIdx);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("FunctionScope{");
        sb.append("'").append(functionName).append("', ");
        sb.append("localVariables = [");
        for (int i = 0; i < localVariableNames.size(); i++) {
            sb.append("(").append(localVariableNames.get(i)).append(",").append(localVariableKinds.get(i)).append("),");
        }
        sb.append("]"); // localVariables
        sb.append("}"); // FunctionScope
        return sb.toString();
    }

}
