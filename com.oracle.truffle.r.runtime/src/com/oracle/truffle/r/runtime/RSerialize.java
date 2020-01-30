/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.r.runtime.context.FastROptions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.launcher.RVersionNumber;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RScalar;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.closures.RToStringVectorClosure;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.ALTREP_SXP;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;
import java.util.logging.Level;

// Code loosely transcribed from GnuR serialize.c.

/**
 * Serialize/unserialize.
 *
 * It is sometimes convenient when debugging to trace the serialization process, particularly when
 * unserializing an object created by GnurR. The following options are available. N.B. These should
 * normally be set in the console using the {@code fastr.debug} function as a great deal of
 * unserialization happens on startup.
 * <p>
 * Debugging options:
 * <ul>
 * <li>unserialize: trace the input as it is read</li>
 * <li>printUclosure: print the pairlist resulting from unserializing an object of type CLOSXP.</li>
 * <li>printWclosure: print the pairlist that will be written when serializing a CLOSXP.</li>
 * </ul>
 * N.B. All output goes to the Java standard output. Once {@code printUclosure} is set all lazily
 * loaded functions will print, e.g. calling {@code quit()} will print the pairlist for the
 * {@code quit} function.
 *
 */
// Checkstyle: stop final class check
public class RSerialize {

    public static final class VersionInfo {
        private VersionInfo(int version, int writerVersion, int minReaderVersion, String format, String nativeEncoding) {
            this.version = version;
            this.writerVersion = new Version(writerVersion);
            this.minReaderVersion = new Version(minReaderVersion);
            this.format = format;
            this.nativeEncoding = nativeEncoding;
        }

        private final int version;
        private final Version writerVersion;
        private final Version minReaderVersion;
        private final String format;
        private final String nativeEncoding;

        public RList toVector() {
            RStringVector names;
            if (nativeEncoding != null) {
                names = RDataFactory.createStringVector(new String[]{"version", "writer_version", "min_reader_version", "format", "native_encoding"}, RDataFactory.COMPLETE_VECTOR);
                return RDataFactory.createList(new Object[]{version, writerVersion.toString(), minReaderVersion.toString(), format, nativeEncoding}, names);
            } else {
                names = RDataFactory.createStringVector(new String[]{"version", "writer_version", "min_reader_version", "format"}, RDataFactory.COMPLETE_VECTOR);
                return RDataFactory.createList(new Object[]{version, writerVersion.toString(), minReaderVersion.toString(), format}, names);
            }
        }

        private static class Version {
            private final int v;
            private final int p;
            private final int s;

            public Version(int version) {
                int ver = version;
                this.v = ver / 65536;
                ver = ver % 65536;
                this.p = ver / 256;
                ver = ver % 256;
                this.s = ver;
            }

            @TruffleBoundary
            @Override
            public String toString() {
                return String.format("%d.%d.%d", v, p, s);
            }
        }
    }

    private static class Flags {
        static final int IS_OBJECT_BIT_MASK = 1 << 8;
        static final int HAS_ATTR_BIT_MASK = 1 << 9;
        static final int HAS_TAG_BIT_MASK = 1 << 10;
        static final int TYPE_MASK = 255;
        static final int LEVELS_SHIFT = 12;
        static final int CACHED_MASK = 1 << 5;
        static final int HASHASH_MASK = 1;
        static final int IS_ACTIVE_BINDING_MASK = 1 << 15;

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

        public static boolean isActiveBinding(int levs) {
            return (levs & IS_ACTIVE_BINDING_MASK) != 0;
        }

        public static int packFlags(SEXPTYPE type, int gpbits, boolean isObj, boolean hasAttr, boolean hasTag) {
            int val = type.code;
            int levs = gpbits & (~(CACHED_MASK | HASHASH_MASK));
            val = type.code | (levs << 12);

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

        Object getSessionRef();
    }

    public static final class ContextStateImpl implements RContext.ContextState {

        /**
         * {@code ...getNamespace} in "namespace.R", used to callback to handle a
         * {@link SEXPTYPE#NAMESPACESXP} item.
         */
        private RFunction dotDotFindNamespace;

        /**
         * Initialize and return the value of {@link #dotDotFindNamespace}. This is lazy because
         * when this instance is created, the {@link REnvironment} context state has not been set
         * up, so we can't look up anything in the base env.
         */
        RFunction getDotDotFindNamespace() {
            if (dotDotFindNamespace == null) {
                CompilerDirectives.transferToInterpreter();
                String name = "..getNamespace";
                Object f = REnvironment.baseEnv().findFunction(name);
                dotDotFindNamespace = (RFunction) RContext.getRRuntimeASTAccess().forcePromise(name, f);
            }
            return dotDotFindNamespace;
        }

        public static ContextStateImpl newContextState() {
            return new ContextStateImpl();
        }
    }

    private static final int MAX_PACKED_INDEX = Integer.MAX_VALUE >> 8;

    private static int packRefIndex(int i) {
        return (i << 8) | SEXPTYPE.REFSXP.code;
    }

    private static int unpackRefIndex(int i) {
        return i >> 8;
    }

    public abstract static class RefCounter {
        protected Object[] refTable = new Object[128];
        protected int refTableIndex;

        protected Object addReadRef(Object item) {
            assert item != null;
            if (refTableIndex >= refTable.length) {
                refTable = Arrays.copyOf(refTable, refTable.length * 2);
            }
            refTable[refTableIndex++] = item;
            return item;
        }

        protected Object getReadRef(int index) {
            assert index > 0 && index <= refTableIndex;
            return refTable[index - 1];
        }

        protected int getRefIndex(Object obj) {
            for (int i = 0; i < refTableIndex; i++) {
                if (refTable[i] == obj) {
                    return i + 1;
                }
            }
            return -1;
        }
    }

    private abstract static class Common extends RefCounter {

        protected final CallHook hook;
        protected final ContextStateImpl contextState;

        protected Common(CallHook hook) {
            this.hook = hook;
            this.contextState = getContextState();
        }

        protected static IOException formatError(byte format, boolean ok) throws IOException {
            throw new IOException("serialized stream format " + (ok ? "not implemented" : "not recognized") + ": " + format);
        }
    }

    public static final int DEFAULT_VERSION;
    static {
        String defVersion = System.getenv("R_DEFAULT_SERIALIZE_VERSION");
        if ("2".equals(defVersion) || "3".equals(defVersion)) {
            DEFAULT_VERSION = Integer.parseInt(defVersion);
        } else {
            DEFAULT_VERSION = 3;
        }
    }

    /**
     * Lazily read in case set during execution for debugging purposes. This is necessary because
     * setting the option on startup will trace all the standard library functions as they are
     * lazily loaded.
     */
    private static boolean trace() {
        return RContext.getInstance().matchesOption(FastROptions.Debug, "unserialize");
    }

    private static ContextStateImpl getContextState() {
        return RContext.getInstance().stateRSerialize;
    }

    /**
     * Supports the saving of deparsed lazily loaded package functions for instrumentation access.
     */
    public static void setSaveDeparse(@SuppressWarnings("unused") boolean status) {
        @SuppressWarnings("unused")
        ContextStateImpl serializeContextState = getContextState();
        // TODO: reenable this functionality
    }

    @TruffleBoundary
    public static VersionInfo unserializeInfo(RConnection conn) throws IOException {
        Input instance = trace() ? new TracingInput(conn) : new Input(conn);
        return instance.unserializeInfo();
    }

    @TruffleBoundary
    public static Object unserialize(RConnection conn) throws IOException {
        Input instance = trace() ? new TracingInput(conn) : new Input(conn);
        Object result = instance.unserialize();
        return result;
    }

    @TruffleBoundary
    public static Object unserialize(RAbstractRawVector data) {
        byte[] buffer = data.materialize().getReadonlyData();
        try {
            return new Input(new ByteArrayInputStream(buffer)).unserialize();
        } catch (IOException e) {
            throw RInternalError.shouldNotReachHere("ByteArrayInputStream should not throw IOExceptiopn");
        }
    }

    /**
     * This variant exists for the {@code lazyLoadDBFetch} function. In certain cases, when
     * {@link Input#persistentRestore} is called, an R function needs to be evaluated with an
     * argument read from the serialized stream. This is handled with a callback object.
     *
     * @param packageName the name of the package that the lozyLoad is from
     */
    @TruffleBoundary
    public static Object unserialize(byte[] data, CallHook hook, String packageName, String functionName) throws IOException {
        InputStream is = new PByteArrayInputStream(data);
        Input instance = trace() ? new TracingInput(is, hook, packageName, functionName) : new Input(is, hook, packageName, functionName);
        Object result = instance.unserialize();
        return result;
    }

    private static class Input extends Common {

        protected final PInputStream stream;
        /**
         * Only set when called from lazyLoadDBFetch. Helps to identify the package of the deparsed
         * closure.
         */
        protected final String packageName;

        /**
         * Only set when called from lazyLoadDBFetch. Helps giving a proper name to deserialized
         * functions.
         */
        protected String functionName;

        /**
         * We need to know whether we are unserializing a {@link SEXPTYPE#CLOSXP},
         * {@link SEXPTYPE#LANGSXP} or {@link SEXPTYPE#PROMSXP} as we do not want convert embedded
         * instances of {@link SEXPTYPE#LANGSXP} into ASTs.
         */
        private int langDepth;

        private Input(RConnection conn) throws IOException {
            this(conn.getInputStream(), null, null, null);
        }

        private Input(InputStream input) throws IOException {
            this(input, null, null, null);
        }

