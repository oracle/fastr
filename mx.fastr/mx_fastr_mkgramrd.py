#
# Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

def mkgramrd(args):
    '''
    converts GNU R gramRd.c into one suitable for invoking via FastR
    '''
    parse_defs = '''
extern SEXP FASTR_R_SrcrefSymbol();
#define R_SrcrefSymbol FASTR_R_SrcrefSymbol()
extern SEXP FASTR_R_SrcfileSymbol();
#define R_SrcfileSymbol FASTR_R_SrcfileSymbol()
extern int R_ParseContextLast;
#define R_EOF -1
#define PARSE_ERROR_SIZE 256
#define PARSE_CONTEXT_SIZE 256
static char    R_ParseErrorMsg[PARSE_ERROR_SIZE];
static char    R_ParseContext[PARSE_CONTEXT_SIZE];
int    R_ParseContextLast;
int    R_ParseContextLine;
int R_ParseError;
extern SEXP FASTR_R_EmptyEnv();
#define R_EmptyEnv FASTR_R_EmptyEnv()
extern SEXP R_NewHashedEnv(SEXP a, SEXP b);

char *dgettext(const char *p, const char *msgid) {
return (char *)msgid;
}

int imax2(int x, int y)
{
    return (x < y) ? y : x;
}
'''

    connect_defs = '''
typedef SEXP Rconnection;
static int Rconn_fgetc(Rconnection con) {
    return -1;
}

'''

    c_parserd = '''
 If there is text then that is read and the other arguments are ignored.

  This is derived fron the function of the same name in the GnuR version.
  Argument checking has already been performed, however, the types of the
  arguments are as per the GnuR version, just passed explicitly (.Call style)
  rather then as a list.
*/
SEXP parseRd(SEXP call, SEXP op, SEXP args, SEXP env) {
    args = CDR(args);   // arg 0 skipped

    SEXP s = R_NilValue, source;
    Rconnection con;
    SEXP conArg;
    Rboolean fragment;
    int ifile; 
    int wcall;
    ParseStatus status;
    SEXP macros;

#if DEBUGMODE
    yydebug = 1;
#endif 

    R_ParseError = 0;
    R_ParseErrorMsg[0] = '\\0';
    
    PushState();

    conArg = CAR(args);  // arg 1
    ifile = asInteger(conArg);
    
	con = R_GetFastRConnection(conArg);

    args = CDR(args);

    source = CAR(args);	  // arg 2
    args = CDR(args);
    /* encoding is unused */
    args = CDR(args);
    if(!isLogical(CAR(args)) || LENGTH(CAR(args)) != 1)   // arg 4
    	error(_("invalid '%s' value"), "verbose");
    parseState.xxDebugTokens = asInteger(CAR(args));	  // arg 4
    args = CDR(args);
    parseState.xxBasename = CHAR(STRING_ELT(CAR(args), 0));	  // arg 5
    args = CDR(args);
    fragment = asLogical(CAR(args));				  // arg 6
    args = CDR(args);
    wcall = asLogical(CAR(args));   // arg 7
    args = CDR(args);
    if (wcall == NA_LOGICAL)
    	error(_("invalid '%s' value"), "warningCalls");
    wCalls = wcall;
    macros = CAR(args);						  // arg 8
    args = CDR(args);
    warnDups = asLogical(CAR(args));	  // arg 9

    if (ifile >= 3) {/* file != "" */
	s = R_ParseRd(con, &status, source, fragment, macros);
	PopState();
    }
    return s;
}

// TODO deparseRd
'''
    with open(args[0]) as f:
        lines = f.readlines()

    with open(args[1], 'w') as f:
        i = 0
        while i < len(lines):
            line = lines[i]
            sline = line.rstrip()
            if sline == '#include <Defn.h>':
                line = '//' + line
                f.write(line)
                f.write('#include <Rinternals.h>\n')
            elif sline == '#include <Parse.h>':
                f.write('#include <R_ext/Parse.h>\n')
            elif 'bison creates a non-static symbol yylloc' in sline:
                f.write(parse_defs)
                f.write(line)
            elif '#include <Rmath.h>' in sline:
                line = '//' + line
                f.write(line)
                f.write('#include "gramRd_fastr.h"\n')
            elif sline == '#include "Rconnections.h"':
                line = '//' + line
                f.write(line)
                f.write(connect_defs)
            elif 'c = Rconn_fgetc(con_parse);' in sline:
                f.write('    c = callGetCMethod(con_parse);\n')
            elif sline == 'static void con_cleanup(void *data)':
                # skip
                i = i + 5
            elif 'installTrChar(STRING_ELT(thename, 0))' in line:
                line = line.replace('installTrChar(STRING_ELT(thename, 0))', 'install(CHAR(STRING_ELT(thename, 0)))')
                f.write(line)
            elif '.External2(C_parseRd' in sline:
                f.write(line)
                f.write(c_parserd)
                break
            else:
                f.write(line)
            i = i + 1
