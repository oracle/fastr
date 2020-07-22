/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.altrep;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;
import com.oracle.truffle.r.ffi.impl.nodes.NewAltRepNode;
import com.oracle.truffle.r.nodes.test.TestBase;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.test.altrep.SimpleDescriptorWrapper;
import org.junit.Test;

import static com.oracle.truffle.r.nodes.test.TestUtilities.createHandle;
import static org.junit.Assert.assertTrue;

public class NewAltrepNodeTests extends TestBase {
    private NodeHandle<NewAltRepNode> newAltRepNodeHandle;

    public NewAltrepNodeTests() {
        execInContext(() -> {
            newAltRepNodeHandle = createHandle(NewAltRepNode.create(),
                            (node, args) -> node.executeObject(args[0], args[1], args[2]));
            return null;
        });
    }

    @Test
    public void testSimple() {
        execInContext(() -> {
            SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
            AltIntegerClassDescriptor descriptor = simpleDescriptorWrapper.getDescriptor();
            Object newAltIntVecObj = newAltRepNodeHandle.call(descriptor, RNull.instance, RNull.instance);

            assertThat(newAltIntVecObj, is(instanceOf(RIntVector.class)));
            RIntVector newAltIntVec = (RIntVector) newAltIntVecObj;
            assertTrue(newAltIntVec.isAltRep());
            assertThat(descriptor, equalTo(AltrepUtilities.getAltIntDescriptor(newAltIntVec)));
            return null;
        });
    }
}
