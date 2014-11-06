/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.control.SequenceNode;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Encapsulates the nodes that save the incoming function arguments into the frame. Functionally a
 * pass-through, but provides structure that assists instrumentation. This <b>always</b> exists even
 * if the function has no formal arguments.
 */
public class SaveArgumentsNode extends SequenceNode {

    public static final SaveArgumentsNode NO_ARGS = new SaveArgumentsNode(RNode.EMTPY_RNODE_ARRAY);

    public SaveArgumentsNode(RNode[] sequence) {
        super(sequence);
    }

    @Override
    public RNode substitute(REnvironment env) {
        SequenceNode seqSub = (SequenceNode) super.substitute(env);
        return new SaveArgumentsNode(seqSub.getSequence());
    }

}
