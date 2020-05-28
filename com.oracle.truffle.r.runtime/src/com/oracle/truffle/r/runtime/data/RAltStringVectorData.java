package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.ops.na.NACheck;


@ExportLibrary(VectorDataLibrary.class)
public class RAltStringVectorData implements TruffleObject, VectorDataWithOwner {

    public RAltStringVectorData(AltStringClassDescriptor descriptor, RAltRepData altRepData) {
        assert hasDescriptorRegisteredNecessaryMethods(descriptor);
    }

    private boolean hasDescriptorRegisteredNecessaryMethods(AltStringClassDescriptor descriptor) {
        return descriptor.isLengthMethodRegistered()
                && descriptor.isDataptrMethodRegistered()
                && descriptor.isEltMethodRegistered()
                && descriptor.isSetEltMethodRegistered();
    }

    @Override
    public void setOwner(RAbstractContainer owner) {

    }

    public RAltRepData getAltrepData() {
        return null;
    }

    public AltStringClassDescriptor getDescriptor() {
        return null;
    }

    @ExportMessage
    public int getLength() {
        throw RInternalError.unimplemented("getLength");
    }

    @ExportMessage
    public RType getType() {
        return RType.Character;
    }

    @ExportMessage
    public Object materialize() {
        throw RInternalError.unimplemented("materialize");
    }

    @ExportMessage
    public Object copy(boolean deep) {
        throw RInternalError.unimplemented("copy");
    }

    @ExportMessage
    public SeqIterator iterator() {
        throw RInternalError.unimplemented("iterato");
    }

    @ExportMessage
    public boolean nextImpl(SeqIterator it, boolean loopCondition) {
        throw RInternalError.unimplemented("nextImpl");
    }

    @ExportMessage
    public void nextWithWrap(SeqIterator it) {
        throw RInternalError.unimplemented("nextWithWrap");
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        throw RInternalError.unimplemented("randomAccessIterator");
    }

    @ExportMessage
    public NACheck getNACheck() {
        throw RInternalError.unimplemented("getNACheck");
    }
}
