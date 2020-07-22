package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.CompilerDirectives;

public class AltComplexClassDescriptor extends AltVecClassDescriptor {
    public static final String eltMethodSignature = "(pointer, sint32):object";
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] eltMethodWrapArguments = new boolean[]{true, false};
    public static final boolean eltMethodUnwrapResult = true;

    public static final String getRegionMethodSignature = "(pointer, sint32, sint32, [object]):sint32";
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] getRegionMethodWrapArguments = new boolean[]{true, false, false, false};
    public static final boolean getRegionMethodUnwrapResult = false;

    private AltrepMethodDescriptor eltMethodDescriptor;
    private AltrepMethodDescriptor getRegionMethodDescriptor;

    public AltComplexClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerEltMethod(AltrepMethodDescriptor eltMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.eltMethodDescriptor);
        this.eltMethodDescriptor = eltMethod;
    }

    public void registerGetRegionMethod(AltrepMethodDescriptor getRegionMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.getRegionMethodDescriptor);
        this.getRegionMethodDescriptor = getRegionMethod;
    }

    public boolean isGetRegionMethodRegistered() {
        return getRegionMethodDescriptor != null;
    }

    public boolean isEltMethodRegistered() {
        return eltMethodDescriptor != null;
    }

    public AltrepMethodDescriptor getEltMethodDescriptor() {
        return eltMethodDescriptor;
    }

    public AltrepMethodDescriptor getGetRegionMethodDescriptor() {
        return getRegionMethodDescriptor;
    }

    @Override
    public String toString() {
        return "ALTCOMPLEX class descriptor for " + super.toString();
    }
}
