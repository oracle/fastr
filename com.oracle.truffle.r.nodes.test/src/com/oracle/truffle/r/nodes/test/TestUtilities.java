/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.test;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.*;

public class TestUtilities {

    /**
     * Creates a handle that emulates the behavior as if this node would be executed inside of a R
     * call.
     */
    public static <T extends Node> NodeHandle<T> createHandle(T node, NodeAdapter<T> invoke) {
        return new NodeHandle<>(Truffle.getRuntime().createCallTarget(new TestRoot<>(node, invoke)));
    }

    public interface NodeAdapter<T extends Node> {

        Object invoke(T node, Object... args);

    }

    public static class NodeHandle<T extends Node> {

        private final RootCallTarget target;

        public NodeHandle(RootCallTarget target) {
            this.target = target;
        }

        public RootCallTarget getTarget() {
            return target;
        }

        @SuppressWarnings("unchecked")
        public T getNode() {
            return ((TestRoot<T>) target.getRootNode()).node;
        }

        public Object call(Object... args) {
            Object[] rArguments = RArguments.createUnitialized(null);
            rArguments = Arrays.copyOf(rArguments, RArguments.INDEX_ARGUMENTS + 1);
            rArguments[RArguments.INDEX_ARGUMENTS] = args;
            return target.call(rArguments);
        }

    }

    private static class TestRoot<T extends Node> extends RootNode {

        private final NodeAdapter<T> invoke;
        @Child private T node;

        public TestRoot(T node, NodeAdapter<T> invoke) {
            this.node = node;
            this.invoke = invoke;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                return invoke.invoke(node, (Object[]) frame.getArguments()[RArguments.INDEX_ARGUMENTS]);
            } catch (ReturnException e) {
                return e.getResult();
            }
        }

    }

}
