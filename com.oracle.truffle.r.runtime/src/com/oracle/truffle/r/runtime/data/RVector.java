/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

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
public abstract class RVector extends RBounded implements RAbstractVector {

    protected boolean complete;
    private int matrixDimension;
    protected int[] dimensions;
    protected Object names;
    protected RList dimNames;
    private HashMap<String, Object> attributes;
    private boolean shared;
    private boolean temporary = true;

    protected RVector(boolean complete, int length, int[] dimensions, Object names) {
        this.complete = complete;
        this.dimensions = dimensions;
        setMatrixDimensions(dimensions, length);
        this.names = names;
        if (names == null && dimensions == null) {
            this.attributes = null;
        } else {
            this.attributes = new LinkedHashMap<>();
            if (names != null) {
                if (names != RNull.instance) {
                    // since this constructor is for internal use only, the assertion shouldn't fail
                    assert ((RStringVector) names).getLength() == length;
                }
                this.attributes.put(RRuntime.NAMES_ATTR_KEY, names);
            }
            if (dimensions != null) {
                this.attributes.put(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(dimensions, true));
            }
        }
        this.dimNames = null;
    }

    protected RVector(boolean complete, int length, int[] dimensions) {
        this(complete, length, dimensions, null);
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

    public final Object getNames() {
        return names == null ? RNull.instance : names;
    }

    public final int getElementIndexByName(String name) {
        if (getNames() == RNull.instance) {
            return -1;
        }
        assert names instanceof RStringVector;
        RStringVector ns = (RStringVector) names;
        for (int i = 0; i < ns.getLength(); ++i) {
            if (ns.getDataAt(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public final void setNames(Object newNames) {
        setNames(newNames, null);
    }

    public void setNames(Object newNames, SourceSection sourceSection) {
        if (attributes == null && newNames != null) {
            attributes = new LinkedHashMap<>();
        }
        if (attributes != null && newNames == null) {
            // whether it's one dimensional array or not, assigning null always removes the "names"
            // attribute
            attributes.remove(RRuntime.NAMES_ATTR_KEY);
            this.names = null;
        } else if (newNames != null && newNames != RNull.instance) {
            if (newNames != RNull.instance && ((RStringVector) newNames).getLength() > this.getLength()) {
                throw RError.getAttributeVectorSameLength(sourceSection, RRuntime.NAMES_ATTR_KEY, ((RStringVector) newNames).getLength(), this.getLength());
            }
            if (this.dimensions != null && dimensions.length == 1) {
                // for one dimensional array, "names" is really "dimnames[[1]]" (see R documentation
                // for "names" function)
                RList newDimNames = RDataFactory.createList(new Object[]{newNames});
                newDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
                attributes.put(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
                this.dimNames = newDimNames;
            } else {
                attributes.put(RRuntime.NAMES_ATTR_KEY, newNames);
                this.names = newNames;
            }
        }
    }

    public final RList getDimNames() {
        return dimNames;
    }

    public void setDimNames(RList newDimNames) {
        setDimNames(newDimNames, null);
    }

    public void setDimNames(RList newDimNames, SourceSection sourceSection) {
        if (attributes == null && newDimNames != null) {
            attributes = new LinkedHashMap<>();
        }
        if (attributes != null && newDimNames == null) {
            attributes.remove(RRuntime.DIMNAMES_ATTR_KEY);
        } else if (newDimNames != null) {
            if (dimensions == null) {
                throw RError.getDimnamesNonarray(sourceSection);
            }
            int newDimNamesLength = newDimNames.getLength();
            if (newDimNamesLength > dimensions.length) {
                throw RError.getDimNamesDontMatchDims(sourceSection, newDimNamesLength, dimensions.length);
            }
            for (int i = 0; i < newDimNamesLength; i++) {
                Object dimObject = newDimNames.getDataAt(i);
                if (dimObject != RNull.instance) {
                    RStringVector dimVector = (RStringVector) dimObject;
                    if (dimVector.getLength() != dimensions[i]) {
                        throw RError.getDimNamesDontMatchExtent(sourceSection, i + 1);
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
            attributes.put(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
            newDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
        }
        this.dimNames = newDimNames;
    }

    public final Map<String, Object> getAttributes() {
        return attributes;
    }

    public final void setAttributes(LinkedHashMap<String, Object> attributes) {
        this.attributes = attributes;
    }

    public final boolean isComplete() {
        return complete;
    }

    public final void markNonTemporary() {
        temporary = false;
    }

    public final boolean isTemporary() {
        return temporary;
    }

    public final boolean isShared() {
        return shared;
    }

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
        if (hasDimensions()) {
            return Arrays.copyOf(dimensions, dimensions.length);
        } else {
            return null;
        }
    }

    public final void setDimensions(int[] newDimensions) {
        setDimensions(newDimensions, null);
    }

    public final void setDimensions(int[] newDimensions, SourceSection sourceSection) {
        if (attributes == null && newDimensions != null) {
            attributes = new LinkedHashMap<>();
        }
        if (attributes != null && newDimensions == null) {
            attributes.remove(RRuntime.DIM_ATTR_KEY);
            setDimNames(null, sourceSection);
        } else if (newDimensions != null) {
            verifyDimensions(newDimensions, sourceSection);
            setMatrixDimensions(newDimensions, getLength());
            attributes.put(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(newDimensions, true));
        }
        this.dimensions = newDimensions;
    }

    public final RVector copy() {
        RVector result = internalCopy();
        result.names = this.names;
        result.dimNames = this.dimNames;
        result.dimensions = this.dimensions;
        result.setMatrixDimensions(result.dimensions, result.getLength());
        if (this.getAttributes() != null) {
            result.setAttributes(new LinkedHashMap<>(this.getAttributes()));
        }
        return result;
    }

    public final boolean verify() {
        return internalVerify();
    }

    public abstract RVector copyResized(int size, boolean fillNA);

    protected abstract void resizeInternal(int size);

    protected abstract String getDataAtAsString(int index);

    protected abstract RVector internalCopy();

    protected abstract boolean internalVerify();

    public final RStringVector toStringVector() {
        String[] values = new String[getLength()];
        for (int i = 0; i < getLength(); ++i) {
            values[i] = this.getDataAtAsString(i);
        }
        return RDataFactory.createStringVector(values, this.isComplete());
    }

    public abstract RVector createEmptySameType(int newLength, boolean newIsComplete);

    public abstract void transferElementSameType(int toIndex, RVector fromVector, int fromIndex);

    public boolean isInBounds(int firstPosition, int secondPosition) {
        assert isMatrix();
        return firstPosition >= 1 && firstPosition <= matrixDimension && (convertToIndex(secondPosition) * matrixDimension + firstPosition) <= getLength();
    }

    public int convertToIndex(int position) {
        return position - 1;
    }

    public int convertToIndex(int firstPosition, int secondPosition) {
        assert isMatrix();
        assert isInBounds(firstPosition, secondPosition);
        return convertToIndex(firstPosition) + convertToIndex(secondPosition) * matrixDimension;
    }

    public void copyAttributesFrom(RAbstractVector vector) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (this.dimNames == null);
        assert (this.dimensions == null);
        assert (this.attributes == null);
        this.names = vector.getNames();
        this.dimNames = vector.getDimNames();
        this.dimensions = vector.getDimensions();
        this.setMatrixDimensions(this.dimensions, this.getLength());
        this.setAttributes(new LinkedHashMap<>(vector.getAttributes()));
    }

    public void copyNamesDimsDimNamesFrom(RAbstractVector vector, SourceSection sourceSection) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (this.dimNames == null);
        assert (this.dimensions == null);
        assert (this.attributes == null);
        // for some reason, names is copied first, then dims, then dimnames
        this.setNames(vector.getNames(), sourceSection);
        this.setDimensions(vector.getDimensions(), sourceSection);
        this.setDimNames(vector.getDimNames(), sourceSection);
    }

    public void copyNamesFrom(RAbstractVector vector) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (this.dimNames == null);
        if (this.dimensions == null) {
            this.setNames(vector.getNames());
        } else {
            this.setDimNames(vector.getDimNames());
        }
    }

    public void resizeWithNames(int size) {
        this.complete = this.complete || this.getLength() <= size;
        resizeInternal(size);
        // reset all atributes other than names;
        this.setDimNames(null);
        this.setDimensions(null);
        this.matrixDimension = 0;
        if (this.names != null && this.names != RNull.instance) {
            ((RStringVector) this.names).resizeWithEmpty(size);
        }
    }

    public void resetDimensions(int[] newDimensions) {
        // reset all atributes other than dimensions;
        this.dimensions = newDimensions;
        this.setMatrixDimensions(newDimensions, getLength());
        // whether we nullify dimensions or re-set them to a different value, names and dimNames
        // must be reset
        this.names = null;
        this.dimNames = null;
        if (this.dimensions != null) {
            if (this.attributes != null) {
                this.attributes.clear();
            } else {
                this.attributes = new LinkedHashMap<>();
            }
            this.attributes.put(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(this.dimensions, true));
        } else {
            // nullifying dimensions does not reset regular attributes
            if (this.attributes != null) {
                this.attributes.remove(RRuntime.DIM_ATTR_KEY);
                this.attributes.remove(RRuntime.DIMNAMES_ATTR_KEY);
                this.attributes.remove(RRuntime.NAMES_ATTR_KEY);
            }
        }
    }

    public HashMap<String, Object> resetAllAttributes(boolean nullify) {
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

    public boolean isObject() {
        return (this.attributes != null && this.attributes.get(RRuntime.CLASS_ATTR_KEY) != null) ? true : false;
    }

    public RStringVector getClassHierarchy() {
        if (isObject()) {
            return (RStringVector) this.attributes.get(RRuntime.CLASS_ATTR_KEY);
        }
        return getImplicitClassHr();
    }

    protected RStringVector getImplicitClassHr() {
        return null;
    }

    // As shape of the vector may change at run-time we need to compute
    // class hierarchy on the fly.
    protected RStringVector getClassHierarchyHelper(final String[] classHr, final String[] classHrDyn) {
        if (isMatrix()) {
            classHrDyn[0] = RRuntime.TYPE_MATRIX;
            return RDataFactory.createStringVector(classHrDyn, true);
        }
        if (isArray()) {
            classHrDyn[0] = RRuntime.TYPE_ARRAY;
            return RDataFactory.createStringVector(classHrDyn, true);
        }
        return RDataFactory.createStringVector(classHr, true);
    }
}
