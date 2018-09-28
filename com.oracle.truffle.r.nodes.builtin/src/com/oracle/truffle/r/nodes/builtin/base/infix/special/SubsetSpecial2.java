package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.ProfiledSpecialsUtilsFactory.ProfiledSubsetSpecial2NodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Subset special only handles single element integer/double index. In the case of list, we need to
 * create the actual list otherwise we just return the primitive type.
 */
public abstract class SubsetSpecial2 extends AccessSpecial2 {

    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    protected SubsetSpecial2(boolean inReplacement) {
        super(inReplacement);
    }

    @Override
    protected boolean simpleVector(RAbstractVector vector) {
        return super.simpleVector(vector) && getNamesNode.getNames(vector) == null;
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)", "!inReplacement"})
    protected RList access(RList vector, int index1, int index2,
                    @Cached("create()") ExtractListElement extract) {
        return RDataFactory.createList(new Object[]{extract.execute(vector, matrixIndex(vector, index1, index2))});
    }

    public static RNode create(boolean inReplacement, RNode vectorNode, ConvertIndex index1, ConvertIndex index2) {
        return ProfiledSubsetSpecial2NodeGen.create(inReplacement, vectorNode, index1, index2);
    }
}