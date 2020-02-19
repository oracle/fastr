/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.GetFunctionBodyNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.GetFunctionEnvironmentNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.GetFunctionFormalsNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.LENGTHNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.OctSizeNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.RDoNewObjectNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.RDoSlotAssignNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.RDoSlotNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.RHasSlotNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.RfPrintValueNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.SET_TRUELENGTHNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.SetFunctionBodyNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.SetFunctionEnvironmentNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.SetFunctionFormalsNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.SetObjectNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.TRUELENGTHNodeGen;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.nodes.access.HasSlotNode;
import com.oracle.truffle.r.nodes.access.UpdateSlotNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctionsFactory.SetNamesAttributeNodeGen;
import com.oracle.truffle.r.nodes.builtin.EnvironmentNodes.GetFunctionEnvironmentNode;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.objects.NewObject;
import com.oracle.truffle.r.nodes.objects.NewObjectNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.SizeToOctalRawNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RForeignObjectWrapper;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeMirror;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

public final class MiscNodes {

    @TypeSystemReference(RTypes.class)
    @ReportPolymorphism
    @GenerateUncached
    public abstract static class LENGTHNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected int length(@SuppressWarnings("unused") RSymbol sym) {
            return 1;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") RFunction fun) {
            return 1;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") RNull obj) {
            return 0;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") int x) {
            return 1;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") double x) {
            return 1;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") byte x) {
            return 1;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") String x) {
            return 1;
        }

        @Specialization
        protected int length(CharSXPWrapper obj) {
            return obj.getLength();
        }

        @Specialization
        protected int length(RAbstractContainer obj, @Cached("create()") RLengthNode lengthNode) {
            return lengthNode.executeInteger(obj);
        }

        @Specialization
        protected int length(REnvironment env) {
            // May seem wasteful of resources, but simple env.getFrame().getDescriptor().getSize()
            // is not correct!
            return env.ls(true, null, false).getLength();
        }

        @Specialization
        protected int length(RArgsValuesAndNames obj) {
            return obj.getLength();
        }

