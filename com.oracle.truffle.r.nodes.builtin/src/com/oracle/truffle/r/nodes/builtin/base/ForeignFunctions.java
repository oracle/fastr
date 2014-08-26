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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
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
        protected static final String[] PARAMETER_NAMES = new String[]{".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"};

        @Override
        public String[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(EMPTY_OBJECT_ARRAY), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
        }

        protected int[] checkNAs(int argIndex, int[] data) throws RError {
            for (int i = 0; i < data.length; i++) {
                if (RRuntime.isNA(data[i])) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.NA_IN_FOREIGN_FUNCTION_CALL, argIndex);
                }
            }
            return data;
        }

        protected double[] checkNAs(int argIndex, double[] data) throws RError {
            for (int i = 0; i < data.length; i++) {
                if (!RRuntime.isFinite(data[i])) {
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
        protected RList fortranDqrdc2(String f, RArgsValuesAndNames args, byte naok, byte dup, RMissing rPackage, RMissing encoding) {
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
                double[] x = xVec.isTemporary() ? xVec.getDataWithoutCopying() : xVec.getDataCopy();
                int[] rank = rankVec.isTemporary() ? rankVec.getDataWithoutCopying() : rankVec.getDataCopy();
                double[] qraux = qrauxVec.isTemporary() ? qrauxVec.getDataWithoutCopying() : qrauxVec.getDataCopy();
                int[] pivot = pivotVec.isTemporary() ? pivotVec.getDataWithoutCopying() : pivotVec.getDataCopy();
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
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_ARG, "dqrdc2");
            }
        }

        public boolean dqrdc2(String f) {
            return f.equals("dqrdc2");
        }

        private static final RStringVector DQRCF_NAMES = RDataFactory.createStringVector(new String[]{E, E, E, E, E, E, "coef", "info"}, RDataFactory.COMPLETE_VECTOR);

        @SuppressWarnings("unused")
        @Specialization(guards = "dqrcf")
        protected RList fortranDqrcf(String f, Object args, byte naok, byte dup, RMissing rPackage, RMissing encoding) {
            controlVisibility();
            // TODO: cannot specify args as RArgsValuesAndNames due to annotation processor error
            Object[] argValues = ((RArgsValuesAndNames) args).getValues();
            try {
                RDoubleVector xVec = (RDoubleVector) argValues[0];
                int n = (int) argValues[1];
                RIntVector k = (RIntVector) argValues[2];
                RDoubleVector qrauxVec = (RDoubleVector) argValues[3];
                RDoubleVector yVec = (RDoubleVector) argValues[4];
                int ny = (int) argValues[5];
                RDoubleVector bVec = (RDoubleVector) argValues[6];
                RIntVector infoVec = (RIntVector) argValues[7];
                double[] x = xVec.isTemporary() ? xVec.getDataWithoutCopying() : xVec.getDataCopy();
                double[] qraux = qrauxVec.isTemporary() ? qrauxVec.getDataWithoutCopying() : qrauxVec.getDataCopy();
                double[] y = yVec.isTemporary() ? yVec.getDataWithoutCopying() : yVec.getDataCopy();
                double[] b = bVec.isTemporary() ? bVec.getDataWithoutCopying() : bVec.getDataCopy();
                int[] info = infoVec.isTemporary() ? infoVec.getDataWithoutCopying() : infoVec.getDataCopy();
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
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_ARG, "dqrcf");
            }
        }

        public boolean dqrcf(String f) {
            return f.equals("dqrcf");
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
        protected RList c(String f, Object args, byte naok, byte dup, RMissing rPackage, RMissing encoding) {
            controlVisibility();
            // TODO: cannot specify args as RArgsValuesAndNames due to annotation processor error
            Object[] argValues = ((RArgsValuesAndNames) args).getValues();
            SymbolInfo symbolInfo = DLL.findSymbolInfo(f, null);
            if (symbolInfo == null) {
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
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNIMPLEMENTED_ARG_TYPE, i + 1);
                }
            }
            try {
                RFFIFactory.getRFFI().getCRFFI().invoke(symbolInfo, nativeArgs);
            } catch (Throwable t) {
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
    public abstract static class Call extends RBuiltinNode {

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
        protected RComplexVector callFFT(VirtualFrame frame, RList f, Object args, RMissing packageName) {
            controlVisibility();
            // TODO: cannot specify args as RArgsValuesAndNames due to annotation processor error
            Object[] argValues = ((RArgsValuesAndNames) args).getValues();
            RComplexVector zVec = (RComplexVector) castComplex(frame, castVector(frame, argValues[0]));
            double[] z = zVec.isTemporary() ? zVec.getDataWithoutCopying() : zVec.getDataCopy();
            RLogicalVector inverse = (RLogicalVector) castLogical(frame, castVector(frame, argValues[1]));
            int inv = RRuntime.isNA(inverse.getDataAt(0)) || inverse.getDataAt(0) == RRuntime.LOGICAL_FALSE ? -2 : 2;
            int retCode = 7;
            if (zVec.getLength() > 1) {
                int[] maxf = new int[1];
                int[] maxp = new int[1];
                if (zVec.getDimensions() == null) {
                    int n = zVec.getLength();
                    RFFIFactory.getRFFI().getRDerivedRFFI().fft_factor(n, maxf, maxp);
                    if (maxf[0] == 0) {
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

        public boolean fft(RList f) {
            return matchName(f, "fft");
        }

        // Translated from GnuR: library/methods/src/methods_list_dispatch.c
        @SuppressWarnings("unused")
        @Specialization(guards = "methodsPackageMetaName")
        protected String callMethodsPackageMetaName(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName) {
            controlVisibility();
            // TODO: cannot specify args as RArgsValuesAndNames due to annotation processor error
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

        @Specialization
        public Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        public Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            SymbolInfo symbolInfo = DLL.findSymbolInfo(name, packageName);
            if (symbolInfo == null) {
                throw RError.error(getEncapsulatingSourceSection(), Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            try {
                return RFFIFactory.getRFFI().getCallRFFI().invokeCall(symbolInfo, args.getValues());
            } catch (Throwable t) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NATIVE_CALL_FAILED, t.getMessage());
            }
        }

    }

}
