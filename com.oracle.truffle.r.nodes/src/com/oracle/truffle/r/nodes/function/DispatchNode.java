/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class DispatchNode extends RNode {

    protected RStringVector type;
    protected final String genericName;

    public DispatchNode(String genericName) {
        this.genericName = genericName;
    }

    static final class FunctionCall {
        RFunction function;
        CallArgumentsNode args;

        FunctionCall(RFunction function, CallArgumentsNode args) {
            this.function = function;
            this.args = args;
        }
    }

    public abstract Object execute(VirtualFrame frame, RStringVector aType);

    @SuppressWarnings("unused")
    public Object executeInternal(VirtualFrame frame, RStringVector aType, Object[] args) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    public Object executeInternal(VirtualFrame frame, Object[] args) {
        throw RInternalError.shouldNotReachHere();
    }

    public String getGenericName() {
        return genericName;
    }
}
