package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
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
        return descriptor.isDataptrMethodRegistered() && descriptor.isEltMethodRegistered() &&
                descriptor.isSetEltMethodRegistered() && descriptor.isLengthMethodRegistered();
            /* TODO: && descriptor.isUnserializeMethodRegistered(); */
    }

    public AltStringClassDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String getDataAt(int index) {
        return invokeEltMethodUncached(index);
    }

    private String invokeEltMethodUncached(int index) {
        Object elem = descriptor.invokeEltMethodUncached(this, index);
        assert elem instanceof NativeDataAccess.NativeMirror;
        RBaseObject delegate = ((NativeDataAccess.NativeMirror) elem).getDelegate();
        assert delegate instanceof CharSXPWrapper;
        return ((CharSXPWrapper) delegate).getContents();
    }

    @Override
    public int getLength() {
        return descriptor.invokeLengthMethodUncached(this);
    }

    @Override
    public VectorAccess access() {
        return vectorAccess;
    }

    @Override
    public VectorAccess slowPathAccess() {
        return null;
    }

    @Override
    public String toString() {
        return "RAltStringVector";
    }

    @Override
    public Object getInternalStore() {
        return this;
    }

    public RAltRepData getData() {
        return data;
    }

    public void setData2(Object data2) {
        data.setData2(data2);
    }

    private static final class FastPathAccess extends FastPathVectorAccess.FastPathFromStringAccess {
        private final ConditionProfile hasMirrorProfile = ConditionProfile.createBinaryProfile();
        private final int instanceId;
        @Child private InteropLibrary eltMethodInterop;

        public FastPathAccess(RAltStringVector value) {
            super(value);
            this.eltMethodInterop = InteropLibrary.getFactory().create(value.getDescriptor().getEltMethod());
            this.instanceId = value.hashCode();
        }

        // TODO: This uses same pattern as in RAltIntegerVec. Does this actually work?
        @Override
        public boolean supports(Object value) {
            if (!(value instanceof RAltStringVector)) {
                return false;
            }
            return instanceId == value.hashCode();
        }

        @Override
        protected String getStringImpl(AccessIterator accessIter, int index) {
            Object elem = getDescriptorFromIterator(accessIter).invokeEltMethodCached(getInstanceFromIterator(accessIter), index,
                    eltMethodInterop, hasMirrorProfile);
            assert elem instanceof NativeDataAccess.NativeMirror || elem instanceof CharSXPWrapper;
            if (elem instanceof NativeDataAccess.NativeMirror) {
                RBaseObject delegate = ((NativeDataAccess.NativeMirror) elem).getDelegate();
                assert delegate instanceof CharSXPWrapper;
                return ((CharSXPWrapper) delegate).getContents();
            } else {
                return ((CharSXPWrapper) elem).getContents();
            }
        }

        @Override
        protected void setStringImpl(AccessIterator accessIter, int index, String value) {
            getDescriptorFromIterator(accessIter).invokeSetEltMethodCached(
                    getInstanceFromIterator(accessIter), index, value, eltMethodInterop, hasMirrorProfile
            );
        }

        private AltStringClassDescriptor getDescriptorFromIterator(AccessIterator iterator) {
            assert iterator.getStore() instanceof RAltStringVector;
            return ((RAltStringVector) iterator.getStore()).getDescriptor();
        }

        private RAltStringVector getInstanceFromIterator(AccessIterator iterator) {
            assert iterator.getStore() instanceof RAltStringVector;
            return (RAltStringVector) iterator.getStore();
        }
    }
}
