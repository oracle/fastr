# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

# Tests of the stack instrospection via sys.frame and parent.frame
# in the presence of special builtins: do.call, eval, Recall and promises.
# The tests are written in a simple way with some code duplication so that
# they can also serve as an easy to follow documentation.

# These tests should be run with:
#  * the FastR default options
#  * -DR:StackIntrospectionIterateLevels=0 -DR:-StackIntrospectionPassCallerInInterpreter
#  * -DR:StackIntrospectionIterateLevels=999999 -DR:-StackIntrospectionPassCallerInInterpreter
#  * -DR:-StackIntrospectionNotifyCallers
#  * STACK_TESTS_LOOP_COUNT=2000 mx --dynamicimports graal/compiler --J @'-Dgraal.TraceTruffleCompilation=true -Dgraal.TruffleCompilationThreshold=100 -Dgraal.TruffleOSRCompilationThreshold=500' r

# TODO: the test run with Graal (last item in the list above) causes infinite deopt, even when
# -DR:-StackIntrospectionPassCallerInInterpreter does not fire the assertion,
# so that there should be no frames iteration going on
# The assertion is not firing even when set during last 100 iterations, but the deopt is still there...

# checks that the given environment is what we think it is
# the global and function environments are tested by presence of a unique variable
# the environments of do.call, eval, and Recall are tested by the presence of all of their arguments
errorsCount <- 0L
check <- function(env, name) {
    checkvars <- function(...) {
        names <- as.character(list(...))
        if (! all(names %in% ls(env))) {
            cat("Error:\n expected: "); print(names);
            cat("   actual: "); print(ls(env)); cat("\n");
            errorsCount <<- errorsCount + 1L
            FALSE
        }
        TRUE
    }
    envMustBeEmpty <- function() {
        if (length(ls(env)) > 0) {
            cat("Error:\n expected empty environment: ");
            cat("   actual: "); print(ls(env)); cat("\n");
            errorsCount <<- errorsCount + 1L
            FALSE
        }
        TRUE
    }
    res <- switch(name,
        do.call = checkvars("args", "envir", "quote", "what"),
        eval = checkvars("expr", "envir", "enclos"),
        Rf_eval = envMustBeEmpty(),
        Recall = assert(length(ls(env)), 0L),
        checkvars(paste0(name, "var")))
    invisible(res)
}

assert <- function(actual, expected, message = NULL) {
    if ((is.null(expected) && !is.null(actual)) || !isTRUE(all.equal(actual, expected))) {
        cat("Error:\n expected: "); print(expected);
        cat("   actual: "); print(actual); cat("\n");
        if (!is.null(message)) {
            cat(message, "\n")
        }
        errorsCount <<- errorsCount + 1L
    }
    invisible(NULL)
}

assertFrames <- function(frames, ...) {
    vars <- list(...)
    if (length(frames) != length(vars)) {
        cat("Error:\n expected frames differ in length.");
        errorsCount <<- errorsCount + 1L
        return(invisible(FALSE))
    }
    vars <- list(...)
    for (i in seq_along(frames)) {
        if (!check(frames[[i]], vars[[i]])) {
            return(invisible(FALSE))
        }
    }
    return(invisible(TRUE))
}

# We first execute the deepest stack introspection so that we can be sure that all the calls along the call stack
# have been notified to materialize their frames. Note that sys.frame(0) has a fast path that returns global
# environment and doesn't notify the callers, so we need to use one stack above global.
checkDeepest <- function(env, name) check(env, name)

globalvar <- 'dummy'
env <- list2env(list(customenvvar = 42))

# We can run the tests in a loop to tests the compilation
N <- Sys.getenv("STACK_TESTS_LOOP_COUNT")
N <- if (N == '') 1L else as.integer(N)

# These two functions enable/disable the assertion in FastR that fails whenever the iterate frames is called,
# i.e. when some call has not materialized its stack frame. We do not enable this assertion if the intetion
# is to explicitly tests iterate frames.
# N.B.: for now this check is ignored when running with compiler so that we can observe the deopts
beginNoIterateFrames <- function(...) {}
endNoIterateFrames <- function(...) {}


# Note: each test has the following structure:
# for (i in 1:N) {
#   beginNoIterateFrames(i > N - 100)
#   --> THE TEST CODE HERE <--
# }
#
# This allows to tests the compilation. Only in the last few iterations we assert
# that there's no iterate frames going no -- the AST should be stable by then and
# all the existing calls should have their "needs caller frame" assumption invalidated.
#
# Note 2: because this doesn't work yet, only the first test has this structure. Others will follow.

# ===============================================
# do.call

# -----------------
# sys.frame: the frame of do.call is present

