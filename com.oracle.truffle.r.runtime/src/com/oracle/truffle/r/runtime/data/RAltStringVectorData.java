package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ops.na.NACheck;


@ExportLibrary(VectorDataLibrary.class)
public class RAltStringVectorData implements TruffleObject, VectorDataWithOwner {
    private final RAltRepData altrepData;
    private final AltStringClassDescriptor descriptor;
    private RStringVector owner;

    public RAltStringVectorData(AltStringClassDescriptor descriptor, RAltRepData altRepData) {
        assert hasDescriptorRegisteredNecessaryMethods(descriptor);
        this.altrepData = altRepData;
        this.descriptor = descriptor;
    }

    private static boolean hasDescriptorRegisteredNecessaryMethods(AltStringClassDescriptor descriptor) {
        return descriptor.isLengthMethodRegistered()
                && descriptor.isDataptrMethodRegistered()
                && descriptor.isEltMethodRegistered()
                && descriptor.isSetEltMethodRegistered();
    }

    @Override
    public void setOwner(RAbstractContainer owner) {
        this.owner = (RStringVector) owner;
    }

    public RAltRepData getAltrepData() {
        return altrepData;
    }

    public AltStringClassDescriptor getDescriptor() {
        return descriptor;
    }

    @ExportMessage
    public int getLength(@Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        return lengthNode.execute(owner);
    }

    @ExportMessage
    public RType getType() {
        return RType.Character;
    }

    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getDisabled();
    }

    @ExportMessage
    public RStringArrayVectorData materialize(@Shared("eltNode") @Cached AltrepRFFI.EltNode eltNode,
                                              @Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        return new RStringArrayVectorData(getStringDataCopy(eltNode, lengthNode), RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public Object copy(boolean deep) {
        throw RInternalError.unimplemented("copy");
    }

    // Access to the elements:

    @ExportMessage
    public SeqIterator iterator(@Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        return new SeqIterator(null, getLength(lengthNode));
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
        return new RandomAccessIterator(null);
    }

    @ExportMessage
    public String[] getStringDataCopy(@Shared("eltNode") @Cached AltrepRFFI.EltNode eltNode,
                                      @Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        final int length = getLength(lengthNode);
        String[] stringData = new String[length];
        for (int i = 0; i < length; i++) {
            stringData[i] = (String) eltNode.execute(owner, i);
        }
        return stringData;
    }

    @ExportMessage
    public String getStringAt(int index,
                              @Shared("eltNode") @Cached AltrepRFFI.EltNode eltNode) {
        return (String) eltNode.execute(owner, index);
    }

    @ExportMessage
    public String getNextString(SeqIterator it,
                                @Shared("eltNode") @Cached AltrepRFFI.EltNode eltNode) {
        return (String) eltNode.execute(owner, it.getIndex());
    }

    // Write access to the elements:

    @ExportMessage
    public SeqWriteIterator writeIterator(@Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        return new SeqWriteIterator(null, getLength(lengthNode));
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(null);
    }

    @ExportMessage
    public void setStringAt(int index, String value,
                            @Exclusive @Cached AltrepRFFI.SetEltNode setEltNode) {
        setEltNode.execute(owner, index, value);
    }

    @ExportMessage
    public void setNextString(SeqWriteIterator it, String value,
                              @Exclusive @Cached AltrepRFFI.SetEltNode setEltNode) {
            setEltNode.execute(owner, it.getIndex(), value);
    }

    @ExportMessage
    public void setString(@SuppressWarnings("unused") RandomAccessWriteIterator it, int index, String value,
                          @Exclusive @Cached AltrepRFFI.SetEltNode setEltNode) {
        setEltNode.execute(owner, index, value);
    }
}
