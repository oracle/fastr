/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RError.NO_CALLER;

import java.util.function.Function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorToArray;
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
public abstract class RVector<ArrayT> extends RSharingAttributeStorage implements RAbstractVector, RFFIAccess {

    private static final RStringVector implicitClassHeaderArray = RDataFactory.createStringVector(new String[]{RType.Array.getName()}, true);
    private static final RStringVector implicitClassHeaderMatrix = RDataFactory.createStringVector(new String[]{RType.Matrix.getName()}, true);

    protected boolean complete; // "complete" means: does not contain NAs

    protected RVector(boolean complete) {
        this.complete = complete;
    }

    private int[] getDimensionsFromAttrs() {
        if (attributes == null) {
            return null;
        } else {
            RIntVector dims = (RIntVector) attributes.get(RRuntime.DIM_ATTR_KEY);
            return dims == null ? null : dims.getInternalStore();
        }
    }

    private RStringVector getNamesFromAttrs() {
        if (attributes == null) {
            return null;
        } else {
            return (RStringVector) attributes.get(RRuntime.NAMES_ATTR_KEY);
        }
    }

    /**
     * Returns the internal data Java array for read only purposes only or {@code null} if the
     * vector has been materialized to native mirror and it does not hold managed data anymore. This
     * method is for only very specific purposes especially of {@link VectorToArray}.
     *
     * @return vector data
     */
    public abstract ArrayT getInternalManagedData();

    /**
     * Intended for external calls where a mutable copy is needed.
     */
    public abstract ArrayT getDataCopy();

    /**
     * Returns data for read-only purposes. The result may or may not be copy of the internal data.
     * This is a slow path operation for vector types that may have a native mirror, use
     * {@link VectorToArray} node for fast path in such cases.
     *
     * @see VectorToArray
     * @see RObject#getNativeMirror()
     * @return vector data
     */
    public abstract ArrayT getReadonlyData();

    /**
     * Return vector data (copying if necessary) that's guaranteed to be either temporary in terms
     * of vector sharing mode, or owned by only one location (non-shared). It is not safe to re-use
     * the array returned to create a new vector.
     *
     * @return vector data
     */
    public final ArrayT getDataNonShared() {
        return !isShared() ? getReadonlyData() : getDataCopy();
    }

    /**
     * Return vector data (copying if necessary) that's guaranteed to be "fresh" (temporary in terms
     * of vector sharing mode). As long as the vector is not retuned or put into a list/environment
     * (i.e. if it is temporary, it will stay temporary), it is safe to reuse the array retuned by
     * this method to create a new vector.
     *
     * @return vector data
     */
    public final ArrayT getDataTemp() {
        return isTemporary() ? getReadonlyData() : getDataCopy();
    }

    @Override
    public final void setComplete(boolean complete) {
        this.complete = complete;
        assert verify();
    }

    private void removeAttributeMapping(String key) {
        if (this.attributes != null) {
            this.attributes.delete(key);
            if (this.attributes.size() == 0) {
                this.attributes = null;
            }
        }
    }

