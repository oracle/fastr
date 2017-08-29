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

import static com.oracle.truffle.r.runtime.nmath.RMath.fmax2;
import static com.oracle.truffle.r.runtime.nmath.RMath.fmin2;

import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

final class GridUtils {
    private GridUtils() {
        // only static members
    }

    static double justify(double coord, double size, double justification) {
        // justification is supposed to be either between 0 and 1
        return coord - size * justification;
    }

    static double getStringWidth(DrawingContext drawingContext, GridDevice dev, String str) {
        String[] lines;
        double result = 0;
        lines = str.split("\n");
        for (String line : lines) {
            result = Math.max(result, dev.getStringWidth(drawingContext, line));
        }
        return result;
    }

    /**
     * Returns the amount of justification required. I.e. transforms the justification from value
     * between 0 and 1 to the value within size.
     */
    static double justification(double size, double justification) {
        return -size * justification;
    }

    static double getDataAtMod(RAbstractDoubleVector vec, int idx) {
        return vec.getDataAt(idx % vec.getLength());
    }

    static int getDataAtMod(RAbstractIntVector vec, int idx) {
        return vec.getDataAt(idx % vec.getLength());
    }

    @ExplodeLoop
    static int maxLength(RAbstractVector... units) {
        int result = 0;
        for (RAbstractVector unit : units) {
            result = Math.max(result, Unit.getLength(unit));
        }
        return result;
    }

    @ExplodeLoop
    static double fmax(double firstVal, double... vals) {
        double result = firstVal;
        for (double val : vals) {
            result = fmax2(result, val);
        }
        return result;
    }

    @ExplodeLoop
    static double fmin(double firstVal, double... vals) {
        double result = firstVal;
        for (double val : vals) {
            result = fmin2(result, val);
        }
        return result;
    }

    static RDoubleVector createDoubleVector(double... values) {
        return RDataFactory.createDoubleVector(values, RDataFactory.COMPLETE_VECTOR);
    }

    static boolean hasRClass(RAttributable obj, String clazz) {
        RStringVector classAttr = obj.getClassAttr();
        if (classAttr == null) {
            return false;
        }
        for (int i = 0; i < classAttr.getLength(); i++) {
            if (classAttr.getDataAt(i).equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    static RList asListOrNull(Object value) {
        if (value == null || value == RNull.instance) {
            return null;
        }
        if (!(value instanceof RList)) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Expected list");
        }
        return (RList) value;
    }

    static RList asList(Object value) {
        if (!(value instanceof RList)) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Expected list");
        }
        return (RList) value;
    }

    static double getDoubleAt(RAbstractVector vector, int index) {
        if (vector instanceof RAbstractDoubleVector) {
            return ((RAbstractDoubleVector) vector).getDataAt(index);
        } else if (vector instanceof RAbstractIntVector) {
            return ((RAbstractIntVector) vector).getDataAt(index);
        }
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected non double/integer value");
    }

    static double asDouble(Object val) {
        if (val instanceof Double) {
            return (double) val;
        } else if (val instanceof RAbstractDoubleVector) {
            if (((RAbstractDoubleVector) val).getLength() > 0) {
                return ((RAbstractDoubleVector) val).getDataAt(0);
            }
        } else if (val instanceof Integer) {
            return (int) val;
        } else if (val instanceof RAbstractIntVector) {
            if (((RAbstractIntVector) val).getLength() > 0) {
                return ((RAbstractIntVector) val).getDataAt(0);
            }
        }
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected non double/integer value " + val.getClass().getSimpleName());
    }

    static double asDouble(Object val, int cyclicIndex) {
        if (val instanceof Double) {
            return (double) val;
        } else if (val instanceof RAbstractDoubleVector) {
            RAbstractDoubleVector vec = (RAbstractDoubleVector) val;
            if (vec.getLength() > 0) {
                return vec.getDataAt(cyclicIndex % vec.getLength());
            }
        }
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected non double value " + val.getClass().getSimpleName());
    }

    static int asInt(Object val, int cyclicIndex) {
        if (val instanceof Integer) {
            return (int) val;
        } else if (val instanceof RAbstractIntVector) {
            RAbstractIntVector vec = (RAbstractIntVector) val;
            if (vec.getLength() > 0) {
                return vec.getDataAt(cyclicIndex % vec.getLength());
            }
        }
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected non integer value " + val.getClass().getSimpleName());
    }

    static String asString(Object val, int cyclicIndex) {
        if (val instanceof String) {
            return (String) val;
        } else if (val instanceof RAbstractStringVector) {
            RAbstractStringVector vec = (RAbstractStringVector) val;
            if (vec.getLength() > 0) {
                return vec.getDataAt(cyclicIndex % vec.getLength());
            }
        }
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected non character value " + val.getClass().getSimpleName());
    }

    static RAbstractIntVector asIntVector(Object value) {
        if (value instanceof Integer) {
            return RDataFactory.createIntVectorFromScalar((Integer) value);
        } else if (value instanceof RAbstractIntVector) {
            return (RAbstractIntVector) value;
        }
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected non integer value " + value.getClass().getSimpleName());
    }

    public static RAbstractDoubleVector asDoubleVector(Object obj) {
        if (obj instanceof Double) {
            return RDataFactory.createDoubleVectorFromScalar((Double) obj);
        } else if (obj instanceof RAbstractDoubleVector) {
            return (RAbstractDoubleVector) obj;
        }
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected non double value " + obj.getClass().getSimpleName());
    }

    static RAbstractContainer asAbstractContainer(Object value) {
        if (value instanceof Integer) {
            return RDataFactory.createIntVectorFromScalar((Integer) value);
        } else if (value instanceof Double) {
            return RDataFactory.createDoubleVectorFromScalar((Double) value);
        } else if (value instanceof RAbstractContainer) {
            return (RAbstractContainer) value;
        }
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected non abstract container type " + value.getClass().getSimpleName());
    }

    static REnvironment asEnvironment(Object value) {
        if (value instanceof REnvironment) {
            return (REnvironment) value;
        }
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected: value is not environment " + value.getClass().getSimpleName());
    }

    static double sum(double[] values) {
        double result = 0;
        for (int i = 0; i < values.length; i++) {
            result += values[i];
        }
        return result;
    }

    static double sum(RAbstractDoubleVector values, int from, int length) {
        double result = 0;
        for (int i = 0; i < length; i++) {
            result += values.getDataAt(from + i);
        }
        return result;
    }
}
