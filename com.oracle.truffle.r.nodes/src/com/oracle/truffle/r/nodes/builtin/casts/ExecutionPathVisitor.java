/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.casts;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.PipelineStepVisitor;

public abstract class ExecutionPathVisitor<T> implements PipelineStepVisitor<T> {

    private Map<MapIfStep<?, ?>, Integer> mapIfStepStatuses;
    private BitSet bs;
    private int mapIfCounter;
    private List<T> results = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public List<T> visitPaths(PipelineStep<?, ?> firstStep, T initial) {
        if (firstStep == null) {
            return Collections.singletonList(initial);
        }
        mapIfStepStatuses = new HashMap<>();
        results.add(firstStep.acceptPipeline(this, initial));
        int n = 1 << mapIfStepStatuses.size();
        for (long i = 1; i < n; i++) {
            bs = BitSet.valueOf(new long[]{i});
            T res;
            try {
                res = firstStep.acceptPipeline(this, initial);
            } catch (PathBreakException br) {
                res = (T) br.result;
            }
            results.add(res);
        }
        return results;
    }

    @Override
    public final T visit(MapIfStep<?, ?> step, T previous) {
        boolean visitTrueBranch = bs == null ? false : bs.get(mapIfStepStatuses.get(step));
        if (bs == null) {
            visitTrueBranch = false;
            mapIfStepStatuses.put(step, mapIfCounter++);
        } else {
            visitTrueBranch = bs.get(mapIfStepStatuses.get(step));
        }
        T res = visitBranch(step, previous, visitTrueBranch);
        if (step.isReturns() && visitTrueBranch) {
            throw new PathBreakException(res);
        } else {
            return res;
        }
    }

    protected abstract T visitBranch(MapIfStep<?, ?> step, T previous, boolean visitTrueBranch);

    @SuppressWarnings("serial")
    static final class PathBreakException extends RuntimeException {

        private final Object result;

        private PathBreakException(Object result) {
            this.result = result;
        }

        @SuppressWarnings("sync-override")
        @Override
        public Throwable fillInStackTrace() {
            return null;
        }
    }
}
