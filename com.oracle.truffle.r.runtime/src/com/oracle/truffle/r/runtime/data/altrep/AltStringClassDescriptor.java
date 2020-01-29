package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;

public class AltStringClassDescriptor extends AltVecClassDescriptor {
    // TODO: Fix signature
    private static final String eltMethodSignature = "(pointer, sint32):string";
    private static final int eltMethodArgCount = 2;
    private static final String setEltMethodSignature = "(pointer, sint32, pointer):void";
    private static final int setEltMethodArgCount = 3;
    private Object eltMethod;

    private Object setEltMethod;
    private Object isSortedMethod;
    private Object noNAMethod;

    public AltStringClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerEltMethod(Object eltMethod) {
        this.eltMethod = eltMethod;
    }

    public void registerSetEltMethod(Object setEltMethod) {
        this.setEltMethod = setEltMethod;
    }

    public void registerIsSortedMethod(Object isSortedMethod) {
        this.isSortedMethod = isSortedMethod;
    }

    public void registerNoNAMethod(Object noNAMethod) {
        this.noNAMethod = noNAMethod;
    }

    public boolean isEltMethodRegistered() {
        return eltMethod != null;
    }

    public boolean isSetEltMethodRegistered() {
        return setEltMethod != null;
    }

    public boolean isNoNAMethodRegistered() {
        return noNAMethod != null;
    }

    public boolean isIsSortedMethodRegistered() {
        return isSortedMethod != null;
    }

    public Object getEltMethod() {
        return eltMethod;
    }

    public Object getSetEltMethod() {
        return setEltMethod;
    }

    public Object getIsSortedMethod() {
        return isSortedMethod;
    }

    public Object getNoNAMethod() {
        return noNAMethod;
    }

    public Object invokeEltMethodUncached(Object instance, int index) {
        InteropLibrary methodInterop = InteropLibrary.getFactory().getUncached(eltMethod);
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
        InteropLibrary methodInterop = InteropLibrary.getFactory().getUncached(setEltMethod);
        ConditionProfile hasMirrorProfile = ConditionProfile.getUncached();
        invokeSetEltMethod(instance, index, element, methodInterop, hasMirrorProfile);
    }

    private Object invokeEltMethod(Object instance, int index, InteropLibrary eltMethodInterop, ConditionProfile hasMirrorProfile) {
        Object elem = invokeNativeFunction(eltMethodInterop, eltMethod, eltMethodSignature, eltMethodArgCount, hasMirrorProfile, instance, index);

        // TODO: This is an ugly hack for nested upcalls.
        // In case that invokeNativeFunction calls into another upcall so that elem is not wrapped in NativeMirror twice.
        if (elem instanceof NativeDataAccess.NativeMirror) {
            elem = ((NativeDataAccess.NativeMirror) elem).getDelegate();
        }
        assert elem instanceof CharSXPWrapper;
        return elem;
    }

    private void invokeSetEltMethod(Object instance, int index, Object element, InteropLibrary setEltMethodInterop, ConditionProfile hasMirrorProfile) {
        invokeNativeFunction(setEltMethodInterop, setEltMethod, setEltMethodSignature, setEltMethodArgCount, hasMirrorProfile, instance, index, element);
    }
}
