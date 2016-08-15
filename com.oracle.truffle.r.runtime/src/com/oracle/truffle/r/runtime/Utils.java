/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
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
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.GraphPrintVisitor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class Utils {

    public static boolean isIsoLatinDigit(char c) {
        return c >= '\u0030' && c <= '\u0039';
    }

    public static boolean isRomanLetter(char c) {
        return (/* lower case */c >= '\u00DF' && c <= '\u00FF') || (/* upper case */c >= '\u00C0' && c <= '\u00DE');
    }

    public static int incMod(int value, int mod) {
        int result = (value + 1);
        if (result == mod) {
            return 0;
        }
        return result;
    }

    public static void dumpFunction(String groupName, RFunction function) {
        GraphPrintVisitor graphPrinter = new GraphPrintVisitor();
        RootCallTarget callTarget = function.getTarget();
        if (callTarget != null) {
            graphPrinter.beginGroup(groupName);
            graphPrinter.beginGraph(RRuntime.toString(function)).visit(callTarget.getRootNode());
        }
        graphPrinter.printToNetwork(true);
    }

    public static Source getResourceAsSource(Class<?> clazz, String resourceName) {
        try {
            URL url = ResourceHandlerFactory.getHandler().getResource(clazz, resourceName);
            if (url == null) {
                throw RInternalError.shouldNotReachHere("resource " + resourceName + " not found, context: " + clazz);
            }
            return RSource.fromURL(url, resourceName);
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere("resource " + resourceName + " not found, context: " + clazz);
        }
    }

    public static String getResourceAsString(Class<?> clazz, String resourceName, boolean mustExist) {
        InputStream is = ResourceHandlerFactory.getHandler().getResourceAsStream(clazz, resourceName);
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
        throw Utils.rSuicide("resource " + resourceName + " not found");
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

    /**
     * When running in "debug" mode, this exception is thrown rather than a call to System.exit, so
     * that control can return to an in-process debugger.
     */
    public static class DebugExitException extends RuntimeException {

        private static final long serialVersionUID = 1L;

    }

    /**
     * Called when the system encounters a fatal internal error and must commit suicide (i.e.
     * terminate). It allows an embedded client to override the default (although they typically
     * invoke the default eventually).
     */
    public static RuntimeException rSuicide(String msg) {
        if (RInterfaceCallbacks.R_Suicide.isOverridden()) {
            RFFIFactory.getRFFI().getREmbedRFFI().suicide(msg);
        }
        throw rSuicideDefault(msg);
    }

    /**
     * The default, non-overrideable, suicide call. It prints the message and throws
     * {@link ExitException}.
     *
     * @param msg
     */
    public static RuntimeException rSuicideDefault(String msg) {
        System.err.println("FastR unexpected failure: " + msg);
        throw new ExitException(2);
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

    private static final class WorkingDirectoryState {
        /**
         * The initial working directory on startup. This and {@link #current} are always absolute
         * paths.
         */
        private FileSystem fileSystem;
        private final String initial;
        private String current;
        private Path currentPath;

        private WorkingDirectoryState() {
            if (fileSystem == null) {
                fileSystem = FileSystems.getDefault();
            }
            initial = System.getProperty("user.dir");
            current = initial;
            currentPath = fileSystem.getPath(initial);
        }

        private Path getCurrentPath() {
            return currentPath;
        }

        private void setCurrent(String path) {
            current = path;
            currentPath = fileSystem.getPath(path);
        }

        private boolean isInitial() {
            return current.equals(initial);
        }

        private FileSystem getFileSystem() {
            return fileSystem;
        }
    }

    /**
     * Keeps a record of the current working directory as Java provides no way to change this AND
     * many of the file methods that operate on relative paths work from the initial value.
     */
    private static WorkingDirectoryState wdState;

    private static WorkingDirectoryState wdState() {
        if (wdState == null) {
            wdState = new WorkingDirectoryState();
        }
        return wdState;
    }

    public static void updateCurwd(String path) {
        wdState().setCurrent(path);
    }

    /**
     * Returns a {@link Path} for a log file with base name {@code fileName}, taking into account
     * whether the system is running in embedded mode. In the latter case, we can't assume that the
     * cwd is writable. Plus some embedded apps, e.g. RStudio, spawn multiple R sub-processes
     * concurrently so we tag the file with the pid.
     */
    public static Path getLogPath(String fileName) {
        String root = RContext.isEmbedded() ? "/tmp" : REnvVars.rHome();
        int pid = RFFIFactory.getRFFI().getBaseRFFI().getpid();
        String baseName = RContext.isEmbedded() ? fileName + "-" + Integer.toString(pid) : fileName;
        return FileSystems.getDefault().getPath(root, baseName);
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
    public static String tildeExpand(String path, boolean keepRelative) {
        if (path.length() > 0 && path.charAt(0) == '~') {
            return userHome() + path.substring(1);
        } else {
            if (wdState().isInitial()) {
                return path;
            } else {
                /*
                 * This is moderately painful, as can't rely on most of the normal relative path
                 * support in Java as much of it works relative to the initial setting.
                 */
                if (path.length() == 0) {
                    return keepRelative ? path : wdState().getCurrentPath().toString();
                } else {
                    Path p = wdState().getFileSystem().getPath(path);
                    if (p.isAbsolute()) {
                        return path;
                    } else {
                        Path currentPath = wdState().getCurrentPath();
                        Path truePath = currentPath.resolve(p);
                        if (keepRelative) {
                            // relativize it (it was relative to start with)
                            return currentPath.relativize(truePath).toString();
                        } else {
                            return truePath.toString();
                        }
                    }
                }
            }
        }
    }

    /**
     * Return an absolute path, with "~" expansion, for {@code path}, taking into account any change
     * in curwd.
     */
    public static String tildeExpand(String path) {
        return tildeExpand(path, false);
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
     * @return {@link Frame} instance or {@code null} if {@code depth} is out of range
     */
    @TruffleBoundary
    public static Frame getStackFrame(FrameAccess fa, RCaller target) {
        assert target != null;
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
            boolean first = true;

            @Override
            public Frame visitFrame(FrameInstance frameInstance) {
                if (!first) {
                    Frame pf = frameInstance.getFrame(fa, false);
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
     * Like {@link #getStackFrame(FrameAccess, RCaller)}, but identifying the stack with its depth.
     */
    @TruffleBoundary
    public static Frame getStackFrame(FrameAccess fa, int depth) {
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
            boolean first = true;

            @Override
            public Frame visitFrame(FrameInstance frameInstance) {
                if (!first) {
                    Frame pf = frameInstance.getFrame(fa, false);
                    Frame f = RArguments.unwrap(pf);
                    if (RArguments.isRFrame(f)) {
                        RCaller call = RArguments.getCall(f);
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
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<T>() {
            boolean first = true;

            @Override
            public T visitFrame(FrameInstance frameInstance) {
                if (!first) {
                    Frame f = RArguments.unwrap(frameInstance.getFrame(fa, false));
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
     *
     * TODO Calls to this method should be validated with respect to whether promise evaluation is
     * in progress and replaced with use of {@code FrameDepthNode}.
     */
    public static Frame getCallerFrame(Frame frame, FrameAccess fa) {
        RCaller parent = RArguments.getCall(frame);
        while (parent != null && parent.isPromise()) {
            parent = parent.getParent();
        }
        parent = parent.getParent();
        while (parent != null && parent.isPromise()) {
            parent = parent.getParent();
        }
        return parent == null ? null : getStackFrame(fa, parent);
    }

    /**
     * Retrieve the actual current frame. This may be different from the frame returned by
     * {@link TruffleRuntime#getCurrentFrame()} due to operations applied in
     * {@code FunctionDefinitionNode.execute(VirtualFrame)}. Also see
     * {@code FunctionDefinitionNode.substituteFrame}.
     */
    @TruffleBoundary
    public static Frame getActualCurrentFrame() {
        FrameInstance frameInstance = Truffle.getRuntime().getCurrentFrame();
        if (frameInstance == null) {
            // Might be the case during initialization, when envs are prepared before the actual
            // Truffle/R system has started
            return null;
        }
        return RArguments.unwrap(frameInstance.getFrame(FrameAccess.MATERIALIZE, true));
    }

    private static final class TracebackVisitor implements FrameInstanceVisitor<Frame> {
        private int skip;
        private RPairList head;
        private RPairList prev;

        private TracebackVisitor(int skip) {
            this.skip = skip;
        }

        @Override
        public Frame visitFrame(FrameInstance frameInstance) {
            Frame f = RArguments.unwrap(frameInstance.getFrame(FrameAccess.READ_ONLY, true));
            if (RArguments.isRFrame(f) && RArguments.getFunction(f) != null) {
                if (skip > 0) {
                    skip--;
                } else {
                    RCaller call = RArguments.getCall(f);
                    assert call != null;
                    RLanguage rl = RContext.getRRuntimeASTAccess().getSyntaxCaller(call);
                    RSyntaxNode sn = (RSyntaxNode) rl.getRep();
                    SourceSection ss = sn != null ? sn.getSourceSection() : null;
                    // fabricate a srcref attribute from ss
                    Source source = ss != null ? ss.getSource() : null;
                    String path = RSource.getPath(source);
                    RStringVector callerSource = RDataFactory.createStringVectorFromScalar(RContext.getRRuntimeASTAccess().getCallerSource(call));
                    if (path != null) {
                        callerSource.setAttr(RRuntime.R_SRCREF, RSrcref.createLloc(ss, path));
                    }
                    RPairList pl = RDataFactory.createPairList(callerSource);
                    if (prev != null) {
                        prev.setCdr(pl);
                    } else {
                        head = pl;
                    }
                    prev = pl;
                }
            }
            return null;
        }

    }

    /**
     * Return a top down stack traceback as a pairlist of character vectors possibly attributed with
     * srcref information.
     *
     * @param skip number of frame to skip
     * @return {@link RNull#instance} if no trace else a {@link RPairList}.
     */
    @TruffleBoundary
    public static Object createTraceback(int skip) {
        FrameInstance current = Truffle.getRuntime().getCurrentFrame();
        if (current != null) {
            TracebackVisitor fiv = new TracebackVisitor(skip);
            Truffle.getRuntime().iterateFrames(fiv);
            return fiv.head == null ? RNull.instance : fiv.head;
        } else {
            return RNull.instance;
        }
    }

    /**
     * Generate a stack trace as a string.
     */
    @TruffleBoundary
    public static String createStackTrace(boolean printFrameSlots) {
        FrameInstance current = Truffle.getRuntime().getCurrentFrame();
        if (current == null) {
            return "no R stack trace available\n";
        } else {
            StringBuilder str = new StringBuilder();
            Truffle.getRuntime().iterateFrames(frameInstance -> {
                dumpFrame(str, frameInstance.getCallTarget(), frameInstance.getFrame(FrameAccess.READ_ONLY, true), false, frameInstance.isVirtualFrame());
                return null;
            });
            if (printFrameSlots) {
                str.append("\n\nwith frame slot contents:\n");
                Truffle.getRuntime().iterateFrames(frameInstance -> {
                    dumpFrame(str, frameInstance.getCallTarget(), frameInstance.getFrame(FrameAccess.READ_ONLY, true), true, frameInstance.isVirtualFrame());
                    return null;
                });
            }
            str.append("\n");
            return str.toString();
        }
    }

    private static void dumpFrame(StringBuilder str, CallTarget callTarget, Frame frame, boolean printFrameSlots, boolean isVirtual) {
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
                for (FrameSlot s : frameDescriptor.getSlots()) {
                    str.append("\n      ").append(s.getIdentifier()).append(" = ");
                    Object value = unwrapped.getValue(s);
                    try {
                        if (value instanceof RAbstractContainer && ((RAbstractContainer) value).getLength() > 32) {
                            str.append('<').append(value.getClass().getSimpleName()).append(" with ").append(((RAbstractContainer) value).getLength()).append(" elements>");
                        } else {
                            String text = String.valueOf(value);
                            str.append(text.length() < 256 ? text : text.substring(0, 256) + "...");
                        }
                    } catch (Throwable t) {
                        // RLanguage values may not react kindly to getLength() calls
                        str.append("<exception ").append(t.getClass().getSimpleName()).append(" while printing value of type ").append(
                                        value == null ? "null" : value.getClass().getSimpleName()).append(
                                                        '>');
                    }
                }
            }
        }
    }

    public static <T> T[] resizeArray(T[] oldValues, int newSize) {
        T[] newValues = oldValues;
        if (oldValues != null) {
            newValues = Arrays.copyOf(oldValues, newSize);
        }
        return newValues;
    }

    // simple implementations of non-recursive hash-maps to enable compilation
    // TODO: consider replacing with a more efficient library implementation

    public static class NonRecursiveHashMap<KEY> {

        private Object[] keys;
        private int[] values;

        public NonRecursiveHashMap(int approxCapacity) {
            keys = new Object[Math.max(approxCapacity, 1)];
            values = new int[Math.max(approxCapacity, 1)];
        }

        public boolean put(KEY key, int value) {
            int ind = Math.abs(key.hashCode()) % keys.length;
            int firstInd = ind;
            while (true) {
                if (keys[ind] == null) {
                    keys[ind] = key;
                    values[ind] = value;
                    return false;
                } else if (key.equals(keys[ind])) {
                    values[ind] = value;
                    return true;
                } else {
                    ind = (ind + 1) % keys.length;
                    if (ind == firstInd) {
                        // resize
                        Object[] newKeys = new Object[keys.length + keys.length / 2];
                        int[] newValues = new int[values.length + values.length / 2];
                        for (int i = 0; i < keys.length; i++) {
                            if (keys[i] != null) {
                                int tmpInd = Math.abs(keys[i].hashCode()) % newKeys.length;
                                while (true) {
                                    if (newKeys[tmpInd] == null) {
                                        newKeys[tmpInd] = keys[i];
                                        newValues[tmpInd] = values[i];
                                        break;
                                    } else {
                                        assert !keys[i].equals(newKeys[tmpInd]);
                                        tmpInd = (tmpInd + 1) % newKeys.length;
                                    }
                                }
                            }
                        }

                        keys = newKeys;
                        values = newValues;

                        // start hashing from the beginning
                        ind = Math.abs(key.hashCode()) % keys.length;
                    }
                }
            }
        }

        public int get(KEY key) {
            int ind = Math.abs(key.hashCode()) % keys.length;
            int firstInd = ind;
            while (true) {
                if (key.equals(keys[ind])) {
                    return values[ind];
                } else {
                    ind = (ind + 1) % keys.length;
                    if (ind == firstInd || keys[ind] == null) {
                        return -1;
                    }
                }
            }
        }
    }

    public static class NonRecursiveHashMapDouble {

        private double[] keys;
        private int[] values;

        public NonRecursiveHashMapDouble(int approxCapacity) {
            keys = new double[Math.max(approxCapacity, 1)];
            Arrays.fill(keys, RRuntime.DOUBLE_NA);
            values = new int[Math.max(approxCapacity, 1)];
        }

        public boolean put(double key, int value) {
            int ind = Math.abs(Double.hashCode(key)) % keys.length;
            int firstInd = ind;
            while (true) {
                if (RRuntime.isNAorNaN(keys[ind])) {
                    keys[ind] = key;
                    values[ind] = value;
                    return false;
                } else if (key == keys[ind]) {
                    values[ind] = value;
                    return true;
                } else {
                    ind = (ind + 1) % keys.length;
                    if (ind == firstInd) {
                        // resize
                        double[] newKeys = new double[keys.length + keys.length / 2];
                        Arrays.fill(newKeys, RRuntime.DOUBLE_NA);
                        int[] newValues = new int[values.length + values.length / 2];
                        for (int i = 0; i < keys.length; i++) {
                            if (!RRuntime.isNAorNaN(keys[i])) {
                                int tmpInd = Math.abs(Double.hashCode(keys[i])) % newKeys.length;
                                while (true) {
                                    if (RRuntime.isNAorNaN(newKeys[tmpInd])) {
                                        newKeys[tmpInd] = keys[i];
                                        newValues[tmpInd] = values[i];
                                        break;
                                    } else {
                                        assert keys[i] != newKeys[tmpInd];
                                        tmpInd = (tmpInd + 1) % newKeys.length;
                                    }
                                }
                            }
                        }

                        keys = newKeys;
                        values = newValues;

                        // start hashing from the beginning
                        ind = Math.abs(Double.hashCode(key)) % keys.length;
                    }
                }
            }
        }

        public int get(double key) {
            int ind = Math.abs(Double.hashCode(key)) % keys.length;
            int firstInd = ind;
            while (true) {
                if (key == keys[ind]) {
                    return values[ind];
                } else {
                    ind = (ind + 1) % keys.length;
                    if (ind == firstInd || RRuntime.isNAorNaN(keys[ind])) {
                        return -1;
                    }
                }
            }
        }
    }

    public static class NonRecursiveHashMapInt {

        private int[] keys;
        private int[] values;

        public NonRecursiveHashMapInt(int approxCapacity) {
            keys = new int[Math.max(approxCapacity, 1)];
            Arrays.fill(keys, RRuntime.INT_NA);
            values = new int[Math.max(approxCapacity, 1)];
        }

        public boolean put(int key, int value) {
            int ind = Math.abs(Integer.hashCode(key)) % keys.length;
            int firstInd = ind;
            while (true) {
                if (RRuntime.isNA(keys[ind])) {
                    keys[ind] = key;
                    values[ind] = value;
                    return false;
                } else if (key == keys[ind]) {
                    values[ind] = value;
                    return true;
                } else {
                    ind = (ind + 1) % keys.length;
                    if (ind == firstInd) {
                        // resize
                        int[] newKeys = new int[keys.length + keys.length / 2];
                        Arrays.fill(newKeys, RRuntime.INT_NA);
                        int[] newValues = new int[values.length + values.length / 2];
                        for (int i = 0; i < keys.length; i++) {
                            if (!RRuntime.isNA(keys[i])) {
                                int tmpInd = Math.abs(Integer.hashCode(keys[i])) % newKeys.length;
                                while (true) {
                                    if (RRuntime.isNA(newKeys[tmpInd])) {
                                        newKeys[tmpInd] = keys[i];
                                        newValues[tmpInd] = values[i];
                                        break;
                                    } else {
                                        assert keys[i] != newKeys[tmpInd];
                                        tmpInd = (tmpInd + 1) % newKeys.length;
                                    }
                                }
                            }
                        }

                        keys = newKeys;
                        values = newValues;

                        // start hashing from the beginning
                        ind = Math.abs(Integer.hashCode(key)) % keys.length;
                    }
                }
            }
        }

        public int get(int key) {
            int ind = Math.abs(Integer.hashCode(key)) % keys.length;
            int firstInd = ind;
            while (true) {
                if (key == keys[ind]) {
                    return values[ind];
                } else {
                    ind = (ind + 1) % keys.length;
                    if (ind == firstInd || RRuntime.isNA(keys[ind])) {
                        return -1;
                    }
                }
            }
        }
    }

    public static class NonRecursiveHashSet<VAL> {
        private NonRecursiveHashMap<VAL> map;

        public NonRecursiveHashSet(int approxCapacity) {
            map = new NonRecursiveHashMap<>(approxCapacity);
        }

        public boolean add(VAL value) {
            return map.put(value, 1);
        }

        public boolean contains(VAL value) {
            return map.get(value) == 1 ? true : false;
        }
    }

    public static class NonRecursiveHashSetInt {
        private NonRecursiveHashMapInt map;

        public NonRecursiveHashSetInt(int approxCapacity) {
            map = new NonRecursiveHashMapInt(approxCapacity);
        }

        public boolean add(int value) {
            return map.put(value, 1);
        }

        public boolean contains(int value) {
            return map.get(value) == 1 ? true : false;
        }
    }

    public static class NonRecursiveHashSetDouble {
        private NonRecursiveHashMapDouble map;

        public NonRecursiveHashSetDouble(int approxCapacity) {
            map = new NonRecursiveHashMapDouble(approxCapacity);
        }

        public boolean add(double value) {
            return map.put(value, 1);
        }

        public boolean contains(double value) {
            return map.get(value) == 1 ? true : false;
        }
    }

    public static void writeStderr(String s, boolean nl) {
        try {
            StdConnections.getStderr().writeString(s, nl);
        } catch (IOException ex) {
            // Very unlikely
            ConsoleHandler consoleHandler = RContext.getInstance().getConsoleHandler();
            consoleHandler.printErrorln("Error writing to stderr: " + ex.getMessage());
            consoleHandler.printErrorln(s);

        }
    }

    public static String toHexString(byte[] data) {
        StringBuffer sb = new StringBuffer();
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
}