        private Input(InputStream is, CallHook hook, String packageName, String functionName) throws IOException {
            super(hook);
            this.packageName = packageName;
            this.functionName = functionName;
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

        private VersionInfo unserializeInfo() throws IOException {
            int version = stream.readInt();
            int writerVersion = stream.readInt();
            int minReaderVersion = stream.readInt();
            String ne = null;
            if (version == 3) {
                int nelen = stream.readInt();
                ne = stream.readString(nelen);
            }

            // 'xdr' format hardcoded; so far it is the only supported
            // and a formatError would already have been thrown from the c-tor
            return new VersionInfo(version, writerVersion, minReaderVersion, "xdr", ne);
        }

        private Object unserialize() throws IOException {
            int version = stream.readInt();
            @SuppressWarnings("unused")
            int writerVersion = stream.readInt();
            @SuppressWarnings("unused")
            int minReaderVersion = stream.readInt();

            if (version != 3 && version != 2) {
                throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unsupported serialization version " + version);
            }
            if (version == 3) {
                // skip native encoding info
                int nelen = stream.readInt();
                stream.readString(nelen);
            }

            Object result = readItem();
            return result;
        }

        protected Object readItem() throws IOException {
            int flags = stream.readInt();
            Object result = readItem(flags);
            assert result != null;
            return result;
        }

        private void incDepth(SEXPTYPE type) {
            switch (type) {
                case CLOSXP:
                case LANGSXP:
                case PROMSXP:
                    langDepth++;
                    break;
                default:
                    break;
            }
        }

        protected RAbstractStringVector readStringVec() throws IOException {
            if (stream.readInt() != 0) {
                throw RError.error(RError.NO_CALLER, Message.GENERIC, "names in persistent strings are not supported yet");
            }
            int len = stream.readInt();
            String[] data = new String[len];
            for (int i = 0; i < len; i++) {
                data[i] = (String) readItem();
            }
            return RDataFactory.createStringVector(data, RDataFactory.INCOMPLETE_VECTOR);
        }

        protected Object readItem(int flags) throws IOException {
            int levs = flags >>> 12;
            Object result = null;
            SEXPTYPE type = SEXPTYPE.mapInt(Flags.ptype(flags));

            String currentFunctionName = functionName;
            functionName = null;

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
                    int index = inRefIndex(flags);
                    Object r = getReadRef(index);
                    return checkResult(r);
                }

                case NAMESPACESXP: {
                    RStringVector s = inStringVec(false);
                    /*
                     * TODO we do not record "lastname", which is passed as second argument, but
                     * only used in a warning message in the unlikely event that the namespace
                     * cannot be found.
                     */
                    // fast path through getRegisteredNamespace
                    Object namespace = REnvironment.getRegisteredNamespace(s.getDataAt(0));
                    if (namespace == null) {
                        namespace = RContext.getEngine().evalFunction(contextState.getDotDotFindNamespace(), null, null, true, null, s, "");
                    }
                    return checkResult(addReadRef(namespace));
                }

                case PERSISTSXP: {
                    RStringVector sv = inStringVec(false);
                    result = persistentRestore(sv);
                    return checkResult(addReadRef(result));
                }

                case ALTREP_SXP: {
                    RPairList info = (RPairList) readItem();
                    Object state = readItem();
                    Object attr = readItem();

                    RSymbol sym = (RSymbol) info.getDataAtAsObject(0);
                    String altrepClass = sym.getName();
                    if (altrepClass.equals("compact_intseq")) {
                        result = readCompactIntSeq(state);
                    } else if (altrepClass.equals("compact_realseq")) {
                        result = readCompactRealSeq(state);
                    } else if (altrepClass.equals("deferred_string")) {
                        RPairList l = (RPairList) state;
                        RAbstractVector vec = (RAbstractVector) l.car();
                        result = vec.castSafe(RType.Character, ConditionProfile.getUncached());
                    } else if (altrepClass.equals("wrap_real") || altrepClass.equals("wrap_integer") || altrepClass.equals("wrap_string")) {
                        RPairList l = (RPairList) state;
                        result = l.car();
                    } else {
                        throw RInternalError.unimplemented(sym.getName());
                    }

                    if (attr != RNull.instance) {
                        result = setAttributes(result, attr);
                    }
                    ((RBaseObject) result).setGPBits(levs);
                    return checkResult(result);
                }

                case ENVSXP: {
                    int locked = stream.readInt();
                    /* MUST register before filling in (see serialize.c) */
                    final REnvironment.NewEnv env = RDataFactory.createNewEnv(null);
                    addReadRef(env);

                    Object enclos = readItem();
                    REnvironment enclosing = enclos == RNull.instance ? REnvironment.baseEnv() : (REnvironment) enclos;
                    RArguments.initializeEnclosingFrame(env.getFrame(), enclosing.getFrame());
                    Object frame = readItem();
                    boolean hashed = frame == RNull.instance;
                    Object hashtab = readItem();
                    if (hashed) {
                        if (hashtab != RNull.instance) {
                            env.setHashed(true);
                            env.setInitialSize(((RList) hashtab).getLength());
                            RList hashList = (RList) hashtab;
                            // GnuR sizes its hash tables, empty slots indicated by RNull
                            for (int i = 0; i < hashList.getLength(); i++) {
                                Object val = hashList.getDataAt(i);
                                if (val == RNull.instance) {
                                    continue;
                                }
                                safePutToEnv(env, (RPairList) val);
                            }
                        }
                    } else {
                        while (frame != RNull.instance) {
                            RPairList pl = (RPairList) frame;
                            safePutToEnv(env, pl);
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
                    return checkResult(env);
                }

                case PACKAGESXP: {
                    RAbstractStringVector sVec = readStringVec();
                    assert sVec.getLength() == 1 : "unsupported yet";
                    String s = sVec.getDataAt(0);
                    /*
                     * TODO GnuR eval's findPackageEnv, but we don't want to eval here. That will
                     * call require, so we can only find packages that are already loaded.
                     */
                    REnvironment pkgEnv = REnvironment.lookupOnSearchPath(s);
                    if (pkgEnv == null) {
                        pkgEnv = REnvironment.globalEnv();
                    }

                    return checkResult(addReadRef(pkgEnv));
                }

                case CLOSXP:
                case LANGSXP:
                case LISTSXP:
                case PROMSXP:
                case DOTSXP: {
                    incDepth(type);
                    Object attrItem = RNull.instance;
                    Object tagItem = RNull.instance;
                    if (Flags.hasAttr(flags)) {
                        // create new language parsing context
                        int safedLangDepth = langDepth;
                        langDepth = 0;
                        attrItem = readItem();

                        // restore language parsing context
                        langDepth = safedLangDepth;

                    }
                    if (Flags.hasTag(flags)) {
                        tagItem = readItem();
                    }
                    Object carItem = readItem();
                    Object cdrItem = readItem();

                    // Unlike GnuR the different types require some special treatment
                    switch (type) {
                        case CLOSXP: {
                            langDepth--;
                            /*
                             * Must convert the RPairList to a FastR AST. We could convert to an AST
                             * directly, but it is easier and more robust to deparse and reparse.
                             * N.B. We always convert closures regardless of whether they are at top
                             * level or not (and they are not always at the top in the default
                             * packages)
                             */
                            if (RContext.getInstance().matchesOption(FastROptions.Debug, "printUclosure")) {
                                RPairList pairList = RDataFactory.createPairList(carItem, cdrItem, tagItem, type);
                                if (attrItem != RNull.instance) {
                                    setAttributes(pairList, attrItem);
                                }
                                Debug.printClosure(pairList);
                            }

                            // older versions of GnuR allowed 'NULL'
                            assert tagItem == RNull.instance || tagItem instanceof REnvironment;
                            REnvironment enclosingEnv = tagItem == RNull.instance ? REnvironment.baseEnv() : (REnvironment) tagItem;
                            boolean restore = setupLibPath(enclosingEnv);
                            RFunction func = PairlistDeserializer.processFunction(carItem, cdrItem, enclosingEnv, currentFunctionName, packageName);
                            if (attrItem != RNull.instance) {
                                setAttributes(func, attrItem);
                                handleFunctionSrcrefAttr(func);
                            }
                            if (restore) {
                                RContext.getInstance().libraryPaths.remove(0);
                            }

                            result = func;
                            break;
                        }

                        case LANGSXP: {
                            langDepth--;
                            /*
                             * N.B. LANGSXP values occur within CLOSXP structures, so we only want
                             * to convert them to an AST when they occur outside of a CLOSXP, as in
                             * the CLOSXP case, the entire structure is deparsed at the end. Ditto
                             * for LANGSXP when specifying a formula
                             */
                            if (langDepth == 0) {
                                RPairList lang = PairlistDeserializer.processLanguage(carItem, cdrItem, tagItem);
                                if (attrItem != RNull.instance) {
                                    setAttributes(lang, attrItem);
                                }
                                result = lang;
                            } else {
                                RPairList pairList = RDataFactory.createPairList(carItem, cdrItem, tagItem, type);
                                result = pairList;
                                if (attrItem != RNull.instance) {
                                    setAttributes(pairList, attrItem);
                                }
                            }
                            break;
                        }

                        case PROMSXP: {
                            langDepth--;
                            /*
                             * tag: environment for eval (or RNull if evaluated), car: value:
                             * RUnboundValue if not evaluated, cdr: expression
                             */
                            result = PairlistDeserializer.processPromise(carItem, cdrItem, tagItem);
                            break;
                        }

                        case DOTSXP: {
                            RPairList pairList = RDataFactory.createPairList(carItem, cdrItem, tagItem, type);
                            int len = pairList.getLength();
                            Object[] values = new Object[len];
                            String[] names = new String[len];
                            for (int i = 0; i < len; i++) {
                                values[i] = pairList.car();
                                if (pairList.getTag() != RNull.instance) {
                                    names[i] = ((RSymbol) pairList.getTag()).getName();
                                }
                                if (i < len - 1) {
                                    pairList = (RPairList) pairList.cdr();
                                }
                            }
                            return new RArgsValuesAndNames(values, ArgumentsSignature.get(names));
                        }

                        case LISTSXP:
                            if (Flags.isActiveBinding(levs)) {
                                assert carItem instanceof RFunction;
                                carItem = new ActiveBinding(RType.Any, (RFunction) carItem);
                            }
                            RPairList pairList = RDataFactory.createPairList(carItem, cdrItem, tagItem, type);
                            result = pairList;
                            if (attrItem != RNull.instance) {
                                /*
                                 * TODO Currently we are losing attributes on CLOSXP (and LANGSXP)
                                 * objects because this code places the attributes on the pairList
                                 * and not on the RFunction object we eventually convert the
                                 * pairlist into.
                                 */
                                setAttributes(pairList, attrItem);
                            }
                            break;
                    }

                    if (result instanceof RScalar) {
                        // for now we only record S4-ness here, and in this case it should be 0
                        assert (levs == 0);
                    } else {
                        assert result != null;
                        ((RBaseObject) result).setGPBits(levs);
                    }
                    return checkResult(result);
                }

                /*
                 * These break out of the top level switch to have their ATTR, LEVELS, and OBJECT
                 * fields filled in.
                 */

                case EXPRSXP:
                case VECSXP: {
                    int len = stream.readInt();
                    // TODO long vector support?
                    assert len >= 0;
                    Object[] data = new Object[len];
                    for (int i = 0; i < len; i++) {
                        Object elem = readItem();
                        data[i] = elem;
                    }
                    if (type == SEXPTYPE.EXPRSXP) {
                        result = RDataFactory.createExpression(data);
                    } else {
                        // this could (ultimately) be a list, factor or dataframe
                        result = RDataFactory.createList(data);
                    }
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
                        if (RRuntime.isNA(reVal) && RRuntime.isNA(imVal)) {
                            data[ix] = RRuntime.COMPLEX_NA_REAL_PART;
                            data[ix + 1] = RRuntime.COMPLEX_NA_IMAGINARY_PART;
                        } else {
                            data[ix] = reVal;
                            data[ix + 1] = imVal;
                        }
                    }
                    result = RDataFactory.createComplexVector(data, complete);
                    break;
                }

                case SPECIALSXP:
                case BUILTINSXP: {
                    int len = stream.readInt();
                    String s = stream.readString(len);
                    result = RContext.getInstance().lookupBuiltin(s);
                    RInternalError.guarantee(result != null, "lookup failed in unserialize for builtin: " + s);
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
                    result = RDataFactory.createSymbolInterned(name);
                    addReadRef(result);
                    break;
                }

                case BCODESXP: {
                    result = readBC();
                    break;
                }

                case S4SXP: {
                    result = RDataFactory.createS4Object();
                    break;
                }

                case EXTPTRSXP: {
                    Object prot = readItem();
                    long addr = 0;
                    Object tag = readItem();
                    result = RDataFactory.createExternalPtr(new DLL.SymbolHandle(addr), tag, prot);
                    addReadRef(result);
                    break;
                }

                case RAWSXP: {
                    int len = stream.readInt();
                    byte[] data = new byte[len];
                    stream.readRaw(data);
                    result = RDataFactory.createRawVector(data);
                    break;
                }

                case UNBOUNDVALUE_SXP: {
                    result = RUnboundValue.instance;
                    break;
                }

                default:
                    throw RInternalError.unimplemented(" " + type);
            }
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
                ((RBaseObject) result).setGPBits(levs);
            }

