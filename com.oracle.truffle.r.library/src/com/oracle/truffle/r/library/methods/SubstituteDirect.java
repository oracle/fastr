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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RList2EnvNode;
import com.oracle.truffle.r.runtime.RError;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_LIST_FOR_SUBSTITUTION;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import com.oracle.truffle.r.runtime.RSubstitute;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class SubstituteDirect extends RExternalBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(SubstituteDirect.class);
        casts.arg(1).defaultError(SHOW_CALLER, INVALID_LIST_FOR_SUBSTITUTION).mustBe(instanceOf(RAbstractListVector.class).or(instanceOf(REnvironment.class)));
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

    @Specialization(guards = {"list.getNames() == null || list.getNames().getLength() == 0"})
    @TruffleBoundary
    protected static Object substituteDirect(Object object, @SuppressWarnings("unused") RList list) {
        return substituteDirect(object, createNewEnvironment());
    }

    @Specialization(guards = {"list.getNames() != null", "list.getNames().getLength() > 0"})
    @TruffleBoundary
    protected static Object substituteDirect(Object object, RList list,
                    @Cached("createList2EnvNode()") RList2EnvNode list2Env) {
        return substituteDirect(object, createEnvironment(list, list2Env));
    }

    @Fallback
    protected Object substituteDirect(@SuppressWarnings("unused") Object object, @SuppressWarnings("unused") Object env) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

    @TruffleBoundary
    public static REnvironment createNewEnvironment() {
        return createEnvironment(null, null);
    }

    @TruffleBoundary
    public static REnvironment createEnvironment(RList list, RList2EnvNode list2Env) {
        REnvironment env = RDataFactory.createNewEnv(null);
        env.setParent(REnvironment.baseEnv());
        if (list2Env != null) {
            list2Env.execute(list, env);
        }
        return env;
    }

    protected static RList2EnvNode createList2EnvNode() {
        return new RList2EnvNode(true);
    }
}
