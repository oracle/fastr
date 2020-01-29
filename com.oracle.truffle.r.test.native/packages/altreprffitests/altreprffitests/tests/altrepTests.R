stopifnot(require(altreprffitests))

MAX_NUMBER_OF_ERRORS <- 1

#' Generates an ALTREP instance of vec_wrapper class with just a portion of methods registered.
#' Note that this portion is randomly chosen.
generate_random_instance <- function() {
    generate_random_data <- function() {
        len <- round( runif(1, min=2, max=30))
        TYPES_COUNT <- 2
        type_idx <- round( runif(1, min=1, max=2))
        if (type_idx == 1) {
            # int
            return (as.integer( round( runif(len, min=1, max=10))))
        } else if (type_idx == 2) {
            # double
            return (as.double( runif(len, min=1, max=10)))
        } else {
            stop("Wrong type specified")
        }
    }

    # Will register random native methods as ALTREP overriden methods.
    generate_random_params <- function() {
        random_bool <- function() {
            as.logical( round( runif(1, min=0, max=1)))
        }
        params <- list()
        for (param_name in c("gen.Duplicate", "gen.Coerce", "gen.Elt", "gen.Sum", "gen.Min", "gen.Max",
                             "gen.Get_region", "gen.Is_sorted"))
        {
            item <- list(random_bool())
            names(item) <- param_name
            params <- c(params, item)
        }
        return (params)
    }

    rnd_data <- generate_random_data()
    rnd_params <- generate_random_params()
    return (vec_wrapper.create_instance(rnd_data,
                                        gen.Duplicate  = rnd_params$gen.Duplicate,
                                        gen.Coerce     = rnd_params$gen.Coerce,
                                        gen.Elt        = rnd_params$gen.Elt,
                                        gen.Sum        = rnd_params$gen.Sum,
                                        gen.Min        = rnd_params$gen.Min,
                                        gen.Max        = rnd_params$gen.Max,
                                        gen.Get_region = rnd_params$gen.Get_region,
                                        gen.Is_sorted  = rnd_params$gen.Is_sorted,
                                        )
    )
}

CURR_NUMBER_OF_ERRORS <- 0

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
        width <- 80L
        name <- substr(deparse(sys.call(), width)[[1L]], 1, width)
        if (!identical(val1, val2)) {
            cat("Fail: ", name, "with val1=", val1, ", val2=", val2, "\n")
            CURR_NUMBER_OF_ERRORS <- CURR_NUMBER_OF_ERRORS + 1
            if (CURR_NUMBER_OF_ERRORS >= MAX_NUMBER_OF_ERRORS) {
                stop("Maximum number of errors reached")
            }
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
        assert_equals( as.integer(instance), as.integer(expected_data))
        assert_equals( as.double(instance), as.double(expected_data))
    }

    test_is_sorted <- function() {
        assert_equals( is.unsorted(instance), is.unsorted(expected_data))
        assert_equals( is.unsorted(instance, strictly=TRUE), is.unsorted(expected_data, strictly=TRUE))
    }

    test_length()
    test_elt()
    test_sum()
    test_min()
    test_max()
    test_coerce()
    test_is_sorted()
    return (TRUE)
}

#' Checks two instances for equality. One of these instances has certain altrep method
#' overriden, the second does not.
test_default_implementations <- function() {
    for (param_name in c("gen.Duplicate", "gen.Coerce", "gen.Elt", "gen.Sum", "gen.Min", "gen.Max",
                         "gen.Get_region", "gen.Is_sorted"))
    {
        for (data in list(1:10, c(2L,5L,6L,1L,2L))) {
            params_with <- list(data, TRUE)
            names(params_with) <- c("", param_name)
            instance_with_method <- do.call(simple_vec_wrapper.create_instance, params_with)

            params_without <- list(data, FALSE)
            names(params_without) <- c("", param_name)
            instance_without_method <- do.call(simple_vec_wrapper.create_instance, params_without)

            cat("Checking whether instance1 with", param_name, "equals instance2 with data =", data, "\n")
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
        logging_vec_wrapper.clear_called_methods()
        instance <- logging_vec_wrapper.create_instance(1:10, gen.Sum = TRUE)
        sum(instance)
        stopifnot( logging_vec_wrapper.was_Sum_called(instance))
    }

    test_duplicate_called()
    test_sum_called()
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

test_simple <- function() {
    data <- 1:10
    instance <- simple_vec_wrapper.create_instance(data)
    check_equal(instance, data)
}

test_two_instances <- function() {
    data <- 1:15
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

TESTS <- list(
    list("test_simple", test_simple),
    list("test_two_instances", test_two_instances),
    list("test_default_implementations", test_default_implementations),
    list("test_calls_to_altrep_methods", test_calls_to_altrep_methods),
    list("test_framework_behavior", test_framework_behavior),
    list("test_generator_class", test_generator_class),
    list("test_first_char_changer_class", test_first_char_changer_class)
)

run_tests(TESTS)



