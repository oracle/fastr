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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.oracle.truffle.r.nodes.builtin.casts.ExecutionPathVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.AndFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.Dim;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.ElementAt;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.NATest;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.ScalarValue;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.StringLength;
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
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class SamplesCollector extends ExecutionPathVisitor<Consumer<Object>>
                implements FilterVisitor<Consumer<Object>>, CompareFilter.SubjectVisitor<Consumer<Object>>, MapperVisitor<Consumer<Object>> {

    public static Set<Object> collect(PipelineStep<?, ?> firstStep) {
        Set<Object> samples = new HashSet<>();
        collect(firstStep, s -> samples.add(s));
        return samples;
    }

    public static Consumer<Object> collect(PipelineStep<?, ?> firstStep, Consumer<Object> initial) {
        List<Consumer<Object>> pathResults = new SamplesCollector().visitPaths(firstStep, initial);
        return pathResults.stream().reduce((x, y) -> s -> {
            x.accept(s);
            y.accept(s);
        }).get();
    }

    @Override
    public Consumer<Object> visit(FindFirstStep<?, ?> step, Consumer<Object> previous) {
        if (step.getDefaultValue() != null) {
            previous.accept(step.getDefaultValue());
            previous.accept(RNull.instance);
            previous.accept(RMissing.instance);
        }
        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(CoercionStep<?, ?> step, Consumer<Object> previous) {
        Set<?> samples = CastUtils.sampleValuesForClasses(CastUtils.rTypeToClasses(step.getType()));
        for (Object s : samples) {
            previous.accept(s);
        }

        previous.accept(RNull.instance);
        previous.accept(RMissing.instance);

        return s -> {
            if (s instanceof VectorPlaceholder) {
                previous.accept(CastUtils.vectorOfSize(step.type, ((VectorPlaceholder) s).size));
            } else {
                previous.accept(s);
            }
        };
    }

    @Override
    public Consumer<Object> visit(MapStep<?, ?> step, Consumer<Object> previous) {
        previous.accept(RNull.instance);
        previous.accept(RMissing.instance);

        return step.getMapper().accept(this, previous);
    }

    @Override
    protected Consumer<Object> visitBranch(MapIfStep<?, ?> step, Consumer<Object> previous, boolean visitTrueBranch) {

        Consumer<Object> filterConsumer = step.getFilter().accept(this, s -> previous.accept(s));

        PipelineStep<?, ?> branch;
        if (visitTrueBranch) {
            branch = step.getTrueBranch();
        } else {
            branch = step.getFalseBranch();
        }
        Consumer<Object> branchSampler = branch != null ? collect(branch, s -> {
            filterConsumer.accept(s);
        }) : s -> filterConsumer.accept(s);
        return s -> {
            branchSampler.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(FilterStep<?, ?> step, Consumer<Object> previous) {
        previous.accept(RNull.instance);
        previous.accept(RMissing.instance);

        Consumer<Object> filterConsumer = step.getFilter().accept(this, s -> previous.accept(s));

        return s -> {
            filterConsumer.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(NotNAStep<?> step, Consumer<Object> previous) {
        previous.accept(RRuntime.INT_NA);
        previous.accept(RRuntime.DOUBLE_NA);
        previous.accept(RRuntime.LOGICAL_NA);
        previous.accept(RRuntime.STRING_NA);
        previous.accept(RRuntime.LOGICAL_NA);
        previous.accept(RDataFactory.createComplex(RRuntime.COMPLEX_NA_REAL_PART, 0));

        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(DefaultErrorStep<?> step, Consumer<Object> previous) {
        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(DefaultWarningStep<?> step, Consumer<Object> previous) {
        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(BoxPrimitiveStep<?> step, Consumer<Object> previous) {
        previous.accept(0);
        previous.accept(0.0);
        previous.accept((byte) 0);
        previous.accept("");
        previous.accept(RNull.instance);
        previous.accept(RMissing.instance);

        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(AttributableCoercionStep<?> step, Consumer<Object> previous) {
        previous.accept(RDataFactory.createInternalEnv());
        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(MapToValue<?, ?> mapper, Consumer<Object> previous) {
        return s -> {
            // no inverse mapping for constants
        };
    }

    @Override
    public Consumer<Object> visit(MapByteToBoolean mapper, Consumer<Object> previous) {
        return s -> {
            previous.accept(RRuntime.asLogical((Boolean) s));
        };
    }

    @Override
    public Consumer<Object> visit(MapDoubleToInt mapper, Consumer<Object> previous) {
        return s -> {
            previous.accept(((Integer) s).doubleValue());
        };
    }

    @Override
    public Consumer<Object> visit(MapToCharAt mapper, Consumer<Object> previous) {
        previous.accept(sampleString(mapper.getIndex()) + (char) mapper.getDefaultValue());
        return s -> {
            previous.accept(("" + (char) s));
        };
    }

    // Filter visitor

    @Override
    @SuppressWarnings("rawtypes")
    public Consumer<Object> visit(TypeFilter<?, ?> filter, Consumer<Object> previous) {
        Class<?>[] filterTypes = new Class[]{filter.getType1(), filter.getType2()};
        Set<?> samples = CastUtils.sampleValuesForClasses(filterTypes);
        for (Object s : samples) {
            previous.accept(s);
        }
        // a negative sample
        previous.accept(RNull.instance);

        return s -> {
            if (s instanceof VectorPlaceholder) {
                previous.accept(CastUtils.vectorOfSize(filter.getType1(), ((VectorPlaceholder) s).size));
            } else {
                previous.accept(s);
            }
        };
    }

    @Override
    public Consumer<Object> visit(RTypeFilter<?> filter, Consumer<Object> previous) {
        Class<?>[] cls = CastUtils.rTypeToClasses(filter.getType());
        if (cls != null) {
            TypeFilter<?, ?> tf = cls.length == 1 ? new TypeFilter<>(cls[0]) : new TypeFilter<>(cls[0], cls[1]);
            visit(tf, previous);
        }

        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(CompareFilter<?> filter, Consumer<Object> previous) {
        Consumer<Object> comparisonConsumer = filter.getSubject().accept(this, filter.getOperation(), previous);

        return s -> {
            comparisonConsumer.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(AndFilter<?, ?> filter, Consumer<Object> previous) {
        Consumer<Object> leftConsumer = filter.getLeft().accept(this, s -> previous.accept(s));
        Consumer<Object> rightConsumer = filter.getRight().accept(this, s -> leftConsumer.accept(s));

        return s -> {
            rightConsumer.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(OrFilter<?> filter, Consumer<Object> previous) {
        Consumer<Object> leftConsumer = filter.getLeft().accept(this, s -> previous.accept(s));
        Consumer<Object> rightConsumer = filter.getRight().accept(this, s -> leftConsumer.accept(s));

        return s -> {
            rightConsumer.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(NotFilter<?> filter, Consumer<Object> previous) {
        Consumer<Object> notConsumer = filter.getFilter().accept(this, s -> previous.accept(s));

        return s -> {
            notConsumer.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(MatrixFilter<?> filter, Consumer<Object> previous) {
        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(DoubleFilter filter, Consumer<Object> previous) {
        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(NullFilter filter, Consumer<Object> previous) {
        previous.accept(RNull.instance);

        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(MissingFilter filter, Consumer<Object> previous) {
        previous.accept(RMissing.instance);

        return s -> {
            previous.accept(s);
        };
    }

    // Subject of comparison visitor

    @Override
    public Consumer<Object> visit(ScalarValue scalarValue, byte operation, Consumer<Object> previous) {
        previous.accept(scalarValue.value);
        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(NATest naTest, byte operation, Consumer<Object> previous) {
        switch (naTest.type) {
            case Integer:
                previous.accept(RRuntime.INT_NA);
                break;
            case Double:
                previous.accept(RRuntime.DOUBLE_NA);
                break;
            case Logical:
                previous.accept(RRuntime.LOGICAL_NA);
                break;
            case Character:
                previous.accept(RRuntime.STRING_NA);
                break;
            case Complex:
                previous.accept(RDataFactory.createComplex(RRuntime.COMPLEX_NA_REAL_PART, 0));
                break;
            default:
                throw RInternalError.shouldNotReachHere();
        }
        return s -> {
            previous.accept(s);
        };
    }

    @Override
    public Consumer<Object> visit(StringLength stringLength, byte operation, Consumer<Object> previous) {
        previous.accept(sampleString(stringLength.length));
        // a negative sample
        previous.accept(sampleString(stringLength.length + 1));

        return s -> {
            previous.accept(s);
        };
    }

    private static String sampleString(int len) {
        if (len < 0) {
            return null;
        } else if (len == 0) {
            return "";
        } else {
            char[] ch = new char[len];
            Arrays.fill(ch, 'a');
            return String.valueOf(ch);
        }
    }

    @Override
    public Consumer<Object> visit(VectorSize vectorSize, byte operation, Consumer<Object> previous) {
        int s = vectorSize.size;
        switch (operation) {
            case CompareFilter.EQ:
                previous.accept(new VectorPlaceholder(s));
                // a negative sample
                previous.accept(new VectorPlaceholder(s + 1));
                break;
            case CompareFilter.GT:
                previous.accept(new VectorPlaceholder(s + 1));
                // a negative sample
                previous.accept(new VectorPlaceholder(s));
                break;
            case CompareFilter.LT:
                previous.accept(new VectorPlaceholder(s - 1));
                // a negative sample
                previous.accept(new VectorPlaceholder(s));
                break;
            case CompareFilter.GE:
                previous.accept(new VectorPlaceholder(s));
                // a negative sample
                previous.accept(new VectorPlaceholder(s - 1));
                break;
            case CompareFilter.LE:
                previous.accept(new VectorPlaceholder(s));
                // a negative sample
                previous.accept(new VectorPlaceholder(s + 1));
                break;
            default:
                throw RInternalError.shouldNotReachHere();
        }

        return sample -> {
            previous.accept(sample);
        };
    }

    @Override
    public Consumer<Object> visit(ElementAt elementAt, byte operation, Consumer<Object> previous) {
        RAbstractVector vec = CastUtils.fillVector(elementAt.type, elementAt.index + 1, elementAt.value, true);
        previous.accept(vec);

        // a negative sample
        vec = CastUtils.fillVector(elementAt.type, elementAt.index, elementAt.value, true);
        previous.accept(vec);

        return s -> {
            if (s instanceof VectorPlaceholder) {
                VectorPlaceholder vectorPlaceholder = (VectorPlaceholder) s;
                if (elementAt.index < vectorPlaceholder.size) {
                    previous.accept(CastUtils.fillVector(elementAt.type, vectorPlaceholder.size, elementAt.value, true));
                } else {
                    previous.accept(s);
                }
            } else {
                previous.accept(s);
            }
        };
    }

    @Override
    public Consumer<Object> visit(Dim dim, byte operation, Consumer<Object> previous) {
        return s -> {
            previous.accept(s);
        };
    }

    private static final class VectorPlaceholder {
        final int size;

        VectorPlaceholder(int size) {
            this.size = size;
        }
    }

}
