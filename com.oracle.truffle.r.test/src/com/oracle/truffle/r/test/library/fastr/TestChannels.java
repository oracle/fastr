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
package com.oracle.truffle.r.test.library.fastr;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestChannels extends TestBase {

    @Test
    public void testChannels() {
        // test remote updated of atomic vector (must stay private)
        assertEvalFastR("{ ch <- fastr.channel.create(1L); cx <- fastr.context.create(\"SHARED_NOTHING\"); fastr.context.spawn(cx, \"ch <- fastr.channel.get(1L); x<-fastr.channel.receive(ch); x[1]<-7; fastr.channel.send(ch, x)\"); y<-c(42); fastr.channel.send(ch, y); x<-fastr.channel.receive(ch); fastr.context.join(cx); fastr.channel.close(ch); print(c(x,y)) }",
                        "print(c(7L, 42L))");
        // test remote update of a list containing atomic vector (must stay private)
        assertEvalFastR("{ ch <- fastr.channel.create(1L); cx <- fastr.context.create(\"SHARED_NOTHING\"); fastr.context.spawn(cx, \"ch <- fastr.channel.get(1L); x<-fastr.channel.receive(ch); x[1][1]<-7; fastr.channel.send(ch, x)\"); y<-list(c(42)); fastr.channel.send(ch, y); x<-fastr.channel.receive(ch); fastr.context.join(cx); fastr.channel.close(ch); print(c(x,y)) }",
                        "print(list(7L, 42L))");
        // test sending a function
        assertEvalFastR("{ ch <- fastr.channel.create(1L); cx <- fastr.context.create(\"SHARED_NOTHING\"); fastr.context.spawn(cx, \"ch <- fastr.channel.get(1L); f<-fastr.channel.receive(ch); x<-f(7); fastr.channel.send(ch, x)\"); mul<-function(y) y*y; fastr.channel.send(ch, mul); x<-fastr.channel.receive(ch); fastr.context.join(cx); fastr.channel.close(ch); x }",
                        "49");
        // test sending global environment and assigning it remotely (should assign remotely but not
        // locally)
        assertEvalFastR("{ ch <- fastr.channel.create(1L); cx <- fastr.context.create(\"SHARED_NOTHING\"); fastr.context.spawn(cx, \"ch <- fastr.channel.get(1L); env<-fastr.channel.receive(ch); assign('y', 7, pos=env); fastr.channel.send(ch, y)\"); fastr.channel.send(ch, .GlobalEnv); x<-fastr.channel.receive(ch); fastr.context.join(cx); fastr.channel.close(ch); list(x, exists('y')) }",
                        "list(7, FALSE)");
        // test sending global environment as an attribute and assigning it remotely (should assign
        // remotely but not locally)
        assertEvalFastR("{ ch <- fastr.channel.create(1L); cx <- fastr.context.create(\"SHARED_NOTHING\"); fastr.context.spawn(cx, \"ch <- fastr.channel.get(1L); msg<-fastr.channel.receive(ch); env<-attr(msg, 'GLOBAL'); assign('y', 7, pos=env); fastr.channel.send(ch, y)\"); l<-list(c(42)); attr(l, 'GLOBAL')<-.GlobalEnv; fastr.channel.send(ch, l); x<-fastr.channel.receive(ch); fastr.context.join(cx); fastr.channel.close(ch); list(x, exists('y')) }",
                        "list(7, FALSE)");
        // test sending global environment as a nested attribute (an attribute of a list which is an
        // attribute of another list) and assigning it remotely (should assign remotely but not
        // locally)
        assertEvalFastR("{ ch <- fastr.channel.create(1L); cx <- fastr.context.create(\"SHARED_NOTHING\"); fastr.context.spawn(cx, \"ch <- fastr.channel.get(1L); msg<-fastr.channel.receive(ch); env<-attr(attr(msg, 'LIST'), 'GLOBAL'); assign('y', 7, pos=env); fastr.channel.send(ch, y)\"); l2<-list(c(42)); l<-list(c(7)); attr(l, 'GLOBAL')<-.GlobalEnv; attr(l2, 'LIST')<-l; fastr.channel.send(ch, l2); x<-fastr.channel.receive(ch); fastr.context.join(cx); fastr.channel.close(ch); list(x, exists('y')) }",
                        "list(7, FALSE)");

    }
}
