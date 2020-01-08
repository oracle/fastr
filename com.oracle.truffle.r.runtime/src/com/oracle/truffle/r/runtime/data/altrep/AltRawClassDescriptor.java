package com.oracle.truffle.r.runtime.data.altrep;

public class AltRawClassDescriptor extends AltVecClassDescriptor{
    private Object eltMethod;
    private Object getRegionMethod;

    public AltRawClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerEltMethod(Object eltMethod) {
        this.eltMethod = eltMethod;
    }

    public void registerGetRegionMethod(Object getRegionMethod) {
        this.getRegionMethod = getRegionMethod;
    }

    public boolean isGetRegionMethodRegistered() {
        return getRegionMethod != null;
    }

    public boolean isEltMethodRegistered() {
        return eltMethod != null;
    }
}
