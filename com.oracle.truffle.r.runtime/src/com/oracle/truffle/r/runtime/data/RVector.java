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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;
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
public abstract class RVector<ArrayT> extends RSharingAttributeStorage implements RAbstractVector, RFFIAccess {

    private static final RStringVector implicitClassHeaderArray = RDataFactory.createStringVector(new String[]{RType.Array.getName()}, true);
    private static final RStringVector implicitClassHeaderMatrix = RDataFactory.createStringVector(new String[]{RType.Matrix.getName()}, true);

    protected boolean complete; // "complete" means: does not contain NAs
    protected RStringVector names;
    // cache rownames for data frames as they are accessed at every data frame access
    private Object rowNames;

    protected RVector(boolean complete, int length, int[] dimensions, RStringVector names) {
        this.complete = complete;
        assert names != this;
        this.names = names;
        this.rowNames = RNull.instance;
        if (names != null) {
            // since this constructor is for internal use only, the assertion shouldn't fail
            assert names.getLength() == length : "size mismatch: " + names.getLength() + " vs. " + length;
            if (dimensions == null) {
                initAttributes(RAttributesLayout.createNames(names));
            } else {
                RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                if (dimensions.length != 1) {
                    initAttributes(RAttributesLayout.createNamesAndDim(names, dimensionsVector));
                } else {
                    // one-dimensional arrays do not have names, only dimnames with one value
                    RList newDimNames = RDataFactory.createList(new Object[]{names});
                    initAttributes(RAttributesLayout.createDimAndDimNames(dimensionsVector, newDimNames));
                }
            }
        } else {
            if (dimensions != null) {
                initAttributes(RAttributesLayout.createDim(RDataFactory.createIntVector(dimensions, true)));
            }
        }
    }

    private int[] getDimensionsFromAttrs() {
        if (attributes == null) {
            return null;
        } else {
            RIntVector dims = (RIntVector) attributes.get(RRuntime.DIM_ATTR_KEY);
            return dims == null ? null : dims.getInternalStore();
        }
    }

    private RList getDimNamesFromAttrs() {
        if (attributes == null) {
            return null;
        } else {
            return (RList) attributes.get(RRuntime.DIMNAMES_ATTR_KEY);
        }
    }

    /**
     * Intended for external calls where a mutable copy is needed.
     */
    public abstract ArrayT getDataCopy();

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    public abstract ArrayT getDataWithoutCopying();

    /**
     * Return vector data (copying if necessary) that's guaranteed not to be shared with any other
     * vector instance (but maybe non-temporary in terms of vector's sharing mode).
     *
     * @return vector data
     */
    public final ArrayT getDataNonShared() {
        return isShared() ? getDataCopy() : getDataWithoutCopying();
    }

    /**
     * Return vector data (copying if necessary) that's guaranteed to be "fresh" (temporary in terms
     * of vector sharing mode).
     *
     * @return vector data
     */
    public final ArrayT getDataTemp() {
        return isTemporary() ? getDataWithoutCopying() : getDataCopy();
    }

    public final int[] getInternalDimensions() {
        return getDimensionsFromAttrs();
    }

    public final RStringVector getInternalNames() {
        return names;
    }

    public final void setInternalNames(RStringVector newNames) {
        assert newNames != this;
        names = newNames;
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
            this.attributes.delete(key);
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
    }

