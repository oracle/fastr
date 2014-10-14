package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;

public class ReadArgumentNode extends RNode {

    private final int index;

    protected ReadArgumentNode(int index) {
        this.index = index;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return RArguments.getArgument(frame, index);
    }

    public int getIndex() {
        return index;
    }
}
