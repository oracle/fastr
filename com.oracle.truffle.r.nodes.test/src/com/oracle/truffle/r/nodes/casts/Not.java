/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.casts;

import java.lang.reflect.Type;
import java.util.Optional;

public final class Not<T> implements Type, TypeAndInstanceCheck {

    public static final Not<Object> NOTHING = new Not<>(Object.class);

    private final Type negated;

    private Not(Type negated) {
        this.negated = negated;
    }

    public Type getNegated() {
        return this.negated;
    }

    @Override
    public boolean isInstance(Object x) {
        if (negated instanceof TypeAndInstanceCheck) {
            return !((TypeAndInstanceCheck) negated).isInstance(x);
        } else {
            assert negated instanceof Class;
            return !((Class<?>) negated).isInstance(x);
        }
    }

    @Override
    public Type normalize() {
        if (negated instanceof TypeAndInstanceCheck) {
            return new Not<>(((TypeAndInstanceCheck) negated).normalize());
        } else {
            return this;
        }
    }

    public static boolean isNegative(Type t) {
        return t instanceof Not;
    }

    public static Type getPositiveType(Type t) {
        if (isNegative(t)) {
            return ((Not<?>) t).getNegated();
        } else {
            return t;
        }
    }

    public static Type negateType(Type t) {
        if (isNegative(t)) {
            return getPositiveType(t);
        } else {
            assert t instanceof Class;
            return new Not<>(t);
        }
    }

    @Override
    public String toString() {
        return "Not(" + (negated instanceof Class ? ((Class<?>) negated).getSimpleName() : negated.toString()) + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((negated == null) ? 0 : negated.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Not<?> other = (Not<?>) obj;
        return other.negated.equals(this.negated);
    }

    @Override
    public Optional<Class<?>> classify() {
        return Optional.empty();
    }
}
