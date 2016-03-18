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
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

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

    /*
     * Used to mediate access to the semaphore instances
     */
    private static final Semaphore create = new Semaphore(1, true);

    private final ArrayBlockingQueue<Object> masterToClient = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final ArrayBlockingQueue<Object> clientToMaster = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    public static int createChannel(int key) {
        if (key == 0) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "channel's key must be non-zero");
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
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "channel with specified id does not exist");
            }
            keys[actualId] = 0;
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

    private static class SerializedList {

        private RList list;

        SerializedList(RList list) {
            this.list = list;
        }

        public RList getList() {
            return list;
        }
    }

    private static void makeShared(Object o) {
        if (o instanceof RShareable) {
            RShareable shareable = (RShareable) o;
            shareable.makeSharedPermanent();
        }
    }

    @TruffleBoundary
    private static Object convertListAttributesToPrivate(RList l, Object shareableList) throws IOException {
        RAttributes attr = l.getAttributes();
        RAttributes newAttr = createShareableSlow(attr);
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
            // shareable attributes are the same - no need for any changes
            return shareableList;
        }
    }

    @TruffleBoundary
    private static Object convertObjectAttributesToPrivate(Object msg) throws IOException {
        RAttributable attributable = (RAttributable) msg;
        RAttributes attr = attributable.getAttributes();
        RAttributes newAttr = createShareableSlow(attr);
        if (attributable instanceof RAbstractVector) {
            attributable = ((RAbstractVector) msg).copy();
        }
        // see convertListAttributesToPrivate() why it is OK to use initAttributes() here
        attributable.initAttributes(newAttr);
        return attributable;
    }

    private static Object convertPrivateList(Object msg) throws IOException {
        RList l = (RList) msg;
        Object newMsg = createShareable(l);
        if (l.getAttributes() != null) {
            return convertListAttributesToPrivate(l, newMsg);
        } else {
            return newMsg;
        }
    }

    private static boolean serializeObject(Object o) {
        return o instanceof RFunction || o instanceof REnvironment || o instanceof RConnection || o instanceof RLanguage || o instanceof RPromise;
    }

    private static Object convertPrivate(Object o) throws IOException {
        if (o instanceof RList) {
            return convertPrivateList(o);
        } else if (!serializeObject(o)) {
            // we need to make internal values (permanently) shared to avoid updates to ref count
            // by different threads
            makeShared(o);
            if (o instanceof RAttributable && ((RAttributable) o).getAttributes() != null) {
                return convertObjectAttributesToPrivate(o);
            } else {
                return o;
            }
        } else {
            return RSerialize.serialize(o, RSerialize.XDR, RSerialize.DEFAULT_VERSION, null);
        }
    }

    private static Object createShareable(RList list) throws IOException {
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
        return list == newList ? list : new SerializedList(newList);
    }

    /*
     * To break recursion within Truffle boundary
     */
    @TruffleBoundary
    private static Object createShareableSlow(RList list) throws IOException {
        return createShareable(list);
    }

    @TruffleBoundary
    private static RAttributes createShareableSlow(RAttributes attr) throws IOException {
        RAttributes newAttr = attr;
        for (RAttribute a : attr) {
            Object val = a.getValue();
            Object newVal = convertPrivate(val);
            if (val != newVal) {
                // conversion happened update element
                if (attr == newAttr) {
                    // create a shallow copy
                    newAttr = attr.copy();
                }
                newAttr.put(a.getName(), newVal);
            }
        }

        return newAttr;
    }

    public static void send(int id, Object data) {
        Object msg = data;
        RChannel channel = getChannelFromId(id);
        if (msg instanceof RList) {
            try {
                msg = convertPrivateList(msg);
            } catch (IOException x) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error creating shareable list");
            }
        } else if (!serializeObject(msg)) {
            // make sure that what's passed through the channel will be copied on the first
            // update
            makeShared(msg);
            try {
                if (msg instanceof RAttributable && ((RAttributable) msg).getAttributes() != null) {
                    msg = convertObjectAttributesToPrivate(msg);
                }
            } catch (IOException x) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error creating channel message");
            }
        } else {
            msg = RSerialize.serialize(msg, RSerialize.XDR, RSerialize.DEFAULT_VERSION, null);
        }
        try {
            (id > 0 ? channel.masterToClient : channel.clientToMaster).put(msg);
        } catch (InterruptedException x) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error sending through the channel");
        }
    }

    private static Object unserializeObject(Object el) throws IOException {
        Object ret;
        if (el instanceof SerializedList) {
            RList elList = ((SerializedList) el).getList();
            unserializeList(elList);
            ret = elList;
        } else if (el instanceof byte[]) {
            ret = RSerialize.unserialize((byte[]) el, null, null, null);
        } else {
            ret = el;
        }
        if (ret instanceof RAttributable && ((RAttributable) ret).getAttributes() != null) {
            unserializeAttributes(((RAttributable) ret).getAttributes());
        }
        return ret;
    }

    @TruffleBoundary
    private static void unserializeList(RList list) throws IOException {
        for (int i = 0; i < list.getLength(); i++) {
            Object el = list.getDataAt(i);
            Object newEl = unserializeObject(el);
            if (newEl != el) {
                list.updateDataAt(i, newEl, null);
            }
        }
    }

    @TruffleBoundary
    private static void unserializeAttributes(RAttributes attr) throws IOException {
        for (RAttribute a : attr) {
            Object val = a.getValue();
            Object newVal = unserializeObject(val);
            if (newVal != val) {
                // TODO: this is a bit brittle as it relies on the iterator to work correctly in the
                // face of updates (which it does under current implementation of attributes)
                attr.put(a.getName(), newVal);
            }
        }
    }

    private static Object processedReceivedMessage(Object msg) {
        try {
            if (msg instanceof SerializedList) {
                RList list = ((SerializedList) msg).getList();
                // list and attributes are already private (shallow copies - do the appropriate
                // changes in place)
                unserializeList(list);
                if (list.getAttributes() != null) {
                    unserializeAttributes(list.getAttributes());
                }
                return list;
            } else if (msg instanceof byte[]) {
                return RSerialize.unserialize((byte[]) msg, null, null, null);
            } else {
                if (msg instanceof RAttributable && ((RAttributable) msg).getAttributes() != null) {
                    unserializeAttributes(((RAttributable) msg).getAttributes());
                }
                return msg;
            }
        } catch (IOException x) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error unserializing msg from the channel");
        }
    }

    public static Object receive(int id) {
        RChannel channel = getChannelFromId(id);
        try {
            Object msg = (id < 0 ? channel.masterToClient : channel.clientToMaster).take();
            return processedReceivedMessage(msg);
        } catch (InterruptedException x) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "error receiving from the channel");
        }
    }

    public static Object poll(int id) {
        RChannel channel = getChannelFromId(id);
        Object msg = (id < 0 ? channel.masterToClient : channel.clientToMaster).poll();
        if (msg != null) {
            return processedReceivedMessage(msg);
        }
        return null;
    }
}
