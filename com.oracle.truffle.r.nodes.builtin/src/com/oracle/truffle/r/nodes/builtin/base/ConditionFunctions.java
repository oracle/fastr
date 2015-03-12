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

import static com.oracle.truffle.r.runtime.ConditionsSupport.*;

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

        protected enum NAMES {
            name,
            exit,
            handler,
            description,
            test,
            interactive;
        }

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
                return ConditionsSupport.createHandlers((RStringVector) classesObj, (RList) handlersObj, parentEnv, target, calling);
            }
        }

    }

    @RBuiltin(name = ".resetCondHands", kind = RBuiltinKind.INTERNAL, parameterNames = {"stack"})
    public abstract static class ResetCondHands extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected RNull resetCondHands(Object stack) {
            controlVisibility();
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = ".addRestart", kind = RBuiltinKind.INTERNAL, parameterNames = "restart")
    public abstract static class AddRestart extends Adapter {
        @Specialization
        protected Object addRestart(RList restart) {
            controlVisibility();
            addRestart(restart);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".getRestart", kind = RBuiltinKind.INTERNAL, parameterNames = "restart")
    public abstract static class GetRestart extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected int getRestart(Object index) {
            controlVisibility();
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = ".invokeRestart", kind = RBuiltinKind.INTERNAL, parameterNames = {"restart", "args"})
    public abstract static class InvokeRestart extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object getRestart(Object restart, Object args) {
            controlVisibility();
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = ".signalCondition", kind = RBuiltinKind.INTERNAL, parameterNames = {"condition", "msg", "call"})
    public abstract static class SignalCondition extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object signalCondition(Object condition, Object msg, Object call) {
            controlVisibility();
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = "geterrmessage", kind = RBuiltinKind.INTERNAL, parameterNames = {})
    public abstract static class Geterrmessage extends Adapter {
        @Specialization
        protected Object geterrmessage() {
            controlVisibility();
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = "seterrmessage", kind = RBuiltinKind.INTERNAL, parameterNames = {})
    public abstract static class Seterrmessage extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object seterrmessage(RAbstractStringVector msg) {
            controlVisibility();
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = ".dfltWarn", kind = RBuiltinKind.INTERNAL, parameterNames = {"message", "call"})
    public abstract static class DfltWarn extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object signalCondition(Object message, Object call) {
            controlVisibility();
            throw RInternalError.unimplemented();
        }
    }
}
