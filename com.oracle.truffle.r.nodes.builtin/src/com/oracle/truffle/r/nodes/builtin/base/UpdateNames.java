/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.HasAttributesNode;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.IsNotObject;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMaterializedVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "names<-", kind = PRIMITIVE, parameterNames = {"x", "value"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateNames extends RBuiltinNode.Arg2 {

    @Child private GetDimAttributeNode getDimNode;
    @Child private RemoveFixedAttributeNode removeDimNamesNode;
    @Child private SetNamesAttributeNode setNamesAttributeNode;
    @Child protected IsNotObject isNotObject = IsNotObject.create();
    @Child private RExplicitCallNode asCharacterCall;
    @CompilationFinal private RFunction asCharacterFunction;

    static {
        Casts casts = new Casts(UpdateNames.class);
        casts.arg("x");
    }

    // Note: if frame is null, then no dispatch to as.character is done
    public abstract Object executeStringVector(VirtualFrame frame, RAbstractContainer container, Object o);

    // Fast-path for RAbstractVector and namesArg that has no class:

    @Specialization(guards = {"isNotObject.execute(namesArgIn)", "vectorReuse.supports(container)"})
    protected RAbstractContainer updateNamesNoObject(RAbstractContainer container, Object namesArgIn,
                    @Cached("create()") StripAttributes stripAttributes,
                    @Cached("createNonShared(container)") VectorReuse vectorReuse) {
        RAbstractContainer result = vectorReuse.getMaterializedResult(container);
        Object namesArg = stripAttributes.execute(namesArgIn);
        return updateNames(result, namesArg);
    }

    @Specialization(replaces = "updateNamesNoObject", guards = {"isNotObject.execute(namesArg)"})
    protected RAbstractContainer updateNamesNoObjectGeneric(RAbstractContainer container, Object namesArg,
                    @Cached("create()") StripAttributes stripAttributes,
                    @Cached("createNonSharedGeneric()") VectorReuse vectorReuse) {
        return updateNamesNoObject(container, namesArg, stripAttributes, vectorReuse);
    }

    // Generic version for RAbstractVector:

    @Specialization(guards = {"!isNotObject.execute(namesArg)", "vectorReuse.supports(container)"})
    protected RAbstractContainer updateNamesVector(VirtualFrame frame, RAbstractContainer container, Object namesArg,
                    @Cached("createNonShared(container)") VectorReuse vectorReuse) {
        RAbstractContainer result = vectorReuse.getMaterializedResult(container);
        return updateNames(result, asCharacter(frame, namesArg));
    }

    @Specialization(replaces = "updateNamesVector", guards = {"!isNotObject.execute(namesArg)"})
    protected RAbstractContainer updateNamesVectorGeneric(VirtualFrame frame, RAbstractVector container, Object namesArg,
                    @Cached("createNonSharedGeneric()") VectorReuse vectorReuse) {
        return updateNamesVector(frame, container, namesArg, vectorReuse);
    }

    // Combinations with NULL and fallback:

    @Specialization
    protected Object updateNames(RNull n, @SuppressWarnings("unused") RNull names) {
        return n;
    }

    @Specialization
    protected Object updateNames(@SuppressWarnings("unused") RNull n, @SuppressWarnings("unused") Object names) {
        return error(RError.Message.SET_ATTRIBUTES_ON_NULL);
    }

    @Fallback
    protected Object doOthers(@SuppressWarnings("unused") Object target, @SuppressWarnings("unused") Object names) {
        throw error(Message.NAMES_NONVECTOR);
    }

    protected RAbstractContainer updateNames(RAbstractContainer result, Object namesArg) {
        if (namesArg == RNull.instance) {
            if (getDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDimNode = insert(GetDimAttributeNode.create());
            }
            if (removeDimNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                removeDimNamesNode = insert(RemoveFixedAttributeNode.createDimNames());
            }
            int[] dims = getDimNode.getDimensions(result);
            if (dims != null && dims.length == 1) {
                removeDimNamesNode.execute(result);
            } else {
                result.setNames(null);
            }
            return result;
        }
        if (setNamesAttributeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setNamesAttributeNode = insert(SetNamesAttributeNode.create());
        }
        setNamesAttributeNode.setAttr(result, namesArg);
        return result;
    }

    private Object asCharacter(VirtualFrame frame, Object namesArg) {
        if (frame == null) {
            // TODO: to suppor the executeStringVector w/o frame, should be removed eventually
            return namesArg;
        }
        // The SetNamesAttribute will handle RNull accordingly
        if (namesArg == RNull.instance) {
            return namesArg;
        }
        // The value should be converted using the "as.character" function
        // Note: the SetNamesAttribute will do internal coercion to character vector, in case the
        // "as.character" function returns non character
        RArgsValuesAndNames args = new RArgsValuesAndNames(new Object[]{namesArg}, ArgumentsSignature.empty(1));
        return getAsCharacterCall().call(frame, getAsCharacterFunction(), args);
    }

    private RFunction getAsCharacterFunction() {
        if (asCharacterFunction == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object value = REnvironment.baseEnv(RContext.getInstance()).findFunction("as.character");
            if (!(value instanceof RFunction)) {
                throw error(Message.GENERIC, "Could not find function 'as.character' in the base environment.");
            }
            asCharacterFunction = (RFunction) value;
        }
        return asCharacterFunction;
    }

    public RExplicitCallNode getAsCharacterCall() {
        if (asCharacterCall == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            asCharacterCall = insert(RExplicitCallNode.create());
        }
        return asCharacterCall;
    }

    @TruffleBoundary
    private static void removeAllAttributes(RAttributable namesArg) {
        namesArg.removeAllAttributes();
    }

    /**
     * Simulates what "as.character" would do, which is to strip the attributes. Note that we do not
     * actually convert to a character vector here as the {@link SetNamesAttributeNode} will do that
     * later for us anyway.
     *
     * TODO: we also copy the names vector here. This should not be necessary, but some tests fail.
     * This should be diagnosed.
     */
    protected abstract static class StripAttributes extends Node {
        @Child HasAttributesNode hasAttributes = HasAttributesNode.create();

        public abstract Object execute(Object names);

        @Specialization
        protected Object doStrings(RAbstractStringVector names,
                        @Exclusive @Cached("createClassProfile()") ValueProfile namesProfile) {
            // always copy string vectors, see the comment on this class
            return namesProfile.profile(names).copyDropAttributes();
        }

        // remove attributes from other types

        @Specialization(guards = {"hasAttributes.execute(names)", "!isStringVector(names)"})
        protected Object doVectorsWithAttrs(RAbstractVector names,
                        @Exclusive @Cached("createClassProfile()") ValueProfile namesProfile) {
            return namesProfile.profile(names).copyDropAttributes();
        }

        @Specialization(guards = {"hasAttributes.execute(names)", "!isMaterializedVector(names)"})
        protected Object doOtherAttributableWithAttrs(RSharingAttributeStorage names,
                        @Exclusive @Cached("createClassProfile()") ValueProfile namesProfile) {
            RAttributable namesArg = namesProfile.profile(names).copy();
            removeAllAttributes(namesArg);
            return namesArg;
        }

        // just forward objects without attributes

        @Specialization(guards = {"!hasAttributes.execute(names)", "!isStringVector(names)"})
        protected Object doOthers(Object names) {
            return names;
        }

        protected static boolean isMaterializedVector(Object names) {
            return names instanceof RMaterializedVector;
        }

        protected static boolean isShareable(Object o) {
            return RSharingAttributeStorage.isShareable(o);
        }

        protected static boolean isStringVector(Object names) {
            return names instanceof RAbstractStringVector;
        }
    }
}
