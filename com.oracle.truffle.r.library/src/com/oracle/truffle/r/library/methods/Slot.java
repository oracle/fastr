/*
 * Copyright (c) 1995-2013, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.library.methods;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.nodes.access.HasSlotNode;
import com.oracle.truffle.r.nodes.access.UpdateSlotNode;
import com.oracle.truffle.r.nodes.access.UpdateSlotNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNode;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;

// Transcribed from src/library/methods/src/slot.c

public class Slot {

    private static void addSlotCasts(Casts casts) {
        casts.arg(1, "name").defaultError(RError.Message.SLOT_INVALID_TYPE_OR_LEN).mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst().mustBe(Predef.lengthGt(0),
                        RError.Message.ZERO_LENGTH_VARIABLE);
    }

    public abstract static class R_getSlot extends RExternalBuiltinNode.Arg2 {

        @Child private AccessSlotNode accessSlotNode = AccessSlotNodeGen.create(false);
        @Child private CastToAttributableNode castAttributable = CastToAttributableNodeGen.create(true, true, true);

        static {
            addSlotCasts(new Casts(R_getSlot.class));
        }

        protected static String getInternedName(String name) {
            return Utils.intern(name);
        }

        @Specialization(guards = {"name.equals(cachedInternedName)"})
        protected Object getSlotCached(Object object, @SuppressWarnings("unused") String name,
                        @Cached("getInternedName(name)") String cachedInternedName) {
            return accessSlotNode.executeAccess(castAttributable.executeObject(object), cachedInternedName);
        }

        @Specialization(replaces = "getSlotCached")
        protected Object getSlot(Object object, String name) {
            return accessSlotNode.executeAccess(castAttributable.executeObject(object), getInternedName(name));
        }
    }

    public abstract static class R_setSlot extends RExternalBuiltinNode.Arg3 {

        @Child private UpdateSlotNode updateSlotNode = UpdateSlotNodeGen.create();
        @Child private CastToAttributableNode castAttributable = CastToAttributableNodeGen.create(true, true, true);

        static {
            addSlotCasts(new Casts(R_setSlot.class));
        }

        protected static String getInternedName(String name) {
            return Utils.intern(name);
        }

        @Specialization(guards = {"name.equals(cachedInternedName)"})
        protected Object setSlotCached(Object object, @SuppressWarnings("unused") String name, Object value,
                        @Cached("getInternedName(name)") String cachedInternedName) {
            return updateSlotNode.executeUpdate(castAttributable.executeObject(object), cachedInternedName, value);
        }

        @Specialization(replaces = "setSlotCached")
        protected Object setSlot(Object object, String name, Object value) {
            return updateSlotNode.executeUpdate(castAttributable.executeObject(object), getInternedName(name), value);
        }
    }

    public abstract static class R_hasSlot extends RExternalBuiltinNode.Arg2 {

        @Child private HasSlotNode hasSlotNode;

        static {
            addSlotCasts(new Casts(R_hasSlot.class));
        }

        protected static String getInternedName(String name) {
            return Utils.intern(name);
        }

        @Specialization(guards = {"name.equals(cachedInternedName)"})
        protected Object hasSlotCached(Object object, @SuppressWarnings("unused") String name,
                        @Cached("getInternedName(name)") String cachedInternedName) {
            if (hasSlotNode == null) {
                CompilerDirectives.transferToInterpreter();
                hasSlotNode = insert(HasSlotNode.create(false));
            }
            return RRuntime.asLogical(hasSlotNode.executeAccess(object, cachedInternedName));
        }

        @Specialization(replaces = "hasSlotCached")
        protected Object hasSlot(Object object, String name) {
            if (hasSlotNode == null) {
                CompilerDirectives.transferToInterpreter();
                hasSlotNode = insert(HasSlotNode.create(false));
            }
            return RRuntime.asLogical(hasSlotNode.executeAccess(object, name));
        }
    }

}
