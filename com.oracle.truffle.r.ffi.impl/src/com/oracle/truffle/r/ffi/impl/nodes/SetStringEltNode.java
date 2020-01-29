package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.altrep.RAltStringVector;

@GenerateUncached
public abstract class SetStringEltNode extends FFIUpCallNode.Arg3 {
    public static SetStringEltNode create() {
        return SetStringEltNodeGen.create();
    }

    @Specialization
    Object doStringVector(RStringVector vector, long index, CharSXPWrapper element) {
        if (RRuntime.isNA(element.getContents())) {
            vector.setComplete(false);
        }
        vector.setElement((int) index, element);
        return null;
    }

    @Specialization(limit = "1")
    Object doAltstringVector(RAltStringVector altStringVector, long index, CharSXPWrapper element,
                             @CachedLibrary("altStringVector.getDescriptor().getSetEltMethod()") InteropLibrary setEltMethodInterop,
                             @Cached("createBinaryProfile()") ConditionProfile instanceHasMirrorProfile,
                             @Cached("createBinaryProfile()") ConditionProfile elementHasMirrorProfile) {
        NativeDataAccess.NativeMirror elemMirror = wrapInNativeMirror(element, elementHasMirrorProfile);
        altStringVector.getDescriptor().invokeSetEltMethodCached(altStringVector, (int) index, elemMirror, setEltMethodInterop, instanceHasMirrorProfile);
        return null;
    }

    private static NativeDataAccess.NativeMirror wrapInNativeMirror(RBaseObject object, ConditionProfile hasMirrorProfile) {
        NativeDataAccess.NativeMirror mirror = object.getNativeMirror();
        if (hasMirrorProfile.profile(mirror != null)) {
            return mirror;
        } else {
            return NativeDataAccess.createNativeMirror(object);
        }
    }
}
