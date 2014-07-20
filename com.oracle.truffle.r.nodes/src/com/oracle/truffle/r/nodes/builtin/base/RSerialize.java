/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import java.io.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.gnur.*;
import com.oracle.truffle.r.runtime.RContext.Engine.ParseException;
import com.oracle.truffle.r.runtime.REnvironment.PutException;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;

// Code loosely transcribed from GnuR serialize.c.

/**
 * Serialize/unserialize.
 *
 */
public final class RSerialize {

    private static class Flags {
        static final int IS_OBJECT_BIT_MASK = 1 << 8;
        static final int HAS_ATTR_BIT_MASK = 1 << 9;
        static final int HAS_TAG_BIT_MASK = 1 << 10;
        static final int TYPE_MASK = 255;
        static final int LEVELS_SHIFT = 12;

        int value;
        int ptype;
        @SuppressWarnings("unused") int plevs;
        @SuppressWarnings("unused") boolean isObj;
        boolean hasAttr;
        boolean hasTag;

        static void decodeFlags(Flags flags, int flagsValue) {
            flags.value = flagsValue;
            flags.ptype = flagsValue & Flags.TYPE_MASK;
            flags.plevs = flagsValue >> Flags.LEVELS_SHIFT;
            flags.isObj = (flagsValue & IS_OBJECT_BIT_MASK) != 0;
            flags.hasAttr = (flagsValue & HAS_ATTR_BIT_MASK) != 0;
            flags.hasTag = (flagsValue & HAS_TAG_BIT_MASK) != 0;
        }
    }

    @SuppressWarnings("unused") private static final int MAX_PACKED_INDEX = Integer.MAX_VALUE >> 8;

    @SuppressWarnings("unused")
    private static int packRefIndex(int i) {
        return (i << 8) | SEXPTYPE.REFSXP.code;
    }

    private static int unpackRefIndex(int i) {
        return i >> 8;
    }

    private PStream stream;
    private Object[] refTable = new Object[128];
    private int refTableIndex;
    @SuppressWarnings("unused") private final RFunction hook;

    private RSerialize(RConnection conn) throws IOException {
        this(conn.getInputStream(), null);
    }

    private RSerialize(InputStream is, RFunction hook) throws IOException {
        this.hook = hook;
        byte[] buf = new byte[2];
        is.read(buf);
        switch (buf[0]) {
            case 'A':
                break;
            case 'B':
                break;
            case 'X':
                stream = new XdrFormat(is);
                break;
            case '\n':
            default:
        }
    }

    private int inRefIndex(Flags flags) {
        int i = unpackRefIndex(flags.value);
        if (i == 0) {
            return stream.readInt();
        } else {
            return i;
        }
    }

    private Object addReadRef(Object item) {
        if (refTableIndex >= refTable.length) {
            Object[] newRefTable = new Object[2 * refTable.length];
            System.arraycopy(refTable, 0, newRefTable, 0, refTable.length);
            refTable = newRefTable;
        }
        refTable[refTableIndex++] = item;
        return item;
    }

    private Object getReadRef(int index) {
        return refTable[index - 1];
    }

    public static Object unserialize(RConnection conn) throws IOException {
        RSerialize instance = new RSerialize(conn);
        return instance.unserialize();
    }

    public static Object unserialize(byte[] data, RFunction hook) throws IOException {
// try (BufferedOutputStream bs = new BufferedOutputStream(new FileOutputStream("problem.rds"))) {
// bs.write(data);
// }
        RSerialize instance = new RSerialize(new ByteArrayInputStream(data), hook);
        return instance.unserialize();
    }

    private Object unserialize() {
        int version = stream.readInt();
        @SuppressWarnings("unused")
        int writerVersion = stream.readInt();
        @SuppressWarnings("unused")
        int releaseVersion = stream.readInt();
        assert version == 2; // TODO proper error message
        return readItem();
    }

