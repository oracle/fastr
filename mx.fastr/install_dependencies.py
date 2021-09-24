#
# Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

"""
A (Python 2 friendly) script that installs all the dependencies in order to build FastR from sources.
During fastr build, GNU-R is built from sources. The listed dependencies are mostly dependencies
for GNU-R.

Supported distributions are:
- Oracle Linux Server 8 and newer.
- Ubuntu 18.04 and newer.
- Fedora 34 and newer.
"""

import os
import subprocess

SUPPORTED_DISTROS = ["Oracle Linux Server 8", "Ubuntu 18.04 and newer", "Fedora 34 and newer"]

UBUNTU_PACKAGES = [
    # Dependencies for Sulong and FastR:
    "cmake",
    "ed",
    "build-essential",
    # Dependencies for GNU-R:
    "g++",
    "unzip",
    "libpcre3",
    # libpcre2-8-0 is installed on Ubuntu by default, but we enumerate it here just to be sure
    "libpcre2-8-0",
    "zlibc",
    "curl",
    "gfortran",
    "bzip2",
    "pkg-config",
    "liblzma-dev",
    "libpcre3-dev",
    "libpcre2-dev",
    "libreadline-dev",
    "zlib1g-dev",
    "libbz2-dev",
    "libcurl4",
    "libcurl4-openssl-dev",
    "libmpc-dev",
    "libssl-dev",
]

FEDORA_PACKAGES = [
    # Dependencies for Sulong and FastR:
    "cmake",
    "ed",
    # Dependencies for GNU-R:
    "make",
    "curl",
    "diffutils",
    "findutils",
    "gcc-c++",
    "gcc-gfortran",
    "unzip",
    "bzip2-devel",
    "pkgconfig",
    "pcre-devel",
    # pcre2-devel is available only in Oraclelinux 8, not in 7
    "pcre2-devel",
    "zlib-devel",
    "xz-devel",
    "readline-devel",
    "libcurl-devel",
]

# Packages are the same for Fedora 34 and OL 8.
OL_PACKAGES = FEDORA_PACKAGES

class _LinuxDistribution:
    def __init__(self, name, version):
        self.name = name
        self.version = version

def _unsupported_distribution(distr):
    print("Unsupported Linux distribution: %s %s" % (distr.name, distr.version))  # pylint: disable=superfluous-parens
    print("Supported distros are: %s" % ", ".join(SUPPORTED_DISTROS))  # pylint: disable=superfluous-parens
    exit(1)

def _install_ubuntu_dependencies(distr):
    major_version = int(distr.version.split(".")[0])
    if major_version < 18:
        print("Ubuntu is supported in version 18.04 or newer, current version is %s", distr.version)  # pylint: disable=superfluous-parens
        exit(1)
    cmd = ["sudo", "apt-get", "install"] + UBUNTU_PACKAGES
    print("Running " + " ".join(cmd))  # pylint: disable=superfluous-parens
    ret = subprocess.call(cmd)
    if ret != 0:
        print("Ubuntu dependencies installation failed")  # pylint: disable=superfluous-parens
        exit(1)
    pass

def _install_fedora_dependencies(distr):
    if int(distr.version) < 34:
        print("Fedora is supported in version 34 or newer, current version is %s" % distr.version)  # pylint: disable=superfluous-parens
        exit(1)
    cmd = ["sudo", "dnf", "install"] + FEDORA_PACKAGES
    print("Running " + " ".join(cmd))  # pylint: disable=superfluous-parens
    ret = subprocess.call(cmd)
    if ret != 0:
        print("Fedora dependencies installation failed")  # pylint: disable=superfluous-parens
        exit(1)

def _install_ol_dependencies(distr):
    major_version = int(distr.version.split(".")[0])
    if major_version < 8:
        print("Oracle Linux Server is supported in version 8 or newer, current version is %s" % distr.version)  # pylint: disable=superfluous-parens
        exit(1)
    # dnf command is available from OL 8.
    cmd = ["sudo", "dnf", "install"] + FEDORA_PACKAGES
    print("Running " + " ".join(cmd))  # pylint: disable=superfluous-parens
    ret = subprocess.call(cmd)
    if ret != 0:
        print("oraclelinux dependencies installation failed")  # pylint: disable=superfluous-parens
        exit(1)

def _get_distribution():
    if not os.path.exists("/etc/os-release"):
        print("Unknown Linux distribution - /etc/os-release does not exist")  # pylint: disable=superfluous-parens
        exit(1)
    assert os.path.exists("/etc/os-release")
    with open("/etc/os-release", "r") as os_release_file:
        for line in os_release_file.readlines():
            line = line.strip()
            if line.startswith("NAME"):
                distr_name = line.split("=")[1]
                distr_name = distr_name.strip('"')
            elif line.startswith("VERSION_ID"):
                distr_version = line.split("=")[1]
                distr_version = distr_version.strip('"')
    return _LinuxDistribution(distr_name, distr_version)


def install_dependencies():
    distr = _get_distribution()
    print("Current distribution is %s:%s" % (distr.name, distr.version))  # pylint: disable=superfluous-parens
    if distr.name == "Ubuntu":
        _install_ubuntu_dependencies(distr)
    elif distr.name == "Fedora":
        _install_fedora_dependencies(distr)
    elif distr.name == "Oracle Linux Server":
        _install_ol_dependencies(distr)
    else:
        _unsupported_distribution(distr)

