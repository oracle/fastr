package com.oracle.truffle.r.nodes.casts;

import java.util.function.Predicate;

import com.oracle.truffle.r.nodes.builtin.VectorPredicateArgumentFilter;
import com.oracle.truffle.r.nodes.casts.ArgumentFilterSampler.ArgumentValueFilterSampler;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class VectorPredicateArgumentFilterSampler<T extends RAbstractVector> extends VectorPredicateArgumentFilter<T> implements ArgumentValueFilterSampler<T> {

    public VectorPredicateArgumentFilterSampler(Predicate<T> valuePredicate, boolean isNullable) {
        super(valuePredicate, isNullable);
    }

    public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
        return Samples.empty();
    }

    public TypeExpr allowedTypes() {
        return TypeExpr.ANYTHING;
    }

}
