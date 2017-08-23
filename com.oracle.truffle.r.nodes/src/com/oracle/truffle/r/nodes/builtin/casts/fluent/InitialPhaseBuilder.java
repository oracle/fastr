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

/**
 * Defines fluent API methods for building cast pipeline steps for generic argument, which was not
 * cast to any specific type yet. Any method that represents casting of the argument to a specific
 * type returns instance of {@link CoercedPhaseBuilder} with its generic parameters set accordingly.
 */
@SuppressWarnings("unchecked")
public class InitialPhaseBuilder<T> extends ArgCastBuilder<T, InitialPhaseBuilder<T>> {

    public InitialPhaseBuilder(PipelineBuilder builder) {
        super(builder);
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendMustBeStep(argFilter, message, messageArgs);
        return (InitialPhaseBuilder<S>) this;
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter) {
        return mustBe(argFilter, null, (Object[]) null);
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
        mustBe(Predef.instanceOf(cls), message, messageArgs);
        return (InitialPhaseBuilder<S>) this;
    }

    public <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls) {
        mustBe(Predef.instanceOf(cls));
        return (InitialPhaseBuilder<S>) this;
    }

    public <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls, RError.Message message, Object... messageArgs) {
        shouldBe(Predef.instanceOf(cls), message, messageArgs);
        return (InitialPhaseBuilder<S>) this;
    }

    public <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls) {
        shouldBe(Predef.instanceOf(cls));
        return (InitialPhaseBuilder<S>) this;
    }

    public <S> InitialPhaseBuilder<S> map(Mapper<T, S> mapFn) {
        pipelineBuilder().appendMap(mapFn);
        return (InitialPhaseBuilder<S>) this;
    }

    public InitialPhaseBuilder<Object> returnIf(Filter<? super T, ?> argFilter) {
        pipelineBuilder().appendMapIf(argFilter, (PipelineStep<?, ?>) null, (PipelineStep<?, ?>) null, true);
        return (InitialPhaseBuilder<Object>) this;
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<? super S, R> trueBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, false);
        return (InitialPhaseBuilder<Object>) this;
    }

    public <S extends T, R> InitialPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, Mapper<? super S, R> trueBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, true);
        return (InitialPhaseBuilder<Object>) this;
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<? super S, R> trueBranchMapper, Mapper<? super T, ?> falseBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper, false);
        return (InitialPhaseBuilder<Object>) this;
    }

    public <S extends T, R> InitialPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, Mapper<? super S, R> trueBranchMapper, Mapper<? super T, ?> falseBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper, true);
        return (InitialPhaseBuilder<Object>) this;
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, false);
        return (InitialPhaseBuilder<Object>) this;
    }

    public <S extends T, R> InitialPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, true);
        return (InitialPhaseBuilder<Object>) this;
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch, false);
        return (InitialPhaseBuilder<Object>) this;
    }

    public <S extends T, R> InitialPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch, true);
        return (InitialPhaseBuilder<Object>) this;
    }

    public InitialPhaseBuilder<T> mustNotBeNA(RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(null, message, messageArgs);
        return this;
    }

    public InitialPhaseBuilder<T> shouldNotBeNA(T naReplacement, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(naReplacement, message, messageArgs);
        return this;
    }

    public InitialPhaseBuilder<T> replaceNA(T naReplacement) {
        pipelineBuilder().appendNotNA(naReplacement, null, null);
        return this;
    }

    public InitialPhaseBuilder<T> boxPrimitive() {
        pipelineBuilder().appendBoxPrimitive();
        return this;
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean reuseNonShared) {
        pipelineBuilder().appendAsVector(RType.Integer, preserveNames, preserveDimensions, preserveAttributes, reuseNonShared);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Integer.class);
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return asIntegerVector(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector() {
        return asIntegerVector(false, false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(boolean reuseNonShared) {
        return asIntegerVector(false, false, false, reuseNonShared);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean reuseNonShared) {
        pipelineBuilder().appendAsVector(RType.Double, preserveNames, preserveDimensions, preserveAttributes, reuseNonShared);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Double.class);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return asDoubleVector(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector() {
        return asDoubleVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractLogicalVector, Byte> asLogicalVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        pipelineBuilder().appendAsVector(RType.Logical, preserveNames, preserveDimensions, preserveAttributes);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Byte.class);
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
        pipelineBuilder().appendAsVector(RType.Complex, preserveNames, preserveDimensions, preserveAttributes);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), RComplex.class);
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
        pipelineBuilder().appendAsVector(false, false, false, true);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
    }

    public CoercedPhaseBuilder<RAbstractVector, Object> asVector(boolean preserveNonVector) {
        pipelineBuilder().appendAsVector(false, false, false, preserveNonVector);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
    }

    public CoercedPhaseBuilder<RAbstractVector, Object> asVectorPreserveAttrs(boolean preserveNonVector) {
        pipelineBuilder().appendAsVector(false, false, true, preserveNonVector);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
    }

    public HeadPhaseBuilder<RAttributable> asAttributable(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        pipelineBuilder().appendAsAttributable(preserveNames, preserveDimensions, preserveAttributes);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }
}
