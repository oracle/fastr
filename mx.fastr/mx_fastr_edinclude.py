#
# Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
from os.path import join

'''
Handles all the editing of R FFI header files from the GNUR include directory to the
FastR include directory.
'''
# variables in Rinternals.h that are Java Objects and so remapped to functions
r_internals_vars = ['R_NilValue', 'R_UnboundValue', 'R_MissingArg', 'R_GlobalEnv',
    'R_EmptyEnv', 'R_BaseEnv', 'R_BaseNamespace', 'R_NamespaceRegistry', 'R_Srcref', 'R_Bracket2Symbol',
    'R_BracketSymbol', 'R_BraceSymbol', 'R_ClassSymbol', 'R_DeviceSymbol', 'R_DevicesSymbol',
    'R_DimNamesSymbol', 'R_DimSymbol', 'R_DollarSymbol', 'R_DotsSymbol', 'R_DropSymbol', 'R_LastvalueSymbol',
    'R_LevelsSymbol', 'R_ModeSymbol', 'R_NameSymbol', 'R_NamesSymbol', 'R_NaRmSymbol', 'R_PackageSymbol',
    'R_QuoteSymbol', 'R_RowNamesSymbol', 'R_SeedsSymbol', 'R_SourceSymbol', 'R_TspSymbol', 'R_dot_defined',
    'R_dot_Method', 'R_dot_target', 'R_SrcrefSymbol', 'R_SrcfileSymbol', 'R_NaString', 'R_BlankString',
    'R_DoubleColonSymbol', 'R_BlankScalarString', 'R_BaseSymbol', 'R_baseSymbol', 'R_NamespaceEnvSymbol']

interface_vars = ['R_Home', 'R_TempDir',]


def edinclude(args):
    '''
    edit GNU include files for FASTR
    args[0] path to GNUR include directory
    '''
    ed_r_internals(args[0])
    ed_r_interface(args[0])
    ed_graphicsengine(args[0])
    ed_rconfig(args[0])

use_internals_section = '''#ifdef FASTR
// packages defining USE_INTERNALS expect certain defs (e.g. isNull) to be there
#ifdef USE_RINTERNALS
#define USE_RINTERNALS_DEFS
#endif
#undef USE_RINTERNALS
#else
'''

sexp = '''#ifdef FASTR
typedef void *SEXP;
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

#else
'''
use_internals_begin = '''#if defined (USE_RINTERNALS_DEFS) && (defined (USE_RINTERNALS) || defined (FASTR))
'''
use_internals_end = '''#endif
#ifdef USE_RINTERNALS

'''
preserveObject = '''#ifdef FASTR
SEXP R_PreserveObject_FASTR(SEXP);
#define R_PreserveObject(var) ((var) = R_PreserveObject_FASTR((var)))
#else
'''

def ed_r_internals(gnu_dir):
    r_internals_h = join(gnu_dir, 'Rinternals.h')
    with open(r_internals_h) as f:
        lines = f.readlines()

    use_rinternals_count = 0
    with open('Rinternals.h', 'w') as f:
        for line in lines:
            if '== USE_RINTERNALS section' in line:
                f.write(use_internals_section)
                f.write(line)
                f.write('#endif\n')
            elif 'typedef struct SEXPREC *SEXP' in line:
                f.write(sexp)
                f.write(line)
                f.write('#endif\n')
            elif '#ifdef USE_RINTERNALS' in line:
                if use_rinternals_count > 0:
                    f.write(use_internals_begin)
                else:
                    f.write(line)
                    use_rinternals_count = 1
            elif 'macro version of R_CheckStack' in line:
                f.write(use_internals_end)
                f.write(line)
            elif 'R_PreserveObject' in line:
                f.write(preserveObject)
                f.write(line)
                f.write('#endif\n')
            elif 'LibExtern' in line:
                var = is_internal_var(line)
                if var:
                    rewrite_var(f, var, line)
                else:
                    f.write(line)
            elif 'R_RestartToken' in line:
                f.write('#ifdef FASTR\n')
                f.write('SEXP FASTR_R_RestartToken();\n')
                f.write('#else\n')
                f.write(line)
                f.write('#endif\n')
            else:
                f.write(line)

def rewrite_var(f, var, line):
    f.write('#ifdef FASTR\n')
    f.write('LibExtern SEXP FASTR_{0}();\n'.format(var))
    if var == 'R_baseSymbol': # alias
        f.write('#define {0} FASTR_R_BaseSymbol()\n'.format(var))
    else:
        f.write('#define {0} FASTR_{0}()\n'.format(var))
    f.write('#else\n')
    # Ugly special case, comment split on two lines, just
    if var == 'R_EmptyEnv':
        split = line.split(';')
        f.write(split[0])
        f.write(';\n')
        f.write('#endif\n')
        f.write(split[1])
    else:
        f.write(line)
        f.write('#endif\n')

def is_internal_var(line):
    for var in r_internals_vars:
        varsemi = var + ';'
        if varsemi in line:
            return var
    return None

context_defs = '''#ifdef FASTR
typedef void *CTXT;
typedef void *SEXP;
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

interactive_rewrite = '''#ifdef FASTR
extern Rboolean FASTR_R_Interactive();
#define R_Interactive FASTR_R_Interactive()
#else
'''

rhome_rewrite = '''#ifdef FASTR
extern char* FASTR_R_Home();
#define R_Home FASTR_R_Home()
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
            elif 'R_Home;' in line:
                f.write(rhome_rewrite)
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
            if 'ENABLE_NLS' in line:
                continue
            else:
                f.write(line)
