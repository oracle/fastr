/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2013, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.methods;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.nodes.access.UpdateSlotNode;
import com.oracle.truffle.r.nodes.access.UpdateSlotNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNode;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;

// Transcribed from src/library/methods/src/slot.c

public class Slot {

    public abstract static class R_getSlot extends RExternalBuiltinNode.Arg2 {

        @Child private AccessSlotNode accessSlotNode = AccessSlotNodeGen.create(false);
        @Child private CastToAttributableNode castAttributable = CastToAttributableNodeGen.create(true, true, true);

        static {
            Casts casts = new Casts(R_getSlot.class);
            casts.arg(1, "name").defaultError(RError.Message.GENERIC, "invalid type or length for slot name").mustBe(stringValue()).asStringVector().mustBe(
                            singleElement()).findFirst().mustBe(Predef.lengthGt(0), RError.Message.ZERO_LENGTH_VARIABLE);
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
            Casts casts = new Casts(R_setSlot.class);
            casts.arg(1, "name").defaultError(RError.Message.GENERIC, "invalid type or length for slot name").mustBe(stringValue()).asStringVector().mustBe(
                            singleElement()).findFirst().mustBe(Predef.lengthGt(0), RError.Message.ZERO_LENGTH_VARIABLE);
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
}
