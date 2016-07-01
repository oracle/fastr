/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.ffi;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.RErrorException;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;

/**
 * Support for Dynamically Loaded Libraries.
 *
 * DLLs are loaded for several reasons in FastR:
 * <ol>
 * <li>support primitive operations</li>
 * <li>support the default packages</li>
 * <li>support the {@link CallRFFI}.
 * <li>support native code of dynamically loaded user packages</li>
 * </ol>
 *
 * In general, unloading a DLL may not be possible, so the set of DLLs have to be considered VM
 * wide, in the sense of multiple {@link RContext}s. TODO what about mutable state in native code?
 * So in the case of multiple {@link RContext}s package shared libraries may be registered multiple
 * times and we must take care not to duplicate them in the meta-data here ({@link #list}).
 *
 * Logic derived from Rdynload.c. For the most part we use the same type/function names as GnuR,
 * e.g. {@link NativeSymbolType}.
 */
public class DLL {

    public static final long SYMBOL_NOT_FOUND = -1;

    /**
     * The list of loaded DLLs.
     */
    private static Deque<DLLInfo> list = new ConcurrentLinkedDeque<>();

    /**
     * Uniquely identifies the DLL (for use in an {@code externalptr}).
     */
    private static final AtomicInteger ID = new AtomicInteger();

    public enum NativeSymbolType {
        C,
        Call,
        Fortran,
        External;

        public static final NativeSymbolType Any = null;
    }

    /**
     * Denotes info in registered native routines. GnuR has "subclasses" for C/Fortran, which is TBD
     * for FastR.
     */
    public static class DotSymbol {
        public final String name;
        public final long fun;
        public final int numArgs;

        public DotSymbol(String name, long fun, int numArgs) {
            this.name = name;
            this.fun = fun;
            this.numArgs = numArgs;
        }
    }

    public static class RegisteredNativeSymbol {
        private NativeSymbolType nst;
        private DotSymbol dotSymbol; // a union in GnuR
        private DLLInfo dllInfo;

        public RegisteredNativeSymbol(NativeSymbolType nst, DotSymbol dotSymbol, DLLInfo dllInfo) {
            this.nst = nst;
            this.dotSymbol = dotSymbol;
            this.dllInfo = dllInfo;
        }

        public DLLInfo getDllInfo() {
            return dllInfo;
        }

        public static RegisteredNativeSymbol any() {
            return new RegisteredNativeSymbol(NativeSymbolType.Any, null, null);
        }

    }

    public static class DLLInfo {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{"name", "path", "dynamicLookup", "handle", "info"}, RDataFactory.COMPLETE_VECTOR);
        public static final String DLL_INFO_REFERENCE = "DLLInfoReference";
        private static final RStringVector INFO_REFERENCE_CLASS = RDataFactory.createStringVectorFromScalar(DLL_INFO_REFERENCE);
        private static final RStringVector HANDLE_CLASS = RDataFactory.createStringVectorFromScalar("DLLHandle");
        private static final String DLLINFO_CLASS = "DLLInfo";

        private final int id;
        public final String name;
        public final String path;
        public final Object handle;
        private boolean dynamicLookup;
        private boolean forceSymbols;
        private DotSymbol[][] nativeSymbols = new DotSymbol[NativeSymbolType.values().length][];

        private DLLInfo(String name, String path, boolean dynamicLookup, Object handle) {
            this.id = ID.getAndIncrement();
            this.name = name;
            this.path = path;
            this.dynamicLookup = dynamicLookup;
            this.handle = handle;
        }

        private static synchronized DLLInfo create(String name, String path, boolean dynamicLookup, Object handle) {
            DLLInfo result = new DLLInfo(name, path, dynamicLookup, handle);
            list.add(result);
            return result;
        }

        public void setNativeSymbols(int nstOrd, DotSymbol[] symbols) {
            nativeSymbols[nstOrd] = symbols;
        }

        public DotSymbol[] getNativeSymbols(NativeSymbolType nst) {
            if (nst == null) {
                int totalLen = 0;
                for (NativeSymbolType nstx : NativeSymbolType.values()) {
                    DotSymbol[] d = nativeSymbols[nstx.ordinal()];
                    if (d != null) {
                        totalLen += d.length;
                    }
                }
                if (totalLen == 0) {
                    return null;
                }
                DotSymbol[] result = new DotSymbol[totalLen];
                int ix = 0;
                for (NativeSymbolType nstx : NativeSymbolType.values()) {
                    DotSymbol[] d = nativeSymbols[nstx.ordinal()];
                    if (d != null) {
                        System.arraycopy(nativeSymbols[nstx.ordinal()], 0, result, ix, d.length);
                        ix += d.length;
                    }
                }
                return result;
            } else {
                return nativeSymbols[nst.ordinal()];
            }
        }