            return checkResult(result);
        }

        private static Object readCompactIntSeq(Object state) throws RuntimeException {
            RAbstractVector result;
            if (state instanceof RAbstractIntVector) {
                RAbstractIntVector vec = (RAbstractIntVector) state;
                int length = vec.getDataAt(0);
                int first = vec.getDataAt(1);
                int stride = vec.getDataAt(2);
                result = RDataFactory.createIntSequence(first, stride, length);
            } else if (state instanceof RAbstractDoubleVector) {
                RAbstractDoubleVector vec = (RAbstractDoubleVector) state;
                int length = (int) vec.getDataAt(0);
                int first = (int) vec.getDataAt(1);
                int stride = (int) vec.getDataAt(2);
                result = RDataFactory.createIntSequence(first, stride, length);
            } else {
                throw RInternalError.unimplemented(state.getClass().getSimpleName());
            }
            return result;
        }

        private static Object readCompactRealSeq(Object state) throws RuntimeException {
            RAbstractVector result;
            if (state instanceof RAbstractDoubleVector) {
                RAbstractDoubleVector vec = (RAbstractDoubleVector) state;
                double length = (int) vec.getDataAt(0);
                double first = vec.getDataAt(1);
                double stride = vec.getDataAt(2);
                if (length > Integer.MAX_VALUE) {
                    throw RError.error(RError.NO_CALLER, RError.Message.TOO_LONG_VECTOR);
                }
                result = RDataFactory.createDoubleSequence(first, stride, (int) length);
            } else {
                throw RInternalError.unimplemented(state.getClass().getSimpleName());
            }
            return result;
        }

        private static void safePutToEnv(REnvironment env, RPairList pl) {
            String name = ((RSymbol) pl.getTag()).getName();
            Object car = pl.car();
            if (ActiveBinding.isActiveBinding(car)) {
                FrameSlot frameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(env.getFrame().getFrameDescriptor(), name, FrameSlotKind.Object);
                FrameSlotChangeMonitor.setActiveBinding(env.getFrame(), frameSlot, (ActiveBinding) car, false, null);
            } else {
                env.safePut(name, car);
            }
        }

        private static Object checkResult(Object result) {
            assert result != null;
            return result;
        }

        /**
         * GnuR uses a pairlist to represent attributes, whereas FastR uses the abstract RAttributes
         * class.
         */
        @TruffleBoundary
        private static Object setAttributes(final Object object, Object attr) {
            RAttributable rAttributable = (RAttributable) object;
            RPairList pl = (RPairList) attr;
            Object result = object;
            while (true) {
                RSymbol tagSym = (RSymbol) pl.getTag();
                String tag = Utils.intern(tagSym.getName());
                // this may convert a plain vector to a data.frame or factor
                Object attrValue = pl.car();
                if (RSharingAttributeStorage.isShareable(attrValue) && ((RSharingAttributeStorage) attrValue).isTemporary()) {
                    ((RSharingAttributeStorage) attrValue).incRefCount();
                }
                if (result instanceof RMaterializedVector && tag.equals(RRuntime.CLASS_ATTR_KEY)) {
                    RStringVector classes = (RStringVector) attrValue;
                    result = ((RAbstractVector) result).setClassAttr(classes);
                } else {
                    rAttributable.setAttr(tag, attrValue);
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
                    throw RError.nyi(null, "names in persistent strings");
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
            return RDataFactory.createPairList(car, cdr, RNull.instance, SEXPTYPE.BCODESXP);
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
                        // TODO: why "assert false;"???
                    }
                    Object tag = readItem();
                    Object car = readBCLang(SEXPTYPE.mapInt(stream.readInt()), reps);
                    Object cdr = readBCLang(SEXPTYPE.mapInt(stream.readInt()), reps);
                    Object ans = RDataFactory.createPairList(car, cdr, tag, type);
                    if (pos >= 0) {
                        reps[pos] = ans;
                    }
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

        abstract void readRaw(byte[] data) throws IOException;

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

        private static final int READ_BUFFER_SIZE = 32 * 1024;

        private final class Buffer {
            private final byte[] buf;
            private int size;
            private int offset;

            Buffer(byte[] buf) {
                this.buf = buf;
            }

            int readInt() {
                return ((buf[offset++] & 0xff) << 24 | (buf[offset++] & 0xff) << 16 | (buf[offset++] & 0xff) << 8 | (buf[offset++] & 0xff));
            }

            double readDouble() {
                long val = ((long) (buf[offset++] & 0xff) << 56 | (long) (buf[offset++] & 0xff) << 48 | (long) (buf[offset++] & 0xff) << 40 | (long) (buf[offset++] & 0xff) << 32 |
                                (long) (buf[offset++] & 0xff) << 24 | (long) (buf[offset++] & 0xff) << 16 | (long) (buf[offset++] & 0xff) << 8 | buf[offset++] & 0xff);
                return Double.longBitsToDouble(val);
            }

            @SuppressWarnings("deprecation")
            String readString(int len) {
                /*
                 * This fast path uses a cheaper String constructor if all incoming bytes are in the
                 * 0-127 range.
                 */
                boolean fastEncode = true;
                for (int i = 0; i < len; i++) {
                    byte b = buf[offset + i];
                    if (b < 0) {
                        fastEncode = false;
                        break;
                    }
                }
                String result;
                if (fastEncode) {
                    result = new String(buf, 0, offset, len);
                } else {
                    result = new String(buf, offset, len, StandardCharsets.UTF_8);
                }
                offset += len;
                WeakReference<String> entry;
                if ((entry = strings.get(result)) != null) {
                    String string = entry.get();
                    if (string != null) {
                        return string;
                    }
                }
                strings.put(result, new WeakReference<>(result));
                return result;
            }

            void readRaw(byte[] data) {
                System.arraycopy(buf, offset, data, 0, data.length);
                offset += data.length;
            }

            void readData(int n) throws IOException {
                if (offset + n > size) {
                    if (offset != size) {
                        // copy end piece to beginning
                        System.arraycopy(buf, offset, buf, 0, size - offset);
                    }
                    size -= offset;
                    offset = 0;
                    while (size < n) {
                        // read some more data
                        int nread = is.read(buf, size, buf.length - size);
                        if (nread <= 0) {
                            throw RInternalError.unimplemented("handle unexpected eof");
                        }
                        size += nread;
                    }
                }
            }
        }

        /**
         * This buffer is used under normal circumstances, i.e. when the read data blocks are
         * smaller than the initial buffer. The ensureData method creates a special buffer for
         * reading big chunks of data exceeding the default buffer.
         */
        private final Buffer defaultBuffer;

        private final WeakHashMap<String, WeakReference<String>> strings = RContext.getInstance().stringMap;

        XdrInputFormat(InputStream is) {
            super(is);
            if (is instanceof PByteArrayInputStream) {
                // we already have the data and we have read the beginning
                PByteArrayInputStream pbis = (PByteArrayInputStream) is;
                defaultBuffer = new Buffer(pbis.getData());
                defaultBuffer.size = pbis.getData().length;
                defaultBuffer.offset = pbis.pos();
            } else {
                defaultBuffer = new Buffer(new byte[READ_BUFFER_SIZE]);
                defaultBuffer.size = 0;
                defaultBuffer.offset = 0;
            }
        }

        @Override
        int readInt() throws IOException {
            return ensureData(4).readInt();
        }

        @Override
        double readDouble() throws IOException {
            return ensureData(8).readDouble();
        }

        @Override
        String readString(int len) throws IOException {
            return ensureData(len).readString(len);
        }

        @Override
        void readRaw(byte[] data) throws IOException {
            ensureData(data.length).readRaw(data);
        }

        private Buffer ensureData(int n) throws IOException {
            Buffer usedBuffer;
            if (n > defaultBuffer.buf.length) {
                if (is instanceof PByteArrayInputStream) {
                    // If the input stream is instance of PByteArrayInputStream, the buffer is
                    // preloaded and thus no more data can be read beyond the current buffer.
                    throw new IOException("Premature EOF");
                }

                // create an enlarged copy of the default buffer
                byte[] enlargedBuf = new byte[n];
                System.arraycopy(defaultBuffer.buf, defaultBuffer.offset, enlargedBuf, defaultBuffer.offset, defaultBuffer.size - defaultBuffer.offset);
                usedBuffer = new Buffer(enlargedBuf);
                usedBuffer.offset = defaultBuffer.offset;
                usedBuffer.size = defaultBuffer.size;

                // reset the default buffer
                defaultBuffer.offset = defaultBuffer.size = 0;

                usedBuffer.readData(n);
                // The previous statement should entirely fill the temporary buffer.
                // It is assumed that the caller will read n bytes, making the temporary buffer
                // disposable. Next time, the default buffer will be used again, unless
                // n > defaultBuffer.buf.length.
                assert usedBuffer.size == n;
            } else {
                usedBuffer = defaultBuffer;
                usedBuffer.readData(n);
            }
            return usedBuffer;
        }
    }

    /**
     * Traces the items read for debugging.
     */
    private static final class TracingInput extends Input {
        private int nesting;

        private static final TruffleLogger LOGGER = RLogger.getLogger(RSerialize.class.getName());

        private TracingInput(RConnection conn) throws IOException {
            this(conn.getInputStream(), null, null, null);
        }

        private TracingInput(InputStream is, CallHook hook, String packageName, String functionName) throws IOException {
            super(is, hook, packageName, functionName);
        }

        @Override
        protected Object readItem() throws IOException {
            int flags = stream.readInt();
            SEXPTYPE type = SEXPTYPE.mapInt(Flags.ptype(flags));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nesting; i++) {
                sb.append("  ");
            }
            sb.append(nesting);
            sb.append(" ");
            sb.append(type);
            nesting++;
            Object result = super.readItem(flags);
            if (type == SEXPTYPE.CHARSXP) {
                sb.append(" \"");
                sb.append(result);
                sb.append("\"");
            }
            if (type == SEXPTYPE.SYMSXP) {
                sb.append(result);
            }
            if (type == SEXPTYPE.ALTREP_SXP) {
                sb.append(result);
            }
            nesting--;
            LOGGER.log(Level.INFO, sb.toString());
            return result;
        }
    }

    private abstract static class POutputStream {

        protected OutputStream os;

        POutputStream(OutputStream os) {
            this.os = os;
        }

        abstract void writeInt(int value) throws IOException;

        abstract void writeString(String value) throws IOException;

        abstract void writeDouble(double value) throws IOException;

