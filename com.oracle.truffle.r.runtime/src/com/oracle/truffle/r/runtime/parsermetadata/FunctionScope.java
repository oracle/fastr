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

import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

public final class FunctionScope {
    private String functionName;
    private int localVarFrameIdx = FrameSlotChangeMonitor.INTERNAL_INDEXED_SLOT_COUNT;

    private static final int INITIAL_LOC_VARS_CAPACITY = 12;

    private final List<FrameSlotKind> localVariableKinds = new ArrayList<>(INITIAL_LOC_VARS_CAPACITY);
    private final List<String> localVariableNames = new ArrayList<>(INITIAL_LOC_VARS_CAPACITY);
    private final Map<String, Integer> localVariableIndexes = new HashMap<>(INITIAL_LOC_VARS_CAPACITY);

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

    public boolean containsLocalVariable(String name) {
        return localVariableIndexes.containsKey(name);
    }

    public void addLocalVariable(String identifier, FrameSlotKind slotKind) {
        if (!containsLocalVariable(identifier)) {
            RLogger.getLogger("RASTBuilder").fine(() -> String.format("Adding local variable %s to function scope '%s'", identifier, functionName));
            localVariableIndexes.put(identifier, localVarFrameIdx);
            localVariableKinds.add(slotKind);
            localVariableNames.add(identifier);
            localVarFrameIdx++;
        }
    }

    public Integer getLocalVariableFrameIndex(String symbol) {
        return localVariableIndexes.get(symbol);
    }

    public List<String> getLocalVariableNames() {
        return localVariableNames;
    }

    public List<FrameSlotKind> getLocalVariableKinds() {
        return localVariableKinds;
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
