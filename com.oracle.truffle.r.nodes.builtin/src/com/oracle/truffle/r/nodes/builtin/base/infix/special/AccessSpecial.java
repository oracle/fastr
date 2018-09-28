package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.SubInterface;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

/**
 * Subscript code for vectors minus list is the same as subset code, this class allows sharing it.
 */
public abstract class AccessSpecial extends IndexingSpecialCommon implements SubInterface {

    protected AccessSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    public abstract double executeDouble(RAbstractDoubleVector vec, int index);

    public abstract int executeInteger(RAbstractIntVector vec, int index);

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "isValidIndex(vector, index)"})
    protected int accessInt(RAbstractIntVector vector, int index,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            return access.getInt(iter, index - 1);
        }
    }

    @Specialization(replaces = "accessInt", guards = {"simpleVector(vector)", "isValidIndex(vector, index)"})
    protected int accessIntGeneric(RAbstractIntVector vector, int index) {
        return accessInt(vector, index, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "isValidIndex(vector, index)"})
    protected double accessDouble(RAbstractDoubleVector vector, int index,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            return access.getDouble(iter, index - 1);
        }
    }

    @Specialization(replaces = "accessDouble", guards = {"simpleVector(vector)", "isValidIndex(vector, index)"})
    protected double accessDoubleGeneric(RAbstractDoubleVector vector, int index) {
        return accessDouble(vector, index, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "isValidIndex(vector, index)"})
    protected String accessString(RAbstractStringVector vector, int index,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            return access.getString(iter, index - 1);
        }
    }

    @Specialization(replaces = "accessString", guards = {"simpleVector(vector)", "isValidIndex(vector, index)"})
    protected String accessStringGeneric(RAbstractStringVector vector, int index) {
        return accessString(vector, index, vector.slowPathAccess());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object access(Object vector, Object index) {
        throw RSpecialFactory.throwFullCallNeeded();
    }
}