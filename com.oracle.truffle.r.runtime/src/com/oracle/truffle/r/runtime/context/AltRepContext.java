/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
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

/**
 * A context that stores all the class descriptors for ALTREP. TODO: Currently this class is not
 * useful. Use it for serialization.
 */
public final class AltRepContext implements RContext.ContextState {
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
     * Saves the given descriptor for some duration.
     *
     * FIXME: This is an ugly hack and should be a temporary solution.
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
