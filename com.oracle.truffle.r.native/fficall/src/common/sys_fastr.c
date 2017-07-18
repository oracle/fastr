/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

#include <Rinternals.h>

#define _(Source) (Source)

// selected functions copied from sys-unix.c and sysutils.c:

#include <stdlib.h>
#include <sys/stat.h>

static char newFileName[PATH_MAX];
static int HaveHOME=-1;
static char UserHOME[PATH_MAX];

SEXP installTrChar(SEXP x) {
	// TODO implement correctly (see sysutils.c)
	return installChar(x);
}

/* Only interpret inputs of the form ~ and ~/... */
const char *R_ExpandFileName_unix(const char *s, char *buff)
{
    char *p;

    if(s[0] != '~') return s;
    if(strlen(s) > 1 && s[1] != '/') return s;
    if(HaveHOME < 0) {
	p = getenv("HOME");
	if(p && *p && (strlen(p) < PATH_MAX)) {
	    strcpy(UserHOME, p);
	    HaveHOME = 1;
	} else
	    HaveHOME = 0;
    }
    if(HaveHOME > 0 && (strlen(UserHOME) + strlen(s+1) < PATH_MAX)) {
	strcpy(buff, UserHOME);
	strcat(buff, s+1);
	return buff;
    } else return s;
}

const char *R_ExpandFileName(const char *s)
{
#ifdef HAVE_LIBREADLINE
    if(UsingReadline) {
	const char * c = R_ExpandFileName_readline(s, newFileName);
	/* we can return the result only if tilde_expand is not broken */
	if (!c || c[0]!='~' || (c[1]!='\0' && c[1]!='/'))
	    return c;
    }
#endif
    return R_ExpandFileName_unix(s, newFileName);
}

Rboolean R_FileExists(const char *path)
{
    struct stat sb;
    return stat(R_ExpandFileName(path), &sb) == 0;
}


/* The MSVC runtime has a global to determine whether an unspecified
   file open is in text or binary mode.  We force explicit text mode
   here to avoid depending on that global, which may have been changed
   by user code (most likely in embedded applications of R).
*/

#ifdef Win32

static char * fixmode(const char *mode)
{
    /* Rconnection can have a mode of 4 chars plus a null; we might
     * add one char */
    static char fixedmode[6];
    fixedmode[4] = '\0';
    strncpy(fixedmode, mode, 4);
    if (!strpbrk(fixedmode, "bt")) {
	strcat(fixedmode, "t");
    }
    return fixedmode;
}

static wchar_t * wcfixmode(const wchar_t *mode)
{
    static wchar_t wcfixedmode[6];
    wcfixedmode[4] = L'\0';
    wcsncpy(wcfixedmode, mode, 4);
    if (!wcspbrk(wcfixedmode, L"bt")) {
	wcscat(wcfixedmode, L"t");
    }
    return wcfixedmode;
}

#else
#define fixmode(mode) (mode)
#define wcfixmode(mode) (mode)
#endif

FILE *R_fopen(const char *filename, const char *mode)
{
    return(filename ? fopen(filename, fixmode(mode)) : NULL );
}


FILE *R_popen(const char *command, const char *type)
{
    FILE *fp;
#ifdef __APPLE__
    /* Luke recommends this to fix PR#1140 */
    sigset_t ss;
    sigemptyset(&ss);
    sigaddset(&ss, SIGPROF);
    sigprocmask(SIG_BLOCK, &ss,  NULL);
    fp = popen(command, type);
    sigprocmask(SIG_UNBLOCK, &ss, NULL);
#else
    fp = popen(command, type);
#endif
    return fp;
}


#ifdef HAVE_SYS_WAIT_H
# include <sys/wait.h>
#endif

int R_system(const char *command)
{
    int res;
#ifdef __APPLE__
    /* Luke recommends this to fix PR#1140 */
    sigset_t ss;
    sigemptyset(&ss);
    sigaddset(&ss, SIGPROF);
    sigprocmask(SIG_BLOCK, &ss,  NULL);
//#ifdef HAVE_AQUA
//    if(ptr_CocoaSystem) res = ptr_CocoaSystem(command); else
//#endif
    res = system(command);
    sigprocmask(SIG_UNBLOCK, &ss, NULL);
#else // not APPLE
    res = system(command);
#endif
#ifdef HAVE_SYS_WAIT_H
    if (WIFEXITED(res)) res = WEXITSTATUS(res);
#else
    /* assume that this is shifted if a multiple of 256 */
    if ((res % 256) == 0) res = res/256;
#endif
    if (res == -1) {
	/* this means that system() failed badly - it didn't
	   even get to try to run the shell */
    unimplemented("warning in R_system");
//	warning(_("system call failed: %s"), strerror(errno));
	/* R system() is documented to return 127 on failure, and a lot of
	   code relies on that - it will misinterpret -1 as success */
	res = 127;
    }
    return res;
}


char * R_tmpnam(const char * prefix, const char * tempdir)
{
    return R_tmpnam2(prefix, tempdir, "");
}

/* NB for use with multicore: parent and all children share the same
   session directory and run in parallel.
   So as from 2.14.1, we make sure getpic() is part of the process.
*/
char * R_tmpnam2(const char *prefix, const char *tempdir, const char *fileext)
{
    char tm[PATH_MAX], *res;
    unsigned int n, done = 0, pid = getpid();
#ifdef Win32
    char filesep[] = "\\";
#else
    char filesep[] = "/";
#endif

    if(!prefix) prefix = "";	/* NULL */
    if(!fileext) fileext = "";  /*  "   */

#if RAND_MAX > 16777215
#define RAND_WIDTH 8
#else
#define RAND_WIDTH 12
#endif

    if(strlen(tempdir) + 1 + strlen(prefix) + RAND_WIDTH + strlen(fileext) >= PATH_MAX)
    	error(_("temporary name too long"));

    for (n = 0; n < 100; n++) {
	/* try a random number at the end.  Need at least 6 hex digits */
#if RAND_MAX > 16777215
	snprintf(tm, PATH_MAX, "%s%s%s%x%x%s", tempdir, filesep, prefix, pid, rand(), fileext);
#else
	snprintf(tm, PATH_MAX, "%s%s%s%x%x%x%s", tempdir, filesep, prefix, pid, rand(), rand(), fileext);
#endif
	if(!R_FileExists(tm)) {
	    done = 1;
	    break;
	}
    }
    if(!done)
	error(_("cannot find unused tempfile name"));
    res = (char *) malloc((strlen(tm)+1) * sizeof(char));
    if(!res)
	error(_("allocation failed in R_tmpnam2"));
    strcpy(res, tm);
    return res;
}
