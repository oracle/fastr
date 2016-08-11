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

import static com.oracle.truffle.r.runtime.RErrorHandling.getHandlerStack;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
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

        @SuppressWarnings("unused")
        @Specialization(guards = "isRNull(classes) || isRNull(handlers)")
        @TruffleBoundary
        protected Object addCondHands(Object classes, Object handlers, Object parentEnv, Object target, byte calling) {
            RContext.getInstance().setVisible(false);
            return getHandlerStack();
        }

        @Specialization(guards = "classes.getLength() == handlers.getLength()")
        @TruffleBoundary
        protected Object addCondHands(RAbstractStringVector classes, RList handlers, REnvironment parentEnv, Object target, byte calling) {
            RContext.getInstance().setVisible(false);
            return RErrorHandling.createHandlers(classes, handlers, parentEnv, target, calling);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object classesObj, Object handlersObj, Object parentEnv, Object target, byte calling) {
            throw RError.error(this, RError.Message.BAD_HANDLER_DATA);
        }
    }

    @RBuiltin(name = ".resetCondHands", visibility = OFF, kind = INTERNAL, parameterNames = {"stack"}, behavior = COMPLEX)
    public abstract static class ResetCondHands extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        protected RNull resetCondHands(Object stack) {
            RContext.getInstance().setVisible(false);
            // TODO
            throw RInternalError.unimplemented();
        }
    }

    public abstract static class RestartAdapter extends RBuiltinNode {
        public static boolean lengthok(Object restart) {
            return (restart instanceof RList) && ((RList) restart).getLength() >= 2;
        }

        protected RError badRestart() throws RError {
            throw RError.error(this, RError.Message.BAD_RESTART);
        }
    }

    @RBuiltin(name = ".addRestart", kind = INTERNAL, parameterNames = "restart", behavior = COMPLEX)
    public abstract static class AddRestart extends RestartAdapter {
        @Specialization
        protected Object addRestart(RList restart) {
            RErrorHandling.addRestart(restart);
            return RNull.instance;
        }

        @Specialization(guards = "lengthok(restart)")
        protected Object addRestart(@SuppressWarnings("unused") Object restart) {
            throw badRestart();
        }
    }

    @RBuiltin(name = ".getRestart", kind = INTERNAL, parameterNames = "restart", behavior = COMPLEX)
    public abstract static class GetRestart extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toInteger(0);
        }

        @Specialization
        protected Object getRestart(int index) {
            Object result = RErrorHandling.getRestart(index);
            return result;
        }
    }

    @RBuiltin(name = ".invokeRestart", kind = INTERNAL, parameterNames = {"restart", "args"}, behavior = COMPLEX)
    public abstract static class InvokeRestart extends RestartAdapter {
        @Specialization(guards = "lengthok(restart)")
        protected RNull invokeRestart(RList restart, Object args) {
            if (RErrorHandling.invokeRestart(restart, args) == null) {
                throw RError.error(this, RError.Message.RESTART_NOT_ON_STACK);
            } else {
                return RNull.instance; // not reached
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object invokeRestart(Object restart, Object args) {
            throw badRestart();
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
        @Specialization
        protected RNull seterrmessage(RAbstractStringVector msg) {
            RContext.getInstance().setVisible(false);
            RErrorHandling.seterrmessage(msg.getDataAt(0));
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".dfltWarn", kind = INTERNAL, parameterNames = {"message", "call"}, behavior = COMPLEX)
    public abstract static class DfltWarn extends RBuiltinNode {
        @Specialization
        protected RNull dfltWarn(RAbstractStringVector msg, Object call) {
            RErrorHandling.dfltWarn(msg.getDataAt(0), call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".dfltStop", kind = INTERNAL, parameterNames = {"message", "call"}, behavior = COMPLEX)
    public abstract static class DfltStop extends RBuiltinNode {
        @Specialization
        protected Object dfltStop(RAbstractStringVector message, Object call) {
            RErrorHandling.dfltStop(message.getDataAt(0), call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "printDeferredWarnings", visibility = OFF, kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class PrintDeferredWarnings extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RNull printDeferredWarnings() {
            RContext.getInstance().setVisible(false);
            RErrorHandling.printDeferredWarnings();
            return RNull.instance;
        }
    }
}
