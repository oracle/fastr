/*
 * Copyright (c) 1995-1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2014, The R Core Team
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */


#include <rffiutils.h>
#include <Riconv.h>
#include <errno.h>
#include <iconv.h>

// Implementation of following functions is taken verbatim from GNUR from sysutils.c
// We do not include the whole file as it contains more functions than we need

void * Riconv_open (const char* tocode, const char* fromcode)
{
#if defined Win32 || __APPLE__
// These two support "utf8"
# ifdef Win32
    const char *cp = "ASCII";
#  ifndef SUPPORT_UTF8_WIN32 /* Always, at present */
    char to[20] = ""; 
    if (localeCP > 0) {snprintf(to, 20, "CP%d", localeCP); cp = to;}
#  endif
# else /* __APPLE__ */
    const char *cp = "UTF-8";
    if (latin1locale) cp = "ISO-8859-1";
    // TODO: copy locale2charset from GNUR
    else if (!utf8locale) cp = locale2charset(NULL);
# endif
    if (!*tocode && !*fromcode) return iconv_open(cp, cp);
    if(!*tocode)  return iconv_open(cp, fromcode);
    else if(!*fromcode) return iconv_open(tocode, cp);
    else return iconv_open(tocode, fromcode);
#else
// "utf8" is not valid but people keep on using it
    const char *to = tocode, *from = fromcode;
    // TODO: strcasecmp? Copy from GNUR?
    if(strcasecmp(tocode, "utf8") == 0) to = "UTF-8";
    if(strcasecmp(fromcode, "utf8") == 0) from = "UTF-8";
    return iconv_open(to, from);
#endif
}

#ifndef ICONV_CONST
# define ICONV_CONST
#endif

size_t Riconv (void *cd, const char **inbuf, size_t *inbytesleft,
           char **outbuf, size_t *outbytesleft)
{
    /* here libiconv has const char **, glibc has char ** for inbuf */
    return iconv((iconv_t) cd, (ICONV_CONST char **) inbuf, inbytesleft,
         outbuf, outbytesleft);
}

int Riconv_close (void *cd)
{
    return iconv_close((iconv_t) cd);
}
