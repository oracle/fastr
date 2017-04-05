/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;

import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * External builtin that converts the string representation of a unit, e.g. "cm", to internal
 * numerical value. This values are then stored in special attribute "valid.unit".
 */
public abstract class LValidUnit extends RExternalBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(LValidUnit.class);
        casts.arg(0).mustBe(stringValue(), Message.GENERIC, "'units' must be character").asStringVector().mustBe(notEmpty(), Message.GENERIC, "'units' must be of length > 0");
    }

    public static LValidUnit create() {
        return LValidUnitNodeGen.create();
    }

    @Specialization
    RIntVector convert(RAbstractStringVector units) {
        int[] result = new int[units.getLength()];
        for (int i = 0; i < units.getLength(); i++) {
            result[i] = convertSingle(units.getDataAt(i));
        }
        return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    private int convertSingle(String name) {
        if (name.equals("npc")) {
            // seems to be by far the most common unit
            return Unit.NPC;
        }
        return convertSingleSlowPath(name);
    }

    @TruffleBoundary
    private int convertSingleSlowPath(String name) {
        Integer result = NamesHolder.NAMES.get(name);
        if (result == null) {
            throw error(Message.GENERIC, "invalid unit");
        }
        return result;
    }

    private static final class NamesHolder {
        private static final HashMap<String, Integer> NAMES = new HashMap<>(30);
        static {
            NAMES.put("npc", Unit.NPC);
            NAMES.put("cm", Unit.CM);
            NAMES.put("inches", Unit.INCHES);
            NAMES.put("lines", Unit.LINES);
            NAMES.put("native", Unit.NATIVE);
            NAMES.put("null", Unit.NULL);
            NAMES.put("snpc", Unit.SNPC);
            NAMES.put("mm", Unit.MM);
            NAMES.put("points", Unit.POINTS);
            NAMES.put("picas", Unit.PICAS);
            NAMES.put("bigpts", Unit.BIGPOINTS);
            NAMES.put("dida", Unit.DIDA);
            NAMES.put("cicero", Unit.CICERO);
            NAMES.put("scaledpts", Unit.SCALEDPOINTS);
            NAMES.put("strwidth", Unit.STRINGWIDTH);
            NAMES.put("strheight", Unit.STRINGHEIGHT);
            NAMES.put("strascent", Unit.STRINGASCENT);
            NAMES.put("strdescent", Unit.STRINGDESCENT);

            NAMES.put("char", Unit.CHAR);
            NAMES.put("grobx", Unit.GROBX);
            NAMES.put("groby", Unit.GROBY);
            NAMES.put("grobwidth", Unit.GROBWIDTH);
            NAMES.put("grobheight", Unit.GROBHEIGHT);
            NAMES.put("grobascent", Unit.GROBASCENT);
            NAMES.put("grobdescent", Unit.GROBDESCENT);
            NAMES.put("mylines", Unit.MYLINES);
            NAMES.put("mychar", Unit.MYCHAR);
            NAMES.put("mystrwidth", Unit.MYSTRINGWIDTH);
            NAMES.put("mystrheight", Unit.MYSTRINGHEIGHT);
            // Some pseudonyms
            NAMES.put("centimetre", Unit.CM);
            NAMES.put("centimetres", Unit.CM);
            NAMES.put("centimeter", Unit.CM);
            NAMES.put("centimeters", Unit.CM);
            NAMES.put("in", Unit.INCHES);
            NAMES.put("inch", Unit.INCHES);
            NAMES.put("line", Unit.LINES);
            NAMES.put("millimetre", Unit.MM);
            NAMES.put("millimetres", Unit.MM);
            NAMES.put("millimeter", Unit.MM);
            NAMES.put("millimeters", Unit.MM);
            NAMES.put("point", Unit.POINTS);
            NAMES.put("pt", Unit.POINTS);
        }
    }
}
