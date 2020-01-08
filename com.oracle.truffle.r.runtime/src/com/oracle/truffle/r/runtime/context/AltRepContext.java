package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.data.altrep.AltComplexClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltLogicalClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRawClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRealClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;

public class AltRepContext implements RContext.ContextState {
    private static final TruffleLogger logger = RLogger.getLogger("altrep");

    private AltRepContext() {
    }

    public static AltRepContext newContextState() {
        return new AltRepContext();
    }

    public AltIntegerClassDescriptor registerNewAltIntClass(String className, String packageName, Object dllInfo) {
        AltIntegerClassDescriptor descriptor = new AltIntegerClassDescriptor(className, packageName, dllInfo);
        logger.fine(() -> "Registered ALTINT class: " + descriptor.toString());
        return descriptor;
    }

    public AltRealClassDescriptor registerNewAltRealClass(String className, String packageName, Object dllInfo) {
        AltRealClassDescriptor descriptor = new AltRealClassDescriptor(className, packageName, dllInfo);
        logger.fine(() -> "Registered ALTREAL class: " + descriptor.toString());
        return descriptor;
    }

    public AltComplexClassDescriptor registerNewAltComplexClass(String className, String packageName, Object dllInfo) {
        return new AltComplexClassDescriptor(className, packageName, dllInfo);
    }

    public AltLogicalClassDescriptor registerNewAltLogicalClass(String className, String packageName, Object dllInfo) {
        return new AltLogicalClassDescriptor(className, packageName, dllInfo);
    }

    public AltStringClassDescriptor registerNewAltStringClass(String className, String packageName, Object dllInfo) {
        return new AltStringClassDescriptor(className, packageName, dllInfo);
    }

    public AltRawClassDescriptor registerNewAltRawClass(String className, String packageName, Object dllInfo) {
        return new AltRawClassDescriptor(className, packageName, dllInfo);
    }
}
