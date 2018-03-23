#!/bin/bash
#
# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

set -e

# Resolve the location of this script
source="${BASH_SOURCE[0]}"
while [ -h "$source" ] ; do
  prev_source="$source"
  source="$(readlink "$source")";
  if [[ "$source" != /* ]]; then
    # if the link was relative, it was relative to where it came from
    dir="$( cd -P "$( dirname "$prev_source" )" && pwd )"
    source="$dir/$source"
  fi
done
dir="$( cd -P "$( dirname "$source" )" && pwd )"

: ${R_HOME?"R_HOME must point to FastR directory"}
: ${NFI_LIB?"NFI_LIB must point to libtrufflenfi.so located in mxbuild directory of Truffle"}

echo "R_HOME: $R_HOME"
echo "NFI_LIB: $NFI_LIB"
echo "Rclasspath: "
$dir/../../bin/execRextras/Rclasspath

echo "Testing 'main' embedding example..."
(cd $dir/bin; ./main -Dtruffle.nfi.library=$NFI_LIB --vanilla < $dir/src/main.input > $dir/main.actual.output 2>&1)
if ! diff -q $dir/main.actual.output $dir/src/main.expected.output > /dev/null 2>&1; then
    echo "'main' embedding test failed"
    echo "for details see $dir/main.actual.output $dir/src/main.expected.output"
    echo "to run this test: mx rembedtest"
    exit 1
fi

echo "Testing 'embedded' embedding example..."
(cd $dir/bin; ./embedded -Dtruffle.nfi.library=$NFI_LIB --vanilla > $dir/embedded.actual.output 2>&1)
if ! diff -q $dir/embedded.actual.output $dir/src/embedded.expected.output > /dev/null 2>&1; then
    echo "'embedded' embedding test failed"
    echo "for details see $dir/embedded.actual.output $dir/src/embedded.expected.output"
    echo "to run this test: mx rembedtest"
    exit 2
fi

echo "DONE"
