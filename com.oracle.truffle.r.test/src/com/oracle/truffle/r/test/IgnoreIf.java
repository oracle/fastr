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
package com.oracle.truffle.r.test;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This test trait allows ignoring tests in certain environments, such as certain operating systems
 * etc. For instance, to ignore the following test on a Solaris OS, one can use the
 * <code>systemPropertyContains</code> factory method to create the test trait. The resulting test
 * trait will test whether the system property <code>os.name</code> contains string
 * <code>sunos</code> and if so, the test will be ignored.
 *
 * <pre>
 * assertEval(IgnoreIf.systemPropertyContains("os.name", "sunos"), "list(c(9.5367431640625e-07, 1.9073486328125e-06)");
 * </pre>
 */
public abstract class IgnoreIf implements TestTrait {

    public abstract boolean isIgnoring();

    @Override
    public String getName() {
        return "IgnoreIf";
    }

    public static boolean containsIgnoring(TestTrait[] traits) {
        IgnoreIf[] ignoreIfTraits = TestTrait.collect(traits, IgnoreIf.class);
        return Arrays.stream(ignoreIfTraits).anyMatch(t -> t.isIgnoring());
    }

    public static IgnoreIf envVarIs(String var, String expectedValue) {
        return new GenericIgnoreIf<>(() -> System.getenv(var), (value) -> expectedValue.equals(value));
    }

    public static IgnoreIf envVarContains(String var, String substring) {
        return new GenericIgnoreIf<>(() -> System.getenv(var), (value) -> value != null && value.contains(substring));
    }

    public static IgnoreIf systemPropertyIs(String property, String expectedValue) {
        return new GenericIgnoreIf<>(() -> System.getProperty(property), (value) -> expectedValue.equals(value));
    }

    public static IgnoreIf systemPropertyContains(String property, String substring) {
        return new GenericIgnoreIf<>(() -> System.getProperty(property), (value) -> value != null && value.contains(substring));
    }

    public static final class GenericIgnoreIf<T> extends IgnoreIf {
        private final Supplier<T> supplier;
        private final Function<T, Boolean> pred;

        private GenericIgnoreIf(Supplier<T> supplier, Function<T, Boolean> pred) {
            this.supplier = supplier;
            this.pred = pred;
        }

        @Override
        public boolean isIgnoring() {
            return pred.apply(supplier.get());
        }

    }

}
