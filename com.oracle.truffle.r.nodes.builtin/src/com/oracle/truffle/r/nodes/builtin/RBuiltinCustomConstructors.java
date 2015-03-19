/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.r.nodes.builtin.RBuiltinNode.RCustomBuiltinNode;
import com.oracle.truffle.r.runtime.*;

/**
 * Avoids the use of reflection in {@link RCustomBuiltinNode} builtins created by
 * {@code RBuiltinPackage.ReflectiveNodeFactory}. Originally created by an annotation processor,
 * edit this file to add a new one.
 */
public class RBuiltinCustomConstructors {
    public static RBuiltinNode createNode(String nodeClassName, RBuiltinNode node) {
        switch (nodeClassName) {
            case "com.oracle.truffle.r.nodes.builtin.base.Floor":
                return new com.oracle.truffle.r.nodes.builtin.base.Floor(node);
            case "com.oracle.truffle.r.nodes.builtin.base.Max":
                return new com.oracle.truffle.r.nodes.builtin.base.Max(node);
            case "com.oracle.truffle.r.nodes.builtin.base.Min":
                return new com.oracle.truffle.r.nodes.builtin.base.Min(node);
            case "com.oracle.truffle.r.nodes.builtin.base.Ceiling":
                return new com.oracle.truffle.r.nodes.builtin.base.Ceiling(node);
            case "com.oracle.truffle.r.nodes.builtin.base.Sum":
                return new com.oracle.truffle.r.nodes.builtin.base.Sum(node);
            case "com.oracle.truffle.r.nodes.builtin.base.Recall":
                return new com.oracle.truffle.r.nodes.builtin.base.Recall(node);
            default:
                throw Utils.fail("unimplemented RCustomBuiltinNode: " + nodeClassName);
        }
    }

}
