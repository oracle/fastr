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
import com.oracle.truffle.r.nodes.casts.ResultTypesAnalyser.AltTypeExpr;
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

public class ResultTypesAnalyser extends ExecutionPathVisitor<AltTypeExpr> implements MapperVisitor<TypeExpr>, FilterVisitor<TypeExpr>, SubjectVisitor<TypeExpr> {

    static final class AltTypeExpr {
        final TypeExpr main;
        final TypeExpr alt;

        private AltTypeExpr(TypeExpr mainBranch, TypeExpr altBranch) {
            this.main = mainBranch;
            this.alt = altBranch;
        }

        static AltTypeExpr create() {
            return new AltTypeExpr(TypeExpr.ANYTHING, null);
        }

        TypeExpr merge() {
            return alt == null ? main : main.or(alt);
        }

        AltTypeExpr setMain(TypeExpr newMainType) {
            return newMainType == null ? this : new AltTypeExpr(newMainType, alt);
        }

        AltTypeExpr addAlt(TypeExpr newAltType) {
            return newAltType == null ? this : new AltTypeExpr(main, alt == null ? newAltType : alt.or(newAltType));
        }

        AltTypeExpr or(AltTypeExpr other) {
            return setMain(main.or(other.main)).addAlt(other.alt);
        }

    }

    public static TypeExpr analyse(PipelineStep<?, ?> firstStep) {
        return analyse(firstStep, AltTypeExpr.create()).merge();
    }

    public static AltTypeExpr analyse(PipelineStep<?, ?> firstStep, AltTypeExpr inputType) {
        List<AltTypeExpr> pathResults = new ResultTypesAnalyser().visitPaths(firstStep, inputType);
        return pathResults.stream().reduce((x, y) -> x.or(y)).get();
    }

