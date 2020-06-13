package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.data.altrep.AltComplexClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltLogicalClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRawClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRealClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRepClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import org.graalvm.collections.EconomicMap;

public class AltRepContext implements RContext.ContextState {
    private static final TruffleLogger logger = RLogger.getLogger(RLogger.LOGGER_ALTREP);
    private AltRepClassDescriptor descriptor;
    EconomicMap<String, AltIntegerClassDescriptor> altIntDescriptors = EconomicMap.create();
    EconomicMap<String, AltRealClassDescriptor> altRealDescriptors = EconomicMap.create();

    private AltRepContext() {
    }

    public static AltRepContext newContextState() {
        return new AltRepContext();
    }

    public AltIntegerClassDescriptor registerNewAltIntClass(String className, String packageName, Object dllInfo) {
        AltIntegerClassDescriptor altIntClassDescr = new AltIntegerClassDescriptor(className, packageName, dllInfo);
        altIntDescriptors.put(altIntClassDescr.toString(), altIntClassDescr);
        logger.fine(() -> "Registered ALTINT class: " + altIntClassDescr.toString());
        return altIntClassDescr;
    }

    public AltRealClassDescriptor registerNewAltRealClass(String className, String packageName, Object dllInfo) {
        AltRealClassDescriptor altRealClassDescr = new AltRealClassDescriptor(className, packageName, dllInfo);
        altRealDescriptors.put(altRealClassDescr.toString(), altRealClassDescr);
        logger.fine(() -> "Registered ALTREAL class: " + altRealClassDescr.toString());
        return altRealClassDescr;
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

    /**
     * Saves the given descriptor for some duration
     * FIXME: This is an ugly hack and should be temporary solution.
     */
    public void saveDescriptor(AltRepClassDescriptor altRepClassDescr) {
        assert this.descriptor == null : "Only one descriptor can be saved at a time";
        this.descriptor = altRepClassDescr;
    }

    public AltRepClassDescriptor loadDescriptor() {
        AltRepClassDescriptor savedDescriptor = descriptor;
        descriptor = null;
        return savedDescriptor;
    }
}