foo <- function(foovar) sys.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) do.call(bar, list(boovar))
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

for (i in 1:N) {
    beginNoIterateFrames(i > N - 100)
    x <- fst(0); check(x, "global");
    x <- fst(1); check(x, "fst");
    x <- fst(2); check(x, "boo");
    x <- fst(3); check(x, "do.call");
    x <- fst(4); check(x, "bar");
    x <- fst(5); check(x, "foo");
}
# fst(6); # not that many frames on the stack
endNoIterateFrames()

# the same situation with sys.nframe
foo <- function(foovar) sys.nframe()
x <- fst(0)
assert(x, 5L)

# the same situation with sys.frames
foo <- function(foovar) sys.frames()
x <- fst(0)
assertFrames(x, "fst", "boo", "do.call", "bar", "foo")

# the same situation with sys.calls
foo <- function(foovar) sys.calls()
x <- fst(42*2)
assert(x[[1]], quote(fst(42*2)))
assert(x[[2]], quote(boo(fstvar)))
assert(x[[3]], quote(do.call(bar, list(boovar))))
# Note: there is formatting or similar issue which prevents us to compare the two directly:
# assert(x[[4]], quote((function(barvar) foo(barvar))(43)))
assert(typeof(x[[4]][[1]]), "closure")
assert(x[[4]][[2]], 42*2)
assert(x[[5]], quote(foo(barvar)))

# -----------------
# parent.frame: the frame of do.call is missing

foo <- function(foovar) parent.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) do.call(bar, list(boovar))
fst <- function(fstvar) boo(fstvar)

x <- fst(3); checkDeepest(x, "fst");

for (i in 1:N) {
    beginNoIterateFrames(i > N - 100)
    x <- fst(1); check(x, "bar");
    x <- fst(2); check(x, "boo");
    x <- fst(3); check(x, "fst");
    x <- fst(4); check(x, "global");
    x <- fst(5); check(x, "global");
    # note: any higher number still returns global
    x <- fst(100); check(x, "global"); # global
}
endNoIterateFrames()

# the same with sys.parent
foo <- function(foovar) sys.parent(foovar)
x <- fst(1); assert(x, 4L)
x <- fst(2); assert(x, 2L)
x <- fst(3); assert(x, 1L)
x <- fst(4); assert(x, 0L)
x <- fst(5); assert(x, 0L)

# the same with sys.parents
foo <- function(foovar) sys.parents()
x <- fst(0); assert(x, c(0L, 1L, 2L, 2L, 4L))

# -----------------
# sys.frame & custom envir: no difference: the frame of do.call is present

foo <- function(foovar) sys.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) do.call(bar, list(boovar), envir = env)
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

for (i in 1:N) {
    beginNoIterateFrames(i > N - 100)
    x <- fst(0); check(x, "global");
    x <- fst(1); check(x, "fst");
    x <- fst(2); check(x, "boo");
    x <- fst(3); check(x, "do.call");
    x <- fst(4); check(x, "bar");
    x <- fst(5); check(x, "foo");
}
# fst(6); # not that many frames on the stack
endNoIterateFrames()

# the same situation with sys.nframe
foo <- function(foovar) sys.nframe()
x <- fst(0)
assert(x, 5L)

# the same situation with sys.frames
foo <- function(foovar) sys.frames()
x <- fst(0)
assertFrames(x, "fst", "boo", "do.call", "bar", "foo")

# the same situation with sys.calls
foo <- function(foovar) sys.calls()
x <- fst(42+1)
assert(x[[1]], quote(fst(42+1)))
assert(x[[2]], quote(boo(fstvar)))
assert(x[[3]], quote(do.call(bar, list(boovar), envir = env)))
# Note: there is formatting or similar issue which prevents us to compare the two directly:
# assert(x[[4]], quote((function(barvar) foo(barvar))(43)))
assert(typeof(x[[4]][[1]]), "closure")
assert(x[[4]][[2]], 43)
assert(x[[5]], quote(foo(barvar)))

# -----------------
# parent.frame & custom envir:
# instead of do.call frame we have the custom environment followed by global
# conclusion: parent of a hand created envs is global

foo <- function(foovar) parent.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) do.call(bar, list(boovar), envir = env)
fst <- function(fstvar) boo(fstvar)

x <- fst(2); checkDeepest(x, "customenv");

for (i in 1:N) {
    beginNoIterateFrames(i > N - 100)
    x <- fst(1); check(x, "bar");
    x <- fst(2); check(x, "customenv");
    x <- fst(3); check(x, "global");
    x <- fst(4); check(x, "global");
}
endNoIterateFrames()

