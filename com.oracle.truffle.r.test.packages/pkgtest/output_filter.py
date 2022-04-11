#
# Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import re
import logging


class ContentFilter:
    scope = "global"
    pkg_pattern = "*"
    action = "d"
    args = []
    remove_before = 0
    remove_after = 0

    def __init__(self, pkg_pattern, action, args, remove_before=0, remove_after=0):
        self.pkg_pattern = pkg_pattern
        self.pkg_prog = re.compile(pkg_pattern)
        self.action = action
        self.args = args
        self.remove_before = remove_before
        self.remove_after = remove_after

    def _apply_to_lines(self, content, action):
        if action is not None:
            idx = 0
            while idx < len(content):
                val = content[idx]
                content[idx] = action(val)
                # check if the line was removed and apply remove_before/after
                if self.action == 'D' and val and not content[idx]:
                    remove_count = self.remove_before + self.remove_after + 1
                    content[idx - self.remove_before:idx + self.remove_after + 1] = [""] * remove_count
                    idx += self.remove_after
                idx += 1
        return content

    def apply(self, content):
        filter_action = None
        if self.action == "r":
            filter_action = lambda l: l.replace(self.args[0], self.args[1])
        elif self.action == "d":
            filter_action = lambda l: l.replace(self.args[0], "")
        elif self.action == "R":
            filter_action = lambda l: self.args[1] if self.args[0] in l else l
        elif self.action == "D":
            filter_action = lambda l: "" if self.args[0] in l else l
        elif self.action == "s":
            class SubstituteAction:
                def __init__(self, pattern, repl):
                    self.compiled_regex = re.compile(pattern)
                    self.repl = repl

                def __call__(self, l):
                    return re.sub(self.compiled_regex, self.repl, l)

            filter_action = SubstituteAction(self.args[0], self.args[1])
        return self._apply_to_lines(content, filter_action)

    def applies_to_pkg(self, pkg_name):
        if pkg_name is None:
            return True
        else:
            return self.pkg_prog.match(pkg_name)

    def __repr__(self):
        fmt_str = "{!s} => {!s}"
        fmt_args = [self.pkg_pattern, self.action]
        for arg in self.args:
            fmt_str = fmt_str + "/{!s}"
            fmt_args.append(arg)
        return fmt_str.format(*tuple(fmt_args))


class InvalidFilterException(Exception):
    pass


def load_filter_file(file_path):
    from os.path import isfile
    filters = []
    if isfile(file_path):
        with open(file_path) as f:
            for linenr, line in enumerate(f.readlines()):
                # ignore comment lines
                if not line.startswith("#") and line.strip() != "":
                    try:
                        filters.append(_parse_filter(line))
                    except InvalidFilterException as e:
                        logging.info("invalid filter at line {!s}: {!s}".format(linenr, e))
    return filters


def _select_filters(filters, pkg):
    pkg_filters = []
    for f in filters:
        if f.applies_to_pkg(pkg):
            pkg_filters.append(f)
    return pkg_filters


def _parse_filter(line):
    arrow_idx = line.find("=>")
    if arrow_idx < 0:
        raise InvalidFilterException("cannot find separator '=>'")
    pkg_pattern = line[:arrow_idx].strip()
    action_str = line[arrow_idx + 2:].strip()
    action = action_str[0]
    args = []
    remove_before = 0
    remove_after = 0
    if action == "d" or action == "D":
        # actions with one argument and possibly numbers that indicate lines to remove after/before
        slash_idx = action_str.find("/")
        skip_before_match = re.search(r'\-(\d+)', action_str)
        skip_after_match = re.search(r'\+(\d+)', action_str)
        remove_before = 0 if not skip_before_match else int(skip_before_match.group(1))
        remove_after = 0 if not skip_after_match else int(skip_after_match.group(1))
        if slash_idx < 0:
            raise InvalidFilterException("cannot find separator '/'")
        args.append(action_str[slash_idx + 1:])
    elif action == "r" or action == "R" or action == "s":
        # actions with two arguments
        slash0_idx = action_str.find("/")
        slash1_idx = action_str.find("/", slash0_idx + 1)
        while slash1_idx > 0 and action_str[slash1_idx - 1] == '\\':
            slash1_idx = action_str.find("/", slash1_idx + 1)
        if slash0_idx < 0:
            raise InvalidFilterException("cannot find first separator '/'")
        if slash1_idx < 0:
            raise InvalidFilterException("cannot find second separator '/'")
        args.append(action_str[slash0_idx + 1:slash1_idx])
        args.append(action_str[slash1_idx + 1:])
    else:
        raise InvalidFilterException("invalid action '" + action_str + "'")
    return ContentFilter(pkg_pattern, action, args, remove_before, remove_after)


def select_filters_for_package(filter_file, pkg):
    return _select_filters(load_filter_file(filter_file), pkg)
