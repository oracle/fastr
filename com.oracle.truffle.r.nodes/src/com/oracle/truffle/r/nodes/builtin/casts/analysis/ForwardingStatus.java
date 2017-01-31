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
package com.oracle.truffle.r.nodes.builtin.casts.analysis;

import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.runtime.RInternalError;

public abstract class ForwardingStatus {

    public static final class Forwarded extends ForwardingStatus {
        Forwarded(Mapper<?, ?> mapper) {
            super((byte) 1, mapper);
        }
    }

    public static final ForwardingStatus BLOCKED = new ForwardingStatus((byte) 0, null) {
    };
    public static final ForwardingStatus UNKNOWN = new ForwardingStatus((byte) -1, null) {
    };
    public static final ForwardingStatus FORWARDED = new Forwarded(null);

    final Mapper<?, ?> mapper;
    private final byte flag;

    protected ForwardingStatus(byte flag, Mapper<?, ?> mapper) {
        this.flag = flag;
        this.mapper = mapper;
    }

    private static byte and(byte x1, byte x2) {
        if (x1 < 0 && x2 < 0) {
            return -1;
        } else {
            return (byte) (x1 * x2);
        }
    }

    private static byte or(byte x1, byte x2) {
        if (x1 == 0 && x2 == 0) {
            return 0;
        } else {
            return x1 + x2 >= 0 ? (byte) 1 : (byte) -1;
        }
    }

    private static byte not(byte x) {
        if (x < 0) {
            return -1;
        } else {
            return x == 0 ? (byte) 1 : (byte) 0;
        }
    }

    static ForwardingStatus fromFlag(byte flag) {
        return fromFlag(flag, null);
    }

    static ForwardingStatus fromFlag(byte flag, Mapper<?, ?> mapper) {
        switch (flag) {
            case -1:
                return UNKNOWN;
            case 0:
                return BLOCKED;
            case 1:
                return mapper == null ? FORWARDED : new Forwarded(mapper);
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    ForwardingStatus and(ForwardingStatus other) {
        if (this.mapper != null && other.mapper != null) {
            // only one mapper per type is supported in this analysis
            return UNKNOWN;
        }
        return fromFlag(and(this.flag, other.flag), this.mapper != null ? this.mapper : other.mapper);
    }

    ForwardingStatus or(ForwardingStatus other) {
        return fromFlag(or(this.flag, other.flag));
    }

    ForwardingStatus not() {
        return fromFlag(not(this.flag));
    }

    public boolean isForwarded() {
        return flag == (byte) 1 && mapper == null;
    }

    public boolean isBlocked() {
        return flag == (byte) 0;
    }

    public boolean isUnknown() {
        return flag == (byte) -1;
    }
}