    private Object readItem() {
        Flags flags = new Flags();
        Flags.decodeFlags(flags, stream.readInt());
        Object result = null;

        SEXPTYPE type = SEXPTYPE.codeMap.get(flags.ptype);
        switch (type) {
            case NILVALUE_SXP:
                return RNull.instance;

            case EMPTYENV_SXP:
                return REnvironment.emptyEnv();

            case BASEENV_SXP:
                return REnvironment.baseEnv();

            case GLOBALENV_SXP:
                return REnvironment.globalEnv();

            case MISSINGARG_SXP:
                return RMissing.instance;

            case REFSXP: {
                return getReadRef(inRefIndex(flags));
            }

            case NAMESPACESXP: {
                RStringVector s = inStringVec(false);
                addReadRef(findNamespace(s));

                break;
            }

            case VECSXP: {
                int len = stream.readInt();
                // TODO long vector support?
                assert len >= 0;
                Object[] data = new Object[len];
                for (int i = 0; i < len; i++) {
                    Object elem = readItem();
                    data[i] = elem;
                }
                // this is a list
                result = RDataFactory.createList(data);
                break;
            }

            case STRSXP: {
                result = inStringVec(true);
                break;
            }

            case INTSXP: {
                int len = stream.readInt();
                int[] data = new int[len];
                boolean complete = RDataFactory.COMPLETE_VECTOR; // really?
                for (int i = 0; i < len; i++) {
                    data[i] = stream.readInt();
                }
                result = RDataFactory.createIntVector(data, complete);
                break;
            }

            case LGLSXP: {
                int len = stream.readInt();
                byte[] data = new byte[len];
                boolean complete = RDataFactory.COMPLETE_VECTOR; // really?
                for (int i = 0; i < len; i++) {
                    data[i] = (byte) stream.readInt();
                }
                result = RDataFactory.createLogicalVector(data, complete);
                break;
            }

            case REALSXP: {
                int len = stream.readInt();
                double[] data = new double[len];
                boolean complete = RDataFactory.COMPLETE_VECTOR; // really?
                for (int i = 0; i < len; i++) {
                    data[i] = stream.readDouble();
                }
                result = RDataFactory.createDoubleVector(data, complete);
                break;
            }

            case CHARSXP: {
                int len = stream.readInt();
                if (len == -1) {
                    return RRuntime.STRING_NA;
                } else {
                    result = stream.readString(len);
                }
                break;
            }

            case LISTSXP:
            case LANGSXP:
            case CLOSXP:
            case PROMSXP:
            case DOTSXP: {
                Object attrItem = null;
                RSymbol tagItem = null;
                if (flags.hasAttr) {
                    attrItem = readItem();

                }
                if (flags.hasTag) {
                    tagItem = (RSymbol) readItem();
                }
                Object carItem = readItem();
                Object cdrItem = readItem();
                RPairList pairList = new RPairList(carItem, cdrItem, tagItem == null ? null : tagItem.getName(), type);
                result = pairList;
                if (attrItem != null) {
                    // Can't attribute a RPairList
                    // pairList.setAttr(name, value);
                }
                if (type == SEXPTYPE.CLOSXP) {
                    // must convert the RPairList to a FastR RFunction
                    // We could convert to an AST directly, but it is easier and more robust
                    // to deparse and reparse.
                    String deparse = DeparseGnu.deparse(result);
                    try {
                        // TODO is there a problem with the lexical environment?
                        RExpression expr = RContext.getEngine().parse(deparse);
                        RFunction func = (RFunction) RContext.getEngine().eval(expr, new REnvironment.NewEnv(REnvironment.globalEnv(), 0));
                        result = func;
                    } catch (RContext.Engine.ParseException | PutException ex) {
                        // denotes a deparse/eval error, which is an unrecoverable bug
                        Utils.fail("internal deparse error");
                    }
                }
                break;
            }

            case SYMSXP: {
                String name = (String) readItem();
                result = RDataFactory.createSymbol(name);
                addReadRef(result);
                break;
            }

            default:
                assert false;
        }
        // TODO SETLEVELS
        if (type == SEXPTYPE.CHARSXP) {
            /*
             * With the CHARSXP cache maintained through the ATTRIB field that field has already
             * been filled in by the mkChar/mkCharCE call above, so we need to leave it alone. If
             * there is an attribute (as there might be if the serialized data was created by an
             * older version) we read and ignore the value.
             */
            if (flags.hasAttr) {
                readItem();
            }
        } else {
            if (flags.hasAttr) {
                Object attr = readItem();
                // Eventually we can use RAttributable
                RVector vec = (RVector) result;
                /*
                 * GnuR uses a pairlist to represent attributes, whereas FastR uses the abstract
                 * RAttributes class.
                 */
                RPairList pl = (RPairList) attr;
                while (true) {
                    String tag = pl.getTag();
                    Object car = pl.car();
                    // Eventually we can just use the generic setAttr
                    if (tag.equals(RRuntime.NAMES_ATTR_KEY)) {
                        vec.setNames(car);
                    } else if (tag.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
                        vec.setDimNames((RList) car);
                    } else if (tag.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
                        vec.setRowNames(car);
                    } else if (tag.equals(RRuntime.CLASS_ATTR_KEY)) {
                        RVector.setClassAttr(vec, (RStringVector) car, null);
                    } else {
                        vec.setAttr(tag, car);
                    }
                    Object cdr = pl.cdr();
                    if (cdr instanceof RNull) {
                        break;
                    } else {
                        pl = (RPairList) cdr;
                    }
                }
            }
        }

        return result;
    }

