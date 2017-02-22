/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.casts;

import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;

/**
 * Represents mapping used in {@link MapStep}.
 */
public abstract class Mapper<T, R> {

    public abstract <D> D accept(MapperVisitor<D> visitor, D previous);

    public interface MapperVisitor<D> {
        D visit(MapToValue<?, ?> mapper, D previous);

        D visit(MapByteToBoolean mapper, D previous);

        D visit(MapDoubleToInt mapper, D previous);

        D visit(MapToCharAt mapper, D previous);
    }

    public static final class MapToValue<T, R> extends Mapper<T, R> {
        private final Object value;

        public MapToValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public <D> D accept(MapperVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    public static final class MapByteToBoolean extends Mapper<Byte, Boolean> {

        public static final MapByteToBoolean INSTANCE = new MapByteToBoolean();

        private MapByteToBoolean() {

        }

        @Override
        public <D> D accept(MapperVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    public static final class MapDoubleToInt extends Mapper<Double, Integer> {

        public static final MapDoubleToInt INSTANCE = new MapDoubleToInt();

        private MapDoubleToInt() {
        }

        @Override
        public <D> D accept(MapperVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    public static final class MapToCharAt extends Mapper<String, Integer> {
        private final int index;
        private final int defaultValue;

        public MapToCharAt(int index, int defaultValue) {
            this.index = index;
            this.defaultValue = defaultValue;
        }

        public int getIndex() {
            return index;
        }

        public int getDefaultValue() {
            return defaultValue;
        }

        @Override
        public <D> D accept(MapperVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }
}
