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

import static com.oracle.truffle.r.nodes.builtin.casts.analysis.ForwardingStatus.BLOCKED;
import static com.oracle.truffle.r.nodes.builtin.casts.analysis.ForwardingStatus.FORWARDED;
import static com.oracle.truffle.r.nodes.builtin.casts.analysis.ForwardingStatus.UNKNOWN;

import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;

public final class ForwardingAnalysisResult {

    public final ForwardingStatus integerForwarded;
    public final ForwardingStatus logicalForwarded;
    public final ForwardingStatus doubleForwarded;
    public final ForwardingStatus complexForwarded;
    public final ForwardingStatus stringForwarded;
    public final ForwardingStatus nullForwarded;
    public final ForwardingStatus missingForwarded;
    public final boolean invalid;

    static final ForwardingAnalysisResult INVALID = new ForwardingAnalysisResult(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, true);

    ForwardingAnalysisResult() {
        this(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, false);
    }

    private ForwardingAnalysisResult(ForwardingStatus integerForwarded,
                    ForwardingStatus logicalForwarded,
                    ForwardingStatus doubleForwarded,
                    ForwardingStatus complexForwarded,
                    ForwardingStatus stringForwarded,
                    ForwardingStatus nullForwarded,
                    ForwardingStatus missingForwarded,
                    boolean invalid) {
        this.integerForwarded = integerForwarded;
        this.logicalForwarded = logicalForwarded;
        this.doubleForwarded = doubleForwarded;
        this.complexForwarded = complexForwarded;
        this.stringForwarded = stringForwarded;
        this.nullForwarded = nullForwarded;
        this.missingForwarded = missingForwarded;
        this.invalid = invalid;
    }

    public boolean isNullForwarded() {
        return !invalid && nullForwarded.isForwarded();
    }

    public boolean isMissingForwarded() {
        return !invalid && missingForwarded.isForwarded();
    }

    public boolean isIntegerForwarded() {
        return !invalid && integerForwarded.isForwarded();
    }

    public boolean isLogicalForwarded() {
        return !invalid && logicalForwarded.isForwarded();
    }

    public boolean isLogicalMappedToBoolean() {
        return !invalid && logicalForwarded.mapper == MapByteToBoolean.INSTANCE;
    }

    public boolean isDoubleForwarded() {
        return !invalid && doubleForwarded.isForwarded();
    }

    public boolean isComplexForwarded() {
        return !invalid && complexForwarded.isForwarded();
    }

    public boolean isStringForwarded() {
        return !invalid && stringForwarded.isForwarded();
    }

    public boolean isAnythingForwarded() {
        return isNullForwarded() || isMissingForwarded() || isIntegerForwarded() || isLogicalForwarded() || isDoubleForwarded() || isComplexForwarded() || isStringForwarded();
    }

    ForwardingAnalysisResult setForwardedType(Class<?> tp, ForwardingStatus status) {
        if (invalid) {
            return this;
        }

        if (Integer.class == tp || int.class == tp) {
            return new ForwardingAnalysisResult(status,
                            logicalForwarded,
                            doubleForwarded,
                            complexForwarded,
                            stringForwarded,
                            nullForwarded,
                            missingForwarded,
                            invalid);
        } else if (Byte.class == tp || byte.class == tp) {
            return new ForwardingAnalysisResult(integerForwarded,
                            status,
                            doubleForwarded,
                            complexForwarded,
                            stringForwarded,
                            nullForwarded,
                            missingForwarded,
                            invalid);
        } else if (Double.class == tp || double.class == tp) {
            return new ForwardingAnalysisResult(integerForwarded,
                            logicalForwarded,
                            status,
                            complexForwarded,
                            stringForwarded,
                            nullForwarded,
                            missingForwarded,
                            invalid);
        } else if (RComplex.class == tp) {
            return new ForwardingAnalysisResult(integerForwarded,
                            logicalForwarded,
                            doubleForwarded,
                            status,
                            stringForwarded,
                            nullForwarded,
                            missingForwarded,
                            invalid);
        } else if (String.class == tp) {
            return new ForwardingAnalysisResult(integerForwarded,
                            logicalForwarded,
                            doubleForwarded,
                            complexForwarded,
                            status,
                            nullForwarded,
                            missingForwarded,
                            invalid);
        } else {
            return this;
        }
    }

