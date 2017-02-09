/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Defines fluent API methods for building cast pipeline steps for generic argument, which was not
 * cast to any specific type yet. Any method that represents casting of the argument to a specific
 * type returns instance of {@link CoercedPhaseBuilder} with its generic parameters set accordingly.
 */
public class InitialPhaseBuilder<T> extends ArgCastBuilder<T, InitialPhaseBuilder<T>> {

    public InitialPhaseBuilder(PipelineBuilder builder) {
        super(builder);
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendMustBeStep(argFilter, callObj, message, messageArgs);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
        return mustBe(argFilter, null, message, messageArgs);
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter) {
        return mustBe(argFilter, null, null, (Object[]) null);
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        mustBe(Predef.instanceOf(cls), callObj, message, messageArgs);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
        mustBe(Predef.instanceOf(cls), message, messageArgs);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls) {
        mustBe(Predef.instanceOf(cls));
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        shouldBe(Predef.instanceOf(cls), callObj, message, messageArgs);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls, RError.Message message, Object... messageArgs) {
        shouldBe(Predef.instanceOf(cls), message, messageArgs);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls) {
        shouldBe(Predef.instanceOf(cls));
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S> InitialPhaseBuilder<S> map(Mapper<T, S> mapFn) {
        pipelineBuilder().appendMap(mapFn);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public InitialPhaseBuilder<Object> returnIf(Filter<? super T, ?> argFilter) {
        pipelineBuilder().appendMapIf(argFilter, (PipelineStep<?, ?>) null, (PipelineStep<?, ?>) null, true);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, false);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, true);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper, false);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper, true);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, false);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, true);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch, false);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch, true);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public InitialPhaseBuilder<T> notNA(RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(null, callObj, message, messageArgs);
        return this;
    }

    public InitialPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(null, null, message, messageArgs);
        return this;
    }

    public InitialPhaseBuilder<T> notNA(T naReplacement, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(naReplacement, callObj, message, messageArgs);
        return this;
    }

    public InitialPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(naReplacement, null, message, messageArgs);
        return this;
    }

    public InitialPhaseBuilder<T> notNA(T naReplacement) {
        pipelineBuilder().appendNotNA(naReplacement, null, null, null);
        return this;
    }

    public InitialPhaseBuilder<T> boxPrimitive() {
        pipelineBuilder().appendBoxPrimitive();
        return this;
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return asIntegerVector(preserveNames, preserveDimensions, preserveAttributes, null);
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, RBaseNode messageCallerObj) {
        pipelineBuilder().appendAsVector(RType.Integer, preserveNames, preserveDimensions, preserveAttributes, messageCallerObj);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Integer.class);
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(RBaseNode messageCallerObj) {
        return asIntegerVector(false, false, false, messageCallerObj);
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector() {
        return asIntegerVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return asDoubleVector(preserveNames, preserveDimensions, preserveAttributes, null);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, RBaseNode messageCallerObj) {
        pipelineBuilder().appendAsVector(RType.Double, preserveNames, preserveDimensions, preserveAttributes, messageCallerObj);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Double.class);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector(RBaseNode messageCallerObj) {
        return asDoubleVector(false, false, false, messageCallerObj);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector() {
        return asDoubleVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractLogicalVector, Byte> asLogicalVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, RBaseNode messageCallerObj) {
        pipelineBuilder().appendAsVector(RType.Logical, preserveNames, preserveDimensions, preserveAttributes, messageCallerObj);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Byte.class);
    }

    public CoercedPhaseBuilder<RAbstractLogicalVector, Byte> asLogicalVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return asLogicalVector(preserveNames, preserveDimensions, preserveAttributes, null);
    }

    public CoercedPhaseBuilder<RAbstractLogicalVector, Byte> asLogicalVector(RBaseNode messageCallerObj) {
        return asLogicalVector(false, false, false, messageCallerObj);
    }

    public CoercedPhaseBuilder<RAbstractLogicalVector, Byte> asLogicalVector() {
        return asLogicalVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractStringVector, String> asStringVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        pipelineBuilder().appendAsVector(RType.Character, preserveNames, preserveDimensions, preserveAttributes);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), String.class);
    }

    public CoercedPhaseBuilder<RAbstractStringVector, String> asStringVector() {
        return asStringVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractComplexVector, RComplex> asComplexVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return asComplexVector(preserveNames, preserveDimensions, preserveAttributes, null);
    }

    public CoercedPhaseBuilder<RAbstractComplexVector, RComplex> asComplexVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, RBaseNode messageCallerObj) {
        pipelineBuilder().appendAsVector(RType.Complex, preserveNames, preserveDimensions, preserveAttributes, messageCallerObj);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), RComplex.class);
    }

    public CoercedPhaseBuilder<RAbstractComplexVector, RComplex> asComplexVector(RBaseNode messageCallerObj) {
        return asComplexVector(false, false, false, messageCallerObj);
    }

    public CoercedPhaseBuilder<RAbstractComplexVector, RComplex> asComplexVector() {
        return asComplexVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractRawVector, RRaw> asRawVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        pipelineBuilder().appendAsVector(RType.Raw, preserveNames, preserveDimensions, preserveAttributes);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), RRaw.class);
    }

    public CoercedPhaseBuilder<RAbstractRawVector, RRaw> asRawVector() {
        return asRawVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractVector, Object> asVector() {
        pipelineBuilder().appendAsVector(false, false, false, true, null);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
    }

    public CoercedPhaseBuilder<RAbstractVector, Object> asVector(boolean preserveNonVector) {
        pipelineBuilder().appendAsVector(false, false, false, preserveNonVector, null);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
    }

    public CoercedPhaseBuilder<RAbstractVector, Object> asVectorPreserveAttrs(boolean preserveNonVector) {
        pipelineBuilder().appendAsVector(false, false, true, preserveNonVector, null);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
    }

    public HeadPhaseBuilder<RAttributable> asAttributable(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        pipelineBuilder().appendAsAttributable(preserveNames, preserveDimensions, preserveAttributes);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }
}
