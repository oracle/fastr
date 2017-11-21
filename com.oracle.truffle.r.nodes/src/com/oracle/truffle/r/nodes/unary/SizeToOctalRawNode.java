package com.oracle.truffle.r.nodes.unary;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.nodes.SetDataAt;

public abstract class SizeToOctalRawNode extends UnaryNode {

    private Charset asciiCharset;

    public abstract RRawVector execute(Object size);

    @Specialization
    protected RRawVector octSize(int s) {
        return RDataFactory.createRawVector(toOctalAsciiString(s));
    }

    @TruffleBoundary
    private byte[] toOctalAsciiString(int s) {
        if (asciiCharset == null) {
            asciiCharset = Charset.forName("US-ASCII");
        }

        ByteBuffer encode = asciiCharset.encode(Integer.toOctalString(s));
        byte[] result = new byte[11];
        Arrays.fill(result, (byte) 48);
        encode.get(result);
        return result;
    }

    // Transcribed from ".../utils/src/stubs.c"
    @Specialization
    protected RRawVector octSize(double size,
                    @Cached("create()") SetDataAt.Raw setDataNode) {

        double s = size;
        if (!RRuntime.isFinite(s) && s >= 0) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, "size must be finite and >= 0");
        }

        RRawVector ans = RDataFactory.createRawVector(11);
        byte[] store = ans.getInternalStore();

        for (int i = 0; i < 11; i++) {
            double s2 = Math.floor(s / 8.0);
            double t = s - 8.0 * s2;
            s = s2;
            setDataNode.setDataAtAsObject(ans, store, 10 - i, (byte) (48.0 + t));
        }
        return ans;
    }

    public static SizeToOctalRawNode create() {
        return SizeToOctalRawNodeGen.create();

    }
}
