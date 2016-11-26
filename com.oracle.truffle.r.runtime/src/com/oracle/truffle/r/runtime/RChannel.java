/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.REnvTruffleFrameAccess;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;

/**
 * Implementation of a channel abstraction used for communication between parallel contexts in
 * shared memory space.
 */
public class RChannel {

    // TODO: cheaper way of serializing data (re-usable buffer?)

    private static final int INITIAL_CHANNEL_NUM = 4;
    private static final int CHANNEL_NUM_GROW_FACTOR = 2;
    private static final int QUEUE_CAPACITY = 1;

    private static int[] keys = new int[INITIAL_CHANNEL_NUM];
    private static RChannel[] channels = new RChannel[INITIAL_CHANNEL_NUM];

    private static final int CLOSED_CHANNEL_KEY = -1;

    /*
     * Used to mediate access to the semaphore instances
     */
    private static final Semaphore create = new Semaphore(1, true);

    private final ArrayBlockingQueue<Object> masterToClient = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final ArrayBlockingQueue<Object> clientToMaster = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    public static int createChannel(int key) {
        if (key <= 0) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "channel's key must be positive");
        }
        try {
            create.acquire();
            while (true) {
                int freeSlot = -1;
                // start from one as we need slots that have distinguishable positive and negative
                // value
                for (int i = 1; i < keys.length; i++) {
                    if (keys[i] == key) {
                        throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "channel with specified key already exists");
                    }
                    if (keys[i] == 0 && freeSlot == -1) {
                        freeSlot = i;
                    }

                }
                if (freeSlot != -1) {
                    keys[freeSlot] = key;
                    channels[freeSlot] = new RChannel();
                    return freeSlot;
                } else {
                    int[] keysTmp = new int[keys.length * CHANNEL_NUM_GROW_FACTOR];
                    RChannel[] channelsTmp = new RChannel[channels.length * CHANNEL_NUM_GROW_FACTOR];
                    for (int i = 1; i < keys.length; i++) {
                        keysTmp[i] = keys[i];
                        channelsTmp[i] = channels[i];
                    }
                    keys = keysTmp;
                    channels = channelsTmp;
                }
            }
        } catch (InterruptedException x) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error creating a channel");
        } finally {
            create.release();
        }
    }

    public static int getChannel(int key) {
        try {
            create.acquire();
            for (int i = 1; i < keys.length; i++) {
                if (keys[i] == key) {
                    return -i;
                }
            }
        } catch (InterruptedException x) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error accessing channel");
        } finally {
            create.release();
        }
        throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "channel does not exist");
    }

    public static void closeChannel(int id) {
        int actualId = Math.abs(id);
        try {
            create.acquire();
            if (actualId == 0 || actualId >= channels.length || channels[actualId] == null) {
                // closing an already closed channel does not necessarily have to be an error (and
                // makes parallell package's worker script work unchanged)
                if (keys[actualId] != CLOSED_CHANNEL_KEY) {
                    throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "channel with specified id does not exist");
                }
            }
            keys[actualId] = CLOSED_CHANNEL_KEY;
            channels[actualId] = null;
        } catch (InterruptedException x) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error closing channel");
        } finally {
            create.release();
        }
    }

    private static RChannel getChannelFromId(int id) {
        int actualId = Math.abs(id);
        try {
            create.acquire();
            if (actualId == 0 || actualId >= channels.length || channels[actualId] == null) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "channel with specified id does not exist");
            }
            RChannel channel = channels[actualId];
            return channel;
        } catch (InterruptedException x) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error transmitting through channel");
        } finally {
            create.release();
        }
    }

    public static void send(int id, Object data) {
        Output out = new Output();
        Object msg = out.processOutgoingMessage(data);
        RChannel channel = getChannelFromId(id);
        try {
            (id > 0 ? channel.masterToClient : channel.clientToMaster).put(msg);
        } catch (InterruptedException x) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error sending through the channel");
        }
    }

    public static Object receive(int id) {
        RChannel channel = getChannelFromId(id);
        try {
            Object msg = (id < 0 ? channel.masterToClient : channel.clientToMaster).take();
            Input in = new Input();
            return in.processedReceivedMessage(msg);
        } catch (InterruptedException x) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error receiving from the channel");
        }
    }

    public static Object poll(int id) {
        RChannel channel = getChannelFromId(id);
        Object msg = (id < 0 ? channel.masterToClient : channel.clientToMaster).poll();
        if (msg != null) {
            Input in = new Input();
            return in.processedReceivedMessage(msg);
        }
        return null;
    }

    private static class TransmitterCommon extends RSerialize.RefCounter {

        protected static class SerializedRef {
            private final int index;

            public SerializedRef(int index) {
                this.index = index;
            }

            public int getIndex() {
                return index;
            }
        }

        protected static class SerializedList {

            private final RList list;

            SerializedList(RList list) {
                this.list = list;
            }

            public RList getList() {
                return list;
            }
        }

        protected static class SerializedEnv {

            public static class Bindings {
                private final String[] names;
                private final Object[] values;

                Bindings(String[] names, Object[] values) {
                    this.names = names;
                    this.values = values;
                }
            }

            private Bindings bindings;
            // parent can be SerializedEnv or byte[]
            private Object parent;
            private DynamicObject attributes;

            SerializedEnv(Bindings bindings, Object parent, DynamicObject attributes) {
                this.bindings = bindings;
                this.parent = parent;
                this.attributes = attributes;
            }

            public String[] getNames() {
                return bindings.names;
            }

            public Object[] getValues() {
                return bindings.values;
            }

            public Object getParent() {
                return parent;
            }

            public DynamicObject getAttributes() {
                return attributes;
            }
        }

        protected static class SerializedPromise {

            private final Object env;
            private final Object value;
            private final RSyntaxElement serializedExpr;

            public SerializedPromise(Object env, Object value, RSyntaxElement serializedExpr) {
                this.env = env;
                this.value = value;
                this.serializedExpr = serializedExpr;
            }

            public Object getEnv() {
                return env;
            }

            public Object getValue() {
                return value;
            }

            public RSyntaxElement getSerializedExpr() {
                return serializedExpr;
            }

        }

        protected static class SerializedFunction {
            private final DynamicObject attributes;
            private final Object env;
            private final RFunction serializedDef;

            public SerializedFunction(DynamicObject attributes, Object env, RFunction serializedDef) {
                this.attributes = attributes;
                this.env = env;
                this.serializedDef = serializedDef;
            }

            public DynamicObject getAttributes() {
                return attributes;
            }

            public Object getEnv() {
                return env;
            }

            public RFunction getSerializedDef() {
                return serializedDef;
            }
        }

        protected static class SerializedAttributable {

            private final DynamicObject attributes;
            private final byte[] serializedAttributable;

            public SerializedAttributable(DynamicObject attributes, byte[] serializedAttributable) {
                this.attributes = attributes;
                this.serializedAttributable = serializedAttributable;
            }

            public DynamicObject getAttributes() {
                return attributes;
            }

            public byte[] getSerializedAttributable() {
                return serializedAttributable;
            }

        }
    }

    private static class Output extends TransmitterCommon {

        private static Object makeShared(Object o) {
            if (o instanceof RShareable) {
                RShareable shareable = (RShareable) o;
                shareable.makeSharedPermanent();
            }
            return o;
        }

        @TruffleBoundary
        private Object convertListAttributesToPrivate(RList l, Object shareableList) throws IOException {
            DynamicObject attr = l.getAttributes();
            DynamicObject newAttr = createShareableSlow(attr, false);
            if (newAttr != attr) {
                RList newList;
                if (shareableList == l) {
                    // need to create a copy due to different attributes
                    newList = (RList) l.copy();
                } else {
                    newList = ((SerializedList) shareableList).getList();
                }
                // it's OK to use initAttributes() as the shape of the list (that
                // could have otherwise been modified by setting attributes, such as dim
                // attribute) is already set correctly by the copy operation
                newList.initAttributes(newAttr);
                return newList;
            } else {
                if (shareableList != l) {
                    // shareableList is "fresh" (not shared) and needs its own attribute object
                    ((SerializedList) shareableList).getList().initAttributes(newAttr);
                } // else list and attribute objects haven't changed
                return shareableList;
            }
        }

        @TruffleBoundary
        private Object convertObjectAttributesToPrivate(Object msg) throws IOException {
            RAttributable attributable = (RAttributable) msg;
            DynamicObject attr = attributable.getAttributes();
            DynamicObject newAttr = createShareableSlow(attr, false);
            if (newAttr != attr && attributable instanceof RShareable) {
                attributable = (RAttributable) ((RShareable) msg).copy();
            }
            // see convertListAttributesToPrivate() why it is OK to use initAttributes() here
            attributable.initAttributes(newAttr);
            return attributable;
        }

        private Object convertPrivateList(Object msg) throws IOException {
            RList l = (RList) msg;
            Object newMsg = createShareable(l);
            if (l.getAttributes() != null) {
                return convertListAttributesToPrivate(l, newMsg);
            } else {
                return newMsg;
            }
        }

        private Object convertPrivateEnv(Object msg) throws IOException {
            int refInd = getRefIndex(msg);
            if (refInd != -1) {
                return new SerializedRef(refInd);
            } else {
                addReadRef(msg);
            }
            REnvironment env = (REnvironment) msg;
            DynamicObject attributes = env.getAttributes();
            if (attributes != null) {
                attributes = createShareableSlow(attributes, true);
            }
            SerializedEnv.Bindings bindings = createShareable(env);

            return new SerializedEnv(bindings, convertPrivateSlow(env.getParent()), attributes);
        }

        private SerializedPromise convertPrivatePromise(Object msg) throws IOException {
            RPromise p = (RPromise) msg;
            if (p.isEvaluated()) {
                return new SerializedPromise(RNull.instance, p.getValue(), p.getClosure().getExpr().asRSyntaxNode());
            } else {
                REnvironment env = p.getFrame() == null ? REnvironment.globalEnv() : REnvironment.frameToEnvironment(p.getFrame());
                return new SerializedPromise(convertPrivate(env), RUnboundValue.instance, p.getClosure().getExpr().asRSyntaxNode());
            }

        }

        private SerializedFunction convertPrivateFunction(Object msg) throws IOException {
            RFunction fn = (RFunction) msg;
            Object env = convertPrivate(REnvironment.frameToEnvironment(fn.getEnclosingFrame()));
            DynamicObject attributes = fn.getAttributes();
            return new SerializedFunction(attributes == null ? null : createShareableSlow(attributes, true), env, fn);
        }

        private Object convertPrivateAttributable(Object msg) throws IOException {
            // do full serialization but handle attributes separately (no reason to serialize them
            // unconditionally)
            RAttributable attributable = (RAttributable) msg;
            DynamicObject attributes = attributable.getAttributes();
            if (attributes != null) {
                assert attributable instanceof RAttributeStorage;
                // TODO: we assume that the following call will reset attributes without clearing
                // them - should we define a new method to be used here?
                attributable.initAttributes(null);
            }
            byte[] serializedAttributable = RSerialize.serialize(attributable, RSerialize.XDR, RSerialize.DEFAULT_VERSION, null);
            if (attributes != null) {
                attributable.initAttributes(attributes);
                attributes = createShareableSlow(attributes, true);
            }
            return new SerializedAttributable(attributes, serializedAttributable);
        }

        private static boolean shareableEnv(Object o) {
            if (o instanceof REnvironment) {
                REnvironment env = (REnvironment) o;
                if (env == REnvironment.emptyEnv() || env == REnvironment.baseEnv() || env == REnvironment.globalEnv() || env == REnvironment.baseNamespaceEnv() || env.isPackageEnv() != null ||
                                env.isNamespaceEnv()) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }

        private static boolean serializeObject(Object o) {
            return o instanceof REnvironment || o instanceof RConnection || o instanceof RLanguage;
        }

        private Object convertPrivate(Object o) throws IOException {
            if (o instanceof RList) {
                return convertPrivateListSlow(o);
            } else if (shareableEnv(o)) {
                return convertPrivateEnv(o);
            } else if (o instanceof RPromise) {
                return convertPrivatePromise(o);
            } else if (o instanceof RFunction) {
                return convertPrivateFunction(o);
            } else if (!serializeObject(o)) {
                // we need to make internal values (permanently) shared to avoid updates to ref
                // count by different threads
                if (o instanceof RAttributable && ((RAttributable) o).getAttributes() != null) {
                    Object newObj = convertObjectAttributesToPrivate(o);
                    if (newObj == o) {
                        makeShared(o);
                    } // otherwise a copy has been created to store new attributes
                    return newObj;
                } else {
                    return makeShared(o);
                }
            } else {
                assert o instanceof RAttributable;
                return convertPrivateAttributable(o);
            }
        }

        private Object createShareable(RList list) throws IOException {
            RList newList = list;
            for (int i = 0; i < list.getLength(); i++) {
                Object el = list.getDataAt(i);
                Object newEl = convertPrivate(el);
                if (el != newEl) {
                    // conversion happened update element
                    if (list == newList) {
                        // create a shallow copy
                        newList = (RList) list.copy();
                    }
                    newList.updateDataAt(i, newEl, null);
                }
            }
            return list == newList ? makeShared(list) : new SerializedList(newList);
        }

        @TruffleBoundary
        private SerializedEnv.Bindings createShareable(REnvironment e) throws IOException {
            String[] names = REnvTruffleFrameAccess.getStringIdentifiers(e.getFrame().getFrameDescriptor());
            Object[] values = new Object[names.length];
            int ind = 0;
            for (String n : names) {
                values[ind++] = convertPrivate(e.get(n));
            }
            return new SerializedEnv.Bindings(names, values);
        }

        /*
         * To break recursion within Truffle boundary
         */
        @TruffleBoundary
        private Object convertPrivateSlow(Object o) throws IOException {
            return convertPrivate(o);
        }

        @TruffleBoundary
        private Object convertPrivateListSlow(Object msg) throws IOException {
            return convertPrivateList(msg);
        }

        @TruffleBoundary
        private DynamicObject createShareableSlow(DynamicObject attr, boolean forceCopy) throws IOException {
            DynamicObject newAttr = forceCopy ? RAttributesLayout.copy(attr) : attr;
            for (RAttributesLayout.RAttribute a : RAttributesLayout.asIterable(attr)) {
                Object val = a.getValue();
                Object newVal = convertPrivate(val);
                if (val != newVal) {
                    // conversion happened update element
                    if (attr == newAttr && !forceCopy) {
                        // create a shallow copy if not already created
                        newAttr = RAttributesLayout.copy(attr);
                    }
                    newAttr.define(a.getName(), newVal);
                }
            }

            return newAttr;
        }

        public Object processOutgoingMessage(Object data) {
            try {
                return convertPrivate(data);
            } catch (IOException x) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error serializing message for channel transmission");
            }
        }

    }

    private static class Input extends TransmitterCommon {

        private Object unserializeObject(Object el) throws IOException {
            Object ret = el;
            if (el instanceof SerializedRef) {
                ret = getReadRef(((SerializedRef) el).getIndex());
            } else {
                if (el instanceof SerializedList) {
                    RList elList = ((SerializedList) el).getList();
                    unserializeListSlow(elList);
                    ret = elList;
                } else if (el instanceof SerializedEnv) {
                    ret = unserializeEnv((SerializedEnv) el);
                } else if (el instanceof SerializedPromise) {
                    ret = unserializePromise((SerializedPromise) el);
                } else if (el instanceof SerializedFunction) {
                    ret = unserializeFunction((SerializedFunction) el);
                } else if (el instanceof SerializedAttributable) {
                    ret = unserializeAttributable((SerializedAttributable) el);
                }
                if (ret instanceof RAttributable && ((RAttributable) ret).getAttributes() != null) {
                    unserializeAttributes((RAttributable) ret);
                }
            }
            return ret;
        }

        private void unserializeList(RList list) throws IOException {
            for (int i = 0; i < list.getLength(); i++) {
                Object el = list.getDataAt(i);
                Object newEl = unserializeObject(el);
                if (newEl != el) {
                    list.updateDataAt(i, newEl, null);
                }
            }
        }

        @TruffleBoundary
        private REnvironment unserializeEnv(SerializedEnv e) throws IOException {
            Object[] values = e.getValues();
            String[] names = e.getNames();
            assert values.length == names.length;
            REnvironment.NewEnv env = RDataFactory.createNewEnv(null);
            addReadRef(env);
            int ind = 0;
            for (String n : names) {
                Object newValue = unserializeObject(values[ind++]);
                env.safePut(n, newValue);
            }
            REnvironment parent = (REnvironment) unserializeObject(e.getParent());
            RArguments.initializeEnclosingFrame(env.getFrame(), parent.getFrame());
            DynamicObject attributes = e.getAttributes();
            if (attributes != null) {
                env.initAttributes(attributes);
            }
            return env;
        }

        @TruffleBoundary
        private RPromise unserializePromise(SerializedPromise p) throws IOException {
            Closure closure = Closure.create(RContext.getASTBuilder().process(p.getSerializedExpr()).asRNode());
            if (p.getValue() == RUnboundValue.instance) {
                Object environment = p.getEnv();
                if (environment != RNull.instance) {
                    environment = unserializeObject(environment);
                }
                REnvironment env = environment == RNull.instance ? REnvironment.baseEnv() : (REnvironment) environment;
                return RDataFactory.createPromise(PromiseState.Explicit, closure, env.getFrame());
            } else {
                return RDataFactory.createEvaluatedPromise(closure, p.getValue());
            }
        }

        @TruffleBoundary
        private RFunction unserializeFunction(SerializedFunction f) throws IOException {
            RFunction fun = f.getSerializedDef();
            REnvironment env = (REnvironment) unserializeObject(f.getEnv());
            MaterializedFrame enclosingFrame = env.getFrame();
            HasSignature root = (HasSignature) fun.getTarget().getRootNode();
            RootCallTarget target = root.duplicateWithNewFrameDescriptor();
            FrameSlotChangeMonitor.initializeEnclosingFrame(target.getRootNode().getFrameDescriptor(), enclosingFrame);
            RFunction fn = RDataFactory.createFunction(fun.getName(), fun.getPackageName(), target, null, enclosingFrame);
            DynamicObject attributes = f.getAttributes();
            if (attributes != null) {
                assert fn.getAttributes() == null;
                // attributes unserialized in caller methods
                fn.initAttributes(attributes);
            }
            return fn;
        }

        @TruffleBoundary
        private static RAttributable unserializeAttributable(SerializedAttributable a) throws IOException {
            DynamicObject attributes = a.getAttributes();
            RAttributable attributable = (RAttributable) RSerialize.unserialize(a.getSerializedAttributable(), null, null, null);
            if (attributes != null) {
                assert attributable.getAttributes() == null;
                // attributes unserialized in caller methods
                attributable.initAttributes(attributes);
            }
            return attributable;
        }

        @TruffleBoundary
        private void unserializeListSlow(RList list) throws IOException {
            unserializeList(list);
        }

        @TruffleBoundary
        private void unserializeAttributes(RAttributable attributable) throws IOException {
            DynamicObject attr = attributable.getAttributes();
            for (RAttributesLayout.RAttribute a : RAttributesLayout.asIterable(attr)) {
                Object val = a.getValue();
                Object newVal = unserializeObject(val);
                if (newVal != val) {
                    // class attribute is a string vector which should be always shared
                    assert !a.getName().equals(RRuntime.CLASS_ATTR_KEY);
                    /*
                     * TODO: this is a bit brittle as it relies on the iterator to work correctly in
                     * the face of updates (which it does under current implementation of
                     * attributes)
                     */
                    attributable.setAttr(a.getName(), newVal);
                }
            }
        }

        public Object processedReceivedMessage(Object msg) {
            try {
                return unserializeObject(msg);
            } catch (IOException x) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error unserializing msg from the channel");
            }
        }

    }
}
