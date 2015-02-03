/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

import edu.umd.cs.findbugs.annotations.*;

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
public abstract class RVector extends RBounded implements RShareable, RAbstractVector {

    protected boolean complete; // "complete" means: does not contain NAs
    private int matrixDimension;
    protected int[] dimensions;
    protected RStringVector names;
    protected RList dimNames;
    private RAttributes attributes;
    private boolean shared;
    private boolean temporary = true;

    protected RVector(boolean complete, int length, int[] dimensions, RStringVector names) {
        this.complete = complete;
        this.dimensions = dimensions;
        setMatrixDimensions(dimensions, length);
        this.names = names;
        if (names != null) {
            // since this constructor is for internal use only, the assertion shouldn't fail
            assert names.getLength() == length;
            if (dimensions == null || dimensions.length != 1) {
                putAttribute(RRuntime.NAMES_ATTR_KEY, names);
                if (dimensions != null) {
                    putAttribute(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(dimensions, true));
                }
                this.dimNames = null;
            } else {
                if (dimensions != null) {
                    putAttribute(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(dimensions, true));
                }
                // one-dimensional arrays do not have names, only dimnames with one value
                RList newDimNames = RDataFactory.createList(new Object[]{names});
                putAttribute(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
                this.dimNames = newDimNames;
            }
        } else {
            if (dimensions != null) {
                putAttribute(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(dimensions, true));
            }
            this.dimNames = null;
        }
    }

    protected RVector(boolean complete, int length, int[] dimensions) {
        this(complete, length, dimensions, null);
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    private void removeAttributeMapping(String key) {
        if (this.attributes != null) {
            this.attributes.remove(key);
            if (this.attributes.size() == 0) {
                this.attributes = null;
            }
        }
    }

    private void setMatrixDimensions(int[] newDimensions, int vectorLength) {
        if (newDimensions != null && newDimensions.length == 2) {
            matrixDimension = newDimensions[0];
            // this assertion should not fail (should be signaled as error before getting to this
            // point)
            assert newDimensions[0] * newDimensions[1] == vectorLength;
        } else {
            matrixDimension = 0;
        }
    }

    public final RStringVector getNames() {
        if (names == null) {
            if (dimNames != null && dimNames.getLength() == 1) {
                RStringVector res = (RStringVector) dimNames.getDataAt(0);
                return res.isShared() ? (RStringVector) res.copy() : res;
            } else {
                return null;
            }
        } else {
            return names.isShared() ? (RStringVector) names.copy() : names;
        }
    }

    @TruffleBoundary
    public final int getElementIndexByName(String name) {
        if (getNames() == null) {
            return -1;
        }
        for (int i = 0; i < names.getLength(); ++i) {
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
    public final int getElementIndexByNameInexact(String name) {
        if (getNames() == null) {
            return -1;
        }
        boolean oneMatch = false;
        int match = -1;
        for (int i = 0; i < names.getLength(); ++i) {
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

    public final RAttributes initAttributes() {
        if (attributes == null) {
            attributes = RAttributes.create();
        }
        return attributes;
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
            setRowNames(value);
        } else if (name.equals(RRuntime.CLASS_ATTR_KEY)) {
            throw Utils.nyi("The \"class\" attribute should be set using a separate method");
        } else {
            attributes.put(name, value);
        }
    }

    public final Object getAttr(String name) {
        if (attributes == null) {
            return null;
        }
        return attributes.get(name);
    }

    public final void removeAttr(String name) {
        if (attributes != null) {
            if (name.equals(RRuntime.NAMES_ATTR_KEY)) {
                setNames(null);
            } else if (name.equals(RRuntime.DIM_ATTR_KEY)) {
                setDimensions(null);
            } else if (name.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
                setDimNames((RList) null);
            } else if (name.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
                setRowNames(null);
            } else if (name.equals(RRuntime.CLASS_ATTR_KEY)) {
                throw Utils.nyi("The \"class\" attribute should be reset using a separate method");
            } else {
                attributes.remove(name);
            }
            if (attributes.size() == 0) {
                attributes = null;
            }
        }
    }

    @TruffleBoundary
    public final void setLevels(Object newLevels) {
        if (attributes != null && newLevels == null) {
            // whether it's one dimensional array or not, assigning null always removes the "Levels"
            // attribute
            removeAttributeMapping(RRuntime.LEVELS_ATTR_KEY);
        } else if (newLevels != null && newLevels != RNull.instance) {
            putAttribute(RRuntime.LEVELS_ATTR_KEY, newLevels);
        }
    }

    public final RVector getLevels() {
        return (RVector) attributes.get(RRuntime.LEVELS_ATTR_KEY);
    }

    public final void setNames(RStringVector newNames) {
        setNames(newNames, null);
    }

    @TruffleBoundary
    public final void setNames(RStringVector newNames, SourceSection sourceSection) {
        if (attributes != null && newNames == null) {
            // whether it's one dimensional array or not, assigning null always removes the "names"
            // attribute
            removeAttributeMapping(RRuntime.NAMES_ATTR_KEY);
            this.names = null;
        } else if (newNames != null) {
            if (newNames.getLength() > this.getLength()) {
                throw RError.error(sourceSection, RError.Message.ATTRIBUTE_VECTOR_SAME_LENGTH, RRuntime.NAMES_ATTR_KEY, newNames.getLength(), this.getLength());
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
                this.names = newNames;
            }
        }
    }

    public final RList getDimNames() {
        return dimNames;
    }

    public final void setDimNames(RList newDimNames) {
        setDimNames(newDimNames, null);
    }

    @TruffleBoundary
    public final void setDimNames(RList newDimNames, SourceSection sourceSection) {
        if (attributes != null && newDimNames == null) {
            removeAttributeMapping(RRuntime.DIMNAMES_ATTR_KEY);
            this.matrixDimension = 0;
        } else if (newDimNames != null) {
            if (dimensions == null) {
                throw RError.error(sourceSection, RError.Message.DIMNAMES_NONARRAY);
            }
            int newDimNamesLength = newDimNames.getLength();
            if (newDimNamesLength > dimensions.length) {
                throw RError.error(sourceSection, RError.Message.DIMNAMES_DONT_MATCH_DIMS, newDimNamesLength, dimensions.length);
            }
            for (int i = 0; i < newDimNamesLength; i++) {
                Object dimObject = newDimNames.getDataAt(i);
                if (dimObject != RNull.instance) {
                    if (dimObject instanceof String) {
                        if (dimensions[i] != 1) {
                            throw RError.error(sourceSection, RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                        }
                    } else {
                        RStringVector dimVector = (RStringVector) dimObject;
                        if (dimVector.getLength() == 0) {
                            newDimNames.updateDataAt(i, RNull.instance, null);
                        } else if (dimVector.getLength() != dimensions[i]) {
                            throw RError.error(sourceSection, RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                        }
                    }
                }
            }

            if (newDimNamesLength < dimensions.length) {
                // resize the array and fill the missing entries with NULL-s
                newDimNames.resizeInternal(dimensions.length);
                for (int i = newDimNamesLength; i < dimensions.length; i++) {
                    newDimNames.updateDataAt(i, RNull.instance, null);
                }
            }
            putAttribute(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
            newDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
        }
        this.dimNames = newDimNames;
    }

    @Override
    public final Object getRowNames() {
        if (attributes == null) {
            return RNull.instance;
        } else {
            Object result = attributes.get(RRuntime.ROWNAMES_ATTR_KEY);
            return result == null ? RNull.instance : result;
        }
    }

    public final void setRowNames(Object rowNames) {
        if (attributes != null && rowNames == null) {
            removeAttributeMapping(RRuntime.ROWNAMES_ATTR_KEY);
        } else if (rowNames != null) {
            assert rowNames instanceof RAbstractVector;
            putAttribute(RRuntime.ROWNAMES_ATTR_KEY, rowNames);
        }
    }

    public final RAttributes getAttributes() {
        return attributes;
    }

    public final boolean isComplete() {
        return complete;
    }

    @Override
    public final void markNonTemporary() {
        temporary = false;
    }

    @Override
    public final boolean isTemporary() {
        return temporary;
    }

    @Override
    public final boolean isShared() {
        return shared;
    }

    @Override
    public final RVector makeShared() {
        if (temporary) {
            temporary = false;
        } else {
            shared = true;
        }
        return this;
    }

    public final boolean hasDimensions() {
        return dimensions != null;
    }

    public final boolean isMatrix() {
        return matrixDimension != 0;
    }

    public final boolean isArray() {
        return dimensions != null && dimensions.length > 0;
    }

    public final int[] getDimensions() {
        return dimensions;
    }

    public final void setDimensions(int[] newDimensions) {
        setDimensions(newDimensions, null);
    }

    public final void setDimensions(int[] newDimensions, SourceSection sourceSection) {
        if (attributes != null && newDimensions == null) {
            removeAttributeMapping(RRuntime.DIM_ATTR_KEY);
            setDimNames(null, sourceSection);
        } else if (newDimensions != null) {
            verifyDimensions(newDimensions, sourceSection);
            setMatrixDimensions(newDimensions, getLength());
            putAttribute(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(newDimensions, true));
        }
        this.dimensions = newDimensions;
    }

    public final RStringVector getClassAttr() {
        if (attributes == null) {
            return null;
        } else {
            return (RStringVector) attributes.get(RRuntime.CLASS_ATTR_KEY);
        }
    }

    @Override
    public RAbstractContainer setClassAttr(RStringVector classAttr) {
        return setClassAttrInternal(this, classAttr, null, null, true);
    }

    public static RAbstractContainer setVectorClassAttr(RVector vector, RStringVector classAttr, RAbstractContainer enclosingDataFrame, RAbstractContainer enclosingFactor) {
        return setClassAttrInternal(vector, classAttr, enclosingDataFrame, enclosingFactor, false);
    }

    private static RAbstractContainer setClassAttrInternal(RVector vector, RStringVector classAttr, RAbstractContainer enclosingDataFrame, RAbstractContainer enclosingFactor, boolean convertToInt) {
        if (vector.attributes == null && classAttr != null && classAttr.getLength() != 0) {
            vector.initAttributes();
        }
        if (vector.attributes != null && (classAttr == null || classAttr.getLength() == 0)) {
            vector.removeAttributeMapping(RRuntime.CLASS_ATTR_KEY);
            // class attribute removed - no longer a data frame or factor (even if it was before)
            return vector;
        } else if (classAttr != null && classAttr.getLength() != 0) {
            boolean ordered = false;
            for (int i = 0; i < classAttr.getLength(); i++) {
                String attr = classAttr.getDataAt(i);
                if (RRuntime.CLASS_ORDERED.equals(attr)) {
                    // "ordered" must be specified before "factor" - hence it's enough to do the
                    // check only before encountering the "factor"
                    ordered = true;
                }
                if (RType.DataFrame.getName().equals(attr)) {
                    vector.putAttribute(RRuntime.CLASS_ATTR_KEY, classAttr);
                    if (enclosingDataFrame != null) {
                        // was a frame and still is a frame
                        return enclosingDataFrame;
                    } else {
                        // it's a data frame now
                        return RDataFactory.createDataFrame(vector);
                    }
                } else if (RType.Factor.getName().equals(attr)) {
                    vector.putAttribute(RRuntime.CLASS_ATTR_KEY, classAttr);
                    if (enclosingFactor != null) {
                        // was a factor and still is a factor
                        return enclosingFactor;
                    } else {
                        RIntVector resVector;
                        if (vector.getElementClass() != RInt.class) {
                            if (vector.getElementClass() == RDouble.class && convertToInt) {
                                RAbstractDoubleVector sourceVector = (RAbstractDoubleVector) vector;
                                int[] data = new int[sourceVector.getLength()];
                                for (int j = 0; j < data.length; j++) {
                                    data[j] = RRuntime.double2int(sourceVector.getDataAt(j));
                                }
                                resVector = RDataFactory.createIntVector(data, sourceVector.isComplete());
                                resVector.copyAttributesFrom(sourceVector);
                            } else {
                                // TODO: add source section
                                throw RError.error(null, RError.Message.ADDING_INVALID_CLASS, "factor");
                            }
                        } else {
                            resVector = (RIntVector) vector;
                        }
                        // it's a factor now
                        return RDataFactory.createFactor(resVector, ordered);
                    }
                }
            }
            vector.putAttribute(RRuntime.CLASS_ATTR_KEY, classAttr);
        }
        return vector;
    }

    protected final void setAttributes(RVector result) {
        result.names = this.names;
        result.dimNames = this.dimNames;
        result.dimensions = this.dimensions;
        result.setMatrixDimensions(result.dimensions, result.getLength());
        if (this.attributes != null) {
            result.attributes = this.attributes.copy();
        }
    }

    @Override
    public final RVector copy() {
        RVector result = internalCopy();
        setAttributes(result);
        return result;
    }

    @Override
    public final RVector copyDropAttributes() {
        return internalCopy();
    }

    public final RVector deepCopy() {
        RVector result = internalDeepCopy();
        setAttributes(result);
        return result;
    }

    // to be overridden by recursive structures
    protected RVector internalDeepCopy() {
        return internalCopy();
    }

    @Override
    public RVector copyResizedWithDimensions(int[] newDimensions) {
        // TODO support for higher dimensions
        assert newDimensions.length == 2;
        RVector result = copyResized(newDimensions[0] * newDimensions[1], false);
        result.setDimensions(newDimensions);
        return result;
    }

    public final boolean verify() {
        return internalVerify();
    }

    protected abstract void resizeInternal(int size);

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

    public final RStringVector toStringVector() {
        String[] values = new String[getLength()];
        for (int i = 0; i < getLength(); ++i) {
            values[i] = this.getDataAtAsString(i);
        }
        return RDataFactory.createStringVector(values, this.isComplete());
    }

    public final boolean isInBounds(int firstPosition, int secondPosition) {
        assert isMatrix();
        return firstPosition >= 1 && firstPosition <= matrixDimension && (convertToIndex(secondPosition) * matrixDimension + firstPosition) <= getLength();
    }

    public static int convertToIndex(int position) {
        return position - 1;
    }

    public final int convertToIndex(int firstPosition, int secondPosition) {
        assert isMatrix();
        assert isInBounds(firstPosition, secondPosition);
        return convertToIndex(firstPosition) + convertToIndex(secondPosition) * matrixDimension;
    }

    public final void copyAttributesFrom(RAbstractVector vector) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (this.dimNames == null);
        assert (this.dimensions == null);
        assert (this.attributes == null);
        if (vector.getDimensions() == null || vector.getDimensions().length != 1) {
            // only assign name attribute if it's not represented as dimnames (as is the case for
            // one-dimensional array)
            this.names = vector.getNames();
        }
        this.dimNames = vector.getDimNames();
        this.dimensions = vector.getDimensions();
        this.setMatrixDimensions(this.dimensions, this.getLength());
        if (vector.getAttributes() != null) {
            this.attributes = vector.getAttributes().copy();
        }
    }

    public final void copyNamesDimsDimNamesFrom(RAbstractVector vector, SourceSection sourceSection) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (this.dimNames == null);
        assert (this.dimensions == null);
        assert (this.attributes == null);
        // for some reason, names is copied first, then dims, then dimnames
        if (vector.getDimensions() == null || vector.getDimensions().length != 1) {
            // only assign name attribute if it's not represented as dimnames (as is the case for
            // one-dimensional arrasy)
            this.setNames(vector.getNames(), sourceSection);
        }
        this.setDimensions(vector.getDimensions(), sourceSection);
        this.setDimNames(vector.getDimNames(), sourceSection);
    }

    public final boolean copyNamesFrom(RAbstractVector vector) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (this.dimNames == null);
        if (this.dimensions == null) {
            if (vector.getNames() != null) {
                this.setNames(vector.getNames());
                return true;
            } else {
                return false;
            }
        } else {
            if (vector.getDimNames() != null) {
                this.setDimNames(vector.getDimNames());
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressFBWarnings(value = "ES_COMPARING_STRINGS_WITH_EQ", justification = "all three string constants below are supposed to be used as identities")
    public final void copyRegAttributesFrom(RAbstractVector vector) {
        RAttributes orgAttributes = vector.getAttributes();
        if (orgAttributes == null) {
            return;
        }
        for (RAttribute e : orgAttributes) {
            String name = e.getName();
            if (name != RRuntime.DIM_ATTR_KEY && name != RRuntime.DIMNAMES_ATTR_KEY && name != RRuntime.NAMES_ATTR_KEY) {
                putAttribute(name, e.getValue());
            }
        }
    }

    public final void resizeWithNames(int size) {
        this.complete &= getLength() >= size;
        resizeInternal(size);
        // reset all atributes other than names;
        setDimNames(null);
        setDimensions(null);
        if (names != null) {
            names.resizeWithEmpty(size);
        }
    }

    @TruffleBoundary
    public final void resetDimensions(int[] newDimensions) {
        // reset all attributes other than dimensions;
        this.dimensions = newDimensions;
        this.setMatrixDimensions(newDimensions, getLength());
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

    public final RAttributes resetAllAttributes(boolean nullify) {
        this.dimensions = null;
        this.matrixDimension = 0;
        this.names = null;
        this.dimNames = null;
        if (nullify) {
            this.attributes = null;
            return null;
        } else {
            if (this.attributes != null) {
                this.attributes.clear();
                return this.attributes;
            } else {
                return null;
            }
        }
    }

    public final boolean isObject() {
        return this.getClassAttr() != null ? true : false;
    }

    public final RStringVector getClassHierarchy() {
        if (isObject()) {
            return (RStringVector) this.attributes.get(RRuntime.CLASS_ATTR_KEY);
        }
        return getImplicitClassHr();
    }

    protected abstract RStringVector getImplicitClassHr();

    // As shape of the vector may change at run-time we need to compute
    // class hierarchy on the fly.
    protected final RStringVector getClassHierarchyHelper(final String[] classHr, final String[] classHrDyn) {
        if (isMatrix()) {
            classHrDyn[0] = RType.Matrix.getName();
            return RDataFactory.createStringVector(classHrDyn, true);
        }
        if (isArray()) {
            classHrDyn[0] = RType.Array.getName();
            return RDataFactory.createStringVector(classHrDyn, true);
        }
        return RDataFactory.createStringVector(classHr, true);
    }

    @Override
    public final RVector materializeNonSharedVector() {
        if (this.isShared()) {
            RVector res = this.copy();
            res.markNonTemporary();
            return res;
        } else {
            return this;
        }
    }

    @Override
    public final RShareable materializeToShareable() {
        return materialize();
    }
}
