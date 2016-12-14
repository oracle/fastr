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

import java.util.Iterator;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Denotes an R type that can have associated attributes, e.g. {@link RVector}, {@link REnvironment}
 *
 * An attribute is a {@code String, Object} pair. The set of attributes associated with an
 * {@link RAttributable} is implemented by the {@link DynamicObject} class.
 */
public interface RAttributable extends RTypedValue {

    /**
     * If the attribute set is not initialized, then initialize it.
     *
     * @return the pre-existing or new value
     */
    DynamicObject initAttributes();

    void initAttributes(DynamicObject newAttributes);

    /**
     * Access all the attributes. Use {@code for (RAttribute a : getAttributes) ... }. Returns
     * {@code null} if not initialized.
     */
    DynamicObject getAttributes();

    /**
     * Returns the value of the {@code class} attribute or empty {@link RStringVector} if class
     * attribute is not set.
     */
    default RStringVector getClassHierarchy() {
        Object v = getAttr(RRuntime.CLASS_ATTR_KEY);
        RStringVector result = v instanceof RStringVector ? (RStringVector) v : getImplicitClass();
        return result != null ? result : RDataFactory.createEmptyStringVector();
    }

    /**
     * Returns {@code true} if the {@code class} attribute is set to {@link RStringVector} whose
     * first element equals to the given className.
     */
    default boolean hasClass(String className) {
        RStringVector v = getClassHierarchy();
        for (int i = 0; i < v.getLength(); ++i) {
            if (v.getDataAt(i).equals(className)) {
                return true;
            }
        }
        return false;
    }

    RStringVector getImplicitClass();

    /**
     * Get the value of an attribute. Returns {@code null} if not set.
     */
    default Object getAttr(RAttributeProfiles profiles, String name) {
        DynamicObject attributes = getAttributes();
        if (profiles.attrNullProfile(attributes == null)) {
            return null;
        } else {
            return attributes.get(name);
        }
    }

    /**
     * Get the value of an attribute. Returns {@code null} if not set.
     */
    default Object getAttr(String name) {
        DynamicObject attr = getAttributes();
        return attr == null ? null : attr.get(name);
    }

    /**
     * Set the attribute {@code name} to {@code value}, overwriting any existing value. This is
     * generic; a class may need to override this to handle certain attributes specially.
     */
    default void setAttr(String name, Object value) {
        DynamicObject attributes = getAttributes();
        if (attributes == null) {
            attributes = initAttributes();
        }
        attributes.define(name, value);
    }

    /**
     * Remove the attribute {@code name}. No error if {@code name} is not an attribute. This is
     * generic; a class may need to override this to handle certain attributes specially.
     */
    default void removeAttr(RAttributeProfiles profiles, String name) {
        DynamicObject attributes = getAttributes();
        if (profiles.attrNullProfile(attributes == null)) {
            return;
        } else {
            attributes.delete(name);
        }
    }

    default void removeAttr(String name) {
        DynamicObject attributes = getAttributes();
        if (attributes != null) {
            attributes.delete(name);
        }
    }

    default void removeAllAttributes() {
        DynamicObject attributes = getAttributes();
        if (attributes != null) {
            RAttributesLayout.clear(attributes);
        }
    }

    /**
     * Removes all attributes. If the attributes instance was not initialized, it will stay
     * uninitialized (i.e. {@code null}). If the attributes instance was initialized, it will stay
     * initialized and will be just cleared, unless nullify is {@code true}.
     *
     * @param nullify Some implementations can force nullifying attributes instance if this flag is
     *            set to {@code true}. Nullifying is not guaranteed for all implementations.
     */
    default void resetAllAttributes(boolean nullify) {
        DynamicObject attributes = getAttributes();
        if (attributes != null) {
            RAttributesLayout.clear(attributes);
        }
    }

    default RAttributable setClassAttr(RStringVector classAttr) {
        if (classAttr == null && getAttributes() != null) {
            getAttributes().delete(RRuntime.CLASS_ATTR_KEY);
        } else {
            setAttr(RRuntime.CLASS_ATTR_KEY, classAttr);
        }
        return this;
    }

    default RStringVector getClassAttr(RAttributeProfiles profiles) {
        return (RStringVector) getAttr(profiles, RRuntime.CLASS_ATTR_KEY);
    }

    default RStringVector getClassAttr() {
        return (RStringVector) getAttr(RRuntime.CLASS_ATTR_KEY);
    }

    /**
     * Returns {@code true} if and only if the value has a {@code class} attribute added explicitly.
     * When {@code true}, it is possible to call {@link RAttributable#getClassHierarchy()}.
     */
    default boolean isObject(RAttributeProfiles profiles) {
        return getClassAttr(profiles) != null ? true : false;
    }

    default boolean isObject() {
        return getClassAttr() != null ? true : false;
    }

    static void copyAttributes(RAttributable obj, DynamicObject attrs) {
        if (attrs == null) {
            return;
        }
        Iterator<RAttributesLayout.RAttribute> iter = RAttributesLayout.asIterable(attrs).iterator();
        while (iter.hasNext()) {
            RAttributesLayout.RAttribute attr = iter.next();
            obj.setAttr(attr.getName(), attr.getValue());
        }
    }
}
