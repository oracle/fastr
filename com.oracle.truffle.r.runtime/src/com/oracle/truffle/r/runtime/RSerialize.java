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
package com.oracle.truffle.r.runtime;

import java.io.*;

import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;
import com.oracle.truffle.r.runtime.gnur.*;

// Code loosely transcribed from GnuR serialize.c.

/**
 * Serialize/unserialize. Only unserialize is implemented currently to support package loading.
 *
 */
// Checkstyle: stop final class check
public class RSerialize {

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

        static Flags decodeFlags(int flagsValue) {
            Flags flags = new Flags();
            flags.value = flagsValue;
            flags.ptype = flagsValue & Flags.TYPE_MASK;
            flags.plevs = flagsValue >> Flags.LEVELS_SHIFT;
            flags.isObj = (flagsValue & IS_OBJECT_BIT_MASK) != 0;
            flags.hasAttr = (flagsValue & HAS_ATTR_BIT_MASK) != 0;
            flags.hasTag = (flagsValue & HAS_TAG_BIT_MASK) != 0;
            return flags;
        }
    }

    /**
     * Provides access to the underlying byte array.
     */
    private static class PByteArrayInputStream extends ByteArrayInputStream {

        public PByteArrayInputStream(byte[] buf) {
            super(buf);
        }

        byte[] getData() {
            return buf;
        }

        int pos() {
            return pos;
        }

    }

    public interface CallHook {
        Object eval(Object arg);
    }

    @SuppressWarnings("unused") private static final int MAX_PACKED_INDEX = Integer.MAX_VALUE >> 8;

    @SuppressWarnings("unused")
    private static int packRefIndex(int i) {
        return (i << 8) | SEXPTYPE.REFSXP.code;
    }

    private static int unpackRefIndex(int i) {
        return i >> 8;
    }

    protected PStream stream;
    private Object[] refTable = new Object[128];
    private int refTableIndex;
    private final CallHook hook;
    private static boolean trace;
    private final int depth;

    private RSerialize(RConnection conn, int depth) throws IOException {
        this(conn.getInputStream(), null, depth);
    }

    private RSerialize(InputStream is, CallHook hook, int depth) throws IOException {
        this.hook = hook;
        this.depth = depth;
        byte[] buf = new byte[2];
        is.read(buf);
        switch (buf[0]) {
            case 'A':
                // TODO error
                break;
            case 'B':
                // TODO error
                break;
            case 'X':
                stream = new XdrFormat(is);
                break;
            case '\n':
                // TODO special case in 'A'
            default:
                // TODO error
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

    public static Object unserialize(RConnection conn, int depth) throws IOException {
        RSerialize instance = trace ? new TracingRSerialize(conn, depth) : new RSerialize(conn, depth);
        return instance.unserialize();
    }

    /**
     * This variant exists for the {@code laxyLoadDBFetch} function. In certain cases, when
     * {@link #persistentRestore} is called, an R function needs to be evaluated with an argument
     * read from the serialized stream. This is handled with a callback object.
     */
    public static Object unserialize(byte[] data, CallHook hook, int depth) throws IOException {
        InputStream is = new PByteArrayInputStream(data);
        RSerialize instance = trace ? new TracingRSerialize(is, hook, depth) : new RSerialize(is, hook, depth);
        return instance.unserialize();
    }

    private Object unserialize() throws IOException {
        int version = stream.readInt();
        @SuppressWarnings("unused")
        int writerVersion = stream.readInt();
        @SuppressWarnings("unused")
        int releaseVersion = stream.readInt();
        assert version == 2; // TODO proper error message
        Object result = readItem();
        return result;
    }

    protected Object readItem() throws IOException {
        Flags flags = Flags.decodeFlags(stream.readInt());
        return readItem(flags);
    }

    protected Object readItem(Flags flags) throws IOException {
        Object result = null;

        SEXPTYPE type = SEXPTYPE.mapInt(flags.ptype);
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
                return addReadRef(RContext.getRASTHelper().findNamespace(s, depth));
            }

            case PERSISTSXP: {
                RStringVector sv = inStringVec(false);
                result = persistentRestore(sv);
                return addReadRef(result);
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
                    int intVal = stream.readInt();
                    if (intVal == RRuntime.INT_NA) {
                        complete = false;
                    }
                    data[i] = intVal;
                }
                result = RDataFactory.createIntVector(data, complete);
                break;
            }

            case LGLSXP: {
                int len = stream.readInt();
                byte[] data = new byte[len];
                boolean complete = RDataFactory.COMPLETE_VECTOR; // really?
                for (int i = 0; i < len; i++) {
                    int intVal = stream.readInt();
                    if (intVal == RRuntime.INT_NA) {
                        complete = false;
                        data[i] = RRuntime.LOGICAL_NA;
                    } else {
                        data[i] = (byte) intVal;
                    }

                }
                result = RDataFactory.createLogicalVector(data, complete);
                break;
            }

            case REALSXP: {
                int len = stream.readInt();
                double[] data = new double[len];
                boolean complete = RDataFactory.COMPLETE_VECTOR; // really?
                for (int i = 0; i < len; i++) {
                    double doubleVal = stream.readDouble();
                    if (doubleVal == RRuntime.DOUBLE_NA) {
                        complete = false;
                    }
                    data[i] = doubleVal;
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
                Object tagItem = null;
                if (flags.hasAttr) {
                    attrItem = readItem();

                }
                if (flags.hasTag) {
                    tagItem = readItem();
                }
                Object carItem = readItem();
                Object cdrItem = readItem();
                RPairList pairList = RDataFactory.createPairList(carItem, cdrItem, tagItem, type);
                result = pairList;
                if (attrItem != null) {
                    assert false;
                    // TODO figure out what attrItem is
                    // pairList.setAttr(name, value);
                }
                if (type == SEXPTYPE.CLOSXP) {
                    // must convert the RPairList to a FastR RFunction
                    // We could convert to an AST directly, but it is easier and more robust
                    // to deparse and reparse.
                    RPairList rpl = (RPairList) result;
                    String deparse = RDeparse.deparse(rpl);
                    try {
                        /*
                         * The tag of result is the enclosing environment (from NAMESPACESEXP) for
                         * the function. However the namespace is locked, so can't just eval there
                         * (and overwrite the promise), so we fix the enclosing frame up on return.
                         */
                        RExpression expr = RContext.getEngine().parse(deparse);
                        RFunction func = (RFunction) RContext.getEngine().eval(expr, RDataFactory.createNewEnv(REnvironment.emptyEnv(), 0), depth + 1);
                        func.setEnclosingFrame(((REnvironment) rpl.getTag()).getFrame());
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

            case BCODESXP: {
                result = readBC();
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
                    RSymbol tagSym = (RSymbol) pl.getTag();
                    String tag = tagSym.getName();
                    Object car = pl.car();
                    // TODO just use the generic setAttr
                    if (tag.equals(RRuntime.NAMES_ATTR_KEY)) {
                        vec.setNames(car);
                    } else if (tag.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
                        vec.setDimNames((RList) car);
                    } else if (tag.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
                        vec.setRowNames(car);
                    } else if (tag.equals(RRuntime.CLASS_ATTR_KEY)) {
                        result = RVector.setVectorClassAttr(vec, (RStringVector) car, null, null);
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

    private RStringVector inStringVec(boolean strsxp) throws IOException {
        if (!strsxp) {
            if (stream.readInt() != 0) {
                throw RError.nyi(null, "names in persistent strings are not supported yet");
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

    private Object persistentRestore(RStringVector sv) throws IOException {
        if (hook == null) {
            throw new IOException("no restore method available");
        }
        // have to evaluate the hook function with sv as argument.
        Object result = hook.eval(sv);
        return result;
    }

    /**
     * Read GnuR bytecode. Not because we care, but it may be in there.
     */
    private Object readBC() throws IOException {
        int repsLength = stream.readInt();
        Object[] reps = new Object[repsLength];
        return readBC1(reps);
    }

    private Object readBC1(Object[] reps) throws IOException {
        Object car = readItem();
        // TODO R_bcEncode(car) (if we care)
        Object cdr = readBCConsts(reps);
        return RDataFactory.createPairList(car, cdr, null, SEXPTYPE.BCODESXP);
    }

    private Object readBCConsts(Object[] reps) throws IOException {
        int n = stream.readInt();
        Object[] ans = new Object[n];
        for (int i = 0; i < n; i++) {
            int intType = stream.readInt();
            SEXPTYPE type = SEXPTYPE.mapInt(intType);
            switch (type) {
                case BCODESXP: {
                    Object c = readBC1(reps);
                    ans[i] = c;
                    break;
                }
                case LANGSXP:
                case LISTSXP:
                case BCREPDEF:
                case BCREPREF:
                case ATTRLANGSXP:
                case ATTRLISTSXP: {
                    Object c = readBCLang(type, reps);
                    ans[i] = c;
                    break;
                }

                default:
                    ans[i] = readItem();
            }
        }
        return RDataFactory.createList(ans);
    }

    private Object readBCLang(final SEXPTYPE typeArg, Object[] reps) throws IOException {
        SEXPTYPE type = typeArg;
        switch (type) {
            case BCREPREF:
                return reps[stream.readInt()];
            case BCREPDEF:
            case LANGSXP:
            case LISTSXP:
            case ATTRLANGSXP:
            case ATTRLISTSXP: {
                int pos = -1;
                boolean hasattr = false;
                if (type == SEXPTYPE.BCREPDEF) {
                    pos = stream.readInt();
                    type = SEXPTYPE.mapInt(stream.readInt());
                }
                switch (type) {
                    case ATTRLANGSXP:
                        type = SEXPTYPE.LANGSXP;
                        hasattr = true;
                        break;
                    case ATTRLISTSXP:
                        type = SEXPTYPE.LISTSXP;
                        hasattr = true;
                        break;
                }
                if (hasattr) {
                    readItem();
                    assert false;
                }
                Object tag = readItem();
                Object car = readBCLang(SEXPTYPE.mapInt(stream.readInt()), reps);
                Object cdr = readBCLang(SEXPTYPE.mapInt(stream.readInt()), reps);
                Object ans = RDataFactory.createPairList(car, cdr, tag, type);
                if (pos >= 0)
                    reps[pos] = ans;
                return ans;
            }
            default: {
                Object ans = readItem();
                return ans;
            }
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
                this(new byte[size], 0);
            }

            Xdr(byte[] data, int offset) {
                this.buf = data;
                this.size = data.length;
                this.offset = offset;

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

        private final Xdr xdr;

        XdrFormat(InputStream is) throws IOException {
            super(is);
            if (is instanceof PByteArrayInputStream) {
                // we already have the data and we have read the beginning
                PByteArrayInputStream pbis = (PByteArrayInputStream) is;
                xdr = new Xdr(pbis.getData(), pbis.pos());
            } else {
                byte[] isbuf = new byte[RConnection.GZIP_BUFFER_SIZE];
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
     * Traces the items read for debugging.
     */
    private static final class TracingRSerialize extends RSerialize {
        private int depth;

        private TracingRSerialize(RConnection conn, int depth) throws IOException {
            this(conn.getInputStream(), null, depth);
        }

        private TracingRSerialize(InputStream is, CallHook hook, int depth) throws IOException {
            super(is, hook, depth);
        }

        @Override
        protected Object readItem() throws IOException {
            // CheckStyle: stop system..print check
            Flags flags = Flags.decodeFlags(stream.readInt());
            SEXPTYPE type = SEXPTYPE.mapInt(flags.ptype);
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }
            System.out.printf("%d %s%n", depth, type);
            depth++;
            Object result = super.readItem(flags);
            depth--;
            return result;
        }

    }
}
