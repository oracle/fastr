package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public class RAltStringVector extends RAbstractStringVector implements RAbstractVector.RMaterializedVector {
    private final AltStringClassDescriptor descriptor;
    private final RAltRepData data;
    private final VectorAccess vectorAccess;

    public RAltStringVector(AltStringClassDescriptor descriptor, RAltRepData data, boolean complete) {
        // TODO: Complete = true?
        super(complete);
        setAltRep();
        this.descriptor = descriptor;
        this.data = data;
        assert hasDescriptorRegisteredNecessaryMethods(descriptor):
                "Descriptor" + descriptor + " does not have registered all necessary methods";
        this.vectorAccess = new RAltStringVector.FastPathAccess(this);
    }

    private static boolean hasDescriptorRegisteredNecessaryMethods(AltStringClassDescriptor descriptor) {
        return descriptor.isEltMethodRegistered() && descriptor.isSetEltMethodRegistered()
                && descriptor.isLengthMethodRegistered();
            /* TODO: && descriptor.isUnserializeMethodRegistered(); */
    }

    public AltStringClassDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String getDataAt(int index) {
        return null;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public VectorAccess access() {
        return vectorAccess;
    }

    @Override
    public VectorAccess slowPathAccess() {
        return null;
    }

    public RAltRepData getData() {
        return data;
    }

    public void setData2(Object data2) {
        data.setData2(data2);
    }

    private static final class FastPathAccess extends FastPathVectorAccess.FastPathFromStringAccess {
        public FastPathAccess(RAltStringVector value) {
            super(value);
        }

        @Override
        protected String getStringImpl(AccessIterator accessIter, int index) {
            return null;
        }
    }
}
