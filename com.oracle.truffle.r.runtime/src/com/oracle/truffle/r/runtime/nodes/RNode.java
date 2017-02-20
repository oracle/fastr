/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.RTypesGen;
import com.oracle.truffle.r.runtime.nodes.instrumentation.RNodeWrapperFactory;

@TypeSystemReference(RTypes.class)
@Instrumentable(factory = RNodeWrapperFactory.class)
public abstract class RNode extends RBaseNode implements RInstrumentableNode {

    /**
     * Normal execute function that is called when the return value, but not its visibility is
     * needed.
     */
    public abstract Object execute(VirtualFrame frame);

    /**
     * This function is called when the result is not needed. Its name does not start with "execute"
     * so that the DSL does not treat it like an execute function.
     */
    public void voidExecute(VirtualFrame frame) {
        execute(frame);
    }

    /**
     * This function is called when both the result and the result's visibility are needed. Its name
     * does not start with "execute" so that the DSL does not treat it like an execute function.
     */
    public Object visibleExecute(VirtualFrame frame) {
        return execute(frame);
    }

    /*
     * Execute functions with primitive return types:
     */

    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectInteger(execute(frame));
    }

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectDouble(execute(frame));
    }

    public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectByte(execute(frame));
    }
}