# the same with sys.parent
foo <- function(foovar) sys.parent(foovar)

x <- fst(1); assert(x, 4L)
x <- fst(2); assert(x, 4L)
x <- fst(3); assert(x, 4L)
# etc. we get stuck on 4L (frame of bar), the last frame that is a function frame

# the same with sys.parents
foo <- function(foovar) sys.parents()
x <- fst(0); assert(x, c(0L, 1L, 2L, 4L, 4L))

# -----------------
# sys.frame & custom envir of another function: no difference, frame of do.call is present

foo <- function(foovar) sys.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) {
    fstenv <- parent.frame()
    check(fstenv, "fst")
    do.call(bar, list(boovar), envir = fstenv)
}
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

for (i in 1:N) {
    beginNoIterateFrames(i > N - 100)
    x <- fst(0); check(x, "global");
    x <- fst(1); check(x, "fst");
    x <- fst(2); check(x, "boo");
    x <- fst(3); check(x, "do.call");
    x <- fst(4); check(x, "bar");
    x <- fst(5); check(x, "foo");
}
# fst(6); # not that many frames on the stack
endNoIterateFrames()

# the same with sys.parent
foo <- function(foovar) sys.parent(foovar)
x <- fst(1); assert(x, 4L)
x <- fst(2); assert(x, 1L)
x <- fst(3); assert(x, 0L)
x <- fst(4); assert(x, 0L)

# the same with sys.parents
foo <- function(foovar) sys.parents()
x <- fst(0); assert(x, c(0L, 1L, 2L, 1L, 4L))

# -----------------
# parent.frame & custom envir of another function 'second':
# the parent hierarchy is reconnected continuing from 'second'

foo <- function(foovar) parent.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) {
    fstenv <- parent.frame(2)
    check(fstenv, "second")
    do.call(bar, list(boovar), envir = fstenv)
}
third <- function(thirdvar) boo(thirdvar)
second <- function(secondvar) third(secondvar)
fst <- function(fstvar) second(fstvar)

x <- fst(3); checkDeepest(x, "fst");

for (i in 1:N) {
    beginNoIterateFrames(i > N - 100)
    x <- fst(1); check(x, "bar");
    x <- fst(2); check(x, "second");
    x <- fst(3); check(x, "fst");
    x <- fst(4); check(x, "global");
}
endNoIterateFrames()

# the same with sys.parent
foo <- function(foovar) sys.parent(foovar)
x <- fst(1); assert(x, 6L)
x <- fst(2); assert(x, 2L)
x <- fst(3); assert(x, 1L)
x <- fst(4); assert(x, 0L)
x <- fst(5); assert(x, 0L)

# the same with sys.parents
foo <- function(foovar) sys.parents()
x <- fst(0); assert(x, c(0L, 1L, 2L, 3L, 4L, 2L, 6L))

# -----------------
# calling a primitive via do.call: the primitive should be executed in the right frame

foo <- function() do.call(nargs, list(), envir = parent.frame(1))
bar <- function(barvar, x, y, z) foo()
assert(bar(1, 2), 2L)

# ===============================================
# promises

# -----------------
# sys.frame and promise: as if the promise was evaluated in "boo"

foo <- function(foovar) foovar
bar <- function(barvar) foo(barvar)
boo <- function(boovar) bar(sys.frame(boovar))
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(0); check(x, "global");
x <- fst(1); check(x, "fst");
x <- fst(2); check(x, "boo");
# fst(3); # not that many frames on the stack
endNoIterateFrames()

# the same situation with sys.nframe
boo <- function(boovar) bar(sys.nframe())
x <- fst(0)
assert(x, 2L)

# the same situation with sys.calls
boo <- function(boovar) bar(sys.calls())
x <- fst(42)
assert(x[[1]], quote(fst(42)))
assert(x[[2]], quote(boo(fstvar)))

# the same situation with sys.frames()
boo <- function(boovar) bar(sys.frames())
x <- fst(42)
assertFrames(x, "fst", "boo")

# -----------------
# parent.frame and promise: as if the promise was evaluated in "boo"

foo <- function(foovar) foovar
bar <- function(barvar) foo(barvar)
boo <- function(boovar) bar(parent.frame(boovar))
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(1); check(x, "fst");
x <- fst(2); check(x, "global");
endNoIterateFrames()

# the same with sys.parent
boo <- function(boovar) bar(sys.parent(boovar))
x <- fst(1); assert(x, 1L)
x <- fst(2); assert(x, 0L)
x <- fst(3); assert(x, 0L)

# the same with sys.parents
boo <- function(boovar) bar(sys.parents())
x <- fst(0); assert(x, c(0L, 1L))

