/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 *  Copyright (C) 1997--2018  The R Core Team
 *  Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, a copy is available at
 *  https://www.R-project.org/Licenses/
 */

/* <UTF8> char here is mainly handled as a whole string.
   Does need readline to support it.
   Appending \n\0 is OK in UTF-8, not general MBCS.
   Removal of \r is OK on UTF-8.
   ? use of isspace OK?
 */


/* See system.txt for a description of functions */

/* select() is essential here, but configure has required it */

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#define R_USE_SIGNALS 1
#include <Defn.h>
#include <Internal.h>

#ifdef HAVE_STRINGS_H
   /* may be needed to define bzero in FD_ZERO (eg AIX) */
  #include <strings.h>
#endif

#include "Fileio.h"
#include <R_ext/Riconv.h>
#include <R_ext/Print.h> // for REprintf

#define __SYSTEM__
/* includes <sys/select.h> and <sys/time.h> */
#include <R_ext/eventloop.h>
#undef __SYSTEM__

#ifdef HAVE_UNISTD_H
# include <unistd.h>		/* for unlink */
#endif

/*
  The following provides a version of select() that catches interrupts
  and handles them using the supplied interrupt handler or the default
  one if NULL is supplied.  The interrupt handler must exit using a
  longjmp.  If the supplied timout value os zero, select is called
  without setting up an error handler since it should return
  immediately.
 */

 static SIGJMP_BUF seljmpbuf;
 
 static RETSIGTYPE (*oldSigintHandler)(int) = SIG_DFL;
 
 typedef void (*sel_intr_handler_t)(void);
 
 static RETSIGTYPE NORET handleSelectInterrupt(int dummy)
 {
     signal(SIGINT, oldSigintHandler);
     SIGLONGJMP(seljmpbuf, 1);
 }
 
 int R_SelectEx(int  n,  fd_set  *readfds,  fd_set  *writefds,
 	       fd_set *exceptfds, struct timeval *timeout,
 	       void (*intr)(void))
 {
    if (timeout != NULL && timeout->tv_sec == 0 && timeout->tv_usec == 0)
 	/* Is it right for select calls with a timeout to be
 	   non-interruptable? LT */
 	return select(n, readfds, writefds, exceptfds, timeout);
     else {
 	volatile sel_intr_handler_t myintr = intr != NULL ?
 	    intr : onintrNoResume;
 	volatile int old_interrupts_suspended = R_interrupts_suspended;
 	if (SIGSETJMP(seljmpbuf, 1)) {
 	    myintr();
 	    R_interrupts_suspended = old_interrupts_suspended;
 	    error(_("interrupt handler must not return"));
 	    return 0; /* not reached */
 	}
 	else {
 	    int val;

	    /* make sure interrupts are enabled -- this will be
	       restored if there is a LONGJMP from myintr() to another
	       context. */
	    R_interrupts_suspended = FALSE;

	    /* install a temporary signal handler for breaking out of
	       a blocking select */
	    oldSigintHandler = signal(SIGINT, handleSelectInterrupt);

	    /* once the new sinal handler is in place we need to check
	       for and handle any pending interrupt registered by the
	       standard handler. */
	    if (R_interrupts_pending)
		myintr();

	    /* now do the (possibly blocking) select, restore the
	       signal handler, and return the result of the select. */
	    val = select(n, readfds, writefds, exceptfds, timeout);
	    signal(SIGINT, oldSigintHandler);
	    R_interrupts_suspended = old_interrupts_suspended;
	    return val;
	}
    }
 }

/*
   This can be reset by the initialization routines which
   can ignore stdin, etc..
*/
InputHandler *R_InputHandlers = NULL;

/*
  Creates and registers a new InputHandler with the linked list `handlers'.
  This sets the global variable InputHandlers if it is not already set.

  Returns the newly created handler which can be used in a call to
  removeInputHandler.
 */
