/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#ifndef TRIVIAL_CLASS_HPP_
#define TRIVIAL_CLASS_HPP_

#include <R.h>
#include <Rinternals.h>
#include <R_ext/Altrep.h>
#include <vector>


/**
 * Simplest possible altrep class that does not have any data. To Dataptr it returns an address
 * to statically-allocated buffer.
 */
class TrivialClass {
public:
    static SEXP createInstance();

private:
    static constexpr size_t m_size = 5;
    static const int m_content[m_size];

    static R_xlen_t Length(SEXP instance);
    static void * Dataptr(SEXP instance, Rboolean writeabble);
    static int Elt(SEXP instance, R_xlen_t idx);
};

#endif //TRIVIAL_CLASS_HPP_