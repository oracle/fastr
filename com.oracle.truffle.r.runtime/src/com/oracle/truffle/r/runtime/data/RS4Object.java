/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess.AccessSlotAccess;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess.ArrayAttributeAccess;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess.UpdateSlotAccess;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.R2Foreign;

/**
 * This is a placeholder class for an S4 object (GnuR S4SXP). It has no functionality at present but
 * is needed as such objects are generated when unserializing the "methods" package.
 */
@ExportLibrary(InteropLibrary.class)
public final class RS4Object extends RSharingAttributeStorage implements Shareable {

    public RS4Object() {
        setS4();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @Cached.Shared("arrayAttribute") @Cached(value = "createCachedArrayAttrAccess()", uncached = "getUncachedArrayAttrAccess()") ArrayAttributeAccess arrayAttrAccess) {
        RAttributesLayout.RAttribute[] attrs = arrayAttrAccess.execute(getAttributes());
        String[] data = new String[attrs.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = attrs[i].getName();
        }
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }

    @ExportMessage
    boolean isMemberReadable(String member,
                    @Cached.Shared("arrayAttribute") @Cached(value = "createCachedArrayAttrAccess()", uncached = "getUncachedArrayAttrAccess()") ArrayAttributeAccess arrayAttrAccess) {
        return hasAttr(member, arrayAttrAccess);
    }

    @ExportMessage
    boolean isMemberModifiable(String member,
                    @Cached.Shared("arrayAttribute") @Cached(value = "createCachedArrayAttrAccess()", uncached = "getUncachedArrayAttrAccess()") ArrayAttributeAccess arrayAttrAccess) {
        return isModifiable(member, hasAttr(member, arrayAttrAccess));
    }

    @SuppressWarnings("static-method")
    private boolean isModifiable(String member, boolean hasAttr) {
        return !member.equals("class") && hasAttr;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isMemberInsertable(@SuppressWarnings("unused") String member) {
        return false;
    }

    @ExportMessage
    boolean isMemberInvocable(String member,
                    @Cached.Shared("accessCallTarget") @Cached(value = "createAccessCallTarget()", uncached = "createAccessCallTarget()") CallTarget ct,
                    @Cached.Shared("indirectCallNode") @Cached IndirectCallNode callNode,
                    @Cached.Shared("arrayAttribute") @Cached(value = "createCachedArrayAttrAccess()", uncached = "getUncachedArrayAttrAccess()") ArrayAttributeAccess arrayAttrAccess,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) {
        if (unknownIdentifier.profile(!hasAttr(member, arrayAttrAccess))) {
            return false;
        }
        Object result = callNode.call(ct, this, member);
        return result instanceof RFunction;
    }

    private boolean hasAttr(String member, ArrayAttributeAccess arrayAttr) {
        RAttribute[] attrs = arrayAttr.execute(getAttributes());
        for (RAttribute attr : attrs) {
            if (attr.getName().equals(member)) {
                return true;
            }
        }
        return false;
    }

    @ExportMessage
    Object readMember(String member,
                    @Cached() R2Foreign r2Foreign,
                    @Cached.Shared("accessCallTarget") @Cached(value = "createAccessCallTarget()", uncached = "createAccessCallTarget()") CallTarget ct,
                    @Cached.Shared("indirectCallNode") @Cached() IndirectCallNode callNode,
                    @Cached.Shared("arrayAttribute") @Cached(value = "createCachedArrayAttrAccess()", uncached = "getUncachedArrayAttrAccess()") ArrayAttributeAccess arrayAttrAccess,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) throws UnknownIdentifierException {
        if (unknownIdentifier.profile(!hasAttr(member, arrayAttrAccess))) {
            throw UnknownIdentifierException.create(member);
        }
        Object result = callNode.call(ct, this, member);
        return r2Foreign.convert(result);
    }

    @ExportMessage
    void writeMember(String member, Object value,
                    @Cached() Foreign2R foreign2R,
                    @Cached.Shared("arrayAttribute") @Cached(value = "createCachedArrayAttrAccess()", uncached = "getUncachedArrayAttrAccess()") ArrayAttributeAccess arrayAttrAccess,
                    @Cached(value = "createUpdateCallTarget()", uncached = "createUpdateCallTarget()") CallTarget ct,
                    @Cached.Shared("indirectCallNode") @Cached() IndirectCallNode callNode,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isModifiable) throws UnsupportedMessageException {
        boolean hasAttr = hasAttr(member, arrayAttrAccess);
        if (unknownIdentifier.profile(!hasAttr)) {
            throw UnsupportedMessageException.create();
        }
        if (isModifiable.profile(!isModifiable(member, hasAttr))) {
            throw UnsupportedMessageException.create();
        }
        callNode.call(ct, this, member, foreign2R.convert(value));
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments,
                    @Cached.Shared("accessCallTarget") @Cached(value = "createAccessCallTarget()", uncached = "createAccessCallTarget()") CallTarget ct,
                    @Cached.Shared("indirectCallNode") @Cached IndirectCallNode callNode,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isFunction,
                    @Cached() RFunction.ExplicitCall c) throws UnknownIdentifierException, UnsupportedMessageException {
        Object result = callNode.call(ct, this, member, arguments);
        if (unknownIdentifier.profile(result == null)) {
            throw UnknownIdentifierException.create(member);
        }
        if (isFunction.profile(!(result instanceof RFunction))) {
            throw UnsupportedMessageException.create();
        }
        return c.execute((RFunction) result, arguments);
    }

    @Override
    public RType getRType() {
        return RType.S4Object;
    }

    @Override
    public RS4Object copy() {
        RS4Object resultS4 = RDataFactory.createS4Object();
        if (getAttributes() != null) {
            resultS4.initAttributes(RAttributesLayout.copy(getAttributes()));
        }
        resultS4.setTypedValueInfo(getTypedValueInfo());
        return resultS4;
    }

    protected static ArrayAttributeAccess createCachedArrayAttrAccess() {
        return DSLConfig.getInteropLibraryCacheSize() > 0 ? RContext.getRRuntimeASTAccess().createArrayAttributeAccess(true) : getUncachedArrayAttrAccess();
    }

    protected static ArrayAttributeAccess getUncachedArrayAttrAccess() {
        return RContext.getRRuntimeASTAccess().createArrayAttributeAccess(false);
    }

    protected static CallTarget createUpdateCallTarget() {
        return RContext.getInstance().getOrCreateCachedCallTarget(UpdateSlotRootNode.class, () -> Truffle.getRuntime().createCallTarget(new UpdateSlotRootNode()));
    }

    private static class UpdateSlotRootNode extends RootNode {
        @Child UpdateSlotAccess delegate = RContext.getRRuntimeASTAccess().createUpdateSlotAccess();

        UpdateSlotRootNode() {
            super(null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return delegate.execute(RContext.getInstance().stateREnvironment.getGlobalFrame(), args[0], args[1], args[2]);
        }
    }

    protected static CallTarget createAccessCallTarget() {
        return RContext.getInstance().getOrCreateCachedCallTarget(AccessSlotRootNode.class, () -> Truffle.getRuntime().createCallTarget(new AccessSlotRootNode()));
    }

    private static class AccessSlotRootNode extends RootNode {
        @Child AccessSlotAccess delegate = RContext.getRRuntimeASTAccess().createAccessSlotAccess();

        AccessSlotRootNode() {
            super(null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return delegate.execute(args[0], args[1]);
        }
    }

}
