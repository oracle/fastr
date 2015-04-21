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

# Startup FastR using the mx tool (development)
# This is exec'ed by the (generic) R script in the parent directory.
#

#echo args="$@"
#printenv | fgrep R_

source="${BASH_SOURCE[0]}"
while [ -h "$source" ] ; do source="$(readlink "$source")"; done
PRIMARY_PATH="$( cd -P "$( dirname "$source" )" && pwd )"/../..

exec mx $MX_R_GLOBAL_ARGS --primary-suite-path $PRIMARY_PATH R $MX_R_CMD_ARGS "$@"
