package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Common code shared between specials doing subset/subscript related operation.
 */
abstract class IndexingSpecialCommon extends Node {

    protected final boolean inReplacement;

    protected IndexingSpecialCommon(boolean inReplacement) {
        this.inReplacement = inReplacement;
    }

    protected boolean simpleVector(@SuppressWarnings("unused") RAbstractVector vector) {
        return true;
    }

    /**
     * Checks whether the given (1-based) index is valid for the given vector.
     */
    protected static boolean isValidIndex(RAbstractVector vector, int index) {
        return index >= 1 && index <= vector.getLength();
    }

    /**
     * Checks if the value is single element that can be put into a list or vector as is, because in
     * the case of vectors on the LSH of update we take each element and put it into the RHS of the
     * update function.
     */
    protected static boolean isSingleElement(Object value) {
        return value instanceof Integer || value instanceof Double || value instanceof Byte || value instanceof String;
    }
}