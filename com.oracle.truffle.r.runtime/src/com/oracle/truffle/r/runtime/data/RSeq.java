package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

// TODO: should replace the RSequence, once all usages or RSequence are removed, we can rename this
public interface RSeq {
    Object getStartObject();
    Object getStrideObject();
    int getLength();
}