InputHandler *
addInputHandler(InputHandler *handlers, int fd, InputHandlerProc handler,
		int activity)
{
    InputHandler *input, *tmp;
    input = (InputHandler*) calloc(1, sizeof(InputHandler));

    input->activity = activity;
    input->fileDescriptor = fd;
    input->handler = handler;

    tmp = handlers;

    if(handlers == NULL) {
	R_InputHandlers = input;
	return(input);
    }

    /* Go to the end of the list to append the new one.  */
    while(tmp->next != NULL) {
	tmp = tmp->next;
    }
    tmp->next = input;

    return(input);
}

/*
  Removes the specified handler from the linked list.

  See getInputHandler() for first locating the target handler instance.
 */
int
removeInputHandler(InputHandler **handlers, InputHandler *it)
{
    InputHandler *tmp;

    /* If the handler is the first one in the list, move the list to point
       to the second element. That's why we use the address of the first
       element as the first argument.
    */

    if (it == NULL) return(0);

    if(*handlers == it) {
	*handlers = (*handlers)->next;
	free(it);
	return(1);
    }

    tmp = *handlers;

    while(tmp) {
	if(tmp->next == it) {
	    tmp->next = it->next;
	    free(it);
	    return(1);
	}
	tmp = tmp->next;
    }

    return(0);
}


InputHandler *
getInputHandler(InputHandler *handlers, int fd)
{
    InputHandler *tmp;
    tmp = handlers;

    while(tmp != NULL) {
	if(tmp->fileDescriptor == fd)
	    return(tmp);
	tmp = tmp->next;
    }

    return(tmp);
}

/*
 Arrange to wait until there is some activity or input pending
 on one of the file descriptors to which we are listening.

 We could make the file descriptor mask persistent across
 calls and change it only when a listener is added or deleted.
 Later.

 This replaces the previous version which looked only on stdin and the
 X11 device connection.  This allows more than one X11 device to be
 open on a different connection. Also, it allows connections a la S4
 to be developed on top of this mechanism.
*/

/* A package can enable polled event handling by making R_PolledEvents
   point to a non-dummy routine and setting R_wait_usec to a suitable
   timeout value (e.g. 100000) */

static void nop(void){}

void (* R_PolledEvents)(void) = nop;
int R_wait_usec = 0; /* 0 means no timeout */

/* For X11 devices */
void (* Rg_PolledEvents)(void) = nop;
int Rg_wait_usec = 0;


static int setSelectMask(InputHandler *, fd_set *);


fd_set *R_checkActivityEx(int usec, int ignore_stdin, void (*intr)(void))
{
    int maxfd;
    struct timeval tv;
    static fd_set readMask;

    if (R_interrupts_pending) {
	if (intr != NULL) intr();
	else onintr();
    }

    /* Solaris (but not POSIX) requires these times to be normalized.
       POSIX requires up to 31 days to be supported, and we only
       use up to 2147 secs here.
     */
    tv.tv_sec = usec/1000000;
    tv.tv_usec = usec % 1000000;
    maxfd = setSelectMask(R_InputHandlers, &readMask);
    if (ignore_stdin)
	FD_CLR(fileno(stdin), &readMask);
    if (R_SelectEx(maxfd+1, &readMask, NULL, NULL,
		   (usec >= 0) ? &tv : NULL, intr) > 0)
	return(&readMask);
    else
	return(NULL);
}

fd_set *R_checkActivity(int usec, int ignore_stdin)
{
    return R_checkActivityEx(usec, ignore_stdin, NULL);
}

/*
  Create the mask representing the file descriptors select() should
  monitor and return the maximum of these file descriptors so that
  it can be passed directly to select().

  If the first element of the handlers is the standard input handler
  then we set its file descriptor to the current value of stdin - its
  file descriptor.
 */

