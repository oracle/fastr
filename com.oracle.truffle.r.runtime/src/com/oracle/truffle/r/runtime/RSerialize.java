/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RScalar;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

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
    }

    public static final class ContextStateImpl implements RContext.ContextState {
        /**
         * {@code true} iff we are saving the source from the deparse of an unserialized function
         * (for debugging later).
         */
        boolean saveDeparse;

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

    public static final int DEFAULT_VERSION = 2;

    /**
     * Lazily read in case set during execution for debugging purposes. This is necessary because
     * setting the option on startup will trace all the standard library functions as they are
     * lazily loaded.
     */
    private static boolean trace() {
        return FastROptions.debugMatches("unserialize");
    }

    private static ContextStateImpl getContextState() {
        return RContext.getInstance().stateRSerialize;
    }

    /**
     * Supports the saving of deparsed lazily loaded package functions for instrumentation access.
     */
    public static void setSaveDeparse(boolean status) {
        ContextStateImpl serializeContextState = getContextState();
        serializeContextState.saveDeparse = status;
    }

    @TruffleBoundary
    public static Object unserialize(RConnection conn) throws IOException {
        Input instance = trace() ? new TracingInput(conn) : new Input(conn);
        Object result = instance.unserialize();
        return result;
    }

    @TruffleBoundary
    public static Object unserialize(RAbstractRawVector data) {
        byte[] buffer = data.materialize().getDataWithoutCopying();
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

        private Object unserialize() throws IOException {
            int version = stream.readInt();
            @SuppressWarnings("unused")
            int writerVersion = stream.readInt();
            @SuppressWarnings("unused")
            int releaseVersion = stream.readInt();
            assert version == DEFAULT_VERSION; // TODO proper error message
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
                    String s = stream.readString(stream.readInt());
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
                            if (FastROptions.debugMatches("printUclosure")) {
                                RPairList pairList = RDataFactory.createPairList(carItem, cdrItem, tagItem, type);
                                result = pairList;
                                if (attrItem != RNull.instance) {
                                    setAttributes(pairList, attrItem);
                                }
                                Debug.printClosure(pairList);
                            }
                            RFunction func = PairlistDeserializer.processFunction(carItem, cdrItem, tagItem, currentFunctionName, packageName);
                            if (attrItem != RNull.instance) {
                                setAttributes(func, attrItem);
                                handleFunctionSrcrefAttr(func);
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
                                RLanguage lang = PairlistDeserializer.processLanguage(carItem, cdrItem, tagItem);
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
                        ((RTypedValue) result).setGPBits(levs);
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
                    result = RContext.lookupBuiltin(s);
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
                    throw RInternalError.unimplemented();
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
                    ((RTypedValue) result).setGPBits(levs);
                }
            }

            return checkResult(result);
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
                String tag = tagSym.getName().intern();
                // this may convert a plain vector to a data.frame or factor
                Object attrValue = pl.car();
                if (attrValue instanceof RShareable && ((RShareable) attrValue).isTemporary()) {
                    ((RShareable) attrValue).incRefCount();
                }
                if (result instanceof RVector<?> && tag.equals(RRuntime.CLASS_ATTR_KEY)) {
                    RStringVector classes = (RStringVector) attrValue;
                    result = ((RVector<?>) result).setClassAttr(classes);
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
                        assert false;
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

        private final byte[] buf;
        private int size;
        private int offset;

        private final WeakHashMap<String, WeakReference<String>> strings = RContext.getInstance().stringMap;

        XdrInputFormat(InputStream is) {
            super(is);
            if (is instanceof PByteArrayInputStream) {
                // we already have the data and we have read the beginning
                PByteArrayInputStream pbis = (PByteArrayInputStream) is;
                buf = pbis.getData();
                size = pbis.getData().length;
                offset = pbis.pos();
            } else {
                buf = new byte[READ_BUFFER_SIZE];
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

        @SuppressWarnings("deprecation")
        @Override
        String readString(int len) throws IOException {
            ensureData(len);
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

        private void ensureData(int n) throws IOException {
            if (n > buf.length) {
                throw RInternalError.unimplemented("dynamically enlarge buffer");
            }
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

        @Override
        void readRaw(byte[] data) throws IOException {
            ensureData(data.length);
            System.arraycopy(buf, offset, data, 0, data.length);
            offset += data.length;
        }
    }

    /**
     * Traces the items read for debugging.
     */
    private static final class TracingInput extends Input {
        private int nesting;

        private TracingInput(RConnection conn) throws IOException {
            this(conn.getInputStream(), null, null, null);
        }

        private TracingInput(InputStream is, CallHook hook, String packageName, String functionName) throws IOException {
            super(is, hook, packageName, functionName);
        }

        @Override
        protected Object readItem() throws IOException {
            // CheckStyle: stop system..print check
            int flags = stream.readInt();
            SEXPTYPE type = SEXPTYPE.mapInt(Flags.ptype(flags));
            for (int i = 0; i < nesting; i++) {
                System.out.print("  ");
            }
            System.out.printf("%d %s", nesting, type);
            if (type != SEXPTYPE.CHARSXP) {
                System.out.println();
            }
            nesting++;
            Object result = super.readItem(flags);
            if (type == SEXPTYPE.CHARSXP) {
                System.out.printf(" \"%s\"%n", result);
            }
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

        abstract void writeRaw(byte[] value) throws IOException;

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
        void writeRaw(byte[] value) throws IOException {
            int valueLen = value.length;
            if (valueLen > buf.length) {
                flushBuffer();
                os.write(value);
            } else {
                ensureSpace(valueLen);
                System.arraycopy(value, 0, buf, offset, valueLen);
                offset += valueLen;
            }
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

        private Output(OutputStream os, int format, int version, CallHook hook) throws IOException {
            super(hook);
            this.state = new PLState();
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
            switch (version) {
                case DEFAULT_VERSION:
                    stream.writeInt(version);
                    stream.writeInt(RVersionNumber.R_VERSION);
                    stream.writeInt(RVersionInfo.SERIALIZE_VERSION);
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
            if (obj instanceof RTypedValue && !(obj instanceof RExternalPtr || obj instanceof RScalar)) {
                return ((RTypedValue) obj).getGPBits();
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
                SEXPTYPE type = SEXPTYPE.typeForClass(obj.getClass());
                SEXPTYPE gnuRType = SEXPTYPE.gnuRType(type, obj);
                int refIndex;
                if ((refIndex = getRefIndex(obj)) != -1) {
                    outRefIndex(refIndex);
                } else if (type == SEXPTYPE.SYMSXP) {
                    writeSymbol((RSymbol) obj);
                } else {
                    SourceSection sourceSection = getSourceSection(obj);
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
                            String[] bindings = env.ls(true, null, false).getDataWithoutCopying();
                            for (String binding : bindings) {
                                Object value = getValueIgnoreActiveBinding(env.getFrame(), binding);
                                writePairListEntry(binding, value);
                            }
                            terminatePairList();
                            writeItem(RNull.instance); // hashtab
                            DynamicObject attributes = env.getAttributes();
                            writeAttributes(attributes, sourceSection);
                            if (attributes == null && sourceSection == null) {
                                writeItem(RNull.instance);
                            }
                        }
                    } else {
                        // flags
                        DynamicObject attributes = null;
                        SourceSection ss = sourceSection;
                        if (obj instanceof RAttributable) {
                            RAttributable rattr = (RAttributable) obj;
                            attributes = rattr.getAttributes();
                            if (attributes != null && attributes.isEmpty()) {
                                attributes = null;
                            }
                        }
                        boolean hasTag = gnuRType == SEXPTYPE.CLOSXP || gnuRType == SEXPTYPE.DOTSXP || (gnuRType == SEXPTYPE.PROMSXP && !((RPromise) obj).isEvaluated()) ||
                                        (type == SEXPTYPE.LISTSXP && !((RPairList) obj).isNullTag());
                        int gpbits = getGPBits(obj);
                        int flags = Flags.packFlags(gnuRType, gpbits, isObject(obj), attributes != null, hasTag);
                        stream.writeInt(flags);
                        switch (type) {
                            case STRSXP: {
                                if (obj instanceof String) {
                                    // length 1 vector
                                    stream.writeInt(1);
                                    writeCHARSXP((String) obj);
                                } else {
                                    outStringVec((RAbstractStringVector) obj, true);
                                }
                                break;
                            }

                            case INTSXP: {
                                if (obj instanceof Integer) {
                                    stream.writeInt(1);
                                    stream.writeInt((int) obj);
                                } else {
                                    RAbstractIntVector vec = (RAbstractIntVector) obj;
                                    stream.writeInt(vec.getLength());
                                    for (int i = 0; i < vec.getLength(); i++) {
                                        stream.writeInt(vec.getDataAt(i));
                                    }
                                }
                                break;
                            }

                            case REALSXP: {
                                if (obj instanceof Double) {
                                    stream.writeInt(1);
                                    stream.writeDouble((double) obj);
                                } else {
                                    RAbstractDoubleVector vec = (RAbstractDoubleVector) obj;
                                    stream.writeInt(vec.getLength());
                                    for (int i = 0; i < vec.getLength(); i++) {
                                        stream.writeDouble(vec.getDataAt(i));
                                    }
                                }
                                break;
                            }

                            case LGLSXP: {
                                // Output as ints
                                if (obj instanceof Byte) {
                                    stream.writeInt(1);
                                    stream.writeInt(RRuntime.logical2int((byte) obj));
                                } else {
                                    RAbstractLogicalVector vec = (RAbstractLogicalVector) obj;
                                    stream.writeInt(vec.getLength());
                                    for (int i = 0; i < vec.getLength(); i++) {
                                        stream.writeInt(RRuntime.logical2int(vec.getDataAt(i)));
                                    }
                                }
                                break;
                            }

                            case CPLXSXP: {
                                RAbstractComplexVector vec = (RAbstractComplexVector) obj;
                                stream.writeInt(vec.getLength());
                                for (int i = 0; i < vec.getLength(); i++) {
                                    RComplex val = vec.getDataAt(i);
                                    if (RRuntime.isNA(val)) {
                                        stream.writeDouble(RRuntime.DOUBLE_NA);
                                        stream.writeDouble(RRuntime.DOUBLE_NA);
                                    } else {
                                        stream.writeDouble(val.getRealPart());
                                        stream.writeDouble(val.getImaginaryPart());
                                    }
                                }
                                break;
                            }

                            case EXPRSXP:
                            case VECSXP: {
                                RAbstractVector list;
                                if (type == SEXPTYPE.EXPRSXP) {
                                    list = (RExpression) obj;
                                } else {
                                    list = (RList) obj;
                                }
                                stream.writeInt(list.getLength());
                                for (int i = 0; i < list.getLength(); i++) {
                                    Object listObj = list.getDataAtAsObject(i);
                                    writeItem(listObj);
                                }
                                break;
                            }

                            case RAWSXP: {
                                RRawVector raw = (RRawVector) obj;
                                byte[] data = raw.getDataWithoutCopying();
                                stream.writeInt(data.length);
                                stream.writeRaw(data);
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
                                if (type == SEXPTYPE.FUNSXP) {
                                    RFunction fun = (RFunction) obj;
                                    RSyntaxFunction body = (RSyntaxFunction) fun.getRootNode();
                                    ss = body.getLazySourceSection();
                                }
                                tailCall = true;

                                // attributes written first to avoid recursion on cdr
                                writeAttributes(attributes, ss);
                                if (attributes != null) {
                                    attributes = null;
                                }
                                if (ss != null) {
                                    ss = null;
                                }

                                switch (type) {
                                    case FUNSXP: {
                                        RFunction fun = (RFunction) obj;
                                        RPairList pl = (RPairList) serializeLanguageObject(state, fun);
                                        assert pl != null;
                                        state.convertUnboundValues(pl);
                                        if (FastROptions.debugMatches("printWclosure")) {
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
                                        state.convertUnboundValues(pl);
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
                                        state.convertUnboundValues(pl);
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

                        writeAttributes(attributes, ss);
                    }
                }
            } while (tailCall);
        }

        private static SourceSection getSourceSection(Object obj) {
            if (obj instanceof RSourceSectionNode) {
                return ((RSourceSectionNode) obj).getLazySourceSection();
            }
            return null;
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
            switch (SEXPTYPE.typeForClass(obj.getClass())) {
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
            if (s == RRuntime.STRING_NA) {
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

        private void writeAttributes(DynamicObject attributes, SourceSection ss) throws IOException {
            if (ss != null && ss != RSyntaxNode.LAZY_DEPARSE) {
                String path = ss.getSource().getURI().getPath();
                REnvironment createSrcfile = RSrcref.createSrcfile(path);
                Object createLloc = RSrcref.createLloc(ss);
                writePairListEntry(RRuntime.R_SRCREF, createLloc);
                writePairListEntry(RRuntime.R_SRCFILE, createSrcfile);
            }
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
            if (attributes != null || ss != null && ss != RSyntaxNode.LAZY_DEPARSE) {
                terminatePairList();
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
     * For {@code lazyLoadDBinsertValue}.
     */
    @TruffleBoundary
    public static byte[] serialize(Object obj, int type, int version, Object refhook) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Output output = new Output(out, type, version, (CallHook) refhook);
            output.serialize(obj);
            return out.toByteArray();
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @TruffleBoundary
    public static void serialize(RConnection conn, Object obj, int type, int version, Object refhook) throws IOException {
        Output output = new Output(conn.getOutputStream(), type, version, (CallHook) refhook);
        output.serialize(obj);
    }

    private static class Debug {
        private static int indent;
        private static PrintStream out;

        private static void printClosure(RPairList pl) {
            indent = 0;
            out = System.out;
            printObject(pl);
        }

        private static SEXPTYPE type(Object obj) {
            if (obj instanceof RPairList) {
                SEXPTYPE s = ((RPairList) obj).getType();
                return s == null ? SEXPTYPE.LISTSXP : s;
            } else {
                return SEXPTYPE.typeForClass(obj.getClass());
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
            for (int i = 0; i < indent * 2; i++) {
                out.write(' ');
            }
            out.printf(format, objects);
            out.write('\n');
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
                serializeArguments(arguments, element.getSyntaxSignature(), infixFieldAccess);
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

        private void serializeArguments(RSyntaxElement[] arguments, ArgumentsSignature signature, boolean infixFieldAccess) {
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
            state.setCdr(state.closePairList());
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
// setSrcrefs(element)
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
        if (obj instanceof RFunction) {
            return RSerialize.serializeFunction(state, (RFunction) obj);
        } else if (obj instanceof RLanguage) {
            return RSerialize.serializeLanguage(state, (RLanguage) obj);
        } else if (obj instanceof RPromise) {
            return RSerialize.serializePromise(state, (RPromise) obj);
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
        state.setCdr(visitor.visitFunctionBody(function));
    }

    private static Object serializeLanguage(State state, RLanguage lang) {
        RSyntaxElement element = lang.getRep().asRSyntaxNode();
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

    private static Object extractFromList(Object tag, SEXPTYPE expectedType) {
        SEXPTYPE type = SEXPTYPE.typeForClass(tag.getClass());
        if (type == expectedType) {
            return tag;
        } else if (type == SEXPTYPE.LISTSXP) {
            for (RPairList item : (RPairList) tag) {
                Object data = item.car();
                if (SEXPTYPE.typeForClass(data.getClass()) == expectedType) {
                    return data;
                }
            }
        }
        return null;
    }

    /**
     * A collection of static functions that will transform a pairlist into an AST using the
     * {@link RCodeBuilder}.
     */
    private static final class PairlistDeserializer {

        public static RFunction processFunction(Object car, Object cdr, Object tag, String functionName, String packageName) {
            // car == arguments, cdr == body, tag == PairList(attributes, environment)

            REnvironment environment = (REnvironment) tag;

            MaterializedFrame enclosingFrame = environment.getFrame();
            RootCallTarget callTarget = RContext.getASTBuilder().rootFunction(RSyntaxNode.LAZY_DEPARSE, processArguments(car), processBody(cdr), functionName);

            FrameSlotChangeMonitor.initializeEnclosingFrame(callTarget.getRootNode().getFrameDescriptor(), enclosingFrame);
            RFunction func = RDataFactory.createFunction(functionName, packageName, callTarget, null, enclosingFrame);

            RContext.getRRuntimeASTAccess().checkDebugRequest(func);

            /*
             * TODO: this is missing the code that registers sources with RPackageSource!
             */
            return func;
        }

        public static RLanguage processLanguage(Object car, Object cdr, Object tag) {
            return RDataFactory.createLanguage(processCall(car, cdr, tag).asRNode());
        }

        public static RPromise processPromise(Object car, Object cdr, Object tag) {
            // car == value, cdr == expression, tag == environment

            Closure closure = Closure.create(processBody(cdr).asRNode());
            if (car == RUnboundValue.instance) {
                REnvironment env = tag == RNull.instance ? REnvironment.baseEnv() : (REnvironment) tag;
                return RDataFactory.createPromise(PromiseState.Explicit, closure, env.getFrame());
            } else {
                return RDataFactory.createEvaluatedPromise(closure, car);
            }
        }

        private static RSyntaxNode process(Object value, boolean isCallLHS) {
            if (value instanceof RSymbol) {
                return RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, ((RSymbol) value).getName(), isCallLHS);
            } else if (value instanceof RPairList) {
                RPairList pl = (RPairList) value;
                switch (pl.getType()) {
                    case LANGSXP:
                        return processCall(pl.car(), pl.cdr(), pl.getTag());
                    case CLOSXP:
                        return processFunctionExpression(pl.car(), pl.cdr(), pl.getTag());
                    default:
                        throw RInternalError.shouldNotReachHere("unexpected SXP type: " + pl.getType());
                }
            } else {
                assert !(value instanceof RMissing) : "should be handled outside";
                assert !(value instanceof RLanguage) : "unexpected RLanguage constant in unserialize";

                return RContext.getASTBuilder().constant(RSyntaxNode.LAZY_DEPARSE, unwrapScalarValues(value));
            }
        }

        /** Convert single-element atomic vectors to their primitive counterparts. */
        private static Object unwrapScalarValues(Object value) {
            if (value instanceof RAbstractVector) {
                RAbstractVector vector = (RAbstractVector) value;
                if (vector.getLength() == 1 && (vector.getAttributes() == null || vector.getAttributes().isEmpty())) {
                    if (vector instanceof RAbstractDoubleVector || vector instanceof RAbstractIntVector || vector instanceof RAbstractStringVector ||
                                    vector instanceof RAbstractLogicalVector || vector instanceof RAbstractRawVector || vector instanceof RAbstractComplexVector) {
                        return vector.getDataAtAsObject(0);
                    }
                }
            }
            return value;
        }

        private static RSyntaxNode processCall(Object car, Object cdr, @SuppressWarnings("unused") Object tag) {
            if (car instanceof RSymbol && ((RSymbol) car).getName().equals("function")) {
                RPairList function = (RPairList) cdr;
                return processFunctionExpression(function.car(), function.cdr(), function.getTag());
            }
            return RContext.getASTBuilder().call(RSyntaxNode.LAZY_DEPARSE, process(car, true), processArguments(cdr));
        }

        private static RSyntaxNode processFunctionExpression(Object car, Object cdr, @SuppressWarnings("unused") Object tag) {
            // car == arguments, cdr == body
            return RContext.getASTBuilder().function(RSyntaxNode.LAZY_DEPARSE, processArguments(car), processBody(cdr), null);
        }

        private static List<RCodeBuilder.Argument<RSyntaxNode>> processArguments(Object args) {
            List<RCodeBuilder.Argument<RSyntaxNode>> list = new ArrayList<>();

            RPairList arglist = args instanceof RNull ? null : (RPairList) args;
            while (arglist != null) {
                // for each argument: tag == name, car == value
                String name = arglist.getTag() == RNull.instance ? null : ((RSymbol) arglist.getTag()).getName();
                RSyntaxNode value = arglist.car() == RMissing.instance ? null : process(arglist.car(), false);
                list.add(RCodeBuilder.argument(RSyntaxNode.LAZY_DEPARSE, name, value));
                arglist = next(arglist);
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
                        body = process(list.getDataAtAsObject(0), false);
                        break;
                    case LISTSXP:
                        assert pl.cdr() == RNull.instance || (pl.cadr() == RNull.instance && pl.cddr() == RNull.instance);
                        body = process(pl.car(), false);
                        break;
                    case LANGSXP:
                        body = processCall(pl.car(), pl.cdr(), pl.getTag());
                        break;
                    default:
                        throw RInternalError.shouldNotReachHere("unexpected SXP type in body: " + pl.getType());
                }
                handleSrcrefAttr(pl, body);
                return body;
            }
            return process(cdr, false);
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
            handleSrcrefAttr(func, (RSyntaxCall) elem);
        } else {
            Object srcref = func.getAttr(RRuntime.R_SRCREF);
            if (srcref instanceof RAbstractIntVector) {
                handleSrcrefAttr((RAbstractIntVector) srcref, null, elem);
            }
        }
    }

    /**
     * @param func Element carrying the {@value RRuntime#R_SRCREF} attribute.
     * @param elem The syntax element to create the source section for.
     */
    private static void handleSrcrefAttr(RAttributable func, RSyntaxCall elem) {
        Object srcref = func.getAttr(RRuntime.R_SRCREF);
        if (srcref instanceof RAbstractIntVector) {
            handleSrcrefAttr((RAbstractIntVector) srcref, null, elem);
        } else if (srcref instanceof RList) {
            try {
                Object srcfile = func.getAttr(RRuntime.R_SRCFILE);
                assert srcfile instanceof REnvironment;
                Source source = RSource.fromFile((REnvironment) srcfile);

                RList l = (RList) srcref;
                RSyntaxElement[] syntaxArguments = elem.getSyntaxArguments();
                assert syntaxArguments.length == l.getLength() - 1;

                for (int i = 0; i < l.getLength(); i++) {
                    Object dataAt = l.getDataAt(i);
                    assert dataAt instanceof RAbstractIntVector;
                    if (i == 0) {
                        handleSrcrefAttr((RAbstractIntVector) dataAt, source, elem);
                    } else {
                        handleSrcrefAttr((RAbstractIntVector) dataAt, source, syntaxArguments[i - 1]);
                    }
                }
            } catch (NoSuchFileException e) {
                RError.warning(RError.SHOW_CALLER, RError.Message.GENERIC, "Missing source file: " + e.getMessage());
            } catch (IOException e) {
                RError.warning(RError.SHOW_CALLER, RError.Message.GENERIC, "Cannot access source file: " + e.getMessage());
            }
        }
    }

    private static void handleSrcrefAttr(RAbstractIntVector srcrefVec, Source sharedSource, RSyntaxElement elem) {

        try {
            Source source;
            if (sharedSource != null) {
                source = sharedSource;
            } else {
                Object srcfile = srcrefVec.getAttr(RRuntime.R_SRCFILE);
                assert srcfile instanceof REnvironment;
                source = RSource.fromFile((REnvironment) srcfile);
            }
            int startLine = srcrefVec.getDataAt(0);
            int startColumn = srcrefVec.getDataAt(1);
            int startIdx = source.getLineStartOffset(startLine) + startColumn;
            int length = source.getLineStartOffset(srcrefVec.getDataAt(2)) + srcrefVec.getDataAt(3) - startIdx;
            SourceSection createSection = source.createSection(startLine, startColumn, length);
            elem.setSourceSection(createSection);
        } catch (NoSuchFileException e) {
            RError.warning(RError.SHOW_CALLER, RError.Message.GENERIC, "Missing source file: " + e.getMessage());
        } catch (IOException e) {
            RError.warning(RError.SHOW_CALLER, RError.Message.GENERIC, "Cannot access source file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            RError.warning(RError.SHOW_CALLER, RError.Message.GENERIC, "Invalid source reference: " + e.getMessage());
        }
    }
}
