package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.ProfiledSpecialsUtilsFactory.ProfiledSubscriptSpecialNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

public abstract class SubscriptSpecial extends AccessSpecial {

    protected SubscriptSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index)", "!inReplacement"})
    protected static Object access(RList vector, int index,
                    @Cached("create()") ExtractListElement extract) {
        return extract.execute(vector, index - 1);
    }

    protected static ExtractVectorNode createAccess() {
        return ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, false);
    }

    @Specialization(guards = {"simpleVector(vector)", "!inReplacement"})
    protected static Object accessObject(RAbstractVector vector, Object index,
                    @Cached("createAccess()") ExtractVectorNode extract) {
        return extract.apply(vector, new Object[]{index}, RRuntime.LOGICAL_TRUE, RLogical.TRUE);
    }

    public static RNode create(boolean inReplacement, RNode profiledVector, ConvertIndex index) {
        return ProfiledSubscriptSpecialNodeGen.create(inReplacement, profiledVector, index);
    }
}