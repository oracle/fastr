package com.oracle.truffle.r.runtime.data.altrep;

public class AltStringClassDescriptor extends AltVecClassDescriptor {
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
}
