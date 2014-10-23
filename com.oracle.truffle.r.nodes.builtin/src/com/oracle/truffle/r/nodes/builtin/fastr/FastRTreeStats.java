/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.control.SequenceNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mjj
 */
public class FastRTreeStats {


@RBuiltin(name = "fastr.seqlengths", kind = PRIMITIVE, parameterNames = {"func"})
@RBuiltinComment("Show SequenceNode lengths")
public abstract static class FastRSeqLengths extends RInvisibleBuiltinNode {
    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected Object seqLengths(RFunction function) {
        controlVisibility();
        List<SequenceNode> list = NodeUtil.findAllNodeInstances(function.getTarget().getRootNode(), SequenceNode.class);
        int[] counts = new int[11];
        for (SequenceNode s : list) {
            int l = s.getSequence().length;
            if (l > counts.length - 1) {
                counts[counts.length -1]++;
            } else {
                counts[l]++;
            }
        }
        return RDataFactory.createIntVector(counts, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RNull printTree(@SuppressWarnings("unused") RMissing function) {
        controlVisibility();
        throw RError.error(RError.Message.ARGUMENTS_PASSED_0_1);
    }

    @Fallback
    protected RNull printTree(@SuppressWarnings("unused") Object function) {
        controlVisibility();
        throw RError.error(RError.Message.INVALID_ARGUMENT, "func");
    }

}
    
}
