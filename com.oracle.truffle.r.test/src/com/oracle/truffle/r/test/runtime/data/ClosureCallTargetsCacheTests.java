/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.runtime.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.test.TestBase;
import org.junit.Test;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure.CallTargetCache;
import com.oracle.truffle.r.runtime.nodes.RNode;

import java.util.function.Supplier;

public class ClosureCallTargetsCacheTests extends TestBase {
    @Test
    public void test() {
        SourceSection src = RSource.createUnknown("test");
        RNode node = RContext.getASTBuilder().lookup(src, "x", false).asRNode();
        CallTargetCache cache = new CallTargetCache() {
            @Override
            protected RootCallTarget generateCallTarget(RNode n, String closureName) {
                return new RootCallTargetImpl(closureName);
            }

            @Override
            protected void log(Supplier<String> messageSupplier) {
                // nop
            }
        };
        FrameDescriptor fd1 = new FrameDescriptor();
        FrameDescriptor fd2 = new FrameDescriptor();
        FrameDescriptor fd3 = new FrameDescriptor();
        assertEquals(new RootCallTargetImpl("name1"), cache.get(fd1, true, node, "name1"));
        assertEquals(new RootCallTargetImpl("name2"), cache.get(fd2, true, node, "name2"));
        assertEquals(new RootCallTargetImpl("name3"), cache.get(fd3, true, node, "name3"));
        assertTrue("fd1 not cached", cache.check(fd1, false, 0));
        assertTrue("fd2 cached with 0", cache.check(fd2, true, 0));
        assertTrue("fd3 cached with latest value", cache.check(fd3, true, 1));

        // fd2 should skip fd3
        assertEquals(new RootCallTargetImpl("name2"), cache.get(fd2, true, node, "name2"));
        assertTrue("fd2 cached with latest value", cache.check(fd2, true, 1));
        assertTrue("fd3 cached with second latest value", cache.check(fd3, true, 0));
    }

    private static final class RootCallTargetImpl implements RootCallTarget {
        private final String name;

        private RootCallTargetImpl(String name) {
            this.name = name;
        }

        @Override
        public RootNode getRootNode() {
            return null;
        }

        @Override
        public Object call(Object... arguments) {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RootCallTargetImpl)) {
                return false;
            }
            return name.equals(((RootCallTargetImpl) obj).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
