/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import java.io.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;
import com.oracle.truffle.r.runtime.gnur.*;

// Code loosely transcribed from GnuR serialize.c.

/**
 * Serialize/unserialize.
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

        private Flags() {
            // prevent construction
        }

        public static int ptype(int flagsValue) {
            return flagsValue & Flags.TYPE_MASK;
        }

        @SuppressWarnings("unused")
        public static int plevs(int flagsValue) {
            return flagsValue >> Flags.LEVELS_SHIFT;
        }

        @SuppressWarnings("unused")
        public static boolean isObj(int flagsValue) {
            return (flagsValue & IS_OBJECT_BIT_MASK) != 0;
        }

        public static boolean hasAttr(int flagsValue) {
            return (flagsValue & HAS_ATTR_BIT_MASK) != 0;
        }

        public static boolean hasTag(int flagsValue) {
            return (flagsValue & HAS_TAG_BIT_MASK) != 0;
        }

        public static int packFlags(SEXPTYPE type, @SuppressWarnings("unused") int levs, boolean isObj, boolean hasAttr, boolean hasTag) {
            // TODO levs
            int val = type.code;
            if (isObj) {
                val |= IS_OBJECT_BIT_MASK;
            }
            if (hasAttr) {
                val |= HAS_ATTR_BIT_MASK;
            }
            if (hasTag) {
                val |= HAS_TAG_BIT_MASK;
            }
            return val;
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

    private static final int MAX_PACKED_INDEX = Integer.MAX_VALUE >> 8;

    private static int packRefIndex(int i) {
        return (i << 8) | SEXPTYPE.REFSXP.code;
    }

    private static int unpackRefIndex(int i) {
        return i >> 8;
    }

    private abstract static class Common {

        protected Object[] refTable = new Object[128];
        protected int refTableIndex;
        protected final CallHook hook;
        protected final int depth;

        protected Common(CallHook hook, int depth) {
            this.hook = hook;
            this.depth = depth;
        }

        protected static IOException formatError(byte format, boolean ok) throws IOException {
            throw new IOException("serialized stream format " + (ok ? "not implemented" : "not recognized") + ": " + format);
        }

        protected Object addReadRef(Object item) {
            if (refTableIndex >= refTable.length) {
                Object[] newRefTable = new Object[2 * refTable.length];
                System.arraycopy(refTable, 0, newRefTable, 0, refTable.length);
                refTable = newRefTable;
            }
            refTable[refTableIndex++] = item;
            return item;
        }

        protected Object getReadRef(int index) {
            return refTable[index - 1];
        }

        protected int getRefIndex(Object obj) {
            for (int i = 0; i < refTableIndex; i++) {
                if (refTable[i] == obj) {
                    return i;
                }
            }
            return -1;
        }

    }

    protected static boolean trace;
    protected static boolean traceInit;

    private static boolean trace() {
        if (!traceInit) {
            trace = FastROptions.debugMatches("serialize");
            traceInit = true;
        }
        return trace;
    }

    @TruffleBoundary
    public static Object unserialize(RConnection conn, int depth) throws IOException {
        Input instance = trace() ? new TracingInput(conn, depth) : new Input(conn, depth);
        return instance.unserialize();
    }

    /**
     * This variant exists for the {@code lazyLoadDBFetch} function. In certain cases, when
     * {@link Input#persistentRestore} is called, an R function needs to be evaluated with an
     * argument read from the serialized stream. This is handled with a callback object.
     */
    @TruffleBoundary
    public static Object unserialize(byte[] data, CallHook hook, int depth) throws IOException {
        InputStream is = new PByteArrayInputStream(data);
        Input instance = trace() ? new TracingInput(is, hook, depth) : new Input(is, hook, depth);
        return instance.unserialize();
    }

    private static class Input extends Common {
        protected final PInputStream stream;

        private Input(RConnection conn, int depth) throws IOException {
            this(conn.getInputStream(), null, depth);
        }

        private Input(InputStream is, CallHook hook, int depth) throws IOException {
            super(hook, depth);
            byte[] buf = new byte[2];
            is.read(buf);
            switch (buf[0]) {
                case 'A':
                case 'B':
                    throw formatError(buf[0], true);
                case 'X':
                    stream = new XdrInputFormat(is);
                    break;
                case '\n':
                    // special case in 'A'
                    throw formatError((byte) 'A', true);
                default:
                    throw formatError(buf[0], false);
            }
        }

        private int inRefIndex(int flags) throws IOException {
            int i = unpackRefIndex(flags);
            if (i == 0) {
                return stream.readInt();
            } else {
                return i;
            }
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
            int flags = stream.readInt();
            return readItem(flags);
        }

        protected Object readItem(int flags) throws IOException {
            Object result = null;

            SEXPTYPE type = SEXPTYPE.mapInt(Flags.ptype(flags));
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

                case BASENAMESPACE_SXP:
                    return REnvironment.baseNamespaceEnv();

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

                case ENVSXP: {
                    /*
                     * Behavior varies depending on whether hashtab is present, since this is
                     * optional in GnuR.
                     */
                    int locked = stream.readInt();
                    Object enclos = readItem();
                    REnvironment env = RDataFactory.createNewEnv(enclos == RNull.instance ? REnvironment.baseEnv() : (REnvironment) enclos, 0);
                    addReadRef(result);
                    Object frame = readItem();
                    Object hashtab = readItem();
                    if (frame == RNull.instance) {
                        RList hashList = (RList) hashtab;
                        // GnuR sizes its hash tables, empty slots indicated by RNull
                        for (int i = 0; i < hashList.getLength(); i++) {
                            Object val = hashList.getDataAt(i);
                            if (val == RNull.instance) {
                                continue;
                            }
                            RPairList pl = (RPairList) val;
                            env.safePut(((RSymbol) pl.getTag()).getName(), pl.car());
                        }
                    } else {
                        while (frame != RNull.instance) {
                            RPairList pl = (RPairList) frame;
                            env.safePut(((RSymbol) pl.getTag()).getName(), pl.car());
                            frame = pl.cdr();
                        }
                    }
                    if (locked != 0) {
                        env.lock(false);
                    }
                    Object attr = readItem();
                    if (attr != RNull.instance) {
                        setAttributes(env, attr);
                    }
                    return env;
                }

                case PACKAGESXP: {
                    RStringVector s = inStringVec(false);
                    /*
                     * TODO GnuR eval's findPackageEnv, but we don't want to eval here. That will
                     * call require, so we can only find packages that are already loaded.
                     */
                    REnvironment pkgEnv = REnvironment.lookupOnSearchPath(s.getDataAt(0));
                    if (pkgEnv == null) {
                        pkgEnv = REnvironment.globalEnv();
                    }
                    return pkgEnv;
                }

                case LISTSXP:
                case LANGSXP:
                case CLOSXP:
                case PROMSXP:
                case DOTSXP: {
                    Object attrItem = null;
                    Object tagItem = null;
                    if (Flags.hasAttr(flags)) {
                        attrItem = readItem();

                    }
                    if (Flags.hasTag(flags)) {
                        tagItem = readItem();
                    }
                    Object carItem = readItem();
                    Object cdrItem = readItem();
                    RPairList pairList = RDataFactory.createPairList(carItem, cdrItem, tagItem, type);
                    result = pairList;
                    if (attrItem != null) {
                        setAttributes(pairList, attrItem);
                    }
                    if (type == SEXPTYPE.CLOSXP) {
                        // must convert the RPairList to a FastR RFunction
                        // We could convert to an AST directly, but it is easier and more robust
                        // to deparse and reparse.
                        RPairList rpl = (RPairList) result;
                        String deparse = RDeparse.deparse(rpl);
                        try {
                            /*
                             * The tag of result is the enclosing environment (from NAMESPACESEXP)
                             * for the function. However the namespace is locked, so can't just eval
                             * there (and overwrite the promise), so we fix the enclosing frame up
                             * on return.
                             */
                            Source source = Source.asPseudoFile(deparse, "<package deparse>");
                            RExpression expr = RContext.getEngine().parse(source);
                            RFunction func = (RFunction) RContext.getEngine().eval(expr, RDataFactory.createNewEnv(REnvironment.emptyEnv(), 0), depth + 1);
                            func.setEnclosingFrame(((REnvironment) rpl.getTag()).getFrame());
                            result = func;
                        } catch (RContext.Engine.ParseException | PutException ex) {
                            // denotes a deparse/eval error, which is an unrecoverable bug
                            Utils.fail("internal deparse error");
                        }
                    }
                    return result;
                }

                /*
                 * These break out of the switch to have their ATTR, LEVELS, and OBJECT fields
                 * filled in.
                 */

                case VECSXP: {
                    int len = stream.readInt();
                    // TODO long vector support?
                    assert len >= 0;
                    Object[] data = new Object[len];
                    for (int i = 0; i < len; i++) {
                        Object elem = readItem();
                        data[i] = elem;
                    }
                    // this could (ultimately) be a list,factor or dataframe
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
                    boolean complete = RDataFactory.COMPLETE_VECTOR;
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
                    boolean complete = RDataFactory.COMPLETE_VECTOR;
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
                    boolean complete = RDataFactory.COMPLETE_VECTOR;
                    for (int i = 0; i < len; i++) {
                        double doubleVal = stream.readDouble();
                        if (RRuntime.isNA(doubleVal)) {
                            complete = false;
                        }
                        data[i] = doubleVal;
                    }
                    result = RDataFactory.createDoubleVector(data, complete);
                    break;
                }

                case CPLXSXP: {
                    int len = stream.readInt();
                    double[] data = new double[2 * len];
                    boolean complete = RDataFactory.COMPLETE_VECTOR;
                    for (int i = 0; i < len; i++) {
                        int ix = 2 * i;
                        double reVal = stream.readDouble();
                        if (RRuntime.isNA(reVal)) {
                            complete = false;
                        }
                        double imVal = stream.readDouble();
                        if (RRuntime.isNA(imVal)) {
                            complete = false;
                        }
                        data[ix] = reVal;
                        data[ix + 1] = imVal;
                    }
                    result = RDataFactory.createComplexVector(data, complete);
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
                    throw RInternalError.unimplemented();
            }
            // TODO SETLEVELS
            if (type == SEXPTYPE.CHARSXP) {
                /*
                 * With the CHARSXP cache maintained through the ATTRIB field that field has already
                 * been filled in by the mkChar/mkCharCE call above, so we need to leave it alone.
                 * If there is an attribute (as there might be if the serialized data was created by
                 * an older version) we read and ignore the value.
                 */
                if (Flags.hasAttr(flags)) {
                    readItem();
                }
            } else {
                if (Flags.hasAttr(flags)) {
                    Object attr = readItem();
                    result = setAttributes(result, attr);
                }
            }

            return result;
        }

        /**
         * GnuR uses a pairlist to represent attributes, whereas FastR uses the abstract RAttributes
         * class. FastR also uses different types to represent data/frame and factor which is
         * handled in the setClassAttr
         */
        private static Object setAttributes(final Object object, Object attr) {
            RAttributable rAttributable = (RAttributable) object;
            RPairList pl = (RPairList) attr;
            Object result = object;
            while (true) {
                RSymbol tagSym = (RSymbol) pl.getTag();
                String tag = tagSym.getName().intern();
                // this may convert a plain vector to a data.frame or factor
                if (result instanceof RVector && tag.equals(RRuntime.CLASS_ATTR_KEY)) {
                    result = ((RVector) result).setClassAttr((RStringVector) pl.car());
                } else {
                    rAttributable.setAttr(tag, pl.car());
                }
                Object cdr = pl.cdr();
                if (cdr instanceof RNull) {
                    break;
                } else {
                    pl = (RPairList) cdr;
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

    }

    private abstract static class PInputStream {
        protected InputStream is;

        PInputStream(InputStream is) {
            this.is = is;
        }

        abstract int readInt() throws IOException;

        abstract String readString(int len) throws IOException;

        abstract double readDouble() throws IOException;

    }

    @SuppressWarnings("unused")
    private abstract static class AsciiInputFormat extends PInputStream {
        AsciiInputFormat(InputStream is) {
            super(is);
        }
    }

    @SuppressWarnings("unused")
    private abstract static class BinaryInputFormat extends PInputStream {
        BinaryInputFormat(InputStream is) {
            super(is);
        }
    }

    private static final class XdrInputFormat extends PInputStream {

        private final byte[] buf;
        private int size;
        private int offset;

        XdrInputFormat(InputStream is) {
            super(is);
            if (is instanceof PByteArrayInputStream) {
                // we already have the data and we have read the beginning
                PByteArrayInputStream pbis = (PByteArrayInputStream) is;
                buf = pbis.getData();
                size = pbis.getData().length;
                offset = pbis.pos();
            } else {
                buf = new byte[8192];
                size = 0;
                offset = 0;
            }
        }

        @Override
        int readInt() throws IOException {
            ensureData(4);
            return ((buf[offset++] & 0xff) << 24 | (buf[offset++] & 0xff) << 16 | (buf[offset++] & 0xff) << 8 | (buf[offset++] & 0xff));
        }

        @Override
        double readDouble() throws IOException {
            ensureData(8);
            long val = ((long) (buf[offset++] & 0xff) << 56 | (long) (buf[offset++] & 0xff) << 48 | (long) (buf[offset++] & 0xff) << 40 | (long) (buf[offset++] & 0xff) << 32 |
                            (long) (buf[offset++] & 0xff) << 24 | (long) (buf[offset++] & 0xff) << 16 | (long) (buf[offset++] & 0xff) << 8 | buf[offset++] & 0xff);
            return Double.longBitsToDouble(val);
        }

        @Override
        String readString(int len) throws IOException {
            ensureData(len);
            String s = new String(buf, offset, len);
            offset += len;
            return s;
        }

        private void ensureData(int n) throws IOException {
            if (offset + n > size) {
                int readOffset = 0;
                if (offset != size) {
                    // copy end piece to beginning
                    int i = 0;
                    while (offset != size) {
                        buf[i++] = buf[offset++];
                    }
                    readOffset = i;
                }
                offset = 0;
                // read some more data
                int nread = is.read(buf, readOffset, buf.length - readOffset);
                assert nread > 0;
                size = nread + readOffset;
            }
        }

    }

    /**
     * Traces the items read for debugging.
     */
    private static final class TracingInput extends Input {
        private int nesting;

        private TracingInput(RConnection conn, int depth) throws IOException {
            this(conn.getInputStream(), null, depth);
        }

        private TracingInput(InputStream is, CallHook hook, int depth) throws IOException {
            super(is, hook, depth);
        }

        @Override
        protected Object readItem() throws IOException {
            // CheckStyle: stop system..print check
            int flags = stream.readInt();
            SEXPTYPE type = SEXPTYPE.mapInt(Flags.ptype(flags));
            for (int i = 0; i < nesting; i++) {
                System.out.print("  ");
            }
            System.out.printf("%d %s%n", nesting, type);
            nesting++;
            Object result = super.readItem(flags);
            nesting--;
            return result;
        }

    }

    // Serialize support is currently very limited, essentially to saving the CRAN package format
// info,

    private abstract static class POutputStream {
        protected OutputStream os;

        POutputStream(OutputStream os) {
            this.os = os;
        }

        abstract void writeInt(int value) throws IOException;

        abstract void writeString(String value) throws IOException;

        abstract void writeDouble(double value) throws IOException;

        abstract void flush() throws IOException;

    }

    private static class XdrOutputFormat extends POutputStream {
        private byte[] buf;
        private int offset;

        XdrOutputFormat(OutputStream os) {
            super(os);
            buf = new byte[8192];
            buf[offset++] = 'X';
            buf[offset++] = '\n';
        }

        @Override
        void writeInt(int value) throws IOException {
            ensureSpace(4);
            buf[offset++] = (byte) (value >>> 24);
            buf[offset++] = (byte) (value >> 16);
            buf[offset++] = (byte) (value >> 8);
            buf[offset++] = (byte) value;
        }

        @Override
        void writeString(String value) throws IOException {
            byte[] bytes = value.getBytes();
            int bytesLen = bytes.length;
            assert bytesLen < buf.length - 4;
            ensureSpace(bytesLen + 4);
            writeInt(bytesLen);
            System.arraycopy(bytes, 0, buf, offset, bytesLen);
            offset += bytesLen;

        }

        @Override
        void writeDouble(double value) throws IOException {
            ensureSpace(8);
            long valueBits = Double.doubleToLongBits(value);
            buf[offset++] = (byte) (valueBits >>> 56);
            buf[offset++] = (byte) ((valueBits >> 48) & 0xff);
            buf[offset++] = (byte) ((valueBits >> 40) & 0xff);
            buf[offset++] = (byte) ((valueBits >> 32) & 0xff);
            buf[offset++] = (byte) ((valueBits >> 24) & 0xff);
            buf[offset++] = (byte) ((valueBits >> 16) & 0xff);
            buf[offset++] = (byte) ((valueBits >> 8) & 0xff);
            buf[offset++] = (byte) (valueBits & 0xff);
        }

        private void ensureSpace(int n) throws IOException {
            if (offset + n > buf.length) {
                flushBuffer();
            }
        }

        void flushBuffer() throws IOException {
            if (offset > 0) {
                os.write(buf, 0, offset);
                offset = 0;
            }
        }

        @Override
        void flush() throws IOException {
            flushBuffer();
            os.flush();
        }
    }

    private static class Output extends Common {
        protected final POutputStream stream;
        private int version;

        private Output(RConnection conn, char format, int version, CallHook hook, int depth) throws IOException {
            this(conn.getOutputStream(), format, version, hook, depth);
        }

        private Output(OutputStream os, char format, int version, CallHook hook, int depth) throws IOException {
            super(hook, depth);
            this.version = version;
            switch (format) {
                case 'A':
                case 'B':
                    throw formatError((byte) format, true);
                case 'X':
                    stream = new XdrOutputFormat(os);
                    break;
                default:
                    throw formatError((byte) format, false);
            }
        }

        private void serialize(Object obj) throws IOException {
            switch (version) {
                case 2:
                    stream.writeInt(version);
                    stream.writeInt(196865);
                    stream.writeInt(RVersionInfo.SERIALIZE_VERSION);
                    break;

                default:
                    RInternalError.unimplemented();
            }
            writeItem(obj);
            stream.flush();
        }

        private void writeItem(Object obj) throws IOException {
            SEXPTYPE type = SEXPTYPE.typeForClass(obj.getClass());
            int refIndex;
            if ((refIndex = getRefIndex(obj)) != -1) {
                outRefIndex(refIndex);
            } else if (type == SEXPTYPE.SYMSXP) {
                addReadRef(obj);
                stream.writeInt(SEXPTYPE.SYMSXP.code);
                writeItem(((RSymbol) obj).getName());
            } else if (type == SEXPTYPE.ENVSXP) {
                throw RInternalError.unimplemented();
            } else {
                // flags
                RAttributes attributes = null;
                if (obj instanceof RAttributable) {
                    RAttributable rattr = (RAttributable) obj;
                    attributes = rattr.getAttributes();
                    if (attributes != null && attributes.isEmpty()) {
                        attributes = null;
                    }
                }
                int flags = Flags.packFlags(type, 0, false, attributes != null, false);
                stream.writeInt(flags);
                switch (type) {
                    case STRSXP: {
                        RStringVector vec = (RStringVector) obj;
                        stream.writeInt(vec.getLength());
                        for (int i = 0; i < vec.getLength(); i++) {
                            writeItem(vec.getDataAt(i));
                        }
                        break;
                    }

                    case CHARSXP: {
                        String s = (String) obj;
                        if (s == RRuntime.STRING_NA) {
                            stream.writeInt(-1);
                        } else {
                            stream.writeString(s);
                        }
                        break;
                    }

                    case INTSXP: {
                        RIntVector vec = (RIntVector) obj;
                        stream.writeInt(vec.getLength());
                        for (int i = 0; i < vec.getLength(); i++) {
                            // TODO NA
                            stream.writeInt(vec.getDataAt(i));
                        }
                        break;
                    }

                    case VECSXP: {
                        RList list = (RList) obj;
                        stream.writeInt(list.getLength());
                        for (int i = 0; i < list.getLength(); i++) {
                            Object listObj = list.getDataAt(i);
                            writeItem(listObj);
                        }
                        break;
                    }

                    default:
                        throw RInternalError.unimplemented();
                }
                if (attributes != null) {
                    // have to convert to GnuR pairlist
                    Iterator<RAttribute> iter = attributes.iterator();
                    while (iter.hasNext()) {
                        RAttribute attr = iter.next();
                        // name is the tag of the virtual pairlist
                        // value is the car
                        // next is the cdr
                        stream.writeInt(Flags.packFlags(SEXPTYPE.LISTSXP, 0, false, false, true));
                        stream.writeInt(SEXPTYPE.SYMSXP.code);
                        writeItem(attr.getName());
                        writeItem(attr.getValue());
                    }
                    stream.writeInt(Flags.packFlags(SEXPTYPE.NILVALUE_SXP, 0, false, false, false));
                }
            }
        }

        private void outRefIndex(int index) throws IOException {
            if (index > MAX_PACKED_INDEX) {
                stream.writeInt(SEXPTYPE.REFSXP.code);
                stream.writeInt(index);
            } else {
                stream.writeInt(packRefIndex(index));
            }
        }
    }

    @TruffleBoundary
    public static void serialize(RConnection conn, Object obj, boolean ascii, int version, Object refhook, int depth) throws IOException {
        Output output = new Output(conn, ascii ? 'A' : 'X', version, (CallHook) refhook, depth);
        output.serialize(obj);
    }

}