    private RStringVector inStringVec(boolean strsxp) {
        if (!strsxp) {
            if (stream.readInt() != 0) {
                throw RError.error(Message.GENERIC, "names in persistent strings are not supported yet");
            }
        }
        int len = stream.readInt();
        String[] data = new String[len];
        boolean complete = RDataFactory.COMPLETE_VECTOR; // optimistic
        for (int i = 0; i < len; i++) {
            String item = (String) readItem();
            if (RRuntime.isNA(item)) {
                complete = RDataFactory.INCOMPLETE_VECTOR;
            }
            data[i] = item;
        }
        return RDataFactory.createStringVector(data, complete);
    }

    private static RCallNode getNamespaceCall;

    private static REnvironment findNamespace(RStringVector name) {
        if (getNamespaceCall == null) {
            try {
                getNamespaceCall = (RCallNode) ((RLanguage) RContext.getEngine().parse("..getNamespace(name)").getDataAt(0)).getRep();
            } catch (ParseException ex) {
                // most unexpected
                Utils.fail("findNameSpace");
            }
        }
        RCallNode call = RCallNode.createCloneReplacingFirstArg(getNamespaceCall, ConstantNode.create(name));
        try {
            return (REnvironment) RContext.getEngine().eval(RDataFactory.createLanguage(call), REnvironment.globalEnv());
        } catch (PutException ex) {
            throw RError.error(null, ex);
        }
    }

    private abstract static class PStream {
        @SuppressWarnings("unused") protected InputStream is;

        PStream(InputStream is) {
            this.is = is;
        }

        int readInt() {
            notImpl();
            return -1;
        }

        String readString(@SuppressWarnings("unused") int len) {
            notImpl();
            return null;
        }

        double readDouble() {
            notImpl();
            return 0.0;
        }

        private static void notImpl() {
            Utils.fail("not implemented");

        }

    }

    @SuppressWarnings("unused")
    private static class AsciiFormat extends PStream {
        AsciiFormat(InputStream is) {
            super(is);
        }
    }

    @SuppressWarnings("unused")
    private static class BinaryFormat extends PStream {
        BinaryFormat(InputStream is) {
            super(is);
        }
    }

