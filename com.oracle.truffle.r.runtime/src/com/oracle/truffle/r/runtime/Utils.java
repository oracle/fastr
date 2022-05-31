/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.TruffleStackTrace;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ConsoleIO;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.MultiSlotData;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

public final class Utils {

    public static boolean isIsoLatinDigit(char c) {
        return c >= '\u0030' && c <= '\u0039';
    }

    public static boolean isRomanLetter(char c) {
        return (/* lower case */c >= '\u00DF' && c <= '\u00FF') || (/* upper case */c >= '\u00C0' && c <= '\u00DE');
    }

    @TruffleBoundary(allowInlining = true)
    public static String toLowerCase(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    @TruffleBoundary(allowInlining = true)
    public static char toLowerCase(char c) {
        return Character.toLowerCase(c);
    }

    /**
     * For methods converting strings to numbers. Removes leading zeroes up to first non zero
     * character, but if the string is only string of zeroes e.g. '000', returns '0'. Strings
     * starting with '0x' are returned as is.
     */
    public static String trimLeadingZeros(String value) {
        if (value.length() <= 1 || value.charAt(0) != '0' || value.charAt(1) == 'x') {
            return value;
        }

        int i;
        for (i = 1; i < value.length() - 1; i++) {
            if (value.charAt(i) != '0') {
                break;
            }
        }
        return value.substring(i);
    }

    public static int incMod(int value, int mod) {
        int result = (value + 1);
        if (result == mod) {
            return 0;
        }
        return result;
    }

    public static int incMod(int value, int mod, ConditionProfile profile) {
        int result = (value + 1);
        if (profile.profile(result == mod)) {
            return 0;
        }
        return result;
    }

    /**
     * Locates a resource that is used within the implementation, e.g. a file of R code, and returns
     * a {@link Source} instance that represents it. Since the location may vary between
     * implementations and, in particular may not be a persistently accessible URL, we read the
     * content and store it as an "internal" instance.
     */
    public static Source getResourceAsSource(RContext context, Class<?> clazz, String resourceName) {
        try {
            InputStream is = ResourceHandlerFactory.getHandler().getResourceAsStream(context, clazz, resourceName);
            if (is == null) {
                throw new IOException();
            }
            String content = getResourceAsString(is);
            return RSource.fromTextInternal(content, RSource.Internal.R_IMPL);
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere("resource " + resourceName + " not found, context: " + clazz);
        }
    }

    public static String getResourceAsString(RContext context, Class<?> clazz, String resourceName, boolean mustExist) {
        InputStream is = ResourceHandlerFactory.getHandler().getResourceAsStream(context, clazz, resourceName);
        if (is == null) {
            if (!mustExist) {
                return null;
            }
        } else {
            try {
                return Utils.getResourceAsString(is);
            } catch (IOException ex) {
            }
        }
        throw RSuicide.rSuicide("resource " + resourceName + " not found");
    }

    private static String getResourceAsString(InputStream is) throws IOException {
        try (BufferedReader bs = new BufferedReader(new InputStreamReader(is))) {
            char[] buf = new char[1024];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = bs.read(buf, 0, buf.length)) > 0) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
    }

    public static void warn(String msg) {
        // CheckStyle: stop system..print check
        System.err.println("FastR warning: " + msg);
        // CheckStyle: resume system..print check
    }

    @TruffleBoundary
    public static String stringValueOf(Object value) {
        return String.valueOf(value);
    }

    @TruffleBoundary
    public static int hashCode(Object obj) {
        return Objects.hashCode(obj);
    }

    @TruffleBoundary(allowInlining = true)
    public static <T> ArrayList<T> createArrayList(int capacity) {
        return new ArrayList<>(capacity);
    }

    @TruffleBoundary(allowInlining = true)
    public static <T> boolean add(ArrayList<T> list, T obj) {
        return list.add(obj);
    }

    @TruffleBoundary(allowInlining = true)
    public static <T> boolean contains(ArrayList<T> list, T obj) {
        return list.contains(obj);
    }