    /*
     * Version without profiles is used by RDeparse and for internal attribute copying (both are not
     * performance-critical)
     */
    @Override
    public final RStringVector getNames() {
        RStringVector names = getNamesFromAttrs();
        if (names == null) {
            RList dimNames = getDimNames();
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
    public final int getElementIndexByName(String name) {
        if (getNames() == null) {
            return -1;
        }
        RStringVector names = getNamesFromAttrs();
        for (int i = 0; i < names.getLength(); i++) {
            if (names.getDataAt(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Guarded method that checks whether {@code attributes} is initialized.
     */
    private void putAttribute(String attribute, Object value) {
        initAttributes().define(attribute, value);
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
            attributes.define(name, value);
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
            attributes.delete(name);
            // nullify only here because other methods invoke removeAttributeMapping which does
            // it already
            if (attributes.size() == 0) {
                attributes = null;
            }
        }
    }

    @Override
    public final void removeAttr(String name) {
        if (attributes != null) {
            removeAttrInternal(name);
        }
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
        } else if (newNames != null) {
            if (newNames.getLength() > this.getLength()) {
                throw RError.error(invokingNode, RError.Message.ATTRIBUTE_VECTOR_SAME_LENGTH, RRuntime.NAMES_ATTR_KEY, newNames.getLength(), this.getLength());
            }
            int[] dimensions = getDimensionsFromAttrs();
            if (dimensions != null && dimensions.length == 1) {
                // for one dimensional array, "names" is really "dimnames[[1]]" (see R documentation
                // for "names" function)
                RList newDimNames = RDataFactory.createList(new Object[]{newNames});
                newDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
                putAttribute(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
            } else {
                putAttribute(RRuntime.NAMES_ATTR_KEY, newNames);
                assert newNames != this;
            }
        }
    }

    @Override
    public final RList getDimNames() {
        if (attributes == null) {
            return null;
        } else {
            return (RList) attributes.get(RRuntime.DIMNAMES_ATTR_KEY);
        }
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
        } else if (newDimNames != null) {
            int[] dimensions = getDimensionsFromAttrs();
            if (dimensions == null) {
                throw invokingNode.error(RError.Message.DIMNAMES_NONARRAY);
            }
            int newDimNamesLength = newDimNames.getLength();
            if (newDimNamesLength > dimensions.length) {
                throw invokingNode.error(RError.Message.DIMNAMES_DONT_MATCH_DIMS, newDimNamesLength, dimensions.length);
            }
            for (int i = 0; i < newDimNamesLength; i++) {
                Object dimObject = newDimNames.getDataAt(i);
                if (dimObject != RNull.instance) {
                    if (dimObject instanceof String) {
                        if (dimensions[i] != 1) {
                            CompilerDirectives.transferToInterpreter();
                            throw invokingNode.error(RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                        }
                    } else {
                        RAbstractStringVector dimVector = (RAbstractStringVector) dimObject;
                        if (dimVector == null) {
                            newDimNames.updateDataAt(i, RNull.instance, null);
                        } else if (dimVector.getLength() != dimensions[i]) {
                            CompilerDirectives.transferToInterpreter();
                            throw invokingNode.error(RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                        }
                    }
                }
            }

            RList resDimNames = newDimNames;
            if (newDimNamesLength < dimensions.length) {
                // resize the array and fill the missing entries with NULL-s
                resDimNames = (RList) resDimNames.copyResized(dimensions.length, true);
                resDimNames.setAttributes(newDimNames);
                for (int i = newDimNamesLength; i < dimensions.length; i++) {
                    resDimNames.updateDataAt(i, RNull.instance, null);
                }
            }
            putAttribute(RRuntime.DIMNAMES_ATTR_KEY, resDimNames);
            resDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
        }
    }

    @Override
    public final Object getRowNames() {
        if (attributes == null) {
            return RNull.instance;
        } else {
            return attributes.get(RRuntime.ROWNAMES_ATTR_KEY);
        }
    }

    @Override
    public final void setRowNames(RAbstractVector newRowNames) {
        if (newRowNames == null) {
            removeAttributeMapping(RRuntime.ROWNAMES_ATTR_KEY);
        } else {
            putAttribute(RRuntime.ROWNAMES_ATTR_KEY, newRowNames);
        }
    }

    @Override
    public final boolean isComplete() {
        return complete;
    }

    @Override
    public final boolean hasDimensions() {
        return attributes == null ? false : attributes.containsKey(RRuntime.DIM_ATTR_KEY);
    }

    private boolean hasDimNames() {
        return attributes == null ? false : attributes.containsKey(RRuntime.DIMNAMES_ATTR_KEY);
    }

    private boolean hasRowNames() {
        return attributes == null ? false : attributes.containsKey(RRuntime.ROWNAMES_ATTR_KEY);
    }

    private boolean hasNames() {
        return attributes == null ? false : attributes.containsKey(RRuntime.NAMES_ATTR_KEY);
    }

    @Override
    public final boolean isMatrix() {
        int[] dimensions = getDimensionsFromAttrs();
        return dimensions != null && dimensions.length == 2;
    }

    @Override
    public final boolean isArray() {
        int[] dimensions = getDimensionsFromAttrs();
        return dimensions != null && dimensions.length > 0;
    }

    @Override
    public final int[] getDimensions() {
        return getDimensionsFromAttrs();
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
    }

    @Override
    public final void setDimensions(int[] newDimensions) {
        setDimensions(newDimensions, NO_CALLER);
    }

    private void setDimensions(int[] newDimensions, RBaseNode invokingNode) {
        if (attributes != null && newDimensions == null) {
            removeAttributeMapping(RRuntime.DIM_ATTR_KEY);
            setDimNames(null, invokingNode);
        } else if (newDimensions != null) {
            verifyDimensions(getLength(), newDimensions, invokingNode);
            putAttribute(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(newDimensions, RDataFactory.COMPLETE_VECTOR));
        }
    }

    @Override
    public RAbstractContainer setClassAttr(RStringVector classAttr) {
        CompilerAsserts.neverPartOfCompilation();
        return setClassAttrInternal(this, classAttr);
    }

    public static RAbstractContainer setVectorClassAttr(RVector<?> vector, RStringVector classAttr) {
        return setClassAttrInternal(vector, classAttr);
    }

    private static RAbstractContainer setClassAttrInternal(RVector<?> vector, RStringVector classAttr) {
        if (vector.attributes == null && classAttr != null && classAttr.getLength() != 0) {
            vector.initAttributes();
        }
        if (vector.attributes != null && (classAttr == null || classAttr.getLength() == 0)) {
            vector.removeAttributeMapping(RRuntime.CLASS_ATTR_KEY);
        } else if (classAttr != null && classAttr.getLength() != 0) {
            for (int i = 0; i < classAttr.getLength(); i++) {
                String attr = classAttr.getDataAt(i);
                if (RRuntime.CLASS_FACTOR.equals(attr)) {
                    if (!(vector instanceof RAbstractIntVector)) {
                        throw RError.error(RError.SHOW_CALLER2, RError.Message.ADDING_INVALID_CLASS, "factor");
                    }
                }
            }
            vector.putAttribute(RRuntime.CLASS_ATTR_KEY, classAttr);
        }

        return vector;
    }

    public final void setAttributes(RVector<?> result) {
        if (this.attributes != null) {
            result.initAttributes(RAttributesLayout.copy(this.attributes));
        }
    }

    // public interface *copy* methods are final and delegate to *internalCopyAndReport* methods

    @Override
    public final RVector<ArrayT> copy() {
        RVector<ArrayT> result = internalCopyAndReport();
        setAttributes(result);
        result.setTypedValueInfo(getTypedValueInfo());
        return result;
    }

    @Override
    public final RVector<ArrayT> copyDropAttributes() {
        return internalCopyAndReport();
    }

    @Override
    public final RVector<ArrayT> deepCopy() {
        RVector<ArrayT> result = internalDeepCopyAndReport();
        setAttributes(result);
        return result;
    }

    @Override
    public final RVector<ArrayT> copyResized(int size, boolean fillNA) {
        RVector<ArrayT> result = internalCopyResized(size, fillNA, null);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public final RVector<ArrayT> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
        // TODO support for higher dimensions
        assert newDimensions.length == 2;
        RVector<ArrayT> result = internalCopyResized(newDimensions[0] * newDimensions[1], fillNA, newDimensions);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    // *internalCopyAndReport* methods do just the copy and report it to MemoryTracer. These should
    // be used if additional logic in public interface *copy* method is not desired.

    private RVector<ArrayT> internalCopyAndReport() {
        RVector<ArrayT> result = internalCopy();
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    private RVector<ArrayT> internalDeepCopyAndReport() {
        RVector<ArrayT> result = internalDeepCopy();
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    // *internalCopy* methods should only be overridden, but never invoked from anywhere but
    // *internalCopyAndReport*

    protected abstract RVector<ArrayT> internalCopyResized(int size, boolean fillNA, int[] dimensions);

    // to be overridden by recursive structures
    protected RVector<ArrayT> internalDeepCopy() {
        return internalCopy();
    }

    protected abstract RVector<ArrayT> internalCopy();

    public abstract boolean verify();

    /**
     * Update a data item in the vector. Possibly not as efficient as type-specific methods, but in
     * some cases it likely does not matter (e.g. if used alongside I/O operations).
     *
     * @param i index of the vector item to be updated
     * @param o updated value
     * @param naCheck NA check used to change vector's mode in case value is NA
     * @return updated vector
     */
    public abstract RVector<ArrayT> updateDataAtAsObject(int i, Object o, NACheck naCheck);

    public abstract void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex);

    public final RAttributable copyAttributesFrom(RAbstractContainer vector) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (!hasNames());
        assert (!hasDimNames());
        assert (!hasRowNames());
        assert (!hasDimensions());
        assert (this.attributes == null || this.attributes.size() == 0) : this.attributes.size();
        DynamicObject vecAttributes = vector.getAttributes();
        if (vecAttributes != null) {
            initAttributes(RAttributesLayout.copy(vecAttributes));
            return copyClassAttr(vecAttributes);
        } else {
            return this;
        }
    }

    @TruffleBoundary
    private RAbstractContainer copyClassAttr(DynamicObject vecAttributes) {
        return setClassAttr((RStringVector) vecAttributes.get(RRuntime.CLASS_ATTR_KEY));
    }

    /*
     * Internal version without profiles used in a rare (and already slow) case of double-to-int
     * vector conversion when setting class attribute
     */
    private RAttributable copyAttributesFromVector(RVector<?> vector) {
        DynamicObject vecAttributes = vector.getAttributes();
        if (vecAttributes != null) {
            initAttributes(RAttributesLayout.copy(vecAttributes));
            return copyClassAttr(vecAttributes);
        } else {
            return this;
        }
    }

    public final void copyNamesDimsDimNamesFrom(RAbstractVector vector, RBaseNode invokingNode) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (!hasDimNames());
        assert (!hasDimNames());
        assert (!hasDimensions());
        assert (this.attributes == null);
        // for some reason, names is copied first, then dims, then dimnames
        if (vector.getDimensions() == null || vector.getDimensions().length != 1) {
            // only assign name attribute if it's not represented as dimnames (as is the case for
            // one-dimensional array)
            this.setNames(vector.getNames(), invokingNode);
        }
        this.setDimensions(vector.getDimensions(), invokingNode);
        this.setDimNames(vector.getDimNames(), invokingNode);
    }

    /**
     * Inits dims, names and dimnames attributes and it should only be invoked if no attributes were
     * initialized yet.
     */
    @TruffleBoundary
    protected final void initDimsNamesDimNames(int[] dimensions, RStringVector names, RList dimNames) {
        assert (this.attributes == null) : "Vector attributes must be null";
        assert names != this;
        assert dimNames != this;
        if (dimNames != null) {
            DynamicObject attrs;
            if (dimensions != null) {
                RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                attrs = RAttributesLayout.createDimAndDimNames(dimensionsVector, dimNames);
                // one-dimensional arrays do not have names, only dimnames with one value so do not
                // init names in that case
                if (names != null && dimensions.length != 1) {
                    assert names.getLength() == getLength() : "size mismatch: names.length=" + names.getLength() + " vs. length=" + getLength();
                    attrs.define(RRuntime.NAMES_ATTR_KEY, names);
                }
            } else {
                attrs = RAttributesLayout.createDimNames(dimNames);
                if (names != null) {
                    assert names.getLength() == getLength() : "size mismatch: names.length=" + names.getLength() + " vs. length=" + getLength();
                    attrs.define(RRuntime.NAMES_ATTR_KEY, names);
                }
            }
            initAttributes(attrs);
        } else {
            if (names != null) {
                // since this constructor is for internal use only, the assertion shouldn't fail
                assert names.getLength() == getLength() : "size mismatch: " + names.getLength() + " vs. " + getLength();
                if (dimensions != null) {
                    RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                    if (dimensions.length != 1) {
                        initAttributes(RAttributesLayout.createNamesAndDim(names, dimensionsVector));
                    } else {
                        // one-dimensional arrays do not have names, only dimnames with one value
                        RList newDimNames = RDataFactory.createList(new Object[]{names});
                        initAttributes(RAttributesLayout.createDimAndDimNames(dimensionsVector, newDimNames));
                    }
                } else {
                    initAttributes(RAttributesLayout.createNames(names));
                }
            } else {
                if (dimensions != null) {
                    RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                    initAttributes(RAttributesLayout.createDim(dimensionsVector));
                }
            }
        }
    }

    public final boolean copyNamesFrom(RAbstractVector vector) {
        CompilerAsserts.neverPartOfCompilation();

        int[] dimensions = getDimensionsFromAttrs();
        if (dimensions == null) {
            RStringVector vecNames = vector.getNames();
            if (vecNames != null) {
                this.setNames(vecNames);
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
    public final RVector<ArrayT> copyRegAttributesFrom(RAbstractContainer vector) {
        DynamicObject orgAttributes = vector.getAttributes();
        if (orgAttributes != null) {
            for (RAttributesLayout.RAttribute e : RAttributesLayout.asIterable(orgAttributes)) {
                String name = e.getName();
                if (name != RRuntime.DIM_ATTR_KEY && name != RRuntime.DIMNAMES_ATTR_KEY && name != RRuntime.NAMES_ATTR_KEY) {
                    Object val = e.getValue();
                    putAttribute(name, val);
                }
            }
        }
        return this;
    }

    @Override
    public final RVector<ArrayT> resize(int size) {
        return resize(size, true);
    }

    private RVector<ArrayT> resize(int size, boolean resetAll) {
        this.complete &= getLength() >= size;
        RVector<ArrayT> res = this;
        RStringVector oldNames = res.getNamesFromAttrs();
        res = copyResized(size, true);
        if (this.isShared()) {
            assert res.isTemporary();
            res.incRefCount();
        }
        if (resetAll) {
            resetAllAttributes(oldNames == null);
        } else {
            res.copyAttributesFromVector(this);
            res.setDimensionsNoCheck(null);
            res.setDimNamesNoCheck(null);
        }
        if (oldNames != null) {
            oldNames = oldNames.resizeWithEmpty(size);
            res.putAttribute(RRuntime.NAMES_ATTR_KEY, oldNames);
        }
        return res;
    }

    @TruffleBoundary
    public final void resetDimensions(int[] newDimensions) {
        // reset all attributes other than dimensions;
        // whether we nullify dimensions or re-set them to a different value, names and dimNames
        // must be reset
        if (newDimensions != null) {
            putAttribute(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(newDimensions, true));
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
    public final void resetAllAttributes(boolean nullify) {
        if (nullify) {
            this.attributes = null;
        } else {
            if (this.attributes != null) {
                RAttributesLayout.clear(this.attributes);
            }
        }
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

    public static void verifyDimensions(int vectorLength, int[] newDimensions, RBaseNode invokingNode) {
        int length = 1;
        for (int i = 0; i < newDimensions.length; i++) {
            if (RRuntime.isNA(newDimensions[i])) {
                throw invokingNode.error(RError.Message.DIMS_CONTAIN_NA);
            } else if (newDimensions[i] < 0) {
                throw invokingNode.error(RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
            }
            length *= newDimensions[i];
        }
        if (length != vectorLength && vectorLength > 0) {
            throw invokingNode.error(RError.Message.DIMS_DONT_MATCH_LENGTH, length, vectorLength);
        }
    }

    private static final int MAX_TOSTRING_LENGTH = 100;

    protected final String toString(Function<Integer, String> element) {
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
