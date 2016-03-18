/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Denotes an R type that can have associated attributes, e.g. {@link RVector}, {@link REnvironment}
 *
 * An attribute is a {@code String, Object} pair. The set of attributes associated with an
 * {@link RAttributable} is implemented by the {@link RAttributes} class.
 */
public interface RAttributable extends RTypedValue {
    /**
     * If the attribute set is not initialized, then initialize it.
     *
     * @return the pre-existing or new value
     */
    RAttributes initAttributes();

    void initAttributes(RAttributes newAttributes);

    /**
     * Access all the attributes. Use {@code for (RAttribute a : getAttributes) ... }. Returns
     * {@code null} if not initialized.
     */
    RAttributes getAttributes();

    /**
     * Returns the value of the {@code class} attribute.
     */
    RStringVector getClassHierarchy();

    RStringVector getImplicitClass();

    /**
     * Get the value of an attribute. Returns {@code null} if not set.
     */
    default Object getAttr(RAttributeProfiles profiles, String name) {
        RAttributes attributes = getAttributes();
        if (profiles.attrNullProfile(attributes == null)) {
            return null;
        } else {
            return attributes.get(name);
        }
    }

    /**
     * Set the attribute {@code name} to {@code value}, overwriting any existing value. This is
     * generic; a class may need to override this to handle certain attributes specially.
     */
    default void setAttr(String name, Object value) {
        RAttributes attributes = getAttributes();
        if (attributes == null) {
            attributes = initAttributes();
        }
        attributes.put(name, value);
    }

    /**
     * Remove the attribute {@code name}. No error if {@code name} is not an attribute. This is
     * generic; a class may need to override this to handle certain attributes specially.
     */
    default void removeAttr(RAttributeProfiles profiles, String name) {
        RAttributes attributes = getAttributes();
        if (profiles.attrNullProfile(attributes == null)) {
            return;
        } else {
            attributes.remove(name);
        }
    }

    default void removeAllAttributes() {
        RAttributes attributes = getAttributes();
        if (attributes != null) {
            attributes.clear();
        }
    }

    default RAttributes resetAllAttributes(@SuppressWarnings("unused") boolean nullify) {
        RAttributes attributes = getAttributes();
        attributes.clear();
        return attributes;
    }

    default RAttributable setClassAttr(RStringVector classAttr, @SuppressWarnings("unused") boolean convertToInt) {
        if (classAttr == null && getAttributes() != null) {
            getAttributes().remove(RRuntime.CLASS_ATTR_KEY);
        } else {
            setAttr(RRuntime.CLASS_ATTR_KEY, classAttr);
        }
        return this;
    }

    default RStringVector getClassAttr(RAttributeProfiles profiles) {
        return (RStringVector) getAttr(profiles, RRuntime.CLASS_ATTR_KEY);
    }

    default boolean isObject(RAttributeProfiles profiles) {
        return getClassAttr(profiles) != null ? true : false;
    }
}