        /**
         * Return array of values that can be plugged directly into an {@code RList}.
         */
        public RList toRList() {
            Object[] data = new Object[NAMES.getLength()];
            data[0] = name;
            data[1] = path;
            data[2] = RRuntime.asLogical(dynamicLookup);
            data[3] = createExternalPtr(System.identityHashCode(handle), HANDLE_CLASS);
            /*
             * GnuR sets the info member to an externalptr whose value is the DllInfo structure
             * itself. We can't do that, but we need a way to get back to it from R code that uses
             * the value, e.g. getRegisteredRoutines. So we use the id value.
             */
            data[4] = createExternalPtr(id, INFO_REFERENCE_CLASS);
            RList dllInfo = RDataFactory.createList(data, DLLInfo.NAMES);
            dllInfo.setClassAttr(RDataFactory.createStringVectorFromScalar(DLLINFO_CLASS));
            return dllInfo;
        }

        @Override
        public String toString() {
            return String.format("name: %s, path: %s, dynamicLookup: %b, forceSymbols %b", name, path, dynamicLookup, forceSymbols);
        }
    }

    public static class SymbolInfo {
        public final DLLInfo libInfo;
        public final String symbol;
        public final long address;

        public SymbolInfo(DLLInfo libInfo, String symbol, long address) {
            this.libInfo = libInfo;
            this.symbol = symbol;
            this.address = address;
        }

        private static final String[] NAMES_3 = new String[]{"name", "address", "dll"};
        private static final String[] NAMES_4 = new String[]{NAMES_3[0], NAMES_3[1], NAMES_3[2], "numParameters"};
        private static final RStringVector NAMES_3_VEC = RDataFactory.createStringVector(NAMES_3, RDataFactory.COMPLETE_VECTOR);
        private static final RStringVector NAMES_4_VEC = RDataFactory.createStringVector(NAMES_4, RDataFactory.COMPLETE_VECTOR);
        private static final String NATIVE_SYMBOL_INFO_CLASS = "NativeSymbolInfo";
        private static final RStringVector NATIVE_SYMBOL_CLASS = RDataFactory.createStringVectorFromScalar("NativeSymbol");
        private static final RStringVector REGISTERED_NATIVE_SYMBOL_CLASS = RDataFactory.createStringVectorFromScalar("RegisteredNativeSymbol");

        /**
         * Method to create the R object representing symbol info. From
         * Rdynload.c/createRSymbolObject.
         */
        public RList createRSymbolObject(RegisteredNativeSymbol rnt, boolean withRegInfo) {
            int n = rnt.nst == NativeSymbolType.Any ? 3 : 4;
            String sname = symbol == null ? rnt.dotSymbol.name : symbol;
            String[] klass = new String[rnt.nst == NativeSymbolType.Any ? 1 : 2];
            klass[klass.length - 1] = NATIVE_SYMBOL_INFO_CLASS;
            Object[] data = new Object[n];
            data[0] = sname;
            if (withRegInfo && rnt.nst != NativeSymbolType.Any) {
                /*
                 * GnuR stores this as an externalptr whose value is the C RegisteredNativeType
                 * struct. We can't do that, and it's not clear any code uses that fact, so we
                 * stored the registered address.
                 */
                data[1] = DLL.createExternalPtr(rnt.dotSymbol.fun, REGISTERED_NATIVE_SYMBOL_CLASS);
            } else {
                data[1] = DLL.createExternalPtr(address, NATIVE_SYMBOL_CLASS);
            }
            data[2] = libInfo.toRList();
            if (n > 3) {
                data[3] = rnt.dotSymbol.numArgs;
                klass[0] = rnt.nst.name() + "Routine";
            }
            return RDataFactory.createList(data, n > 3 ? NAMES_4_VEC : NAMES_3_VEC);
        }
    }

    public static DLLInfo getDLLInfoForId(int id) {
        for (DLLInfo dllInfo : list) {
            if (dllInfo.id == id) {
                return dllInfo;
            }
        }
        return null;
    }

    public static boolean isDLLInfo(RExternalPtr info) {
        RSymbol tag = (RSymbol) info.getTag();
        return tag.getName().equals(DLLInfo.DLL_INFO_REFERENCE);
    }

    public static RExternalPtr createExternalPtr(long value, RStringVector rClass) {
        CompilerAsserts.neverPartOfCompilation(); // for interning
        RExternalPtr result = RDataFactory.createExternalPtr(value, RDataFactory.createSymbolInterned(rClass.getDataAt(0)));
        result.setClassAttr(rClass);
        return result;
    }

    public static class DLLException extends RErrorException {
        private static final long serialVersionUID = 1L;

        DLLException(RError.Message msg, Object... args) {
            super(msg, args);
        }
    }

    /*
     * There is no sense in throwing an RError if we fail to load/init a (default) package during
     * initial context initialization, as it is essentially fatal for any of the standard packages
     * and indicates a bug in the RFFI implementation. So we call Utils.fatalError instead. When the
     * system is stable, we can undo this, so that errors loading (user) packages added to
     * R_DEFAULT_PACKAGES do throw RErrors.
     */

    @TruffleBoundary
    public static synchronized DLLInfo load(String path, boolean local, boolean now) throws DLLException {
        String absPath = Utils.tildeExpand(path);
        for (DLLInfo dllInfo : list) {
            if (dllInfo.path.equals(absPath)) {
                // already loaded
                return dllInfo;
            }
        }
        File file = new File(absPath);
        Object handle = RFFIFactory.getRFFI().getBaseRFFI().dlopen(absPath, local, now);
        if (handle == null) {
            String dlError = RFFIFactory.getRFFI().getBaseRFFI().dlerror();
            if (RContext.isInitialContextInitialized()) {
                throw new DLLException(RError.Message.DLL_LOAD_ERROR, path, dlError);
            } else {
                throw Utils.rSuicide("error loading default package: " + path + "\n" + dlError);
            }
        }
        String name = file.getName();
        int dx = name.lastIndexOf('.');
        if (dx > 0) {
            name = name.substring(0, dx);
        }
        return DLLInfo.create(name, absPath, true, handle);
    }

    private static final String R_INIT_PREFIX = "R_init_";

    @TruffleBoundary
    public static DLLInfo loadPackageDLL(String path, boolean local, boolean now) throws DLLException {
        DLLInfo dllInfo = load(path, local, now);
        // Search for init method
        String pkgInit = R_INIT_PREFIX + dllInfo.name;
        long initFunc = RFFIFactory.getRFFI().getBaseRFFI().dlsym(dllInfo.handle, pkgInit);
        if (initFunc != 0) {
            synchronized (DLL.class) {
                try {
                    RFFIFactory.getRFFI().getCallRFFI().invokeVoidCall(initFunc, pkgInit, new Object[]{dllInfo});
                } catch (ReturnException ex) {
                    // An error call can, due to condition handling, throw this which we must
                    // propogate
                    throw ex;
                } catch (Throwable ex) {
                    if (RContext.isInitialContextInitialized()) {
                        throw new DLLException(RError.Message.DLL_RINIT_ERROR);
                    } else {
                        throw Utils.rSuicide(RError.Message.DLL_RINIT_ERROR.message + " on default package: " + path);
                    }
                }
            }
        }
        return dllInfo;
    }

    @TruffleBoundary
    public static synchronized void unload(String path) throws DLLException {
        String absPath = Utils.tildeExpand(path);
        for (DLLInfo info : list) {
            if (info.path.equals(absPath)) {
                int rc = RFFIFactory.getRFFI().getBaseRFFI().dlclose(info.handle);
                if (rc != 0) {
                    throw new DLLException(RError.Message.DLL_LOAD_ERROR, path, "");
                }
                return;
            }
        }
        throw new DLLException(RError.Message.DLL_NOT_LOADED, path);
    }

    public static ArrayList<DLLInfo> getLoadedDLLs() {
        ArrayList<DLLInfo> result = new ArrayList<>();
        for (DLLInfo dllInfo : list) {
            result.add(dllInfo);
        }
        return result;
    }

    /**
     * Directly analogous to the GnuR function of same name. If {@code rns} is not {@code null} and
     * the search is successful, it is updated.
     *
     * @param dllInfo dll to search in
     * @param name name of symbol to lookup
     * @param rns if not {@code null} may limit the search to a specific {@link NativeSymbolType}
     * @return the address of the (function) symbol or {@code 0} if not found.
     */
    public static long getDLLRegisteredSymbol(DLLInfo dllInfo, String name, RegisteredNativeSymbol rns) {
        NativeSymbolType rnsNst = rns == null ? NativeSymbolType.Any : rns.nst;
        for (NativeSymbolType nst : NativeSymbolType.values()) {
            if (rnsNst == NativeSymbolType.Any || rnsNst == nst) {
                DotSymbol[] dotSymbols = dllInfo.getNativeSymbols(nst);
                if (dotSymbols == null) {
                    continue;
                }
                for (DotSymbol dotSymbol : dotSymbols) {
                    if (dotSymbol.name.equals(name)) {
                        if (rns != null) {
                            rns.nst = nst;
                            rns.dotSymbol = dotSymbol;
                            rns.dllInfo = dllInfo;
                        }
                        return dotSymbol.fun;
                    }
                }
            }
        }
        return SYMBOL_NOT_FOUND;
    }

    /**
     * Directly analogous to the GnuR function {@code R_dlsym}. Checks first for a
     * {@link RegisteredNativeSymbol} using {@code rns}, then, unless dynamic lookup has been
     * disabled, looks up the symbol using the {@code dlopen} machinery.
     */
    @TruffleBoundary
    public static long dlsym(DLLInfo dllInfo, String name, RegisteredNativeSymbol rns) {
        long f = getDLLRegisteredSymbol(dllInfo, name, rns);
        if (f != -1) {
            return f;
        }

        if (!dllInfo.dynamicLookup) {
            return SYMBOL_NOT_FOUND;
        }

        String mName = name;
        // assume Fortran underscore, although GnuR has cc code for this
        if (rns != null && rns.nst == NativeSymbolType.Fortran) {
            mName = name + "_";
        }
        f = RFFIFactory.getRFFI().getBaseRFFI().dlsym(dllInfo.handle, mName);
        if (f != 0) {
            return f;
        } else {
            // symbol might actually be zero
            if (RFFIFactory.getRFFI().getBaseRFFI().dlerror() == null) {
                return f;
            } else {
                return SYMBOL_NOT_FOUND;
            }
        }
    }

    /**
     * Directly analogous to the GnuR function {@code R_FindSymbol}.
     *
     * @param name name of symbol (as appears in code) to look up
     * @param libName name of library to restrict search to (or all if {@code null} or empty string)
     * @param rns {@code rns.nst} encodes the type of native symbol to restrict search to (or all if
     *            {@code null})
     */
    @TruffleBoundary
    public static synchronized long findSymbol(String name, String libName, RegisteredNativeSymbol rns) {
        boolean all = libName == null || libName.length() == 0;
        for (DLLInfo dllInfo : list) {
            if (dllInfo.forceSymbols) {
                continue;
            }
            if (all || dllInfo.name.equals(libName)) {
                long func = dlsym(dllInfo, name, rns);
                if (func != SYMBOL_NOT_FOUND) {
                    if (rns != null) {
                        rns.dllInfo = dllInfo;
                    }
                    return func;
                }
            }
            if (!all && dllInfo.name.equals(libName)) {
                return SYMBOL_NOT_FOUND;
            }
        }
        return SYMBOL_NOT_FOUND;
    }

    public static synchronized DLLInfo findLibrary(String name) {
        for (DLLInfo dllInfo : list) {
            if (dllInfo.name.equals(name)) {
                return dllInfo;
            }
        }
        return null;
    }

    @TruffleBoundary
    public static DLLInfo findLibraryContainingSymbol(String symbol) {
        RegisteredNativeSymbol rns = RegisteredNativeSymbol.any();
        long func = findSymbol(symbol, null, rns);
        if (func == SYMBOL_NOT_FOUND) {
            return null;
        } else {
            return rns.dllInfo;
        }
    }

    /*
     * Methods called from native code during library loading. These methods are single threaded by
     * virtue of the Semaphore in loadPackageDLL.
     */

    public static int useDynamicSymbols(DLLInfo dllInfo, int value) {
        int old = dllInfo.dynamicLookup ? 1 : 0;
        dllInfo.dynamicLookup = value == 0 ? false : true;
        return old;
    }

    public static int forceSymbols(DLLInfo dllInfo, int value) {
        int old = dllInfo.forceSymbols ? 1 : 0;
        dllInfo.forceSymbols = value == 0 ? false : true;
        return old;
    }

    private static final String EMBEDDING = "(embedding)";

    public static DLLInfo getEmbeddingDLLInfo() {
        DLLInfo result = findLibrary(EMBEDDING);
        if (result == null) {
            result = DLLInfo.create(EMBEDDING, EMBEDDING, false, null);
        }
        return result;
    }
}