        abstract void writeRaw(byte value) throws IOException;

        abstract void flush() throws IOException;

    }

    private static class XdrOutputFormat extends POutputStream {
        private final byte[] buf;
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
            boolean simple = true;
            for (int i = 0; i < value.length(); i++) {
                if (value.charAt(i) >= 0x80) {
                    simple = false;
                    break;
                }
            }
            if (simple && value.length() <= buf.length) {
                writeInt(value.length());
                ensureSpace(value.length());
                for (int i = 0; i < value.length(); i++) {
                    buf[offset++] = (byte) value.charAt(i);
                }
            } else {
                byte[] bytes = value.getBytes();
                int bytesLen = bytes.length;
                int totalLen = bytesLen + 4;
                if (totalLen > buf.length) {
                    // too large to fit buffer
                    ensureSpace(4);
                    writeInt(bytesLen);
                    flushBuffer();
                    os.write(bytes);
                } else {
                    ensureSpace(totalLen);
                    writeInt(bytesLen);
                    System.arraycopy(bytes, 0, buf, offset, bytesLen);
                    offset += bytesLen;
                }
            }
        }

        @Override
        void writeRaw(byte value) throws IOException {
            ensureSpace(1);
            buf[offset++] = value;
        }

        @Override
        void writeDouble(double value) throws IOException {
            ensureSpace(8);
            long valueBits = Double.doubleToRawLongBits(value);
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

    public static final int XDR = 0; // actually any value other than the following
    public static final int ASCII = 1;
    public static final int ASCII_HEX = 2;
    public static final int BINARY = 3;

    private static final class Output extends Common {

        private final State state;
        private final POutputStream stream;
        private final int version;
        private final RContext context;

        private Output(RContext context, int format, int version, CallHook hook, OutputStream os) throws IOException {
            super(hook);
            this.context = context;
            this.state = new PLState(context, hook != null ? hook.getSessionRef() : null);
            this.version = version;
            switch (format) {
                case ASCII:
                case ASCII_HEX:
                case BINARY:
                    throw formatError((byte) format, true);
                default:
                    stream = new XdrOutputFormat(os);
                    break;
            }
        }

        private void serialize(Object obj) throws IOException {
            stream.writeInt(version);
            stream.writeInt(RVersionNumber.R_VERSION);
            switch (version) {
                case 2:
                    stream.writeInt(RVersionInfo.SERIALIZE_VERSION_2);
                    break;
                case 3:
                    stream.writeInt(RVersionInfo.SERIALIZE_VERSION_3);
                    // native encoding
                    stream.writeString("UTF-8");
                    break;

                default:
                    throw RInternalError.unimplemented();
            }
            writeItem(obj);
            stream.flush();
        }

        private static SEXPTYPE saveSpecialHook(Object item) {
            if (item == RNull.instance) {
                return SEXPTYPE.NILVALUE_SXP;
            }
            if (item == REnvironment.emptyEnv()) {
                return SEXPTYPE.EMPTYENV_SXP;
            }
            if (item == REnvironment.baseEnv()) {
                return SEXPTYPE.BASEENV_SXP;
            }
            if (item == REnvironment.globalEnv()) {
                return SEXPTYPE.GLOBALENV_SXP;
            }
            if (item == RUnboundValue.instance) {
                return SEXPTYPE.UNBOUNDVALUE_SXP;
            }
            if (item == RMissing.instance) {
                return SEXPTYPE.MISSINGARG_SXP;
            }
            if (item == REmpty.instance) {
                return SEXPTYPE.MISSINGARG_SXP;
            }
            if (item == REnvironment.baseNamespaceEnv()) {
                return SEXPTYPE.BASENAMESPACE_SXP;
            }
            if (item instanceof RArgsValuesAndNames && ((RArgsValuesAndNames) item).getLength() == 0) {
                // empty DOTSXP
                return SEXPTYPE.NILVALUE_SXP;
            }
            return null;
        }

        private static int getGPBits(Object obj) {
            // TODO: this feels a bit ad hoc
            if (obj instanceof RBaseObject && !(obj instanceof RExternalPtr || obj instanceof RScalar)) {
                return ((RBaseObject) obj).getGPBits();
            } else {
                return 0;
            }
        }

        @TruffleBoundary
        private static boolean isObject(Object obj) {
            if (obj instanceof RAttributable) {
                return ((RAttributable) obj).isObject();
            } else {
                return false;
            }
        }

        private void writeItem(Object objArg) throws IOException {
            Object obj = objArg;
            boolean tailCall;
            do {
                // convert primitive types into RAbstractVectors
                obj = RRuntime.asAbstractVector(obj);

                tailCall = false;
                SEXPTYPE specialType;
                Object psn;
                if ((psn = getPersistentName(obj)) != RNull.instance) {
                    addReadRef(obj);
                    stream.writeInt(SEXPTYPE.PERSISTSXP.code);
                    outStringVec((RStringVector) psn, false);
                    return;
                }
                if ((specialType = saveSpecialHook(obj)) != null) {
                    stream.writeInt(specialType.code);
                    return;
                }
                SEXPTYPE type = SEXPTYPE.typeForClass(obj);
                SEXPTYPE gnuRType = SEXPTYPE.gnuRType(type, obj);

                if (isALTREP(obj) && version >= 3) {
                    RPairList info = null;
                    Object data = null;
                    String cls = null;
                    if (obj instanceof RIntSequence) {
                        info = RDataFactory.createPairList(RDataFactory.createIntVectorFromScalar(SEXPTYPE.INTSXP.code));
                        RIntSequence vec = (RIntSequence) obj;
                        data = RDataFactory.createDoubleVector(new double[]{vec.getLength(), vec.getStart(), vec.getStride()}, RDataFactory.COMPLETE_VECTOR);
                        cls = "compact_intseq";
                    } else if (obj instanceof RToStringVectorClosure) {
                        info = RDataFactory.createPairList(RDataFactory.createIntVectorFromScalar(SEXPTYPE.STRSXP.code));
                        data = RDataFactory.createPairList(((RToStringVectorClosure) obj).getDelegate(), RDataFactory.createIntVectorFromScalar(0));
                        cls = "deferred_string";
                    }
                    assert info != null && data != null & cls != null;

                    info = RDataFactory.createPairList(RDataFactory.createSymbol("base"), info);
                    info = RDataFactory.createPairList(RDataFactory.createSymbol(cls), info);

                    OutAttributes attributes = new OutAttributes(obj, ALTREP_SXP, ALTREP_SXP);
                    int flags = Flags.packFlags(ALTREP_SXP, getGPBits(obj), isObject(obj), attributes.hasAttributes(), false);

                    stream.writeInt(flags);
                    writeItem(info);
                    writeItem(data);
                    if (attributes.hasAttributes()) {
                        writeAttributes(attributes);
                    } else {
                        writeItem(RNull.instance);
                    }
                    return;
                } /* else fall through to standard processing */

                int refIndex;
                if ((refIndex = getRefIndex(obj)) != -1) {
                    outRefIndex(refIndex);
                } else if (type == SEXPTYPE.SYMSXP) {
                    writeSymbol((RSymbol) obj);
                } else {
                    if (type == SEXPTYPE.ENVSXP) {
                        REnvironment env = (REnvironment) obj;
                        addReadRef(obj);
                        String name = null;
                        if ((name = env.isPackageEnv()) != null) {
                            RError.warning(RError.SHOW_CALLER2, RError.Message.PACKAGE_AVAILABLE, name);
                            stream.writeInt(SEXPTYPE.PACKAGESXP.code);
                            stream.writeString(name);
                        } else if (env.isNamespaceEnv()) {
                            stream.writeInt(SEXPTYPE.NAMESPACESXP.code);
                            RStringVector nameSpaceEnvSpec = env.getNamespaceSpec();
                            outStringVec(nameSpaceEnvSpec, false);
                        } else {
                            stream.writeInt(SEXPTYPE.ENVSXP.code);
                            stream.writeInt(env.isLocked() ? 1 : 0);
                            writeItem(env.getParent());
                            /*
                             * TODO To be truly compatible with GnuR we should remember whether an
                             * environment was created with new.env(hash=T) and output it in that
                             * form with the associated size. For internal FastR use it does not
                             * matter, so we use the "frame" form, which is a pairlist. tag is
                             * binding name, car is binding value
                             */
                            RStringVector bindings = env.ls(true, null, false);
                            for (int i = 0; i < bindings.getLength(); i++) {
                                Object value = getValueIgnoreActiveBinding(env.getFrame(), bindings.getDataAt(i));
                                writePairListEntry(bindings.getDataAt(i), value);
                            }
                            terminatePairList();
                            writeItem(RNull.instance); // hashtab
                            OutAttributes attributes = new OutAttributes(env, type, gnuRType);
                            if (attributes.hasAttributes()) {
                                writeAttributes(attributes);
                            } else {
                                writeItem(RNull.instance);
                            }
                        }
                    } else {
                        // flags
                        OutAttributes attributes = new OutAttributes(obj, type, gnuRType);
                        boolean hasTag = gnuRType == SEXPTYPE.CLOSXP || gnuRType == SEXPTYPE.DOTSXP || (gnuRType == SEXPTYPE.PROMSXP && !((RPromise) obj).isEvaluated()) ||
                                        (type == SEXPTYPE.LISTSXP && !((RPairList) obj).isNullTag());
                        int gpbits = getGPBits(obj);
                        int flags = Flags.packFlags(gnuRType, gpbits, isObject(obj), attributes.hasAttributes(), hasTag);
                        stream.writeInt(flags);
                        switch (type) {
                            case STRSXP: {
                                outStringVec((RAbstractStringVector) obj, true);
                                break;
                            }

                            case INTSXP:
                            case LGLSXP: {
                                // logicals are written as ints
                                RAbstractVector vector = (RAbstractVector) obj;
                                VectorAccess access = vector.slowPathAccess();
                                try (SequentialIterator iter = access.access(vector)) {
                                    stream.writeInt(access.getLength(iter));
                                    while (access.next(iter)) {
                                        stream.writeInt(access.getInt(iter));
                                    }
                                }
                                break;
                            }

                            case REALSXP: {
                                RAbstractDoubleVector vector = (RAbstractDoubleVector) obj;
                                VectorAccess access = vector.slowPathAccess();
                                try (SequentialIterator iter = access.access(vector)) {
                                    stream.writeInt(access.getLength(iter));
                                    while (access.next(iter)) {
                                        stream.writeDouble(access.getDouble(iter));
                                    }
                                }
                                break;
                            }

                            case CPLXSXP: {
                                RAbstractComplexVector vector = (RAbstractComplexVector) obj;
                                VectorAccess access = vector.slowPathAccess();
                                try (SequentialIterator iter = access.access(vector)) {
                                    stream.writeInt(access.getLength(iter));
                                    while (access.next(iter)) {
                                        if (access.isNA(iter)) {
                                            stream.writeDouble(RRuntime.DOUBLE_NA);
                                            stream.writeDouble(RRuntime.DOUBLE_NA);
                                        } else {
                                            stream.writeDouble(access.getComplexR(iter));
                                            stream.writeDouble(access.getComplexI(iter));
                                        }
                                    }
                                }
                                break;
                            }

                            case EXPRSXP:
                            case VECSXP: {
                                RAbstractListBaseVector vector = (RAbstractListBaseVector) obj;
                                VectorAccess access = vector.slowPathAccess();
                                try (SequentialIterator iter = access.access(vector)) {
                                    stream.writeInt(access.getLength(iter));
                                    while (access.next(iter)) {
                                        writeItem(access.getListElement(iter));
                                    }
                                }
                                break;
                            }

                            case RAWSXP: {
                                RAbstractRawVector vector = (RAbstractRawVector) obj;
                                VectorAccess access = vector.slowPathAccess();
                                try (SequentialIterator iter = access.access(vector)) {
                                    stream.writeInt(access.getLength(iter));
                                    while (access.next(iter)) {
                                        stream.writeRaw(access.getRaw(iter));
                                    }
                                }
                                break;
                            }

                            case EXTPTRSXP: {
                                addReadRef(obj);
                                RExternalPtr xptr = (RExternalPtr) obj;
                                writeItem(xptr.getProt());
                                writeItem(xptr.getTag());
                                break;
                            }

                            case S4SXP: {
                                break;
                            }

                            /*
                             * The objects that GnuR represents as a pairlist. To avoid stack
                             * overflow, these utilize manual tail recursion on the cdr of the
                             * pairlist.closePairList
                             */

                            case FUNSXP:
                            case PROMSXP:
                            case LANGSXP:
                            case LISTSXP:
                            case DOTSXP: {
                                if (type == SEXPTYPE.FUNSXP && gnuRType == SEXPTYPE.BUILTINSXP) {
                                    // special case
                                    RFunction fun = (RFunction) obj;
                                    String name = fun.getRBuiltin().getName();
                                    stream.writeString(name);
                                    break;
                                }
                                tailCall = true;

                                // attributes written first to avoid recursion on cdr
                                writeAttributes(attributes);
                                if (attributes != null) {
                                    attributes = null;
                                }

                                switch (type) {
                                    case FUNSXP: {
                                        RFunction fun = (RFunction) obj;
                                        RPairList pl = (RPairList) serializeLanguageObject(state, fun);
                                        assert pl != null;
                                        if (RContext.getInstance().matchesOption(FastROptions.Debug, "printWclosure")) {
                                            Debug.printClosure(pl);
                                        }
                                        writeItem(pl.getTag());
                                        writeItem(pl.car());
                                        obj = pl.cdr();
                                        break;
                                    }

                                    case PROMSXP: {
                                        RPairList pl = (RPairList) serializeLanguageObject(state, obj);
                                        assert pl != null;
                                        if (pl.getTag() != RNull.instance) {
                                            writeItem(pl.getTag());
                                        }
                                        writeItem(pl.car());
                                        obj = pl.cdr();
                                        break;
                                    }

                                    case LISTSXP: {
                                        RPairList pl = (RPairList) obj;
                                        if (!pl.isNullTag()) {
                                            writeItem(pl.getTag());
                                        }
                                        writeItem(pl.car());
                                        obj = pl.cdr();
                                        break;
                                    }

                                    case LANGSXP: {
                                        RPairList pl = (RPairList) serializeLanguageObject(state, obj);
                                        writeItem(pl.car());
                                        obj = pl.cdr();
                                        break;
                                    }

                                    case DOTSXP: {
                                        // This in GnuR is a pairlist
                                        RArgsValuesAndNames rvn = (RArgsValuesAndNames) obj;
                                        Object list = RNull.instance;
                                        for (int i = rvn.getLength() - 1; i >= 0; i--) {
                                            String name = rvn.getSignature().getName(i);
                                            list = RDataFactory.createPairList(rvn.getArgument(i), list, name == null ? RNull.instance : RDataFactory.createSymbolInterned(name));
                                        }
                                        RPairList pl = (RPairList) list;
                                        if (!pl.isNullTag()) {
                                            writeItem(pl.getTag());
                                        }
                                        writeItem(pl.car());
                                        obj = pl.cdr();
                                        break;
                                    }
                                }
                                break;
                            }

                            default:
                                throw RInternalError.unimplemented(type.name());
                        }

                        writeAttributes(attributes);
                    }
                }
            } while (tailCall);
        }

        private static Object getValueIgnoreActiveBinding(Frame frame, String key) {
            FrameDescriptor fd = frame.getFrameDescriptor();
            FrameSlot slot = fd.findFrameSlot(key);
            if (slot == null) {
                return null;
            } else {
                return frame.getValue(slot);
            }
        }

        private Object getPersistentName(Object obj) {
            if (hook == null) {
                return RNull.instance;
            }
            switch (SEXPTYPE.typeForClass(obj)) {
                case WEAKREFSXP:
                case EXTPTRSXP:
                    break;
                case ENVSXP:
                    REnvironment env = (REnvironment) obj;
                    if (env == REnvironment.globalEnv() || env == REnvironment.emptyEnv() || env == REnvironment.baseEnv() || env.isNamespaceEnv() || env.isPackageEnv() != null) {
                        return RNull.instance;
                    } else {
                        break;
                    }
                default:
                    return RNull.instance;
            }
            Object result = hook.eval(obj);
            if (result instanceof String) {
                result = RDataFactory.createStringVectorFromScalar((String) result);
            }
            return result;
        }

        private void outStringVec(RAbstractStringVector vec, boolean strsxp) throws IOException {
            if (!strsxp) {
                stream.writeInt(0);
            }
            stream.writeInt(vec.getLength());
            for (int i = 0; i < vec.getLength(); i++) {
                writeCHARSXP(vec.getDataAt(i));
            }
        }

        private static final int ASCII_MASK = 1 << 6;

        /**
         * Write the element of a STRSXP. We can't call {@link #writeItem} because that always
         * treats a {@code String} as an STRSXP.
         */
        private void writeCHARSXP(String s) throws IOException {
            if (RRuntime.isNA(s)) {
                int flags = Flags.packFlags(SEXPTYPE.CHARSXP, 0, false, false, false);
                stream.writeInt(flags);
                stream.writeInt(-1);
            } else {
                /*
                 * GnuR uses the gpbits field of an SEXP to encode CHARSXP charset bits. We
                 * obviously can't do that for a String as we have nowhere to store the value. For
                 * temporary compatibility we set the ASCII bit to allow tests that inspect the raw
                 * form of the serialized output (e.g digest) to pass
                 */
                int flags = Flags.packFlags(SEXPTYPE.CHARSXP, ASCII_MASK, false, false, false);
                stream.writeInt(flags);
                stream.writeString(s);
            }
        }

        private void writeAttributes(OutAttributes outAttrs) throws IOException {
            if (outAttrs != null) {
                SourceSection ss = outAttrs.getSourceReferenceAttributes();
                if (ss != null) {
                    String path = RSource.getPathInternal(ss.getSource());
                    if (path != null) {
                        // do this only for packages
                        TruffleFile relPath = relativizeLibPath(context, path);
                        if (relPath != null) {
                            REnvironment createSrcfile = RSrcref.createSrcfile(context, relPath, state.envRefHolder);
                            Object createLloc = RSrcref.createLloc(ss, createSrcfile);
                            writePairListEntry(RRuntime.R_SRCREF, createLloc);
                            writePairListEntry(RRuntime.R_SRCFILE, createSrcfile);
                        }
                    }
                }
                DynamicObject attributes = outAttrs.getExplicitAttributes();
                if (attributes != null) {
                    // have to convert to GnuR pairlist
                    Iterator<RAttributesLayout.RAttribute> iter = RAttributesLayout.asIterable(attributes).iterator();
                    while (iter.hasNext()) {
                        RAttributesLayout.RAttribute attr = iter.next();
                        // name is the tag of the virtual pairlist
                        // value is the car
                        // next is the cdr
                        writePairListEntry(attr.getName(), attr.getValue());
                    }
                }
                if (outAttrs.hasAttributes()) {
                    terminatePairList();
                }
            }
        }

        private void writePairListEntry(String name, Object value) throws IOException {
            boolean isActiveBinding = ActiveBinding.isActiveBinding(value);
            stream.writeInt(Flags.packFlags(SEXPTYPE.LISTSXP, isActiveBinding ? Flags.IS_ACTIVE_BINDING_MASK : 0, false, false, true));
            RSymbol sym = state.findSymbol(name);
            int refIndex;
            if ((refIndex = getRefIndex(sym)) != -1) {
                outRefIndex(refIndex);
            } else {
                writeSymbol(sym);
            }
            if (isActiveBinding) {
                writeItem(((ActiveBinding) value).getFunction());
            } else {
                writeItem(value);
            }
        }

        private void writeSymbol(RSymbol name) throws IOException {
            addReadRef(name);
            stream.writeInt(SEXPTYPE.SYMSXP.code);
            writeCHARSXP(name.getName());
        }

        private void terminatePairList() throws IOException {
            // TODO: gpbits for encoding NULL value flags (second parameter)
            stream.writeInt(Flags.packFlags(SEXPTYPE.NILVALUE_SXP, 0, false, false, false));
        }

        private void outRefIndex(int index) throws IOException {
            if (index > MAX_PACKED_INDEX) {
                stream.writeInt(SEXPTYPE.REFSXP.code);
                stream.writeInt(index);
            } else {
                stream.writeInt(packRefIndex(index));
            }
        }

        private static boolean isALTREP(Object obj) {
            return obj instanceof RIntSequence || obj instanceof RToStringVectorClosure;
        }
    }

    /**
     * Value that is passed to the Truffle AST walker {@code RSyntaxNode.serialize} to convert AST
     * nodes into a pairlist as required by the GnuR serialization format. The intent is to abstract
     * the client from the details of physical pairlists, possibly not actually creating them at
     * all. Since pairlists are inherently recursive structures through the {@code car} and
     * {@code cdr} fields, the class maintains a virtual stack, however for the most part only the
     * top entry is of interest. The general invariant is that on entry to
     * {@code RSyntaxNode.serialize} the top entry is the one that the method should update and
     * leave on the stack. Any child nodes visited by the method may need a new virtual pairlist
     * that should be pushed with one of the {@code openXXX} methods prior to calling the child's
     * {@code serialize} method. On return the caller is responsible for removing the virtual
     * pairlist with {@link State#closePairList()} and assigning it into the appropriate field (
     * {@code car} or {@code cdr}) of it's virtual pairlist.
     */
    private abstract static class State {

        private final Map<String, RSymbol> symbolMap = new HashMap<>();

        /**
         * This set holds all srcfile attribute paths created during a serialization. It prevents
         * the associated environments from being garbage-collected before the serialization
         * finishes. The holding set is stored in the static <code>envRefHolderMap</code>, where the
         * key is an object representing the serialization session, which usually is some hook
         * function. See {@link RContext#srcfileEnvironments}.
         */
        private final Set<Object> envRefHolder;

        private static final WeakHashMap<Object, Set<Object>> envRefHolderMap = new WeakHashMap<>();

        private final RContext context;

        private State(RContext context, Object serialSessionRef) {
            this.context = context;
            if (serialSessionRef != null) {
                Set<Object> refHolder = envRefHolderMap.get(serialSessionRef);
                if (refHolder == null) {
                    refHolder = new HashSet<>();
                    envRefHolderMap.put(serialSessionRef, refHolder);
                }
                envRefHolder = refHolder;
            } else {
                envRefHolder = new HashSet<>();
            }
        }

        public RContext getContext() {
            return context;
        }

        /**
         * Pushes a new virtual pairlist (no type) onto the stack. An untyped pairlist is subject to
         * the down-shifting to a simple value on {@link #closePairList()}.
         *
         * @return {@code this}, to allow immediate use of {@code setTag}.
         */
        public abstract State openPairList();

        /**
         * Pushes a new virtual pairlist of specific type onto the stack. Such a virtual pairlist
         * will never down-shift" to its {@code car}.
         *
         * @return {@code this}, to allow immediate use of {@code setTag}.
         */
        public abstract State openPairList(SEXPTYPE type);

        /**
         * Change the type of the active element to LANGSXP.
         */
        public abstract void setAsLangType();

        /**
         * Sets the {@code tag} of the current pairlist.
         */
        public abstract void setTag(Object tag);

        /**
         * A special form of {@link #setTag} that <b<must</b> be used for symbols, i.e. identifiers.
         */
        public abstract void setTagAsSymbol(String name);

        /**
         * A special form of {@link #setCar} that <b<must</b> be used for symbols, i.e. identifiers.
         */
        public abstract void setCarAsSymbol(String name);

        /**
         * Sets the {@code car} of the current pairlist.
         */
        public abstract void setCar(Object car);

        /**
         * Sets the {@code cdr} of the current pairlist.
         */
        public abstract void setCdr(Object cdr);

        /**
         * Use this for the case where the current pairlist should be replaced by an {@link RNull}
         * value, e.g., empty statement sequence.
         */
        public abstract void setNull();

        /**
         * Closes the current pairlist, handling the case where a "simple" value is down-shifted
         * from a pairlist to just the value.
         *
         * @return If the {@code tag}, {@code type} and the {@cdr} are unset ({@code null}), return
         *         the {@code car} else return the pairlist.
         */
        public abstract Object closePairList();

        /**
         * Use this for sequences, e.g. formals, call arguments, statements. If {@code n <= 1} has
         * no effect, otherwise it connects the stack of pairlists through their {@code cdr} fields,
         * terminates the pairlist with a {@link RNull#instance} and and pops {@code n - 1} entries
         * off the stack. On exit, therefore, the top element of the stack is the head of a chained
         * list.
         */
        public abstract void linkPairList(int n);

        /**
         * Special case where the value is in the {@code cdr} and it needs to be in the {@code car}.
         */
        public abstract void switchCdrToCar();

        /**
         * Clean up any {@link RUnboundValue}s from shrink optimization.
         */
        public abstract void convertUnboundValues(RPairList pl);

        // Implementation independent convenience methods

        /**
         * Similar to {@link #setNull} but denotes a missing value, e.g., missing default for
         * function argument.
         */
        public void setCarMissing() {
            setCar(RMissing.instance);
        }

        RSymbol findSymbol(String name) {
            RSymbol symbol = symbolMap.get(name);
            if (symbol == null) {
                CompilerAsserts.neverPartOfCompilation(); // for interning
                symbol = RDataFactory.createSymbolInterned(name);
                symbolMap.put(name, symbol);
            }
            return symbol;
        }
    }

    /**
     * Implementation that creates a physical {@link RPairList}.
     */
    private static class PLState extends State {
        private static final RPairList NULL = RDataFactory.createPairList();
        private final Deque<RPairList> active = new LinkedList<>();

        private PLState(RContext context, Object serialSessionRef) {
            super(context, serialSessionRef);
        }

        @Override
        public State openPairList() {
            /*
             * In order to implement the "shrink" optimization in closePairList we set the tag and
             * the cdr to RUnboundValue. N.B. It is a bug if this ever escapes to the outside world.
             */
            RPairList result = RDataFactory.createPairList(RNull.instance, RUnboundValue.instance, RUnboundValue.instance);
            active.addFirst(result);
            return this;
        }

        @Override
        public State openPairList(SEXPTYPE type) {
            RPairList result = RDataFactory.createPairList();
            result.setType(type);
            active.addFirst(result);
            return this;
        }

        @Override
        public void setAsLangType() {
            assert active.peekFirst() != NULL;
            active.peekFirst().setType(SEXPTYPE.LANGSXP);
        }

        @Override
        public void setTag(Object tag) {
            active.peekFirst().setTag(tag);
        }

        @Override
        public void setTagAsSymbol(String name) {
            active.peekFirst().setTag(findSymbol(name));
        }

        @Override
        public void setCarAsSymbol(String name) {
            active.peekFirst().setCar(findSymbol(name));
        }

        @Override
        public void setCar(Object car) {
            active.peekFirst().setCar(car);
        }

        @Override
        public void setCdr(Object cdr) {
            active.peekFirst().setCdr(cdr);
        }

        @Override
        public void setNull() {
            active.removeFirst();
            active.addFirst(NULL);
        }

        @Override
        public Object closePairList() {
            RPairList top = active.removeFirst();
            if (top == NULL) {
                return RNull.instance;
            } else {
                if (top.cdr() == RUnboundValue.instance) {
                    if (top.getTag() == RUnboundValue.instance && top.getType() == null) {
                        // shrink back to non-pairlist (cf GnuR)
                        return top.car();
                    } else {
                        top.setCdr(RNull.instance);
                        return top;
                    }
                } else {
                    return top;
                }
            }
        }

        @Override
        public void linkPairList(int n) {
            if (n > 1) {
                setCdr(RNull.instance); // terminate pairlist
                for (int i = 0; i < n - 1; i++) {
                    RPairList top = active.removeFirst();
                    setCdr(top); // chain
                }
            }
        }

        @Override
        public void switchCdrToCar() {
            RPairList pl = active.removeFirst();
            // setting the type prevents the usual value down-shift on close
            RPairList spl;
            if (pl.cdr() instanceof RPairList && ((RPairList) pl.cdr()).getType() == null) {
                // preserve the "shrink" optimization
                spl = RDataFactory.createPairList(pl.cdr(), RUnboundValue.instance, RUnboundValue.instance);
            } else {
                spl = RDataFactory.createPairList(pl.cdr(), RNull.instance, RNull.instance, SEXPTYPE.LISTSXP);
            }
            active.addFirst(spl);
        }

        @Override
        public String toString() {
            // IDE debugging
            Iterator<RPairList> iter = active.iterator();
            if (iter.hasNext()) {
                StringBuilder sb = new StringBuilder();
                while (iter.hasNext()) {
                    RPairList pl = iter.next();
                    sb.append('[');
                    if (pl == NULL) {
                        sb.append("NULL");
                    } else {
                        sb.append(pl.toString());
                    }
                    sb.append("] ");
                }
                return sb.toString();
            } else {
                return "EMPTY";
            }
        }

        @Override
        public void convertUnboundValues(RPairList pl) {
            Object obj = pl;
            while (obj instanceof RPairList) {
                RPairList plt = (RPairList) obj;
                if (plt.getTag() == RUnboundValue.instance) {
                    plt.setTag(RNull.instance);
                }
                if (plt.car() instanceof RPairList) {
                    convertUnboundValues((RPairList) plt.car());
                }
                obj = plt.cdr();
                assert !(obj instanceof RUnboundValue);
            }
        }
    }

    /**
     * An abstraction of implicit and explicit attributes to serialize.
     */
    private static class OutAttributes {
        private DynamicObject explicitAttributes;
        private SourceSection ss;

        private OutAttributes(Object obj, SEXPTYPE type, SEXPTYPE gnuRType) {

            if (obj instanceof RAttributable) {
                explicitAttributes = ((RAttributable) obj).getAttributes();
            }
            initSourceSection(obj, type, gnuRType);
        }

        private void initSourceSection(Object obj, SEXPTYPE type, SEXPTYPE gnuRType) {
            if (type == SEXPTYPE.FUNSXP && gnuRType != SEXPTYPE.BUILTINSXP) {
                RFunction fun = (RFunction) obj;
                RSyntaxFunction body = (RSyntaxFunction) fun.getRootNode();
                setSourceSection(body);
            }
        }

        private void setSourceSection(RSyntaxElement body) {
            SourceSection lazySourceSection = body.getLazySourceSection();
            if (RSource.getPathInternal(lazySourceSection.getSource()) != null) {
                ss = lazySourceSection;
            }
        }

        public boolean hasAttributes() {
            return explicitAttributes != null && explicitAttributes.getShape().getPropertyCount() != 0 || ss != null;
        }

        public DynamicObject getExplicitAttributes() {
            return explicitAttributes;
        }

        public SourceSection getSourceReferenceAttributes() {
            return ss;
        }
    }

    /**
     * For {@code lazyLoadDBinsertValue}.
     */
    @TruffleBoundary
    public static byte[] serialize(RContext context, Object obj, int type, int version, Object refhook) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Output output = new Output(context, type, version, (CallHook) refhook, out);
            output.serialize(obj);
            return out.toByteArray();
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @TruffleBoundary
    public static void serialize(RContext context, RConnection conn, Object obj, int type, int version, Object refhook) throws IOException {
        Output output = new Output(context, type, version, (CallHook) refhook, conn.getOutputStream());
        output.serialize(obj);
    }

    private static class Debug {
        private static final TruffleLogger LOGGER = RLogger.getLogger(RSerialize.class.getName());
        private static int indent;

        private static void printClosure(RPairList pl) {
            indent = 0;
            printObject(pl);
        }

        private static SEXPTYPE type(Object obj) {
            if ((obj instanceof RPairList && !((RPairList) obj).isLanguage())) {
                SEXPTYPE s = ((RPairList) obj).getType();
                return s == null ? SEXPTYPE.LISTSXP : s;
            } else {
                return SEXPTYPE.typeForClass(obj);
            }
        }

        private static void printObject(Object obj) {
            printObject(obj, true);

        }

        private static void printObject(Object obj, boolean printType) {
            SEXPTYPE type = type(obj);
            if (printType) {
                print("%s", type.name());
            }
            switch (type) {
                case SYMSXP: {
                    print("\"%s\"", ((RSymbol) obj).getName());
                    break;
                }

                case CLOSXP:
                case LISTSXP:
                case LANGSXP: {
                    RPairList pl = (RPairList) obj;
                    indent++;
                    print("TAG: %s", pl.getTag());
                    SEXPTYPE carType = type(pl.car());
                    print("CAR: %s %s", type(pl.car()).name(), (carType == SEXPTYPE.SYMSXP ? ((RSymbol) pl.car()).getName() : ""));
                    if (carType != SEXPTYPE.SYMSXP) {
                        printObject(pl.car(), false);
                    }
                    SEXPTYPE cdrType = type(pl.cdr());
                    print("CDR: %s %s", type(pl.cdr()).name(), (cdrType == SEXPTYPE.SYMSXP ? ((RSymbol) pl.cdr()).getName() : ""));
                    if (cdrType != SEXPTYPE.SYMSXP) {
                        printObject(pl.cdr(), false);
                    }
                    indent--;
                    break;
                }

                default:
            }
        }

        private static void print(String format, Object... objects) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < indent * 2; i++) {
                sb.append(' ');
            }
            sb.append(String.format(format, objects));
            LOGGER.info(sb.toString());
        }
    }