    private static class XdrFormat extends PStream {
        /**
         * XDR buffer handling. Copied and modified from code in <a
         * href="https://java.net/projects/yanfs">YANFS</a>, developed at Sun Microsystems.
         */
        public static class Xdr {
            private byte[] buf;
            @SuppressWarnings("unused") private int size;
            private int offset;

            /**
             * Build a new Xdr object with a buffer of given size.
             *
             * @param size of the buffer in bytes
             */
            Xdr(int size) {
                this.buf = new byte[size];
                this.size = size;
                this.offset = 0;
            }

            /**
             * Get an integer from the buffer.
             *
             * @return integer
             */
            int getInt() {
                return ((buf[offset++] & 0xff) << 24 | (buf[offset++] & 0xff) << 16 | (buf[offset++] & 0xff) << 8 | (buf[offset++] & 0xff));
            }

            /**
             * Put an integer into the buffer.
             *
             * @param i Integer to store in XDR buffer.
             */
            void putInt(int i) {
                buf[offset++] = (byte) (i >>> 24);
                buf[offset++] = (byte) (i >> 16);
                buf[offset++] = (byte) (i >> 8);
                buf[offset++] = (byte) i;
            }

            /**
             * Get an unsigned integer from the buffer,
             *
             * <br>
             * Note that Java has no unsigned integer type so we must return it as a long.
             *
             * @return long
             */
            @SuppressWarnings("unused")
            long getUInt() {
                return ((buf[offset++] & 0xff) << 24 | (buf[offset++] & 0xff) << 16 | (buf[offset++] & 0xff) << 8 | (buf[offset++] & 0xff));
            }

            /**
             * Put an unsigned integer into the buffer
             *
             * Note that Java has no unsigned integer type so we must submit it as a long.
             *
             * @param i unsigned integer to store in XDR buffer.
             */
            @SuppressWarnings("unused")
            void putUInt(long i) {
                buf[offset++] = (byte) (i >>> 24 & 0xff);
                buf[offset++] = (byte) (i >> 16);
                buf[offset++] = (byte) (i >> 8);
                buf[offset++] = (byte) i;
            }

            /**
             * Get a long from the buffer.
             *
             * @return long
             */
            long getHyper() {
                return ((long) (buf[offset++] & 0xff) << 56 | (long) (buf[offset++] & 0xff) << 48 | (long) (buf[offset++] & 0xff) << 40 | (long) (buf[offset++] & 0xff) << 32 |
                                (long) (buf[offset++] & 0xff) << 24 | (long) (buf[offset++] & 0xff) << 16 | (long) (buf[offset++] & 0xff) << 8 | buf[offset++] & 0xff);
            }

            /**
             * Put a long into the buffer.
             *
             * @param i long to store in XDR buffer
             */
            void putHyper(long i) {
                buf[offset++] = (byte) (i >>> 56);
                buf[offset++] = (byte) ((i >> 48) & 0xff);
                buf[offset++] = (byte) ((i >> 40) & 0xff);
                buf[offset++] = (byte) ((i >> 32) & 0xff);
                buf[offset++] = (byte) ((i >> 24) & 0xff);
                buf[offset++] = (byte) ((i >> 16) & 0xff);
                buf[offset++] = (byte) ((i >> 8) & 0xff);
                buf[offset++] = (byte) (i & 0xff);
            }

            /**
             * Get a floating point number from the buffer.
             *
             * @return float
             */
            double getDouble() {
                return (Double.longBitsToDouble(getHyper()));
            }

            /**
             * Put a floating point number into the buffer.
             *
             * @param f float
             */
            @SuppressWarnings("unused")
            void putDouble(double f) {
                putHyper(Double.doubleToLongBits(f));
            }

            String string(int len) {
                String s = new String(buf, offset, len);
                offset += len;
                return s;
            }

            /**
             * Put a counted array of bytes into the buffer.
             *
             * @param b byte array
             * @param len number of bytes to encode
             */
            @SuppressWarnings("unused")
            void putBytes(byte[] b, int len) {
                putBytes(b, 0, len);
            }

