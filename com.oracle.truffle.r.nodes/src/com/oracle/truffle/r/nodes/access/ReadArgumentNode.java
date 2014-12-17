package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.runtime.*;

@CreateWrapper
public class ReadArgumentNode extends RNode {

    private final int index;

    protected ReadArgumentNode(int index) {
        this.index = index;
    }

    /**
     * for WrapperNode subclass.
     */
    protected ReadArgumentNode() {
        index = 0;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return RArguments.getArgument(frame, index);
    }

    public int getIndex() {
        return index;
    }

    @Override
    public ProbeNode.WrapperNode createWrapperNode(RNode node) {
        return new ReadArgumentNodeWrapper((ReadArgumentNode) node);
    }
}