# -----------------
# sys.frame and promise that does few more R functions calls before calling sys.frame
# all the functions are linearized into the sys.frame output
#

foo <- function(foovar) foovar
bar <- function(barvar) foo(barvar)
fork2 <- function(fork2var) sys.frame(fork2var)
fork <- function(forkvar) fork2(forkvar)
boo <- function(boovar) bar(fork(boovar))
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(1); check(x, "fst");
x <- fst(2); check(x, "boo");
x <- fst(3); check(x, "bar");
x <- fst(4); check(x, "foo");
x <- fst(5); check(x, "fork");
x <- fst(6); check(x, "fork2");
endNoIterateFrames()
# fst(7); # not that many frames on the stack

# -----------------
# parent.frame and promise that does few more R functions calls before calling sys.frame

foo <- function(foovar) foovar
bar <- function(barvar) foo(barvar)
fork2 <- function(fork2var) parent.frame(fork2var)
fork <- function(forkvar) fork2(forkvar)
boo <- function(boovar) bar(fork(boovar))
fst <- function(fstvar) boo(fstvar)

x <- fst(3); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(1); check(x, "fork");
x <- fst(2); check(x, "boo");
x <- fst(3); check(x, "fst");
x <- fst(4); check(x, "global");
endNoIterateFrames()

# the same with sys.parent
fork2 <- function(fork2var) sys.parent(fork2var)
# TODO: this is an issue because of eager promise evaluation,
# we are evaluating "barvar" in "bar", not in "foo" and "foo"
# is not on the call stack yet.
# TODO: x <- fst(1); assert(x, 5L);
x <- fst(2); assert(x, 2L);
x <- fst(3); assert(x, 1L);
x <- fst(4); assert(x, 0L);
x <- fst(5); assert(x, 0L);

# the same with sys.parents
fork2 <- function(fork2var) sys.parents()
# TODO: x <- fst(0); assert(x, c(0L, 1L, 2L, 3L, 2L, 5L))

# -----------------
# parent.frame and return as a promise
# nothing changes: the same as parent.frame and promise

foo <- function(foovar) foovar
bar <- function(barvar) foo(barvar)
fork2 <- function(fork2var) parent.frame(fork2var)
fork <- function(forkvar) fork2(forkvar)
boo <- function(boovar) bar(return(fork(boovar)))
fst <- function(fstvar) boo(fstvar)

x <- fst(3); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(1); check(x, "fork");
x <- fst(2); check(x, "boo");
x <- fst(3); check(x, "fst");
x <- fst(4); check(x, "global");
endNoIterateFrames()

# ----------------
# sys.frame and nested promises
f4 <- function(f4var) sys.frame(f4var);
f3 <- function(f3var) f3var; # promise to f4(f1var)
f2 <- function(f2var) f2var; # promise to f3(f4(f1var))
f1 <- function(f1var) f2(f3(f4(f1var)));

x <- f1(1); checkDeepest(x, "f1");

beginNoIterateFrames()
x <- f1(1); check(x, "f1");
x <- f1(2); check(x, "f2");
x <- f1(3); check(x, "f3");
x <- f1(4); check(x, "f4");
endNoIterateFrames()

# the same situation with sys.nframe
f4 <- function(f4var) sys.nframe()
x <- f1(0); assert(x, 4L)

# the same situation with sys.calls
f4 <- function(f4var) sys.calls()
x <- f1(42)
assert(x[[1]], quote(f1(42)))
assert(x[[2]], quote(f2(f3(f4(f1var)))))
assert(x[[3]], quote(f3(f4(f1var))))
assert(x[[4]], quote(f4(f1var)))

# the same situation with sys.parents
f4 <- function(f4var) sys.parents()
x <- f1(42)
assert(x, c(0, 1, 1, 1))

# ----------------
# parent.frame and nested promises
f4 <- function(f4var) parent.frame(f4var);
f3 <- function(f3var) f3var; # promise to f4(f1var)
f2 <- function(f2var) f2var; # promise to f3(f4(f1var))
f1 <- function(f1var) f2(f3(f4(f1var)));

x <- f1(1); checkDeepest(x, "f1");

beginNoIterateFrames()
x <- f1(1); check(x, "f1");
x <- f1(2); check(x, "global");
endNoIterateFrames()

# the same with sys.parent
f4 <- function(f4var) sys.parent(f4var)
x <- f1(1); assert(x, 1L);
x <- f1(2); assert(x, 0L);
x <- f1(3); assert(x, 0L);

# the same with sys.parents
f4 <- function(f4var) sys.parents()
x <- f1(0); assert(x, c(0L, 1L, 1L, 1L))