            /**
             * Put a counted array of bytes into the buffer.
             *
             * @param b byte array
             * @param boff offset into byte array
             * @param len number of bytes to encode
             */
            void putBytes(byte[] b, int boff, int len) {
                putInt(len);
                System.arraycopy(b, boff, buf, offset, len);
                offset += len;
            }

            /**
             * Put a counted array of bytes into the buffer. The length is not encoded.
             *
             * @param b byte array
             * @param boff offset into byte array
             * @param len number of bytes to encode
             */
            void putRawBytes(byte[] b, int boff, int len) {
                System.arraycopy(b, boff, buf, offset, len);
                offset += len;
            }

            void ensureCanAdd(int n) {
                if (offset + n > buf.length) {
                    byte[] b = new byte[offset + n];
                    System.arraycopy(buf, 0, b, 0, buf.length);
                    buf = b;
                }
            }
        }

        private Xdr xdr;

        XdrFormat(InputStream is) throws IOException {
            super(is);
            byte[] isbuf = new byte[4096];
            xdr = new Xdr(0);
            int count = 0;
            // read entire stream
            while (true) {
                int nr = is.read(isbuf, 0, isbuf.length);
                if (nr == -1) {
                    break;
                }
                xdr.ensureCanAdd(nr);
                xdr.putRawBytes(isbuf, 0, nr);
                count += nr;
            }
            xdr.size = count;
            xdr.offset = 0;
        }

        @Override
        int readInt() {
            return xdr.getInt();
        }

        @Override
        String readString(int len) {
            String result = xdr.string(len);
            return result;
        }

        @Override
        double readDouble() {
            return xdr.getDouble();
        }
    }

    /**
     * Deparses a GnuR object, primarily usedf for convertinf closures to FastR format.
     */
    private static class DeparseGnu {
        private static enum PP {
            FUNCALL,
            RETURN,
            BINARY,
            BINARY2,
            UNARY,
            IF,
            WHILE,
            FOR,
            ASSIGN,
            CURLY,
            SUBSET,
            DOLLAR;
        }

        private static final int PREC_SUM = 9;
        private static final int PREC_SIGN = 13;

        private static class PPInfo {
            final PP kind;
            final int prec;
            final boolean rightassoc;

            PPInfo(PP kind, int prec, boolean rightassoc) {
                this.kind = kind;
                this.prec = prec;
                this.rightassoc = rightassoc;
            }

        }

        private static class Func {
            final String op;
            final PPInfo info;

            Func(String op, PPInfo info) {
                this.op = op;
                this.info = info;
            }

        }

        // @formatter:off
        private static final Func[] FUNCTAB = new Func[]{
            new Func("+", new PPInfo(PP.BINARY, PREC_SUM, false)),
            new Func("-", new PPInfo(PP.BINARY, PREC_SUM, false)),
            new Func("*", new PPInfo(PP.BINARY, 10, false)),
            new Func("/", new PPInfo(PP.BINARY, 10, false)),
            new Func("^", new PPInfo(PP.BINARY2, 14, false)),
            new Func("%%", new PPInfo(PP.BINARY2, 11, false)),
            new Func("%/%", new PPInfo(PP.BINARY2, 11, false)),
            new Func("%*%", new PPInfo(PP.BINARY2, 11, false)),
            new Func("==", new PPInfo(PP.BINARY, 8, false)),
            new Func("!=", new PPInfo(PP.BINARY, 8, false)),
            new Func("<", new PPInfo(PP.BINARY, 8, false)),
            new Func("<=", new PPInfo(PP.BINARY, 8, false)),
            new Func(">=", new PPInfo(PP.BINARY, 8, false)),
            new Func(">", new PPInfo(PP.BINARY, 8, false)),
            new Func("&", new PPInfo(PP.BINARY, 6, false)),
            new Func("|", new PPInfo(PP.BINARY, 5, false)),
            new Func("!", new PPInfo(PP.BINARY, 7, false)),
            new Func("&&", new PPInfo(PP.BINARY, 6, false)),
            new Func("||", new PPInfo(PP.BINARY, 5, false)),
            new Func(":", new PPInfo(PP.BINARY2, 12, false)),

            new Func("if", new PPInfo(PP.IF, 0, false)),
            new Func("{", new PPInfo(PP.CURLY, 0, false)),
            new Func("return", new PPInfo(PP.RETURN, 0, false)),
            new Func("<-", new PPInfo(PP.ASSIGN, 1, true)),
            new Func("[", new PPInfo(PP.SUBSET, 17, false)),
            new Func("$", new PPInfo(PP.DOLLAR, 15, false)),


        };
        // @formatter:on

