/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RPerfStats;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Base class for all vectors.
 *
 * <pre>
 * Reference counting on vectors:
 * A vector can have three states: temporary, non-shared, shared
 * Operations with copy plus escape semantics (e.g., local variable assignment):
 * - temporary => non-shared
 * - non-shared => temporary copy
 * Operations with copy and non-escaping semantics (e.g., argument)
 * - temporary => temporary
 * - non-shared => shared
 * </pre>
 */
public abstract class RVector extends RSharingAttributeStorage implements RShareable, RAbstractVector, RFFIAccess {

    private static final RStringVector implicitClassHeaderArray = RDataFactory.createStringVector(new String[]{RType.Array.getName()}, true);
    private static final RStringVector implicitClassHeaderMatrix = RDataFactory.createStringVector(new String[]{RType.Matrix.getName()}, true);

    protected boolean complete; // "complete" means: does not contain NAs
    protected int[] dimensions;
    protected RStringVector names;
    private RList dimNames;
    // cache rownames for data frames as they are accessed at every data frame access
    private Object rowNames;

    protected RVector(boolean complete, int length, int[] dimensions, RStringVector names) {
        this.complete = complete;
        this.dimensions = dimensions;
        assert names != this;
        this.names = names;
        this.rowNames = RNull.instance;
        if (names != null) {
            // since this constructor is for internal use only, the assertion shouldn't fail
            assert names.getLength() == length : "size mismatch: " + names.getLength() + " vs. " + length;
            if (dimensions == null) {
                initAttributes(RAttributes.createInitialized(new String[]{RRuntime.NAMES_ATTR_KEY}, new Object[]{names}));
            } else {
                RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                if (dimensions.length != 1) {
                    initAttributes(RAttributes.createInitialized(new String[]{RRuntime.NAMES_ATTR_KEY, RRuntime.DIM_ATTR_KEY}, new Object[]{names, dimensionsVector}));
                } else {
                    // one-dimensional arrays do not have names, only dimnames with one value
                    RList newDimNames = RDataFactory.createList(new Object[]{names});
                    initAttributes(RAttributes.createInitialized(new String[]{RRuntime.DIM_ATTR_KEY, RRuntime.DIMNAMES_ATTR_KEY}, new Object[]{dimensionsVector, newDimNames}));
                    this.dimNames = newDimNames;
                }
            }
        } else {
            if (dimensions != null) {
                initAttributes(RAttributes.createInitialized(new String[]{RRuntime.DIM_ATTR_KEY}, new Object[]{RDataFactory.createIntVector(dimensions, true)}));
            }
        }
    }

    public final int[] getInternalDimensions() {
        return dimensions;
    }

    public final void setInternalDimensions(int[] newDimensions) {
        dimensions = newDimensions;
    }

    public final RStringVector getInternalNames() {
        return names;
    }

    public final void setInternalNames(RStringVector newNames) {
        assert newNames != this;
        names = newNames;
    }

    public final RList getInternalDimNames() {
        return dimNames;
    }

    public final void setInternalDimNames(RList newDimNames) {
        dimNames = newDimNames;
    }

    public final Object getInternalRowNames() {
        return rowNames;
    }

    public final void setInternalRowNames(Object newRowNames) {
        rowNames = newRowNames;
    }

    @Override
    public final void setComplete(boolean complete) {
        this.complete = complete;
        assert verify();
    }

    private void removeAttributeMapping(String key) {
        if (this.attributes != null) {
            this.attributes.remove(key);
            if (this.attributes.size() == 0) {
                this.attributes = null;
            }
        }
    }

    @Override
    public final RStringVector getNames(RAttributeProfiles attrProfiles) {
        if (attrProfiles.attrNullProfile(attributes == null)) {
            return null;
        } else {
            if (attrProfiles.attrNullNamesProfile(names == null)) {
                if (dimNames != null && dimNames.getLength() == 1) {
                    return (RStringVector) dimNames.getDataAt(0);
                } else {
                    return null;
                }
            } else {
                return names;
            }
        }
    }

    /*
     * Version without profiles is used by RDeparse and for internal attribute copying (both are not
     * performance-critical)
     */
    public final RStringVector getNames() {
        if (names == null) {
            if (dimNames != null && dimNames.getLength() == 1) {
                return (RStringVector) dimNames.getDataAt(0);
            } else {
                return null;
            }
        } else {
            return names;
        }
    }

