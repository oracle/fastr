/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable.RAttributes;

/**
 * The facade to facilitate manipulation with attributes in FastR. The attributes in FastR are
 * implemented on top of the Truffle object mode. This class contains helper methods for
 * manipulating the attributes.
 * <p>
 * This class used to serve also as a register of known attributes "layouts" in R, which allowed for
 * faster creation of objects that are known from the start to have certain common layout, e.g.,
 * "dims" and "dimnames" attributes. This relied on a deprecated and later removed class
 * {@code Layout} from Truffle, see git history. We may consider re-enabling this optimization in
 * the future.
 */
public final class RAttributesLayout {

    public static class RAttributesType extends ObjectType {
        public static final RAttributesType INSTANCE = new RAttributesType();
    }

    private static final Shape EMPTY_SHAPE = Shape.newBuilder().dynamicType(RAttributesType.INSTANCE).build();

    private static final AttrsLayout CLASS_ATTRS_LAYOUT = new AttrsLayout(RRuntime.CLASS_ATTR_KEY);
    private static final AttrsLayout NAMES_ATTRS_LAYOUT = new AttrsLayout(RRuntime.NAMES_ATTR_KEY);
    private static final AttrsLayout DIM_ATTRS_LAYOUT = new AttrsLayout(RRuntime.DIM_ATTR_KEY);
    private static final AttrsLayout DIMNAMES_ATTRS_LAYOUT = new AttrsLayout(RRuntime.DIMNAMES_ATTR_KEY);
    private static final AttrsLayout NAMES_AND_DIMNAMES_ATTRS_LAYOUT = new AttrsLayout(RRuntime.NAMES_ATTR_KEY, RRuntime.DIMNAMES_ATTR_KEY);
    private static final AttrsLayout ROWNAMES_ATTRS_LAYOUT = new AttrsLayout(RRuntime.ROWNAMES_ATTR_KEY);
    private static final AttrsLayout TSP_ATTRS_LAYOUT = new AttrsLayout(RRuntime.TSP_ATTR_KEY);
    private static final AttrsLayout COMMENT_ATTRS_LAYOUT = new AttrsLayout(RRuntime.COMMENT_ATTR_KEY);
    private static final AttrsLayout NAMES_AND_DIM_ATTRS_LAYOUT = new AttrsLayout(RRuntime.NAMES_ATTR_KEY, RRuntime.DIM_ATTR_KEY);
    private static final AttrsLayout DIM_AND_DIMNAMES_ATTRS_LAYOUT = new AttrsLayout(RRuntime.DIM_ATTR_KEY, RRuntime.DIMNAMES_ATTR_KEY);
    private static final AttrsLayout NAMES_AND_DIM_AND_DIMNAMES_ATTRS_LAYOUT = new AttrsLayout(RRuntime.NAMES_ATTR_KEY, RRuntime.DIM_ATTR_KEY, RRuntime.DIMNAMES_ATTR_KEY);
    private static final AttrsLayout CLASS_AND_CONNID_ATTRS_LAYOUT = new AttrsLayout(RRuntime.CLASS_ATTR_KEY, RRuntime.CONN_ID_ATTR_KEY);

    private static final class AttrsLayout {
        private final String[] attributes;

        AttrsLayout(String... attrs) {
            attributes = attrs;
        }

        // TODO: remove this TruffleBoundary, maybe we should have two versions one for PE code and
        // one for slow-path. GR-34992
        @TruffleBoundary
        public DynamicObject newInstance(Object... values) {
            DynamicObject result = new RAttributes(EMPTY_SHAPE);
            DynamicObjectLibrary dylib = DynamicObjectLibrary.getUncached();
            for (int i = 0; i < attributes.length; i++) {
                dylib.put(result, attributes[i], values[i]);
            }
            return result;
        }
    }

    private RAttributesLayout() {
    }

    public static DynamicObject createRAttributes() {
        return new RAttributes(EMPTY_SHAPE);
    }

    public static DynamicObject createClass(Object cls) {
        return CLASS_ATTRS_LAYOUT.newInstance(cls);
    }

    public static DynamicObject createNames(Object names) {
        return NAMES_ATTRS_LAYOUT.newInstance(names);
    }

    public static DynamicObject createDim(Object dim) {
        return DIM_ATTRS_LAYOUT.newInstance(dim);
    }

