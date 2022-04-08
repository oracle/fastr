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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

public final class FunctionScope {
    private String functionName;
    private final Map<String, LocalVariable> localVariables = new HashMap<>();
    private int localVarFrameIdx = FrameSlotChangeMonitor.INTERNAL_INDEXED_SLOT_COUNT;

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
        return localVariables.containsKey(name);
    }

    public void addLocalVariable(LocalVariable localVariable) {
        if (!containsLocalVariable(localVariable.getName())) {
            RLogger.getLogger("RASTBuilder").fine(() -> String.format("Adding local variable %s to function scope '%s'", localVariable, functionName));
            localVariables.put(localVariable.getName(), localVariable);
            localVarFrameIdx++;
        }
    }

    public LocalVariable getLocalVariable(String symbol) {
        return localVariables.get(symbol);
    }

    public int getLocalVariablesCount() {
        return localVariables.size();
    }

    public int getNextLocalVariableFrameIndex() {
        return localVarFrameIdx;
    }

    /**
     * Returns list of all the local variables sorted ascending by frame index.
     */
    public List<LocalVariable> getLocalVariablesSortedByFrameIdx() {
        return localVariables.values().stream().sorted(Comparator.comparingInt(LocalVariable::getFrameIndex)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("FunctionScope{");
        sb.append("'").append(functionName).append("', ");
        sb.append("localVariables = [");
        localVariables.values().forEach(
                        (localVariable) -> sb.append(localVariable).append(", "));
        sb.append("]"); // localVariables
        sb.append("}"); // FunctionScope
        return sb.toString();
    }

}