        private static final PPInfo BUILTIN = new PPInfo(PP.FUNCALL, 0, false);

        static PPInfo ppInfo(String op) {
            for (Func func : FUNCTAB) {
                if (func.op.equals(op)) {
                    return func.info;
                }
            }
            // must be a builtin that we don't have in FUNCTAB
            return BUILTIN;
        }

        @SuppressWarnings("unused")
        private static class State {
            private final StringBuilder sb = new StringBuilder();
            private int linenumber;
            private int len;
            private int incurly;
            private int inlist;
            private boolean startline;
            private int indent;
            private int cutoff;
            private boolean backtick;
            private int opts;
            private int sourceable;
            private int longstring;
            private int maxlines;
            private boolean active = true;
            private int isS4;

            State(int widthCutOff, boolean backtick, int maxlines) {
                this.cutoff = widthCutOff;
                this.backtick = backtick;
                this.maxlines = maxlines;
            }

            void preAppend() {
                if (startline) {
                    startline = false;
                    indent();
                }
            }

            void indent() {
                for (int i = 1; i <= indent; i++) {
                    if (i <= 4) {
                        append("    ");
                    } else {
                        append("  ");
                    }
                }
            }

            void append(String s) {
                preAppend();
                sb.append(s);
                len += s.length();
            }

            void append(char ch) {
                preAppend();
                sb.append(ch);
                len++;
            }

            boolean linebreak(boolean lbreak) {
                boolean result = lbreak;
                if (len > cutoff) {
                    if (!lbreak) {
                        result = true;
                        indent++;
                    }
                    writeline();
                }
                return result;
            }

            void writeline() {
                // nl for debugging really, we don't care about format,
                // although line length couldbe an issues also.
                sb.append('\n');
                linenumber++;
                if (linenumber >= maxlines) {
                    active = false;
                }
                /* reset */
                len = 0;
                startline = true;
            }

        }

        /**
         * Version for use by {@link RSerialize}.
         */
        @SlowPath
        static String deparse(Object obj) {
            State state = new State(80, false, Integer.MAX_VALUE);
            return deparse2buff(state, obj).sb.toString();
        }