# -----------------
# TODO: sys.frame and return as a promise

#foo <- function(foovar) foovar
#bar <- function(barvar) foo(barvar)
#fork2 <- function(fork2var) sys.frame(fork2var)
#fork <- function(forkvar) fork2(forkvar)
#boo <- function(boovar) bar(return(fork(boovar))))
#fst <- function(fstvar) boo(fstvar)

# ===============================================
# UseMethod

# -----------------
# UseMethod and parent.frame
# the callee of UseMethod has an illusion that it is invoked by the caller of the dispatching function
baz <- function(bazvar) parent.frame(bazvar)
foo.c1 <- function(foo.c1var) baz(foo.c1var)
foo.default <- function(foo.defaultvar) baz(foo.defaultvar)
foo <- function(foovar) {
    dispatch <<- parent.frame();
    UseMethod("foo")
}
bar <- function(barvar, class) foo(structure(barvar, class=class))

dispatch <- NULL
x <- bar(1, 'c1'); check(x, 'foo.c1'); check(dispatch, 'bar');
x <- bar(2, 'c1'); check(x, 'bar');
x <- bar(3, 'c1'); check(x, 'global');

# with the default method nothing changes
dispatch <- NULL
x <- bar(1, 'dummy'); check(x, 'foo.default'); check(dispatch, 'bar');
x <- bar(2, 'dummy'); check(x, 'bar');
x <- bar(3, 'dummy'); check(x, 'global');

# the same with sys.parents()
baz <- function(bazvar) sys.parents()

x <- bar(1, 'c1');    assert(x, c(0L, 1L, 1L, 3L))
x <- bar(1, 'dummy'); assert(x, c(0L, 1L, 1L, 3L))

# the same with sys.calls()
baz <- function(bazvar) sys.calls()

x <- bar(1, 'c1');
assert(x[[1]], quote(bar(1, 'c1')))
assert(x[[2]], quote(foo(structure(barvar, class = class))))
assert(x[[3]], quote(foo.c1(structure(barvar, class = class))))
assert(x[[4]], quote(baz(foo.c1var)))

# the same with sys.frames()
baz <- function(bazvar) sys.frames()

x <- bar(1, 'c1');
check(x[[1]], "bar");
check(x[[2]], "foo");
check(x[[3]], "foo.c1");
check(x[[4]], "baz");

# -----------------
# UseMethod & NextMethod & parent.frame
# the callee of NextMethod has an illusion that it is invoked by the caller of the dispatching function UseMethod
baz <- function(bazvar) parent.frame(bazvar)
foo.c2 <- function(foo.c2var) baz(foo.c2var)
foo.c1 <- function(foo.c1var) {
    dispatch <<- parent.frame(foo.c1var)
    NextMethod()
}
foo <- function(foovar) UseMethod("foo")
bar <- function(barvar, class) foo(structure(barvar, class=class))

dispatch <- NULL
x <- bar(1, c('c1', 'c2')); check(x, 'foo.c2'); check(dispatch, 'bar');
x <- bar(2, c('c1', 'c2')); check(x, 'bar');
x <- bar(3, c('c1', 'c2')); check(x, 'global');

# the same with sys.parents()
baz <- function(foo.c2var) sys.parents()
x <- bar(1, c('c1', 'c2')); assert(x, c(0L, 1L, 1L, 1L, 1L, 5L))

# the same with sys.calls()
baz <- function(foo.c2var) sys.calls()
x <- bar(1, c('c1', 'c2'));
assert(x[[1]], quote(bar(1, c('c1', 'c2'))))
assert(x[[2]], quote(foo(structure(barvar, class = class))))
assert(x[[3]], quote(foo.c1(structure(barvar, class = class))))
assert(x[[4]], quote(NextMethod()))
assert(x[[5]], quote(foo.c2(structure(barvar, class = class))))
assert(x[[6]], quote(baz(foo.c2var)))

# ===============================================
# eval

# -----------------
# sys.frame and eval:
# eval creates a new frame that points to the same environment as "boo" (i.e. where it was called)
# this frame is regular frame visible to the user

foo <- function(foovar)    sys.frame(foovar)
bar <- function(barvar)    foo(barvar)
boo <- function(boovar)    eval(quote(bar(boovar)))
fst <- function(fstvar)    boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(0); check(x, "global");
x <- fst(1); check(x, "fst");
x <- fst(2); check(x, "boo");
x <- fst(3); check(x, "eval");
x <- fst(4); check(x, "boo");
x <- fst(5); check(x, "bar");
x <- fst(6); check(x, "foo");
# fst(7);  # not that many frames on the stack
endNoIterateFrames()

