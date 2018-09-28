package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.ProfiledSpecialsUtilsFactory.ProfiledUpdateSubscriptSpecial2NodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertValue;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.nodes.RNode;

public abstract class UpdateSubscriptSpecial2 extends IndexingSpecial2Common {

    protected UpdateSubscriptSpecial2(boolean inReplacement) {
        super(inReplacement);
    }

    protected abstract Object execute(VirtualFrame frame, Object vec, Object index1, Object index2, Object value);

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RIntVector setInt(RIntVector vector, int index1, int index2, int value,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            access.setInt(iter, matrixIndex(vector, index1, index2), value);
            if (RRuntime.isNA(value)) {
                vector.setComplete(false);
            }
            return vector;
        }
    }

    @Specialization(replaces = "setInt", guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RIntVector setIntGeneric(RIntVector vector, int index1, int index2, int value) {
        return setInt(vector, index1, index2, value, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RDoubleVector setDouble(RDoubleVector vector, int index1, int index2, double value,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            access.setDouble(iter, matrixIndex(vector, index1, index2), value);
            if (RRuntime.isNA(value)) {
                vector.setComplete(false);
            }
            return vector;
        }
    }

    @Specialization(replaces = "setDouble", guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RDoubleVector setDoubleGeneric(RDoubleVector vector, int index1, int index2, double value) {
        return setDouble(vector, index1, index2, value, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RStringVector setString(RStringVector vector, int index1, int index2, String value,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            access.setString(iter, matrixIndex(vector, index1, index2), value);
            if (RRuntime.isNA(value)) {
                vector.setComplete(false);
            }
            return vector;
        }
    }

    @Specialization(replaces = "setString", guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RStringVector setStringGeneric(RStringVector vector, int index1, int index2, String value) {
        return setString(vector, index1, index2, value, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(list)", "simpleVector(list)", "!list.isShared()", "isValidIndex(list, index1, index2)", "isSingleElement(value)"})
    protected Object setList(RList list, int index1, int index2, Object value,
                    @Cached("list.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(list)) {
            access.setListElement(iter, matrixIndex(list, index1, index2), value);
            return list;
        }
    }

    @Specialization(replaces = "setList", guards = {"simpleVector(list)", "!list.isShared()", "isValidIndex(list, index1, index2)", "isSingleElement(value)"})
    protected Object setListGeneric(RList list, int index1, int index2, Object value) {
        return setList(list, index1, index2, value, list.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RDoubleVector setDoubleIntIndexIntValue(RDoubleVector vector, int index1, int index2, int value,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            if (RRuntime.isNA(value)) {
                access.setDouble(iter, matrixIndex(vector, index1, index2), RRuntime.DOUBLE_NA);
                vector.setComplete(false);
            } else {
                access.setDouble(iter, matrixIndex(vector, index1, index2), value);
            }
            return vector;
        }
    }

    @Specialization(replaces = "setDoubleIntIndexIntValue", guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RDoubleVector setDoubleIntIndexIntValueGeneric(RDoubleVector vector, int index1, int index2, int value) {
        return setDoubleIntIndexIntValue(vector, index1, index2, value, vector.slowPathAccess());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object setFallback(Object vector, Object index1, Object index2, Object value) {
        throw RSpecialFactory.throwFullCallNeeded(value);
    }

    public static RNode create(boolean inReplacement, RNode vector, ConvertIndex index1, ConvertIndex index2, ConvertValue value) {
        return ProfiledUpdateSubscriptSpecial2NodeGen.create(inReplacement, vector, index1, index2, value);
    }
}