    @TruffleBoundary(allowInlining = true)
    public static <T> T[] toArray(ArrayList<T> list, T[] arr) {
        return list.toArray(arr);
    }

    @TruffleBoundary(allowInlining = true)
    public static byte byteValue(Number value) {
        return value.byteValue();
    }

    @TruffleBoundary(allowInlining = true)
    public static int intValue(Number num) {
        return num.intValue();
    }

    @TruffleBoundary(allowInlining = true)
    public static double doubleValue(Number num) {
        return num.doubleValue();
    }

    @TruffleBoundary
    public static <K, T> void put(EconomicMap<K, T> map, K k, T t) {
        map.put(k, t);
    }

    @TruffleBoundary
    public static <K, T> T get(EconomicMap<K, T> map, K k) {
        return map.get(k);
    }

    @TruffleBoundary
    public static AtomicInteger get(EconomicMap<RBaseObject, AtomicInteger> preserveList, RBaseObject x) {
        return preserveList.get(x);
    }

    @TruffleBoundary
    public static String newString(char[] chars) {
        return new String(chars);
    }

    @TruffleBoundary(allowInlining = true)
    public static StringBuilder newStringBuilder(int capacity) {
        return new StringBuilder(capacity);
    }

    @TruffleBoundary(allowInlining = true)
    public static StringBuilder append(StringBuilder sb, String str) {
        return sb.append(str);
    }

    @TruffleBoundary(allowInlining = true)
    public static String toString(StringBuilder sb) {
        return sb.toString();
    }

    @TruffleBoundary(allowInlining = true)
    public static void putByte(ByteBuffer buffer, byte b) {
        buffer.put(b);
    }

    @TruffleBoundary(allowInlining = true)
    public static void putBytes(ByteBuffer buffer, byte[] bytes) {
        buffer.put(bytes);
    }

    @TruffleBoundary(allowInlining = true)
    public static void putDouble(ByteBuffer buffer, double aDouble) {
        buffer.putDouble(aDouble);
    }

    @TruffleBoundary(allowInlining = true)
    public static void putInt(ByteBuffer buffer, int anInt) {
        buffer.putInt(anInt);
    }

    /**
     * When running in "debug" mode, this exception is thrown rather than a call to System.exit, so
     * that control can return to an in-process debugger.
     */
    public static class DebugExitException extends RuntimeException {

        private static final long serialVersionUID = 1L;

    }

    /**
     * This the real, final, non-overrideable, exit of the entire R system. TODO well, modulo how
     * quit() is interpreted when R is started implicitly from a Polyglot shell that is running
     * other languages.
     */
    public static void systemExit(int status) {
        System.exit(status);
    }

    private static String userHome;

    private static String userHome() {
        CompilerAsserts.neverPartOfCompilation("property access cannot be expanded by PE");
        if (userHome == null) {
            userHome = System.getProperty("user.home");
        }
        return userHome;
    }

