package com.oracle.truffle.r.nodes.builtin.fastr;

import java.lang.reflect.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "fastr.setfield", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"field", "value"})
public abstract class FastRSetField extends RInvisibleBuiltinNode {

    @Specialization
    RNull setField(RAbstractStringVector vec, Object value) {
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
