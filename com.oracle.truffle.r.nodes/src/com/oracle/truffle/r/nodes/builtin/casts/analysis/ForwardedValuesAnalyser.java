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

import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.AndFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.Dim;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.ElementAt;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.NATest;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.ScalarValue;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.StringLength;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.VectorSize;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.DoubleFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MatrixFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MissingFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NotFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NullFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.OrFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.RTypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapDoubleToInt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToCharAt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToValue;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.AttributableCoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.BoxPrimitiveStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.PipelineStepVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.analysis.ForwardingStatus.Forwarded;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RNull;

public final class ForwardedValuesAnalyser implements PipelineStepVisitor<ForwardingAnalysisResult> {

    private ForwardingAnalysisResult result = new ForwardingAnalysisResult().forwardAll();
    private ForwardingAnalysisResult altResult = null;

    public ForwardingAnalysisResult analyse(PipelineStep<?, ?> firstStep) {
        firstStep.acceptPipeline(this, null);
        ForwardingAnalysisResult mainRes = result;
        if (altResult != null) {
            mainRes = mainRes.or(altResult);
        }
        return mainRes;
    }

    private void addAlternativeResult(ForwardingAnalysisResult res) {
        if (altResult == null) {
            altResult = res;
        } else {
            altResult = altResult.or(res);
        }
    }

    @Override
    public ForwardingAnalysisResult visit(FindFirstStep<?, ?> step, ForwardingAnalysisResult previous) {
        ForwardingAnalysisResult localRes = new ForwardingAnalysisResult().blockAll().setForwardedType(step.getElementClass(), FORWARDED);
        if (step.getDefaultValue() == RNull.instance) {
            // see CoercedPhaseBuilder.findFirstOrNull()
            localRes.setNull(FORWARDED);
        }
        result = result.and(localRes);
        return result;
    }

    @Override
    public ForwardingAnalysisResult visit(CoercionStep<?, ?> step, ForwardingAnalysisResult previous) {
        ForwardingAnalysisResult localRes = new ForwardingAnalysisResult().blockAll().setForwardedType(step.getType(), FORWARDED);
        if (step.preserveNonVector) {
            // i.e. preserve NULL and MISSING
            localRes = localRes.setMissing(FORWARDED).setNull(FORWARDED);
        }
        result = result.and(localRes);
        return result;
    }

    @Override
    public ForwardingAnalysisResult visit(MapStep<?, ?> step, ForwardingAnalysisResult previous) {
        ForwardingAnalysisResult localRes = step.getMapper().accept(new Mapper.MapperVisitor<ForwardingAnalysisResult>() {

            @Override
            public ForwardingAnalysisResult visit(MapToValue<?, ?> mapper, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return ForwardingAnalysisResult.INVALID;
            }

            @Override
            public ForwardingAnalysisResult visit(MapByteToBoolean mapper, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return new ForwardingAnalysisResult().blockAll().setForwardedType(RType.Logical, new Forwarded(mapper));
            }

            @Override
            public ForwardingAnalysisResult visit(MapDoubleToInt mapper, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return ForwardingAnalysisResult.INVALID;
            }

            @Override
            public ForwardingAnalysisResult visit(MapToCharAt mapper, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return ForwardingAnalysisResult.INVALID;
            }
        }, previous);
        result = result.and(localRes);
        return result;
    }

    @Override
    public ForwardingAnalysisResult visit(MapIfStep<?, ?> mapIfStep, ForwardingAnalysisResult previous) {
        // analyze the true branch
        ForwardingAnalysisResult trueBranchFwdRes;
        PipelineStep<?, ?> trueBranchFirstStep = new FilterStep<>(mapIfStep.getFilter(), null, false);
        ForwardedValuesAnalyser trueBranchFwdAnalyser = new ForwardedValuesAnalyser();
        if (mapIfStep.getTrueBranch() != null) {
            trueBranchFirstStep.setNext(mapIfStep.getTrueBranch());
        }
        trueBranchFwdRes = trueBranchFwdAnalyser.analyse(trueBranchFirstStep);

        // analyze the false branch
        ForwardingAnalysisResult falseBranchFwdRes;
        ForwardedValuesAnalyser falseBranchFwdAnalyser = new ForwardedValuesAnalyser();
        PipelineStep<?, ?> falseBranchFirstStep = new FilterStep<>(new NotFilter<>(mapIfStep.getFilter()), null, false);
        if (mapIfStep.getFalseBranch() != null) {
            falseBranchFirstStep.setNext(mapIfStep.getFalseBranch());
        }
        falseBranchFwdRes = falseBranchFwdAnalyser.analyse(falseBranchFirstStep);

        if (mapIfStep.isReturns()) {
            addAlternativeResult(result.and(trueBranchFwdRes));
            result = result.and(falseBranchFwdRes);
        } else {
            result = result.and(trueBranchFwdRes.or(falseBranchFwdRes));
        }

        return result;
    }

