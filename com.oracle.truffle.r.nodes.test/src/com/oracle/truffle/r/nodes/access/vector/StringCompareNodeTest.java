/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.vector;

import static com.oracle.truffle.r.nodes.test.TestUtilities.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assume.*;

import org.junit.*;
import org.junit.experimental.theories.*;
import org.junit.runner.*;

import com.oracle.truffle.r.nodes.access.vector.SearchFirstStringNode.CompareStringNode;
import com.oracle.truffle.r.nodes.access.vector.SearchFirstStringNode.CompareStringNode.StringEqualsNode;
import com.oracle.truffle.r.nodes.test.*;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.*;

@RunWith(Theories.class)
public class StringCompareNodeTest extends TestBase {

    // Please note that "FB" and "Ea" produce a hash collision. Thats why its tested here.
    @DataPoints public static String[] TEST = {RRuntime.STRING_NA, "a", "abc", "bc".intern(), "bc".intern(), "FB", "Ea"};

    @Theory
    public void testExactNA(String a, String b) {
        assumeTrue(a == RRuntime.STRING_NA || b == RRuntime.STRING_NA);
        try {
            executeCompare(true, a, b);
            Assert.fail();
        } catch (AssertionError e) {
        }
    }

    @Theory
    public void testNonExactNA(String a, String b) {
        assumeTrue(a == RRuntime.STRING_NA || b == RRuntime.STRING_NA);
        try {
            executeCompare(false, a, b);
            Assert.fail();
        } catch (AssertionError e) {
        }
    }

    @Theory
    public void testExact(String a, String b) {
        assumeFalse(a == RRuntime.STRING_NA);
        assumeFalse(b == RRuntime.STRING_NA);
        assertThat(executeCompare(true, a, b), is(a.equals(b)));
        assertThat(executeHashCompare(a, b), is(a.equals(b)));
    }

    @Theory
    public void testNonExact(String a, String b) {
        assumeFalse(a == RRuntime.STRING_NA);
        assumeFalse(b == RRuntime.STRING_NA);
        assertThat(executeCompare(false, a, b), is(a.startsWith(b)));
    }

    private static boolean executeCompare(boolean exact, String a, String b) {
        NodeHandle<CompareStringNode> handle = createHandle(exact ? CompareStringNode.createEquals() : CompareStringNode.createStartsWith(), //
                        (node, args) -> node.executeCompare((String) args[0], (String) args[1]));
        return (Boolean) handle.call(a, b);
    }

    private static boolean executeHashCompare(String a, String b) {
        NodeHandle<StringEqualsNode> handle = createHandle(CompareStringNode.createEquals(), //
                        (node, args) -> node.executeCompare((String) args[0], ((String) args[0]).hashCode(), (String) args[1]));
        return (Boolean) handle.call(a, b);
    }
}
