/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import static com.oracle.truffle.r.runtime.RError.NO_CALLER;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFFIAccess;
import com.oracle.truffle.r.runtime.data.RIntVecClosureData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSeq;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.UpdateShareableChildValue;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * When implementing, make sure to invoke related {@link MemoryCopyTracer} methods.
 */
@ExportLibrary(InteropLibrary.class)
public abstract class RAbstractVector extends RAbstractContainer implements RFFIAccess {

    /**
     * Dummy volatile field that can be used to create memory barrier.
     */
    protected static volatile int fence;

    protected boolean complete; // "complete" means: does not contain NAs

    protected RAbstractVector(boolean complete) {
        this.complete = complete;
    }

    public boolean isSequence() {
        return this instanceof RSequence;
    }

    public RSeq getSequence() { return (RSeq) this; }

    public boolean isClosure() {
        return this instanceof RClosure;
    }

    public RClosure getClosure() {
        return (RClosure) this;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public long getArraySize() {
        return getLength();
    }

    @ExportMessage
    public boolean isArrayElementReadable(long index) {
        return index >= 0 && index < getLength();
    }

    @ExportMessage
    public Object readArrayElement(long index,
                    @Cached.Exclusive @Cached() R2Foreign r2Foreign,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile invalidIndex) throws InvalidArrayIndexException {
        if (!invalidIndex.profile(isArrayElementReadable(index))) {
            throw InvalidArrayIndexException.create(index);
        }
        Object value = getDataAtAsObject((int) index);
        return boxReadElements() ? r2Foreign.convert(value) : r2Foreign.convertNoBox(value);
    }

    protected abstract boolean boxReadElements();

    private int[] getDimensionsFromAttrs() {
        if (attributes == null) {
            return null;
        } else {
            com.oracle.truffle.r.runtime.data.RIntVector dims = (com.oracle.truffle.r.runtime.data.RIntVector) attributes.get(RRuntime.DIM_ATTR_KEY);
            return dims == null ? null : dims.getReadonlyData();
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
     * method is for only very specific purposes especially of {@link GetReadonlyData}.
     *
     * @return vector data
     */
    public abstract Object getInternalManagedData();

    public final boolean hasNativeMemoryData() {
        return getInternalManagedData() == null;
    }

    @Override
    public Object getInternalStore() {
        return this;
    }

    /**
     * Intended for external calls where a mutable copy is needed.
     */
    public abstract Object getDataCopy();

    /**
     * Returns data for read-only purposes. The result may or may not be copy of the internal data.
     * This is a slow path operation for vector types that may have a native mirror, use
     * {@link GetReadonlyData} node for fast path in such cases.
     *
     * @see GetReadonlyData
     * @see RBaseObject#getNativeMirror()
     * @return vector data
     */
    public abstract Object getReadonlyData();

    /**
     * Return vector data (copying if necessary) that's guaranteed to be either temporary in terms
     * of vector sharing mode, or owned by only one location (non-shared). It is not safe to re-use
     * the array returned to create a new vector.
     *
     * @return vector data
     */
    public final Object getDataNonShared() {
        return !isShared() ? getReadonlyData() : getDataCopy();
    }

    /**
     * Return vector data (copying if necessary) that's guaranteed to be "fresh" (temporary in terms
     * of vector sharing mode). As long as the vector is not returned or put into a list/environment
     * (i.e. if it is temporary, it will stay temporary), it is safe to reuse the array returned by
     * this method to create a new vector.
     *
     * @return vector data
     */
    public Object getDataTemp() {
        return isTemporary() ? getReadonlyData() : getDataCopy();
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
        assert RAbstractVector.verifyVector(this);
    }

    /*
     * Version without profiles is used by RDeparse and for internal attribute copying (both are not
     * performance-critical)
     */
    @Override
    public RStringVector getNames() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return null;
        }
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

    @CompilerDirectives.TruffleBoundary
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

    @Override
    @CompilerDirectives.TruffleBoundary
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
                setDimensions(((RIntVector) value).materialize().getDataCopy());
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
            if (attributes.getShape().getPropertyCount() == 0) {
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
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            // should only be used on materialized vector
            throw RInternalError.shouldNotReachHere();
        }
        // TODO pass invoking Node
        setNames(newNames, RError.SHOW_CALLER2);
    }

    @CompilerDirectives.TruffleBoundary
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
                putAttribute(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
            } else {
                putAttribute(RRuntime.NAMES_ATTR_KEY, newNames);
                assert newNames != this;
            }
        }
    }

    @Override
    public final RList getDimNames() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return null;
        }
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
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            // should only be used on materialized vector
            throw RInternalError.shouldNotReachHere();
        }
        // TODO pass invoking node
        setDimNames(newDimNames, RError.SHOW_CALLER2);
    }

    @CompilerDirectives.TruffleBoundary
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
        }
    }

    @Override
    public final Object getRowNames() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return RNull.instance;
        }
        if (attributes == null) {
            return RNull.instance;
        } else {
            return attributes.get(RRuntime.ROWNAMES_ATTR_KEY);
        }
    }

    @Override
    public final void setRowNames(RAbstractVector newRowNames) {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            // should only be used on materialized vector
            throw RInternalError.shouldNotReachHere();
        }
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

    // Tagging interface for vectors with array based data. Make sure that an implementation also is
    // a RAbstractVector. <br/>
    // TODO for the time being:
    // RScalarVector, RSequence and RForeienWrapper types are now instances of RAttributable,
    // so attribute handling could be managed w/o materialization, but first need to check and
    // refactor places with attr related RSequence/RScalar/RScalarVector/RForeignVectorXXX based
    // assuptions
    public interface RMaterializedVector extends TruffleObject {

    }

    public boolean isMaterialized() {
        return this instanceof RMaterializedVector;
    }

    @Override
    public final boolean hasDimensions() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return false;
        }
        return attributes == null ? false : attributes.containsKey(RRuntime.DIM_ATTR_KEY);
    }

    private boolean hasDimNames() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return false;
        }
        return attributes == null ? false : attributes.containsKey(RRuntime.DIMNAMES_ATTR_KEY);
    }

    private boolean hasRowNames() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return false;
        }
        return attributes == null ? false : attributes.containsKey(RRuntime.ROWNAMES_ATTR_KEY);
    }

    private boolean hasNames() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return false;
        }
        return attributes == null ? false : attributes.containsKey(RRuntime.NAMES_ATTR_KEY);
    }

    public final boolean isMatrix() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return false;
        }
        int[] dimensions = getDimensionsFromAttrs();
        return dimensions != null && dimensions.length == 2;
    }

    public final boolean isArray() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return false;
        }
        int[] dimensions = getDimensionsFromAttrs();
        return dimensions != null && dimensions.length > 0;
    }

    @Override
    public final int[] getDimensions() {
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            return null;
        }
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
        if (!isMaterialized()) {
            assert !(this instanceof RMaterializedVector) : this.getClass().getSimpleName();
            // should only be used on materialized vector
            throw RInternalError.shouldNotReachHere();
        }
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

    public static RAbstractContainer setVectorClassAttr(RAbstractVector vector, RStringVector classAttr) {
        return (RAbstractContainer) setClassAttrInternal(vector, classAttr);
    }

    public final void setAttributes(RAbstractVector result) {
        if (this.attributes != null) {
            result.initAttributes(RAttributesLayout.copy(this.attributes));
        }
    }

    // public interface *copy* methods are final and delegate to *internalCopyAndReport* methods

    /**
     * Creates a copy of the vector. This copies all of the contained data as well. If the data in
     * the vector is to be updated upon copying, the corresponding {@code copyResetData()} method
     * should be used.
     */
    @Override
    public RAbstractVector copy() {
        RAbstractVector result = internalCopyAndReport();
        setAttributes(result);
        result.setTypedValueInfo(getTypedValueInfo());
        return result;
    }

    public RAbstractVector copyDropAttributes() {
        RAbstractVector materialized = materialize();
        assert materialized.isMaterialized();
        return materialized.internalCopyAndReport();
    }

    @Override
    public final RAbstractVector deepCopy() {
        RAbstractVector result = internalDeepCopyAndReport();
        setAttributes(result);
        return result;
    }

    public RAbstractVector copyResized(int size, boolean fillNA) {
        RAbstractVector materialized = materialize();
        assert materialized.isMaterialized();
        RAbstractVector result = materialized.internalCopyResized(size, fillNA, null);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    public RAbstractVector copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
        RAbstractVector materialized = materialize();
        assert materialized.isMaterialized();
        // TODO support for higher dimensions
        assert newDimensions.length == 2;
        RAbstractVector result = materialized.internalCopyResized(newDimensions[0] * newDimensions[1], fillNA, newDimensions);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    protected boolean isResizedComplete(int newSize, boolean filledNAs) {
        return complete && ((getLength() >= newSize) || !filledNAs);
    }

    // *internalCopyAndReport* methods do just the copy and report it to MemoryTracer. These should
    // be used if additional logic in public interface *copy* method is not desired.

    private RAbstractVector internalCopyAndReport() {
        RAbstractVector result = internalCopy();
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    private RAbstractVector internalDeepCopyAndReport() {
        RAbstractVector result = internalDeepCopy();
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    // *internalCopy* methods should only be overridden, but never invoked from anywhere but
    // *internalCopyAndReport*

    protected RAbstractVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        RAbstractVector materialized = materialize();
        assert materialized.isMaterialized();
        return materialized.internalCopyResized(size, fillNA, dimensions);
    }

    // to be overridden by recursive structures
    protected RAbstractVector internalDeepCopy() {
        return internalCopy();
    }

    protected RAbstractVector internalCopy() {
        return materialize();
    }

    /**
     * Update a data item in the vector. Possibly not as efficient as type-specific methods, but in
     * some cases it likely does not matter (e.g. if used alongside I/O operations).
     *
     * @param i index of the vector item to be updated
     * @param o updated value
     * @param naCheck NA check used to change vector's mode in case value is NA
     * @return updated vector
     */
    @SuppressWarnings("unused")
    public RAbstractVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        throw new UnsupportedOperationException();
    }

    public final RAttributable copyAttributesFrom(RAbstractContainer vector) {
        // it's meant to be used on a "fresh" vector with only dimensions potentially set
        assert (!hasNames());
        assert (!hasDimNames());
        assert (!hasRowNames());
        assert (!hasDimensions());
        assert (this.attributes == null || this.attributes.getShape().getPropertyCount() == 0) : this.attributes.getShape().getPropertyCount();
        DynamicObject vecAttributes = vector.getAttributes();
        if (vecAttributes != null) {
            initAttributes(RAttributesLayout.copy(vecAttributes));
            return copyClassAttr(vecAttributes);
        } else {
            return this;
        }
    }

    @CompilerDirectives.TruffleBoundary
    private RAbstractContainer copyClassAttr(DynamicObject vecAttributes) {
        return (RAbstractContainer) setClassAttr((RStringVector) vecAttributes.get(RRuntime.CLASS_ATTR_KEY));
    }

    /*
     * Internal version without profiles used in a rare (and already slow) case of double-to-int
     * vector conversion when setting class attribute
     */
    private RAttributable copyAttributesFromVector(RAbstractVector vector) {
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
    public final void initDimsNamesDimNames(int[] dimensions, RStringVector names, RList dimNames) {
        assert (this.attributes == null) : "Vector attributes must be null";
        assert names != this;
        assert dimNames != this;
        assert names == null || names.getLength() == getLength() : "size mismatch: names.length=" + names.getLength() + " vs. length=" + getLength();
        initAttributes(createAttributes(dimensions, names, dimNames));
    }

    @CompilerDirectives.TruffleBoundary
    public static DynamicObject createAttributes(int[] dimensions, RStringVector names, RList dimNames) {
        if (dimNames != null) {
            if (dimensions != null) {
                com.oracle.truffle.r.runtime.data.RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                // one-dimensional arrays do not have names, only dimnames with one value so do not
                // init names in that case
                if (names != null && dimensions.length != 1) {
                    return RAttributesLayout.createNamesAndDimAndDimNames(names, dimensionsVector, dimNames);
                } else {
                    return RAttributesLayout.createDimAndDimNames(dimensionsVector, dimNames);
                }
            } else {
                if (names != null) {
                    return RAttributesLayout.createNamesAndDimNames(names, dimNames);
                } else {
                    return RAttributesLayout.createDimNames(dimNames);
                }
            }
        } else {
            if (dimensions != null) {
                com.oracle.truffle.r.runtime.data.RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                if (names != null) {
                    if (dimensions.length != 1) {
                        return RAttributesLayout.createNamesAndDim(names, dimensionsVector);
                    } else {
                        // one-dimensional arrays do not have names, only dimnames with one value
                        RList newDimNames = RDataFactory.createList(new Object[]{names});
                        return RAttributesLayout.createDimAndDimNames(dimensionsVector, newDimNames);
                    }
                } else {
                    return RAttributesLayout.createDim(dimensionsVector);
                }
            } else {
                if (names != null) {
                    return RAttributesLayout.createNames(names);
                } else {
                    return null;
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
    @CompilerDirectives.TruffleBoundary
    public final RAbstractVector copyRegAttributesFrom(RAbstractContainer vector) {
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
    public final RBaseObject getNonShared() {
        RAbstractVector materialized = materialize();
        assert materialized.isMaterialized();
        return materialized.getNonSharedSuper();
    }

    private RBaseObject getNonSharedSuper() {
        return super.getNonShared();
    }

    @Override
    public RAbstractVector resize(int size) {
        RAbstractVector materialized = materialize();
        assert materialized.isMaterialized();
        return materialized.resize(size, true);
    }

    private RAbstractVector resize(int size, boolean resetAll) {
        this.complete = this.complete && getLength() >= size;
        RStringVector oldNames = UpdateShareableChildValue.update(this, this.getNamesFromAttrs());
        RAbstractVector res = copyResized(size, true);
        if (this.isShared()) {
            assert res.isTemporary();
            res.incRefCount();
        }
        if (resetAll) {
            res.resetAllAttributes(oldNames == null);
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

    @CompilerDirectives.TruffleBoundary
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

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        StringBuilder str = new StringBuilder("[");
        VectorAccess access = slowPathAccess();
        try (SequentialIterator iter = access.access(this)) {
            if (access.next(iter)) {
                while (true) {
                    str.append(access.getType().isAtomic() ? access.getString(iter) : access.getListElement(iter).toString());
                    if (!access.next(iter)) {
                        break;
                    }
                    str.append(", ");
                    if (str.length() > MAX_TOSTRING_LENGTH - 1) {
                        str.setLength(MAX_TOSTRING_LENGTH - 4);
                        str.append("...");
                        break;
                    }
                }
            }
        }
        return str.append(']').toString();
    }

    protected boolean canBeValidStore(Object store, Object data) {
        // We can be only sure if there is only one thread
        return !RContext.isSingle() || store == data;
    }

    @Override
    public abstract RAbstractVector materialize();

    public abstract RAbstractVector createEmptySameType(int newLength, boolean newIsComplete);

    /**
     * Casts a vector to another {@link RType}. If a safe cast to the target {@link RType} is not
     * supported <code>null</code> is returned. Instead of materializing the cast for each index the
     * implementation may decide to just wrap the original vector with a closure. This method is
     * optimized for invocation with a compile-time constant {@link RType}.
     *
     * @see #castSafe(RType, ConditionProfile, boolean)
     */
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        return castSafe(type, isNAProfile, false);
    }

    /**
     * Casts a vector to another {@link RType}. If a safe cast to the target {@link RType} is not
     * supported <code>null</code> is returned. Instead of materializing the cast for each index the
     * implementation may decide to just wrap the original vector with a closure. This method is
     * optimized for invocation with a compile-time constant {@link RType}.
     *
     * @param type
     * @param isNAProfile
     * @param keepAttributes If {@code true}, the cast itself will keep the attributes. This is,
     *            however, a rather slow operation and you should set this to {@code false} and use
     *            nodes for copying attributes if possible.
     *
     * @see RType#getPrecedence()
     */
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        if (type == getRType()) {
            return this;
        } else {
            return null;
        }
    }

    /**
     * Verifies the integrity of the vector, mainly whether a vector that claims to be
     * {@link #isComplete()} contains NA values.
     */
    public static boolean verifyVector(RAbstractVector vector) {
        CompilerAsserts.neverPartOfCompilation();
        VectorAccess access = vector.slowPathAccess();
        assert access.getType().isVector();
        if (!access.getType().isAtomic()) {
            // check non-atomic vectors for nullness
            try (SequentialIterator iter = access.access(vector)) {
                while (access.next(iter)) {
                    assert access.getListElement(iter) != null : "element " + iter.getIndex() + " of vector " + vector + " is null";
                }
            }
        } else if (access.getType() == RType.List) {
            assert !vector.isComplete();
        }
        if (vector.isComplete() && !(vector instanceof RSequence)) {
            // check all vectors for completeness
            access.na.enable(true);
            try (SequentialIterator iter = access.access(vector)) {
                while (access.next(iter)) {
                    assert !access.isNA(iter) : "element " + iter.getIndex() + " of vector " + vector + " is NA";
                }
            }
        }
        return true;
    }

    @Override
    public void setLength(int l) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public int getTrueLength() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void setTrueLength(int l) {
        throw RInternalError.shouldNotReachHere();
    }
}
