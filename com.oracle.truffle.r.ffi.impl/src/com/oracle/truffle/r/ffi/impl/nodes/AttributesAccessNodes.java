/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
import static com.oracle.truffle.r.runtime.RError.NO_CALLER;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodesFactory.ATTRIBNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodesFactory.CopyMostAttribNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodesFactory.RfSetAttribNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodesFactory.SetAttribNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodesFactory.TAGNodeGen;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNode;
import com.oracle.truffle.r.nodes.attributes.GetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.GetAttributesNode;
import com.oracle.truffle.r.nodes.attributes.RemoveAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetPropertyNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetRowNamesAttributeNode;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.InternStringNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;

public final class AttributesAccessNodes {

    public static final class GetAttrib extends FFIUpCallNode.Arg2 {
        @Child private GetAttributeNode getAttributeNode = GetAttributeNode.create();
        @Child private UpdateShareableChildValueNode sharedAttrUpdate = UpdateShareableChildValueNode.create();
        @Child private CastNode castStringNode;
        private final ConditionProfile nameIsSymbolProfile = ConditionProfile.createBinaryProfile();

        @Override
        public Object executeObject(Object source, Object nameObj) {
            String name;
            if (nameIsSymbolProfile.profile(nameObj instanceof RSymbol)) {
                name = ((RSymbol) nameObj).getName();
            } else {
                name = castToString(nameObj);
            }
            Object result = null;
            if (source instanceof RAttributable) {
                result = getAttributeNode.execute((RAttributable) source, name);
            }
            return result == null ? RNull.instance : sharedAttrUpdate.updateState(source, result);
        }

        private String castToString(Object name) {
            if (castStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castStringNode = insert(newCastBuilder().asStringVector().mustBe(singleElement()).findFirst().buildCastNode());
            }
            return (String) castStringNode.doCast(name);
        }
    }

    /**
     * Sets a single attribute.
     */
    public abstract static class RfSetAttribNode extends FFIUpCallNode.Arg3 {
        @Child private CastNode castNameNode;

        public static RfSetAttribNode create() {
            return RfSetAttribNodeGen.create();
        }

        @Specialization(guards = "!isNull(value)")
        protected Object setValue(RAttributable target, Object name, Object value,
                        @Cached("create()") InternStringNode intern,
                        @Cached("create()") SetAttributeNode setAttribNode) {
            setAttribNode.execute(target, intern.execute((String) getCastNameNode().doCast(name)), value);
            return RNull.instance;
        }

        @Specialization
        protected Object setValue(@SuppressWarnings("unused") RNull target, @SuppressWarnings("unused") Object name, @SuppressWarnings("unused") Object value) {
            return RNull.instance;
        }

        @Specialization
        protected Object unsetValue(RAttributable target, Object name, @SuppressWarnings("unused") RNull nullVal,
                        @Cached("create()") InternStringNode intern,
                        @Cached("create()") RemoveAttributeNode removeAttributeNode) {
            removeAttributeNode.execute(target, intern.execute((String) getCastNameNode().doCast(name)));
            return RNull.instance;
        }

        @Fallback
        protected Object others(Object target, Object name, Object value) {
            throw unsupportedTypes("Rf_setAttrib", target, name, value);
        }

        protected static boolean isNull(Object o) {
            return o == RNull.instance;
        }

        private CastNode getCastNameNode() {
            if (castNameNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castNameNode = insert(newCastBuilder().asStringVector().findFirst().buildCastNode());
            }
            return castNameNode;
        }
    }

    public abstract static class ATTRIB extends FFIUpCallNode.Arg1 {

        @Specialization
        public Object doAttributable(RAttributable obj,
                        @Cached("create()") InternStringNode internStringNode,
                        @Cached("createWithCompactRowNames()") GetAttributesNode getAttributesNode) {
            Object resultObj = getAttributesNode.execute(obj);
            if (resultObj == RNull.instance) {
                return resultObj;
            }
            assert resultObj instanceof RList : "GetAttributesNode should return RList or RNull";
            RList list = (RList) resultObj;
            Object result = RNull.instance;
            RStringVector names = list.getNames();
            assert names != null && names.getLength() == list.getLength();
            for (int i = list.getLength() - 1; i >= 0; i--) {
                Object item = list.getDataAt(i);
                String name = names.getDataAt(i);
                if (name.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
                    item = GetRowNamesAttributeNode.ensureRowNamesCompactFormat(item);
                }
                RSymbol symbol = RDataFactory.createSymbol(internStringNode.execute(name));
                result = RDataFactory.createPairList(item, result, symbol);
            }
            return result;
        }

