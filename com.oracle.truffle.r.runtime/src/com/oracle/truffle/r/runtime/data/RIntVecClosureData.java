package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import static com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary.notWriteableError;

@ExportLibrary(RIntVectorDataLibrary.class)
public class RIntVecClosureData extends RIntVectorData implements RClosure {
    private final RAbstractVector vector;

    public RIntVecClosureData(RAbstractVector vector) {
        this.vector = vector;
    }

    @ExportMessage
    @Override
    public int getLength() {
        return vector.getLength();
    }

    @ExportMessage
    public RIntArrayVectorData materialize() {
        throw new RuntimeException("TODO?");
    }

    @ExportMessage
    public boolean isWriteable() {
        return false;
    }

    @ExportMessage
    public RIntArrayVectorData copy(@SuppressWarnings("unused") boolean deep) {
        throw new RuntimeException("TODO?");
    }

    @ExportMessage
    public RIntArrayVectorData copyResized(int newSize, boolean deep, boolean fillNA) {
        throw new RuntimeException("TODO?");
    }

    // TODO: this will be message exported by the generic VectorDataLibrary
    // @ExportMessage
    public void transferElement(RVectorData destination, int index,
                                @CachedLibrary("destination") RIntVectorDataLibrary dataLib) {
        dataLib.setIntAt((RIntVectorData) destination, index, getIntAt(index));
    }

    @ExportMessage
    public boolean isComplete() {
        return vector.isComplete();
    }

    @ExportMessage
    public int[] getReadonlyIntData() {
        throw new RuntimeException("TODO?");
    }

    @ExportMessage
    public int[] getIntDataCopy() {
        throw new RuntimeException("TODO?");
    }

    // TODO: the accesses may be done more efficiently with nodes and actually using the "store" in the iterator object

    @ExportMessage
    public SeqIterator iterator() {
        return new SeqIterator(null, getLength());
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(null, getLength());
    }

    @ExportMessage
    @Override
    public int getIntAt(int index) {
        VectorAccess access = vector.slowPathAccess();
        RandomIterator it = access.randomAccess(vector);
        return access.getInt(it, index);
    }

    @ExportMessage
    public int getNext(SeqIterator it) {
        return getIntAt(it.getIndex());
    }

    @ExportMessage
    public int getAt(RandomAccessIterator it, int index) {
        return getIntAt(index);
    }

    // RClosure overrides:

    @Override
    public Object getDelegateDataAt(int idx) {
        return vector.getDataAtAsObject(idx);
    }

    @Override
    public RAbstractVector getDelegate() {
        return vector;
    }
}
