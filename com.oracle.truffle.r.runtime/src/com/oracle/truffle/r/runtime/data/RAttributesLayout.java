/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public final class RAttributesLayout {

    public static class RAttributesType extends com.oracle.truffle.api.object.ObjectType {
    }

    private static final RAttributesType RATTR_TYPE = new RAttributesType();

    private static final Layout LAYOUT = Layout.newLayout().build();
    private static final Shape.Allocator RATTR_ALLOCATOR = LAYOUT.createAllocator();

    private static final HiddenKey COMMON_IDENTIFIER = new HiddenKey("commonProperties");
    private static final Property COMMON_PROPERTIES = Property.create(COMMON_IDENTIFIER, RATTR_ALLOCATOR.locationForType(Object.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)),
                    0);

    private static final DynamicObjectFactory RATTR_FACTORY = createAttrFactory();

    private RAttributesLayout() {
    }

    private static DynamicObjectFactory createAttrFactory() {
        return LAYOUT.createShape(RATTR_TYPE).addProperty(COMMON_PROPERTIES).createFactory();
    }

    public static DynamicObject createRAttributes() {
        return RATTR_FACTORY.newInstance(new CommonLocations());
    }

    public static DynamicObject createRAttributes(String[] names, Object[] values) {
        assert names != null && values != null && names.length == values.length;

        DynamicObject attrs = createRAttributes();
        for (int i = 0; i < names.length; i++) {
            attrs.define(names[i], values[i]);
        }
        return attrs;
    }

    public static DynamicObject createClass(Object cls) {
        DynamicObject attrs = createRAttributes();
        attrs.define(RRuntime.CLASS_ATTR_KEY, cls);
        return attrs;
    }

    public static DynamicObject createNames(Object names) {
        DynamicObject attrs = createRAttributes();
        attrs.define(RRuntime.NAMES_ATTR_KEY, names);
        return attrs;
    }

    public static DynamicObject createDim(Object dim) {
        DynamicObject attrs = createRAttributes();
        attrs.define(RRuntime.DIM_ATTR_KEY, dim);
        return attrs;
    }

    public static DynamicObject createDimNames(Object dimNames) {
        DynamicObject attrs = createRAttributes();
        attrs.define(RRuntime.DIMNAMES_ATTR_KEY, dimNames);
        return attrs;
    }

    public static DynamicObject createNamesAndDim(Object names, Object dim) {
        DynamicObject attrs = createRAttributes();
        attrs.define(RRuntime.NAMES_ATTR_KEY, names);
        attrs.define(RRuntime.DIM_ATTR_KEY, dim);
        return attrs;
    }

    public static DynamicObject createDimAndDimNames(Object dim, Object dimNames) {
        DynamicObject attrs = createRAttributes();
        attrs.define(RRuntime.DIM_ATTR_KEY, dim);
        attrs.define(RRuntime.DIMNAMES_ATTR_KEY, dimNames);
        return attrs;
    }

    public static boolean isRAttributes(Object attrs) {
        return (attrs instanceof DynamicObject) && isRAttributes((DynamicObject) attrs);
    }

    public static boolean isRAttributes(DynamicObject attrs) {
        return isRAttributes(attrs.getShape().getObjectType());
    }

    public static boolean isRAttributes(ObjectType objectType) {
        return objectType instanceof RAttributesType;
    }

    public static DynamicObject copy(DynamicObject attrs) {
        assert isRAttributes(attrs);

        return attrs.copy(attrs.getShape());
    }

    public static void clear(DynamicObject attrs) {
        assert isRAttributes(attrs);

        for (Property p : getPropertyList(attrs)) {
            attrs.delete(p.getKey());
        }
    }

    public static List<Property> getPropertyList(DynamicObject attrs, BranchProfile listProfile) {
        assert isRAttributes(attrs);

        CommonLocations comLoc = (CommonLocations) COMMON_PROPERTIES.get(attrs, attrs.getShape());
        return comLoc.getPropertyList(attrs, listProfile);
    }

    public static List<Property> getPropertyList(DynamicObject attrs) {
        assert isRAttributes(attrs);

        CommonLocations comLoc = (CommonLocations) COMMON_PROPERTIES.get(attrs, attrs.getShape());
        return comLoc.getPropertyList(attrs);
    }

    public static Iterable<RAttributesLayout.RAttribute> asIterable(DynamicObject attrs, BranchProfile listProfile) {
        return new RAttributeIterable(attrs, listProfile);
    }

    public static Iterable<RAttributesLayout.RAttribute> asIterable(DynamicObject attrs) {
        return new RAttributeIterableNoProfile(attrs);
    }

    static final class RAttributeIterable implements Iterable<RAttributesLayout.RAttribute> {
        private final DynamicObject attrs;
        private final BranchProfile listProfile;

        RAttributeIterable(DynamicObject attrs, BranchProfile listProfile) {
            this.attrs = attrs;
            this.listProfile = listProfile;
        }

        @Override
        public Iterator<RAttributesLayout.RAttribute> iterator() {
            List<Property> propertyList = getPropertyList(attrs, listProfile);
            return new RAttributesLayout.Iter(attrs, propertyList.iterator());
        }

    }

    static final class RAttributeIterableNoProfile implements Iterable<RAttributesLayout.RAttribute> {
        private final DynamicObject attrs;

        RAttributeIterableNoProfile(DynamicObject attrs) {
            this.attrs = attrs;
        }

        @Override
        public Iterator<RAttributesLayout.RAttribute> iterator() {
            List<Property> propertyList = getPropertyList(attrs);
            return new Iter(attrs, propertyList.iterator());
        }

    }

    public static Object getNames(DynamicObject attrs, BranchProfile attrProfile) {
        assert isRAttributes(attrs);

        CommonLocations comLoc = (CommonLocations) COMMON_PROPERTIES.get(attrs, attrs.getShape());
        return comLoc.namesLoc.get(attrs, attrProfile);
    }

    public static void setNames(DynamicObject attrs, Object value, BranchProfile attrProfile) {
        assert isRAttributes(attrs);

        CommonLocations comLoc = (CommonLocations) COMMON_PROPERTIES.get(attrs, attrs.getShape());
        comLoc.namesLoc.set(attrs, value, attrProfile);
    }

    public static Object getDim(DynamicObject attrs, BranchProfile attrProfile) {
        assert isRAttributes(attrs);

        CommonLocations comLoc = (CommonLocations) COMMON_PROPERTIES.get(attrs, attrs.getShape());
        return comLoc.dimLoc.get(attrs, attrProfile);
    }

    public static void setDim(DynamicObject attrs, Object value, BranchProfile attrProfile) {
        assert isRAttributes(attrs);

        CommonLocations comLoc = (CommonLocations) COMMON_PROPERTIES.get(attrs, attrs.getShape());
        comLoc.dimLoc.set(attrs, value, attrProfile);
    }

    public static Object getClass(DynamicObject attrs, BranchProfile attrProfile) {
        assert isRAttributes(attrs);

        CommonLocations comLoc = (CommonLocations) COMMON_PROPERTIES.get(attrs, attrs.getShape());
        return comLoc.classLoc.get(attrs, attrProfile);
    }

    public static void setClass(DynamicObject attrs, Object value, BranchProfile attrProfile) {
        assert isRAttributes(attrs);

        CommonLocations comLoc = (CommonLocations) COMMON_PROPERTIES.get(attrs, attrs.getShape());
        comLoc.classLoc.set(attrs, value, attrProfile);
    }

    private static final class CommonLocations {

        private final class CommonLocation {
            private final String name;
            private Shape shape;
            private Property prop;

            CommonLocation(String name) {
                this.name = name;
            }

            public Object get(DynamicObject attrs, BranchProfile attrProfile) {
                Shape curShape = attrs.getShape();
                if (prop == null || curShape != shape) {
                    attrProfile.enter();
                    shape = curShape;
                    prop = shape.getProperty(name);
                    if (prop == null) {
                        return null;
                    }
                }

                return prop.get(attrs, shape);
            }

            public void set(DynamicObject attrs, Object value, BranchProfile attrProfile) {
                Shape curShape = attrs.getShape();

                if (prop == null || curShape != shape) {
                    attrProfile.enter();
                    shape = curShape;
                    prop = shape.getProperty(name);
                    if (prop == null) {
                        attrs.define(name, value);
                        shape = attrs.getShape();
                        prop = shape.getProperty(name);
                        return;
                    }
                }

                try {
                    prop.set(attrs, value, shape);
                } catch (IncompatibleLocationException | FinalLocationException e) {
                    throw new UnsupportedOperationException(e);
                }

            }

        }

        private final CommonLocation namesLoc = new CommonLocation(RRuntime.NAMES_ATTR_KEY);
        private final CommonLocation dimLoc = new CommonLocation(RRuntime.DIM_ATTR_KEY);
        private final CommonLocation classLoc = new CommonLocation(RRuntime.CLASS_ATTR_KEY);

        private List<Property> properties;
        private Shape shapeForProperies;

        CommonLocations() {
        }

        List<Property> getPropertyList(DynamicObject attrs, BranchProfile listProfile) {
            if (properties == null || attrs.getShape() != shapeForProperies) {
                listProfile.enter();
                properties = attrs.getShape().getPropertyList();
                shapeForProperies = attrs.getShape();
            }
            return properties;
        }

        List<Property> getPropertyList(DynamicObject attrs) {
            if (properties == null || attrs.getShape() != shapeForProperies) {
                properties = attrs.getShape().getPropertyList();
                shapeForProperies = attrs.getShape();
            }
            return properties;
        }

    }

    public interface RAttribute {
        String getName();

        Object getValue();
    }

    @ValueType
    static class AttrInstance implements RAttribute {
        private final String name;
        private Object value;

        AttrInstance(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return name + "=" + value;
        }
    }

    static class Iter implements Iterator<RAttribute> {
        private final Iterator<Property> iter;
        private final DynamicObject attrs;
        private final Shape shape;

        Iter(DynamicObject attrs, Iterator<Property> propertyIterator) {
            this.attrs = attrs;
            this.iter = propertyIterator;
            this.shape = attrs.getShape();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public RAttribute next() {
            Property p = iter.next();
            Object value = p.get(attrs, shape);
            return new AttrInstance((String) p.getKey(), value);
        }

    }
}
