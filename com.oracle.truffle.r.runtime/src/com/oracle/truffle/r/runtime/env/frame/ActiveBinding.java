/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.env.frame;

import java.util.Objects;

import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Represent an active binding of a function. This requires special treatment when reading and
 * writing variables.
 */
public class ActiveBinding implements RTruffleObject {

    private final RType expectedType;
    private final RFunction function;
    private boolean initialized = false;
    private final boolean hidden;

    public ActiveBinding(RType expectedType, RFunction fun, boolean hidden) {
        this.expectedType = Objects.requireNonNull(expectedType);
        this.function = Objects.requireNonNull(fun);
        this.hidden = hidden;
    }

    public ActiveBinding(RType expectedType, RFunction fun) {
        this(expectedType, fun, false);
    }

    public RFunction getFunction() {
        return function;
    }

    public RType getExpectedType() {
        return expectedType;
    }

    public static boolean isActiveBinding(Object binding) {
        return binding instanceof ActiveBinding;
    }

    @Override
    public String toString() {
        return "active binding";
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Object writeValue(Object value) {
        Object result = RContext.getEngine().evalFunction(function, REnvironment.baseEnv().getFrame(), RCaller.createInvalid(null), true, null, value);
        initialized = true;
        return result;
    }

    public Object readValue() {
        if (hidden && !initialized) {
            return RMissing.instance;
        }
        return RContext.getEngine().evalFunction(function, REnvironment.baseEnv().getFrame(), RCaller.createInvalid(null), true, null);
    }

    public void setInitialized(boolean value) {
        initialized = value;
    }

    public static boolean isListed(Object value) {
        if (isActiveBinding(value)) {
            ActiveBinding binding = (ActiveBinding) value;
            return !binding.hidden || binding.initialized;
        }
        return true;
    }
}