    private static final class SerializeVisitor extends RSyntaxVisitor<Void> {

        private final State state;

        SerializeVisitor(State state) {
            this.state = state;
        }

        @Override
        protected Void visit(RSyntaxCall element) {
            state.setAsLangType();
            state.openPairList();
            accept(element.getSyntaxLHS());
            state.setCar(state.closePairList());
            RSyntaxElement[] arguments = element.getSyntaxArguments();
            RSyntaxElement lhs = element.getSyntaxLHS();
            if (isColon(lhs)) {
                // special case, have to translate Identifier names to Symbols
                for (int i = 0; i < 2; i++) {
                    RSyntaxElement arg = arguments[i];
                    state.openPairList();
                    if (arg instanceof RSyntaxLookup) {
                        state.setCarAsSymbol(((RSyntaxLookup) arg).getIdentifier());
                    } else {
                        state.setCar(((RSyntaxConstant) arg).getValue());
                    }
                }
                state.linkPairList(2);
                state.setCdr(state.closePairList());
            } else {
                boolean infixFieldAccess = false;
                if (lhs instanceof RSyntaxLookup) {
                    String name = ((RSyntaxLookup) lhs).getIdentifier();
                    infixFieldAccess = "$".equals(name) || "@".equals(name);
                }
                serializeArguments(element, arguments, element.getSyntaxSignature(), infixFieldAccess);
            }
            return null;
        }

