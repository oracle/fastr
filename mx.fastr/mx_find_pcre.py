#
# Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
import os
import platform
import mx

def findpcre(args):
    '''
    GNU R is built using GNUR_LDFLAGS_OVERRIDE to specify the location of necessary external libraries,
    but this isn't captured anywhere, so we re-analyze here
    '''
    if not os.environ.has_key('GNUR_LDFLAGS_OVERRIDE'):
        mx.abort('GNUR_LDFLAGS_OVERRIDE is not set')
    parts = os.environ['GNUR_LDFLAGS_OVERRIDE'].split(' ')
    ext = '.dylib' if platform.system() == 'Darwin' else '.so'
    name = 'libpcre' + ext
    for part in parts:
        path = part.lstrip('-I')
        for f in os.listdir(path):
            if name == f:
                mx.log(os.path.join(path, f))
                return 0
    mx.abort(name + ' not found')