    @TruffleBoundary
    public final int getElementIndexByName(RAttributeProfiles attrProfiles, String name) {
        if (getNames(attrProfiles) == null) {
            return -1;
        }
        for (int i = 0; i < names.getLength(); i++) {
            if (names.getDataAt(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the first element in the names list that {@code name} is a prefix of, and return its
     * index. If there are no names, or none is found, or there are multiple inexact matches, return
     * -1.
     */
    @TruffleBoundary
    public final int getElementIndexByNameInexact(RAttributeProfiles attrProfiles, String name) {
        if (getNames(attrProfiles) == null) {
            return -1;
        }
        boolean oneMatch = false;
        int match = -1;
        for (int i = 0; i < names.getLength(); i++) {
            if (names.getDataAt(i).startsWith(name)) {
                if (oneMatch) {
                    return -1;
                } else {
                    match = i;
                    oneMatch = true;
                }
            }
        }
        return match;
    }

    /**
     * Guarded method that checks whether {@code attributes} is initialized.
     *
     * @param attribute
     * @param value
     */
    private void putAttribute(String attribute, Object value) {
        initAttributes();
        attributes.put(attribute, value);
    }

    @Override
    @TruffleBoundary
    public final void setAttr(String name, Object value) {
        if (attributes == null) {
            initAttributes();
        }
        if (name.equals(RRuntime.NAMES_ATTR_KEY)) {
            setNames((RStringVector) value);
        } else if (name.equals(RRuntime.DIM_ATTR_KEY)) {
            if (value instanceof Integer) {
                setDimensions(new int[]{(int) value});
            } else {
                setDimensions(((RAbstractIntVector) value).materialize().getDataCopy());
            }
        } else if (name.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
            setDimNames((RList) value);
        } else if (name.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
            setRowNames((RAbstractVector) RRuntime.asAbstractVector(value));
        } else if (name.equals(RRuntime.CLASS_ATTR_KEY)) {
            throw RInternalError.unimplemented("The \"class\" attribute should be set using a separate method");
        } else {
            attributes.put(name, value);
        }
    }

    @Override
    public final Object getAttr(RAttributeProfiles attrProfiles, String name) {
        if (attrProfiles.attrNullProfile(attributes == null)) {
            return null;
        } else {
            return attributes.get(name);
        }
    }

    private void removeAttrInternal(String name) {
        if (name.equals(RRuntime.NAMES_ATTR_KEY)) {
            setNames(null);
        } else if (name.equals(RRuntime.DIM_ATTR_KEY)) {
            setDimensions(null);
        } else if (name.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
            setDimNames((RList) null);
        } else if (name.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
            setRowNames(null);
        } else if (name.equals(RRuntime.CLASS_ATTR_KEY)) {
            throw RInternalError.unimplemented("The \"class\" attribute should be reset using a separate method");
        } else {
            attributes.remove(name);
            // nullify only here because other methods invoke removeAttributeMapping which does
            // it already
            if (attributes.size() == 0) {
                attributes = null;
            }
        }

    }

    @Override
    public final void removeAttr(RAttributeProfiles attrProfiles, String name) {
        if (attrProfiles.attrNullProfile(attributes == null)) {
            return;
        } else {
            removeAttrInternal(name);
        }
    }

    @Override
    public final void removeAttr(String name) {
        if (attributes != null) {
            removeAttrInternal(name);
        }
    }

    /**
     * Sets names attribute without doing any error checking - to be used sparingly.
     *
     * @param newNames
     */
    public final void setNamesNoCheck(RStringVector newNames) {
        if (newNames == null) {
            removeAttributeMapping(RRuntime.NAMES_ATTR_KEY);
        } else {
            putAttribute(RRuntime.NAMES_ATTR_KEY, newNames);
        }
        assert newNames != this;
        this.names = newNames;
    }

    @Override
    public final void setNames(RStringVector newNames) {
        // TODO pass invoking Node
        setNames(newNames, RError.SHOW_CALLER2);
    }

    @TruffleBoundary
    private void setNames(RStringVector newNames, RBaseNode invokingNode) {
        if (attributes != null && newNames == null) {
            // whether it's one dimensional array or not, assigning null always removes the "names"
            // attribute
            removeAttributeMapping(RRuntime.NAMES_ATTR_KEY);
            this.names = null;
        } else if (newNames != null) {
            if (newNames.getLength() > this.getLength()) {
                throw RError.error(invokingNode, RError.Message.ATTRIBUTE_VECTOR_SAME_LENGTH, RRuntime.NAMES_ATTR_KEY, newNames.getLength(), this.getLength());
            }
            if (this.dimensions != null && dimensions.length == 1) {
                // for one dimensional array, "names" is really "dimnames[[1]]" (see R documentation
                // for "names" function)
                RList newDimNames = RDataFactory.createList(new Object[]{newNames});
                newDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
                putAttribute(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
                this.dimNames = newDimNames;
            } else {
                putAttribute(RRuntime.NAMES_ATTR_KEY, newNames);
                assert newNames != this;
                this.names = newNames;
            }
        }
    }

    @Override
    public final RList getDimNames(RAttributeProfiles attrProfiles) {
        return getDimNames();
    }

    public final RList getDimNames() {
        return dimNames;
    }

    /**
     * Sets dimnames attribute without doing any error checking - to be used sparingly.
     *
     * @param newDimNames
     */
    public final void setDimNamesNoCheck(RList newDimNames) {
        if (newDimNames == null) {
            removeAttributeMapping(RRuntime.DIMNAMES_ATTR_KEY);
        } else {
            putAttribute(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
        }
        this.dimNames = newDimNames;
    }

    @Override
    public final void setDimNames(RList newDimNames) {
        // TODO pass invoking node
        setDimNames(newDimNames, RError.SHOW_CALLER2);
    }

    @TruffleBoundary
    private void setDimNames(RList newDimNames, RBaseNode invokingNode) {
        if (attributes != null && newDimNames == null) {
            removeAttributeMapping(RRuntime.DIMNAMES_ATTR_KEY);
            this.dimNames = null;
        } else if (newDimNames != null) {
            if (dimensions == null) {
                throw RError.error(invokingNode, RError.Message.DIMNAMES_NONARRAY);
            }
            int newDimNamesLength = newDimNames.getLength();
            if (newDimNamesLength > dimensions.length) {
                throw RError.error(invokingNode, RError.Message.DIMNAMES_DONT_MATCH_DIMS, newDimNamesLength, dimensions.length);
            }
            for (int i = 0; i < newDimNamesLength; i++) {
                Object dimObject = newDimNames.getDataAt(i);
                if (dimObject != RNull.instance) {
                    if (dimObject instanceof String) {
                        if (dimensions[i] != 1) {
                            throw RError.error(invokingNode, RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                        }
                    } else {
                        RStringVector dimVector = (RStringVector) dimObject;
                        if (dimVector == null || dimVector.getLength() == 0) {
                            newDimNames.updateDataAt(i, RNull.instance, null);
                        } else if (dimVector.getLength() != dimensions[i]) {
                            throw RError.error(invokingNode, RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                        }
                    }
                }
            }

            RList resDimNames = newDimNames;
            if (newDimNamesLength < dimensions.length) {
                // resize the array and fill the missing entries with NULL-s
                resDimNames = resDimNames.copyResized(dimensions.length, true);
                resDimNames.setAttributes(newDimNames);
                for (int i = newDimNamesLength; i < dimensions.length; i++) {
                    resDimNames.updateDataAt(i, RNull.instance, null);
                }
            }
            putAttribute(RRuntime.DIMNAMES_ATTR_KEY, resDimNames);
            resDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
            this.dimNames = resDimNames;
        }
    }

    @Override
    public final Object getRowNames(RAttributeProfiles attrProfiles) {
        return getRowNames();
    }

    public final Object getRowNames() {
        return rowNames;
    }

    @Override
    public final void setRowNames(RAbstractVector newRowNames) {
        if (newRowNames == null) {
            removeAttributeMapping(RRuntime.ROWNAMES_ATTR_KEY);
            this.rowNames = RNull.instance;
        } else {
            putAttribute(RRuntime.ROWNAMES_ATTR_KEY, newRowNames);
            this.rowNames = newRowNames;
        }
    }

    @Override
    public final boolean isComplete() {
        return complete;
    }

    @Override
    public final boolean hasDimensions() {
        return dimensions != null;
    }

    @Override
    public final boolean isMatrix() {
        return dimensions != null && dimensions.length == 2;
    }

    @Override
    public final boolean isArray() {
        return dimensions != null && dimensions.length > 0;
    }

    @Override
    public final int[] getDimensions() {
        return dimensions;
    }

    /**
     * Sets dimensions attribute without doing any error checking - to be used sparingly.
     *
     * @param newDimensions
     */
    public final void setDimensionsNoCheck(int[] newDimensions) {
        if (newDimensions == null) {
            removeAttributeMapping(RRuntime.DIM_ATTR_KEY);
        } else {
            putAttribute(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(newDimensions, RDataFactory.COMPLETE_VECTOR));
        }
        this.dimensions = newDimensions;
    }

    @Override
    public final void setDimensions(int[] newDimensions) {
        setDimensions(newDimensions, null);
    }

    private void setDimensions(int[] newDimensions, RBaseNode invokingNode) {
        if (attributes != null && newDimensions == null) {
            removeAttributeMapping(RRuntime.DIM_ATTR_KEY);
            setDimNames(null, invokingNode);
        } else if (newDimensions != null) {
            verifyDimensions(getLength(), newDimensions, invokingNode);
            putAttribute(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(newDimensions, RDataFactory.COMPLETE_VECTOR));
        }
        this.dimensions = newDimensions;
    }

    @Override
    public RAbstractContainer setClassAttr(RStringVector classAttr, boolean convertToInt) {
        return setClassAttrInternal(this, classAttr, null, convertToInt);
    }

    public static RAbstractContainer setVectorClassAttr(RVector vector, RStringVector classAttr, RAbstractContainer enclosingFactor) {
        return setClassAttrInternal(vector, classAttr, enclosingFactor, false);
    }

    private static RAbstractContainer setClassAttrInternal(RVector vector, RStringVector classAttr, RAbstractContainer enclosingFactor, boolean convertToInt) {
        if (vector.attributes == null && classAttr != null && classAttr.getLength() != 0) {
            vector.initAttributes();
        }
        if (vector.attributes != null && (classAttr == null || classAttr.getLength() == 0)) {
            vector.removeAttributeMapping(RRuntime.CLASS_ATTR_KEY);
            // class attribute removed - no longer a data frame or factor (even if it was before)
            return vector;
        } else if (classAttr != null && classAttr.getLength() != 0) {
            for (int i = 0; i < classAttr.getLength(); i++) {
                String attr = classAttr.getDataAt(i);
                if (RRuntime.CLASS_FACTOR.equals(attr)) {
                    // For Factors we only have to check if the data-type is Integer, because
                    // otherwise we must show error.
                    // Note: this can only happen if the class is set by hand to some non-integral
                    // vector, i.e. attr(doubles, 'class') <- 'factor'
                    vector.putAttribute(RRuntime.CLASS_ATTR_KEY, classAttr);
                    if (vector.getElementClass() != RInteger.class) {
                        // TODO: check when this 'convertToInt' is necessary
                        if (vector.getElementClass() == RDouble.class && convertToInt) {
                            RDoubleVector sourceVector = (RDoubleVector) vector;
                            int[] data = new int[sourceVector.getLength()];
                            for (int j = 0; j < data.length; j++) {
                                data[j] = RRuntime.double2int(sourceVector.getDataAt(j));
                            }
                            RIntVector resVector = RDataFactory.createIntVector(data, sourceVector.isComplete());
                            resVector.copyAttributesFrom(sourceVector);
                            return resVector;
                        } else {
                            // TODO: add invoking node
                            throw RError.error(RError.SHOW_CALLER2, RError.Message.ADDING_INVALID_CLASS, "factor");
                        }
                    }

                    break;  // TODO: check if setting connection and then factor shows both errors
                } else if (RType.Connection.getName().equals(attr)) {
                    // convert to RConnection
                    return ConnectionSupport.fromVector(vector, classAttr);
                }
            }
            vector.putAttribute(RRuntime.CLASS_ATTR_KEY, classAttr);
        }
        return vector;
    }

    public final void setAttributes(RVector result) {
        result.names = this.names;
        result.dimNames = this.dimNames;
        result.rowNames = this.rowNames;
        result.dimensions = this.dimensions;
        if (this.attributes != null) {
            result.attributes = this.attributes.copy();
        }
    }

    @Override
    public final RVector copy() {
        RVector result = internalCopy();
        setAttributes(result);
        incCopyCount();
        result.typedValueInfo = typedValueInfo;
        return result;
    }

    @Override
    public final RVector copyDropAttributes() {
        return internalCopy();
    }

    @Override
    public RVector deepCopy() {
        RVector result = internalDeepCopy();
        setAttributes(result);
        return result;
    }

    // to be overridden by recursive structures
    protected RVector internalDeepCopy() {
        return internalCopy();
    }

    @Override
    public RVector copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
        // TODO support for higher dimensions
        assert newDimensions.length == 2;
        RVector result = copyResized(newDimensions[0] * newDimensions[1], fillNA);
        result.setDimensions(newDimensions);
        return result;
    }

    public final boolean verify() {
        return internalVerify();
    }

    protected abstract String getDataAtAsString(int index);

    protected abstract RVector internalCopy();

    protected abstract boolean internalVerify();

    /**
     * Update a data item in the vector. Possibly not as efficient as type-specific methods, but in
     * some cases it likely does not matter (e.g. if used alongside I/O operations).
     *
     * @param i index of the vector item to be updated
     * @param o updated value
     * @param naCheck NA check used to change vector's mode in case value is NA
     * @return updated vector
     */
    public abstract RVector updateDataAtAsObject(int i, Object o, NACheck naCheck);

    public abstract void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex);

    public final RAttributable copyAttributesFrom(RAttributeProfiles attrProfiles, RAbstractContainer vector) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (this.dimNames == null);
        assert (this.rowNames == RNull.instance);
        assert (this.dimensions == null);
        assert (this.attributes == null || this.attributes.size() == 0) : this.attributes.size();
        if (vector.getDimensions() == null || vector.getDimensions().length != 1) {
            // only assign name attribute if it's not represented as dimnames (as is the case for
            // one-dimensional array)
            this.names = vector.getNames(attrProfiles);
        }
        this.dimNames = vector.getDimNames(attrProfiles);
        this.rowNames = vector.getRowNames(attrProfiles);
        this.dimensions = vector.getDimensions();
        RAttributes vecAttributes = vector.getAttributes();
        if (vecAttributes != null) {
            this.attributes = vecAttributes.copy();
            return this.setClassAttr((RStringVector) vecAttributes.get(RRuntime.CLASS_ATTR_KEY), false);
        } else {
            return this;
        }
    }

    /*
     * Internal version without profiles used in a rare (and already slow) case of double-to-int
     * vector conversion when setting class attribute
     */
    protected final RAttributable copyAttributesFrom(RVector vector) {
        if (vector.getDimensions() == null || vector.getDimensions().length != 1) {
            this.names = vector.getNames();
        }
        this.dimNames = vector.getDimNames();
        this.rowNames = vector.getRowNames();
        this.dimensions = vector.getDimensions();
        RAttributes vecAttributes = vector.getAttributes();
        if (vecAttributes != null) {
            this.attributes = vecAttributes.copy();
            return this.setClassAttr((RStringVector) vecAttributes.get(RRuntime.CLASS_ATTR_KEY), false);
        } else {
            return this;
        }
    }

    public final void copyNamesDimsDimNamesFrom(RAttributeProfiles attrProfiles, RAbstractVector vector, RBaseNode invokingNode) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (this.dimNames == null);
        assert (this.dimensions == null);
        assert (this.attributes == null);
        // for some reason, names is copied first, then dims, then dimnames
        if (vector.getDimensions() == null || vector.getDimensions().length != 1) {
            // only assign name attribute if it's not represented as dimnames (as is the case for
            // one-dimensional arrasy)
            this.setNames(vector.getNames(attrProfiles), invokingNode);
        }
        this.setDimensions(vector.getDimensions(), invokingNode);
        this.setDimNames(vector.getDimNames(attrProfiles), invokingNode);
    }

    public final boolean copyNamesFrom(RAttributeProfiles attrProfiles, RAbstractVector vector) {
        if (this.dimensions == null) {
            RStringVector vecNames = vector.getNames(attrProfiles);
            if (vecNames != null) {
                this.setNames(vecNames);
                return true;
            } else {
                return false;
            }
        } else {
            if (vector.getDimNames(attrProfiles) != null) {
                this.setDimNames(vector.getDimNames(attrProfiles));
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressFBWarnings(value = "ES_COMPARING_STRINGS_WITH_EQ", justification = "all three string constants below are supposed to be used as identities")
    public final RVector copyRegAttributesFrom(RAbstractContainer vector) {
        RAttributes orgAttributes = vector.getAttributes();
        if (orgAttributes != null) {
            Object newRowNames = null;
            for (RAttribute e : orgAttributes) {
                String name = e.getName();
                if (name != RRuntime.DIM_ATTR_KEY && name != RRuntime.DIMNAMES_ATTR_KEY && name != RRuntime.NAMES_ATTR_KEY) {
                    Object val = e.getValue();
                    putAttribute(name, val);
                    if (name == RRuntime.ROWNAMES_ATTR_KEY) {
                        newRowNames = val;
                    }
                }
            }
            this.rowNames = newRowNames == null ? RNull.instance : newRowNames;
        }
        return this;
    }

    @Override
    public final RVector resize(int size) {
        return resize(size, true);
    }

    private RVector resize(int size, boolean resetAll) {
        this.complete &= getLength() >= size;
        RVector res = this;
        RStringVector oldNames = res.names;
        res = copyResized(size, true);
        if (this.isShared()) {
            assert res.isTemporary();
            res.incRefCount();
        }
        if (resetAll) {
            resetAllAttributes(oldNames == null);
        } else {
            res.copyAttributesFrom(this);
            res.setDimensionsNoCheck(null);
            res.setDimNamesNoCheck(null);
        }
        if (oldNames != null) {
            oldNames = oldNames.resizeWithEmpty(size);
            res.putAttribute(RRuntime.NAMES_ATTR_KEY, oldNames);
            res.names = oldNames;
        }
        return res;
    }

    @TruffleBoundary
    public final void resetDimensions(int[] newDimensions) {
        // reset all attributes other than dimensions;
        this.dimensions = newDimensions;
        // whether we nullify dimensions or re-set them to a different value, names and dimNames
        // must be reset
        this.names = null;
        this.dimNames = null;
        if (this.dimensions != null) {
            putAttribute(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(this.dimensions, true));
        } else {
            // nullifying dimensions does not reset regular attributes
            if (this.attributes != null) {
                removeAttributeMapping(RRuntime.DIM_ATTR_KEY);
                removeAttributeMapping(RRuntime.DIMNAMES_ATTR_KEY);
                removeAttributeMapping(RRuntime.NAMES_ATTR_KEY);
            }
        }
    }

    @Override
    public final RAttributes resetAllAttributes(boolean nullify) {
        this.dimensions = null;
        this.names = null;
        this.dimNames = null;
        this.rowNames = RNull.instance;
        if (nullify) {
            this.attributes = null;
        } else {
            if (this.attributes != null) {
                this.attributes.clear();
            }
        }
        return this.attributes;
    }

    @Override
    public final boolean isObject(RAttributeProfiles attrProfiles) {
        return this.getClassAttr(attrProfiles) != null ? true : false;
    }

    // As shape of the vector may change at run-time we need to compute
    // class hierarchy on the fly.
    protected final RStringVector getClassHierarchyHelper(RStringVector implicitClassHeader) {
        if (isMatrix()) {
            return implicitClassHeaderMatrix;
        }
        if (isArray()) {
            return implicitClassHeaderArray;
        }
        return implicitClassHeader;
    }

    @Override
    public final RShareable materializeToShareable() {
        return materialize();
    }

    public static void verifyDimensions(int vectorLength, int[] newDimensions, RBaseNode invokingNode) {
        int length = 1;
        for (int i = 0; i < newDimensions.length; i++) {
            if (RRuntime.isNA(newDimensions[i])) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(invokingNode, RError.Message.DIMS_CONTAIN_NA);
            } else if (newDimensions[i] < 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(invokingNode, RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
            }
            length *= newDimensions[i];
        }
        if (length != vectorLength && vectorLength > 0) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(invokingNode, RError.Message.DIMS_DONT_MATCH_LENGTH, length, vectorLength);
        }
    }

    private static final ConditionProfile statsProfile = ConditionProfile.createBinaryProfile();

    @CompilationFinal private static PerfHandler stats;

    private static void incCopyCount() {
        if (statsProfile.profile(stats != null)) {
            stats.record(null);
        }
    }

    static {
        RPerfStats.register(new PerfHandler());
    }

    private static class PerfHandler implements RPerfStats.Handler {

        private static int count;

        void record(@SuppressWarnings("unused") Object data) {
            count++;
        }

        @Override
        public void initialize(String optionData) {
            stats = this;
            count = 0;
        }

        @Override
        public String getName() {
            return "vectorcopies";
        }

        @Override
        public void report() {
            RPerfStats.out().printf("NUMBER OF VECTOR COPIES: %d\n", count);
        }
    }

    private static final int MAX_TOSTRING_LENGTH = 100;

    protected String toString(Function<Integer, String> element) {
        CompilerAsserts.neverPartOfCompilation();
        StringBuilder str = new StringBuilder("[");
        for (int i = 0; i < getLength(); i++) {
            if (i > 0) {
                str.append(", ");
            }
            str.append(element.apply(i));
            if (str.length() > MAX_TOSTRING_LENGTH - 1) {
                str.setLength(MAX_TOSTRING_LENGTH - 4);
                str.append("...");
                break;
            }
        }
        return str.append(']').toString();
    }
}