# -----------------
# parent.frame and eval:
# eval creates a new frame that points to the same environment as "boo" (i.e. where it was called)
# the parent of this frame is parent of "boo"

foo <- function(foovar) parent.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) eval(quote(bar(boovar)))
fst <- function(fstvar) boo(fstvar)

x <- fst(5); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(1); check(x, "bar");
x <- fst(2); check(x, "boo");
x <- fst(3); check(x, "eval");
x <- fst(4); check(x, "boo");
x <- fst(5); check(x, "fst");
x <- fst(6); check(x, "global");
endNoIterateFrames()

# -----------------
# sys.frame and eval and custom environment
# there is frame for the function that calls 'eval', then the frame of 'eval' and then the frame for custom environment

foo <- function(foovar) sys.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) { env$n <- boovar; eval(quote(bar(n)), envir = env);}
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(0); check(x, "global");
x <- fst(1); check(x, "fst");
x <- fst(2); check(x, "boo");
x <- fst(3); check(x, "eval");
x <- fst(4); check(x, "customenv");
x <- fst(5); check(x, "bar");
x <- fst(6); check(x, "foo");
# fst(7);  # not that many frames on the stack
endNoIterateFrames()


# -----------------
# parent.frame and eval and custom environment
# there is a frame for the function that calls 'eval', then the frame of 'eval' and then the frame for custom environment

foo <- function(foovar) parent.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) { env$n <- boovar; eval(quote(bar(n)), envir = env);}
fst <- function(fstvar) boo(fstvar)

x <- fst(5); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(1); check(x, "bar");
x <- fst(2); check(x, "customenv");
x <- fst(3); check(x, "eval");
x <- fst(4); check(x, "boo");
x <- fst(5); check(x, "fst");
x <- fst(6); check(x, "global");
endNoIterateFrames()

# -----------------
# sys.frame and eval and other function's environment

foo <- function(foovar) sys.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) { env <- parent.frame(); check(env, "second"); eval(quote(bar(secondvar)), envir = env);}
second <- function(secondvar) boo(secondvar)
fst <- function(fstvar) second(fstvar)

x <- fst(1); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(0); check(x, "global");
x <- fst(1); check(x, "fst");
x <- fst(2); check(x, "second");
x <- fst(3); check(x, "boo");
x <- fst(4); check(x, "eval");
x <- fst(5); check(x, "second");
x <- fst(6); check(x, "bar");
x <- fst(7); check(x, "foo");
# fst(8);  # not that many frames on the stack
endNoIterateFrames()

# -----------------
# parent.frame and eval and other function's environment

foo <- function(foovar) parent.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) { env <- parent.frame(); check(env, "second"); eval(quote(bar(secondvar)), envir = env);}
second <- function(secondvar) boo(secondvar)
fst <- function(fstvar) second(fstvar)

x <- fst(6); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(1); check(x, "bar");
x <- fst(2); check(x, "second");
x <- fst(3); check(x, "eval");
x <- fst(4); check(x, "boo");
x <- fst(5); check(x, "second");
x <- fst(6); check(x, "fst");
x <- fst(7); check(x, "global");
endNoIterateFrames()

# ===============================================
# Rf_eval

library(testrffi)

# -----------------
# sys.frame and Rf_eval:

foo <- function(foovar)    sys.frame(foovar)
bar <- function(barvar)    foo(barvar)
boo <- function(boovar)    api.Rf_eval(quote(bar(boovar)), environment())
fst <- function(fstvar)    boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(0); check(x, "global");
x <- fst(1); check(x, "fst");
x <- fst(2); check(x, "boo");
x <- fst(3); check(x, "Rf_eval");
x <- fst(4); check(x, "bar");
x <- fst(5); check(x, "foo");
# fst(6);  # not that many frames on the stack
endNoIterateFrames()

# -----------------
# parent.frame and Rf_eval:

foo <- function(foovar) parent.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) api.Rf_eval(quote(bar(boovar)), environment())
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "bar");

beginNoIterateFrames()
x <- fst(1); check(x, "bar");
x <- fst(2); check(x, "boo");
x <- fst(3); check(x, "fst");
x <- fst(4); check(x, "global");
endNoIterateFrames()

# -----------------
# sys.frame and Rf_eval and custom environment

foo <- function(foovar) sys.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) { env$n <- boovar; api.Rf_eval(quote(bar(n)), env);}
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(0); check(x, "global");
x <- fst(1); check(x, "fst");
x <- fst(2); check(x, "boo");
x <- fst(3); check(x, "Rf_eval");
x <- fst(4); check(x, "bar");
x <- fst(5); check(x, "foo");
# fst(6);  # not that many frames on the stack
endNoIterateFrames()

