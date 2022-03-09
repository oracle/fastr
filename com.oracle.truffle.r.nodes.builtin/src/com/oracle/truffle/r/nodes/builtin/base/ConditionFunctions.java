/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

/**
 * Condition handling. Derived from GnUR src/main/errors.c
 */
public class ConditionFunctions {

    @RBuiltin(name = ".addCondHands", visibility = OFF, kind = INTERNAL, parameterNames = {"classes", "handlers", "parentenv", "target", "calling"}, behavior = COMPLEX)
    public abstract static class AddCondHands extends RBuiltinNode.Arg5 {

        static {
            Casts casts = new Casts(AddCondHands.class);
            casts.arg("classes").allowNull().mustBe(stringValue()).asStringVector();
            casts.arg("handlers").allowNull().mustBe(instanceOf(RList.class));
            casts.arg("calling").asLogicalVector().findFirst();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isRNull(classes) || isRNull(handlers)")
        @TruffleBoundary
        protected Object addCondHands(Object classes, Object handlers, Object parentEnv, Object target, byte calling) {
            return getHandlerStack(getRContext());
        }

        protected int createHandlerFrameIndex(VirtualFrame frame) {
            RootNode rootNode = RArguments.getFunction(frame).getRootNode();
            return ((FunctionDefinitionNode) rootNode).getHandlerFrameIndex(frame);
        }

        @Specialization
        protected Object addCondHands(VirtualFrame frame, RStringVector classes, RList handlers, REnvironment parentEnv, Object target, byte calling,
                        @Cached("createHandlerFrameIndex(frame)") int handlerFrameIndex) {
            if (classes.getLength() != handlers.getLength()) {
                CompilerDirectives.transferToInterpreter();
                throw error(RError.Message.BAD_HANDLER_DATA);
            }
            try {
                if (!FrameSlotChangeMonitor.isObject(frame, handlerFrameIndex) || FrameSlotChangeMonitor.getObject(frame, handlerFrameIndex) == null) {
                    // We save the original condition handlers to a frame slot, so that
                    // FunctionDefinitionNode can restore them on function exit
                    FrameSlotChangeMonitor.setObject(frame, handlerFrameIndex, RErrorHandling.getHandlerStack(getRContext()));
                }
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
            return createHandlers(classes, handlers, parentEnv, target, calling);
        }

        @TruffleBoundary
        private static Object createHandlers(RStringVector classes, RList handlers, REnvironment parentEnv, Object target, byte calling) {
            return RErrorHandling.createHandlers(classes, handlers, parentEnv, target, calling);
        }
    }

    @RBuiltin(name = ".resetCondHands", visibility = OFF, kind = INTERNAL, parameterNames = {"stack"}, behavior = COMPLEX)
    public abstract static class ResetCondHands extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(ResetCondHands.class);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull resetCondHands(Object stack) {
            // TODO
            throw RInternalError.unimplemented();
        }
    }

    @RBuiltin(name = ".addRestart", kind = INTERNAL, parameterNames = "restart", behavior = COMPLEX)
    public abstract static class AddRestart extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(AddRestart.class);
            casts.arg("restart").mustBe(instanceOf(RList.class), RError.Message.BAD_RESTART);
        }

        protected int createRestartFrameIndex(VirtualFrame frame) {
            RootNode rootNode = RArguments.getFunction(frame).getRootNode();
            return ((FunctionDefinitionNode) rootNode).getRestartFrameIndex(frame);
        }

        @Specialization
        protected Object addRestart(VirtualFrame frame, RList restart,
                        @Cached("createRestartFrameIndex(frame)") int restartFrameIndex) {
            if (restart.getLength() < 2) {
                throw error(RError.Message.BAD_RESTART);
            }
            try {
                if (!FrameSlotChangeMonitor.isObject(frame, restartFrameIndex) ||
                                FrameSlotChangeMonitor.getObject(frame, restartFrameIndex) == null) {
                    FrameSlotChangeMonitor.setObject(frame, restartFrameIndex, RErrorHandling.getRestartStack());
                }
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
            RErrorHandling.addRestart(restart);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".getRestart", kind = INTERNAL, parameterNames = "restart", behavior = COMPLEX)
    public abstract static class GetRestart extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(GetRestart.class);
            casts.arg("restart").asIntegerVector().findFirst();
        }

        @Specialization
        protected Object getRestart(int index) {
            return RErrorHandling.getRestart(index);
        }
    }

    @RBuiltin(name = ".invokeRestart", kind = INTERNAL, parameterNames = {"restart", "args"}, behavior = COMPLEX)
    public abstract static class InvokeRestart extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(InvokeRestart.class);
            casts.arg("restart").mustBe(instanceOf(RList.class), RError.Message.BAD_RESTART);
        }

        @Specialization
        @TruffleBoundary
        protected RNull invokeRestart(RList restart, Object args) {
            if (restart.getLength() < 2) {
                throw error(RError.Message.BAD_RESTART);
            }
            RErrorHandling.invokeRestart(restart, args);
            // invokeRestart is expected to always return via a ReturnException
            throw error(RError.Message.RESTART_NOT_ON_STACK);
        }
    }

    @RBuiltin(name = ".signalCondition", kind = INTERNAL, parameterNames = {"condition", "msg", "call"}, behavior = COMPLEX)
    public abstract static class SignalCondition extends RBuiltinNode.Arg3 {

        static {
            Casts.noCasts(SignalCondition.class);
        }

        @Specialization
        protected RNull signalCondition(RList condition, Object msg, Object call) {
            String msgStr = "";
            if (msg instanceof RStringVector) {
                RStringVector msgVec = (RStringVector) msg;
                if (msgVec.getLength() > 0) {
                    msgStr = ((RStringVector) msg).getDataAt(0);
                }
            }
            RErrorHandling.signalCondition(condition, msgStr, call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "geterrmessage", kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class Geterrmessage extends RBuiltinNode.Arg0 {
        @Specialization
        protected String geterrmessage() {
            return RErrorHandling.geterrmessage();
        }
    }

    @RBuiltin(name = "seterrmessage", visibility = OFF, kind = INTERNAL, parameterNames = "msg", behavior = COMPLEX)
    public abstract static class Seterrmessage extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(Seterrmessage.class);
            casts.arg("msg").defaultError(RError.Message.ERR_MSG_MUST_BE_STRING).mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        }

        @Specialization
        protected RNull seterrmessage(String msg) {
            RErrorHandling.seterrmessage(msg);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".dfltWarn", kind = INTERNAL, parameterNames = {"message", "call"}, behavior = COMPLEX)
    public abstract static class DfltWarn extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(DfltWarn.class);
            casts.arg("message").defaultError(RError.Message.ERR_MSG_BAD).mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        }

        @Specialization
        protected RNull dfltWarn(String msg, Object call) {
            RErrorHandling.dfltWarn(msg, call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".dfltStop", kind = INTERNAL, parameterNames = {"message", "call"}, behavior = COMPLEX)
    public abstract static class DfltStop extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(DfltStop.class);
            casts.arg("message").defaultError(RError.Message.ERR_MSG_BAD).mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        }

        @Specialization
        protected Object dfltStop(String message, Object call) {
            RErrorHandling.dfltStop(message, call);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "printDeferredWarnings", visibility = OFF, kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class PrintDeferredWarnings extends RBuiltinNode.Arg0 {
        @Specialization
        @TruffleBoundary
        protected RNull printDeferredWarnings() {
            RErrorHandling.printDeferredWarnings();
            return RNull.instance;
        }
    }
}
