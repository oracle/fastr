#include <R.h>
#include <Rinternals.h>
#include "Rinit.h"
#include "Rcallbacks.h"
#include "Rdecl.h"

/*-------------------------------------------------------------------*
 * UNIX initialization (includes Darwin/Mac OS X)                    *
 *-------------------------------------------------------------------*/

#ifndef Win32

#define R_INTERFACE_PTRS 1
#define CSTACK_DEFNS 1
#include <Rinterface.h>
/* and SaveAction is not officially exported */
extern SA_TYPE SaveAction;


int initR(int argc, char **argv) {
    structRstart rp;
    Rstart Rp = &rp;
    /* getenv("R_HOME","/Library/Frameworks/R.framework/Resources",1); */
    if (!getenv("R_HOME")) {
        fprintf(stderr, "R_HOME is not set. Please set all required environment variables before running this program.\n");
        return -1;
    }

    /* this is probably unnecessary, but we could set any other parameters here */
    R_DefParams(Rp);
    Rp->NoRenviron = 0;
    R_SetParams(Rp);

#ifdef RIF_HAS_RSIGHAND
    R_SignalHandlers=0;
#endif
    {
      int stat=Rf_initialize_R(argc, argv);
      if (stat<0) {
        printf("Failed to initialize embedded R! (stat=%d)\n",stat);
        return -1;
      }
    }

#ifdef RIF_HAS_RSIGHAND
    R_SignalHandlers=0;
#endif
    /* disable stack checking, because threads will thow it off */
    R_CStackLimit = (uintptr_t) -1;

#ifdef JGR_DEBUG
    printf("R primary initialization done. Setting up parameters.\n");
#endif

    R_Outputfile = NULL;
    R_Consolefile = NULL;
    R_Interactive = 1;
    SaveAction = SA_SAVEASK;

    /* ptr_R_Suicide = Re_Suicide; */
    /* ptr_R_CleanUp = Re_CleanUp; */
    ptr_R_ShowMessage = Re_ShowMessage;
    ptr_R_ReadConsole = Re_ReadConsole;
    ptr_R_WriteConsole = NULL;
    ptr_R_WriteConsoleEx = Re_WriteConsoleEx;
    ptr_R_ResetConsole = Re_ResetConsole;
    ptr_R_FlushConsole = Re_FlushConsole;
    ptr_R_ClearerrConsole = Re_ClearerrConsole;
    ptr_R_Busy = Re_Busy;
    ptr_R_ShowFiles = Re_ShowFiles;
    ptr_R_ChooseFile = Re_ChooseFile;
	ptr_R_loadhistory = Re_loadhistory;
    ptr_R_savehistory = Re_savehistory;

#ifdef JGR_DEBUG
	printf("Setting up R event loop\n");
#endif

    setup_Rmainloop();

#ifdef JGR_DEBUG
    printf("R initialized.\n");
#endif

    return 0;
}

void initRinside() {
    /* disable stack checking, because threads will thow it off */
    R_CStackLimit = (uintptr_t) -1;
}

#else

/*-------------------------------------------------------------------*
 * Windows initialization is different and uses Startup.h            *
 *-------------------------------------------------------------------*/

#define NONAMELESSUNION
#include <windows.h>
#include <winreg.h>
#include <stdio.h>
#include <stdlib.h>

/* before we include RStatup.h we need to work around a bug in it for Win64:
   it defines wrong R_size_t if R_SIZE_T_DEFINED is not set */
#if defined(WIN64) && ! defined(R_SIZE_T_DEFINED)
#include <stdint.h>
#define R_size_t uintptr_t
#define R_SIZE_T_DEFINED 1
#endif

#include "R_ext/RStartup.h"

#ifndef WIN64
/* according to fixed/config.h Windows has uintptr_t, my windows hasn't */
#if !defined(HAVE_UINTPTR_T) && !defined(uintptr_t) && !defined(_STDINT_H)
typedef unsigned uintptr_t;
#endif
#endif
extern __declspec(dllimport) uintptr_t R_CStackLimit; /* C stack limit */
extern __declspec(dllimport) uintptr_t R_CStackStart; /* Initial stack address */

/* for signal-handling code */
/* #include "psignal.h" - it's not included, so just get SIGBREAK */
#define	SIGBREAK 21	/* to readers pgrp upon background tty read */

/* one way to allow user interrupts: called in ProcessEvents */
#ifdef _MSC_VER
__declspec(dllimport) int UserBreak;
#else
#ifndef WIN64
#define UserBreak     (*_imp__UserBreak)
#endif
extern int UserBreak;
#endif

/* calls into the R DLL */
extern char *getDLLVersion();
extern void R_DefParams(Rstart);
extern void R_SetParams(Rstart);
extern void setup_term_ui(void);
extern void ProcessEvents(void);
extern void end_Rmainloop(void), R_ReplDLLinit(void);
extern int R_ReplDLLdo1();
extern void run_Rmainloop(void);

void myCallBack()
{
    /* called during i/o, eval, graphics in ProcessEvents */
}

#ifndef YES
#define YES    1
#endif
#ifndef NO
#define NO    -1
#endif
#ifndef CANCEL
#define CANCEL 0
#endif

int myYesNoCancel(char *s)
{
    char  ss[128];
    unsigned char a[3];

    sprintf(ss, "%s [y/n/c]: ", s);
    Re_ReadConsole(ss, a, 3, 0);
    switch (a[0]) {
    case 'y':
    case 'Y':
	return YES;
    case 'n':
    case 'N':
	return NO;
    default:
	return CANCEL;
    }
}

