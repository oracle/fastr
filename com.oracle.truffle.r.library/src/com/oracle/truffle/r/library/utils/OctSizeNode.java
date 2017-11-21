package com.oracle.truffle.r.library.utils;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.unary.SizeToOctalRawNode;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RTypes;

@TypeSystemReference(RTypes.class)
public abstract class OctSizeNode extends RExternalBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(OctSizeNode.class);
        casts.arg(0).mustNotBeMissing().mustNotBeNull().returnIf(Predef.integerValue()).asDoubleVector().findFirst();
    }

    @Child private SizeToOctalRawNode sizeToOctal = SizeToOctalRawNode.create();

    @Specialization
    protected RRawVector octSize(Object size) {
        return sizeToOctal.execute(size);
    }

    public static OctSizeNode create() {
        return OctSizeNodeGen.create();
    }
}