        @SlowPath
        private static State deparse2buff(State state, Object obj) {
            boolean lbreak = false;
            if (!state.active) {
                return state;
            }

            SEXPTYPE type = typeof(obj);
            switch (type) {
                case NILSXP:
                    state.append("NULL");
                    break;

                case SYMSXP:
                    // TODO backtick
                    state.append(((RSymbol) obj).getName());
                    break;

                case CHARSXP:
                    state.append((String) obj);
                    break;

                case CLOSXP: {
                    RPairList f = (RPairList) obj;
                    state.append("function (");
                    args2buff(state, (RPairList) f.car(), false, true);
                    state.append(") ");
                    state.writeline();
                    deparse2buff(state, f.cdr());
                    break;
                }

                case LANGSXP: {
                    RPairList f = (RPairList) obj;
                    Object car = f.car();
                    Object cdr = f.cdr();
                    SEXPTYPE carType = typeof(car);
                    if (carType == SEXPTYPE.SYMSXP) {
                        RSymbol symbol = (RSymbol) car;
                        String op = symbol.getName();
                        RPairList pl = (RPairList) cdr;
                        // TODO BUILTINSXP, SPECIALSXP, userBinOp
                        PPInfo fop = ppInfo(op);
                        if (fop.kind == PP.BINARY) {
                            switch (pl.length()) {
                                case 1:
                                    fop = new PPInfo(PP.UNARY, fop.prec == PREC_SUM ? PREC_SIGN : fop.prec, fop.rightassoc);
                                    break;
                                case 2:
                                    break;
                                default:
                                    assert false;
                            }
                        } else if (fop.kind == PP.BINARY2) {
                            if (pl.length() != 2) {
                                fop = new PPInfo(PP.FUNCALL, 0, false);
                            } /*
                               * else if (userbinop) { fop = new PPInfo(PP.BINARY, fop.prec,
                               * fop.rightassoc); }
                               */
                        }
                        switch (fop.kind) {
                            case CURLY: {
                                state.append(op);
                                state.incurly++;
                                state.indent++;
                                state.writeline();
                                while (pl != null) {
                                    deparse2buff(state, pl.car());
                                    state.writeline();
                                    pl = next(pl);
                                }
                                state.indent--;
                                state.append('}');
                                state.incurly--;
                                break;
                            }

                            case ASSIGN: {
                                // TODO needsparens
                                deparse2buff(state, pl.car());
                                state.append(' ');
                                state.append(op);
                                state.append(' ');
                                deparse2buff(state, ((RPairList) pl.cdr()).car());
                                break;
                            }

                            case IF: {
                                state.append("if (");
                                deparse2buff(state, pl.car());
                                state.append(')');
                                boolean lookahead = false;
                                if (state.incurly > 0 && state.inlist == 0) {
                                    lookahead = curlyahead(pl.cadr());
                                    if (!lookahead) {
                                        state.writeline();
                                        state.indent++;
                                    }
                                }
                                int lenpl = pl.length();
                                if (lenpl > 2) {
                                    deparse2buff(state, pl.cadr());
                                    if (state.incurly > 0 && state.inlist == 0) {
                                        state.writeline();
                                        if (!lookahead) {
                                            state.indent--;
                                        }
                                    } else {
                                        state.append(' ');
                                    }
                                    state.append("else ");
                                    deparse2buff(state, pl.caddr());
                                } else {
                                    deparse2buff(state, pl.cadr());
                                    if (state.incurly > 0 && !lookahead && state.inlist == 0) {
                                        state.indent--;
                                    }
                                }
                                break;
                            }

                            case BINARY:
                            case BINARY2: {
                                // TODO parens
                                deparse2buff(state, pl.car());
                                state.append(' ');
                                state.append(op);
                                state.append(' ');
                                if (fop.kind == PP.BINARY) {
                                    lbreak = state.linebreak(lbreak);
                                }
                                deparse2buff(state, pl.cadr());
                                if (fop.kind == PP.BINARY) {
                                    if (lbreak) {
                                        state.indent--;
                                        lbreak = false;
                                    }
                                }
                                break;
                            }

                            case UNARY: {
                                state.append(op);
                                deparse2buff(state, pl.car());
                                break;
                            }

                            case SUBSET: {
                                deparse2buff(state, pl.car());
                                state.append(op);
                                args2buff(state, (RPairList) pl.cdr(), false, false);
                                if (op.equals("[")) {
                                    state.append(']');
                                } else {
                                    state.append("]]");
                                }
                                break;
                            }

                            case DOLLAR: {
                                // TODO needparens, etc
                                deparse2buff(state, pl.car());
                                state.append(op);
                                deparse2buff(state, pl.cadr());
                                break;
                            }

                            case FUNCALL:
                            case RETURN: {
                                if (state.backtick) {
                                    state.append('`');
                                    state.append(op);
                                    state.append('`');
                                } else {
                                    state.append(op);
                                }
                                state.append('(');
                                state.inlist++;
                                args2buff(state, (RPairList) cdr, false, false);
                                state.inlist--;
                                state.append(')');
                                break;
                            }
                        }
                    } else {
                        // lambda
                        if (parenthesizeCaller(car)) {
                            state.append('(');
                            deparse2buff(state, car);
                            state.append(')');
                        } else {
                            deparse2buff(state, car);
                        }
                        state.append('(');
                        args2buff(state, (RPairList) f.cdr(), false, false);
                        state.append(')');
                    }
                    break;
                }

                case STRSXP:
                case LGLSXP:
                case INTSXP:
                case REALSXP:
                case CPLXSXP:
                case RAWSXP:
                    vector2buff(state, (RVector) obj);
                    break;

                default:
                    assert false;
            }
            return state;
        }

