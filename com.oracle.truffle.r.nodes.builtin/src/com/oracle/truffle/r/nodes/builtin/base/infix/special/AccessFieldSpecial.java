package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.nodes.helpers.AccessListField;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.nodes.RNode;

@NodeInfo(cost = NodeCost.NONE)
@NodeChild(value = "arguments", type = RNode[].class)
public abstract class AccessFieldSpecial extends RNode {

    @Child private AccessListField accessListField;

    @Specialization
    public Object doList(RList list, String field) {
        // Note: special call construction turns lookups into string constants for field accesses
        if (accessListField == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            accessListField = insert(AccessListField.create());
        }
        Object result = accessListField.execute(list, field);
        if (result == null) {
            throw RSpecialFactory.throwFullCallNeeded();
        } else {
            return result;
        }
    }

    @Fallback
    @SuppressWarnings("unused")
    public Object doFallback(Object container, Object field) {
        throw RSpecialFactory.throwFullCallNeeded();
    }
}