static void my_onintr(int sig)
{
    UserBreak = 1;
}

static char Rversion[25], RUser[MAX_PATH], RHome[MAX_PATH];

int initR(int argc, char **argv)
{
    structRstart rp;
    Rstart Rp = &rp;
    char *p;
    char rhb[MAX_PATH+10];
    DWORD t, s = MAX_PATH;
    HKEY k;
    int cvl;

    sprintf(Rversion, "%s.%s", R_MAJOR, R_MINOR);
    cvl=strlen(R_MAJOR)+2;
    if(strncmp(getDLLVersion(), Rversion, cvl) != 0) {
        char msg[512];
	sprintf(msg, "Error: R.DLL version does not match (DLL: %s, expecting: %s)\n", getDLLVersion(), Rversion);
	fprintf(stderr, msg);
	MessageBox(0, msg, "Version mismatch", MB_OK|MB_ICONERROR);
	return -1;
    }

    R_DefParams(Rp);
    if(getenv("R_HOME")) {
	strcpy(RHome, getenv("R_HOME"));
    } else { /* fetch R_HOME from the registry - try preferred architecture first */
#ifdef WIN64
      const char *pref_path = "SOFTWARE\\R-core\\R64";
#else
      const char *pref_path = "SOFTWARE\\R-core\\R32";
#endif
      if ((RegOpenKeyEx(HKEY_LOCAL_MACHINE, pref_path, 0, KEY_QUERY_VALUE, &k) != ERROR_SUCCESS ||
	   RegQueryValueEx(k, "InstallPath", 0, &t, (LPBYTE) RHome, &s) != ERROR_SUCCESS) &&
	  (RegOpenKeyEx(HKEY_CURRENT_USER, pref_path, 0, KEY_QUERY_VALUE, &k) != ERROR_SUCCESS ||
           RegQueryValueEx(k, "InstallPath", 0, &t, (LPBYTE) RHome, &s) != ERROR_SUCCESS) &&
	  (RegOpenKeyEx(HKEY_LOCAL_MACHINE, "SOFTWARE\\R-core\\R", 0, KEY_QUERY_VALUE, &k) != ERROR_SUCCESS ||
	   RegQueryValueEx(k, "InstallPath", 0, &t, (LPBYTE) RHome, &s) != ERROR_SUCCESS) &&
	  (RegOpenKeyEx(HKEY_CURRENT_USER, "SOFTWARE\\R-core\\R", 0, KEY_QUERY_VALUE, &k) != ERROR_SUCCESS ||
           RegQueryValueEx(k, "InstallPath", 0, &t, (LPBYTE) RHome, &s) != ERROR_SUCCESS)) {
	fprintf(stderr, "R_HOME must be set or R properly installed (\\Software\\R-core\\R\\InstallPath registry entry must exist).\n");
	MessageBox(0, "R_HOME must be set or R properly installed (\\Software\\R-core\\R\\InstallPath registry entry must exist).\n", "Can't find R home", MB_OK|MB_ICONERROR);
	return -2;
      }
      sprintf(rhb,"R_HOME=%s",RHome);
      putenv(rhb);
    }
    /* on Win32 this should set R_Home (in R_SetParams) as well */
    Rp->rhome = RHome;
    /*
     * try R_USER then HOME then working directory
     */
    if (getenv("R_USER")) {
	strcpy(RUser, getenv("R_USER"));
    } else if (getenv("HOME")) {
	strcpy(RUser, getenv("HOME"));
    } else if (getenv("HOMEDIR")) {
	strcpy(RUser, getenv("HOMEDIR"));
	strcat(RUser, getenv("HOMEPATH"));
    } else
	GetCurrentDirectory(MAX_PATH, RUser);
    p = RUser + (strlen(RUser) - 1);
    if (*p == '/' || *p == '\\') *p = '\0';
    Rp->home = RUser;
    Rp->ReadConsole = Re_ReadConsole;
    Rp->WriteConsole = NULL;
    Rp->WriteConsoleEx = Re_WriteConsoleEx;

    Rp->Busy = Re_Busy;
    Rp->ShowMessage = Re_ShowMessage;
    Rp->YesNoCancel = myYesNoCancel;
    Rp->CallBack = myCallBack;
    Rp->CharacterMode = LinkDLL;

    Rp->R_Quiet = FALSE;
    Rp->R_Interactive = TRUE;
    Rp->RestoreAction = SA_RESTORE;
    Rp->SaveAction = SA_SAVEASK;
    /* process common command line options */
    R_common_command_line(&argc, argv, Rp);
    /* what is left should be assigned to args */
    R_set_command_line_arguments(argc, argv);

    R_SetParams(Rp); /* so R_ShowMessage is set */
    R_SizeFromEnv(Rp);
    R_SetParams(Rp);

    /* R_SetParams implicitly calls R_SetWin32 which sets the
       stack start/limit which we need to override */
    R_CStackLimit = (uintptr_t) -1;

    FlushConsoleInputBuffer(GetStdHandle(STD_INPUT_HANDLE));

    signal(SIGBREAK, my_onintr);
    setup_term_ui();
    setup_Rmainloop();

    return 0;
}

void initRinside() {
    /* disable stack checking, because threads will thow it off */
    R_CStackLimit = (uintptr_t) -1;
}

#endif

