package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;

public class AltStringClassDescriptor extends AltVecClassDescriptor {
    // TODO: Fix signature
    public static final int eltMethodArgCount = 2;
    public static final int setEltMethodArgCount = 3;
    public static final String eltMethodSignature = "(pointer, sint32) : string";
    public static final String setEltMethodSignature = "(pointer, sint32, pointer) : void";
    public static final String isSortedMethodSignature = "(pointer) : sint32";
    public static final String noNAMethodSignature = "(pointer) : sint32";

    private AltrepDownCall eltDownCall;
    private AltrepDownCall setEltDownCall;
    private AltrepDownCall isSortedDownCall;
    private AltrepDownCall noNADownCall;

    public AltStringClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerEltMethod(AltrepDownCall eltMethod) {
        this.eltDownCall = eltMethod;
    }

    public void registerSetEltMethod(AltrepDownCall setEltMethod) {
        this.setEltDownCall = setEltMethod;
    }

    public void registerIsSortedMethod(AltrepDownCall isSortedMethod) {
        this.isSortedDownCall = isSortedMethod;
    }

    public void registerNoNAMethod(AltrepDownCall noNAMethod) {
        this.noNADownCall = noNAMethod;
    }

    public boolean isEltMethodRegistered() {
        return eltDownCall != null;
    }

    public boolean isSetEltMethodRegistered() {
        return setEltDownCall != null;
    }

    public boolean isNoNAMethodRegistered() {
        return noNADownCall != null;
    }

    public boolean isIsSortedMethodRegistered() {
        return isSortedDownCall != null;
    }

    public AltrepDownCall getEltDownCall() {
        return eltDownCall;
    }

    public AltrepDownCall getSetEltDownCall() {
        return setEltDownCall;
    }

    public AltrepDownCall getIsSortedDownCall() {
        return isSortedDownCall;
    }

    public AltrepDownCall getNoNADownCall() {
        return noNADownCall;
    }

    public Object invokeEltMethodUncached(Object instance, int index) {
        InteropLibrary methodInterop = InteropLibrary.getFactory().getUncached(eltDownCall.method);
        ConditionProfile hasMirrorProfile = ConditionProfile.getUncached();
        return invokeEltMethod(instance, index, methodInterop, hasMirrorProfile);
    }

    public Object invokeEltMethodCached(Object instance, int index, InteropLibrary eltMethodInterop, ConditionProfile hasMirrorProfile) {
        return invokeEltMethod(instance, index, eltMethodInterop, hasMirrorProfile);
    }

    public void invokeSetEltMethodCached(Object instance, int index, Object element, InteropLibrary setEltMethodInterop, ConditionProfile hasMirrorProfile) {
        invokeSetEltMethod(instance, index, element, setEltMethodInterop, hasMirrorProfile);
    }

    public void invokeSetEltMethodUncached(Object instance, int index, Object element) {
        InteropLibrary methodInterop = InteropLibrary.getFactory().getUncached(setEltDownCall.method);
        ConditionProfile hasMirrorProfile = ConditionProfile.getUncached();
        invokeSetEltMethod(instance, index, element, methodInterop, hasMirrorProfile);
    }

    private Object invokeEltMethod(Object instance, int index, InteropLibrary eltMethodInterop, ConditionProfile hasMirrorProfile) {
        Object elem = invokeNativeFunction(eltMethodInterop, eltDownCall.method, eltMethodSignature, eltMethodArgCount, hasMirrorProfile, instance, index);

        // TODO: This is an ugly hack for nested upcalls.
        // In case that invokeNativeFunction calls into another upcall so that elem is not wrapped in NativeMirror twice.
        if (elem instanceof NativeDataAccess.NativeMirror) {
            elem = ((NativeDataAccess.NativeMirror) elem).getDelegate();
        }
        assert elem instanceof CharSXPWrapper;
        return elem;
    }

    private void invokeSetEltMethod(Object instance, int index, Object element, InteropLibrary setEltMethodInterop, ConditionProfile hasMirrorProfile) {
        invokeNativeFunction(setEltMethodInterop, setEltDownCall.method, setEltMethodSignature, setEltMethodArgCount, hasMirrorProfile, instance, index, element);
    }
}
