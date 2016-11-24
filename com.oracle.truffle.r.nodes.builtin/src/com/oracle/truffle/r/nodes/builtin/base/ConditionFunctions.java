/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RErrorHandling.getHandlerStack;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Condition handling. Derived from GnUR src/main/errors.c
 */
public class ConditionFunctions {

    public abstract static class EvalAdapter extends RBuiltinNode {
        @Child private PromiseHelperNode promiseHelper;

        protected PromiseHelperNode initPromiseHelper() {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            return promiseHelper;
        }
    }

    @RBuiltin(name = ".addCondHands", visibility = OFF, kind = INTERNAL, parameterNames = {"classes", "handlers", "parentenv", "target", "calling"}, behavior = COMPLEX)
    public abstract static class AddCondHands extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("classes").allowNull().mustBe(stringValue()).asStringVector();
            casts.arg("handlers").allowNull().mustBe(instanceOf(RList.class));
            casts.arg("calling").asLogicalVector().findFirst();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isRNull(classes) || isRNull(handlers)")
        @TruffleBoundary
        protected Object addCondHands(Object classes, Object handlers, Object parentEnv, Object target, byte calling) {
            return getHandlerStack();
        }

        @Specialization
        @TruffleBoundary
        protected Object addCondHands(RAbstractStringVector classes, RList handlers, REnvironment parentEnv, Object target, byte calling) {
            if (classes.getLength() != handlers.getLength()) {
                throw RError.error(this, RError.Message.BAD_HANDLER_DATA);
            }
            return RErrorHandling.createHandlers(classes, handlers, parentEnv, target, calling);
        }

    }

    @RBuiltin(name = ".resetCondHands", visibility = OFF, kind = INTERNAL, parameterNames = {"stack"}, behavior = COMPLEX)
    public abstract static class ResetCondHands extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        protected RNull resetCondHands(Object stack) {
            // TODO
            throw RInternalError.unimplemented();
        }
    }

    public abstract static class RestartAdapter extends RBuiltinNode {
        protected void checkLength(RList restart) {
            if (restart.getLength() < 2) {
                throw RError.error(this, RError.Message.BAD_RESTART);
            }
        }

        protected void restart(CastBuilder casts) {
            casts.arg("restart").mustBe(instanceOf(RList.class), RError.Message.BAD_RESTART);
        }

    }

    @RBuiltin(name = ".addRestart", kind = INTERNAL, parameterNames = "restart", behavior = COMPLEX)
    public abstract static class AddRestart extends RestartAdapter {
        @Override
        public void createCasts(CastBuilder casts) {
            restart(casts);
        }

        @Specialization
        protected Object addRestart(RList restart) {
            checkLength(restart);
            RErrorHandling.addRestart(restart);
            return RNull.instance;
        }

    }

    @RBuiltin(name = ".getRestart", kind = INTERNAL, parameterNames = "restart", behavior = COMPLEX)
    public abstract static class GetRestart extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("restart").asIntegerVector().findFirst();
        }

        @Specialization
        protected Object getRestart(int index) {
            Object result = RErrorHandling.getRestart(index);
            return result;
        }
    }

    @RBuiltin(name = ".invokeRestart", kind = INTERNAL, parameterNames = {"restart", "args"}, behavior = COMPLEX)
    public abstract static class InvokeRestart extends RestartAdapter {
        @Override
        public void createCasts(CastBuilder casts) {
            restart(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RNull invokeRestart(RList restart, Object args) {
            checkLength(restart);
            if (RErrorHandling.invokeRestart(restart, args) == null) {
                throw RError.error(this, RError.Message.RESTART_NOT_ON_STACK);
            } else {
                return RNull.instance; // not reached
            }
        }

    }

    @RBuiltin(name = ".signalCondition", kind = INTERNAL, parameterNames = {"condition", "msg", "call"}, behavior = COMPLEX)
    public abstract static class SignalCondition extends RBuiltinNode {
        @Specialization
        protected RNull signalCondition(RList condition, RAbstractStringVector msg, Object call) {
            RErrorHandling.signalCondition(condition, msg.getDataAt(0), call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "geterrmessage", kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class Geterrmessage extends RBuiltinNode {
        @Specialization
        protected String geterrmessage() {
            return RErrorHandling.geterrmessage();
        }
    }

    @RBuiltin(name = "seterrmessage", visibility = OFF, kind = INTERNAL, parameterNames = "msg", behavior = COMPLEX)
    public abstract static class Seterrmessage extends RBuiltinNode {
        @Override
        public void createCasts(CastBuilder casts) {
            casts.arg("msg").defaultError(RError.Message.ERR_MSG_MUST_BE_STRING).mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        }

        @Specialization
        protected RNull seterrmessage(String msg) {
            RErrorHandling.seterrmessage(msg);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".dfltWarn", kind = INTERNAL, parameterNames = {"message", "call"}, behavior = COMPLEX)
    public abstract static class DfltWarn extends RBuiltinNode {
        @Override
        public void createCasts(CastBuilder casts) {
            casts.arg("message").defaultError(RError.Message.ERR_MSG_BAD).mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        }

        @Specialization
        protected RNull dfltWarn(String msg, Object call) {
            RErrorHandling.dfltWarn(msg, call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".dfltStop", kind = INTERNAL, parameterNames = {"message", "call"}, behavior = COMPLEX)
    public abstract static class DfltStop extends RBuiltinNode {
        @Override
        public void createCasts(CastBuilder casts) {
            casts.arg("message").defaultError(RError.Message.ERR_MSG_BAD).mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        }

        @Specialization
        protected Object dfltStop(String message, Object call) {
            RErrorHandling.dfltStop(message, call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "printDeferredWarnings", visibility = OFF, kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class PrintDeferredWarnings extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RNull printDeferredWarnings() {
            RErrorHandling.printDeferredWarnings();
            return RNull.instance;
        }
    }
}
