package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;


@ExportLibrary(VectorDataLibrary.class)
public class RAltStringVectorData extends RAltrepVectorData {
    private final AltStringClassDescriptor descriptor;

    public RAltStringVectorData(AltStringClassDescriptor descriptor, RAltRepData altRepData) {
        super(altRepData);
        assert hasDescriptorRegisteredNecessaryMethods(descriptor);
        this.descriptor = descriptor;
    }

    private static boolean hasDescriptorRegisteredNecessaryMethods(AltStringClassDescriptor descriptor) {
        return descriptor.isLengthMethodRegistered()
                && descriptor.isDataptrMethodRegistered()
                && descriptor.isEltMethodRegistered()
                && descriptor.isSetEltMethodRegistered();
    }

    public AltStringClassDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    @ExportMessage
    public RType getType() {
        return RType.Character;
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

    @ExportMessage
    public void setStringAt(int index, String value,
                            @Exclusive @Cached AltrepRFFI.SetEltNode setEltNode) {
        setEltNode.execute((RStringVector) owner, index, value);
    }

    @ExportMessage
    public void setNextString(SeqWriteIterator it, String value,
                              @Exclusive @Cached AltrepRFFI.SetEltNode setEltNode) {
            setEltNode.execute((RStringVector) owner, it.getIndex(), value);
    }

    @ExportMessage
    public void setString(@SuppressWarnings("unused") RandomAccessWriteIterator it, int index, String value,
                          @Exclusive @Cached AltrepRFFI.SetEltNode setEltNode) {
        setEltNode.execute((RStringVector) owner, index, value);
    }
}
