/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes.attributes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNodeGen.RemoveClassAttributeAccessNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNodeGen.RemoveCommentAttributeAccessNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNodeGen.RemoveDimAttributeAccessNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNodeGen.RemoveDimNamesAttributeAccessNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNodeGen.RemoveGenericAttributeAccessNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNodeGen.RemoveNamesAttributeAccessNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNodeGen.RemoveRowNamesAttributeAccessNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNodeGen.RemoveTspAttributeAccessNodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RPairList;

@GenerateUncached
public abstract class RemoveFixedAttributeNode extends FixedAttributeAccessNode {

    public static RemoveFixedAttributeNode create(String name) {
        return RemoveGenericAttributeAccessNodeGen.create(name);
    }

    public static RemoveNamesAttributeAccessNode createNames() {
        return RemoveNamesAttributeAccessNodeGen.create();
    }

    public static RemoveRowNamesAttributeAccessNode createRowNames() {
        return RemoveRowNamesAttributeAccessNodeGen.create();
    }

    public static RemoveDimAttributeAccessNode createDim() {
        return RemoveDimAttributeAccessNodeGen.create();
    }

    public static RemoveDimNamesAttributeAccessNode createDimNames() {
        return RemoveDimNamesAttributeAccessNodeGen.create();
    }

    public static RemoveClassAttributeAccessNode createClass() {
        return RemoveClassAttributeAccessNodeGen.create();
    }

    public static RemoveTspAttributeAccessNode createTsp() {
        return RemoveTspAttributeAccessNodeGen.create();
    }

    public static RemoveCommentAttributeAccessNode createComment() {
        return RemoveCommentAttributeAccessNodeGen.create();
    }

    public abstract void execute(RAttributable attrs);

    protected RemoveFixedPropertyNode createRemoveFixedPropertyNode() {
        throw RInternalError.shouldNotReachHere();
    }

    @Specialization
    protected static void removeAttrFromAttributable(RAttributable x,
                    @Cached("create()") BranchProfile attrNullProfile,
                    @Cached("createRemoveFixedPropertyNode()") RemoveFixedPropertyNode removeFixedPropertyNode,
                    @Cached("create()") BranchProfile emptyAttrProfile) {
        DynamicObject attributes = x.getAttributes();

        if (attributes == null) {
            attrNullProfile.enter();
            return;
        }
        removeFixedPropertyNode.execute(attributes);

        if (attributes.getShape().getPropertyCount() == 0) {
            emptyAttrProfile.enter();
            x.initAttributes(null);
        }
    }

    public abstract static class RemoveGenericAttributeAccessNode extends RemoveFixedAttributeNode {
        private final String name;

        public RemoveGenericAttributeAccessNode(String name) {
            this.name = name;
        }

        @Override
        protected String getAttributeName() {
            return name;
        }

        @Override
        protected RemoveFixedPropertyNode createRemoveFixedPropertyNode() {
            return RemoveFixedPropertyNode.create(name);
        }
    }

    @GenerateUncached
    public abstract static class RemoveNamesAttributeAccessNode extends RemoveFixedAttributeNode {
        @Override
        protected String getAttributeName() {
            return RRuntime.NAMES_ATTR_KEY;
        }

        @Override
        protected RemoveFixedPropertyNode createRemoveFixedPropertyNode() {
            return RemoveFixedPropertyNode.createNames();
        }

    }

    @GenerateUncached
    public abstract static class RemoveRowNamesAttributeAccessNode extends RemoveFixedAttributeNode {
        @Override
        protected String getAttributeName() {
            return RRuntime.ROWNAMES_ATTR_KEY;
        }

        @Override
        protected RemoveFixedPropertyNode createRemoveFixedPropertyNode() {
            return RemoveFixedPropertyNode.createRowNames();
        }
    }

    @GenerateUncached
    public abstract static class RemoveDimAttributeAccessNode extends RemoveFixedAttributeNode {
        @Override
        protected String getAttributeName() {
            return RRuntime.DIM_ATTR_KEY;
        }

        @Override
        protected RemoveFixedPropertyNode createRemoveFixedPropertyNode() {
            return RemoveFixedPropertyNode.createDim();
        }
    }

    @GenerateUncached
    public abstract static class RemoveDimNamesAttributeAccessNode extends RemoveFixedAttributeNode {
        @Override
        protected String getAttributeName() {
            return RRuntime.DIMNAMES_ATTR_KEY;
        }

        @Specialization(insertBefore = "removeAttrFromAttributable")
        protected static void removeAttrFromAttributable(RPairList x) {
            x.setNames(null);
        }

        @Override
        protected RemoveFixedPropertyNode createRemoveFixedPropertyNode() {
            return RemoveFixedPropertyNode.createDimNames();
        }
    }

    @GenerateUncached
    public abstract static class RemoveClassAttributeAccessNode extends RemoveFixedAttributeNode {
        @Override
        protected String getAttributeName() {
            return RRuntime.CLASS_ATTR_KEY;
        }

        @Override
        protected RemoveFixedPropertyNode createRemoveFixedPropertyNode() {
            return RemoveFixedPropertyNode.createClass();
        }
    }

    @GenerateUncached
    public abstract static class RemoveTspAttributeAccessNode extends RemoveFixedAttributeNode {
        @Override
        protected String getAttributeName() {
            return RRuntime.TSP_ATTR_KEY;
        }

        @Override
        protected RemoveFixedPropertyNode createRemoveFixedPropertyNode() {
            return RemoveFixedPropertyNode.createTsp();
        }
    }

    @GenerateUncached
    public abstract static class RemoveCommentAttributeAccessNode extends RemoveFixedAttributeNode {
        @Override
        protected String getAttributeName() {
            return RRuntime.COMMENT_ATTR_KEY;
        }

        @Override
        protected RemoveFixedPropertyNode createRemoveFixedPropertyNode() {
            return RemoveFixedPropertyNode.createComment();
        }
    }
}
