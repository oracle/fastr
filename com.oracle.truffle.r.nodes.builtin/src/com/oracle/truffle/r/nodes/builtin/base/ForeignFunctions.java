/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.utils.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.ConsoleHandler;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.ffi.*;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;

/**
 * {@code .C} and {.Fortran} functions.
 *
 * TODO Completeness (more types, more error checks), Performance (copying). Especially all the
 * subtleties around copying. See <a
 * href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/Foreign.html">here</a>.
 */
public class ForeignFunctions {
    public abstract static class FortranCAdapter extends RBuiltinNode {
        @CompilationFinal protected static final String[] PARAMETER_NAMES = new String[]{".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"};

        protected final BranchProfile errorProfile = BranchProfile.create();

        @Override
        public String[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(EMPTY_OBJECT_ARRAY), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
        }

        protected int[] checkNAs(int argIndex, int[] data) {
            for (int i = 0; i < data.length; i++) {
                if (RRuntime.isNA(data[i])) {
                    errorProfile.enter();
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.NA_IN_FOREIGN_FUNCTION_CALL, argIndex);
                }
            }
            return data;
        }

        protected double[] checkNAs(int argIndex, double[] data) {
            for (int i = 0; i < data.length; i++) {
                if (!RRuntime.isFinite(data[i])) {
                    errorProfile.enter();
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.NA_NAN_INF_IN_FOREIGN_FUNCTION_CALL, argIndex);
                }
            }
            return data;
        }
    }

    /**
     * For now, just some special case functions that are built in to the implementation.
     */
    @RBuiltin(name = ".Fortran", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"})
    public abstract static class Fortran extends FortranCAdapter {
        private static final String E = RRuntime.NAMES_ATTR_EMPTY_VALUE;
        private static final RStringVector DQRDC2_NAMES = RDataFactory.createStringVector(new String[]{"qr", E, E, E, E, "rank", "qraux", "pivot", E}, RDataFactory.COMPLETE_VECTOR);

        @SuppressWarnings("unused")
        @Specialization(guards = "dqrdc2")
        protected RList fortranDqrdc2(RList f, RArgsValuesAndNames args, byte naok, byte dup, RMissing rPackage, RMissing encoding) {
            controlVisibility();
            Object[] argValues = args.getValues();
            try {
                RDoubleVector xVec = (RDoubleVector) argValues[0];
                int ldx = (int) argValues[1];
                int n = (int) argValues[2];
                int p = (int) argValues[3];
                double tol = (double) argValues[4];
                RIntVector rankVec = (RIntVector) argValues[5];
                RDoubleVector qrauxVec = (RDoubleVector) argValues[6];
                RIntVector pivotVec = (RIntVector) argValues[7];
                RDoubleVector workVec = (RDoubleVector) argValues[8];
                double[] x = xVec.getDataTemp();
                int[] rank = rankVec.getDataTemp();
                double[] qraux = qrauxVec.getDataTemp();
                int[] pivot = pivotVec.getDataTemp();
                RFFIFactory.getRFFI().getRDerivedRFFI().dqrdc2(x, ldx, n, p, tol, rank, qraux, pivot, workVec.getDataCopy());
                // @formatter:off
                Object[] data = new Object[]{
                            RDataFactory.createDoubleVector(x, RDataFactory.COMPLETE_VECTOR, xVec.getDimensions()),
                            argValues[1], argValues[2], argValues[3], argValues[4],
                            RDataFactory.createIntVector(rank, RDataFactory.COMPLETE_VECTOR),
                            RDataFactory.createDoubleVector(qraux, RDataFactory.COMPLETE_VECTOR),
                            RDataFactory.createIntVector(pivot, RDataFactory.COMPLETE_VECTOR),
                            argValues[8]
                };
                // @formatter:on
                return RDataFactory.createList(data, DQRDC2_NAMES);
            } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_ARG, "dqrdc2");
            }
        }

        public boolean dqrdc2(RList f) {
            return matchName(f, "dqrdc2");
        }

        private static final RStringVector DQRCF_NAMES = RDataFactory.createStringVector(new String[]{E, E, E, E, E, E, "coef", "info"}, RDataFactory.COMPLETE_VECTOR);

        @SuppressWarnings("unused")
        @Specialization(guards = "dqrcf")
        protected RList fortranDqrcf(RList f, RArgsValuesAndNames args, byte naok, byte dup, RMissing rPackage, RMissing encoding) {
            controlVisibility();
            Object[] argValues = args.getValues();
            try {
                RDoubleVector xVec = (RDoubleVector) argValues[0];
                int n = (int) argValues[1];
                RIntVector k = (RIntVector) argValues[2];
                RDoubleVector qrauxVec = (RDoubleVector) argValues[3];
                RDoubleVector yVec = (RDoubleVector) argValues[4];
                int ny = (int) argValues[5];
                RDoubleVector bVec = (RDoubleVector) argValues[6];
                RIntVector infoVec = (RIntVector) argValues[7];
                double[] x = xVec.getDataTemp();
                double[] qraux = qrauxVec.getDataTemp();
                double[] y = yVec.getDataTemp();
                double[] b = bVec.getDataTemp();
                int[] info = infoVec.getDataTemp();
                RFFIFactory.getRFFI().getRDerivedRFFI().dqrcf(x, n, k.getDataAt(0), qraux, y, ny, b, info);
                RDoubleVector coef = RDataFactory.createDoubleVector(b, RDataFactory.COMPLETE_VECTOR);
                coef.copyAttributesFrom(bVec);
                // @formatter:off
                Object[] data = new Object[]{
                            RDataFactory.createDoubleVector(x, RDataFactory.COMPLETE_VECTOR),
                            argValues[1],
                            k.copy(),
                            RDataFactory.createDoubleVector(qraux, RDataFactory.COMPLETE_VECTOR),
                            RDataFactory.createDoubleVector(y, RDataFactory.COMPLETE_VECTOR),
                            argValues[5],
                            coef,
                            RDataFactory.createIntVector(info, RDataFactory.COMPLETE_VECTOR),
                };
                // @formatter:on
                return RDataFactory.createList(data, DQRCF_NAMES);

            } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_ARG, "dqrcf");
            }
        }

        public boolean dqrcf(RList f) {
            return matchName(f, "dqrcf");
        }

    }

    @RBuiltin(name = ".C", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"})
    public abstract static class C extends FortranCAdapter {

        private static final int SCALAR_DOUBLE = 0;
        private static final int SCALAR_INT = 1;
        private static final int SCALAR_LOGICAL = 2;
        @SuppressWarnings("unused") private static final int SCALAR_STRING = 3;
        private static final int VECTOR_DOUBLE = 10;
        private static final int VECTOR_INT = 11;
        private static final int VECTOR_LOGICAL = 12;
        @SuppressWarnings("unused") private static final int VECTOR_STRING = 12;

        @SuppressWarnings("unused")
        @Specialization
        protected RList c(String f, RArgsValuesAndNames args, byte naok, byte dup, RMissing rPackage, RMissing encoding) {
            controlVisibility();
            Object[] argValues = args.getValues();
            SymbolInfo symbolInfo = DLL.findSymbolInfo(f, null);
            if (symbolInfo == null) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.C_SYMBOL_NOT_IN_TABLE, f);
            }
            boolean dupArgs = RRuntime.fromLogical(dup);
            boolean checkNA = RRuntime.fromLogical(naok);
            // Analyze the args, making copies (ignoring dup for now)
            int[] argTypes = new int[argValues.length];
            Object[] nativeArgs = new Object[argValues.length];
            for (int i = 0; i < argValues.length; i++) {
                Object arg = argValues[i];
                if (arg instanceof RDoubleVector) {
                    argTypes[i] = VECTOR_DOUBLE;
                    nativeArgs[i] = checkNAs(i + 1, ((RDoubleVector) arg).getDataCopy());
                } else if (arg instanceof RIntVector) {
                    argTypes[i] = VECTOR_INT;
                    nativeArgs[i] = checkNAs(i + 1, ((RIntVector) arg).getDataCopy());
                } else if (arg instanceof RLogicalVector) {
                    argTypes[i] = VECTOR_LOGICAL;
                    // passed as int[]
                    byte[] data = ((RLogicalVector) arg).getDataWithoutCopying();
                    int[] dataAsInt = new int[data.length];
                    for (int j = 0; j < data.length; j++) {
                        // An NA is an error but the error handling happens in checkNAs
                        dataAsInt[j] = RRuntime.isNA(data[j]) ? RRuntime.INT_NA : data[j];
                    }
                    nativeArgs[i] = checkNAs(i + 1, dataAsInt);
                } else if (arg instanceof Double) {
                    argTypes[i] = SCALAR_DOUBLE;
                    nativeArgs[i] = checkNAs(i + 1, new double[]{(double) arg});
                } else if (arg instanceof Integer) {
                    argTypes[i] = SCALAR_INT;
                    nativeArgs[i] = checkNAs(i + 1, new int[]{(int) arg});
                } else if (arg instanceof Byte) {
                    argTypes[i] = SCALAR_LOGICAL;
                    nativeArgs[i] = checkNAs(i + 1, new int[]{RRuntime.isNA((byte) arg) ? RRuntime.INT_NA : (byte) arg});
                } else {
                    errorProfile.enter();
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNIMPLEMENTED_ARG_TYPE, i + 1);
                }
            }
            try {
                RFFIFactory.getRFFI().getCRFFI().invoke(symbolInfo, nativeArgs);
            } catch (Throwable t) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NATIVE_CALL_FAILED, t.getMessage());
            }
            // we have to assume that the native method updated everything
            RStringVector listNames = validateArgNames(argValues.length, getSuppliedArgsNames());
            Object[] results = new Object[argValues.length];
            for (int i = 0; i < argValues.length; i++) {
                switch (argTypes[i]) {
                    case SCALAR_DOUBLE:
                        results[i] = RDataFactory.createDoubleVector((double[]) nativeArgs[i], RDataFactory.COMPLETE_VECTOR);
                        break;
                    case SCALAR_INT:
                        results[i] = RDataFactory.createIntVector((int[]) nativeArgs[i], RDataFactory.COMPLETE_VECTOR);
                        break;
                    case SCALAR_LOGICAL:
                        results[i] = RDataFactory.createLogicalVector((byte[]) nativeArgs[i], RDataFactory.COMPLETE_VECTOR);
                        break;
                    case VECTOR_DOUBLE: {
                        results[i] = ((RDoubleVector) argValues[i]).copyResetData((double[]) nativeArgs[i]);
                        break;
                    }
                    case VECTOR_INT: {
                        results[i] = ((RIntVector) argValues[i]).copyResetData((int[]) nativeArgs[i]);
                        break;
                    }
                    case VECTOR_LOGICAL: {
                        results[i] = ((RLogicalVector) argValues[i]).copyResetData((byte[]) nativeArgs[i]);
                        break;
                    }

                }
            }
            return RDataFactory.createList(results, listNames);
        }

        private static RStringVector validateArgNames(int argsLength, String[] argNames) {
            String[] listArgNames = new String[argsLength];
            for (int i = 0; i < argsLength; i++) {
                String name = argNames == null ? null : argNames[i + 1];
                if (name == null) {
                    name = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                }
                listArgNames[i] = name;
            }
            return RDataFactory.createStringVector(listArgNames, RDataFactory.COMPLETE_VECTOR);
        }

    }

    /**
     * For now, just some special case functions that are built in to the implementation.
     */
    @RBuiltin(name = ".Call", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"})
    public abstract static class DotCall extends RBuiltinNode {

        private final BranchProfile errorProfile = BranchProfile.create();
        private final ConditionProfile zVecLgt1 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile noDims = ConditionProfile.createBinaryProfile();

        @Child private CastComplexNode castComplex;
        @Child private CastLogicalNode castLogical;
        @Child private CastToVectorNode castVector;

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(EMPTY_OBJECT_ARRAY), ConstantNode.create(RMissing.instance)};
        }

        private Object castComplex(VirtualFrame frame, Object operand) {
            if (castComplex == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castComplex = insert(CastComplexNodeFactory.create(null, true, true, false));
            }
            return castComplex.executeCast(frame, operand);
        }

        private Object castLogical(VirtualFrame frame, Object operand) {
            if (castLogical == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castLogical = insert(CastLogicalNodeFactory.create(null, true, false, false));
            }
            return castLogical.executeCast(frame, operand);
        }

        private RAbstractVector castVector(VirtualFrame frame, Object value) {
            if (castVector == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castVector = insert(CastToVectorNodeFactory.create(null, false, false, false, false));
            }
            return (RAbstractVector) castVector.executeObject(frame, value);
        }

        // TODO: handle more argument types (this is sufficient to run the b25 benchmarks)
        @SuppressWarnings("unused")
        @Specialization(guards = "fft")
        protected RComplexVector callFFT(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName) {
            controlVisibility();
            Object[] argValues = args.getValues();
            RComplexVector zVec = (RComplexVector) castComplex(frame, castVector(frame, argValues[0]));
            double[] z = zVec.getDataTemp();
            RLogicalVector inverse = (RLogicalVector) castLogical(frame, castVector(frame, argValues[1]));
            int inv = RRuntime.isNA(inverse.getDataAt(0)) || inverse.getDataAt(0) == RRuntime.LOGICAL_FALSE ? -2 : 2;
            int retCode = 7;
            if (zVecLgt1.profile(zVec.getLength() > 1)) {
                int[] maxf = new int[1];
                int[] maxp = new int[1];
                if (noDims.profile(zVec.getDimensions() == null)) {
                    int n = zVec.getLength();
                    RFFIFactory.getRFFI().getRDerivedRFFI().fft_factor(n, maxf, maxp);
                    if (maxf[0] == 0) {
                        errorProfile.enter();
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.FFT_FACTORIZATION);
                    }
                    double[] work = new double[4 * maxf[0]];
                    int[] iwork = new int[maxp[0]];
                    retCode = RFFIFactory.getRFFI().getRDerivedRFFI().fft_work(z, 1, n, 1, inv, work, iwork);
                } else {
                    int maxmaxf = 1;
                    int maxmaxp = 1;
                    int[] d = zVec.getDimensions();
                    int ndims = d.length;
                    /* do whole loop just for error checking and maxmax[fp] .. */
                    for (int i = 0; i < ndims; i++) {
                        if (d[i] > 1) {
                            RFFIFactory.getRFFI().getRDerivedRFFI().fft_factor(d[i], maxf, maxp);
                            if (maxf[0] == 0) {
                                errorProfile.enter();
                                throw RError.error(getEncapsulatingSourceSection(), RError.Message.FFT_FACTORIZATION);
                            }
                            if (maxf[0] > maxmaxf) {
                                maxmaxf = maxf[0];
                            }
                            if (maxp[0] > maxmaxp) {
                                maxmaxp = maxp[0];
                            }
                        }
                    }
                    double[] work = new double[4 * maxmaxf];
                    int[] iwork = new int[maxmaxp];
                    int nseg = zVec.getLength();
                    int n = 1;
                    int nspn = 1;
                    for (int i = 0; i < ndims; i++) {
                        if (d[i] > 1) {
                            nspn *= n;
                            n = d[i];
                            nseg /= n;
                            RFFIFactory.getRFFI().getRDerivedRFFI().fft_factor(n, maxf, maxp);
                            RFFIFactory.getRFFI().getRDerivedRFFI().fft_work(z, nseg, n, nspn, inv, work, iwork);
                        }
                    }

                }
            }

            return RDataFactory.createComplexVector(z, zVec.isComplete(), zVec.getDimensions());
        }

        public boolean fft(RList f) {
            return matchName(f, "fft");
        }

        // Translated from GnuR: library/methods/src/methods_list_dispatch.c
        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization(guards = "methodsPackageMetaName")
        protected String callMethodsPackageMetaName(RList f, RArgsValuesAndNames args, RMissing packageName) {
            controlVisibility();
            Object[] argValues = args.getValues();
            // TODO proper error checks
            String prefixString = (String) argValues[0];
            String nameString = (String) argValues[1];
            String pkgString = (String) argValues[2];
            if (pkgString.length() == 0) {
                return String.format(".__%s__%s", prefixString, nameString);
            } else {
                return String.format(".__%s__%s:%s", prefixString, nameString, pkgString);
            }
        }

        public boolean methodsPackageMetaName(RList f) {
            return matchName(f, "R_methodsPackageMetaName");
        }

        // Translated from GnuR: library/methods/src/methods_list_dispatch.c
        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization(guards = "getClassFromCache")
        protected Object callGetClassFromCache(RList f, RArgsValuesAndNames args, RMissing packageName) {
            controlVisibility();
            Object[] argValues = args.getValues();
            REnvironment table = (REnvironment) argValues[1];
            String klassString = RRuntime.asString(argValues[0]);
            if (klassString != null) {
                Object value = table.get(klassString);
                if (value == null) {
                    return RNull.instance;
                } else {
                    // TODO check PACKAGE equality
                    return value;
                }
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARG_TYPE);
            }
        }

        public boolean getClassFromCache(RList f) {
            return matchName(f, "R_getClassFromCache");
        }

        @Specialization
        public Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        public Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            SymbolInfo symbolInfo = DLL.findSymbolInfo(name, packageName);
            if (symbolInfo == null) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            try {
                return RFFIFactory.getRFFI().getCallRFFI().invokeCall(symbolInfo, args.getValues());
            } catch (Throwable t) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NATIVE_CALL_FAILED, t.getMessage());
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isFlushconsole")
        @TruffleBoundary
        protected RNull flushConsole(VirtualFrame frame, RList f, Object[] args, RMissing packageName) {
            return RNull.instance;
        }

        public static boolean isFlushconsole(RList f) {
            return matchName(f, "flushconsole");
        }

        // Translated from GnuR: library/utils/io.c
        @SuppressWarnings("unused")
        @Specialization(guards = "isMenu")
        @TruffleBoundary
        protected int menu(RList f, RArgsValuesAndNames args, RMissing packageName) {
            Object[] values = args.getValues();
            String[] choices;
            if (values[0] instanceof String) {
                choices = new String[]{(String) values[0]};
            } else if (values[0] instanceof RStringVector) {
                choices = ((RStringVector) values[0]).getDataWithoutCopying();
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "choices");
            }
            ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
            int first = choices.length + 1;
            ch.print("Selection: ");
            String response = ch.readLine().trim();
            if (response.length() > 0) {
                if (Character.isDigit(response.charAt(0))) {
                    try {
                        first = Integer.parseInt(response);
                    } catch (NumberFormatException ex) {
                        //
                    }
                } else {
                    for (int i = 0; i < choices.length; i++) {
                        String entry = choices[i];
                        if (entry.equals(response)) {
                            first = i + 1;
                            break;
                        }
                    }
                }
            }
            return first;
        }

        public static boolean isMenu(RList f) {
            return matchName(f, "menu");
        }

    }

    /**
     * This is an inefficient guard but it matters little unless there are many different calls
     * being made within the same evaluation. A {@code NativeSymbolInfo} object would provide a more
     * efficient basis.
     */
    private static boolean matchName(RList f, String name) {
        if (f.getNames() == RNull.instance) {
            return false;
        }
        RStringVector names = (RStringVector) f.getNames();
        for (int i = 0; i < names.getLength(); i++) {
            if (names.getDataAt(i).equals("name")) {
                return f.getDataAt(i).equals(name) ? true : false;
            }
        }
        return false;
    }

    private static String isString(Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        } else if (arg instanceof RStringVector) {
            if (((RStringVector) arg).getLength() == 0) {
                return null;
            } else {
                return ((RStringVector) arg).getDataAt(0);
            }
        } else {
            return null;
        }
    }

    private abstract static class CastAdapter extends RBuiltinNode {
        @Child private CastLogicalNode castLogical;
        @Child private CastIntegerNode castInt;

        protected byte castLogical(VirtualFrame frame, Object operand) {
            if (castLogical == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castLogical = insert(CastLogicalNodeFactory.create(null, false, false, false));
            }
            return (byte) castLogical.executeCast(frame, operand);
        }

        protected int castInt(VirtualFrame frame, Object operand) {
            if (castInt == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castInt = insert(CastIntegerNodeFactory.create(null, false, false, false));
            }
            return (int) castInt.executeCast(frame, operand);
        }
    }

    @RBuiltin(name = ".External", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"})
    public abstract static class DotExternal extends CastAdapter {

        // Transcribed from GnuR, library/utils/src/io.c
        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization(guards = "isCountFields")
        protected Object countFields(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName) {
            controlVisibility();
            Object[] argValues = args.getValues();
            RConnection conn = (RConnection) argValues[0];
            Object sepArg = argValues[1];
            char sepChar;
            Object quoteArg = argValues[2];
            int nskip = castInt(frame, argValues[3]);
            byte blskip = castLogical(frame, argValues[4]);
            String commentCharArg = isString(argValues[5]);
            char comChar;
            if (!(commentCharArg != null && commentCharArg.length() == 1)) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "comment.char");
            } else {
                comChar = commentCharArg.charAt(0);
            }

            if (nskip < 0 || nskip == RRuntime.INT_NA) {
                nskip = 0;
            }
            if (blskip == RRuntime.LOGICAL_NA) {
                blskip = RRuntime.LOGICAL_TRUE;
            }

            if (sepArg instanceof RNull) {
                sepChar = 0;
            } else {
                String s = isString(sepArg);
                if (s == null) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "sep");
                } else {
                    if (s.length() == 0) {
                        sepChar = 0;
                    } else {
                        sepChar = s.charAt(0);
                    }
                }
            }
            String quoteSet;
            if (quoteArg instanceof RNull) {
                quoteSet = "";
            } else {
                String s = isString(quoteArg);
                if (s == null) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, "invalid quote symbol set");
                } else {
                    quoteSet = s;
                }
            }
            try {
                return CountFields.execute(conn, sepChar, quoteSet, nskip, RRuntime.fromLogical(blskip), comChar);
            } catch (IllegalStateException | IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
            }
        }

        public boolean isCountFields(RList f) {
            return matchName(f, "countfields");
        }
    }

    @RBuiltin(name = ".External2", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "..."})
    public abstract static class DotExternal2 extends CastAdapter {

        @Child private CastToVectorNode castVector;

        private RAbstractVector castVector(VirtualFrame frame, Object value) {
            if (castVector == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castVector = insert(CastToVectorNodeFactory.create(null, false, false, false, false));
            }
            return (RAbstractVector) castVector.executeObject(frame, value);
        }

        // Transcribed from GnuR, library/utils/src/io.c
        @TruffleBoundary
        @Specialization(guards = "isWriteTable")
        protected Object doWriteTable(VirtualFrame frame, @SuppressWarnings("unused") RList f, RArgsValuesAndNames args) {
            controlVisibility();
            Object[] argValues = args.getValues();
            Object conArg = argValues[1];
            RConnection con;
            if (!(conArg instanceof RConnection)) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, "'file' is not a connection");
            } else {
                con = (RConnection) conArg;
            }
            // TODO check connection writeable

            int nr = castInt(frame, argValues[2]);
            int nc = castInt(frame, argValues[3]);
            Object rnamesArg = argValues[4];
            Object sepArg = argValues[5];
            Object eolArg = argValues[6];
            Object naArg = argValues[7];
            Object decArg = argValues[8];
            Object quoteArg = argValues[9];
            byte qmethod = castLogical(frame, argValues[10]);

            String csep;
            String ceol;
            String cna;
            String cdec;

            if (nr == RRuntime.INT_NA) {
                invalidArgument("nr");
            }
            if (nc == RRuntime.INT_NA) {
                invalidArgument("nc");
            }
            if (!(rnamesArg instanceof RNull) && isString(rnamesArg) == null) {
                invalidArgument("rnames");
            }
            if ((csep = isString(sepArg)) == null) {
                invalidArgument("sep");
            }
            if ((ceol = isString(eolArg)) == null) {
                invalidArgument("eol");
            }
            if ((cna = isString(naArg)) == null) {
                invalidArgument("na");
            }
            if ((cdec = isString(decArg)) == null) {
                invalidArgument("dec");
            }
            if (qmethod == RRuntime.LOGICAL_NA) {
                invalidArgument("qmethod");
            }
            if (cdec.length() != 1) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, "'dec' must be a single character");
            }
            boolean[] quoteCol = new boolean[nc];
            boolean quoteRn = false;
            RIntVector quote = (RIntVector) castVector(frame, quoteArg);
            for (int i = 0; i < quote.getLength(); i++) {
                int qi = quote.getDataAt(i);
                if (qi == 0) {
                    quoteRn = true;
                }
                if (qi > 0) {
                    quoteCol[qi - 1] = true;
                }
            }
            try {
                WriteTable.execute(con, argValues[0], nr, nc, rnamesArg, csep, ceol, cna, cdec.charAt(0), RRuntime.fromLogical(qmethod), quoteCol, quoteRn);
            } catch (IOException | IllegalArgumentException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
            }
            return RNull.instance;
        }

        protected void invalidArgument(String name) throws RError {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, name);
        }

        public boolean isWriteTable(RList f) {
            return matchName(f, "writetable");
        }
    }

}
