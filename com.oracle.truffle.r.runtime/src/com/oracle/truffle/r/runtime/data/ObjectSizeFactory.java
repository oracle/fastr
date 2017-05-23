/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data;

import java.util.ArrayList;

import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.data.RObjectSize.IgnoreObjectHandler;
import com.oracle.truffle.r.runtime.data.RObjectSize.TypeCustomizer;

public abstract class ObjectSizeFactory {

    private static ArrayList<TypeCustomizerData> typeCustomizers = new ArrayList<>(); // system wide

    static {
        String prop = System.getProperty("fastr.objectsize.factory.class");
        if (prop == null) {
            if (FastRConfig.ManagedMode) {
                prop = SimpleObjectSizeFactory.class.getName();
            } else {
                prop = AgentObjectSizeFactory.class.getName();
            }
        }
        try {
            theInstance = (ObjectSizeFactory) Class.forName(prop).newInstance();
        } catch (Exception ex) {
            // CheckStyle: stop system..print check
            System.err.println("Failed to instantiate class: " + prop);
        }
    }

    private static ObjectSizeFactory theInstance;

    public static ObjectSizeFactory getInstance() {
        return theInstance;
    }

    /**
     * See {@link RObjectSize#getObjectSize}.
     */
    public abstract long getObjectSize(Object obj, IgnoreObjectHandler ignoreObjectHandler);

    public void registerTypeCustomizer(Class<?> klass, TypeCustomizer typeCustomizer) {
        typeCustomizers.add(new TypeCustomizerData(klass, typeCustomizer));
    }

    protected static TypeCustomizer getCustomizer(Class<?> objClass) {
        for (TypeCustomizerData customizer : typeCustomizers) {
            if (customizer.type.isAssignableFrom(objClass)) {
                return customizer.customizer;
            }
        }
        return null;
    }

    private static final class TypeCustomizerData {
        private final TypeCustomizer customizer;
        private final Class<?> type;

        private TypeCustomizerData(Class<?> type, TypeCustomizer customizer) {
            this.customizer = customizer;
            this.type = type;
        }
    }
}