    /**
     * Returns a path for a log file with base name {@code fileNamePrefix}, adding '_pid' and
     * process-id and '.log' and taking into account whether the system is running in embedded mode.
     * In the latter case, we can't assume that the cwd is writable so '/tmp' is attempted. For
     * regular mode dirs are attempted in this order: cwd, user's home, '/tmp', rHome. If none of
     * the dirs is writable null is returned. <br/>
     * Currently {@link TruffleFile} object is returned since {@link RContext} instance (in order to
     * obtain a {@link TruffleFile} instance) might be uninitialized at time when log writing would
     * be needed.
     */
    public static TruffleFile getLogPath(RContext context, String fileNamePrefix) {
        String tmpDir = robustGetProperty("java.io.tmpdir");
        String dir = RContext.isEmbedded() ? tmpDir : robustGetProperty("user.dir");
        int dirId = 0;
        if (dir != null && tmpDir != null && context.getSafeTruffleFile(dir).startsWith(tmpDir) && dir.contains("R.INSTALL")) {
            // Simple heuristic to find out if we are in the middle of package installation, in
            // which case we do not want to write to the working directory, since the installation
            // process will remove that directory when finished.
            dirId = 1;
            dir = robustGetProperty("user.home");
        }
        int pid = RContext.getInitialPid();
        // Do not use PID if it was not set yet (logging/error during initialization)
        String pidStr = pid == 0 ? "" : "_pid" + pid;
        String baseName = fileNamePrefix + pidStr + ".log";
        while (true) {
            if (dir != null) {
                TruffleFile path = context.getSafeTruffleFile(dir).resolve(baseName);
                if (robustCheckWriteable(path)) {
                    return path;
                }
            }
            switch (dirId) {
                case 0:
                    if (RContext.isEmbedded()) {
                        return null;
                    } else {
                        dir = robustGetProperty("user.home");
                    }
                    break;
                case 1:
                    dir = tmpDir;
                    break;
                case 2:
                    dir = REnvVars.rHome(context);
                    break;
                default:
                    return null;
            }
            dirId++;
        }
    }

    private static String robustGetProperty(String name) {
        try {
            return System.getProperty(name);
        } catch (Throwable ex) {
            // System.getProperty may throw SecurityException, we catch all since we really need to
            // be robust at this point
            logGetLogPathError(ex.getMessage());
            return null;
        }
    }

    private static boolean robustCheckWriteable(TruffleFile logFile) {
        try {
            if (logFile == null) {
                return false;
            }
            TruffleFile parent = logFile.getParent();
            return parent != null && parent.isWritable() && (!logFile.exists() || logFile.isWritable());
        } catch (Throwable ex) {
            // may throw SecurityException, we catch all since we really need to be robust
            logGetLogPathError(ex.getMessage());
            return false;
        }
    }

    private static void logGetLogPathError(String message) {
        System.err.println("Note: error during determining the error log file location: " + message);
    }

    /**
     * Performs "~" expansion and also checks whether we need to take special case over relative
     * paths due to the curwd having moved from the initial setting. In the latter case, if the path
     * was relative it is adjusted for the new curwd setting. If {@code keepRelative == true} the
     * value is returned as a relative path, otherwise absolute. Almost all use cases should call
     * {@link #tildeExpand(String)} because providing a relative path to Java file methods with a
     * shifted curwd will not produce the right result. This {@code keepRelative == true} case is
     * required for file/directory listings.
     */
    public static String tildeExpand(String path) {
        if (path.length() > 0 && path.charAt(0) == '~') {
            return userHome() + path.substring(1);
        } else {
            return path;
        }
    }