# -----------------
# parent.frame and Rf_eval and custom environment

foo <- function(foovar) parent.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) { env$n <- boovar; api.Rf_eval(quote(bar(n)), env);}
fst <- function(fstvar) boo(fstvar)

x <- fst(1); checkDeepest(x, "bar");

beginNoIterateFrames()
x <- fst(1); check(x, "bar");
x <- fst(2); check(x, "customenv");
x <- fst(3); check(x, "global");
endNoIterateFrames()

# -----------------
# sys.frame and Rf_eval and other function's environment

foo <- function(foovar) sys.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) { env <- parent.frame(); check(env, "second"); api.Rf_eval(quote(bar(secondvar)), env);}
second <- function(secondvar) boo(secondvar)
fst <- function(fstvar) second(fstvar)

x <- fst(1); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(0); check(x, "global");
x <- fst(1); check(x, "fst");
x <- fst(2); check(x, "second");
x <- fst(3); check(x, "boo");
x <- fst(4); check(x, "Rf_eval");
x <- fst(5); check(x, "bar");
x <- fst(6); check(x, "foo");
# fst(7);  # not that many frames on the stack
endNoIterateFrames()

# -----------------
# parent.frame and Rf_eval and other function's environment

foo <- function(foovar) parent.frame(foovar)
bar <- function(barvar) foo(barvar)
boo <- function(boovar) { env <- parent.frame(); check(env, "second"); api.Rf_eval(quote(bar(secondvar)), env);}
second <- function(secondvar) boo(secondvar)
fst <- function(fstvar) second(fstvar)

x <- fst(3); checkDeepest(x, "fst");

beginNoIterateFrames()
x <- fst(1); check(x, "bar");
x <- fst(2); check(x, "second");
x <- fst(3); check(x, "fst");
x <- fst(4); check(x, "global");
endNoIterateFrames()


# -----------------
# environment function must return the correct environment when invoked from a promise evaluated by Rf_eval

myenv <- new.env()
myenv$aaavar <- 1

foo <- function(foobar)    foobar
bar <- function(barvar)    barvar
boo <- function(boovar)    api.Rf_eval(quote(bar(foo(environment()))), myenv)
fst <- function(fstvar)    boo(fstvar)

retEnv <- fst(1)
check(retEnv, "aaa")


# ===============================================
# More complex interaction: sys.frame and do.call and promises

top <- function(topvar) topvar;
foo <- function(foovar) top(foovar);
boo <- function(boovar) foo(sys.frame(boovar));
bar <- function(barvar) do.call(boo, list(barvar), envir = parent.frame(2));
baz <- function(bazvar) bar(bazvar);
start <- function(startvar) baz(startvar);

x <- start(1); checkDeepest(x, "start");

beginNoIterateFrames()
x <- start(0); check(x, "global");
x <- start(1); check(x, "start");
x <- start(2); check(x, "baz");
x <- start(3); check(x, "bar");
x <- start(4); check(x, "do.call");
x <- start(5); check(x, "boo");
endNoIterateFrames()
# start(6) # not that many frames


# ===============================================
# Some more use cases

# -----------------
# environment uses GetCallerFrameNode
foo <- function(fun2) { myvar <- 42; fun2(environment()); }
x <- foo(function(env) env$myvar)
assert(x, 42)

# ------------------
# eval example similar to eval vs. bare_eval from rlang

fn <- function(eval_fn, fnvar) list(middle(eval_fn, fnvar))
middle <- function(eval_fn, middlevar) deep(eval_fn, middlevar, environment())
deep <- function(eval_fn, deepvar, eval_env) {
    expr <- quote(parent.frame(1))
    expr[[2]] <- deepvar
    eval_fn(expr, eval_env)
}
x <- fn(base::eval, 1); check(x[[1]], "eval")
x <- fn(base::eval, 2); check(x[[1]], "deep")
x <- fn(base::eval, 3); check(x[[1]], "middle")
x <- fn(base::eval, 4); check(x[[1]], "fn")
x <- fn(base::eval, 5); check(x[[1]], "global")

# ------------------
# variation of the previous example,
# but the expression eval'ed is function call that leads to stack introspection

