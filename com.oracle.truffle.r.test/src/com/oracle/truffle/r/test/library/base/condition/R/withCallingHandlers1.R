#
# Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

cond0 <- function(message)
  structure(list(message=message, call=NULL), class=c("cond0", "condition"))

cond1 <- function(message)
  structure(list(message=message, call=NULL), class=c("cond1", "condition"))

handle_cond0 <- function(e) {
  print(paste("enter handle_cond0", e))
  invokeRestart("continue_test")
  print("after cond0 restart")
}

handle_cond1 <- function(e) {
  print(paste("enter handle_cond1", e))
  signalCondition(e)
  print("after cond1 restart")
}

fun0 <- function(code) {
      withCallingHandlers({
      print(code)
      eval(code)
      },
      cond0 = handle_cond0,
      cond1 = handle_cond1
    ); 
    print("exit fun0") 
}

fun1 <- function(s) {
	print(paste("enter fun1", s))
	withRestarts(
		{ signalCondition(cond1(paste0("signal", s)));signalCondition(cond0(paste0("signal", s))); print("afterSignal") } ,
		continue_test = function(e) print("continue")
	)
	print(paste("exit fun1", s))
	NULL
}

fun0(quote({
	fun1("first")
	fun1("second")
}))
fun0({
	fun1("first")
	fun1("second")
})
