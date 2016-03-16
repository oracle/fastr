/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "class", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class GetClass extends RBuiltinNode {

    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(true, false);

    @Specialization
    protected RAbstractStringVector getClass(Object x) {
        controlVisibility();
        return classHierarchy.execute(x);
    }
}