    @Override
    public AltTypeExpr visit(FindFirstStep<?, ?> step, AltTypeExpr inputType) {
        TypeExpr rt;
        if (step.getElementClass() == null || step.getElementClass() == Object.class) {
            if (inputType.main.isAnything()) {
                rt = atom(RAbstractVector.class).not();
            } else {
                Set<Type> resTypes = inputType.main.classify().stream().map(c -> CastUtils.elementType(c)).collect(Collectors.toSet());
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

        return inputType.setMain(rt);
    }

    private static TypeExpr inferResultTypeFromSpecializations(Class<? extends CastNode> castNodeClass, TypeExpr inputType) {
        return CastUtils.Casts.createCastNodeCasts(castNodeClass).narrow(inputType);
    }

    @Override
    public AltTypeExpr visit(CoercionStep<?, ?> step, AltTypeExpr inputType) {
        RType type = step.getType();
        TypeExpr res;
        switch (type) {
            case Integer:
                res = step.vectorCoercion ? inferResultTypeFromSpecializations(CastIntegerNode.class, inputType.main) : inferResultTypeFromSpecializations(CastIntegerBaseNode.class, inputType.main);
                break;
            case Double:
                res = step.vectorCoercion ? inferResultTypeFromSpecializations(CastDoubleNode.class, inputType.main) : inferResultTypeFromSpecializations(CastDoubleBaseNode.class, inputType.main);
                break;
            case Character:
                res = step.vectorCoercion ? inferResultTypeFromSpecializations(CastStringNode.class, inputType.main) : inferResultTypeFromSpecializations(CastStringBaseNode.class, inputType.main);
                break;
            case Logical:
                res = step.vectorCoercion ? inferResultTypeFromSpecializations(CastLogicalNode.class, inputType.main) : inferResultTypeFromSpecializations(CastLogicalBaseNode.class, inputType.main);
                break;
            case Complex:
                res = inferResultTypeFromSpecializations(CastComplexNode.class, inputType.main);
                break;
            case Raw:
                res = inferResultTypeFromSpecializations(CastRawNode.class, inputType.main);
                break;
            case Any:
                TypeExpr funOrVecTp = atom(RFunction.class).or(atom(RAbstractVector.class));
                res = inputType.main.and(funOrVecTp);
                break;
            default:
                throw RInternalError.shouldNotReachHere(Utils.stringFormat("Unsupported type '%s' in AsVectorStep", type));
        }

        if (step.preserveNonVector) {
            TypeExpr maskedNullMissing = inputType.main.and(atom(RNull.class).or(atom(RMissing.class)));
            res = res.or(maskedNullMissing);
        }

        return inputType.setMain(res);
    }

    @Override
    public AltTypeExpr visit(MapStep<?, ?> step, AltTypeExpr inputType) {
        return inputType.setMain(step.getMapper().accept(this, inputType.main));
    }

    @Override
    protected AltTypeExpr visitBranch(MapIfStep<?, ?> step, AltTypeExpr inputType, boolean visitTrueBranch) {
        TypeExpr filterRes = visitFilter(step.getFilter(), inputType.main);
        if (visitTrueBranch) {
            if (step.isReturns()) {
                AltTypeExpr returnedType = trueBranchResultTypes(step, inputType, filterRes);
                inputType.addAlt(returnedType.merge());
                return inputType;
            } else {
                return trueBranchResultTypes(step, inputType, filterRes);
            }
        } else {
            return falseBranchResultTypes(step, inputType, filterRes);
        }
    }

    private static AltTypeExpr trueBranchResultTypes(MapIfStep<?, ?> step, AltTypeExpr inputType, TypeExpr filterRes) {
        TypeExpr filterTrueCaseType = inputType.main.and(filterRes);
        if (step.getTrueBranch() != null) {
            return analyse(step.getTrueBranch(), inputType.setMain(filterTrueCaseType));
        } else {
            return inputType.setMain(filterTrueCaseType);
        }
    }

    private static AltTypeExpr falseBranchResultTypes(MapIfStep<?, ?> step, AltTypeExpr inputType, TypeExpr filterRes) {
        TypeExpr filterFalseCaseType = inputType.main.and(filterRes.not());
        if (step.getFalseBranch() != null) {
            return analyse(step.getFalseBranch(), inputType.setMain(filterFalseCaseType));
        } else {
            return inputType.setMain(filterFalseCaseType);
        }
    }

    @Override
    public AltTypeExpr visit(FilterStep<?, ?> step, AltTypeExpr inputType) {
        if (step.isWarning()) {
            return inputType;
        } else {
            return inputType.setMain(inputType.main.and(visitFilter(step.getFilter(), inputType.main)));
        }
    }

    @Override
    public AltTypeExpr visit(NotNAStep<?> step, AltTypeExpr inputType) {
        Set<Object> naSamples = inputType.main.toNormalizedConjunctionSet().stream().filter(t -> t instanceof Class).map(t -> CastUtils.naValue((Class<?>) t)).filter(x -> x != null).collect(
                        Collectors.toSet());
        TypeExpr resType = inputType.main.lower(step);
        resType = resType.negativeSamples(naSamples);
        if (step.getReplacement() != null) {
            resType = resType.positiveSamples(step.getReplacement());
        }
        return inputType.setMain(resType);
    }

    @Override
    public AltTypeExpr visit(DefaultErrorStep<?> step, AltTypeExpr inputType) {
        return inputType;
    }

    @Override
    public AltTypeExpr visit(DefaultWarningStep<?> step, AltTypeExpr inputType) {
        return inputType;
    }

    @Override
    public AltTypeExpr visit(BoxPrimitiveStep<?> step, AltTypeExpr inputType) {
        TypeExpr res = TypeExpr.union(RNull.class, RMissing.class, RInteger.class, RLogical.class, RDouble.class, RString.class);
        return inputType.setMain(res.or(res.not()));
    }

    @Override
    public AltTypeExpr visit(AttributableCoercionStep<?> step, AltTypeExpr inputType) {
        return inputType.setMain(inferResultTypeFromSpecializations(CastToAttributableNode.class, inputType.main));
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
        return resTp.and(atom(RNull.class).not().and(atom(RMissing.class).not()));
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
        return previous.lower(filter);
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
        return previous;
    }

    @Override
    public TypeExpr visit(ElementAt elementAt, byte operation, TypeExpr previous) {
        return previous;
    }

    @Override
    public TypeExpr visit(Dim dim, byte operation, TypeExpr previous) {
        return previous;
    }

}