        private static boolean isColon(RSyntaxElement element) {
            if (element instanceof RSyntaxLookup) {
                String name = ((RSyntaxLookup) element).getIdentifier();
                return name.equals("::") || name.equals(":::");
            }
            return false;
        }

        private void serializeArguments(RSyntaxCall callElement, RSyntaxElement[] arguments, ArgumentsSignature signature, boolean infixFieldAccess) {
            state.openPairList(SEXPTYPE.LISTSXP);
            if (arguments.length == 0) {
                state.setNull();
            } else {
                for (int i = 0; i < arguments.length; i++) {
                    RSyntaxElement argument = arguments[i];
                    String name = signature.getName(i);
                    if (name != null) {
                        state.setTagAsSymbol(name);
                    }
                    if (argument == null) {
                        state.setCarMissing();
                    } else {
                        if (infixFieldAccess && i == 1 && argument instanceof RSyntaxConstant) {
                            RSyntaxConstant c = (RSyntaxConstant) argument;
                            String identifier = RRuntime.asStringLengthOne(c.getValue());
                            assert identifier != null;
                            state.setCarAsSymbol(identifier);
                        } else {
                            state.openPairList();
                            accept(argument);
                            state.setCar(state.closePairList());
                        }
                    }
                    if (i != arguments.length - 1) {
                        state.openPairList();
                    }
                }
                state.linkPairList(arguments.length);
            }
            Object pl = state.closePairList();
            if (callElement instanceof Node && RContext.getRRuntimeASTAccess().isTaggedWith((Node) callElement, StandardTags.StatementTag.class)) {
                attachSrcref(callElement, pl, state);
            }
            state.setCdr(pl);
        }