        @Fallback
        protected int length(Object obj) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.SHOW_CALLER2, RError.Message.LENGTH_MISAPPLIED, SEXPTYPE.gnuRTypeForObject(obj).name());
        }

        public static LENGTHNode create() {
            return LENGTHNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class TRUELENGTHNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected int truelength(@SuppressWarnings("unused") RNull obj) {
            return 0;
        }

        @Specialization
        protected int truelength(CharSXPWrapper obj) {
            return obj.getTruelength();
        }

        @Specialization
        protected int truelength(RStringVector obj) {
            return obj.getTrueLength();
        }

        @Specialization
        protected int truelength(RIntVector obj) {
            return obj.getTrueLength();
        }

        @Specialization
        protected int truelength(RDoubleVector obj) {
            return obj.getTrueLength();
        }

        @Specialization
        protected int truelength(RLogicalVector obj) {
            return obj.getTrueLength();
        }

        @Specialization
        protected int truelength(RComplexVector obj) {
            return obj.getTrueLength();
        }

        @Specialization
        protected int truelength(RRawVector obj) {
            return obj.getTrueLength();
        }

        @Specialization
        protected int truelength(RList obj) {
            return obj.getTrueLength();
        }

        @Fallback
        protected int truelength(Object obj) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.SHOW_CALLER2, RError.Message.LENGTH_MISAPPLIED, SEXPTYPE.gnuRTypeForObject(obj).name());
        }

        public static TRUELENGTHNode create() {
            return TRUELENGTHNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class SET_TRUELENGTHNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected RNull truelength(CharSXPWrapper obj, int trueLength) {
            obj.setTruelength(trueLength);
            return RNull.instance;
        }

        @Specialization
        protected RNull truelength(RStringVector obj, int trueLength) {
            obj.setTrueLength(trueLength);
            return RNull.instance;
        }

        @Specialization
        protected RNull truelength(RIntVector obj, int trueLength) {
            obj.setTrueLength(trueLength);
            return RNull.instance;
        }

        @Specialization
        protected RNull truelength(RDoubleVector obj, int trueLength) {
            obj.setTrueLength(trueLength);
            return RNull.instance;
        }

        @Specialization
        protected RNull truelength(RLogicalVector obj, int trueLength) {
            obj.setTrueLength(trueLength);
            return RNull.instance;
        }

        @Specialization
        protected RNull truelength(RComplexVector obj, int trueLength) {
            obj.setTrueLength(trueLength);
            return RNull.instance;
        }

        @Specialization
        protected RNull truelength(RRawVector obj, int trueLength) {
            obj.setTrueLength(trueLength);
            return RNull.instance;
        }

        @Specialization
        protected RNull truelength(RList obj, int trueLength) {
            obj.setTrueLength(trueLength);
            return RNull.instance;
        }

        public static SET_TRUELENGTHNode create() {
            return SET_TRUELENGTHNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class RDoSlotNode extends FFIUpCallNode.Arg2 {

        @Child private AccessSlotNode accessSlotNode;

        RDoSlotNode() {
            accessSlotNode = AccessSlotNodeGen.create(false);
        }

        @Specialization
        Object doSlot(Object o, RSymbol nameSym) {
            return accessSlotNode.executeAccess(o, nameSym.getName());
        }

        @Fallback
        Object doSlot(@SuppressWarnings("unused") Object o, Object name) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.INVALID_ARGUMENT_OF_TYPE, "name", SEXPTYPE.gnuRTypeForObject(name).name());
        }

        public static RDoSlotNode create() {
            return RDoSlotNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class RDoSlotAssignNode extends FFIUpCallNode.Arg3 {

        @Specialization
        Object doSlotAssign(Object o, String name, Object value,
                        @Cached() UpdateSlotNode updateSlotNode) {
            return updateSlotNode.executeUpdate(o, name, value);
        }

        @Specialization
        Object doSlotAssign(Object o, RSymbol name, Object value,
                        @Cached() UpdateSlotNode updateSlotNode) {
            return updateSlotNode.executeUpdate(o, name.getName(), value);
        }

        @Fallback
        Object doSlot(@SuppressWarnings("unused") Object o, Object name, @SuppressWarnings("unused") Object value) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.INVALID_ARGUMENT_OF_TYPE, "name", SEXPTYPE.gnuRTypeForObject(name).name());
        }

        public static RDoSlotAssignNode create() {
            return RDoSlotAssignNodeGen.create();
        }

        public static RDoSlotAssignNode getUncached() {
            return RDoSlotAssignNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class RDoNewObjectNode extends FFIUpCallNode.Arg1 {

        @Child private NewObject newObjectNode;

        RDoNewObjectNode() {
            newObjectNode = NewObjectNodeGen.create();
        }

        @Specialization
        Object doNewObject(Object classDef) {
            return newObjectNode.execute(classDef);
        }

        public static RDoNewObjectNode create() {
            return RDoNewObjectNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class RHasSlotNode extends FFIUpCallNode.Arg2 {

        @Child private HasSlotNode hasSlotNode;

        RHasSlotNode() {
            hasSlotNode = HasSlotNode.create(false);
        }

        @Specialization
        Object doSlot(Object o, RSymbol nameSym) {
            return hasSlotNode.executeAccess(o, nameSym.getName());
        }

        @Fallback
        @SuppressWarnings("unused")
        Object doSlot(Object o, Object name) {
            return false;
        }

        public static RHasSlotNode create() {
            return RHasSlotNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class NamesGetsNode extends FFIUpCallNode.Arg2 {

        @Child private SetNamesAttributeNode setNamesNode;

        NamesGetsNode() {
            setNamesNode = SetNamesAttributeNodeGen.create();
        }

        @Specialization
        Object doNewObject(RAttributable vec, Object val) {
            setNamesNode.setAttr(vec, val);
            return vec;
        }

        @Fallback
        Object doFallback(Object vec, Object val) {
            throw unsupportedTypes("Rf_namesgets", vec, val);
        }

        public static NamesGetsNode create() {
            return MiscNodesFactory.NamesGetsNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class GetFunctionBody extends FFIUpCallNode.Arg1 {

        @Specialization
        @TruffleBoundary
        public static Object body(RFunction fun) {
            RootNode root = fun.getRootNode();
            if (root instanceof FunctionDefinitionNode) {
                return RContext.getInstance().getRFFI().getOrCreateFunctionBody(fun, f -> {
                    FunctionDefinitionNode funRoot = (FunctionDefinitionNode) f.getRootNode();
                    return RASTUtils.createLanguageElement(funRoot.getBody());
                });
            } else {
                return RNull.instance;
            }
        }

        public static GetFunctionBody create() {
            return GetFunctionBodyNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class GetFunctionFormals extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object formals(RFunction fun) {
            if (fun.isBuiltin()) {
                return RNull.instance;
            } else {
                return RContext.getInstance().getRFFI().getOrCreateFunctionFormals(fun, RASTUtils::createFormals);
            }
        }

        public static GetFunctionFormals create() {
            return GetFunctionFormalsNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class GetFunctionEnvironment extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object environment(RFunction fun,
                        @Cached("create()") GetFunctionEnvironmentNode getEnvNode) {
            return getEnvNode.execute(fun);
        }

        public static GetFunctionEnvironment create() {
            return GetFunctionEnvironmentNodeGen.create();
        }

        public static GetFunctionEnvironment getUncached() {
            return GetFunctionEnvironmentNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class SetFunctionBody extends FFIUpCallNode.Arg2 {

        @Specialization
        @TruffleBoundary
        protected Object body(RFunction fun, Object body) {
            if (!fun.isBuiltin()) {
                RASTUtils.modifyFunction(fun, body, RASTUtils.createFormals(fun), fun.getEnclosingFrame());
                RContext.getInstance().getRFFI().removeFunctionBody(fun);
            }
            return RNull.instance;
        }

        public static SetFunctionBody create() {
            return SetFunctionBodyNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class SetFunctionFormals extends FFIUpCallNode.Arg2 {

        @Specialization
        @TruffleBoundary
        protected Object formals(RFunction fun, Object formals) {
            if (!fun.isBuiltin()) {
                RASTUtils.modifyFunction(fun, GetFunctionBody.body(fun), formals, fun.getEnclosingFrame());
                RContext.getInstance().getRFFI().removeFunctionFormals(fun);
            }
            return RNull.instance;
        }

        public static SetFunctionFormals create() {
            return SetFunctionFormalsNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class SetFunctionEnvironment extends FFIUpCallNode.Arg2 {

        @Specialization
        @TruffleBoundary
        protected Object environment(RFunction fun, REnvironment env) {
            RASTUtils.modifyFunction(fun, GetFunctionBody.body(fun), RASTUtils.createFormals(fun), env.getFrame());
            fun.reassignEnclosingFrame(env.getFrame());
            return RNull.instance;
        }

        public static SetFunctionEnvironment create() {
            return SetFunctionEnvironmentNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class OctSizeNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected RRawVector octSize(Object size,
                        @Cached("create()") SizeToOctalRawNode sizeToOctal,
                        @Cached("createCast()") CastNode castToDoubleNode) {
            return sizeToOctal.execute(castToDoubleNode.doCast(size));

        }

        protected CastNode createCast() {
            return CastNodeBuilder.newCastBuilder().mustNotBeMissing().allowNull().asDoubleVector().findFirst().buildCastNode();
        }

        public static OctSizeNode create() {
            return OctSizeNodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class RfPrintValueNode extends FFIUpCallNode.Arg1 {
        @Specialization
        public Object exec(Object value) {
            RContext.getEngine().printResult(RContext.getInstance(), value);
            return RNull.instance;
        }

        public static RfPrintValueNode create() {
            return RfPrintValueNodeGen.create();
        }

        public static RfPrintValueNode getUncached() {
            return RfPrintValueNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class SetObjectNode extends FFIUpCallNode.Arg2 {
        public static SetObjectNode create() {
            return SetObjectNodeGen.create();
        }

        @Specialization
        protected Object doIt(RBaseObject target, int flag,
                        @Cached() GetClassAttributeNode getClassAttributeNode) {
            // Note: "OBJECT" is an internal flag in SEXP that internal dispatching (in FastR
            // INTERNAL_DISPATCH builtins) is checking first before even checking the attributes
            // collection for presence of the "class" attribute. FastR always checks attributes and
            // ignores the OBJECT flag. The only possible difference is hence if someone sets
            // OBJECT flag to 0 for a SEXP that actually has some class, in which case in GNUR the
            // internal dispatch builtins like 'as.character' will not dispatch to the S3 method
            // even thought the object has S3 class and FastR would dispatch.
            // See simpleTests.R in testrffi package for example.
            if (flag == 0 && target instanceof RAttributable) {
                RStringVector clazz = getClassAttributeNode.getClassAttr((RAttributable) target);
                if (clazz != null && clazz.getLength() != 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(RError.NO_CALLER, Message.GENERIC, "SET_OBJECT(SEXP, 0) not implemented for SEXP with 'class' attribute");
                }
            }
            return RNull.instance;
        }

        @Specialization
        protected Object doOthers(Object value, Object flag) {
            throw unsupportedTypes("SET_OBJECT", value, flag);
        }

    }
}
