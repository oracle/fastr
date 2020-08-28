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

stopifnot(require(altreprffitests))

run_tests <- function(tests) {
    for (test in tests) {
        func_name <- test[[1]]
        func <- test[[2]]
        cat("[R] =============================================================================\n")
        cat("[R] Running ", func_name, "\n")
        func()
        cat("=============================================================================\n")
    }
}

#' Checks whether given altrep instance and underlying data are same.
#' Underlying data should be simple - eg. vector - which is a case for simplemmap.
#' 
#' @param instance ALTREP instance.
#' @param expected_data Underlying data for ALTREP instance. Optionally this can be
#'                      also another ALTREP instance.
#' 
check_equal <- function(instance, expected_data) {
    assert_equals <- function(val1, val2) {
        if (!identical(val1, val2)) {
            width <- 80L
            name <- substr(deparse(sys.call(), width)[[1L]], 1, width)
            stop("Fail: ", name, "with val1=", val1, ", val2=", val2, "\n")
        }
    }

    test_length <- function() {
        assert_equals( length(instance), length(expected_data))
    }

    test_elt <- function() {
        for (idx in 1:length(instance)) {
            assert_equals( instance[idx], expected_data[idx])
        }
    }

    test_sum <- function() {
        assert_equals( sum(instance), sum(expected_data))
        assert_equals( sum(instance, 1:6), sum(expected_data, 1:6))
        assert_equals( sum(1:6, instance), sum(1:6, expected_data))
    }

    test_min <- function() {
        assert_equals( min(instance), min(expected_data))
    }

    test_max <- function() {
        assert_equals( max(instance), max(expected_data))
    }

    test_coerce <- function() {
        # This test may cause many warnings of type: "Introduced NAs". We do not care about that.
        suppressWarnings(assert_equals( as.integer(instance), as.integer(expected_data)))
        suppressWarnings(assert_equals( as.double(instance), as.double(expected_data)))
    }

    test_is_unsorted <- function() {
        assert_equals( is.unsorted(instance), is.unsorted(expected_data))
        assert_equals( is.unsorted(instance, strictly=TRUE), is.unsorted(expected_data, strictly=TRUE))
    }

    test_is_sorted <- function() {
        stopifnot( is.logical(is_sorted(instance)))
        stopifnot( is.logical(is_sorted(expected_data)))
    }

    test_length()
    test_elt()
    test_coerce()
    if (!is.raw(instance) && !is.raw(expected_data)) {
        if (!is.character(instance) && !is.character(instance)) {
            test_sum()
        }
        if (!is.complex(instance) && !is.complex(expected_data)) {
            test_min()
            test_max()
            test_is_sorted()
        }
        test_is_unsorted()
    }
    return (TRUE)
}

#' Checks two instances for equality. One of these instances has certain altrep method
#' overriden, the second does not.
test_default_implementations <- function() {
    DATA_LIST <- list(
        # Integers
        1:10,
        c(2L, 5L, 6L, 1L, 2L),
        # Reals
        c(1,2,3,4,5,6,7,42,-10),
        # Logical
        c(TRUE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE),
        # Raw
        as.raw(c(1,2,3,4,56,99)),
        # String
        c("Hello", "World", "!", "How", " ", "are", " ", "you", "?")
    )

    for (param_name in c("gen.Duplicate", "gen.Coerce", "gen.Elt", "gen.Sum", "gen.Min", "gen.Max",
                         "gen.Get_region", "gen.Is_sorted"))
    {
        for (data in DATA_LIST) {
            params_with <- list(data, TRUE)
            names(params_with) <- c("", param_name)
            # gen.* = TRUE
            instance_with_method <- do.call(simple_vec_wrapper.create_instance, params_with)

            params_without <- list(data, FALSE)
            names(params_without) <- c("", param_name)
            # gen.* = FALSE
            instance_without_method <- do.call(simple_vec_wrapper.create_instance, params_without)

            check_equal(instance_with_method, instance_without_method)
        }
    }
}