        @Override
        protected Void visit(RSyntaxConstant element) {
            if (element.getValue() == RMissing.instance) {
                state.setCar(RMissing.instance);
            } else {
                state.setCar(element.getValue());
            }
            return null;
        }

        @Override
        protected Void visit(RSyntaxLookup element) {
            state.setCarAsSymbol(element.getIdentifier());
            return null;
        }

        @Override
        protected Void visit(RSyntaxFunction element) {
            state.setAsLangType();
            state.setCarAsSymbol("function");
            state.openPairList(SEXPTYPE.LISTSXP);
            state.setCar(visitFunctionFormals(element));
            state.openPairList(SEXPTYPE.LISTSXP);
            state.setCdr(visitFunctionBody(element));
            state.switchCdrToCar();
            state.openPairList(SEXPTYPE.LISTSXP);
            state.setCar(RNull.instance);
            state.setCdr(RNull.instance);
            state.setCdr(state.closePairList());
            state.setCdr(state.closePairList());
            state.setCdr(state.closePairList());
            return null;
        }

        /**
         * Serialize a function's formal arguments. On entry {@code state} has an active pairlist,
         * whose {@code tag} is the enclosing {@link REnvironment}. The {@code car} will be set to
         * the pairlist representing the formal arguments (or {@link RNull} if none). Each formal
         * argument is represented as a pairlist:
         * <ul>
         * <li>{@code tag}: RSymbol(name)</li>
         * <li>{@code car}: Missing or default value</li>
         * <li>{@code cdr}: if last formal then RNull else pairlist for next argument.
         * </ul>
         */
        public Object visitFunctionFormals(RSyntaxFunction element) {
            ArgumentsSignature signature = element.getSyntaxSignature();
            RSyntaxElement[] defaults = element.getSyntaxArgumentDefaults();
            if (signature.getLength() > 0) {
                for (int i = 0; i < signature.getLength(); i++) {
                    state.openPairList();
                    state.setTagAsSymbol(signature.getName(i));
                    if (defaults[i] != null) {
                        state.openPairList();
                        accept(defaults[i]);
                        state.setCar(state.closePairList());
                    } else {
                        state.setCarMissing();
                    }
                }
                state.linkPairList(signature.getLength());
                return state.closePairList();
            } else {
                return RNull.instance;
            }
        }

        /**
         * Serialize a function's body. On entry {@code state} has an active pairlist, whose
         * {@code tag} is the enclosing {@link REnvironment}. The {@code cdr} to the pairlist
         * representing the body. The body is never empty as the syntax "{}" has a value, however if
         * the body is a simple expression, e.g. {@code function(x) x}, the body is not represented
         * as a pairlist, just a SYMSXP, which is handled transparently in
         * {@code RSerialize.State.closePairList()}.
         */
        public Object visitFunctionBody(RSyntaxFunction element) {
            state.openPairList();
            accept(element.getSyntaxBody());
            return state.closePairList();
        }
    }

    private static Object serializeLanguageObject(RSerialize.State state, Object obj) {
        if (obj instanceof RPairList) {
            RPairList list = (RPairList) obj;
            if (list.isLanguage() && list.hasClosure()) {
                RPairList result = (RPairList) RSerialize.serializeLanguage(state, list);
                state.convertUnboundValues(result);
                return result;
            } else {
                return list;
            }
        } else if (obj instanceof RFunction) {
            RPairList result = (RPairList) RSerialize.serializeFunction(state, (RFunction) obj);
            state.convertUnboundValues(result);
            return result;
        } else if (obj instanceof RPromise) {
            RPairList result = (RPairList) RSerialize.serializePromise(state, (RPromise) obj);
            state.convertUnboundValues(result);
            return result;
        } else {
            throw RInternalError.unimplemented("serialize");
        }
    }

    private static Object serializeFunction(State state, RFunction f) {
        RSyntaxFunction function = (RSyntaxFunction) f.getRootNode();
        REnvironment env = REnvironment.frameToEnvironment(f.getEnclosingFrame());
        state.openPairList().setTag(env);
        serializeFunctionDefinition(state, function);
        Object pl = state.closePairList();
        return pl;
    }

    private static void serializeFunctionDefinition(State state, RSyntaxFunction function) {
        SerializeVisitor visitor = new SerializeVisitor(state);
        state.setCar(visitor.visitFunctionFormals(function));
        Object body = visitor.visitFunctionBody(function);

        // convert and attach source section to srcref attribute
        attachSrcref(function, body, state);

        state.setCdr(body);
    }

    /**
     * Converts the source section from the syntax element to a srcref attribute and attaches it to
     * the serialization object.
     *
     * @param syntaxElement The syntax element providing the source section.
     * @param serObj The object to attribute (most likely a pair list).
     */
    private static void attachSrcref(RSyntaxElement syntaxElement, Object serObj, State state) {
        SourceSection ss = getFileSourceSection(syntaxElement);
        if (ss != null && serObj instanceof RAttributable) {
            String pathInternal = RSource.getPathInternal(ss.getSource());

            // do this only for packages
            RContext ctx = state.getContext();
            TruffleFile relPath = relativizeLibPath(ctx, pathInternal);
            if (relPath != null) {
                RAttributable attributable = (RAttributable) serObj;
                attributable.setAttr(RRuntime.R_SRCFILE, RSrcref.createSrcfile(ctx, relPath, state.envRefHolder));
                RList createBlockSrcrefs = RSrcref.createBlockSrcrefs(syntaxElement);
                if (createBlockSrcrefs != null) {
                    attributable.setAttr(RRuntime.R_SRCREF, createBlockSrcrefs);
                    attributable.setAttr(RRuntime.R_WHOLE_SRCREF, RSrcref.createLloc(ctx, ss));
                } else {
                    Object createLloc = RSrcref.createLloc(ctx, ss);
                    attributable.setAttr(RRuntime.R_SRCREF, createLloc);
                    attributable.setAttr(RRuntime.R_WHOLE_SRCREF, RSrcref.createLloc(ctx, ss));
                }
            }
        }
    }

    /**
     * Relativizes the given path to its corresponding library path. If the given path is not a
     * child of any library path, {@code null} will be returned.
     */
    private static TruffleFile relativizeLibPath(RContext context, String path) {
        TruffleFile file = context.getSafeTruffleFile(path);
        for (String libPath : RContext.getInstance().libraryPaths) {
            if (file.startsWith(libPath)) {
                return context.getSafeTruffleFile(libPath).relativize(file);
            }
        }
        return null;
    }

    private static SourceSection getFileSourceSection(RSyntaxElement syntaxElement) {
        SourceSection ss = syntaxElement.getLazySourceSection();
        if (ss != null && RSource.getPathInternal(ss.getSource()) != null) {
            return ss;
        }
        return null;
    }

    private static Object serializeLanguage(State state, RPairList lang) {
        RSyntaxElement element = lang.getSyntaxElement();
        state.openPairList(SEXPTYPE.LANGSXP);
        new SerializeVisitor(state).accept(element);
        return state.closePairList();
    }

    private static Object serializePromise(State state, RPromise promise) {
        /*
         * If the promise is evaluated, we store the value (in car) and the tag is set to RNull,
         * else we record the environment in the tag and store RUnboundValue. In either case we
         * record the expression.
         */
        Object value;
        Object tag;
        if (promise.isEvaluated()) {
            value = promise.getValue();
            tag = RNull.instance;
        } else {
            value = RUnboundValue.instance;
            tag = promise.getFrame() == null ? REnvironment.globalEnv() : REnvironment.frameToEnvironment(promise.getFrame());
        }
        state.openPairList().setTag(tag);
        state.setCar(value);
        state.openPairList();
        new SerializeVisitor(state).accept(promise.getRep().asRSyntaxNode());
        state.setCdr(state.closePairList());
        return state.closePairList();
    }

