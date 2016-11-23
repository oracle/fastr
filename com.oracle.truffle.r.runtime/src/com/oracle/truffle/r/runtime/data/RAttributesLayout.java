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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RRuntime;

public final class RAttributesLayout {

    public static class RAttributesType extends ObjectType {
    }

    private static final AttrsLayout EMPTY_ATTRS_LAYOUT = new AttrsLayout();
    private static final AttrsLayout CLASS_ATTRS_LAYOUT = new AttrsLayout(RRuntime.CLASS_ATTR_KEY);
    private static final AttrsLayout NAMES_ATTRS_LAYOUT = new AttrsLayout(RRuntime.NAMES_ATTR_KEY);
    private static final AttrsLayout DIM_ATTRS_LAYOUT = new AttrsLayout(RRuntime.DIM_ATTR_KEY);
    private static final AttrsLayout DIMNAMES_ATTRS_LAYOUT = new AttrsLayout(RRuntime.DIMNAMES_ATTR_KEY);
    private static final AttrsLayout NAMES_AND_DIM_ATTRS_LAYOUT = new AttrsLayout(RRuntime.NAMES_ATTR_KEY, RRuntime.DIM_ATTR_KEY);
    private static final AttrsLayout DIM_AND_DIMNAMES_ATTRS_LAYOUT = new AttrsLayout(RRuntime.DIM_ATTR_KEY, RRuntime.DIMNAMES_ATTR_KEY);

    public static final AttrsLayout[] LAYOUTS = {EMPTY_ATTRS_LAYOUT, CLASS_ATTRS_LAYOUT, NAMES_ATTRS_LAYOUT, DIM_ATTRS_LAYOUT, DIMNAMES_ATTRS_LAYOUT, NAMES_AND_DIM_ATTRS_LAYOUT,
                    DIM_AND_DIMNAMES_ATTRS_LAYOUT};

    private static final Map<String, ConstantShapesAndLocations> constantShapesAndLocationsForAttribute = new HashMap<>();

    static {
        constantShapesAndLocationsForAttribute.put(RRuntime.CLASS_ATTR_KEY, new ConstantShapesAndLocations(
                        new Shape[]{
                                        CLASS_ATTRS_LAYOUT.shape
                        },
                        new Location[]{
                                        CLASS_ATTRS_LAYOUT.properties[0].getLocation()
                        }));
        constantShapesAndLocationsForAttribute.put(RRuntime.NAMES_ATTR_KEY, new ConstantShapesAndLocations(
                        new Shape[]{
                                        NAMES_ATTRS_LAYOUT.shape,
                                        NAMES_AND_DIM_ATTRS_LAYOUT.shape
                        },
                        new Location[]{
                                        NAMES_ATTRS_LAYOUT.properties[0].getLocation(),
                                        NAMES_AND_DIM_ATTRS_LAYOUT.properties[0].getLocation()
                        }));
        constantShapesAndLocationsForAttribute.put(RRuntime.DIM_ATTR_KEY, new ConstantShapesAndLocations(
                        new Shape[]{
                                        DIM_ATTRS_LAYOUT.shape,
                                        NAMES_AND_DIM_ATTRS_LAYOUT.shape,
                                        DIM_AND_DIMNAMES_ATTRS_LAYOUT.shape
                        },
                        new Location[]{
                                        DIM_ATTRS_LAYOUT.properties[0].getLocation(),
                                        NAMES_AND_DIM_ATTRS_LAYOUT.properties[1].getLocation(),
                                        DIM_AND_DIMNAMES_ATTRS_LAYOUT.properties[0].getLocation()
                        }));
        constantShapesAndLocationsForAttribute.put(RRuntime.DIMNAMES_ATTR_KEY, new ConstantShapesAndLocations(
                        new Shape[]{
                                        DIMNAMES_ATTRS_LAYOUT.shape,
                                        DIM_AND_DIMNAMES_ATTRS_LAYOUT.shape
                        },
                        new Location[]{
                                        DIMNAMES_ATTRS_LAYOUT.properties[0].getLocation(),
                                        DIM_AND_DIMNAMES_ATTRS_LAYOUT.properties[1].getLocation()
                        }));

    }

