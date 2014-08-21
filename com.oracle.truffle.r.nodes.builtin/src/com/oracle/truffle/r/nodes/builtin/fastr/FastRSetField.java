/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import java.lang.reflect.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "fastr.setfield", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"field", "value"})
public abstract class FastRSetField extends RInvisibleBuiltinNode {

    @Specialization
    protected RNull setField(RAbstractStringVector vec, Object value) {
        controlVisibility();
        String qualFieldName = vec.getDataAt(0);
        int lx = qualFieldName.lastIndexOf('.');
        String simpleName = qualFieldName.substring(lx + 1);
        String className = qualFieldName.substring(0, lx);
        if (!className.startsWith("com")) {
            className = "com.oracle.truffle.r." + className;
        }
        try {
            Class<?> klass = Class.forName(className);
            Field field = klass.getDeclaredField(simpleName);
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            switch (fieldType.getSimpleName()) {
                case "boolean":
                    if (value instanceof Byte) {
                        field.setBoolean(null, RRuntime.fromLogical((byte) value));
                    } else {
                        error(qualFieldName);
                    }
            }
        } catch (Exception ex) {
            throw RError.error(Message.GENERIC, ex.getMessage());
        }
        return RNull.instance;
    }

    private static void error(String fieldName) throws RError {
        throw RError.error(Message.GENERIC, "value is wrong type for %s", fieldName);
    }

}
