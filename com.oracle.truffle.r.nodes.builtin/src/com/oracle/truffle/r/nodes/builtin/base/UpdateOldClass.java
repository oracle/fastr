/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;

// oldClass<- (as opposed to class<-), simply sets the attribute (without handling "implicit" attributes)
@RBuiltin(name = "oldClass<-", kind = PRIMITIVE, parameterNames = {"x", "value"}, behavior = PURE)
public abstract class UpdateOldClass extends RBuiltinNode.Arg2 {

    @Child private SetClassAttributeNode setClassAttributeNode = SetClassAttributeNode.create();
    @Child private ShareObjectNode shareObjectNode;

    static {
        Casts casts = new Casts(UpdateOldClass.class);
        casts.arg("x").asAttributable(true, true, true);
        casts.arg("value").allowNull().mustBe(stringValue(), Message.SET_INVALID_ATTR, "class").asStringVector();
    }

    @Specialization
    protected Object setOldClass(RAttributable arg, RStringVector className) {
        if (className.getLength() == 0) {
            return setOldClass(arg, RNull.instance);
        }
        RAttributable result = (RAttributable) getShareObjectNode().execute(arg);
        setClassAttributeNode.setAttr(result, className);
        return result;
    }

    @Specialization
    @TruffleBoundary
    protected Object setOldClass(RAttributable arg, @SuppressWarnings("unused") RNull className) {
        RAttributable result = (RAttributable) getShareObjectNode().execute(arg);
        setClassAttributeNode.reset(result);
        return result;
    }

    @Specialization
    protected Object setOldClass(@SuppressWarnings("unused") RNull arg, @SuppressWarnings("unused") RNull className) {
        return RNull.instance;
    }

    @Specialization(guards = "!isRNull(className)")
    protected Object setOldClass(@SuppressWarnings("unused") RNull arg, @SuppressWarnings("unused") Object className) {
        throw error(Message.INVALID_NULL_LHS);
    }

    @Fallback
    @TruffleBoundary
    protected Object setOldClass(Object x, @SuppressWarnings("unused") Object className) {
        throw error(Message.CANNOT_SET_ATTR_ON, Utils.getTypeName(x));
    }

    public ShareObjectNode getShareObjectNode() {
        if (shareObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            shareObjectNode = insert(ShareObjectNode.create());
        }
        return shareObjectNode;
    }
}
