/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARGUMENT;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = "dump", visibility = OFF, kind = INTERNAL, parameterNames = {"list", "file", "envir", "opts", "evaluate"}, behavior = IO)
public abstract class Dump extends RBuiltinNode.Arg5 {

    @Child protected Helper helper;

    static {
        Casts casts = new Casts(Dump.class);
        casts.arg("list").mustNotBeMissing().mustBe(Predef.stringValue()).asStringVector();
        casts.arg("file").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();
        casts.arg("envir").mustNotBeMissing().mustBe(REnvironment.class, INVALID_ARGUMENT, "envir");
        casts.arg("opts").mustNotBeMissing().asIntegerVector().findFirst();
        casts.arg("evaluate").mustNotBeMissing().asLogicalVector().findFirst().map(Predef.toBoolean());
    }

    @Specialization
    protected Object dump(VirtualFrame frame, RAbstractStringVector x, int file, REnvironment envir, int opts, boolean evaluate) {
        if (helper == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            helper = insert(new Helper());
        }
        int retrieved = 0;
        String[] retrievedNames = new String[x.getLength()];
        Object[] objects = new Object[x.getLength()];
        for (int i = 0; i < x.getLength(); i++) {
            String name = x.getDataAt(i);
            objects[i] = helper.getAndCheck(frame, name, envir, evaluate);
            if (objects[i] != null) {
                retrievedNames[retrieved++] = name;
            }
        }
        writeToConnection(deparse(x, objects, prepareOpts(opts, evaluate)), file);
        return RDataFactory.createStringVector(Arrays.copyOfRange(retrievedNames, 0, retrieved), RDataFactory.COMPLETE_VECTOR);
    }

    @TruffleBoundary
    private static String deparse(RAbstractStringVector x, Object[] objects, int opts) {
        assert x.getLength() == objects.length;
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] != null) {
                if (i != 0) {
                    code.append(System.lineSeparator());
                }
                code.append(x.getDataAt(i)).append(" <-").append(System.lineSeparator()).append(RDeparse.deparse(objects[i], RDeparse.DEFAULT_CUTOFF, true, opts, -1));
            }
        }
        return code.toString();
    }

    @TruffleBoundary
    private void writeToConnection(String code, int file) {
        try (RConnection openConn = RConnection.fromIndex(file).forceOpen("wt")) {
            openConn.writeString(code, true);
        } catch (IOException ex) {
            throw error(RError.Message.GENERIC, ex.getMessage());
        }
    }

    private static int prepareOpts(int opts, boolean evaluate) {
        return evaluate ? opts : opts | RDeparse.DELAYPROMISES;
    }

    private static final class Helper extends RBaseNode {

        @Child private PromiseHelperNode promiseHelper;

        @CompilationFinal private boolean firstExecution = true;

        protected void unknownObject(String x) {
            warning(RError.Message.UNKNOWN_OBJECT, x);
        }

        protected Object checkPromise(VirtualFrame frame, Object r, String identifier) {
            if (r instanceof RPromise) {
                if (firstExecution) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    firstExecution = false;
                    return ReadVariableNode.evalPromiseSlowPathWithName(identifier, frame, (RPromise) r);
                }
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                }
                return promiseHelper.evaluate(frame, (RPromise) r);
            } else {
                return r;
            }
        }

        protected Object getAndCheck(VirtualFrame frame, String x, REnvironment env, boolean evaluatePromise) {
            Object obj;
            if (evaluatePromise) {
                obj = checkPromise(frame, env.get(x), x);
            } else {
                obj = env.get(x);
            }
            if (obj != null) {
                return obj;
            } else {
                unknownObject(x);
                return null;
            }
        }
    }
}
