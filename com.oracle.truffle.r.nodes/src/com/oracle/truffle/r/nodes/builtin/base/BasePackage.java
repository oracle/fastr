/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.ops.*;

public class BasePackage extends RPackage {

    public BasePackage() {
        // primitive operations
        load(UnaryNotNode.class).names("!");
        load(BinaryArithmeticNode.class).names("+").arguments(BinaryArithmetic.ADD, null);
        load(BinaryArithmeticNode.class).names("-").arguments(BinaryArithmetic.SUBTRACT, UnaryArithmetic.NEGATE);
        load(BinaryArithmeticNode.class).names("/").arguments(BinaryArithmetic.DIV, null);
        load(BinaryArithmeticNode.class).names("%/%").arguments(BinaryArithmetic.INTEGER_DIV, null);
        load(BinaryArithmeticNode.class).names("%%").arguments(BinaryArithmetic.MOD, null);
        load(BinaryArithmeticNode.class).names("*").arguments(BinaryArithmetic.MULTIPLY, null);
        load(BinaryArithmeticNode.class).names("^").arguments(BinaryArithmetic.POW, null);
        load(MatMult.class).names("%*%");
        load(OuterMult.class).names("%o%");
        load(BinaryBooleanNode.class).names("==").arguments(BinaryCompare.EQUAL);
        load(BinaryBooleanNode.class).names("!=").arguments(BinaryCompare.NOT_EQUAL);
        load(BinaryBooleanNode.class).names(">=").arguments(BinaryCompare.GREATER_EQUAL);
        load(BinaryBooleanNode.class).names(">").arguments(BinaryCompare.GREATER_THAN);
        load(BinaryBooleanNode.class).names("<").arguments(BinaryCompare.LESS_THAN);
        load(BinaryBooleanNode.class).names("<=").arguments(BinaryCompare.LESS_EQUAL);

        load(BinaryBooleanNonVectorizedNode.class).names("&&").arguments(BinaryLogic.NON_VECTOR_AND);
        load(BinaryBooleanNonVectorizedNode.class).names("||").arguments(BinaryLogic.NON_VECTOR_OR);
        load(BinaryBooleanNode.class).names("&").arguments(BinaryLogic.AND);
        load(BinaryBooleanNode.class).names("|").arguments(BinaryLogic.OR);

        // please follow alphabetic order for esthetics
        load(Abs.class);
        load(All.class);
        load(Any.class);
        load(Apply.class);
        load(AsCharacter.class);
        load(AsComplex.class);
        load(AsDouble.class);
        load(AsInteger.class);
        load(AsLogical.class);
        load(AsRaw.class);
        load(AsVector.class);
        load(Assign.class);
        load(Attr.class);
        load(Attributes.class);
        load(Cat.class);
        load(Cbind.class);
        load(Ceiling.class);
        load(CharacterBuiltin.class);
        load(ColSums.class);
        load(CommandArgs.class);
        load(Combine.class);
        load(Complex.class);
        load(Contributors.class);
        load(Cor.class);
        load(Cov.class);
        load(CumSum.class);
        load(Diag.class);
        load(Dim.class);
        load(DimNames.class);
        load(DoubleBuiltin.class);
        load(EmptyEnv.class);
        load(Exists.class);
        load(Floor.class);
        load(Get.class);
        load(GlobalEnv.class);
        load(Gregexpr.class);
        load(GSub.class);
        load(Ifelse.class);
        load(Im.class);
        load(IntegerBuiltin.class);
        load(Invisible.class);
        load(IsAtomic.class);
        load(IsCharacter.class);
        load(IsComplex.class);
        load(IsDouble.class);
        load(IsInteger.class);
        load(IsLogical.class);
        load(IsMatrix.class);
        load(IsNA.class);
        load(IsNull.class);
        load(IsNumeric.class);
        load(IsObject.class);
        load(IsRaw.class);
        load(IsTypeNode.class);
        load(IsUnsorted.class);
        load(Length.class);
        load(License.class);
        load(ListBuiltin.class);
        load(Log.class);
        load(Log2.class);
        load(Log10.class);
        load(LogicalBuiltin.class);
        load(Ls.class);
        load(Match.class);
        load(MatchFun.class);
        load(MatMult.class);
        load(Matrix.class);
        load(Max.class);
        load(Mean.class);
        load(Min.class);
        load(Missing.class);
        load(Mod.class);
        load(Names.class);
        load(NChar.class);
        load(NewEnv.class);
        load(Order.class);
        load(Outer.class);
        load(Paste.class);
        load(Print.class);
        load(ProcTime.class);
        load(Quit.class);
        load(RawBuiltin.class);
        load(Rbind.class);
        load(Re.class);
        load(Recall.class);
        load(Regexp.class);
        load(Repeat.class);
        load(RepeatInternal.class);
        load(Return.class);
        load(Rev.class);
        load(Rm.class);
        load(Rnorm.class);
        load(Round.class);
        load(Runif.class);
        load(SApply.class);
        load(Sd.class);
        load(SetSeed.class);
        load(Seq.class);
        load(Sprintf.class);
        load(Sqrt.class);
        load(Stop.class);
        load(Strsplit.class);
        load(Sub.class);
        load(Substr.class);
        load(Sum.class);
        // load(SysGetpid.class);
        load(ToLower.class);
        load(ToString.class);
        load(ToUpper.class);
        load(Transpose.class);
        load(Typeof.class);
        load(Unlist.class);
        load(UpdateAttr.class);
        load(UpdateAttributes.class);
        load(UpdateDiag.class);
        load(UpdateDim.class);
        load(UpdateDimNames.class);
        load(UpdateLength.class);
        load(UpdateNames.class);
        load(UpperTri.class);
        load(Which.class);
    }

    @Override
    public String getName() {
        return "Base";
    }

}