    @Override
    public ForwardingAnalysisResult visit(FilterStep<?, ?> step, ForwardingAnalysisResult previous) {
        class ForwardedValuesFilterVisitor implements Filter.FilterVisitor<ForwardingAnalysisResult> {

            @Override
            public ForwardingAnalysisResult visit(TypeFilter<?, ?> filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                ForwardingAnalysisResult res = new ForwardingAnalysisResult().blockAll();
                if (filter.getExtraCondition() == null) {
                    res = res.setForwardedType(filter.getType1(), FORWARDED);
                } else {
                    res = res.setForwardedType(filter.getType1(), UNKNOWN);
                }
                return res;
            }

            @Override
            public ForwardingAnalysisResult visit(RTypeFilter<?> filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return new ForwardingAnalysisResult().blockAll().setForwardedType(filter.getType(), FORWARDED);
            }

            @Override
            public ForwardingAnalysisResult visit(CompareFilter<?> filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return filter.getSubject().accept(new Filter.CompareFilter.SubjectVisitor<ForwardingAnalysisResult>() {

                    @Override
                    public ForwardingAnalysisResult visit(ScalarValue scalarValue, byte operation, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                        return new ForwardingAnalysisResult().blockAll().setForwardedType(scalarValue.type, UNKNOWN);
                    }

                    @Override
                    public ForwardingAnalysisResult visit(NATest naTest, byte operation, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                        return new ForwardingAnalysisResult().blockAll().setForwardedType(naTest.type, UNKNOWN);
                    }

                    @Override
                    public ForwardingAnalysisResult visit(StringLength stringLength, byte operation, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                        return new ForwardingAnalysisResult().blockAll().setForwardedType(RType.Character, UNKNOWN);
                    }

                    @Override
                    public ForwardingAnalysisResult visit(VectorSize vectorSize, byte operation, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                        if (vectorSize.size == 0 && operation == CompareFilter.EQ) {
                            return new ForwardingAnalysisResult().blockAll().setNull(FORWARDED);
                        } else if (vectorSize.size == 1 && (operation == CompareFilter.EQ || operation == CompareFilter.GT || operation == CompareFilter.GE)) {
                            return new ForwardingAnalysisResult().forwardAll().setNull(BLOCKED).setMissing(BLOCKED);
                        } else if (vectorSize.size > 1 && (operation == CompareFilter.EQ || operation == CompareFilter.GT || operation == CompareFilter.GE)) {
                            return new ForwardingAnalysisResult().blockAll();
                        } else if (vectorSize.size > 1 && (operation == CompareFilter.LE || operation == CompareFilter.LT)) {
                            return new ForwardingAnalysisResult().forwardAll().setMissing(BLOCKED);
                        } else {
                            return new ForwardingAnalysisResult().unknownAll().setMissing(BLOCKED);
                        }
                    }

                    @Override
                    public ForwardingAnalysisResult visit(ElementAt elementAt, byte operation, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                        if (elementAt.index == 0) {
                            return new ForwardingAnalysisResult().blockAll().setForwardedType(elementAt.type, UNKNOWN);
                        } else {
                            return new ForwardingAnalysisResult().blockAll();
                        }
                    }

                    @Override
                    public ForwardingAnalysisResult visit(Dim dim, byte operation, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                        return new ForwardingAnalysisResult().blockAll();
                    }
                }, filter.getOperation(), previous);
            }

            @Override
            public ForwardingAnalysisResult visit(AndFilter<?, ?> filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                ForwardedValuesFilterVisitor leftVis = new ForwardedValuesFilterVisitor();
                ForwardedValuesFilterVisitor rightVis = new ForwardedValuesFilterVisitor();
                ForwardingAnalysisResult leftResult = filter.getLeft().accept(leftVis, previous);
                ForwardingAnalysisResult rightResult = filter.getRight().accept(rightVis, previous);
                return leftResult.and(rightResult);
            }

            @Override
            public ForwardingAnalysisResult visit(OrFilter<?> filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                ForwardedValuesFilterVisitor leftVis = new ForwardedValuesFilterVisitor();
                ForwardedValuesFilterVisitor rightVis = new ForwardedValuesFilterVisitor();
                ForwardingAnalysisResult leftResult = filter.getLeft().accept(leftVis, previous);
                ForwardingAnalysisResult rightResult = filter.getRight().accept(rightVis, previous);
                return leftResult.or(rightResult);
            }

            @Override
            public ForwardingAnalysisResult visit(NotFilter<?> filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                ForwardedValuesFilterVisitor vis = new ForwardedValuesFilterVisitor();
                return filter.getFilter().accept(vis, previous).not();
            }

            @Override
            public ForwardingAnalysisResult visit(MatrixFilter<?> filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return new ForwardingAnalysisResult().blockAll();
            }

            @Override
            public ForwardingAnalysisResult visit(DoubleFilter filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return new ForwardingAnalysisResult().blockAll().setForwardedType(RType.Double, UNKNOWN);
            }

            @Override
            public ForwardingAnalysisResult visit(NullFilter filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return new ForwardingAnalysisResult().blockAll().setNull(FORWARDED);
            }

            @Override
            public ForwardingAnalysisResult visit(MissingFilter filter, @SuppressWarnings("hiding") ForwardingAnalysisResult previous) {
                return new ForwardingAnalysisResult().blockAll().setMissing(FORWARDED);
            }
        }
        ForwardingAnalysisResult localRes = step.getFilter().accept(new ForwardedValuesFilterVisitor(), previous);
        result = result.and(localRes);
        return result;
    }

    @Override
    public ForwardingAnalysisResult visit(NotNAStep<?> step, ForwardingAnalysisResult previous) {
        result = result.and(new ForwardingAnalysisResult().unknownAll().setNull(FORWARDED).setMissing(FORWARDED));
        return result;
    }

    @Override
    public ForwardingAnalysisResult visit(BoxPrimitiveStep<?> step, ForwardingAnalysisResult previous) {
        // TODO
        result = ForwardingAnalysisResult.INVALID;
        return result;
    }

    @Override
    public ForwardingAnalysisResult visit(AttributableCoercionStep<?> step, ForwardingAnalysisResult previous) {
        // TODO
        result = ForwardingAnalysisResult.INVALID;
        return result;
    }
}
