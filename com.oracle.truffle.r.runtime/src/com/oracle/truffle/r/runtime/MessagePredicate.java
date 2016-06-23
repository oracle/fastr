/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.util.function.Predicate;

import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

public enum MessagePredicate {

    MUST_NOT_BE_NA_VALUE((Integer x) -> !RRuntime.isNA(x), Message.INVALID_LARGE_NA_VALUE),
    MUST_BE_GT_ZERO((Integer x) -> x >= 0, Message.INVALID_NEGATIVE_VALUE),
    SEED_MUST_BE_INT(x -> x instanceof RAbstractIntVector || x instanceof Integer, Message.SEED_NOT_VALID_INT),
    FILL_SHOULD_BE_POSITIVE(x -> x instanceof Byte || x instanceof Integer && ((Integer) x) > 0, Message.NON_POSITIVE_FILL);

    private final Predicate<?> predicate;
    private final Message msg;

    MessagePredicate(Predicate<?> predicate, Message msg) {
        this.predicate = predicate;
        this.msg = msg;
    }

    public Predicate<?> getPredicate() {
        return predicate;
    }

    public Message getMessage() {
        return msg;
    }
}