        private static SEXPTYPE typeof(Object obj) {
            Class<?> klass = obj.getClass();
            if (klass == RPairList.class) {
                return ((RPairList) obj).getType();
            } else {
                return SEXPTYPE.typeForClass(klass);
            }
        }

        private static boolean curlyahead(@SuppressWarnings("unused") Object obj) {
            return false;
        }

        private static boolean parenthesizeCaller(@SuppressWarnings("unused") Object s) {
            // TODO implement
            return false;
        }

        private static RPairList next(RPairList pairlist) {
            if (pairlist.cdr() == RNull.instance) {
                return null;
            } else {
                return (RPairList) pairlist.cdr();
            }

        }

        @SlowPath
        private static State args2buff(State state, RPairList args, @SuppressWarnings("unused") boolean lineb, boolean formals) {
            boolean lbreak = false;
            RPairList arglist = args;
            while (arglist != null) {
                if (arglist.getTag() != null) {
                    state.append(arglist.getTag());
                    if (formals) {
                        if (arglist.car() != RMissing.instance) {
                            state.append(" = ");
                            deparse2buff(state, arglist.car());
                        }
                    } else {
                        state.append(" = ");
                        if (arglist.car() != RMissing.instance) {
                            deparse2buff(state, arglist.car());
                        }
                    }
                } else {
                    deparse2buff(state, arglist.car());
                }
                arglist = next(arglist);
                if (arglist != null) {
                    state.append(", ");
                    lbreak = state.linebreak(lbreak);
                }
            }
            if (lbreak) {
                state.indent--;
            }
            return state;
        }

        @SlowPath
        private static State vector2buff(State state, RVector vec) {
            SEXPTYPE type = SEXPTYPE.typeForClass(vec.getClass());
            boolean surround = false;
            int len = vec.getLength();
            if (len == 0) {
                switch (type) {
                    case LGLSXP:
                        state.append("logical(0)");
                        break;
                    case INTSXP:
                        state.append("integer(0)");
                        break;
                    case REALSXP:
                        state.append("numeric(0)");
                        break;
                    case CPLXSXP:
                        state.append("complex(0)");
                        break;
                    case STRSXP:
                        state.append("character(0)");
                        break;
                    case RAWSXP:
                        state.append("raw(0)");
                        break;
                    default:
                        assert false;
                }
            } else if (type == SEXPTYPE.INTSXP) {

            } else {
                // TODO NA checks
                if (len > 1) {
                    state.append("c(");
                }
                for (int i = 0; i < len; i++) {
                    Object element = vec.getDataAtAsObject(i);
                    switch (type) {
                        case STRSXP:
                            // TODO encoding
                            state.append('"');
                            state.append((String) element);
                            state.append('"');
                            break;
                        case LGLSXP:
                            state.append(RRuntime.fromLogical((byte) element) ? "TRUE" : "FALSE");
                            break;
                        case REALSXP:
                            String rep = Double.toString((double) element);
                            if (rep.equals("Infinity")) {
                                rep = "Inf";
                            }
                            state.append(rep);
                            break;
                        default:
                            assert false;
                    }
                }
                if (len > 1) {
                    state.append(')');
                }
                if (surround) {
                    state.append(')');
                }
            }
            return state;
        }

    }

}
