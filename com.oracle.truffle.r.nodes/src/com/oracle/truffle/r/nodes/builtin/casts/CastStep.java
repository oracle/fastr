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
package com.oracle.truffle.r.nodes.builtin.casts;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.PipelineConfigBuilder;
import com.oracle.truffle.r.runtime.RType;

/**
 * Represents a single step in the cast pipeline.
 */
public abstract class CastStep {

    private CastStep next;

    public final CastStep getNext() {
        return next;
    }

    public final void setNext(CastStep next) {
        this.next = next;
    }

    public abstract <T> T accept(CastStepVisitor<T> visitor);

    public interface CastStepVisitor<T> {
        T visit(PipelineConfStep step);

        T visit(FindFirstStep step);

        T visit(AsVectorStep step);

        T visit(MapStep step);

        T visit(MapIfStep step);

        T visit(FilterStep step);

        T visit(NotNAStep step);
    }

    public static class PipelineConfStep extends CastStep {
        private final PipelineConfigBuilder pcb;
        // TODO??: just remember from the builder: boolean acceptNull, boolean acceptMissing,
        // defaultError?, ...

        public PipelineConfStep(PipelineConfigBuilder pcb) {
            this.pcb = pcb;
        }

        public PipelineConfigBuilder getConfigBuilder() {
            return pcb;
        }

        @Override
        public <T> T accept(CastStepVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class NotNAStep extends CastStep {
        @Override
        public <T> T accept(CastStepVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FindFirstStep extends CastStep {
        private final Object defaultValue;
        private final Class<?> elementClass;

        public FindFirstStep(Object defaultValue, Class<?> elementClass) {
            this.defaultValue = defaultValue;
            this.elementClass = elementClass;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public Class<?> getElementClass() {
            return elementClass;
        }

        @Override
        public <T> T accept(CastStepVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class AsVectorStep extends CastStep {
        private final RType type;

        public AsVectorStep(RType type) {
            assert type.isVector() && type != RType.List : "AsVectorStep supports only vector types minus list.";
            this.type = type;
        }

        public RType getType() {
            return type;
        }

        @Override
        public <T> T accept(CastStepVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MapStep extends CastStep {
        @Override
        public <T> T accept(CastStepVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MapIfStep extends CastStep {
        @Override
        public <T> T accept(CastStepVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FilterStep extends CastStep {
        private final Filter filter;

        public FilterStep(Filter filter) {
            this.filter = filter;
        }

        public Filter getFilter() {
            return filter;
        }

        @Override
        public <T> T accept(CastStepVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
