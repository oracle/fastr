package com.oracle.truffle.r.runtime.data.altrep;

public class AltLogicalClassDescriptor extends AltVecClassDescriptor {
    private Object eltMethod;
    private Object getRegionMethod;
    private Object isSortedMethod;
    private Object noNAMethod;
    private Object sumMethod;
    private Object maxMethod;
    private Object minMethod;

    public AltLogicalClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerEltMethod(Object eltMethod) {
        this.eltMethod = eltMethod;
    }

    public void registerGetRegionMethod(Object getRegionMethod) {
        this.getRegionMethod = getRegionMethod;
    }

    public void registerIsSortedMethod(Object isSortedMethod) {
        this.isSortedMethod = isSortedMethod;
    }

    public void registerNoNAMethod(Object noNAMethod) {
        this.noNAMethod = noNAMethod;
    }

    public void registerSumMethod(Object sumMethod) {
        this.sumMethod = sumMethod;
    }

    public void registerMaxMethod(Object maxMethod) {
        this.maxMethod = maxMethod;
    }

    public void registerMinMethod(Object minMethod) {
        this.minMethod = minMethod;
    }

    public boolean isEltMethodRegistered() {
        return eltMethod != null;
    }

    public boolean isGetRegionMethodRegistered() {
        return getRegionMethod != null;
    }

    public boolean isNoNAMethodRegistered() {
        return noNAMethod != null;
    }

    public boolean issumMethodMethodRegistered() {
        return sumMethod != null;
    }

    public boolean isMaxMethodRegistered() {
        return maxMethod != null;
    }

    public boolean isMinMethodRegistered() {
        return minMethod != null;
    }

    public boolean isIsSortedMethodRegistered() {
        return isSortedMethod != null;
    }
}