    ForwardingAnalysisResult setForwardedType(RType tp, ForwardingStatus status) {
        if (invalid) {
            return this;
        }

        switch (tp) {
            case Integer:
                return new ForwardingAnalysisResult(status,
                                logicalForwarded,
                                doubleForwarded,
                                complexForwarded,
                                stringForwarded,
                                nullForwarded,
                                missingForwarded,
                                invalid);
            case Logical:
                return new ForwardingAnalysisResult(integerForwarded,
                                status,
                                doubleForwarded,
                                complexForwarded,
                                stringForwarded,
                                nullForwarded,
                                missingForwarded,
                                invalid);
            case Double:
                return new ForwardingAnalysisResult(integerForwarded,
                                logicalForwarded,
                                status,
                                complexForwarded,
                                stringForwarded,
                                nullForwarded,
                                missingForwarded,
                                invalid);
            case Complex:
                return new ForwardingAnalysisResult(integerForwarded,
                                logicalForwarded,
                                doubleForwarded,
                                status,
                                stringForwarded,
                                nullForwarded,
                                missingForwarded,
                                invalid);
            case Character:
                return new ForwardingAnalysisResult(integerForwarded,
                                logicalForwarded,
                                doubleForwarded,
                                complexForwarded,
                                status,
                                nullForwarded,
                                missingForwarded,
                                invalid);
            default:
                return this;
        }
    }

    ForwardingAnalysisResult and(ForwardingAnalysisResult other) {
        if (this.invalid || other.invalid) {
            return ForwardingAnalysisResult.INVALID;
        } else {
            return new ForwardingAnalysisResult(this.integerForwarded.and(other.integerForwarded),
                            this.logicalForwarded.and(other.logicalForwarded),
                            this.doubleForwarded.and(other.doubleForwarded),
                            this.complexForwarded.and(other.complexForwarded),
                            this.stringForwarded.and(other.stringForwarded),
                            this.nullForwarded.and(other.nullForwarded),
                            this.missingForwarded.and(other.missingForwarded),
                            false);
        }
    }

    ForwardingAnalysisResult or(ForwardingAnalysisResult other) {
        if (this.invalid) {
            return other;
        } else if (other.invalid) {
            return this;
        } else {
            return new ForwardingAnalysisResult(this.integerForwarded.or(other.integerForwarded),
                            this.logicalForwarded.or(other.logicalForwarded),
                            this.doubleForwarded.or(other.doubleForwarded),
                            this.complexForwarded.or(other.complexForwarded),
                            this.stringForwarded.or(other.stringForwarded),
                            this.nullForwarded.or(other.nullForwarded),
                            this.missingForwarded.or(other.missingForwarded),
                            false);
        }
    }

    ForwardingAnalysisResult not() {
        if (this.invalid) {
            return this;
        } else {
            return new ForwardingAnalysisResult(this.integerForwarded.not(),
                            this.logicalForwarded.not(),
                            this.doubleForwarded.not(),
                            this.complexForwarded.not(),
                            this.stringForwarded.not(),
                            this.nullForwarded.not(),
                            this.missingForwarded.not(),
                            false);
        }
    }

    ForwardingAnalysisResult forwardAll() {
        if (this.invalid) {
            return this;
        } else {
            return new ForwardingAnalysisResult(FORWARDED,
                            FORWARDED,
                            FORWARDED,
                            FORWARDED,
                            FORWARDED,
                            FORWARDED,
                            FORWARDED,
                            false);
        }
    }

    ForwardingAnalysisResult blockAll() {
        if (this.invalid) {
            return this;
        } else {
            return new ForwardingAnalysisResult(BLOCKED,
                            BLOCKED,
                            BLOCKED,
                            BLOCKED,
                            BLOCKED,
                            BLOCKED,
                            BLOCKED,
                            false);
        }
    }

    ForwardingAnalysisResult unknownAll() {
        if (this.invalid) {
            return this;
        } else {
            return new ForwardingAnalysisResult(UNKNOWN,
                            UNKNOWN,
                            UNKNOWN,
                            UNKNOWN,
                            UNKNOWN,
                            UNKNOWN,
                            UNKNOWN,
                            false);
        }
    }

    ForwardingAnalysisResult setNull(ForwardingStatus status) {
        if (this.invalid) {
            return this;
        } else {
            return new ForwardingAnalysisResult(this.integerForwarded,
                            this.logicalForwarded,
                            this.doubleForwarded,
                            this.complexForwarded,
                            this.stringForwarded,
                            status,
                            this.missingForwarded,
                            false);
        }
    }

    ForwardingAnalysisResult setMissing(ForwardingStatus status) {
        if (this.invalid) {
            return this;
        } else {
            return new ForwardingAnalysisResult(this.integerForwarded,
                            this.logicalForwarded,
                            this.doubleForwarded,
                            this.complexForwarded,
                            this.stringForwarded,
                            this.nullForwarded,
                            status,
                            false);
        }
    }
}
