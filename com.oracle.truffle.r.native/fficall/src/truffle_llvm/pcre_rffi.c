/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2015,  The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
#include <rffiutils.h>

#include "../truffle_common/pcre_rffi.h"

char *call_pcre_maketables() {
	return pcre_maketables();
}

int call_pcre_exec(long code, long extra, char *subject, int subjectLength, int startOffset, int options, int *ovectorElems, int ovectorLen) {
	int rc = pcre_exec((void *) code, (void *) extra, (char *) subject, subjectLength, startOffset, options,
			ovectorElems, ovectorLen);
	return rc;
}

