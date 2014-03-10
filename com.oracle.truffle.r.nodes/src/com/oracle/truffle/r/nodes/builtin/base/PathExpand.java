package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("path.expand")
public abstract class PathExpand extends RBuiltinNode {

    private static String userHome;

    @Specialization
    public Object doPathExapnd(RAbstractStringVector vec) {
        String[] results = new String[vec.getLength()];
        for (int i = 0; i < results.length; i++) {
            String path = check(vec.getDataAt(i));
            results[i] = path;
        }
        return RDataFactory.createStringVector(results, RDataFactory.COMPLETE_VECTOR);
    }

    private static String userHome() {
        if (userHome == null) {
            userHome = System.getProperty("user.home");
        }
        return userHome;
    }

    static String check(String path) {
        if (path.charAt(0) == '~') {
            return userHome() + path.substring(1);
        } else {
            return path;
        }
    }

}
