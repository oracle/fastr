/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.nodes.promises;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.test.TestBase;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public class ExplicitPromisesTest extends TestBase {
    private static final int nestedPromiseConstant = 42;

    @Test
    public void testNestedPromise() {
        execInContext(() -> {
            RPromise nestedPromise = createNestedPromise();
            Object value = RContext.getEngine().evalPromise(nestedPromise);
            Assert.assertEquals(nestedPromiseConstant, value);
            return null;
        });
    }

    private static RPromise createNestedPromise() {
        SourceSection sourceSection = RSyntaxNode.LAZY_DEPARSE;
        RCodeBuilder<RSyntaxNode> codeBuilder = RContext.getASTBuilder();
        MaterializedFrame globalFrame = RContext.getInstance().stateREnvironment.getGlobalFrame();

        RSyntaxNode constant = codeBuilder.constant(sourceSection, nestedPromiseConstant);
        Closure nestedClosure = Closure.createPromiseClosure(constant.asRNode());
        RPromise nestedPromise = RDataFactory.createPromise(RPromise.PromiseState.Explicit, nestedClosure, globalFrame);

        RSyntaxNode callNestedPromise = codeBuilder.constant(sourceSection, nestedPromise);
        Closure closure = Closure.createPromiseClosure(callNestedPromise.asRNode());
        return RDataFactory.createPromise(RPromise.PromiseState.Explicit, closure, globalFrame);
    }
}
