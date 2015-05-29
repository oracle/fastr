/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.ffi;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.RErrorException;
import com.oracle.truffle.r.runtime.data.*;

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
 * So in the case of multiple {@link RContext}s package shared libraries may be registered muliple
 * times and we must take care not to duplicate them in the meta-data here ({@link #list}).
 *
 * logic derived from Rdynload.c
 */
public class DLL {

    /**
     * The list of loaded DLLs.
     */
    private static Deque<DLLInfo> list = new ConcurrentLinkedDeque<>();

    /**
     * Uniquely identifies the DLL (for use in an {@code externalptr}).
     */
    private static final AtomicInteger ID = new AtomicInteger();

    public static enum NativeSymbolType {
        C,
        Call,
        Fortran,
        External;

        public static final NativeSymbolType Any = null;
    }

    /**
     * Denotes info in registered native routines. GnuR has "subclasses" for C/Fortran, which is
     * TBD.
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

    public static class RegisteredNativeType {
        NativeSymbolType nst;
        DotSymbol dotSymbol;
        DLLInfo dllInfo;

        public RegisteredNativeType(NativeSymbolType nst, DotSymbol dotSymbol, DLLInfo dllInfo) {
            this.nst = nst;
            this.dotSymbol = dotSymbol;
            this.dllInfo = dllInfo;
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

        DLLInfo(String name, String path, boolean dynamicLookup, Object handle) {
            this.id = ID.getAndIncrement();
            this.name = name;
            this.path = path;
            this.dynamicLookup = dynamicLookup;
            this.handle = handle;
        }

        private void setNativeSymbols(int nstOrd, DotSymbol[] symbols) {
            nativeSymbols[nstOrd] = symbols;
        }

        public DotSymbol[] getNativeSymbols(NativeSymbolType nst) {
            return nativeSymbols[nst.ordinal()];
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
            dllInfo.setClassAttr(RDataFactory.createStringVectorFromScalar(DLLINFO_CLASS), false);
            return dllInfo;
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
         * Rdynload.c/crreateRSymbolObject.
         */
        public RList createRSymbolObject(RegisteredNativeType rnt, boolean withRegInfo) {
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
        RExternalPtr result = RDataFactory.createExternalPtr(value, RDataFactory.createSymbol(rClass.getDataAt(0)));
        result.setClassAttr(rClass, false);
        return result;
    }

    public static class DLLException extends RErrorException {
        private static final long serialVersionUID = 1L;

        DLLException(RError.Message msg, Object... args) {
            super(msg, args);
        }
    }

    private static final Semaphore available = new Semaphore(1, false);

    public static DLLInfo load(String path, boolean local, boolean now) throws DLLException {
        String absPath = Utils.tildeExpand(path);
        try {
            available.acquire();
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
                throw new DLLException(RError.Message.DLL_LOAD_ERROR, path, dlError);
            }
            String name = file.getName();
            int dx = name.lastIndexOf('.');
            if (dx > 0) {
                name = name.substring(0, dx);
            }
            DLLInfo result = new DLLInfo(name, absPath, true, handle);
            list.add(result);
            return result;
        } catch (InterruptedException ex) {
            throw RInternalError.shouldNotReachHere();
        } finally {
            available.release();
        }
    }

    private static final String R_INIT_PREFIX = "R_init_";

    public static DLLInfo loadPackageDLL(String path, boolean local, boolean now) throws DLLException {
        DLLInfo dllInfo = load(path, local, now);
        // Search for init method
        DLL.SymbolInfo symbolInfo = DLL.findSymbolInDLL(R_INIT_PREFIX + dllInfo.name, dllInfo);
        if (symbolInfo != null) {
            try {
                RFFIFactory.getRFFI().getCallRFFI().invokeVoidCall(symbolInfo, new Object[]{dllInfo});
            } catch (Throwable ex) {
                throw new DLLException(RError.Message.DLL_RINIT_ERROR);
            }
        }
        return dllInfo;
    }

    public static void unload(String path) throws DLLException {
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
     * Attempts to locate a symbol in the list of loaded libraries, possible constrained by the
     * {@code libName} argument.
     *
     * @param symbol the symbol name to search for
     * @param libName if not {@code null} or "", restrict search to this library.
     * @return a {@code SymbolInfo} instance or {@code null} if not found.
     */
    public static SymbolInfo findSymbolInfo(String symbol, String libName) {
        SymbolInfo symbolInfo = null;
        for (DLLInfo dllInfo : list) {
            if (libName == null || libName.length() == 0 || dllInfo.name.equals(libName)) {
                symbolInfo = findSymbolInDLL(symbol, dllInfo);
                if (symbolInfo != null) {
                    break;
                }
            }
        }
        return symbolInfo;
    }

    /**
     * Attempts to locate a symbol in the given library. This does no filtering on registered
     * symbols, it uses the OS level search of the library.
     */
    public static SymbolInfo findSymbolInDLL(String symbol, DLLInfo dllInfo) {
        boolean found = false;
        long val = RFFIFactory.getRFFI().getBaseRFFI().dlsym(dllInfo.handle, symbol);
        if (val != 0) {
            found = true;
        } else {
            // symbol might actually be zero
            if (RFFIFactory.getRFFI().getBaseRFFI().dlerror() == null) {
                found = true;
            }
        }
        if (found) {
            return new SymbolInfo(dllInfo, symbol, val);
        } else {
            return null;
        }
    }

    public static DLLInfo findLibraryContainingSymbol(String symbol) {
        SymbolInfo symbolInfo = findSymbolInfo(symbol, null);
        if (symbolInfo == null) {
            return null;
        } else {
            return symbolInfo.libInfo;
        }
    }

    /**
     * Similar to {@link #findSymbolInDLL(String, DLLInfo)} but restricts the search to those
     * symbols that have been registered from packages, i.e. that can be used in {@code .Call} etc.
     * functions.
     */
    public static SymbolInfo findRegisteredSymbolinInDLL(String symbol, String libName) {
        for (DLLInfo dllInfo : list) {
            if (libName == null || libName.length() == 0 || dllInfo.name.equals(libName)) {
                if (dllInfo.forceSymbols) {
                    continue;
                }
                for (NativeSymbolType nst : NativeSymbolType.values()) {
                    DotSymbol[] dotSymbols = dllInfo.getNativeSymbols(nst);
                    if (dotSymbols == null) {
                        continue;
                    }
                    for (DotSymbol dotSymbol : dotSymbols) {
                        if (dotSymbol.name.equals(symbol)) {
                            return new SymbolInfo(dllInfo, symbol, dotSymbol.fun);
                        }
                    }
                }

            }
        }
        return null;
    }

    // Methods called from native code during library loading.

    /**
     * Upcall from native to set the routines of type denoted by {@code nstOrd}.
     */
    public static void registerRoutines(DLLInfo dllInfo, int nstOrd, int num, long routines) {
        DotSymbol[] array = new DotSymbol[num];
        for (int i = 0; i < num; i++) {
            array[i] = setSymbol(nstOrd, routines, i);
        }
        dllInfo.setNativeSymbols(nstOrd, array);
    }

    /**
     * Upcxall from native to create a {@link DotSymbol} value.
     */
    public static DotSymbol setDotSymbolValues(String name, long fun, int numArgs) {
        return new DotSymbol(name, fun, numArgs);
    }

    private static native DotSymbol setSymbol(int nstOrd, long routines, int index);

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

    @SuppressWarnings("unused")
    public static void registerCCallable(String pkgName, String functionName, long address) {
        // TBD
    }

    @SuppressWarnings("unused")
    public static long getCCallable(String pkgName, String functionName) {
        // TBD
        throw RInternalError.unimplemented();
    }

}
