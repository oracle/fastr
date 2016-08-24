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
package com.oracle.truffle.r.runtime.builtins;

import java.util.function.Supplier;

import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

/**
 * This interface can be used to provide a fast path, implemented in Java, for an R function. This
 * may be useful for cases in which there is a significantly simpler implementation for a known
 * configuration of arguments. Returning {@code null} from the fast path node will revert the call
 * site so that it calls the normal R code again.
 *
 * A fast path can be added to a function using {@code RootNode.setFastPath(FastPathFactory)}. When
 * the function is invoked, the fast path is invoked first and only if it returns {@code null}, then
 * the original implementation is invoked.
 */
public interface FastPathFactory {

    FastPathFactory EVALUATE_ARGS = new FastPathFactory() {

        @Override
        public RFastPathNode create() {
            return null;
        }

        @Override
        public RVisibility getVisibility() {
            return null;
        }

        @Override
        public boolean evaluatesArgument(int index) {
            return true;
        }

        @Override
        public boolean forcedEagerPromise(int index) {
            return false;
        }
    };

    FastPathFactory FORCED_EAGER_ARGS = new FastPathFactory() {

        @Override
        public RFastPathNode create() {
            return null;
        }

        @Override
        public RVisibility getVisibility() {
            return null;
        }

        @Override
        public boolean evaluatesArgument(int index) {
            return false;
        }

        @Override
        public boolean forcedEagerPromise(int index) {
            return FastROptions.noEagerEval() ? false : true;
        }
    };

    static FastPathFactory fromRBuiltin(RBuiltin builtin, final Supplier<RFastPathNode> factory) {
        return new FastPathFactory() {
            @Override
            public RFastPathNode create() {
                return factory.get();
            }

            @Override
            public RVisibility getVisibility() {
                return builtin.visibility();
            }

            @Override
            public boolean evaluatesArgument(int index) {
                final int[] nonEvalArgs = builtin.nonEvalArgs();
                for (int i = 0; i < nonEvalArgs.length; ++i) {
                    if (nonEvalArgs[i] == index) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean forcedEagerPromise(int index) {
                return false;
            }
        };
    }

    static FastPathFactory fromVisibility(RVisibility visibility, Supplier<RFastPathNode> factory) {
        return new FastPathFactory() {
            @Override
            public RFastPathNode create() {
                return factory.get();
            }

            @Override
            public RVisibility getVisibility() {
                return visibility;
            }

            @Override
            public boolean evaluatesArgument(int index) {
                return true;
            }

            @Override
            public boolean forcedEagerPromise(int index) {
                return false;
            }
        };
    }

    RFastPathNode create();

    boolean evaluatesArgument(int index);

    boolean forcedEagerPromise(int index);

    /**
     * Visibility of the output. This corresponds to {@link RBuiltin#visibility()}
     */
    RVisibility getVisibility();
}
