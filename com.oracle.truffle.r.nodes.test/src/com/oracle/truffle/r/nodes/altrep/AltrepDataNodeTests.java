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

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.instanceOf;
import static com.oracle.truffle.r.nodes.test.TestUtilities.createHandle;

import com.oracle.truffle.r.ffi.impl.nodes.AltrepData1Node;
import com.oracle.truffle.r.ffi.impl.nodes.AltrepData2Node;
import com.oracle.truffle.r.nodes.test.TestBase;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.test.altrep.IntVectorDescriptorWrapper;
import com.oracle.truffle.r.test.altrep.SimpleDescriptorWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Assume;

public class AltrepDataNodeTests extends TestBase {

    private NodeHandle<AltrepData1Node> altrepData1NodeNodeHandle;
    private NodeHandle<AltrepData2Node> altrepData2NodeNodeHandle;

    public AltrepDataNodeTests() {
        execInContext(() -> {
            altrepData1NodeNodeHandle = createHandle(AltrepData1Node.create(),
                            (node, args) -> node.executeObject(args[0]));
            altrepData2NodeNodeHandle = createHandle(AltrepData2Node.create(),
                            (node, args) -> node.executeObject(args[0]));
            return null;
        });
    }

    @Test
    public void testSimple() {
        execInContext(() -> {
            SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
            RIntVector altIntVector = simpleDescriptorWrapper.getAltIntVector();

            Object data1 = altrepData1NodeNodeHandle.call(altIntVector);
            Object data2 = altrepData2NodeNodeHandle.call(altIntVector);

            assertThat(data1, is(RNull.instance));
            assertThat(data2, is(RNull.instance));
            return null;
        });
    }

    @Test
    public void testIntVectorWrapper() {
        execInContext(() -> {
            IntVectorDescriptorWrapper intVectorDescriptorWrapper = new IntVectorDescriptorWrapper();
            RIntVector altIntVector = intVectorDescriptorWrapper.getAltIntVector();

            Object data1 = altrepData1NodeNodeHandle.call(altIntVector);
            Object data2 = altrepData2NodeNodeHandle.call(altIntVector);

            assertThat(data1, is(instanceOf(RIntVector.class)));
            assertThat(data2, is(RNull.instance));
            return null;
        });
    }
}
