#!/bin/bash
#
# This material is distributed under the GNU General Public License
# Version 2. You may review the terms of this license at
# http://www.gnu.org/licenses/gpl-2.0.html
#
# Copyright (c) 1995-2015, The R Core Team
# Copyright (c) 2003, The R Foundation
# Copyright (c) 2015, Oracle and/or its affiliates
#
# All rights reserved.
#

# Fledgling R command to startup FastR
# Currently all R CMD calls are forwarded to GnuR
#
GNUR=`which R`

source="${BASH_SOURCE[0]}"
while [ -h "$source" ] ; do source="$(readlink "$source")"; done
PRIMARY_PATH="$( cd -P "$( dirname "$source" )" && pwd )"/..

case ${1} in
    CMD)
      exec $GNUR "$@";;
    *)
      exec mx --primary-suite-path $PRIMARY_PATH R "$@";;
 esac
