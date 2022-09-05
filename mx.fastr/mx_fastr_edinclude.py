#
# Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
#
from os.path import join

'''
Handles all the editing of R FFI header files from the GNUR include directory to the
FastR include directory.
'''
# variables in Rinternals.h that are Java Objects and so remapped to functions
r_internals_vars = ['R_GlobalEnv', 'R_BaseEnv', 'R_BaseNamespace', 'R_NamespaceRegistry']

def edinclude(args):
    '''
    edit GNU include files for FASTR
    args[0] path to GNUR include directory
    '''
    ed_r_internals(args[1])
    ed_r_interface(args[1])
    ed_graphicsengine(args[1])
    ed_rconfig(args[0])

use_internals_section_and_glob_var_api = '''#ifdef FASTR
typedef void * FASTR_GlobalVar_t;

FASTR_GlobalVar_t FASTR_GlobalVarAlloc();
void FASTR_GlobalVarInit(FASTR_GlobalVar_t id);
void FASTR_GlobalVarInitWithDtor(FASTR_GlobalVar_t id, void (*dtor)(void *));
void FASTR_GlobalVarSetSEXP(FASTR_GlobalVar_t id, SEXP value);
SEXP FASTR_GlobalVarGetSEXP(FASTR_GlobalVar_t id);
void FASTR_GlobalVarSetPtr(FASTR_GlobalVar_t id, void *value);
void * FASTR_GlobalVarGetPtr(FASTR_GlobalVar_t id);
void FASTR_GlobalVarSetInt(FASTR_GlobalVar_t id, int value);
int FASTR_GlobalVarGetInt(FASTR_GlobalVar_t id);
void FASTR_GlobalVarSetDouble(FASTR_GlobalVar_t id, double value);
double FASTR_GlobalVarGetDouble(FASTR_GlobalVar_t id);
void FASTR_GlobalVarSetBool(FASTR_GlobalVar_t id, Rboolean value);
Rboolean FASTR_GlobalVarGetBool(FASTR_GlobalVar_t id);
void FASTR_GlobalVarPrintDescrs();

// packages defining USE_INTERNALS expect certain defs (e.g. isNull) to be there
#ifdef USE_RINTERNALS
#define USE_RINTERNALS_DEFS
#endif
#undef USE_RINTERNALS
#else // FASTR
'''

sexp = '''#ifdef FASTR
#define DATAPTR(x)\t\tR_DATAPTR(x)
void *(R_DATAPTR)(SEXP x);

#define IS_BYTES IS_BYTES
#define IS_LATIN1 IS_LATIN1
#define IS_ASCII IS_ASCII
#define IS_UTF8 IS_UTF8
#define ENC_KNOWN ENC_KNOWN

Rboolean IS_BYTES(SEXP x);
Rboolean IS_LATIN1(SEXP x);
Rboolean IS_ASCII(SEXP x);
Rboolean IS_UTF8(SEXP x);
Rboolean ENC_KNOWN(SEXP x);

#ifdef USE_RINTERNALS
// Some packages rely on this macro not being defined as an indication of old R versions
#define XLENGTH XLENGTH
#endif

#endif
'''
use_internals_begin = '''#if defined (USE_RINTERNALS_DEFS) && (defined (USE_RINTERNALS) || defined (FASTR))
'''
use_internals_end = '''#endif
#ifdef USE_RINTERNALS

'''

def ed_r_internals(gnu_dir):
    r_internals_h = join(gnu_dir, 'Rinternals.h')
    with open(r_internals_h) as f:
        lines = f.readlines()

    use_rinternals_count = 0
    with open('Rinternals.h', 'w') as f:
        for line in lines:
            if '== USE_RINTERNALS section' in line:
                f.write(use_internals_section_and_glob_var_api)
                f.write(line)
                f.write('#endif\n')
            elif 'typedef struct SEXPREC *SEXP' in line:
                f.write(line)
                f.write(sexp)
            elif '#ifdef USE_RINTERNALS' in line:
                if use_rinternals_count > 0:
                    f.write(use_internals_begin)
                else:
                    f.write(line)
                    use_rinternals_count = 1
            elif 'macro version of R_CheckStack' in line:
                f.write(use_internals_end)
                f.write(line)
            elif 'LibExtern' in line:
                var = is_internal_var(line)
                if var:
                    rewrite_var(f, var, line)
                else:
                    f.write(line)
            else:
                f.write(line)

def rewrite_var(f, var, line):
    f.write('#ifdef FASTR\n')
    f.write('LibExtern SEXP FASTR_{0}();\n'.format(var))
    f.write('LibExtern SEXP {0};\n'.format(var))
    f.write('#ifndef NO_FASTR_REDEFINE\n')
    f.write('#define {0} FASTR_{0}()\n'.format(var))
    f.write('#endif\n')
    f.write('#else\n')
    f.write(line)
    f.write('#endif\n')

def is_internal_var(line):
    for var in r_internals_vars:
        varsemi = var + ';'
        if varsemi in line:
            return var
    return None

context_defs = '''#ifdef FASTR
#include <Rinternals.h> // for SEXP
typedef void *CTXT;
extern CTXT FASTR_GlobalContext();
#define R_GlobalContext FASTR_GlobalContext()
extern CTXT R_getGlobalFunctionContext();
extern CTXT R_getParentFunctionContext(CTXT);
extern SEXP R_getContextEnv(CTXT);
extern SEXP R_getContextFun(CTXT);
extern SEXP R_getContextCall(CTXT);
extern SEXP R_getContextSrcRef(CTXT);
extern int R_insideBrowser();
extern int R_isGlobal(CTXT);
extern int R_isEqual(void*, void*);
#else
'''

interactive_rewrite = '''
#include <R_ext/RStartup.h>
#ifdef FASTR
extern Rboolean FASTR_R_Interactive();
extern Rboolean R_Interactive;
#ifndef NO_FASTR_REDEFINE
#define R_Interactive FASTR_R_Interactive()
#endif
#else
'''

def ed_r_interface(gnu_dir):
    r_interface_h = join(gnu_dir, 'Rinterface.h')
    with open(r_interface_h) as f:
        lines = f.readlines()

    with open('Rinterface.h', 'w') as f:
        for line in lines:
            if 'R_GlobalContext' in line:
                f.write(context_defs)
                f.write(line)
                f.write('#endif\n')
            elif 'R_Interactive' in line:
                f.write(interactive_rewrite)
                f.write(line)
                f.write('#endif\n')
            else:
                f.write(line)

def ed_graphicsengine(gnu_dir):
    graphicsengine_h = join(gnu_dir, 'R_ext', 'GraphicsEngine.h')
    with open(graphicsengine_h) as f:
        lines = f.readlines()

    with open(join('R_ext', 'GraphicsEngine.h'), 'w') as f:
        for line in lines:
            if 'MAX_GRAPHICS_SYSTEMS' in line:
                f.write(line.replace('24', '256'))
            else:
                f.write(line)

def ed_rconfig(gnu_dir):
    '''
    GNU R is built with ENABLE_NLS (internationalized strings) but FastR
    does not do that in native code, so we disable it.
    '''
    rconfig_h = join(gnu_dir, 'Rconfig.h')
    with open(rconfig_h) as f:
        lines = f.readlines()

    with open(join('Rconfig.h'), 'w') as f:
        for line in lines:
            if 'ENABLE_NLS' not in line:
                f.write(line)
