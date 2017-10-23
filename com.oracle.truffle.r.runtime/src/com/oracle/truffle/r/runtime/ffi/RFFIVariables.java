/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import java.util.Arrays;
import java.util.stream.Stream;

import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.TempPathName;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Note: regenerate the C glue code upon any change in this enum, use {@link #main(String[])}.
 */
public enum RFFIVariables {
    R_Home(REnvVars.rHome()),
    R_TempDir(null), // Set later
    R_NilValue(RNull.instance),
    R_UnboundValue(RUnboundValue.instance),
    R_MissingArg(RMissing.instance),
    R_EmptyEnv(REnvironment.emptyEnv()),
    R_Srcref(null),
    R_Bracket2Symbol(RDataFactory.createSymbol("[[")),
    R_BracketSymbol(RDataFactory.createSymbol("[")),
    R_BraceSymbol(RDataFactory.createSymbol("{")),
    R_DoubleColonSymbol(RDataFactory.createSymbol("::")),
    R_ClassSymbol(RDataFactory.createSymbol("class")),
    R_DeviceSymbol(RDataFactory.createSymbol(".Device")),
    R_DevicesSymbol(RDataFactory.createSymbol(".Devices")),
    R_DimNamesSymbol(RDataFactory.createSymbol("dimnames")),
    R_DimSymbol(RDataFactory.createSymbol("dim")),
    R_DollarSymbol(RDataFactory.createSymbol("$")),
    R_DotsSymbol(RDataFactory.createSymbol("...")),
    R_DropSymbol(RDataFactory.createSymbol("drop")),
    R_LastvalueSymbol(RDataFactory.createSymbol(".Last.value")),
    R_LevelsSymbol(RDataFactory.createSymbol("levels")),
    R_ModeSymbol(RDataFactory.createSymbol("mode")),
    R_NameSymbol(RDataFactory.createSymbol("name")),
    R_NamesSymbol(RDataFactory.createSymbol("names")),
    R_NaRmSymbol(RDataFactory.createSymbol("na.rm")),
    R_PackageSymbol(RDataFactory.createSymbol("package")),
    R_QuoteSymbol(RDataFactory.createSymbol("quote")),
    R_RowNamesSymbol(RDataFactory.createSymbol("row.names")),
    R_SeedsSymbol(RDataFactory.createSymbol(".Random.seed")),
    R_SourceSymbol(RDataFactory.createSymbol("source")),
    R_TspSymbol(RDataFactory.createSymbol("tsp")),
    R_dot_defined(RDataFactory.createSymbol(".defined")),
    R_dot_Method(RDataFactory.createSymbol(".Method")),
    R_dot_target(RDataFactory.createSymbol(".target")),
    R_dot_packageName(RDataFactory.createSymbol(".packageName")),
    R_dot_Generic(RDataFactory.createSymbol(".Generic")),
    R_SrcrefSymbol(RDataFactory.createSymbol("srcref")),
    R_SrcfileSymbol(RDataFactory.createSymbol("srcfile")),
    R_NaString(CharSXPWrapper.create(RRuntime.STRING_NA)),
    R_NaN(Double.NaN),
    R_PosInf(Double.POSITIVE_INFINITY),
    R_NegInf(Double.NEGATIVE_INFINITY),
    R_NaReal(RRuntime.DOUBLE_NA),
    R_NaInt(RRuntime.INT_NA),
    R_BlankString(CharSXPWrapper.create("")),
    R_BlankScalarString(RDataFactory.createStringVectorFromScalar("")),
    R_BaseSymbol(RDataFactory.createSymbol("base")),
    R_NamespaceEnvSymbol(RDataFactory.createSymbol(".__NAMESPACE__.")),
    R_RestartToken(null),
    R_SortListSymbol(RDataFactory.createSymbol("sort.list")),
    R_SpecSymbol(RDataFactory.createSymbol("spec")),
    R_TripleColonSymbol(RDataFactory.createSymbol(":::")),
    R_PreviousSymbol(RDataFactory.createSymbol("previous"));

    private Object value;
    public final boolean alwaysUpCall;

    RFFIVariables(Object value, boolean alwaysUpCall) {
        this.value = value;
        this.alwaysUpCall = alwaysUpCall;
    }

    RFFIVariables(Object value) {
        this(value, false);
    }

    public Object getValue() {
        return value;
    }

    /**
     * Sets {@link #R_TempDir} for the initial context.
     */
    public static RFFIVariables[] initialize() {
        R_TempDir.value = TempPathName.tempDirPath();
        return values();
    }

    /**
     * Generates C code necessary to glue the Java and C part together. To run this, run
     * {@code mx -v r} to get full command line that runs R and replace the main class with this
     * class.
     */
    public static void main(String[] args) {
        R_TempDir.value = "dummy string";
        System.out.println("// Update com.oracle.truffle.r.native/fficall/src/common/rffi_variablesindex.h with the following: \n");
        System.out.println("// Generated by RFFIVariables.java:\n");
        for (RFFIVariables var : RFFIVariables.values()) {
            System.out.printf("#define %s_x %d\n", var.name(), var.ordinal());
        }
        System.out.printf("\n#define VARIABLES_TABLE_SIZE %d", values().length);

        System.out.println("\n\n// Update com.oracle.truffle.r.native/fficall/src/truffle_nfi/variables.c with the following: \n");

        System.out.println("// Generated by RFFIVariables.java:\n");

        for (RFFIVariables val : values()) {
            if (val.value instanceof Double) {
                System.out.printf("double %s;\n", val.name());
            } else if (val.value instanceof Integer) {
                System.out.printf("int %s;\n", val.name());
            } else if (val.value instanceof String) {
                System.out.printf("char* %s;\n", val.name());
            } else if (val.value instanceof RSymbol) {
                System.out.printf("SEXP %s; /* \"%s\" */\n", val.name(), ((RSymbol) val.value).getName());
            } else {
                System.out.printf("SEXP %s;\n", val.name());
            }
        }

        System.out.println("\nvoid Call_initvar_double(int index, double value) {");
        printInitVarFor(Arrays.stream(RFFIVariables.values()).filter(x -> x.value instanceof Double), "Call_initvar_double");
        System.out.println("}\n");

        System.out.println("void Call_initvar_int(int index, int value) {");
        printInitVarFor(Arrays.stream(RFFIVariables.values()).filter(x -> x.value instanceof Integer), "Call_initvar_int");
        System.out.println("}\n");

        System.out.println("void Call_initvar_string(int index, char* value) {");
        printInitVarFor(Arrays.stream(RFFIVariables.values()).filter(x -> x.value instanceof String), "copystring(value)", "Call_initvar_string");
        System.out.println("}\n");

        System.out.println("void Call_initvar_obj(TruffleEnv* env, int index, void* value) {");
        System.out.println("    init_utils(env);");
        printInitVarFor(Arrays.stream(RFFIVariables.values()).filter(x -> !(x.value instanceof Number || x.value instanceof String)), "Call_initvar_obj");
        System.out.println("}\n");
    }

    private static void printInitVarFor(Stream<RFFIVariables> vars, String callName) {
        printInitVarFor(vars, "value", callName);
    }

    private static void printInitVarFor(Stream<RFFIVariables> vars, String value, String callName) {
        System.out.println("    switch (index) {");
        vars.forEachOrdered(x -> System.out.printf("        case %s_x: %s = %s; break;\n", x.name(), x.name(), value));
        System.out.println("        default:");
        System.out.printf("            printf(\"%s: unimplemented index %%d\\n\", index);\n", callName);
        System.out.println("            exit(1);");
        System.out.println("    }");
    }
}
