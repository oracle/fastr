/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.interop.AccessForeignElementNode.ReadElementNode;
import com.oracle.truffle.r.runtime.interop.ToJavaStaticNodeGen.ExecuteMethodNodeGen;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
@ImportStatic(DSLConfig.class)
public abstract class ToJavaStaticNode extends RBaseNode {

    public static ToJavaStaticNode create() {
        return ToJavaStaticNodeGen.create();
    }

    public static ToJavaStaticNode getUncached() {
        return ToJavaStaticNodeGen.getUncached();
    }

    public abstract TruffleObject execute(Object obj);

    @Specialization(guards = {"isNonClassHostObject(obj)", "!interop.hasArrayElements(obj)"}, limit = "getInteropLibraryCacheSize()")
    protected TruffleObject doTruffleObject(TruffleObject obj,
                    @Cached("create()") ReadElementNode readClass,
                    @Cached("create()") ReadElementNode readName,
                    @Cached("create()") ExecuteMethodNode getClass,
                    @Cached("create()") ExecuteMethodNode getName,
                    @SuppressWarnings("unused") @CachedLibrary("obj") InteropLibrary interop,
                    @CachedLibrary(limit = "1") InteropLibrary nameInterop) {
        Env e = getRContext().getEnv();
        assert e.isHostLookupAllowed() && e.isHostObject(obj) && !(e.asHostObject(obj) instanceof Class);

        if (e.isHostLookupAllowed()) {
            Object gcf = readClass.execute(obj, "getClass");
            Object clazz = getClass.execute(gcf);
            Object cnf = readName.execute(clazz, "getName");
            try {
                String className = nameInterop.asString(getName.execute(cnf));
                return (TruffleObject) e.lookupHostSymbol(className);
            } catch (UnsupportedMessageException ex) {
                throw CompilerDirectives.shouldNotReachHere(ex);
            }
        }
        return null;
    }

    @Specialization(guards = {"isNonClassHostObject(obj)", "interop.hasArrayElements(obj)"}, limit = "getInteropLibraryCacheSize()")
    protected TruffleObject doTruffleObject(@SuppressWarnings("unused") TruffleObject obj,
                    @SuppressWarnings("unused") @CachedLibrary("obj") InteropLibrary interop) {
        return null;
    }

    @Specialization(guards = "isClassHostObject(obj)")
    public TruffleObject doClassObject(@SuppressWarnings("unused") Object obj) {
        return null;
    }

    @Specialization(guards = "!isHostObject(obj)")
    public TruffleObject doObject(@SuppressWarnings("unused") Object obj) {
        return null;
    }

    protected boolean isHostObject(Object object) {
        TruffleLanguage.Env env = getRContext().getEnv();
        return env.isHostObject(object);
    }

    protected boolean isClassHostObject(Object object) {
        TruffleLanguage.Env env = getRContext().getEnv();
        return env.isHostObject(object) && (env.asHostObject(object) instanceof Class);
    }

    protected boolean isNonClassHostObject(TruffleObject object) {
        TruffleLanguage.Env env = getRContext().getEnv();
        return env.isHostObject(object) && !(env.asHostObject(object) instanceof Class);
    }

    @GenerateUncached
    @ImportStatic(DSLConfig.class)
    protected abstract static class ExecuteMethodNode extends Node {
        static ExecuteMethodNode create() {
            return ExecuteMethodNodeGen.create();
        }

        abstract Object execute(Object classMethod);

        @Specialization(limit = "getInteropLibraryCacheSize()")
        Object exec(TruffleObject obj,
                        @CachedLibrary("obj") InteropLibrary interop) {
            try {
                return interop.execute(obj);
            } catch (InteropException ex) {
                CompilerDirectives.transferToInterpreter();
                throw RError.interopError(RError.findParentRBase(this), ex, obj);
            }
        }
    }

}
