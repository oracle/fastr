/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <h2>Attributes handling nodes</h2> Generally, this package contains nodes performing basic
 * operations on attributes, such as setting, getting, removing and iterating. To achieve better
 * performance, these nodes should be used in preference to the methods on the objects carrying
 * attributes. In essence, the <code>execute</code> method of each node accepts as its first
 * argument an object carrying attributes, which may be either an instance of
 * {@link com.oracle.truffle.api.object.DynamicObject} or
 * {@link com.oracle.truffle.r.runtime.data.RAttributable} (i.e. lists, vectors etc.).
 * <p>
 * <h3>Arbitrary attribute nodes</h3> The nodes in this group operate on the attribute specified as
 * the second argument of the <code>execute</code> method.
 * <ul>
 * <li>{@link com.oracle.truffle.r.nodes.attributes.GetAttributeNode}: retrieves the value of an
 * arbitrary attribute
 * <li>{@link com.oracle.truffle.r.nodes.attributes.SetAttributeNode}: sets the value of an
 * arbitrary attribute. If the first argument is an instance
 * {@link com.oracle.truffle.r.runtime.data.RAttributable}, the node initializes the object with the
 * empty attributes.
 * </ul>
 *
 * <h3>Fixed attribute nodes</h3> The nodes in this group operate on the attribute that is specified
 * during the initialization of a node.
 * <ul>
 * <li>{@link com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode}: retrieves the value of
 * the predefined attribute
 * <li>{@link com.oracle.truffle.r.nodes.attributes.HasFixedAttributeNode}: determines the existence
 * of the predefined attribute
 * <li>{@link com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode}: sets the value of the
 * predefined attribute. If the first argument is an instance
 * {@link com.oracle.truffle.r.runtime.data.RAttributable}, the node initializes the object with the
 * empty attributes.
 * <li>{@link com.oracle.truffle.r.nodes.attributes.RemoveFixedAttributeNode}: removes the
 * predefined attribute
 * </ul>
 * There are additional subclasses of the above-mentioned nodes handling the special attributes,
 * such as <code>names</code>, <code>dimnames</code> etc.
 *
 * <h3>Special attributes handling</h3> The nodes handling the special attributes are derived from
 * the fixed attribute nodes described in the previous section. The logic in these special attribute
 * nodes implements side-effects that take place when a given special attribute is retrieved from,
 * set to or removed from an instance of {@link com.oracle.truffle.r.runtime.data.RAttributable}.
 * <p>
 * N.B. The nodes define additional specializations reflecting the fact that those side-effects are
 * polymorphic (i.e. they may depend on the particular class). These specializations implement in a
 * more efficient way the logic of their counterparts in attributable objects (such as
 * {@link com.oracle.truffle.r.runtime.data.model.RAbstractContainer#setDimNames(com.oracle.truffle.r.runtime.data.RList)}
 * ).
 * <p>
 * The setter nodes are outlined in the following list:
 * <ul>
 * <li>
 * {@link com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode}
 * <li>{@link com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode}
 * <li>
 * {@link com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode}
 * <li>
 * {@link com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode}
 * <li>
 * {@link com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetRowNamesAttributeNode}
 * </ul>
 * For each node in the list there is a corresponding "get", "has" and "remove" counterpart.
 * <p>
 * When creating a fixed attribute node, one needn't take care of whether the attribute is a special
 * one or not. The static factory methods defined on the base fixed attribute nodes take care of
 * that and create the corresponding instance as long as the attribute is a special one. Thus, all
 * the following initializations produce an instance of
 * {@link com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode}.
 *
 * <pre>
 * &#64;Child private SetFixedAttributeNode setDimNode = SetFixedAttributeNode.create("dim");
 * &#64;Child private SetFixedAttributeNode setDimNode = SetFixedAttributeNode.create(RRuntime.DIM_ATTR_KEY);
 * &#64;Child private SetFixedAttributeNode setDimNode = SetFixedAttributeNode.createDim();
 * &#64;Child private SetFixedAttributeNode setDimNode = SetDimAttributeNode.create();
 * </pre>
 *
 * Similarly, one does not need to take care of the special attributes when accessing arbitrary
 * attributes in an attributable instance. As shown in the following snippet, the arbitrary
 * attribute node recognizes a potential special attribute and handles it appropriately.
 * <p>
 * N.B. This mechanism works for instances of
 * {@link com.oracle.truffle.r.runtime.data.RAttributable} only.
 *
 * <pre>
 * &#64;Child
 * private SetFixedAttributeNode setAttrNode = SetAttributeNode.create();
 *
 * &#64;Specialization
 * protected Object handleStringVector(RAbstractStringVector v, String attrName, Object attrValue) {
 *    ...
 *    setAttrNode.execute(vector, attrName, attrValue);
 *    ...
 * }
 * </pre>
 *
 * <h3>Iterative nodes</h3> There are two nodes returning iterable instances. The elements returned
 * by those objects are instances of
 * {@link com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute}.
 * <ul>
 * <li>{@link com.oracle.truffle.r.nodes.attributes.ArrayAttributeNode}
 * <li>{@link com.oracle.truffle.r.nodes.attributes.IterableAttributeNode}
 * </ul>
 * The above-mentioned nodes always return a non-null instance, even if an attributable instance has
 * no attributes.
 *
 * <pre>
 * &#64;Child private IterableAttributeNode iterAttrAccess = IterableAttributeNode.create();
 *
 * &#64;Specialization
 * protected Object handleStringVector(RAbstractStringVector v) {
 *    ...
 *    for (RAttribute a : iterAttrAccess.execute(v)) {
 *      if ("foo".equals(a.getName())) {
 *          ...
 *      }
 *    }
 *    ...
 * }
 * </pre>
 *
 */
package com.oracle.truffle.r.nodes.attributes;
