package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.nodes.helpers.UpdateListField;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;

@NodeInfo(cost = NodeCost.NONE)
@NodeChild(value = "arguments", type = RNode[].class)
public abstract class UpdateFieldSpecial extends RNode {

    @Child private UpdateListField updateListField;

    /**
     * {@link RNull} and lists have special handling when they are RHS of update. Nulls delete the
     * field and lists can cause cycles.
     */
    static boolean isNotRNullRList(Object value) {
        return value != RNull.instance && !(value instanceof RList);
    }

    @Specialization(guards = {"!list.isShared()", "isNotRNullRList(value)"})
    public Object doList(RList list, String field, Object value) {
        // Note: special call construction turns lookups into string constants for field accesses
        if (updateListField == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateListField = insert(UpdateListField.create());
        }
        boolean result = updateListField.execute(list, field, value);
        if (!result) {
            throw RSpecialFactory.throwFullCallNeeded(value);
        } else {
            return list;
        }
    }

    @SuppressWarnings("unused")
    @Fallback
    public RList doFallback(Object container, Object field, Object value) {
        throw RSpecialFactory.throwFullCallNeeded(value);
    }
}