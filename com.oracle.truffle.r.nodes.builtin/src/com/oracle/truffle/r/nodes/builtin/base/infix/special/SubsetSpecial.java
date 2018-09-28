package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.ProfiledSpecialsUtilsFactory.ProfiledSubsetSpecialNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Subset special only handles single element integer/double index. In the case of list, we need to
 * create the actual list otherwise we just return the primitive type.
 */
public abstract class SubsetSpecial extends AccessSpecial {

    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    protected SubsetSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    @Override
    protected boolean simpleVector(RAbstractVector vector) {
        return super.simpleVector(vector) && getNamesNode.getNames(vector) == null;
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index)", "!inReplacement"})
    protected static RList access(RList vector, int index,
                    @Cached("create()") ExtractListElement extract) {
        return RDataFactory.createList(new Object[]{extract.execute(vector, index - 1)});
    }

    protected static ExtractVectorNode createAccess() {
        return ExtractVectorNode.create(ElementAccessMode.SUBSET, false);
    }

    @Specialization(guards = {"simpleVector(vector)", "!inReplacement"})
    protected Object accessObject(RAbstractVector vector, Object index,
                    @Cached("createAccess()") ExtractVectorNode extract) {
        return extract.apply(vector, new Object[]{index}, RRuntime.LOGICAL_TRUE, RLogical.TRUE);
    }

    public static RNode create(boolean inReplacement, RNode vectorNode, ConvertIndex index) {
        return ProfiledSubsetSpecialNodeGen.create(inReplacement, vectorNode, index);
    }
}