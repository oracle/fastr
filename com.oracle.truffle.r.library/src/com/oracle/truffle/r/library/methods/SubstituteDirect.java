/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.methods;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RList2EnvNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RSubstitute;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class SubstituteDirect extends RExternalBuiltinNode.Arg2 {

    static {
        Casts.noCasts(SubstituteDirect.class);
    }

    @Specialization
    @TruffleBoundary
    protected static Object substituteDirect(Object object, REnvironment env) {
        if (object instanceof RLanguage) {
            RLanguage lang = (RLanguage) object;
            return RASTUtils.createLanguageElement(RSubstitute.substitute(env, lang.getRep()));
        } else {
            return object;
        }
    }

    @Specialization
    @TruffleBoundary
    protected static Object substituteDirect(Object object, RList list,
                    @Cached("new()") RList2EnvNode list2Env) {
        REnvironment env = RDataFactory.createNewEnv(null);
        env.setParent(REnvironment.baseEnv());
        list2Env.execute(list, env);
        return substituteDirect(object, env);
    }

    @Fallback
    protected Object substituteDirect(@SuppressWarnings("unused") Object object, @SuppressWarnings("unused") Object env) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }
}