#' Tests whether altrep methods (native functions) are successfully called in
#' appropriate situations.
#' 
#' An example of appropriate situation is when we have an altrep instance with
#' registered Sum method and we invoke `sum(instance)`, then Sum method should
#' be called rather than falling back to the default implementation.
#' 
#' Note that there are some special cases like `sum(instance1, instance2)`,
#' which for some unexplained reasons does not call Sum method of instance1
#' and Sum method of instance2 in GNU-R.
test_calls_to_altrep_methods <- function() {
    test_duplicate_called <- function() {
        logging_vec_wrapper.clear_called_methods()
        instance <- logging_vec_wrapper.create_instance(1:10, gen.Duplicate = TRUE)
        copied_instance <- instance
        if (typeof(copied_instance) == "integer") {
            # Force duplication
            copied_instance[[1L]] <- 42L
        }
        stopifnot( logging_vec_wrapper.was_Duplicate_called(instance))
    }

    test_sum_called <- function() {
        DATA_LIST <- list(
            as.integer(1:10),
            as.double(1:10)
        )
        for (data in DATA_LIST) {
            logging_vec_wrapper.clear_called_methods()
            instance <- logging_vec_wrapper.create_instance(data, gen.Sum = TRUE)
            sum(instance)
            stopifnot( logging_vec_wrapper.was_Sum_called(instance))
        }
    }

    test_max_called <- function() {
        for (data in list(as.integer(1:10), as.double(1:10))) {
            logging_vec_wrapper.clear_called_methods()
            instance <- logging_vec_wrapper.create_instance(data, gen.Max = TRUE)
            max(instance)
            stopifnot( logging_vec_wrapper.was_Max_called(instance))
        }
    }

    test_min_called <- function() {
        for (data in list(as.integer(1:10), as.double(1:10))) {
            logging_vec_wrapper.clear_called_methods()
            instance <- logging_vec_wrapper.create_instance(data, gen.Min = TRUE)
            min(instance)
            stopifnot( logging_vec_wrapper.was_Min_called(instance))
        }
    }

    test_duplicate_called()
    test_sum_called()
    test_max_called()
    test_min_called()
}

test_framework_behavior <- function() {
    test_after_resize <- function() {
        instance <- simple_vec_wrapper.create_instance(1:5)
        stopifnot( is.altrep(instance))
        # Resize should happen.
        instance[[60]] <- 42L
        # instance is obviously no longer altrep.
        stopifnot( !is.altrep(instance))
    }

    test_after_coerce <- function() {
        # instance is integer vector
        instance <- simple_vec_wrapper.create_instance(1:5)
        stopifnot( is.altrep(instance))
        # instance is coerced to double vector.
        instance[[1]] <- as.double(42)
        stopifnot( !is.altrep(instance))
    }

    test_after_resize()
    test_after_coerce()
}

test_trivial <- function() {
    expected_data <- as.integer(c(1, 2, 3, 4, 5))
    instance <- trivial_class.create_instance()
    stopifnot( is.altrep(instance))
    stopifnot( altrep.get_data1(instance) == NULL)
    stopifnot( altrep.get_data2(instance) == NULL)
    check_equal(instance, expected_data)
}

test_simple_altint <- function() {
    data <- 1:10
    instance <- simple_vec_wrapper.create_instance(data)
    check_equal(instance, data)
}

test_two_altints <- function() {
    data <- 1:15
    instance_1 <- simple_vec_wrapper.create_instance(data)
    instance_2 <- simple_vec_wrapper.create_instance(data)
    check_equal(instance_1, data)
    check_equal(instance_2, data)
    check_equal(instance_1, instance_2)
}

test_altreal <- function() {
    data <- as.double(c(1,2,3,4,5,6,7,8,9,10))
    instance <- simple_vec_wrapper.create_instance(data)
    check_equal(instance, data)
}

test_altlogical <- function() {
    data <- c(TRUE, FALSE, FALSE, TRUE, TRUE, FALSE)
    instance <- simple_vec_wrapper.create_instance(data)
    check_equal(instance, data)
}

test_two_altlogicals <- function() {
    data <- c(TRUE, FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE)
    instance_1 <- simple_vec_wrapper.create_instance(data)
    instance_2 <- simple_vec_wrapper.create_instance(data)
    check_equal(instance_1, data)
    check_equal(instance_2, data)
    check_equal(instance_1, instance_2)
}

test_altraw <- function() {
    data <- as.raw(c(42, 23, 15, 213, 156, 13, 20))
    instance <- simple_vec_wrapper.create_instance(data)
    check_equal(instance, data)
}

test_altcomplex <- function() {
    data <- complex(real=c(1,15,-6,4,10), imaginary=c(5,3,10,-6,-4))
    instance <- simple_vec_wrapper.create_instance(data)
    check_equal(instance, data)
}

test_altstring <- function() {
    data <- c("Hello", "World", "!")
    instance <- simple_vec_wrapper.create_instance(data)
    check_equal(instance, data)
}

