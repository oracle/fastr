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
    public static final boolean[] eltMethodWrapArguments = new boolean[]{true, false};
    public static final boolean eltMethodUnwrapResult = true;

    public static final String setEltMethodSignature = "(pointer, sint32, pointer) : void";
    public static final boolean[] setEltMethodWrapArguments = new boolean[]{true, false, true};
    public static final boolean setEltMethodUnwrapResult = false;

    public static final String isSortedMethodSignature = "(pointer) : sint32";
    public static final boolean[] isSortedMethodWrapArguments = new boolean[]{true};
    public static final boolean isSortedMethodUnwrapResult = false;

    public static final String noNAMethodSignature = "(pointer) : sint32";
    public static final boolean[] noNAMethodWrapArguments = new boolean[]{true};
    public static final boolean noNAMethodUnwrapResult = false;

    private AltrepMethodDescriptor eltMethodDescriptor;
    private AltrepMethodDescriptor setEltMethodDescriptor;
    private AltrepMethodDescriptor isSortedMethodDescriptor;
    private AltrepMethodDescriptor noNAMethodDescriptor;

    public AltStringClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerEltMethod(AltrepMethodDescriptor eltMethod) {
        this.eltMethodDescriptor = eltMethod;
    }

    public void registerSetEltMethod(AltrepMethodDescriptor setEltMethod) {
        this.setEltMethodDescriptor = setEltMethod;
    }

    public void registerIsSortedMethod(AltrepMethodDescriptor isSortedMethod) {
        this.isSortedMethodDescriptor = isSortedMethod;
    }

    public void registerNoNAMethod(AltrepMethodDescriptor noNAMethod) {
        this.noNAMethodDescriptor = noNAMethod;
    }

    public boolean isEltMethodRegistered() {
        return eltMethodDescriptor != null;
    }

    public boolean isSetEltMethodRegistered() {
        return setEltMethodDescriptor != null;
    }

    public boolean isNoNAMethodRegistered() {
        return noNAMethodDescriptor != null;
    }

    public boolean isIsSortedMethodRegistered() {
        return isSortedMethodDescriptor != null;
    }

    public AltrepMethodDescriptor getEltMethodDescriptor() {
        return eltMethodDescriptor;
    }

    public AltrepMethodDescriptor getSetEltMethodDescriptor() {
        return setEltMethodDescriptor;
    }

    public AltrepMethodDescriptor getIsSortedMethodDescriptor() {
        return isSortedMethodDescriptor;
    }

    public AltrepMethodDescriptor getNoNAMethodDescriptor() {
        return noNAMethodDescriptor;
    }

    public Object invokeEltMethodUncached(Object instance, int index) {
        InteropLibrary methodInterop = InteropLibrary.getFactory().getUncached(eltMethodDescriptor.method);
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
        InteropLibrary methodInterop = InteropLibrary.getFactory().getUncached(setEltMethodDescriptor.method);
        ConditionProfile hasMirrorProfile = ConditionProfile.getUncached();
        invokeSetEltMethod(instance, index, element, methodInterop, hasMirrorProfile);
    }

    private Object invokeEltMethod(Object instance, int index, InteropLibrary eltMethodInterop, ConditionProfile hasMirrorProfile) {
        Object elem = invokeNativeFunction(eltMethodInterop, eltMethodDescriptor.method, eltMethodSignature, eltMethodArgCount, hasMirrorProfile, instance, index);

        // TODO: This is an ugly hack for nested upcalls.
        // In case that invokeNativeFunction calls into another upcall so that elem is not wrapped in NativeMirror twice.
        if (elem instanceof NativeDataAccess.NativeMirror) {
            elem = ((NativeDataAccess.NativeMirror) elem).getDelegate();
        }
        assert elem instanceof CharSXPWrapper;
        return elem;
    }

    private void invokeSetEltMethod(Object instance, int index, Object element, InteropLibrary setEltMethodInterop, ConditionProfile hasMirrorProfile) {
        invokeNativeFunction(setEltMethodInterop, setEltMethodDescriptor.method, setEltMethodSignature, setEltMethodArgCount, hasMirrorProfile, instance, index, element);
    }
}