    /**
     * A collection of static functions that will transform a pairlist into an AST using the
     * {@link RCodeBuilder}.
     */
    private static final class PairlistDeserializer {

        public static RFunction processFunction(Object car, Object cdr, REnvironment environment, String functionName, String packageName) {
            // car == arguments, cdr == body, tag == PairList(attributes, environment)

            MaterializedFrame enclosingFrame = environment.getFrame();

            RootCallTarget callTarget = RContext.getASTBuilder().rootFunction(RContext.getInstance().getLanguage(), RSyntaxNode.LAZY_DEPARSE, processArguments(car, false), processBody(cdr),
                            functionName == null ? "<deserialized function>" : functionName);
            FrameSlotChangeMonitor.initializeEnclosingFrame(callTarget.getRootNode().getFrameDescriptor(), enclosingFrame);
            RFunction func = RDataFactory.createFunction(functionName, packageName, callTarget, null, enclosingFrame);

            /*
             * TODO: this is missing the code that registers sources with RPackageSource!
             */
            return func;
        }

        public static RPairList processLanguage(Object car, Object cdr, Object tag) {
            Closure closure = Closure.createLanguageClosure(processCall(car, cdr, tag, null).asRNode());
            return RDataFactory.createLanguage(closure);
        }

        public static RPromise processPromise(Object car, Object cdr, Object tag) {
            // car == value, cdr == expression, tag == environment

            Closure closure = Closure.createPromiseClosure(processBody(cdr).asRNode());
            if (car == RUnboundValue.instance) {
                REnvironment env = tag == RNull.instance ? REnvironment.baseEnv() : (REnvironment) tag;
                return RDataFactory.createPromise(PromiseState.Explicit, closure, env.getFrame());
            } else {
                return RDataFactory.createEvaluatedPromise(closure, car);
            }
        }

        private static RSyntaxNode process(Object value, boolean isCallLHS, String name) {
            if (value instanceof RSymbol) {
                return RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, ((RSymbol) value).getName(), isCallLHS);
            } else if (value instanceof RPairList) {
                RPairList pl = (RPairList) value;
                switch (pl.getType()) {
                    case LANGSXP:
                        return processCall(pl.car(), pl.cdr(), pl.getTag(), name);
                    case CLOSXP:
                        return processFunctionExpression(pl.car(), pl.cdr(), pl.getTag(), name);
                    default:
                        // other pairlists: include as constants
                        return RContext.getASTBuilder().constant(RSyntaxNode.LAZY_DEPARSE, unwrapScalarValues(value));
                }
            } else {
                assert !(value instanceof RMissing) : "should be handled outside";
                return RContext.getASTBuilder().constant(RSyntaxNode.LAZY_DEPARSE, unwrapScalarValues(value));
            }
        }

        /** Convert single-element atomic vectors to their primitive counterparts. */
        private static Object unwrapScalarValues(Object value) {
            if (value instanceof RAbstractVector) {
                RAbstractVector vector = (RAbstractVector) value;
                if (vector.getLength() == 1 && (vector.getAttributes() == null || vector.getAttributes().getShape().getPropertyCount() == 0)) {
                    if (vector instanceof RAbstractDoubleVector || vector instanceof RAbstractIntVector || vector instanceof RAbstractStringVector ||
                                    vector instanceof RAbstractLogicalVector || vector instanceof RAbstractRawVector || vector instanceof RAbstractComplexVector) {
                        return vector.getDataAtAsObject(0);
                    }
                }
            }
            return value;
        }

        private static RSyntaxNode processCall(Object car, Object cdr, @SuppressWarnings("unused") Object tag, String name) {
            if (car instanceof RSymbol && ((RSymbol) car).getName().equals("function")) {
                RPairList function = (RPairList) cdr;
                return processFunctionExpression(function.car(), function.cdr(), function.getTag(), name);
            }
            boolean isAssignment = car instanceof RSymbol && ((RSymbol) car).getName().equals("<-");
            RSyntaxNode call = RContext.getASTBuilder().call(RSyntaxNode.LAZY_DEPARSE, process(car, true, null), processArguments(cdr, isAssignment));
            if (cdr instanceof RAttributable) {
                handleSrcrefAttr((RAttributable) cdr, call);
            }
            return call;
        }

        private static RSyntaxNode processFunctionExpression(Object car, Object cdr, @SuppressWarnings("unused") Object tag, String name) {
            // car == arguments, cdr == body
            return RContext.getASTBuilder().function(RContext.getInstance().getLanguage(), RSyntaxNode.LAZY_DEPARSE, processArguments(car, false), processBody(cdr),
                            name == null ? "<deserialized function>" : name);
        }

        private static List<RCodeBuilder.Argument<RSyntaxNode>> processArguments(Object args, boolean isAssignment) {
            List<RCodeBuilder.Argument<RSyntaxNode>> list = new ArrayList<>();

            RPairList arglist = args instanceof RNull ? null : (RPairList) args;
            int index = 0;
            String assignedName = null;
            while (arglist != null) {
                // for each argument: tag == name, car == value
                String name = arglist.getTag() == RNull.instance ? null : ((RSymbol) arglist.getTag()).getName();
                if (isAssignment && index == 0 && arglist.car() instanceof RSymbol) {
                    assignedName = ((RSymbol) arglist.car()).getName();
                }
                RSyntaxNode value = arglist.car() == RMissing.instance ? null : process(arglist.car(), false, index == 1 ? assignedName : null);
                list.add(RCodeBuilder.argument(RSyntaxNode.LAZY_DEPARSE, name, value));
                arglist = next(arglist);
                index++;
            }

            return list;
        }

        private static RPairList next(RPairList pairlist) {
            if (pairlist.cdr() == RNull.instance) {
                return null;
            } else {
                return (RPairList) pairlist.cdr();
            }
        }

        private static RSyntaxNode processBody(Object cdr) {
            if (cdr instanceof RPairList) {
                RPairList pl = (RPairList) cdr;
                RSyntaxNode body;
                switch (pl.getType()) {
                    case BCODESXP:
                        RAbstractListVector list = (RAbstractListVector) pl.cdr();
                        body = process(list.getDataAt(0), false, null);
                        break;
                    case LISTSXP:
                        // TODO: it is not clear why is this assertion here
                        // assert pl.cdr() == RNull.instance || (pl.cadr() == RNull.instance &&
                        // pl.cddr() == RNull.instance);
                        body = process(pl.car(), false, null);
                        break;
                    case LANGSXP:
                        body = processCall(pl.car(), pl.cdr(), pl.getTag(), null);
                        break;
                    default:
                        throw RInternalError.shouldNotReachHere("unexpected SXP type in body: " + pl.getType());
                }
                handleSrcrefAttr(pl, body);
                return body;
            }
            return process(cdr, false, null);
        }
    }

    private static void handleFunctionSrcrefAttr(RFunction func) {
        handleSrcrefAttr(func, (RSyntaxElement) func.getRootNode());
    }

    /**
     * @param func Element carrying the {@value RRuntime#R_SRCREF} attribute.
     * @param elem The syntax element to create the source section for.
     */
    private static void handleSrcrefAttr(RAttributable func, RSyntaxElement elem) {
        if (elem instanceof RSyntaxCall) {
            handleSrcrefAttr(RContext.getInstance(), func, (RSyntaxCall) elem);
        } else {
            Object srcref = func.getAttr(RRuntime.R_SRCREF);
            if (srcref instanceof RAbstractIntVector) {
                SourceSection ss = RSrcref.createSourceSection(RContext.getInstance(), (RAbstractIntVector) srcref, null);
                elem.setSourceSection(ss);
            }
        }
    }

    /**
     * @param func Element carrying the {@value RRuntime#R_SRCREF} attribute.
     * @param elem The syntax element to create the source section for.
     */
    private static void handleSrcrefAttr(RContext context, RAttributable func, RSyntaxCall elem) {
        Object srcref = func.getAttr(RRuntime.R_SRCREF);
        if (srcref instanceof RAbstractIntVector) {
            Object srcfile = func.getAttr(RRuntime.R_SRCFILE);
            assert srcfile instanceof REnvironment;
            Source source;
            try {
                source = RSource.fromSrcfile(context, (REnvironment) srcfile);
            } catch (IOException e) {
                source = null;
            }
            SourceSection ss = RSrcref.createSourceSection(context, (RAbstractIntVector) srcref, source);
            elem.setSourceSection(ss);
        } else if (srcref instanceof RList) {
            try {
                Object srcfile = func.getAttr(RRuntime.R_SRCFILE);
                assert srcfile instanceof REnvironment;
                Source source = RSource.fromSrcfile(context, (REnvironment) srcfile);

                RList blockSrcref = (RList) srcref;
                RSyntaxElement[] syntaxArguments = elem.getSyntaxArguments();
                assert syntaxArguments.length == blockSrcref.getLength() - 1;

                for (int i = 0; i < blockSrcref.getLength(); i++) {
                    Object singleSrcref = blockSrcref.getDataAt(i);
                    // could also be NULL
                    if (singleSrcref instanceof RAbstractIntVector) {
                        SourceSection ss = RSrcref.createSourceSection(context, (RAbstractIntVector) singleSrcref, source);
                        if (i == 0) {
                            elem.setSourceSection(ss);
                        } else {
                            syntaxArguments[i - 1].setSourceSection(ss);
                        }
                    }
                }
            } catch (NoSuchFileException e) {
                assert debugWarning("Missing source file: " + e.getMessage());
            } catch (IOException e) {
                assert debugWarning("Cannot access source file: " + e.getMessage());
            }
        }
    }

    private static boolean debugWarning(String message) {
        RError.warning(RError.SHOW_CALLER, RError.Message.GENERIC, message);
        return true;
    }

    /**
     * Prepends the namespace's library location to the library paths in order to enable resolving
     * of serialized relative paths.
     *
     * If a namespace is loaded, it remembers the path where it was loaded from. However, functions
     * are loaded lazily and when deserializing them, there is no information about their origin.
     * This method searches for the enclosing namespace environment (if available) and adds the
     * origin path to the library paths.
     *
     * @param environment The function's environment.
     * @return {@code true} if a path has been added, {@code false} otherwise.
     */
    private static boolean setupLibPath(REnvironment environment) {
        if (environment == REnvironment.baseEnv()) {
            return false;
        }

        REnvironment cur = environment;
        while (cur != REnvironment.emptyEnv() && !cur.isNamespaceEnv()) {
            cur = cur.getParent();
        }
        Object namespaceEnv = cur.get(REnvironment.NAMESPACE_KEY);
        if (namespaceEnv != null) {
            assert namespaceEnv instanceof REnvironment;
            Object pathObj = ((REnvironment) namespaceEnv).get("path");
            if (pathObj instanceof RAbstractStringVector) {
                String path = ((RAbstractStringVector) pathObj).getDataAt(0);
                RContext context = RContext.getInstance();
                TruffleFile libLoc = context.getSafeTruffleFile(path).getParent();
                assert libLoc != null;
                context.libraryPaths.add(0, libLoc.toString());
                return true;
            }
        }
        return false;
    }
}
