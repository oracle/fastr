/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2005-12   The R Core Team.
 *
 *  This program is free software{ return NULL UNIMPLEMENTED } you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation{ return NULL UNIMPLEMENTED } either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY{ return NULL UNIMPLEMENTED } without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program{ return NULL UNIMPLEMENTED } if not, a copy is available at
 *  http://www.r-project.org/Licenses/
 */

#include <Rinternals.h>

#define UNIMPLEMENTED { error("unimplemented function at %s:%d", __FILE__, __LINE__); return NULL; }

#ifndef _WIN32
SEXP mc_children() UNIMPLEMENTED
SEXP mc_close_fds(SEXP a) UNIMPLEMENTED
SEXP mc_close_stderr(SEXP a) UNIMPLEMENTED
SEXP mc_close_stdout(SEXP a) UNIMPLEMENTED
SEXP mc_create_list(SEXP a) UNIMPLEMENTED
SEXP mc_exit(SEXP a) UNIMPLEMENTED
SEXP mc_fds(SEXP a) UNIMPLEMENTED
SEXP mc_fork(SEXP a) UNIMPLEMENTED
SEXP mc_is_child(void) UNIMPLEMENTED
SEXP mc_kill(SEXP a, SEXP b) UNIMPLEMENTED
SEXP mc_master_fd(void) UNIMPLEMENTED
SEXP mc_read_child(SEXP a) UNIMPLEMENTED
SEXP mc_read_children(SEXP a) UNIMPLEMENTED
SEXP mc_rm_child(SEXP a) UNIMPLEMENTED
SEXP mc_send_master(SEXP a) UNIMPLEMENTED
SEXP mc_select_children(SEXP a, SEXP b) UNIMPLEMENTED
SEXP mc_send_child_stdin(SEXP a, SEXP b) UNIMPLEMENTED
SEXP mc_affinity(SEXP a) UNIMPLEMENTED
SEXP mc_interactive(SEXP a) UNIMPLEMENTED
#else
SEXP ncpus(SEXP a) UNIMPLEMENTED
#endif
