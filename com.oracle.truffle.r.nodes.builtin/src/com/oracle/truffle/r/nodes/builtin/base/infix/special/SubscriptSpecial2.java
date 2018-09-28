package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.ProfiledSpecialsUtilsFactory.ProfiledSubscriptSpecial2NodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.nodes.RNode;

public abstract class SubscriptSpecial2 extends AccessSpecial2 {

    protected SubscriptSpecial2(boolean inReplacement) {
        super(inReplacement);
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)", "!inReplacement"})
    protected Object access(RList vector, int index1, int index2,
                    @Cached("create()") ExtractListElement extract) {
        return extract.execute(vector, matrixIndex(vector, index1, index2));
    }

    public static RNode create(boolean inReplacement, RNode vectorNode, ConvertIndex index1, ConvertIndex index2) {
        return ProfiledSubscriptSpecial2NodeGen.create(inReplacement, vectorNode, index1, index2);
    }
}