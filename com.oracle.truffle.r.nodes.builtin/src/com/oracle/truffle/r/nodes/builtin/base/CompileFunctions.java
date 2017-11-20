/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * FastR does not byte-compile, obviously, but these keep the package installation code happy.
 */

public class CompileFunctions {
    @RBuiltin(name = "compilePKGS", kind = INTERNAL, parameterNames = "enable", behavior = PURE)
    public abstract static class CompilePKGS extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(CompilePKGS.class);
            casts.arg("enable").asIntegerVector().findFirst(0);
        }

        @Specialization
        protected byte compilePKGS(@SuppressWarnings("unused") int enable) {
            return 0;
        }
    }

    @RBuiltin(name = "enableJIT", kind = INTERNAL, parameterNames = "level", behavior = PURE)
    public abstract static class EnableJIT extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(EnableJIT.class);
            casts.arg("level").asIntegerVector().findFirst(0);
        }

        @Specialization
        protected byte enableJIT(@SuppressWarnings("unused") int level) {
            return 0;
        }
    }

    @RBuiltin(name = "mkCode", kind = INTERNAL, parameterNames = {"bytes", "consts"}, behavior = PURE)
    @SuppressWarnings("unused")
    public abstract static class MkCode extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(MkCode.class);
            casts.arg("bytes"); // Looks like an int vector in R_registerBC
            casts.arg("consts"); // precise type not found yet
        }

        @Specialization
        protected Object mkCode(Object bytes, Object consts) {
            return RNull.instance;
            // RIntVector code = RDataFactory.createEmptyIntVector();
            // return RDataFactory.createPairList(code, consts, RNull.instance, SEXPTYPE.BCODESXP);
        }
    }

    @RBuiltin(name = "bcClose", kind = INTERNAL, parameterNames = {"forms", "body", "env"}, behavior = PURE)
    public abstract static class BcClose extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(BcClose.class);
            casts.arg("forms");
            casts.arg("body");
            casts.arg("env");
        }

        @Specialization
        @SuppressWarnings("unused")
        protected Object bcClose(Object forms, Object body, Object env) {
            return RNull.instance; // Body
        }
    }

    @RBuiltin(name = "is.builtin.internal", kind = INTERNAL, parameterNames = {"symbol"}, behavior = PURE)
    public abstract static class IsBuiltinInternal extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(IsBuiltinInternal.class);
            casts.arg("symbol").asStringVector().findFirst();
        }

        @Specialization
        protected boolean isBuiltinInternal(@SuppressWarnings("unused") String symbol) {
            return false;
        }
    }

    @RBuiltin(name = "disassemble", kind = INTERNAL, parameterNames = {"symbol"}, behavior = PURE)
    public abstract static class Disassemble extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(Disassemble.class);
            casts.arg("symbol").asStringVector().findFirst();
        }

        @Specialization
        protected boolean disassemble(@SuppressWarnings("unused") String symbol) {
            return false;
        }
    }

    @RBuiltin(name = "bcVersion", kind = INTERNAL, parameterNames = {}, behavior = PURE)
    @SuppressWarnings("unused")
    public abstract static class BcVersion extends RBuiltinNode.Arg0 {

        static {
            Casts casts = new Casts(BcVersion.class);
        }

        @Specialization
        protected int bcVersion() {
            return 10; // R_bcVersion in eval.c
        }
    }

    @RBuiltin(name = "load.from.file", kind = INTERNAL, parameterNames = {"file"}, behavior = PURE)
    public abstract static class LoadFromFile extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(LoadFromFile.class);
            casts.arg("file").asStringVector().findFirst();
        }

        @Specialization
        protected Object loadFromFile(@SuppressWarnings("unused") String file) {
            return RNull.instance;
        }
    }

    @RBuiltin(name = "save.to.file", kind = INTERNAL, parameterNames = {"content", "file", "ascii"}, behavior = PURE)
    public abstract static class SaveToFile extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(SaveToFile.class);
            // 'content' arg traced till "NewMakeLists" where it can be anything except WEAKREFSXP
            casts.arg("content");
            casts.arg("file").asStringVector().findFirst();
            casts.arg("ascii").asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        protected Object saveToFile(@SuppressWarnings("unused") Object content, @SuppressWarnings("unused") String file, @SuppressWarnings("unused") boolean ascii) {
            return RNull.instance;
        }
    }

    @RBuiltin(name = "growconst", kind = INTERNAL, parameterNames = {"constBuf"}, behavior = PURE)
    public abstract static class Growconst extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(Growconst.class);
            // RList required in do_growconst() in eval.c
            casts.arg("constBuf").mustNotBeMissing().mustNotBeNull().mustBe(RList.class);
        }

        @Specialization
        protected RList growconst(RList constBuf) {
            int n = constBuf.getLength();
            RList ret = (RList) constBuf.copyResized(n << 1, true);
            return ret;
        }
    }

    @RBuiltin(name = "putconst", kind = INTERNAL, parameterNames = {"constBuf", "constCount", "x"}, behavior = PURE)
    public abstract static class Putconst extends RBuiltinNode.Arg3 {

        @Child Identical identical = Identical.create();

        static {
            Casts casts = new Casts(Putconst.class);
            // RList required in do_putconst() in eval.c
            casts.arg("constBuf").mustNotBeMissing().mustNotBeNull().mustBe(RList.class);
            casts.arg("constCount").mustNotBeMissing().mustNotBeNull().asIntegerVector().findFirst();
            casts.arg("x");
        }

        @Specialization
        protected int putconst(RList constBuf, int constCount, Object x) {
            // constCount <= constBuf length
            // if x is present in constBuf within <0,constCindexount) return its index otherwise set
            // x as element of constBuf at constCount index and return constCount
            if (constCount < 0 || constCount > constBuf.getLength()) {
                throw RError.error(this, RError.Message.BAD_CONSTANT_COUNT);
            }
            for (int i = 0; i < constCount; i++) {
                Object y = constBuf.getDataAt(i);
                if (identical.executeByte(x, y, true, true, true, true, true, false) == RRuntime.LOGICAL_TRUE) {
                    return i;
                }
            }
            constBuf.setDataAt(constCount, x);
            return constCount;
        }
    }

    @RBuiltin(name = "getconst", kind = INTERNAL, parameterNames = {"constBuf", "n"}, behavior = PURE)
    public abstract static class Getconst extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(Getconst.class);
            casts.arg("constBuf").mustNotBeMissing().mustNotBeNull().mustBe(RList.class);
            casts.arg("n").mustNotBeMissing().mustNotBeNull().asIntegerVector().findFirst();
        }

        @Specialization
        protected RList getconst(RAbstractVector constBuf, int n) {
            // Return new vector of size n with content copied from constBuf
            if (n < 0 || n > constBuf.getLength()) {
                throw RError.error(this, RError.Message.BAD_CONSTANT_COUNT);
            }
            RList ret = (RList) constBuf.copyResized(n, false);
            return ret;
        }
    }

}