    private RAttributesLayout() {
    }

    public static DynamicObject createRAttributes() {
        return EMPTY_ATTRS_LAYOUT.factory.newInstance();
    }

    public static DynamicObject createRAttributes(String[] names, Object[] values) {
        assert names != null && values != null && names.length == values.length;

        AttrsLayout attrsLayout = new AttrsLayout(names);
        return attrsLayout.factory.newInstance(values);
    }

    public static DynamicObject createClass(Object cls) {
        return CLASS_ATTRS_LAYOUT.factory.newInstance(cls);
    }

    public static DynamicObject createNames(Object names) {
        return NAMES_ATTRS_LAYOUT.factory.newInstance(names);
    }

    public static DynamicObject createDim(Object dim) {
        return DIM_ATTRS_LAYOUT.factory.newInstance(dim);
    }

    public static DynamicObject createDimNames(Object dimNames) {
        return DIMNAMES_ATTRS_LAYOUT.factory.newInstance(dimNames);
    }

    public static DynamicObject createNamesAndDim(Object names, Object dim) {
        return NAMES_AND_DIM_ATTRS_LAYOUT.factory.newInstance(names, dim);
    }

    public static DynamicObject createDimAndDimNames(Object dim, Object dimNames) {
        return DIM_AND_DIMNAMES_ATTRS_LAYOUT.factory.newInstance(dim, dimNames);
    }

    public static ConstantShapesAndLocations getConstantShapesAndLocations(String attrName) {
        return constantShapesAndLocationsForAttribute.getOrDefault(attrName, ConstantShapesAndLocations.EMPTY);
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

        // todo
        return attrs.getShape().getPropertyList();
    }

    public static List<Property> getPropertyList(DynamicObject attrs) {
        assert isRAttributes(attrs);

        // todo
        return attrs.getShape().getPropertyList();
    }

    public static Iterable<RAttributesLayout.RAttribute> asIterable(DynamicObject attrs, BranchProfile listProfile) {
        return new RAttributeIterable(attrs, listProfile);
    }

    public static Iterable<RAttributesLayout.RAttribute> asIterable(DynamicObject attrs) {
        return new RAttributeIterableNoProfile(attrs);
    }

    public static final class AttrsLayout {
        private final Layout layout = Layout.newLayout().build();
        private final Shape.Allocator allocator = layout.createAllocator();
        public final Shape shape;
        public final Property[] properties;
        public final DynamicObjectFactory factory;

        private AttrsLayout(String... attrNames) {
            this.properties = new Property[attrNames.length];
            Shape s = layout.createShape(new RAttributesType());
            for (int i = 0; i < attrNames.length; i++) {
                Property p = Property.create(attrNames[i], allocator.locationForType(Object.class), 0);
                this.properties[i] = p;
                s = s.addProperty(p);
            }
            shape = s;
            factory = s.createFactory();
        }
    }

    public static final class ConstantShapesAndLocations {
        private static final Shape[] EMPTY_SHAPES_ARRAY = new Shape[0];
        private static final Location[] EMPTY_LOCATIONS_ARRAY = new Location[0];

        public static final ConstantShapesAndLocations EMPTY = new ConstantShapesAndLocations(EMPTY_SHAPES_ARRAY, EMPTY_LOCATIONS_ARRAY);

        private final Shape[] constantShapes;
        private final Location[] constantLocations;

        private ConstantShapesAndLocations(Shape[] constantShapes, Location[] constantLocations) {
            this.constantShapes = constantShapes;
            this.constantLocations = constantLocations;
        }

        public Shape[] getConstantShapes() {
            return constantShapes;
        }

        public Location[] getConstantLocations() {
            return constantLocations;
        }
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

    public interface RAttribute {
        String getName();

        Object getValue();
    }

    @ValueType
    public static final class AttrInstance implements RAttribute {
        private final String name;
        private Object value;

        public AttrInstance(String name, Object value) {
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