    /*
     * Version without profiles is used by RDeparse and for internal attribute copying (both are not
     * performance-critical)
     */
    public final RStringVector getNames() {
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
            attributes.delete(name);
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
                this.names = newNames;
            }
        }
    }

    @Override
    public final RList getDimNames(RAttributeProfiles attrProfiles) {
        return getDimNames();
    }

    public final RList getDimNames() {
        return getDimNamesFromAttrs();
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
                        if (dimVector == null) {
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
        return attributes == null ? false : attributes.containsKey(RRuntime.DIM_ATTR_KEY);
    }

    public final boolean hasDimNames() {
        return attributes == null ? false : attributes.containsKey(RRuntime.DIMNAMES_ATTR_KEY);
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
    }

    @Override
    public RAbstractContainer setClassAttr(RStringVector classAttr) {
        return setClassAttrInternal(this, classAttr);
    }

    public static RAbstractContainer setVectorClassAttr(RVector<?> vector, RStringVector classAttr) {
        return setClassAttrInternal(vector, classAttr);
    }

    public abstract class CNode extends RBaseNode {

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
                    vector.putAttribute(RRuntime.CLASS_ATTR_KEY, classAttr);
                    if (vector.getElementClass() != RInteger.class) {
                        // N.B. there used to be conversion to integer under certain circumstances.
                        // However, it seems that it was dead/obsolete code so it was removed.
                        // Notes: this can only happen if the class is set by hand to some
                        // non-integral vector, i.e. attr(doubles, 'class') <- 'factor'. GnuR also
                        // does not update the 'class' attr with other, possibly
                        // valid classes when it reaches this error.
                        throw RError.error(RError.SHOW_CALLER2, RError.Message.ADDING_INVALID_CLASS, "factor");
                    }
                }
            }
            vector.putAttribute(RRuntime.CLASS_ATTR_KEY, classAttr);
        }

        return vector;
    }

    public final void setAttributes(RVector<?> result) {
        result.names = this.names;
        result.rowNames = this.rowNames;
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
        return internalCopyResizedAndReport(size, fillNA);
    }

    // *internalCopyAndReport* methods do just the copy and report it to MemoryTracer. These should
    // be used if additional logic in public interface *copy* method is not desired.

    protected final RVector<ArrayT> internalCopyAndReport() {
        RVector<ArrayT> result = internalCopy();
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    protected final RVector<ArrayT> internalDeepCopyAndReport() {
        RVector<ArrayT> result = internalDeepCopy();
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    protected final RVector<ArrayT> internalCopyResizedAndReport(int size, boolean fillNA) {
        RVector<ArrayT> result = internalCopyResized(size, fillNA);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    // *internalCopy* methods should only be overridden, but never invoked from anywhere but
    // *internalCopyAndReport*

    protected abstract RVector<ArrayT> internalCopyResized(int size, boolean fillNA);

    // to be overridden by recursive structures
    protected RVector<ArrayT> internalDeepCopy() {
        return internalCopy();
    }

    protected abstract RVector<ArrayT> internalCopy();

    @Override
    public RVector<ArrayT> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
        // TODO support for higher dimensions
        assert newDimensions.length == 2;
        RVector<ArrayT> result = copyResized(newDimensions[0] * newDimensions[1], fillNA);
        result.setDimensions(newDimensions);
        return result;
    }

    public final boolean verify() {
        return internalVerify();
    }

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
    public abstract RVector<ArrayT> updateDataAtAsObject(int i, Object o, NACheck naCheck);

    public abstract void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex);

    public final RAttributable copyAttributesFrom(RAttributeProfiles attrProfiles, RAbstractContainer vector) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (!hasDimNames());
        assert (this.rowNames == RNull.instance);
        assert (!hasDimensions());
        assert (this.attributes == null || this.attributes.size() == 0) : this.attributes.size();
        if (vector.getDimensions() == null || vector.getDimensions().length != 1) {
            // only assign name attribute if it's not represented as dimnames (as is the case for
            // one-dimensional array)
            this.names = vector.getNames(attrProfiles);
        }
        this.rowNames = vector.getRowNames(attrProfiles);
        DynamicObject vecAttributes = vector.getAttributes();
        if (vecAttributes != null) {
            initAttributes(RAttributesLayout.copy(vecAttributes));
            return this.setClassAttr((RStringVector) vecAttributes.get(RRuntime.CLASS_ATTR_KEY));
        } else {
            return this;
        }
    }

    /*
     * Internal version without profiles used in a rare (and already slow) case of double-to-int
     * vector conversion when setting class attribute
     */
    protected final RAttributable copyAttributesFrom(RVector<?> vector) {
        if (vector.getDimensions() == null || vector.getDimensions().length != 1) {
            this.names = vector.getNames();
        }
        this.rowNames = vector.getRowNames();
        DynamicObject vecAttributes = vector.getAttributes();
        if (vecAttributes != null) {
            initAttributes(RAttributesLayout.copy(vecAttributes));
            return this.setClassAttr((RStringVector) vecAttributes.get(RRuntime.CLASS_ATTR_KEY));
        } else {
            return this;
        }
    }

    public final void copyNamesDimsDimNamesFrom(RAttributeProfiles attrProfiles, RAbstractVector vector, RBaseNode invokingNode) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (this.names == null);
        assert (!hasDimNames());
        assert (!hasDimensions());
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
        int[] dimensions = getDimensionsFromAttrs();
        if (dimensions == null) {
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
    public final RVector<ArrayT> copyRegAttributesFrom(RAbstractContainer vector) {
        DynamicObject orgAttributes = vector.getAttributes();
        if (orgAttributes != null) {
            Object newRowNames = null;
            for (RAttributesLayout.RAttribute e : RAttributesLayout.asIterable(orgAttributes)) {
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
    public final RVector<ArrayT> resize(int size) {
        return resize(size, true);
    }

    private RVector<ArrayT> resize(int size, boolean resetAll) {
        this.complete &= getLength() >= size;
        RVector<ArrayT> res = this;
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
        // whether we nullify dimensions or re-set them to a different value, names and dimNames
        // must be reset
        this.names = null;
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
        this.names = null;
        this.rowNames = RNull.instance;
        if (nullify) {
            this.attributes = null;
        } else {
            if (this.attributes != null) {
                RAttributesLayout.clear(this.attributes);
            }
        }
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