        @Fallback
        public RNull doOthers(Object obj) {
            if (obj == RNull.instance || RRuntime.isForeignObject(obj)) {
                return RNull.instance;
            } else {
                CompilerDirectives.transferToInterpreter();
                String type = obj == null ? "null" : obj.getClass().getSimpleName();
                throw RError.error(NO_CALLER, Message.GENERIC, "object of type '" + type + "' cannot be attributed");
            }
        }

        public static ATTRIB create() {
            return ATTRIBNodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class TAG extends FFIUpCallNode.Arg1 {

        @Specialization
        public Object doPairlist(RPairList obj,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib) {
            return plLib.getTag(obj);
        }

        @Specialization
        public Object doArgs(RArgsValuesAndNames obj) {
            ArgumentsSignature signature = obj.getSignature();
            if (signature.getLength() > 0 && signature.getName(0) != null) {
                return getSymbol(signature.getName(0));
            }
            return RNull.instance;
        }

        @Specialization
        public Object doExternalPtr(RExternalPtr obj) {
            return obj.getTag();
        }

        @Specialization
        public Object doNull(RNull x) {
            return x;
        }

        @Specialization
        public Object doSymbol(@SuppressWarnings("unused") RSymbol s) {
            return RNull.instance;
        }

        @Fallback
        @TruffleBoundary
        public RNull doOthers(Object obj) {
            throw RInternalError.unimplemented("TAG is not implemented for type " + obj.getClass().getSimpleName());
        }

        @TruffleBoundary
        private static Object getSymbol(String name) {
            return RDataFactory.createSymbol(name);
        }

        public static TAG create() {
            return TAGNodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class CopyMostAttrib extends FFIUpCallNode.Arg2 {
        @Specialization
        public Void doRAttributable(RAttributable x, RAttributable y,
                        @Cached() CopyOfRegAttributesNode copyRegAttributes) {
            copyRegAttributes.execute(x, y);
            if (x.isS4()) {
                y.setS4();
            }
            return null;
        }

        @Fallback
        @SuppressWarnings("unused")
        public Void doOthers(Object x, Object y) {
            throw RInternalError.unimplemented("Rf_copyMostAttrib only works with atrributables.");
        }

        public static CopyMostAttrib create() {
            return CopyMostAttribNodeGen.create();
        }
    }

    /**
     * Overrides the attributes pairlist of given object with a new pairlist. In FastR, we have to
     * convert the pairlist to our representation. This doesn't do any validation in GNU-R and
     * simply sets the attributes pairlist to given value and some packages assume that they can,
     * e.g., fixup any inconsistencies in special attributes like dims afterwards.
     */
    @GenerateUncached
    public abstract static class SetAttribNode extends FFIUpCallNode.Arg2 {

        public static SetAttribNode create() {
            return SetAttribNodeGen.create();
        }

        @Specialization
        protected Object doIt(RAttributable target, RPairList attributes,
                        @Cached SetPropertyNode setPropertyNode,
                        @Cached ShareObjectNode shareObjectNode,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib) {
            clearAttrs(target);
            DynamicObject attrs = target.getAttributes();
            for (RPairList attr : attributes) {
                Object tag = plLib.getTag(attr);
                if (!(tag instanceof RSymbol)) {
                    CompilerDirectives.transferToInterpreter();
                    // GNUR seems to set the attr name to NULL and fails when printing
                    // To be compatible we don't fail, but at least print warning...
                    RError.warning(NO_CALLER, Message.NO_TAG_IN_SET_ATTRIB, Utils.getTypeName(tag));
                    continue;
                }
                Object value = plLib.car(attr);
                shareObjectNode.execute(value);
                setPropertyNode.execute(attrs, ((RSymbol) tag).getName(), value);
            }
            return RNull.instance;
        }

        @Specialization
        protected Object doIt(RAttributable target, @SuppressWarnings("unused") RNull attributes) {
            clearAttrs(target);
            return RNull.instance;
        }

        @Fallback
        protected Object doOthers(Object target, Object attrs) {
            throw unsupportedTypes("SET_ATTRIB", target, attrs);
        }

        @TruffleBoundary
        private static void clearAttrs(RAttributable target) {
            target.initAttributes(RAttributesLayout.createRAttributes());
        }

        protected static boolean isShareable(Object o) {
            return RSharingAttributeStorage.isShareable(o);
        }
    }
}
