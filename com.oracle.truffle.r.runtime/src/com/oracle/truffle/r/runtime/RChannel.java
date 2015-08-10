/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.*;

import com.oracle.truffle.r.runtime.data.*;

/**
 * Implementation of a channel abstraction used for communication between parallel contexts in
 * shared memory space.
 */
public class RChannel {

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
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "channel's key must be non-zero");
        }
        try {
            create.acquire();
            while (true) {
                int freeSlot = -1;
                // start from one as we need slots that have distinguishable positive and negative
                // value
                for (int i = 1; i < keys.length; i++) {
                    if (keys[i] == key) {
                        create.release();
                        throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "channel with specified key already exists");
                    }
                    if (keys[i] == 0 && freeSlot == -1) {
                        freeSlot = i;
                    }

                }
                if (freeSlot != -1) {
                    keys[freeSlot] = key;
                    channels[freeSlot] = new RChannel();
                    create.release();
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
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "error creating a channel");
        }
    }

    public static int getChannel(int key) {
        try {
            create.acquire();
            for (int i = 1; i < keys.length; i++) {
                if (keys[i] == key) {
                    create.release();
                    return -i;
                }
            }
            create.release();
        } catch (InterruptedException x) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "error accessing channel");
        }
        throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "channel does not exist");
    }

    public static void closeChannel(int id) {
        int actualId = Math.abs(id);
        try {
            create.acquire();
            if (actualId == 0 || actualId >= channels.length || channels[actualId] == null) {
                throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "channel with specified id does not exist");
            }
            keys[actualId] = 0;
            channels[actualId] = null;
            create.release();
        } catch (InterruptedException x) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "error closing channel");
        }
    }

    private static RChannel getChannelFromId(int id) {
        int actualId = Math.abs(id);
        try {
            create.acquire();
            if (actualId == 0 || actualId >= channels.length || channels[actualId] == null) {
                throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "channel with specified id does not exist");
            }
            RChannel channel = channels[actualId];
            create.release();
            return channel;
        } catch (InterruptedException x) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "error transmitting through channel");
        }
    }

    public static void send(int id, Object data) {
        RChannel channel = getChannelFromId(id);
        if (data instanceof RShareable) {
            // make sure that what's passed through the channel will be copied on the first update
            RShareable shareable = (RShareable) data;
            if (FastROptions.NewStateTransition) {
                shareable.incRefCount();
                shareable.incRefCount();
            } else {
                shareable.makeShared();
            }
        }
        try {
            (id > 0 ? channel.masterToClient : channel.clientToMaster).put(data);
        } catch (InterruptedException x) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "error sending through the channel");
        }
    }

    public static Object receive(int id) {
        RChannel channel = getChannelFromId(id);
        try {
            return (id < 0 ? channel.masterToClient : channel.clientToMaster).take();
        } catch (InterruptedException x) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "error receiving from the channel");
        }
    }

}
