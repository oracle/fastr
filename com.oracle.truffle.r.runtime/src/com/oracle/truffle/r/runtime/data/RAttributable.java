/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import java.util.Iterator;

/**
 * Denotes an R type that can have associated attributes, e.g. {@link REnvironment}
 *
 * An attribute is a {@code String, Object} pair. The set of attributes associated with an
 * {@link RAttributable} is implemented by the {@link DynamicObject} class.
 */
public abstract class RAttributable extends RBaseObject {

    /**
     * This hidden property is used to store a pair-list that presents these attributes to the
     * native code.
     */
    public static final HiddenKey ATTRS_KEY = new HiddenKey("attrs");

    protected DynamicObject attributes;

    /**
     * Access all the attributes. Use {@code for (RAttribute a : getAttributes) ... }. Returns
     * {@code null} if not initialized.
     */
    public final DynamicObject getAttributes() {
        return attributes;
    }

    /**
     * If the attribute set is not initialized, then initialize it.
     *
     * @return the pre-existing or new value
     */
    public final DynamicObject initAttributes() {
        if (attributes == null) {
            attributes = RAttributesLayout.createRAttributes();
        }
        return attributes;
    }

    public final void initAttributes(DynamicObject newAttributes) {
        this.attributes = newAttributes;
    }

    /**
     * Get the value of an attribute. Returns {@code null} if not set.
     */
    public final Object getAttr(String name) {
        CompilerAsserts.neverPartOfCompilation();
        DynamicObject attr = getAttributes();
        return attr == null ? null : attr.get(name);
    }

    /**
     * Set the attribute {@code name} to {@code value}, overwriting any existing value. This is
     * generic; a class may need to override this to handle certain attributes specially.
     */
    public void setAttr(String name, Object value) {
        CompilerAsserts.neverPartOfCompilation();
        putAttribute(name, value);
    }

    /**
     * Guarded method that checks whether {@code attributes} is initialized. Simply sets the
     * attribute, can't be overridden.
     */
    protected final void putAttribute(String name, Object value) {
        initAttributes().define(name, value);
    }

    public void removeAttr(String name) {
        CompilerAsserts.neverPartOfCompilation();
        DynamicObject attrs = getAttributes();
        if (attrs != null) {
            attrs.delete(name);
            if (attrs.getShape().getPropertyCount() == 0) {
                initAttributes(null);
            }
        }
    }

    public final void removeAllAttributes() {
        CompilerAsserts.neverPartOfCompilation();
        DynamicObject attrs = getAttributes();
        if (attrs != null) {
            RAttributesLayout.clear(attrs);
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
    public final void resetAllAttributes(boolean nullify) {
        if (nullify) {
            this.attributes = null;
        } else {
            if (this.attributes != null) {
                RAttributesLayout.clear(this.attributes);
            }
        }
    }

    public RAttributable setClassAttr(RStringVector classAttr) {
        CompilerAsserts.neverPartOfCompilation();
        return setClassAttrInternal(this, classAttr);
    }

    protected static final RAttributable setClassAttrInternal(RAttributable attributable, RStringVector classAttr) {
        if (attributable.attributes == null && classAttr != null && classAttr.getLength() != 0) {
            attributable.initAttributes();
        }
        if (attributable.attributes != null && (classAttr == null || classAttr.getLength() == 0)) {
            attributable.removeAttributeMapping(RRuntime.CLASS_ATTR_KEY);
        } else if (classAttr != null && classAttr.getLength() != 0) {
            if (attributable instanceof RAbstractVector && !(attributable instanceof RIntVector)) {
                for (int i = 0; i < classAttr.getLength(); i++) {
                    String attr = classAttr.getDataAt(i);
                    if (RRuntime.CLASS_FACTOR.equals(attr)) {
                        throw RError.error(RError.SHOW_CALLER2, RError.Message.ADDING_INVALID_CLASS, "factor");
                    }
                }
            }
            attributable.initAttributes().define(RRuntime.CLASS_ATTR_KEY, classAttr);
        }
        return attributable;
    }

    protected final void removeAttributeMapping(String key) {
        if (this.attributes != null) {
            this.attributes.delete(key);
            if (this.attributes.getShape().getPropertyCount() == 0) {
                this.attributes = null;
            }
        }
    }

    public final RStringVector getClassAttr() {
        return (RStringVector) getAttr(RRuntime.CLASS_ATTR_KEY);
    }

    /**
     * Returns {@code true} if and only if the value has a {@code class} attribute added explicitly.
     */
    public final boolean isObject() {
        return getClassAttr() != null;
    }

    public static void copyAttributes(RAttributable obj, DynamicObject attrs) {
        if (attrs == null) {
            return;
        }
        Iterator<RAttributesLayout.RAttribute> iter = RAttributesLayout.asIterable(attrs).iterator();
        while (iter.hasNext()) {
            RAttributesLayout.RAttribute attr = iter.next();
            obj.putAttribute(attr.getName(), attr.getValue());
        }
    }
}