test_two_altstrings <- function() {
    data <- c("Hello", "World", "!")
    instance_1 <- simple_vec_wrapper.create_instance(data)
    instance_2 <- simple_vec_wrapper.create_instance(data)
    check_equal(instance_1, data)
    check_equal(instance_2, data)
    check_equal(instance_1, instance_2)
}

test_mmap <- function() {
    stopifnot(require(simplemmap))
    # TODO: finish ...
    data <- runif(20)
    mmap_instance <- mmap(...)
    check_equal(mmap_instance, data)
}

#' See GeneratorClass.
test_generator_class <- function() {
    f <- function(idx) as.integer(idx + 1)
    fn <- as.symbol("f")
    LEN <- 10
    instance <- generator_class.new(LEN, fn)

    acc <- as.integer(0)
    for (i in 1:length(instance)) {
        # Every instance[[i]] calls f(i)
        acc <- acc + instance[[i]]
    }

    expected_acc <- sum(1:LEN + 1)
    stopifnot( acc == expected_acc)
}

test_first_char_changer_class <- function() {
    char_vec <- c("ahoj", "karle")
    instance <- first_char_changer_class.new(char_vec, "X")

    stopifnot( instance[[1]] == "Xhoj")
    stopifnot( instance[[2]] == "Xarle")
}

#' *_NO_NA functions returns FALSE as default, therefore it is possible that all of the
#' no_na calls return either FALSE or TRUE. 
#' This test may seem unnecessary, but it is in fact valuable to test that these
#' functions do not at least throw exceptions.
test_no_na <- function() {
    # Simple integer vectors.
    v1 <- c(1L, 2L, 5L)
    stopifnot( no_na(v1) == TRUE || no_na(v1) == FALSE)

    v2 <- c(1L, 2L, NA)
    stopifnot( no_na(v2) == TRUE || no_na(v2) == FALSE)

    # Simple real vectors.
    v3 <- c(1, 2, 3)
    stopifnot( no_na(v3) == TRUE || no_na(v3) == FALSE)

    v4 <- as.double(1:10)
    stopifnot( no_na(v4) == TRUE || no_na(v4) == FALSE)

    # Compact sequence is an altrep instance, but even as such are not required to
    # return TRUE even if there are no NA values.
    v5 <- 1:10
    stopifnot( no_na(v5) == TRUE || no_na(v5) == FALSE)

    # Modify an element of an altrep instance.
    v6 <- 1:15
    v6[[2]] <- NA
    stopifnot( no_na(v6) == TRUE || no_na(v6) == FALSE)
    v6[[2]] <- 42
    stopifnot( no_na(v6) == TRUE || no_na(v6) == FALSE)
}

temp_test <- function() {
    data <- as.integer(c(1,2,3,4,5,6,42))
    instance1 <- simple_vec_wrapper.create_instance(data, gen.Elt=FALSE)
    instance2 <- simple_vec_wrapper.create_instance(data, gen.Elt=FALSE)
    cat("Instance1 =", instance1, "\n")
    cat("Instance2 =", instance2, "\n")

    cat("is_sorted(instance1) = ", is_sorted(instance1), "\n")
    cat("is_sorted(instance2) = ", is_sorted(instance2), "\n")

    normal_vector <- data
    cat("Normal vector sorted =", is_sorted(normal_vector), "\n")
}

TESTS <- list(
    list("test_trivial", test_trivial),
    list("test_simple_altint", test_simple_altint),
    list("test_two_altints", test_two_altints),
    list("test_altreal", test_altreal),
    list("test_altlogical", test_altlogical),
    list("test_two_altlogicals", test_two_altlogicals),
    list("test_altraw", test_altraw),
    list("test_altcomplex", test_altcomplex),
    list("test_altstring", test_altstring),
    list("test_two_altstrings", test_two_altstrings),
    list("test_default_implementations", test_default_implementations),
    list("test_calls_to_altrep_methods", test_calls_to_altrep_methods),
    list("test_framework_behavior", test_framework_behavior),
    list("test_generator_class", test_generator_class),
    list("test_first_char_changer_class", test_first_char_changer_class),
    list("test_no_na", test_no_na)
)

ONE_TEST <- list(list("temp_test", temp_test))
RUN_ONE_TEST <- FALSE
if (RUN_ONE_TEST) {
    TESTS <- ONE_TEST
}

run_tests(TESTS)



