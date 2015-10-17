/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2013, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.methods;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.nodes.access.UpdateSlotNode;
import com.oracle.truffle.r.nodes.access.UpdateSlotNodeGen;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

// Transcribed from src/library/methods/slot.c

public class Slot {

    public abstract static class R_getSlot extends RExternalBuiltinNode.Arg2 {

        @Child AccessSlotNode accessSlotNode = AccessSlotNodeGen.create(null, null);

        @Specialization(guards = "nameVec.getLength() == 1")
        protected Object getSlot(Object object, RAbstractStringVector nameVec) {
            return accessSlotNode.executeAccess(object, nameVec.getDataAt(0));
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object getSlot(Object object, Object nameVec) {
            throw RError.error(this, RError.Message.GENERIC, "invalid type or length for slot name");
        }

    }

    public abstract static class R_setSlot extends RExternalBuiltinNode.Arg3 {

        @Child UpdateSlotNode updateSlotNode = UpdateSlotNodeGen.create(null, null, null);

        @Specialization(guards = "nameVec.getLength() == 1")
        protected Object setSlot(Object object, RAbstractStringVector nameVec, Object value) {
            return updateSlotNode.executeUpdate(object, nameVec.getDataAt(0), value);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object setSlot(Object object, Object name, Object value) {
            throw RError.error(this, RError.Message.GENERIC, "invalid type or length for slot name");
        }

    }

}
