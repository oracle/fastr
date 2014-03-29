/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Encapsulates all the builtins related to R environments as nested static classes.
 */
public class EnvFunctions {

    @RBuiltin("emptyenv")
    public abstract static class EmptyEnv extends RBuiltinNode {

        @Specialization
        public REnvironment emptyenv() {
            return RRuntime.EMPTY_ENV;
        }
    }

    @RBuiltin({"globalenv"})
    public abstract static class GlobalEnv extends RBuiltinNode {

        @Specialization
        public Object globalenv() {
            return RRuntime.GLOBAL_ENV;
        }
    }

    @RBuiltin("baseenv")
    public abstract static class BaseEnv extends RBuiltinNode {

        @Specialization
        public Object baseenv() {
            return RRuntime.BASE_ENV;
        }
    }

    @RBuiltin(".Internal.parent.env")
    public abstract static class ParentEnv extends RBuiltinNode {

        @Specialization
        public REnvironment parentenv(REnvironment env) {
            if (env == RRuntime.EMPTY_ENV) {
                throw RError.getGenericError(getEncapsulatingSourceSection(), "the empty environment has no parent");
            }
            return env.getParent();
        }

        @Generic
        public REnvironment parentenv(@SuppressWarnings("unused") Object x) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), "argument is not an environment");
        }
    }

    @RBuiltin("is.environment")
    public abstract static class IsEnvironment extends RBuiltinNode {

        @Specialization
        public byte isEnvironment(Object env) {
            return env instanceof REnvironment ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(".Internal.environment")
    public abstract static class Environment extends RBuiltinNode {

        @Specialization
        public Object environment(@SuppressWarnings("unused") RNull x) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), "environment(NULL) is not implemented");
        }

        @Specialization
        public Object environment(@SuppressWarnings("unused") RFunction func) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), "environment(func) is not implemented");
        }

        @Generic
        public RNull environment(@SuppressWarnings("unused") Object x) {
            // Not an error according to GnuR
            return RNull.instance;
        }
    }

    @RBuiltin(".Internal.environmentName")
    public abstract static class EnvironmentName extends RBuiltinNode {

        @Specialization
        public String environmentName(REnvironment env) {
            return env.getName();
        }

        @Generic
        public String environmentName(@SuppressWarnings("unused") Object env) {
            // Not an error according to GnuR
            return "";
        }
    }

    @RBuiltin("new.env")
    public abstract static class NewEnv extends RBuiltinNode {

        private static final Object[] PARAMETER_NAMES = new Object[]{"hash", "parent", "size"};

        @Override
        public Object[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RRuntime.LOGICAL_TRUE), ConstantNode.create(RMissing.instance), ConstantNode.create(29)};
        }

        @CreateCast("arguments")
        protected RNode[] castStatusArgument(RNode[] arguments) {
            // size argument is at index 2, and an int
            arguments[2] = CastIntegerNodeFactory.create(arguments[2], true, false);
            return arguments;
        }

        @Specialization
        @SuppressWarnings("unused")
        public REnvironment newEnv(byte hash, RMissing parent, int size) {
            // FIXME don't ignore hash parameter
            return new REnvironment(RRuntime.GLOBAL_ENV, REnvironment.UNNAMED, size);
        }

        @Specialization
        public REnvironment newEnv(@SuppressWarnings("unused") byte hash, REnvironment parent, int size) {
            // FIXME don't ignore hash parameter
            return new REnvironment(parent, REnvironment.UNNAMED, size);
        }
    }

    /**
     * TODO implement.
     */
    @RBuiltin(".Internal.parent.frame")
    public abstract static class ParentFrame extends RBuiltinNode {
        @Specialization
        public Object parentFrame(Object x) {
            return RNull.instance;
        }
    }

}