fn <- function(eval_fn, fnvar) list(middle(eval_fn, fnvar))
middle <- function(eval_fn, middlevar) deep(eval_fn, middlevar, environment())
introspecting <- function(introspectingvar) parent.frame(introspectingvar)
evaled <- function(evaledvar) introspecting(evaledvar)
deep <- function(eval_fn, deepvar, eval_env) {
    expr <- quote(evaled(1))
    expr[[2]] <- deepvar
    eval_fn(expr, eval_env)
}
x <- fn(base::eval, 1); check(x[[1]], "evaled")
x <- fn(base::eval, 2); check(x[[1]], "middle")
x <- fn(base::eval, 3); check(x[[1]], "eval")
x <- fn(base::eval, 4); check(x[[1]], "deep")
x <- fn(base::eval, 5); check(x[[1]], "middle")
x <- fn(base::eval, 6); check(x[[1]], "fn")
x <- fn(base::eval, 7); check(x[[1]], "global")

# ===============================================
# Recall

# -----------------
# sys.frame and Recall: like with eval

 foo <- function(foovar) sys.frame(foovar)
 dispatcher <- function(name, x) {
     if (name == "bar") {
         barvar <- x
         foo(barvar)
     } else if (name == "boo") {
         boovar <- x
         Recall("bar", boovar)
     }
 }
 fst <- function(fstvar) dispatcher("boo", fstvar)

 x <- fst(0); checkDeepest(x, "global");

 x <- fst(0); check(x, "global");
 x <- fst(1); check(x, "fst");
 x <- fst(2); check(x, "boo");
 x <- fst(3); check(x, "Recall"); # Note: empty env, hard to guess what it is, probably env of the Recall R wrapper
 x <- fst(4); check(x, "bar");
 x <- fst(5); check(x, "foo");
# fst(6);  # not that many frames on the stack

# -----------------
# parent.frame and Recall: only the last frame from Recalled function counts

 foo <- function(foovar) parent.frame(foovar)
 dispatcher <- function(name, x) {
     if (name == "bar") {
         barvar <- x
         foo(barvar)
     } else if (name == "boo") {
         boovar <- x
         Recall("bar", boovar)
     }
 }
 fst <- function(fstvar) dispatcher("boo", fstvar)

x <- fst(3); checkDeepest(x, "global");

x <- fst(1); check(x, "bar");
x <- fst(2); check(x, "fst"); # Note: empty env, hard to guess what it is, probably env of the Recall R wrapper
x <- fst(3); check(x, "global");

# -----------------
# sys.call: returns the correct caller from a given level

foo <- function(level, callNum) sys.call(callNum)
bar <- function(level, callNum) if (level == 3) sys.call(callNum) else foo(level, callNum)
boo <- function(level, callNum) if (level == 2) sys.call(callNum) else do.call(bar, list(level, callNum))
fst <- function(level, callNum) if (level == 1) sys.call(callNum) else boo(level, callNum)

x <- fst(1,0)
assert(x, quote(fst(1,0)))
x <- fst(1,1)
assert(x, quote(fst(1,1)))
x <- fst(1,-1)
assert(x, NULL)
x <- tryCatch(fst(1,-6), error=function(e) e)
assert(x, simpleError("not that many frames on the stack", call=quote(sys.call(callNum))))
x <- tryCatch(fst(1,6), error=function(e) e)
assert(x, simpleError("not that many frames on the stack", call=quote(sys.call(callNum))))

x <- fst(2,0)
assert(x, quote(boo(level, callNum)))
x <- fst(2,1)
assert(x, quote(fst(2, 1)))
x <- fst(2,2)
assert(x, quote(boo(level, callNum)))

x <- fst(3,0)
assert(x, substitute(bar(3,0), env=as.list(.GlobalEnv)))
x <- fst(3,1)
assert(x, quote(fst(3, 1)))
x <- fst(3,2)
assert(x, quote(boo(level, callNum)))
x <- fst(3,3)
assert(x, quote(do.call(bar, list(level, callNum))))
x <- fst(3,4)
assert(x, substitute(bar(3,4), env=as.list(.GlobalEnv)))

x <- fst(4,0)
assert(x, quote(foo(level, callNum)))
x <- fst(4,1)
assert(x, quote(fst(4,1)))
x <- fst(4,2)
assert(x, quote(boo(level, callNum)))
x <- fst(4,3)
assert(x, quote(do.call(bar, list(level, callNum))))
x <- fst(4,4)
assert(x, substitute(bar(4,4), env=as.list(.GlobalEnv)))
x <- fst(4,5)
assert(x, quote(foo(level, callNum)))
x <- fst(4,-1)
assert(x, substitute(bar(4,-1), env=as.list(.GlobalEnv)))
x <- fst(4,-2)
assert(x, quote(do.call(bar, list(level, callNum))))
x <- fst(4,-3)
assert(x, quote(boo(level, callNum)))
x <- fst(4,-4)
assert(x, quote(fst(4,-4)))

if (errorsCount > 0) {
    cat("FAILED TESTS: ", errorsCount)
}