    public static String unShQuote(String s) {
        if (s.charAt(0) == '\'') {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    /**
     * Helper for logging: create a summary describing the give value (Java class, vector length,
     * first few elements, etc.).
     */
    public static String getDebugInfo(Object value) {
        StringBuilder sb = new StringBuilder();
        printDebugInfo(sb, value);
        return sb.toString();
    }

    public static void printDebugInfo(StringBuilder sb, Object arg) {
        printDebugInfo(sb, arg, "");
    }

    /**
     * @see #getDebugInfo(Object)
     */
    public static void printDebugInfo(StringBuilder sb, Object arg, String additional) {
        if (arg == null) {
            sb.append("[null]");
            return;
        }
        sb.append(arg.getClass().getSimpleName()).append('(').append("hash:").append(Long.toHexString(arg.hashCode()));
        if (arg instanceof RSymbol) {
            sb.append(";\"").append(arg.toString()).append("\"");
        } else if (arg instanceof RAbstractVector) {
            RAbstractVector vec = (RAbstractVector) arg;
            if (vec.getLength() == 0) {
                sb.append(";empty");
            } else {
                sb.append(";len:").append(vec.getLength()).append(";data:");
                for (int i = 0; i < Math.min(3, vec.getLength()); i++) {
                    String str = ((RAbstractVector) arg).getDataAtAsObject(0).toString();
                    str = str.length() > 30 ? str.substring(0, 27) + "..." : str;
                    sb.append(',').append(str);
                }
            }
        } else if (arg instanceof TruffleObject) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            if (interop.isPointer(arg)) {
                try {
                    sb.append(";ptr:").append(Long.toHexString(interop.asPointer(arg)));
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere();
                }
            }
        }
        sb.append(additional).append(')');
    }

    /**
     * Retrieve a frame from the call stack. N.B. To avoid the iterator overhead use
     * {@link #getActualCurrentFrame()} for the current frame.
     *
     * TODO The check for {@code first} seems bogus. It assumes that {@code depth} never equals that
     * associated with {@link #getActualCurrentFrame()}, i.e. all requests for the top frame use
     * {@link #getActualCurrentFrame()} as suggested. But if they don't, then this will incorrectly
     * return {@code null}.
     *
     * @param fa kind of access required to the frame
     * @param target identifies which frame is required
     * @return {@link Frame} instance or {@code null} if {@code target} is not found
     */
    @TruffleBoundary
    public static Frame getStackFrame(FrameAccess fa, RCaller target) {
        RError.performanceWarning("slow frame access - getStackFrame1");
        assert target != null;
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
            boolean first = true;

            @Override
            public Frame visitFrame(FrameInstance frameInstance) {
                if (!first) {
                    Frame pf = frameInstance.getFrame(fa);
                    Frame f = RArguments.unwrap(pf);
                    if (RArguments.isRFrame(f)) {
                        return RArguments.getCall(f) == target ? f : null;
                    } else {
                        return null;
                    }
                }
                first = false;
                return null;
            }
        });
    }

    /**
     * Searches for the frame on the call stack whose caller is the same as the {@code target}
     * argument. It uses the {@link RArguments#INDEX_CALLER_FRAME} frame argument to traverse the
     * chain of frames, instead of iterating the stack using
     * {@link TruffleRuntime#iterateFrames(FrameInstanceVisitor)}. A benefit of so doing is that
     * this method does not have to be put beyond the Truffle boundary. This method returns null if
     * no such frame is found or if the search loop encounters a frame not containing a frame in the
     * {@link RArguments#INDEX_CALLER_FRAME} argument.
     *
     * @param frame the current frame
     * @param target the target caller
     */
    public static Frame getStackFrame(Frame frame, RCaller target) {
        Frame f = frame;
        RCaller call = RArguments.getCall(f);
        while (call != target) {
            Object fObj = RArguments.getCallerFrame(f);
            if (fObj instanceof Frame) {
                assert fObj instanceof MaterializedFrame;
                f = (Frame) fObj;
            } else if (fObj instanceof CallerFrameClosure) {
                CallerFrameClosure fc = (CallerFrameClosure) fObj;
                fc.setNeedsCallerFrame();
                f = fc.getMaterializedCallerFrame();
                if (f == null) {
                    return null;
                }
            } else {
                assert fObj == null;
                return null;
            }
            call = RArguments.getCall(f);
        }

        return f;
    }

    /**
     * Like {@link #getStackFrame(FrameAccess, RCaller)}, but identifying the stack with its depth.
     * Along the way it invalidates the assumptions that the caller frame is needed.
     */
    @TruffleBoundary
    public static Frame getStackFrame(FrameAccess fa, int depth, boolean notifyCallers) {
        RError.performanceWarning("slow frame access - getStackFrame2");
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
            boolean first = true;

            @Override
            public Frame visitFrame(FrameInstance frameInstance) {
                if (!first) {
                    Frame pf = frameInstance.getFrame(fa);
                    Frame f = RArguments.unwrap(pf);
                    if (RArguments.isRFrame(f)) {
                        RCaller call = RArguments.getCall(f);
                        if (notifyCallers) {
                            Object callerFrame = RArguments.getCallerFrame(f);
                            if (callerFrame instanceof CallerFrameClosure) {
                                CallerFrameClosure closure = (CallerFrameClosure) callerFrame;
                                closure.setNeedsCallerFrame();
                            }
                        }
                        return (!call.isPromise() && call.getDepth() == depth) ? f : null;
                    } else {
                        return null;
                    }
                }
                first = false;
                return null;
            }
        });
    }

    /**
     * Iterate over all R stack frames (skipping the first, current one) until the given function
     * returns a non-null value.
     *
     * @return the non-null value returned by the given function, or null if it never returned a
     *         non-null value.
     */
    @TruffleBoundary
    public static <T> T iterateRFrames(FrameAccess fa, Function<Frame, T> func) {
        RError.performanceWarning("slow frame access - iterateRFrames");
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<T>() {
            boolean first = true;

            @Override
            public T visitFrame(FrameInstance frameInstance) {
                if (!first) {
                    Frame f = RArguments.unwrap(frameInstance.getFrame(fa));
                    if (RArguments.isRFrame(f)) {
                        return func.apply(f);
                    } else {
                        return null;
                    }
                }
                first = false;
                return null;
            }
        });
    }

    /**
     * Retrieve the caller frame of the current frame.
     * <p>
     * TODO Calls to this method should be validated with respect to whether promise evaluation is
     * in progress.
     */
    public static Frame getCallerFrame(RCaller caller, FrameAccess fa) {
        RCaller parent = RCaller.unwrapPromiseCaller(caller);
        parent = RCaller.unwrapPromiseCaller(parent.getPrevious());
        return parent == null ? null : getStackFrame(fa, parent);
    }

    /**
     * Retrieve the actual current frame. This may be different from the frame returned by
     * {@code TruffleRuntime#getCurrentFrame()} due to operations applied in
     * {@code FunctionDefinitionNode.execute(VirtualFrame)}. Also see
     * {@code FunctionDefinitionNode.substituteFrame}.
     */
    @TruffleBoundary
    public static Frame getActualCurrentFrame() {
        RError.performanceWarning("slow frame access - getActualCurrentFrame");
        FrameInstance frameInstance = Truffle.getRuntime().getCurrentFrame();
        if (frameInstance == null) {
            // Might be the case during initialization, when envs are prepared before the actual
            // Truffle/R system has started
            return null;
        }
        Frame frame = RArguments.unwrap(frameInstance.getFrame(FrameAccess.MATERIALIZE));
        if (!RArguments.isRFrame(frame)) {
            return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
                @Override
                public Frame visitFrame(FrameInstance instance) {
                    Frame current = RArguments.unwrap(instance.getFrame(FrameAccess.MATERIALIZE));
                    return RArguments.isRFrame(current) ? current : null;
                }
            });
        }
        return frame;
    }

    /**
     * Return a top down stack traceback as a pairlist of character vectors possibly attributed with
     * srcref information.
     *
     * @param skipArg number of frame to skip
     * @return {@link RNull#instance} if no trace else a {@link RPairList}.
     */
    @TruffleBoundary
    public static Object createTraceback(int skipArg) {
        RError.performanceWarning("slow frame access - createTraceback");
        List<TruffleStackTraceElement> st = TruffleStackTrace.getStackTrace(new DummyTracebackPolyglotException());
        if (st == null) {
            return RNull.instance;
        }
        return toPairList(st, skipArg);
    }

    public static Object toPairList(List<TruffleStackTraceElement> st, int skipArg) {
        RPairList head = null;
        RPairList prev = null;
        int skip = skipArg;
        Iterator<TruffleStackTraceElement> it = st.iterator();
        while (it.hasNext()) {
            TruffleStackTraceElement element = it.next();
            RootNode targetRoot = element.getTarget().getRootNode();
            if (targetRoot.isInternal()) {
                continue;
            }
            String rootName = targetRoot.getName();

            LanguageInfo info = targetRoot.getLanguageInfo();
            if (info == null) {
                continue;
            }

            if (skip > 0) {
                skip--;
                continue;
            }

            String languageId = info.getId();
            RPairList pl = "R".equals(languageId) ? toPairList(element.getFrame()) : toPairList(element, languageId, rootName);
            if (pl != null) {
                if (prev != null) {
                    prev.setCdr(pl);
                } else {
                    head = pl;
                }
                prev = pl;
            }
        }
        return head == null ? RNull.instance : head;
    }

    private static RPairList toPairList(TruffleStackTraceElement element, String languageId, String rootName) {
        // copied from PolyglotExceptionFrame.createGuest()
        Node callNode = element.getLocation();
        SourceSection section;
        if (callNode != null) {
            section = callNode.getEncapsulatingSourceSection();
        } else {
            section = null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("at ");
        sb.append(spaces(Math.max(0, languageId.length()) - languageId.length())).append("<").append(languageId).append("> ");
        sb.append(rootName);
        sb.append("(");

        sb.append(formatSource(section));
        sb.append(")");
        return RDataFactory.createPairList(RDataFactory.createStringVectorFromScalar(sb.toString()));
    }

    private static RPairList toPairList(Frame f) {
        if (f == null) {
            return null;
        }
        Frame frame = RArguments.unwrap(f);
        if (!RArguments.isRFrame(frame) || RArguments.getFunction(frame) == null) {
            return null;
        }
        RCaller call = RArguments.getCall(frame);

        assert call != null;
        if (!call.isValidCaller()) {
            // this is extra robustness. In ideal world we should not encounter invalid ones
            return null;
        }
        RPairList rl = RContext.getRRuntimeASTAccess().getSyntaxCaller(call);
        if (rl == null) {
            // this can happen if the call represents promise frame and its logical parent is
            // the top level execution context
            return null;
        }
        SourceSection ss = rl.getSourceSection();
        // fabricate a srcref attribute from ss
        Source source = ss != null ? ss.getSource() : null;
        String path = RSource.getPath(source);
        RStringVector callerSource = RDataFactory.createStringVectorFromScalar(RContext.getRRuntimeASTAccess().getCallerSource(call));
        if (path != null) {
            callerSource.setAttr(RRuntime.R_SRCREF, RSrcref.createLloc(RContext.getInstance(), ss, path));
        }
        return RDataFactory.createPairList(callerSource);
    }

    private static String spaces(int length) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; i++) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String formatSource(SourceSection sourceSection) {
        if (sourceSection == null) {
            return "Unknown";
        }
        Source source = sourceSection.getSource();
        if (source == null) {
            // safety check. likely not necsssary
            return "Unknown";
        }
        StringBuilder b = new StringBuilder();
        String path = source.getPath();
        if (path == null) {
            b.append(source.getName());
        } else {
            b.append(path);
        }

        b.append(":").append(formatIndices(sourceSection, true));
        return b.toString();
    }

    private static String formatIndices(SourceSection sourceSection, boolean needsColumnSpecifier) {
        StringBuilder b = new StringBuilder();
        boolean singleLine = sourceSection.getStartLine() == sourceSection.getEndLine();
        if (singleLine) {
            b.append(sourceSection.getStartLine());
        } else {
            b.append(sourceSection.getStartLine()).append("-").append(sourceSection.getEndLine());
        }
        if (needsColumnSpecifier) {
            b.append(":");
            if (sourceSection.getCharLength() <= 1) {
                b.append(sourceSection.getCharIndex());
            } else {
                b.append(sourceSection.getCharIndex()).append("-").append(sourceSection.getCharIndex() + sourceSection.getCharLength() - 1);
            }
        }
        return b.toString();
    }

    /**
     * Generate a stack trace as a string.
     */
    @TruffleBoundary
    public static String createStackTrace(boolean printFrameSlots) {
        RError.performanceWarning("slow frame access - createStackTrace");
        FrameInstance current = Truffle.getRuntime().getCurrentFrame();
        if (current == null) {
            return "no R stack trace available\n";
        } else {
            StringBuilder str = new StringBuilder();
            Truffle.getRuntime().iterateFrames(frameInstance -> {
                dumpFrame(str, frameInstance.getCallTarget(), frameInstance.getFrame(FrameAccess.READ_ONLY), false, frameInstance.isVirtualFrame());
                return null;
            });
            if (printFrameSlots) {
                str.append("\n\nwith frame slot contents:\n");
                Truffle.getRuntime().iterateFrames(frameInstance -> {
                    dumpFrame(str, frameInstance.getCallTarget(), frameInstance.getFrame(FrameAccess.READ_ONLY), true, frameInstance.isVirtualFrame());
                    return null;
                });
            }
            str.append("\n");
            return str.toString();
        }
    }

    private static void dumpFrame(StringBuilder str, CallTarget callTarget, Frame frame, boolean printFrameSlots, boolean isVirtual) {
        CompilerAsserts.neverPartOfCompilation();
        try {
            CompilerAsserts.neverPartOfCompilation();
            if (str.length() > 0) {
                str.append("\n");
            }
            Frame unwrapped = RArguments.unwrap(frame);
            if (!RArguments.isRFrame(unwrapped)) {
                if (unwrapped.getArguments().length == 0) {
                    str.append("<empty frame>");
                } else {
                    str.append("<unknown frame>");
                }
            } else {
                if (callTarget.toString().equals("<promise>")) {
                    /* these have the same depth as the next frame, and add no useful info. */
                    return;
                }
                RCaller call = RArguments.getCall(unwrapped);
                if (call != null) {
                    String callSrc = call.isValidCaller() ? RContext.getRRuntimeASTAccess().getCallerSource(call) : "<invalid call>";
                    int depth = RArguments.getDepth(unwrapped);
                    str.append("Frame(d=").append(depth).append("): ").append(callTarget).append(isVirtual ? " (virtual)" : "");
                    str.append(" (called as: ").append(callSrc).append(')');
                }
                if (printFrameSlots) {
                    FrameDescriptor frameDescriptor = unwrapped.getFrameDescriptor();
                    for (Object identifier : FrameSlotChangeMonitor.getIdentifiers(frameDescriptor)) {
                        str.append("\n      ").append(identifier.toString()).append(" = ");
                        Object value = FrameSlotChangeMonitor.getObject(unwrapped, identifier);
                        if (value instanceof MultiSlotData) {
                            value = ((MultiSlotData) value).get(RContext.getInstance().getMultiSlotInd());
                        }
                        try {
                            if (value instanceof RAbstractContainer && ((RAbstractContainer) value).getLength() > 32) {
                                str.append('<').append(value.getClass().getSimpleName()).append(" with ").append(((RAbstractContainer) value).getLength()).append(" elements>");
                            } else {
                                String text = String.valueOf(value);
                                str.append(text.length() < 256 ? text : text.substring(0, 256) + "...");
                            }
                        } catch (Throwable t) {
                            // RPairList values may not react kindly to getLength() calls
                            str.append("<exception ").append(t.getClass().getSimpleName()).append(" while printing value of type ").append(
                                            value == null ? "null" : value.getClass().getSimpleName()).append('>');
                        }
                    }
                }
            }
        } catch (Throwable t) {
            str.append("<exception ").append(t.getMessage()).append(" ").append(t.getClass().getSimpleName()).append("<");
        }
    }

    public static void writeStderr(String s, boolean nl) {
        try {
            StdConnections.getStderr().writeString(s, nl);
        } catch (IOException ex) {
            // Very unlikely
            ConsoleIO console = RContext.getInstance().getConsole();
            console.printErrorln("Error writing to stderr: " + ex.getMessage());
            console.printErrorln(s);
        }
    }

    public static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            int ub = Byte.toUnsignedInt(b);
            if (ub > 15) {
                sb.append(Integer.toHexString(ub >> 4));
            } else {
                sb.append('0');
            }
            sb.append(Integer.toHexString(ub & 0xF));
        }
        return sb.toString();
    }

    public static int intFilePermissions(Set<PosixFilePermission> permissions) {
        int r = 0;
        for (PosixFilePermission pfp : permissions) {
            // @formatter:off
            switch (pfp) {
                case OTHERS_EXECUTE: r |= 1; break;
                case OTHERS_WRITE: r |= 2; break;
                case OTHERS_READ: r |= 4; break;
                case GROUP_EXECUTE: r |= 8; break;
                case GROUP_WRITE: r |= 16; break;
                case GROUP_READ: r |= 32; break;
                case OWNER_EXECUTE: r |= 64; break;
                case OWNER_WRITE: r |= 128; break;
                case OWNER_READ: r |= 256; break;
            }
            // @formatter:on
        }
        return r;
    }

    @TruffleBoundary
    public static String intern(String s) {
        return s.intern();
    }

    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ", justification = "intended behavior")
    public static boolean isInterned(String s) {
        return s == s.intern();
    }

    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ", justification = "this is intended for interned strings")
    public static boolean identityEquals(String value, String other) {
        assert (value == null || isInterned(value)) && (other == null || isInterned(other));
        return value == other;
    }

    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ", justification = "this is intended for fast path string comparisons that are followed by proper calls to equals")
    public static boolean fastPathIdentityEquals(String value, String other) {
        return value == other;
    }

    @SuppressFBWarnings(value = "FE_FLOATING_POINT_EQUALITY", justification = "intentional comparisons of floating point numbers")
    public static boolean identityEquals(double value, double other) {
        return value == other;
    }

    @TruffleBoundary
    public static String toString(Object obj) {
        return obj.toString();
    }

    @TruffleBoundary
    public static boolean equals(Object a, Object b) {
        return a.equals(b);
    }

    @TruffleBoundary
    public static String stringFormat(String format, Object... objects) {
        return String.format(format, objects);
    }

    /**
     * Makes the best effort to create end user understandable String representation of the type of
     * the parameter. When the parameter is null, returns "null".
     */
    public static String getTypeName(Object value) {
        // Typically part of error reporting. The whole error reporting should be behind TB.
        CompilerAsserts.neverPartOfCompilation();
        if (value == null) {
            return "null";
        }
        RType rType = RType.getRType(value);
        return rType == null ? value.getClass().getSimpleName() : rType.getName();
    }

    private static boolean isWriteableDirectory(String path) {
        TruffleFile f = RContext.getInstance().getSafeTruffleFile(path);
        return f.exists() && f.isDirectory() && f.isWritable();
    }

    public static String getUserTempDir() {
        final String[] envVars = new String[]{"TMPDIR", "TMP", "TEMP"};
        String startingTempDir = null;
        for (String envVar : envVars) {
            String value = System.getenv(envVar);
            if (value != null && isWriteableDirectory(value)) {
                startingTempDir = value;
            }
        }
        if (startingTempDir == null) {
            startingTempDir = "/tmp";
        }
        return startingTempDir;
    }

    public static int getTimeInSecs(FileTime fileTime) {
        if (fileTime == null) {
            return RRuntime.INT_NA;
        } else {
            return (int) (fileTime.toMillis() / 1000);
        }
    }

    /**
     * Determines the PID using a system call.
     */
    public static int getPid() {
        return (int) BaseRFFI.GetpidRootNode.create().getCallTarget().call();
    }

    public static String wildcardToRegex(String wildcard) {
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                case '^': // escape character in cmd.exe
                    s.append("\\");
                    break;
                // escape special regexp-characters
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return (s.toString());
    }

    @TruffleBoundary(allowInlining = true)
    public static byte[] intArrayToByteArray(int[] intArr, ByteOrder o) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(intArr.length * 4);
        byteBuffer.order(o);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(intArr);
        return byteBuffer.array();
    }

    private static final class DummyTracebackPolyglotException extends AbstractTruffleException {
        private static final long serialVersionUID = -1529107348094772364L;
    }

}
