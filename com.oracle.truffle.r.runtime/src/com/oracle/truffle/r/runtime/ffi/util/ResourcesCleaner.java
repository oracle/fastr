/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * Provides one unified reference queue for any {@link java.lang.ref.WeakReference}s holding to a
 * resource that needs to be cleaned-up.
 */
public class ResourcesCleaner {

    /**
     * Defines a method that is called once the object is pooled from the reference queue.
     * Preferable method is to sublcass {@link ReleasableWeakReference}.
     */
    public interface Releasable {
        void release();
    }

    /**
     * WeakReference with a method that is called once the weak reference is pooled from the
     * reference queue.
     */
    public abstract static class ReleasableWeakReference<T> extends WeakReference<T> implements Releasable {
        public ReleasableWeakReference(T referent) {
            super(referent, nativeReferenceQueue());
        }
    }

    /**
     * Gets reference queue that should be passed to the {@link java.lang.ref.WeakReference}
     * constructor. This method is PE safe.
     */
    public static ReferenceQueue<Object> nativeReferenceQueue() {
        initNativeRefQueueThread();
        return nativeRefQueue;
    }

    private static final ReferenceQueue<Object> nativeRefQueue = new ReferenceQueue<>();
    private static final AtomicReference<Thread> nativeRefQueueThread = new AtomicReference<>(null);

    private static void initNativeRefQueueThread() {
        Thread thread = nativeRefQueueThread.get();
        if (thread == null) {
            // One off initialization: exclude from PE
            CompilerDirectives.transferToInterpreter();
            createNativeRefQueueThread();
        }
    }

    @TruffleBoundary
    private static void createNativeRefQueueThread() {
        Thread thread;
        thread = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    while (true) {
                                        Reference<?> ref = nativeRefQueue.remove();
                                        if (ref instanceof Releasable) {
                                            try {
                                                ((Releasable) ref).release();
                                            } catch (Throwable ex) {
                                                assert false : getDebugInfo(ex);
                                            }
                                        }
                                    }
                                } catch (InterruptedException ex) {
                                    // TODO: some check that no FastR context is indeed active
                                    // anymore
                                }
                            }
                        },
                        "Native-Reference-Queue-Worker");
        if (nativeRefQueueThread.compareAndSet(null, thread)) {
            thread.setDaemon(true);
            thread.start();
        }
    }

    private static String getDebugInfo(Throwable ex) {
        try {
            StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);
            ex.printStackTrace(printer);
            return ex.getMessage() + "\n\n" + writer.getBuffer();
        } catch (Throwable newEx) {
            return "Exception during retrieval of the original exception message and stack trace";
        }
    }
}
