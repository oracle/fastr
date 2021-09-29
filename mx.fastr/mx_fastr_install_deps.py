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
from argparse import ArgumentParser, RawTextHelpFormatter

import mx
import os
import subprocess

DRY_RUN = False

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
    "curl",
    "gfortran",
    "bzip2",
    "pkg-config",
    "liblzma-dev",
    "libpcre3-dev",
    "libpcre2-dev",
    "libreadline-dev",
    "zlib1g",
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

UBUNTU_INSTALL_COMMAND = ["sudo", "apt-get", "install"] + UBUNTU_PACKAGES
FEDORA_INSTALL_COMMAND = ["sudo", "dnf", "install"] + FEDORA_PACKAGES
OL_INSTALL_COMMAND = ["sudo", "yum", "install"] + FEDORA_PACKAGES

UBUNTU_DISTR_NAME = "Ubuntu"
FEDORA_DISTR_NAME = "Fedora"
OL_DISTR_NAME = "Oracle Linux Server"

# Packages are the same for Fedora 34 and OL 8.
OL_PACKAGES = FEDORA_PACKAGES

class _LinuxDistribution:
    def __init__(self, name, version):
        self.name = name
        self.version = version
        self.major_int_version = 0
        if name == UBUNTU_DISTR_NAME:
            self.major_int_version = int(version.split(".")[0])
        elif name == FEDORA_DISTR_NAME:
            self.major_int_version = int(version)
        elif name == OL_DISTR_NAME:
            self.major_int_version = int(version)

    def __repr__(self):
        return self.name + " " + self.version

# Minimal version of supported distributions
MIN_SUPPORTED_DISTROS = [
    _LinuxDistribution("Ubuntu", "18.04"),
    _LinuxDistribution("Fedora", "34"),
    _LinuxDistribution("Oracle Linux Server", "8"),
]

def print_info(distr):
    msg = "Current distro is %s" % str(distr) + "\n"
    msg += "  Supported distros are: %s" % ", ".join([str(supported_distro) for supported_distro in MIN_SUPPORTED_DISTROS]) + "\n"
    msg += "  On Debian-based systems, packages would be installed with `" + " ".join(UBUNTU_INSTALL_COMMAND) + "`\n"
    msg += "  On RedHat-based systems, packages would be installed with `" + " ".join(FEDORA_INSTALL_COMMAND) + "`\n"
    mx.log(msg)

def _is_distr_supported(linux_distr):
    for supported_distro in MIN_SUPPORTED_DISTROS:
        supported_distr_name = supported_distro.name
        min_supported_distr_version = supported_distro.major_int_version
        if linux_distr.name == supported_distr_name and linux_distr.major_int_version >= min_supported_distr_version:
            return True
    return False

def _install_distr_dependencies(distr, cmds):
    mx.log("Running: " + " ".join(cmds))
    ret = subprocess.call(cmds)
    if ret != 0:
        mx.abort("Installation of dependencies on %s failed" % distr)

def _get_distribution():
    if not os.path.exists("/etc/os-release"):
        mx.abort("Unknown Linux distribution - /etc/os-release does not exist")
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


def install_dependencies(args):
    """Installs all the dependencies for FastR and GNU-R, so that FastR can be built from sources.

    Currently, only Ubuntu, Fedora and Oracle Linux Server are supported. There are no plans to
    support MacOS.
    """
    parser = ArgumentParser(prog='mx r-install-deps', description=install_dependencies.__doc__, formatter_class=RawTextHelpFormatter)
    parser.add_argument('-i', '--info', action='store_true', default=False,
                        help="Only print info about what would be run for Debian-based and RedHat-based systems, do not run anything")
    parsed_args = parser.parse_args(args)

    distr = _get_distribution()
    if parsed_args.info:
        print_info(distr)
        return

    if not _is_distr_supported(distr):
        mx.abort("Unsupported Linux distribution: %s, run again with `--info`\n" % distr)

    if distr.name == UBUNTU_DISTR_NAME:
        _install_distr_dependencies(distr, UBUNTU_INSTALL_COMMAND)
    elif distr.name == FEDORA_DISTR_NAME:
        _install_distr_dependencies(distr, FEDORA_INSTALL_COMMAND)
    elif distr.name == OL_DISTR_NAME:
        _install_distr_dependencies(distr, OL_INSTALL_COMMAND)
