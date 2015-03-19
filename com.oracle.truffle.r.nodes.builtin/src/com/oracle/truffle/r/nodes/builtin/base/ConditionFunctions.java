/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RErrorHandling.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Condition handling. Derived from GnUR src/main/errors.c
 */
public class ConditionFunctions {
    public abstract static class Adapter extends RInvisibleBuiltinNode {
        protected final BranchProfile errorProfile = BranchProfile.create();

    }

    public abstract static class EvalAdapter extends Adapter {
        @Child private PromiseHelperNode promiseHelper;

        protected PromiseHelperNode initPromiseHelper() {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            return promiseHelper;
        }

    }

    @RBuiltin(name = ".addCondHands", kind = RBuiltinKind.INTERNAL, parameterNames = {"classes", "handlers", "parentenv", "target", "calling"})
    public abstract static class AddCondHands extends Adapter {
        private final ConditionProfile nullArgs = ConditionProfile.createBinaryProfile();

        @SuppressWarnings("unused")
        @Specialization
        protected Object addCondHands(Object classesObj, Object handlersObj, REnvironment parentEnv, Object target, byte calling) {
            controlVisibility();
            if (nullArgs.profile(classesObj == RNull.instance || handlersObj == RNull.instance)) {
                return getHandlerStack();
            }
            RStringVector classes;
            RList handlers;
            if (!(classesObj instanceof RStringVector && handlersObj instanceof RList && ((RStringVector) classesObj).getLength() == ((RList) handlersObj).getLength())) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.BAD_HANDLER_DATA);
            } else {
                return RErrorHandling.createHandlers((RStringVector) classesObj, (RList) handlersObj, parentEnv, target, calling);
            }
        }

    }

    @RBuiltin(name = ".resetCondHands", kind = RBuiltinKind.INTERNAL, parameterNames = {"stack"})
    public abstract static class ResetCondHands extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected RNull resetCondHands(Object stack) {
            controlVisibility();
            // TODO
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = ".addRestart", kind = RBuiltinKind.INTERNAL, parameterNames = "restart")
    public abstract static class AddRestart extends Adapter {
        @Specialization
        protected Object addRestart(RList restart) {
            controlVisibility();
            RErrorHandling.addRestart(restart);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".getRestart", kind = RBuiltinKind.INTERNAL, parameterNames = "restart")
    public abstract static class GetRestart extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected int getRestart(Object index) {
            controlVisibility();
            // TODO
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = ".invokeRestart", kind = RBuiltinKind.INTERNAL, parameterNames = {"restart", "args"})
    public abstract static class InvokeRestart extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object getRestart(Object restart, Object args) {
            controlVisibility();
            // TODO
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = ".signalCondition", kind = RBuiltinKind.INTERNAL, parameterNames = {"condition", "msg", "call"})
    public abstract static class SignalCondition extends Adapter {
        @Specialization
        protected RNull signalCondition(RList condition, RAbstractStringVector msg, Object call) {
            controlVisibility();
            RErrorHandling.signalCondition(condition, msg.getDataAt(0), call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "geterrmessage", kind = RBuiltinKind.INTERNAL, parameterNames = {})
    public abstract static class Geterrmessage extends Adapter {
        @Specialization
        protected String geterrmessage() {
            controlVisibility();
            return RErrorHandling.geterrmessage();
        }
    }

    @RBuiltin(name = "seterrmessage", kind = RBuiltinKind.INTERNAL, parameterNames = {})
    public abstract static class Seterrmessage extends Adapter {
        @Specialization
        protected RNull seterrmessage(RAbstractStringVector msg) {
            controlVisibility();
            RErrorHandling.seterrmessage(msg.getDataAt(0));
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".dfltWarn", kind = RBuiltinKind.INTERNAL, parameterNames = {"message", "call"})
    public abstract static class DfltWarn extends Adapter {
        @Specialization
        protected RNull dfltWarn(RAbstractStringVector msg, Object call) {
            controlVisibility();
            RErrorHandling.dfltWarn(msg.getDataAt(0), call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".dfltStop", kind = RBuiltinKind.INTERNAL, parameterNames = {"message", "call"})
    public abstract static class DfltStop extends Adapter {
        @Specialization
        protected Object dfltStop(RAbstractStringVector message, Object call) {
            controlVisibility();
            RErrorHandling.dfltStop(message.getDataAt(0), call);
            return RNull.instance;
        }
    }
}