static int
setSelectMask(InputHandler *handlers, fd_set *readMask)
{
    int maxfd = -1;
    InputHandler *tmp = handlers;
    FD_ZERO(readMask);

    while(tmp) {
	FD_SET(tmp->fileDescriptor, readMask);
	maxfd = maxfd < tmp->fileDescriptor ? tmp->fileDescriptor : maxfd;
	tmp = tmp->next;
    }

    return(maxfd);
}

void R_runHandlers(InputHandler *handlers, fd_set *readMask)
{
    InputHandler *tmp = handlers, *next;

    if (readMask == NULL) {
		Rg_PolledEvents();
		R_PolledEvents();
    } else {
		while(tmp) {
	    	/* Do this way as the handler function might call
		       removeInputHandlers */
		    next = tmp->next;
		    if(FD_ISSET(tmp->fileDescriptor, readMask)
		       && tmp->handler != NULL)
			tmp->handler((void*) tmp->userData);
		    tmp = next;
		}
	}
}

/* The following routine is still used by the internet routines, but
 * it should eventually go away. */

InputHandler *
getSelectedHandler(InputHandler *handlers, fd_set *readMask)
{
    InputHandler *tmp = handlers;

    while(tmp) {
	if(FD_ISSET(tmp->fileDescriptor, readMask))
	    return(tmp);
	tmp = tmp->next;
    }
    /* Now deal with the first one. */
    if(FD_ISSET(handlers->fileDescriptor, readMask))
	return(handlers);

    return((InputHandler*) NULL);
}

#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pthread.h>
#include <errno.h>

static void
handleInterrupt(void)
{
    onintrNoResume();
}

char hint1 = 64;
char hint2 = 65;
fd_set *what;
char* fifoInPath;
char* fifoOutPath;

static int notifyExecutorAndWait() {
	int fd = open(fifoInPath, O_WRONLY);
	if (fd < 0) {
	    return errno;
    }
	int res = write(fd, &hint1, 1);
	if (res < 0) {
	    return errno;
    }
	res = close(fd);
	if (res < 0) {
	    return errno;
    }
	
	// wait until the executor confirms the dispatching of the handlers is done
	fd = open(fifoOutPath, O_RDONLY);
	if (fd < 0) {
	    return errno;
    }
	char confirmed[1];
	res = read(fd, confirmed, sizeof(confirmed) + 1);
	if (res < 0) {
	    return errno;
    }
	res = close(fd);
	if (res < 0) {
	    return errno;
    }

	return 0;
}

int dispatchHandlers() {
	R_runHandlers(R_InputHandlers, what);
	
	int fd = open(fifoOutPath, O_WRONLY);
	if (fd < 0) {
	    return errno;
    }
	int res = write(fd, &hint2, 1);
	if (res < 0) {
	    return errno;
    }
	res = close(fd);
	if (res < 0) {
	    return errno;
    }

	return 0;
}

static void *eventLoop(void *params) {

	int wt = 1000000;
	
	for (;;) {
		fflush(stdout);
		
		what = R_checkActivityEx(wt, 0, handleInterrupt);
		if (what != NULL) {
			int res = notifyExecutorAndWait();
			if (res != 0) {
				return NULL;
			}
		}
	}
	
	return NULL;
}

int initEventLoop(char* fifoInPathParam, char* fifoOutPathParam) {
	fifoInPath = malloc(strlen(fifoInPathParam) * (sizeof(char) + 1));
	strcpy(fifoInPath, fifoInPathParam);

	fifoOutPath = malloc(strlen(fifoOutPathParam) * (sizeof(char) + 1));
	strcpy(fifoOutPath, fifoOutPathParam);
    
    int res = mkfifo(fifoInPath, 0666);
    if (res != 0 && errno != EEXIST) {
	    return errno;
    }

    res = mkfifo(fifoOutPath, 0666);
    if (res != 0 && errno != EEXIST) {
	    return errno;
    }
    
    pthread_t eventLoopThread;
	if(pthread_create(&eventLoopThread, NULL, eventLoop, NULL)) {
		fprintf(stderr, "Error creating dispatch thread\n");
	}

	return 0;
}
