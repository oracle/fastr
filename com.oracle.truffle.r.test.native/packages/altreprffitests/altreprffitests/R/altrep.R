# Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

PACKAGE_NAME <- "altreprffitests"

is.altrep <- function(x) {
    .Call("is_altrep", x)
}

altrep.get_data1 <- function(x) {
    stopifnot(is.altrep(x))
    .Call("altrep_get_data1", x)
}

altrep.get_data2 <- function(x) {
    stopifnot(is.altrep(x))
    .Call("altrep_get_data2", x)
}

# Wrapper for INTEGER_NO_NA, REAL_NO_NA, etc..
no_na <- function(x) {
    if (typeof(x) == "integer") {
        .Call("integer_no_na", x)
    } else if (typeof(x) == "double") {
        .Call("real_no_na", x)
    } else {
        stop("Type of ", x, " does not have C function equivalent yet")
    }
}

is_sorted <- function(x) {
    if (typeof(x) == "complex" || typeof(x) == "raw") {
        return (FALSE)
    }
    .Call("is_sorted", x)
}

trivial_class.create_instance <- function() {
    .Call("trivial_class_create_instance")
}

simple_vec_wrapper.create_instance <- function(data,
                                               gen.Duplicate=F,
                                               gen.Coerce=F,
                                               gen.Elt=F,
                                               gen.Sum=F,
                                               gen.Min=F,
                                               gen.Max=F,
                                               gen.Get_region=F,
                                               gen.Is_sorted=F
                                               )
{
    if (missing(data)) {
        stop("Missing data argument")
    }

    .Call("simple_vec_wrapper_create_instance", data, gen.Duplicate, gen.Coerce, gen.Elt,
        gen.Sum, gen.Min, gen.Max, gen.Get_region, gen.Is_sorted)
}

logging_vec_wrapper.create_instance <- function(data,
                                               gen.Duplicate=F,
                                               gen.Coerce=F,
                                               gen.Elt=F,
                                               gen.Sum=F,
                                               gen.Min=F,
                                               gen.Max=F,
                                               gen.Get_region=F,
                                               gen.Is_sorted=F
                                               )
{
    if (missing(data)) {
        stop("Missing data argument")
    }

    .Call("logging_vec_wrapper_create_instance", data, gen.Duplicate, gen.Coerce, gen.Elt,
        gen.Sum, gen.Min, gen.Max, gen.Get_region, gen.Is_sorted)
}

logging_vec_wrapper.was_Duplicate_called <- function(instance) {
    .Call("logging_vec_wrapper_was_method_called", instance, "Duplicate")
}

logging_vec_wrapper.was_Coerce_called <- function(instance) {
    .Call("logging_vec_wrapper_was_method_called", instance, "Coerce")
}

logging_vec_wrapper.was_Elt_called <- function(instance) {
    .Call("logging_vec_wrapper_was_method_called", instance, "Elt")
}

logging_vec_wrapper.was_Sum_called <- function(instance) {
    .Call("logging_vec_wrapper_was_method_called", instance, "Sum")
}

logging_vec_wrapper.was_Min_called <- function(instance) {
    .Call("logging_vec_wrapper_was_method_called", instance, "Min")
}

logging_vec_wrapper.was_Max_called <- function(instance) {
    .Call("logging_vec_wrapper_was_method_called", instance, "Max")
}

logging_vec_wrapper.was_Get_region_called <- function(instance) {
    .Call("logging_vec_wrapper_was_method_called", instance, "Get_region")
}

logging_vec_wrapper.was_Is_sorted_called <- function(instance) {
    .Call("logging_vec_wrapper_was_method_called", instance, "Is_sorted")
}

logging_vec_wrapper.clear_called_methods <- function() {
    .Call("logging_vec_wrapper_clear_called_methods")
}

native_mem_vec.create_instance <- function(data_length) {
    .Call("native_mem_vec_create_instance", data_length)
}

native_mem_vec.delete_instance <- function(instance) {
    .Call("native_mem_vec_delete_instance", instance)
}

generator_class.new <- function(data_length, fn) {
    stopifnot( is.symbol(fn))
    stopifnot( length(data_length) == 1)
    .Call("generator_class_new", as.integer(data_length), fn, parent.frame())
}

first_char_changer_class.new <- function(char_vec, replace_char) {
    .Call("first_char_changer_class_new", char_vec, replace_char)
}
