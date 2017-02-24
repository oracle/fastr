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
package com.oracle.truffle.r.nodes.casts;

import static com.oracle.truffle.r.nodes.casts.TypeExpr.atom;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.builtin.casts.ExecutionPathVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.AndFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.Dim;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.ElementAt;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.NATest;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.ScalarValue;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.StringLength;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.SubjectVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.VectorSize;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.DoubleFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.FilterVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MatrixFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MissingFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NotFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NullFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.OrFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.RTypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapDoubleToInt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToCharAt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToValue;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapperVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.AttributableCoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.BoxPrimitiveStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultErrorStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultWarningStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.nodes.unary.CastComplexNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleBaseNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerBaseNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalBaseNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNode;
import com.oracle.truffle.r.nodes.unary.CastStringBaseNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class ResultTypesAnalyser extends ExecutionPathVisitor<TypeExpr> implements MapperVisitor<TypeExpr>, FilterVisitor<TypeExpr>, SubjectVisitor<TypeExpr> {

    private static final TypeExpr NOT_NULL_NOT_MISSING = atom(RNull.class).not().and(atom(RMissing.class).not());

    public static TypeExpr analyse(PipelineStep<?, ?> firstStep) {
        return analyse(firstStep, TypeExpr.ANYTHING);
    }

    public static TypeExpr analyse(PipelineStep<?, ?> firstStep, TypeExpr inputType) {
        List<TypeExpr> pathResults = new ResultTypesAnalyser().visitPaths(firstStep, inputType);
        return pathResults.stream().reduce((x, y) -> x.or(y)).get();
    }

    @Override
    public TypeExpr visit(FindFirstStep<?, ?> step, TypeExpr inputType) {
        TypeExpr rt;
        if (step.getElementClass() == null || step.getElementClass() == Object.class) {
            if (inputType.isAnything()) {
                rt = atom(RAbstractVector.class).not();
            } else {
                Set<Type> resTypes = inputType.classify().stream().map(c -> CastUtils.elementType(c)).collect(Collectors.toSet());
                rt = TypeExpr.union(resTypes);
            }
        } else {
            rt = atom(step.getElementClass());
        }

        // findFirstOrNull
        if (step.getDefaultValue() == RNull.instance) {
            rt = rt.or(atom(RNull.class));
        }

        if (step.getDefaultValue() != null) {
            rt = rt.positiveSamples(step.getDefaultValue());
        } else {
            rt = rt.and(atom(RNull.class).not()).and(atom(RMissing.class).not());
        }

        return rt;
    }

    private static TypeExpr inferResultTypeFromSpecializations(Class<? extends CastNode> castNodeClass, TypeExpr inputType) {
        return CastUtils.Casts.createCastNodeCasts(castNodeClass).narrow(inputType);
    }

    @Override
    public TypeExpr visit(CoercionStep<?, ?> step, TypeExpr inputType) {
        RType type = step.getType();
        TypeExpr res;
        switch (type) {
            case Integer:
                res = step.vectorCoercion ? inferResultTypeFromSpecializations(CastIntegerNode.class, inputType) : inferResultTypeFromSpecializations(CastIntegerBaseNode.class, inputType);
                break;
            case Double:
                res = step.vectorCoercion ? inferResultTypeFromSpecializations(CastDoubleNode.class, inputType) : inferResultTypeFromSpecializations(CastDoubleBaseNode.class, inputType);
                break;
            case Character:
                res = step.vectorCoercion ? inferResultTypeFromSpecializations(CastStringNode.class, inputType) : inferResultTypeFromSpecializations(CastStringBaseNode.class, inputType);
                break;
            case Logical:
                res = step.vectorCoercion ? inferResultTypeFromSpecializations(CastLogicalNode.class, inputType) : inferResultTypeFromSpecializations(CastLogicalBaseNode.class, inputType);
                break;
            case Complex:
                res = inferResultTypeFromSpecializations(CastComplexNode.class, inputType);
                break;
            case Raw:
                res = inferResultTypeFromSpecializations(CastRawNode.class, inputType);
                break;
            case Any:
                TypeExpr funOrVecTp = atom(RFunction.class).or(atom(RAbstractVector.class));
                res = inputType.and(funOrVecTp);
                break;
            default:
                throw RInternalError.shouldNotReachHere(Utils.stringFormat("Unsupported type '%s' in AsVectorStep", type));
        }

        if (step.preserveNonVector) {
            TypeExpr maskedNullMissing = inputType.and(atom(RNull.class).or(atom(RMissing.class)));
            res = res.or(maskedNullMissing);
        }

        return res;
    }

    @Override
    public TypeExpr visit(MapStep<?, ?> step, TypeExpr inputType) {
        return step.getMapper().accept(this, inputType);
    }

    @Override
    protected TypeExpr visitBranch(MapIfStep<?, ?> step, TypeExpr inputType, boolean visitTrueBranch) {
        TypeExpr filterRes = visitFilter(step.getFilter(), inputType);
        if (visitTrueBranch) {
            if (step.isReturns()) {
                TypeExpr returnedType = trueBranchResultTypes(step, inputType, filterRes);
                return returnedType;
            } else {
                return trueBranchResultTypes(step, inputType, filterRes);
            }
        } else {
            return falseBranchResultTypes(step, inputType, filterRes);
        }
    }

    private static TypeExpr trueBranchResultTypes(MapIfStep<?, ?> step, TypeExpr inputType, TypeExpr filterRes) {
        TypeExpr filterTrueCaseType = inputType.and(filterRes);
        if (step.getTrueBranch() != null) {
            return analyse(step.getTrueBranch(), filterTrueCaseType);
        } else {
            return filterTrueCaseType;
        }
    }

    private static TypeExpr falseBranchResultTypes(MapIfStep<?, ?> step, TypeExpr inputType, TypeExpr filterRes) {
        TypeExpr filterFalseCaseType = inputType.and(filterRes.not());
        if (step.getFalseBranch() != null) {
            return analyse(step.getFalseBranch(), filterFalseCaseType);
        } else {
            return filterFalseCaseType;
        }
    }

    @Override
    public TypeExpr visit(FilterStep<?, ?> step, TypeExpr inputType) {
        if (step.isWarning()) {
            return inputType;
        } else {
            return inputType.and(visitFilter(step.getFilter(), inputType));
        }
    }

    @Override
    public TypeExpr visit(NotNAStep<?> step, TypeExpr inputType) {
        Set<Object> naSamples = inputType.toNormalizedConjunctionSet().stream().filter(t -> t instanceof Class).map(t -> CastUtils.naValue((Class<?>) t)).filter(x -> x != null).collect(
                        Collectors.toSet());
        TypeExpr resType = inputType.lower(step);
        resType = resType.negativeSamples(naSamples);
        if (step.getReplacement() != null) {
            resType = resType.positiveSamples(step.getReplacement());
        }
        return resType;
    }

    @Override
    public TypeExpr visit(DefaultErrorStep<?> step, TypeExpr inputType) {
        return inputType;
    }

    @Override
    public TypeExpr visit(DefaultWarningStep<?> step, TypeExpr inputType) {
        return inputType;
    }

    @Override
    public TypeExpr visit(BoxPrimitiveStep<?> step, TypeExpr inputType) {
        TypeExpr noPrimType = atom(Integer.class).not().and(atom(Byte.class).not()).and(atom(Double.class).not()).and(atom(String.class).not());
        // cancel potential primitive types in the input
        TypeExpr noPrimInput = inputType.and(noPrimType);
        // the positive output type union
        TypeExpr res = TypeExpr.union(RInteger.class, RLogical.class, RDouble.class, RString.class);
        // intersect the to stop propagating the primitive types, such as String
        res = res.and(noPrimInput);
        // the output of the boxing is actually the union of the positive union with its negation
        // that represents the fallback output for non-vectors
        TypeExpr negRes = res.not().and(noPrimInput);
        res = res.or(negRes);
        return res;
    }

    @Override
    public TypeExpr visit(AttributableCoercionStep<?> step, TypeExpr inputType) {
        return inferResultTypeFromSpecializations(CastToAttributableNode.class, inputType);
    }

    // MapperVisitor

    @Override
    public TypeExpr visit(MapToValue<?, ?> mapper, TypeExpr inputType) {
        return atom(mapper.getValue().getClass()).lower(mapper);
    }

    @Override
    public TypeExpr visit(MapByteToBoolean mapper, TypeExpr inputType) {
        return atom(Boolean.class);
    }

    @Override
    public TypeExpr visit(MapDoubleToInt mapper, TypeExpr inputType) {
        return atom(Integer.class);
    }

    @Override
    public TypeExpr visit(MapToCharAt mapper, TypeExpr inputType) {
        return atom(Integer.class);
    }

    // FilterVisitor

    private TypeExpr visitFilter(Filter<?, ?> filter, TypeExpr inputType) {
        return filter.accept(this, inputType);
    }

    @Override
    public TypeExpr visit(TypeFilter<?, ?> filter, TypeExpr inputType) {
        TypeExpr resTp = atom(filter.getType1());
        if (filter.getType2() != null) {
            resTp = resTp.or(atom(filter.getType2()));
        }
        return resTp.and(NOT_NULL_NOT_MISSING);
    }

    @Override
    public TypeExpr visit(RTypeFilter<?> filter, TypeExpr previous) {
        switch (filter.getType()) {
            case Integer:
                return visit(new TypeFilter<>(Integer.class, RAbstractIntVector.class), previous);
            case Double:
                return visit(new TypeFilter<>(Double.class, RAbstractDoubleVector.class), previous);
            case Logical:
                return visit(new TypeFilter<>(Byte.class, RAbstractLogicalVector.class), previous);
            case Complex:
                return visit(new TypeFilter<>(RAbstractComplexVector.class), previous);
            case Character:
                return visit(new TypeFilter<>(String.class, RAbstractStringVector.class), previous);
            case Raw:
                return visit(new TypeFilter<>(RAbstractRawVector.class), previous);
            default:
                throw RInternalError.unimplemented("TODO: more types here");
        }
    }

    @Override
    public TypeExpr visit(CompareFilter<?> filter, TypeExpr previous) {
        return filter.getSubject().accept(this, filter.getOperation(), previous).lower(filter);
    }

    @Override
    public TypeExpr visit(AndFilter<?, ?> filter, TypeExpr previous) {
        TypeExpr res1 = visitFilter(filter.getLeft(), previous);
        TypeExpr res2 = visitFilter(filter.getRight(), res1);
        TypeExpr res = res1.and(res2);
        return res;
    }

    @Override
    public TypeExpr visit(OrFilter<?> filter, TypeExpr previous) {
        TypeExpr res1 = visitFilter(filter.getLeft(), previous);
        TypeExpr res2 = visitFilter(filter.getRight(), previous);
        TypeExpr res = res1.or(res2);
        return res;
    }

    @Override
    public TypeExpr visit(NotFilter<?> filter, TypeExpr previous) {
        return visitFilter(filter.getFilter(), previous).not();
    }

    @Override
    public TypeExpr visit(MatrixFilter<?> filter, TypeExpr previous) {
        return previous.lower(filter).and(NOT_NULL_NOT_MISSING);
    }

    @Override
    public TypeExpr visit(DoubleFilter filter, TypeExpr previous) {
        return previous.lower(filter);
    }

    @Override
    public TypeExpr visit(NullFilter filter, TypeExpr previous) {
        TypeExpr res = atom(RNull.class);
        return res;
    }

    @Override
    public TypeExpr visit(MissingFilter filter, TypeExpr previous) {
        TypeExpr res = atom(RMissing.class);
        return res;
    }

    // CompareFilter.SubjectVisitor

    @Override
    public TypeExpr visit(ScalarValue scalarValue, byte operation, TypeExpr previous) {
        return visitRType(scalarValue.type);
    }

    private static TypeExpr visitRType(RType type) {
        switch (type) {
            case Integer:
                return atom(Integer.class);
            case Double:
                return atom(Double.class);
            case Logical:
                return atom(Byte.class);
            case Complex:
                return atom(RComplex.class);
            case Character:
                return atom(String.class);
            case Raw:
                return atom(RRaw.class);
            case Any:
                return TypeExpr.ANYTHING;
            default:
                throw RInternalError.unimplemented("Unexpected type: " + type);
        }
    }

    @Override
    public TypeExpr visit(NATest naTest, byte operation, TypeExpr previous) {
        return visitRType(naTest.type);
    }

    @Override
    public TypeExpr visit(StringLength stringLength, byte operation, TypeExpr previous) {
        return previous;
    }

    @Override
    public TypeExpr visit(VectorSize vectorSize, byte operation, TypeExpr previous) {
        return previous.and(NOT_NULL_NOT_MISSING);
    }

    @Override
    public TypeExpr visit(ElementAt elementAt, byte operation, TypeExpr previous) {
        return previous.and(NOT_NULL_NOT_MISSING);
    }

    @Override
    public TypeExpr visit(Dim dim, byte operation, TypeExpr previous) {
        return previous.and(NOT_NULL_NOT_MISSING);
    }

}
