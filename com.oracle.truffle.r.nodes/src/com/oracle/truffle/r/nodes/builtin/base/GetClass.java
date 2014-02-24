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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "class")
public abstract class GetClass extends RBuiltinNode {
    @Specialization
    public Object getClass(VirtualFrame frame, RAbstractVector arg) {
        if (arg.isObject()) {
            return arg.getAttributes().get(RRuntime.CLASS_ATTR_KEY);
        }
        return arg.getClassHierarchy().get(0);
    }
}
