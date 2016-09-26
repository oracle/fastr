package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
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

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch);
        return new InitialPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch);
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

    /**
     * This method should be used as a step in pipeline, not as an argument to {@code mustBe}.
     * Example: {@code casts.arg("x").notNA()}.
     */
    public InitialPhaseBuilder<T> notNA() {
        pipelineBuilder().appendNotNA(null, null, null, null);
        return this;
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        pipelineBuilder().appendAsIntegerVector(preserveNames, dimensionsPreservation, attrPreservation);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Integer.class);
    }

    public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector() {
        return asIntegerVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        pipelineBuilder().appendAsDoubleVector(preserveNames, dimensionsPreservation, attrPreservation);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Double.class);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector() {
        return asDoubleVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Byte> asLogicalVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        pipelineBuilder().appendAsLogicalVector(preserveNames, dimensionsPreservation, attrPreservation);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Byte.class);
    }

    public CoercedPhaseBuilder<RAbstractDoubleVector, Byte> asLogicalVector() {
        return asLogicalVector(false, false, false);
    }

    public CoercedPhaseBuilder<RAbstractStringVector, String> asStringVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        pipelineBuilder().appendAsStringVector(preserveNames, dimensionsPreservation, attrPreservation);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), String.class);
    }

    public CoercedPhaseBuilder<RAbstractStringVector, String> asStringVector() {
        pipelineBuilder().appendAsStringVector();
        return new CoercedPhaseBuilder<>(pipelineBuilder(), String.class);
    }

    public CoercedPhaseBuilder<RAbstractComplexVector, RComplex> asComplexVector() {
        pipelineBuilder().appendAsComplexVector();
        return new CoercedPhaseBuilder<>(pipelineBuilder(), RComplex.class);
    }

    public CoercedPhaseBuilder<RAbstractRawVector, RRaw> asRawVector() {
        pipelineBuilder().appendAsRawVector();
        return new CoercedPhaseBuilder<>(pipelineBuilder(), RRaw.class);
    }

    public CoercedPhaseBuilder<RAbstractVector, Object> asVector() {
        // TODO: asVector() sets preserveNonVector to false, which is against intended semantics
        // of cast nodes that always forward RNull, we need to revise all calls to this method
        // and remove the preserveNonVector option of CastToVectorNode
        pipelineBuilder().appendAsVector();
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
    }

    public CoercedPhaseBuilder<RAbstractVector, Object> asVector(boolean preserveNonVector) {
        pipelineBuilder().appendAsVector(false, false, false, preserveNonVector);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
    }

    public CoercedPhaseBuilder<RAbstractVector, Object> asVectorPreserveAttrs(boolean preserveNonVector) {
        pipelineBuilder().appendAsVector(false, false, true, false);
        return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
    }

    public HeadPhaseBuilder<RAttributable> asAttributable(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        pipelineBuilder().appendAsAttributable(preserveNames, dimensionsPreservation, attrPreservation);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

}
