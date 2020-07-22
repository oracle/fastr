package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.CompilerDirectives;

public class AltLogicalClassDescriptor extends AltVecClassDescriptor {
    // All the signatures and wrap/unwrap argument information is same as for
    // AltIntegerClassDescriptor.
    // We still want to have these fields here for consistency.

    public static final String eltMethodSignature = AltIntegerClassDescriptor.eltMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] eltMethodWrapArguments = AltIntegerClassDescriptor.eltMethodWrapArguments;
    public static final boolean eltMethodUnwrapResult = AltIntegerClassDescriptor.eltMethodUnwrapResult;

    public static final String getRegionMethodSignature = AltIntegerClassDescriptor.getRegionMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] getRegionMethodWrapArguments = AltIntegerClassDescriptor.getRegionMethodWrapArguments;
    public static final boolean getRegionMethodUnwrapResult = AltIntegerClassDescriptor.getRegionMethodUnwrapResult;

    public static final String isSortedMethodSignature = AltIntegerClassDescriptor.isSortedMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] isSortedMethodWrapArguments = AltIntegerClassDescriptor.isSortedMethodWrapArguments;
    public static final boolean isSortedMethodUnwrapResult = AltIntegerClassDescriptor.isSortedMethodUnwrapResult;

    public static final String noNAMethodSignature = AltIntegerClassDescriptor.noNAMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] noNAMethodWrapArguments = AltIntegerClassDescriptor.noNAMethodWrapArguments;
    public static final boolean noNAMethodUnwrapResult = AltIntegerClassDescriptor.noNAMethodUnwrapResult;

    public static final String sumMethodSignature = AltIntegerClassDescriptor.sumMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] sumMethodWrapArguments = AltIntegerClassDescriptor.sumMethodWrapArguments;
    public static final boolean sumMethodUnwrapResult = AltIntegerClassDescriptor.sumMethodUnwrapResult;

    private AltrepMethodDescriptor eltMethodDescriptor;
    private AltrepMethodDescriptor getRegionMethodDescriptor;
    private AltrepMethodDescriptor isSortedMethodDescriptor;
    private AltrepMethodDescriptor noNAMethodDescriptor;
    private AltrepMethodDescriptor sumMethodDescriptor;

    public AltLogicalClassDescriptor(String className, String packageName, Object dllInfo) {
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

    public void registerIsSortedMethod(AltrepMethodDescriptor isSortedMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.isSortedMethodDescriptor);
        this.isSortedMethodDescriptor = isSortedMethod;
    }

    public void registerNoNAMethod(AltrepMethodDescriptor noNAMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.noNAMethodDescriptor);
        this.noNAMethodDescriptor = noNAMethod;
    }

    public void registerSumMethod(AltrepMethodDescriptor sumMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.sumMethodDescriptor);
        this.sumMethodDescriptor = sumMethod;
    }

    public boolean isEltMethodRegistered() {
        return eltMethodDescriptor != null;
    }

    public boolean isGetRegionMethodRegistered() {
        return getRegionMethodDescriptor != null;
    }

    public boolean isNoNAMethodRegistered() {
        return noNAMethodDescriptor != null;
    }

    public boolean isSumMethodMethodRegistered() {
        return sumMethodDescriptor != null;
    }

    public boolean isIsSortedMethodRegistered() {
        return isSortedMethodDescriptor != null;
    }

    public AltrepMethodDescriptor getEltMethodDescriptor() {
        return eltMethodDescriptor;
    }

    public AltrepMethodDescriptor getGetRegionMethodDescriptor() {
        return getRegionMethodDescriptor;
    }

    public AltrepMethodDescriptor getIsSortedMethodDescriptor() {
        return isSortedMethodDescriptor;
    }

    public AltrepMethodDescriptor getNoNAMethodDescriptor() {
        return noNAMethodDescriptor;
    }

    public AltrepMethodDescriptor getSumMethodDescriptor() {
        return sumMethodDescriptor;
    }

    @Override
    public String toString() {
        return "ALTLOGICAL class descriptor for " + super.toString();
    }
}
