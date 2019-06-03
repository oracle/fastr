/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime.ffi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RError.RErrorException;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSuicide;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.CustomNativeMirror;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RObject;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.data.StringArrayWrapper;
import com.oracle.truffle.r.runtime.ffi.CallRFFI.InvokeVoidCallNode;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI.DLCloseRootNode;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI.LibHandle;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.rng.user.UserRNG;

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
 * Logic derived from Rdynload.c. For the most part we use the same type/function names as GnuR,
 * e.g. {@link NativeSymbolType}.
 *
 * Abstractly every {@link RContext} has its own unique list of loaded libraries, stored in the
 * {@link ContextStateImpl} class. Concretely, an implementation of the {@link DLLRFFI} may or may
 * not maintain separate instances.
 *
 * The {@code libR} library is a special case, as it is an implementation artifact and not visible
 * at the R level. However, it is convenient to manage it in a similar way in this code. It is
 * always stored in slot 0 of the list, and is hidden from R code. It is loaded by {@link #loadLibR}
 * , which should only be called once.
 *
 * As far as possible, all execution is via Truffle {@link Node} classes as, in most cases, the
 * invocation is from an existing AST node.
 */
public class DLL {

    public static class ContextStateImpl implements RContext.ContextState {
        private ArrayList<DLLInfo> list;
        private RContext context;
        private static DLLInfo libRdllInfo;

        public static ContextStateImpl newContextState() {
            return new ContextStateImpl();
        }

        @Override
        public ContextState initialize(RContext contextArg) {
            this.context = contextArg;
            if (isShareDLLKind(context.getKind())) {
                list = context.getParent().stateDLL.list;
            } else {
                list = new ArrayList<>();
                if (!context.isInitial()) {
                    assert list.isEmpty();
                    list.add(libRdllInfo);
                }
            }
            return this;
        }

        @Override
        public void beforeDispose(RContext contextArg) {
            // Note: the first entry should be RLib
            if (!isShareDLLKind(context.getKind()) && list.size() > 1) {
                RootCallTarget closeCallTarget = DLCloseRootNode.create(contextArg);
                for (int i = 1; i < list.size(); i++) {
                    DLLInfo dllInfo = list.get(i);
                    if (!dllInfo.isSynthetic()) {
                        closeCallTarget.call(dllInfo.handle);
                    }
                }
            }
            list = null;
        }

        private static boolean isShareDLLKind(RContext.ContextKind kind) {
            return kind == ContextKind.SHARE_PARENT_RW || kind == ContextKind.SHARE_ALL;
        }

        private void addLibR(DLLInfo dllInfo) {
            assert list.isEmpty();
            list.add(dllInfo);
            libRdllInfo = dllInfo;
        }
    }

    /**
     * The list of loaded DLLs.
     */

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
    public static class DotSymbol extends RObject implements RTruffleObject {
        public final String name;
        public final SymbolHandle fun;
        public final int numArgs;

        public DotSymbol(String name, SymbolHandle fun, int numArgs) {
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

    public static final class DLLInfo extends RObject implements RTruffleObject, CustomNativeMirror {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{"name", "path", "dynamicLookup", "handle", "info"}, RDataFactory.COMPLETE_VECTOR);
        public static final String DLL_INFO_REFERENCE = "DLLInfoReference";
        private static final RStringVector INFO_REFERENCE_CLASS = RDataFactory.createStringVectorFromScalar(DLL_INFO_REFERENCE);
        private static final RStringVector HANDLE_CLASS = RDataFactory.createStringVectorFromScalar("DLLHandle");
        private static final String DLLINFO_CLASS = "DLLInfo";

        private final int id;

        public final String name;
        public final String path;
        /**
         * The CharSXPWrapper fields maintain the wrapped strings that are returned as a response to
         * the READ message sent to this Truffle object. See {@code DLLInfoMR}.
         */
        public final CharSXPWrapper nameSXP;
        public final CharSXPWrapper pathSXP;

        public final LibHandle handle;
        private boolean dynamicLookup;
        private boolean forceSymbols;
        private final DotSymbol[][] nativeSymbols = new DotSymbol[NativeSymbolType.values().length][];
        private static Map<String, ArrayList<CEntry>> cEntryTable = new HashMap<>();
        private final HashSet<String> unsuccessfulLookups = new HashSet<>();
        /**
         * A synthetic DLLInfo faking {@link RFunction}-s as if they were real native symbols to
         * .Call etc.
         */
        private final boolean syntheticHandle;

        private DLLInfo(String name, String path, boolean dynamicLookup, LibHandle handle, boolean syntheticHandle) {
            this.id = ID.getAndIncrement();
            this.name = name;
            this.nameSXP = CharSXPWrapper.create(name);
            this.path = path;
            this.pathSXP = CharSXPWrapper.create(path);
            this.dynamicLookup = dynamicLookup;
            this.handle = handle;
            this.syntheticHandle = syntheticHandle;
        }

        private static DLLInfo create(String name, String path, boolean dynamicLookup, LibHandle handle, boolean addToList) {
            return create(name, path, dynamicLookup, handle, addToList, false);
        }

        private static DLLInfo create(String name, String path, boolean dynamicLookup, LibHandle handle, boolean addToList, boolean syntheticHandle) {
            DLLInfo result = new DLLInfo(name, path, dynamicLookup, handle, syntheticHandle);
            if (addToList) {
                ContextStateImpl contextState = getContextState();
                contextState.list.add(result);
            }
            return result;
        }

        public DLLInfo replaceHandle(LibHandle newHandle) {
            return new DLLInfo(name, path, dynamicLookup, newHandle, syntheticHandle);
        }

        /**
         * Embedding {@link DLLInfo} is just a placeholder. It does not represent any concrete dll
         * and thus we e.g. cannot find any symbols in it.
         */
        public boolean isEmbeddingDllInfo() {
            return handle == null;
        }

        /**
         * Determines whether this is a synthetic {@link DLLInfo} faking {@link RFunction}-s as if
         * they were real native symbols.
         */
        private boolean isSynthetic() {
            return syntheticHandle;
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

        public static synchronized void registerCEntry(String pkgName, CEntry entry) {
            ArrayList<CEntry> pEnv = cEntryTable.get(pkgName);
            if (pEnv == null) {
                pEnv = new ArrayList<>();
                cEntryTable.put(pkgName, pEnv);
            }
            pEnv.add(entry);
        }

        public static synchronized CEntry lookupCEntry(String pkgName, String symbol) {
            ArrayList<CEntry> pEnv = cEntryTable.get(pkgName);
            if (pEnv != null) {
                for (CEntry entry : pEnv) {
                    if (entry.symbol.equals(symbol)) {
                        return entry;
                    }
                }
            }
            return null;
        }

        /**
         * Return array of values that can be plugged directly into an {@code RList}.
         */
        @TruffleBoundary
        public RList toRList() {
            Object[] data = new Object[NAMES.getLength()];
            data[0] = name;
            data[1] = path;
            data[2] = RRuntime.asLogical(dynamicLookup);
            data[3] = createExternalPtr(new SymbolHandle(new Long(System.identityHashCode(handle))), HANDLE_CLASS, handle);
            /*
             * GnuR sets the info member to an externalptr whose value is the C DllInfo structure
             * itself. We use the internal "externalObject" slot instead, and we use the "id" for
             * the "addr" slot.
             */
            data[4] = createExternalPtr(new SymbolHandle(new Long(id)), INFO_REFERENCE_CLASS, this);
            RList result = RDataFactory.createList(data, DLLInfo.NAMES);
            result.setClassAttr(RDataFactory.createStringVectorFromScalar(DLLINFO_CLASS));
            return result;
        }

        @Override
        public String toString() {
            return String.format("name: %s, path: %s, dynamicLookup: %b, forceSymbols %b", name, path, dynamicLookup, forceSymbols);
        }

        @Override
        public long getCustomMirrorAddress() {
            RStringVector table = RDataFactory.createStringVector(new String[]{path, name}, true);
            return new StringArrayWrapper(table).asPointer();
        }
    }

    public static final class SymbolInfo {
        public final DLLInfo libInfo;
        public final String symbol;
        public final SymbolHandle address;

        public SymbolInfo(DLLInfo libInfo, String symbol, SymbolHandle address) {
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
                 * stored the registered address. TODO use externalObject slot?
                 */
                data[1] = DLL.createExternalPtr(rnt.dotSymbol.fun, REGISTERED_NATIVE_SYMBOL_CLASS, null);
            } else {
                data[1] = DLL.createExternalPtr(address, NATIVE_SYMBOL_CLASS, null);
            }
            data[2] = libInfo.toRList();
            if (n > 3) {
                data[3] = rnt.dotSymbol.numArgs;
                klass[0] = rnt.nst.name() + "Routine";
            }
            RList result = RDataFactory.createList(data, n > 3 ? NAMES_4_VEC : NAMES_3_VEC);
            result.setClassAttr(RDataFactory.createStringVector(klass, RDataFactory.COMPLETE_VECTOR));
            return result;
        }
    }

    /**
     * R has an interface for exporting and importing functions between packages' native code. The
     * functions have to be exported, i.e. registered in a directory, called CEntry table in GNU R.
     * Another package can they as the directory for address or a function with specified name.
     */
    public static final class CEntry {
        public final String symbol;
        public final SymbolHandle address;

        public CEntry(String symbol, SymbolHandle address) {
            this.symbol = symbol;
            this.address = address;
        }
    }

    /**
     * Abstracts the way that DLL function symbols are represented, either as a machine address (
     * {@link Long}) or a {@link TruffleObject}. At the present time, both forms can exists within a
     * single VM, so the class is defined as a "union" for simplicity.
     *
     * N.B. It is explicitly allowed to register a {@code null} value as the base package registers
     * some (Fortran) functions that are implemented in Java but have a bogus (zero) native symbol
     * definition. Any use of {@code null} is failed
     */
    public static final class SymbolHandle {
        public final Object value;

        public SymbolHandle(Object value) {
            assert value == null || value instanceof Long || value instanceof TruffleObject;
            this.value = value;
        }

        public long asAddress() {
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof TruffleObject) {
                return asAddressTO((TruffleObject) value);
            } else {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @TruffleBoundary
        private static long asAddressTO(TruffleObject val) {
            try {
                return InteropLibrary.getFactory().getUncached().asPointer(val);
            } catch (UnsupportedMessageException ex) {
                // Let it flow to throw RInternalError
            }
            throw RInternalError.shouldNotReachHere();
        }

        public boolean isAddress() {
            if (value instanceof Long) {
                return true;
            } else if (value instanceof TruffleObject) {
                return isAddressTO((TruffleObject) value);
            } else {
                return false;
            }
        }

        @TruffleBoundary
        private static boolean isAddressTO(TruffleObject val) {
            return InteropLibrary.getFactory().getUncached().isPointer(val);
        }

        public boolean isLong() {
            return value instanceof Long;
        }

        public TruffleObject asTruffleObject() {
            if (value instanceof TruffleObject) {
                return (TruffleObject) value;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("value: " + value);
            }
        }
    }

    public static final SymbolHandle SYMBOL_NOT_FOUND = null;

    private static ContextStateImpl getContextState() {
        return RContext.getInstance().stateDLL;
    }

    public static boolean isDLLInfo(RExternalPtr info) {
        RSymbol tag = (RSymbol) info.getTag();
        return tag.getName().equals(DLLInfo.DLL_INFO_REFERENCE);
    }

    @TruffleBoundary
    public static RExternalPtr createExternalPtr(SymbolHandle value, RStringVector rClass, Object externalObject) {
        CompilerAsserts.neverPartOfCompilation(); // for interning
        RExternalPtr result = RDataFactory.createExternalPtr(value, externalObject, RDataFactory.createSymbolInterned(rClass.getDataAt(0)), RNull.instance);
        result.setClassAttr(rClass);
        return result;
    }

    public static class DLLException extends RErrorException {
        private static final long serialVersionUID = 1L;

        public DLLException(Throwable cause, RError.Message msg, Object... args) {
            super(cause, msg, args);
        }
    }

    /**
     * Loads a the {@code libR} library. This is an implementation specific library.
     */
    public static void loadLibR(RContext context, String path) {
        LibHandle handle = null;
        try {
            handle = (LibHandle) DLLRFFI.DLOpenRootNode.create(context).call(path, false, false);
        } catch (UnsatisfiedLinkError ex) {
            throw RSuicide.rSuicide(context, "error loading libR from: " + path + ".\n" +
                            "If running on the NFI backend, did you provide the location of libtrufflenfi.so as the value of the system " +
                            "property 'truffle.nfi.library'?\nThe current value is '" + System.getProperty("truffle.nfi.library") + "'.\n" +
                            "Is the OpenMP runtime library (libgomp.so) present on your system? This library is, e.g., typically part of the GCC package.\n" +
                            "Details: " + ex.getMessage());
        } catch (Throwable ex) {
            throw RSuicide.rSuicide(context, "error loading libR from: " + path + ". Details: " + ex.getMessage());
        }
        if (handle == null) {
            throw RSuicide.rSuicide(context, "error loading libR from: " + path + "\n");
        }
        ContextStateImpl dllContext = context.stateDLL;
        dllContext.addLibR(DLLInfo.create(libName(path), path, true, handle, false));
    }

    private static final class SynthLibHandle implements LibHandle {

        @Override
        public Type getRFFIType() {
            return null;
        }

    }

    public static DLLInfo createSyntheticLib(RContext context, String library) {
        DLLInfo dllInfo = DLLInfo.create(library, library, true, new SynthLibHandle(), false, true);
        context.stateDLL.list.add(dllInfo);
        return dllInfo;
    }

    public static String libName(String absPath) {
        TruffleFile file = RContext.getInstance().getEnv().getTruffleFile(absPath);
        String name = file.getName();
        int dx = name.lastIndexOf('.');
        if (dx > 0) {
            name = name.substring(0, dx);
        }
        return name;
    }

    public static final String R_INIT_PREFIX = "R_init_";

    public static final class LoadPackageDLLNode extends Node {
        @Child private InvokeVoidCallNode invokeVoidCallNode;
        @Child private DLLRFFI.DLSymNode dlSymNode = RFFIFactory.getDLLRFFI().createDLSymNode();
        @Child private DLLRFFI.DLOpenNode dlOpenNode = RFFIFactory.getDLLRFFI().createDLOpenNode();

        public static LoadPackageDLLNode create() {
            return new LoadPackageDLLNode();
        }

        @TruffleBoundary
        public DLLInfo execute(String path, boolean local, boolean now) throws DLLException {
            String absPath = Utils.tildeExpand(path);
            ContextStateImpl contextState = getContextState();
            for (DLLInfo dllInfo : contextState.list) {
                if (dllInfo.path.equals(absPath)) {
                    // already loaded
                    return dllInfo;
                }
            }
            DLLInfo dllInfo = doLoad(absPath, local, now, true);

            // Search for an init method
            String pkgInit = R_INIT_PREFIX + dllInfo.name;
            try {
                SymbolHandle initFunc = dlSymNode.execute(dllInfo.handle, pkgInit);
                try {
                    if (invokeVoidCallNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        invokeVoidCallNode = (InvokeVoidCallNode) insert((Node) RFFIFactory.getCallRFFI().createInvokeVoidCallNode());
                    }
                    invokeVoidCallNode.dispatch(null, new NativeCallInfo(pkgInit, initFunc, dllInfo), new Object[]{dllInfo});
                } catch (ReturnException ex) {
                    // An error call can, due to condition handling, throw this which we must
                    // propagate
                    throw ex;
                } catch (Throwable ex) {
                    if (RContext.isInitialContextInitialized()) {
                        throw new DLLException(ex, RError.Message.DLL_RINIT_ERROR);
                    } else {
                        throw RSuicide.rSuicide(RContext.getInstance(), ex, RError.Message.DLL_RINIT_ERROR.message + " on default package: " + path);
                    }
                }
            } catch (UnsatisfiedLinkError ex) {
                // no such symbol, that's ok
            }
            return dllInfo;
        }

        /**
         * There is no sense in throwing an RError if we fail to load/init a (default) package
         * during initial context initialization, as it is essentially fatal for any of the standard
         * packages and likely indicates a bug in the RFFI implementation. So we call
         * {@link RSuicide#rSuicide(String)} instead. When the system is stable, we can undo this,
         * so that errors loading (user) packages added to R_DEFAULT_PACKAGES do throw RErrors.
         */
        private synchronized DLLInfo doLoad(String absPath, boolean local, boolean now, boolean addToList) throws DLLException {
            try {
                LibHandle handle = dlOpenNode.execute(absPath, local, now);
                return DLLInfo.create(libName(absPath), absPath, true, handle, addToList);
            } catch (UnsatisfiedLinkError ex) {
                String dlError = ex.getMessage();
                if (RContext.isInitialContextInitialized()) {
                    throw new DLLException(ex, RError.Message.DLL_LOAD_ERROR, absPath, dlError);
                } else {
                    throw RSuicide.rSuicide(RContext.getInstance(), ex, "error loading default package: " + absPath + "\n" + dlError);
                }
            }
        }
    }

    public static class UnloadNode extends Node {
        @Child private DLLRFFI.DLCloseNode dlCloseNode = RFFIFactory.getDLLRFFI().createDLCloseNode();

        @TruffleBoundary
        public void execute(String path) throws DLLException {
            String absPath = Utils.tildeExpand(path);
            ContextStateImpl contextState = getContextState();
            for (DLLInfo info : contextState.list) {
                if (info.path.equals(absPath)) {
                    int rc = dlCloseNode.execute(info.handle);
                    if (rc != 0) {
                        throw new DLLException(null, RError.Message.DLL_LOAD_ERROR, path, "");
                    }
                    contextState.list.remove(info);
                    return;
                }
            }
            throw new DLLException(null, RError.Message.DLL_NOT_LOADED, path);
        }

        public static UnloadNode create() {
            return new UnloadNode();
        }
    }

    /**
     * Returns the list of loaded DLLs in the current context.
     */
    public static ArrayList<DLLInfo> getLoadedDLLs() {
        ArrayList<DLLInfo> result = new ArrayList<>();
        ContextStateImpl contextState = getContextState();
        // skip first entry (libR)
        for (int i = 1; i < contextState.list.size(); i++) {
            DLLInfo dllInfo = contextState.list.get(i);
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
    public static SymbolHandle getDLLRegisteredSymbol(DLLInfo dllInfo, String name, RegisteredNativeSymbol rns) {
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

    public static final class RFindSymbolNode extends Node {
        @Child private RdlsymNode rdlsymNode = new RdlsymNode();

        /**
         * Directly analogous to the GnuR function {@code R_FindSymbol}.
         *
         * @param name name of symbol (as appears in code) to look up
         * @param libName name of library to restrict search to (or all if {@code null} or empty
         *            string)
         * @param rns {@code rns.nst} encodes the type of native symbol to restrict search to (or
         *            all if {@code null})
         */
        @TruffleBoundary
        public SymbolHandle execute(String name, String libName, RegisteredNativeSymbol rns) {
            boolean all = libName == null || libName.length() == 0;
            ContextStateImpl contextState = getContextState();
            for (DLLInfo dllInfo : contextState.list) {
                if (dllInfo.forceSymbols) {
                    continue;
                }
                if (all || dllInfo.name.equals(libName)) {
                    SymbolHandle func = rdlsymNode.execute(dllInfo, name, rns);
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

        public static RFindSymbolNode create() {
            return new RFindSymbolNode();
        }
    }

    private static final class RFindSymbolRootNode extends RootNode {
        @Child private RFindSymbolNode findSymbolNode = RFindSymbolNode.create();

        private RFindSymbolRootNode() {
            super(RContext.getInstance().getLanguage());
            Truffle.getRuntime().createCallTarget(this);
        }

        @Override
        public SourceSection getSourceSection() {
            return RSyntaxNode.INTERNAL;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return findSymbolNode.execute((String) args[0], (String) args[1], (RegisteredNativeSymbol) args[2]);
        }

        private static CallTarget create(RContext context) {
            return context.getOrCreateCachedCallTarget(RFindSymbolRootNode.class, () -> new RFindSymbolRootNode().getCallTarget());
        }
    }

    public static final class RdlsymNode extends Node {
        @Child private DLLRFFI.DLSymNode dlSymNode = RFFIFactory.getDLLRFFI().createDLSymNode();

        /**
         * Directly analogous to the GnuR function {@code R_dlsym}. Checks first for a
         * {@link RegisteredNativeSymbol} using {@code rns}, then, unless dynamic lookup has been
         * disabled, looks up the symbol using the {@code dlopen} machinery.
         *
         * N.B. Unlike the underlying {@link DLLRFFI.DLSymNode} node this does <b>not</b> throw
         * {@link UnsatisfiedLinkError} if the symbol is not found; it returns
         * {@link #SYMBOL_NOT_FOUND}.
         */
        @TruffleBoundary
        public SymbolHandle execute(DLLInfo dllInfo, String name, RegisteredNativeSymbol rns) {
            SymbolHandle f = getDLLRegisteredSymbol(dllInfo, name, rns);
            if (f != SYMBOL_NOT_FOUND) {
                return f;
            }
            if (dllInfo.isSynthetic()) {
                return SYMBOL_NOT_FOUND;
            }

            // TODO: there is a weird interaction with namespace environments that makes this not
            // true in all cases
            // if (!dllInfo.dynamicLookup) {
            // return SYMBOL_NOT_FOUND;
            // }

            String mName = name;
            // assume Fortran underscore and lower case, although GnuR has cc code for this
            if (rns != null && rns.nst == NativeSymbolType.Fortran) {
                mName = name.toLowerCase() + "_";
            }
            try {
                if (dllInfo.unsuccessfulLookups.contains(mName)) {
                    return SYMBOL_NOT_FOUND;
                }
                return dlSymNode.execute(dllInfo.handle, mName);
            } catch (UnsatisfiedLinkError ex) {
                dllInfo.unsuccessfulLookups.add(mName);
                return SYMBOL_NOT_FOUND;
            }
        }

        public static RdlsymNode create() {
            return new RdlsymNode();
        }
    }

    /**
     * This is called by {@link UserRNG} because at the time the user-defined RNG is initialized it
     * is not known which library defines the RNG symbols.
     */
    public static DLLInfo findLibraryContainingSymbol(RContext context, String symbol) {
        RegisteredNativeSymbol rns = RegisteredNativeSymbol.any();
        SymbolHandle func = (SymbolHandle) RFindSymbolRootNode.create(context).call(symbol, null, rns);
        if (func == SYMBOL_NOT_FOUND) {
            return null;
        } else {
            return rns.dllInfo;
        }
    }

    /**
     * Searches the loaded libraries (packages) in this context for one that matches {@code name}.,
     * where {@code name} should be equivalent to having called {@link #libName} on the path to the
     * library.
     */
    public static DLLInfo findLibrary(String name) {
        ContextStateImpl contextState = getContextState();
        for (DLLInfo dllInfo : contextState.list) {
            if (dllInfo.name.equals(name)) {
                return dllInfo;
            }
        }
        return null;
    }

    public static DLLInfo getRdllInfo() {
        return ContextStateImpl.libRdllInfo;
    }

    /**
     * Search for symbol {@code name} in library defined by {@code dllInfo}, or {@code null} for
     * search in all loaded libraries. Used in the rare cases where no Truffle execution context
     * available.
     */
    public static SymbolHandle findSymbol(String name, DLLInfo dllInfo) {
        if (dllInfo != null) {
            assert !dllInfo.isEmbeddingDllInfo() : "Dynamic symbols lookup is not supported for the embedding DLLInfo";
            return (SymbolHandle) DLLRFFI.DLSymRootNode.create(RContext.getInstance()).call(dllInfo.handle, name);
        } else {
            return (SymbolHandle) RFindSymbolRootNode.create(RContext.getInstance()).call(name, null, RegisteredNativeSymbol.any());
        }
    }

    /*
     * Methods called from native code during library loading. These methods are single threaded by
     * virtue of the Semaphore in loadPackageDLL.
     */

    public static int useDynamicSymbols(DLLInfo dllInfo, int value) {
        int old = dllInfo.dynamicLookup ? 1 : 0;
        dllInfo.dynamicLookup = value != 0;
        return old;
    }

    public static int forceSymbols(DLLInfo dllInfo, int value) {
        int old = dllInfo.forceSymbols ? 1 : 0;
        dllInfo.forceSymbols = value != 0;
        return old;
    }

    private static final String EMBEDDING = "(embedding)";

    public static DLLInfo getEmbeddingDLLInfo() {
        DLLInfo result = findLibrary(EMBEDDING);
        if (result == null) {
            result = DLLInfo.create(EMBEDDING, EMBEDDING, false, null, true);
        }
        return result;
    }

    public static DLLInfo safeFindLibrary(String pkgName) {
        DLLInfo lib = DLL.findLibrary(pkgName);
        if (lib == null) {
            // It seems GNU R would create an C entry even for non-existing package, we are more
            // defensive
            throw RError.error(RError.NO_CALLER, Message.DLL_NOT_LOADED, pkgName);
        }
        return lib;
    }
}