    public static DynamicObject createDimNames(Object dimNames) {
        return DIMNAMES_ATTRS_LAYOUT.newInstance(dimNames);
    }

    public static DynamicObject createNamesAndDimNames(Object names, Object dimNames) {
        return NAMES_AND_DIMNAMES_ATTRS_LAYOUT.newInstance(names, dimNames);
    }

    public static DynamicObject createRowNames(Object rowNames) {
        return ROWNAMES_ATTRS_LAYOUT.newInstance(rowNames);
    }

    public static DynamicObject createTsp(Object tsp) {
        return TSP_ATTRS_LAYOUT.newInstance(tsp);
    }

    public static DynamicObject createComment(Object comment) {
        return COMMENT_ATTRS_LAYOUT.newInstance(comment);
    }

    public static DynamicObject createNamesAndDim(Object names, Object dim) {
        return NAMES_AND_DIM_ATTRS_LAYOUT.newInstance(names, dim);
    }

    public static DynamicObject createDimAndDimNames(Object dim, Object dimNames) {
        return DIM_AND_DIMNAMES_ATTRS_LAYOUT.newInstance(dim, dimNames);
    }

    public static DynamicObject createNamesAndDimAndDimNames(Object names, Object dim, Object dimNames) {
        return NAMES_AND_DIM_AND_DIMNAMES_ATTRS_LAYOUT.newInstance(names, dim, dimNames);
    }

    public static DynamicObject createClassWithConnId(Object cls, Object connId) {
        return CLASS_AND_CONNID_ATTRS_LAYOUT.newInstance(cls, connId);
    }

    public static boolean isRAttributes(DynamicObject attrs) {
        return attrs.getShape().getDynamicType() instanceof RAttributesType;
    }

    public static RAttributeIterable asIterable(DynamicObject attrs) {
        return new RAttributeIterable(attrs, attrs.getShape().getProperties());
    }

    @TruffleBoundary
    public static DynamicObject copy(DynamicObject attrs) {
        assert isRAttributes(attrs);
        DynamicObject result = new RAttributes(EMPTY_SHAPE);
        DynamicObjectLibrary dylib = DynamicObjectLibrary.getUncached();
        for (Object key : dylib.getKeyArray(attrs)) {
            Object value = dylib.getOrDefault(attrs, key, null);
            dylib.put(result, key, value);
            if (RSharingAttributeStorage.isShareable(value)) {
                // There is no simple way to determine the correct reference count here and since
                // the value will end up in two attributes collections, it will end up being shared
                // most likely anyway.
                ((RSharingAttributeStorage) value).makeSharedPermanent();
            }
        }
        return result;
    }

    @TruffleBoundary
    public static void clear(DynamicObject attrs) {
        assert isRAttributes(attrs);

        for (Property p : attrs.getShape().getProperties()) {
            DynamicObjectLibrary.getUncached().removeKey(attrs, p.getKey());
        }
    }

    public static final class RAttributeIterable implements Iterable<RAttributesLayout.RAttribute> {

        public static final RAttributeIterable EMPTY = new RAttributeIterable(null, null);

        private final DynamicObject attrs;
        private final Iterable<Property> properties;

        @TruffleBoundary
        public static RAttributeIterable create(DynamicObject attrs) {
            if (attrs == null) {
                return RAttributesLayout.RAttributeIterable.EMPTY;
            }
            return new RAttributeIterable(attrs, Arrays.asList(DynamicObjectLibrary.getUncached().getPropertyArray(attrs)));
        }

        RAttributeIterable(DynamicObject attrs, Iterable<Property> properties) {
            this.attrs = attrs;
            this.properties = properties;
        }

        @Override
        public Iterator<RAttributesLayout.RAttribute> iterator() {
            if (attrs == null || properties == null) {
                return Collections.emptyIterator();
            } else {
                return new Iter(attrs, properties.iterator());
            }
        }
    }

    @ValueType
    public static final class RAttribute {
        private final String name;
        private final Object value;

        public RAttribute(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

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
        @TruffleBoundary
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        @TruffleBoundary
        public RAttribute next() {
            Property p = iter.next();
            Object value = p.get(attrs, shape);
            return new RAttribute((String) p.getKey(), value);
        }
    }
}
