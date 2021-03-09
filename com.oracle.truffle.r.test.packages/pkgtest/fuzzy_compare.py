#
# Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import logging

def fuzzy_compare(gnur_content, fastr_content, gnur_filename, fastr_filename, custom_filters=None, dump_preprocessed=False):
    """
    Compares the test output of GnuR and FastR by ignoring implementation-specific differences like header, error,
    and warning messages.
    It returns a 3-tuple (<status>, <statements passed>, <statements failed>), where status=0 if files are equal,
    status=1 if the files are different, status=-1 if the files could not be compared. In case of status=1,
    statements passed and statements failed give the numbers on how many statements produced the same or a different
    output, respectively.
    """
    logging.debug("Using custom filters:\n" + str(custom_filters))
    gnur_content = _preprocess_content(gnur_content, custom_filters)
    fastr_content = _preprocess_content(fastr_content, custom_filters)
    if dump_preprocessed:
        with open(gnur_filename + '.preprocessed', 'w') as f:
            f.writelines(gnur_content)
        with open(fastr_filename + '.preprocessed', 'w') as f:
            f.writelines(fastr_content)
    gnur_start = _find_start(gnur_content)
    gnur_end = _find_end(gnur_content)
    fastr_start = _find_start(fastr_content)
    fastr_len = len(fastr_content)
    if not gnur_start or not gnur_end or not fastr_start:
        if not gnur_start:
            logging.info("malformed start of the GnuR output")
        if not gnur_end:
            logging.info("malformed end of the GnuR output")
        if not fastr_start:
            logging.info("malformed start of the FastR output")
        return -1, 0, 0
    gnur_i = gnur_start
    fastr_i = fastr_start
    # the overall result for comparing the file
    overall_result = 0
    # the local result, i.e., for the current statement
    result = 0
    statements_passed = set()
    statements_failed = set()

    # the first line must start with the prompt, so capture it
    gnur_prompt = _capture_prompt(gnur_content, gnur_i)
    fastr_prompt = _capture_prompt(fastr_content, fastr_i)

    gnur_line, gnur_i = _get_next_line(gnur_prompt, gnur_content, gnur_end, gnur_i)
    fastr_line, fastr_i = _get_next_line(fastr_prompt, fastr_content, fastr_len, fastr_i)
    gnur_cur_statement_start = gnur_i
    fastr_cur_statement_start = fastr_i

    while True:
        if gnur_line is None or fastr_line is None:
            # fail if FastR's output is shorter than GnuR's
            if gnur_line is not None and fastr_line is None:
                logging.info("FastR's output is shorter than GnuR's")
                overall_result = 1
            break

        # flag indicating that we want to synchronize
        sync = False
        if gnur_line != fastr_line:
            if fastr_line.startswith('Warning') and 'FastR does not support graphics package' in fastr_content[
                fastr_i + 1]:
                # ignore warning about FastR not supporting the graphics package
                fastr_i = fastr_i + 2
                if fastr_content[fastr_i].startswith('NULL') and not gnur_line.startswith('NULL'):
                    # ignore additional visible NULL
                    fastr_i = fastr_i + 1
                sync = True
            elif gnur_line.startswith('Warning') and gnur_i + 1 < gnur_end and 'closing unused connection' in \
                    gnur_content[gnur_i + 1]:
                # ignore message about closed connection
                gnur_i = gnur_i + 2
                sync = True
            elif gnur_i > 0 and gnur_content[gnur_i - 1].startswith('   user  system elapsed'):
                # ignore differences in timing
                gnur_i = gnur_i + 1
                fastr_i = fastr_i + 1
                sync = True
            # we are fuzzy on Error/Warning as FastR often differs
            # in the context/format of the error/warning message AND GnuR is sometimes
            # inconsistent over which error message it uses. Unlike the unit test environment,
            # we cannot tag tests in any way, so we simply check that FastR does report
            # an error. We then scan forward to try to get the files back in sync, as the
            # the number of error/warning lines may differ.
            elif 'Error' in gnur_line or 'Warning' in gnur_line:
                to_match = 'Error' if 'Error' in gnur_line else 'Warning'
                if to_match not in fastr_line:
                    result = 1
                else:
                    # accept differences in the error/warning messages but we need to synchronize
                    gnur_i = gnur_i + 1
                    fastr_i = fastr_i + 1
                    sync = True
            elif _is_ignored_function("sessionInfo", gnur_content, gnur_cur_statement_start, fastr_content,
                                      fastr_cur_statement_start):
                # ignore differences in 'sessionInfo' output
                gnur_i = gnur_i + 1
                fastr_i = fastr_i + 1
                sync = True
            elif _is_ignored_function("extSoftVersion", gnur_content, gnur_cur_statement_start, fastr_content,
                                      fastr_cur_statement_start):
                # ignore differences in 'extSoftVersion' output
                gnur_i = gnur_i + 1
                fastr_i = fastr_i + 1
                sync = True
            else:
                # genuine difference (modulo whitespace)
                if not _ignore_whitespace(gnur_line, fastr_line):
                    result = 1

        # report a mismatch or success
        if result == 1:
            logging.info(gnur_filename + ':%d' % (gnur_cur_statement_start + 1) + ' vs. ' + fastr_filename + ':%d' % (
                    fastr_cur_statement_start + 1))
            logging.info("%s\nvs.\n%s" % (gnur_line.strip(), fastr_line.strip()))

            # we need to synchronize the indices such that we can continue
            gnur_i = gnur_i + 1
            fastr_i = fastr_i + 1
            sync = True
            # report the last statement to produce different output
            assert fastr_cur_statement_start != -1
            if fastr_cur_statement_start in statements_passed:
                statements_passed.remove(fastr_cur_statement_start)
            statements_failed.add(fastr_cur_statement_start)

            # set overall result and reset temporary result
            overall_result = 1
            result = 0
        else:
            assert result == 0
            if fastr_cur_statement_start not in statements_failed:
                statements_passed.add(fastr_cur_statement_start)

        # synchronize: skip until lines match (or file end reached)
        if sync:
            if gnur_i == gnur_end - 1:
                # at end (there is always a blank line)
                break
            ni = -1
            # find next statement line (i.e. starting with a prompt)

            while gnur_i < gnur_end:
                if _is_statement_begin(gnur_prompt, gnur_content[gnur_i]):
                    ni = _find_line(gnur_content[gnur_i], fastr_content, fastr_i)
                    if ni > 0:
                        break
                gnur_i = gnur_i + 1
            if ni > 0:
                fastr_i = ni
        else:
            # just advance by one line in FastR and GnuR
            gnur_i = gnur_i + 1
            fastr_i = fastr_i + 1

        gnur_line, gnur_i = _get_next_line(gnur_prompt, gnur_content, gnur_end, gnur_i)
        fastr_line, fastr_i = _get_next_line(fastr_prompt, fastr_content, fastr_len, fastr_i)

        # check if the current line starts a statement
        if _is_statement_begin(gnur_prompt, gnur_line) and gnur_cur_statement_start != gnur_i:
            gnur_cur_statement_start = gnur_i

        # if we find a new statement begin
        if _is_statement_begin(fastr_prompt, fastr_line) and fastr_cur_statement_start != fastr_i:
            fastr_cur_statement_start = fastr_i

    return overall_result, len(statements_passed), len(statements_failed)


def _get_next_line(prompt, content, content_len, line_idx):
    i = line_idx
    while i < content_len:
        line = content[i]
        if prompt is not None:
            line = line.replace(prompt, "", 1)
        line = line.strip()
        if line != "":
            return line, i
        i = i + 1
    return None, i


def _ignore_whitespace(gnur_line, fastr_line):
    translate_table = {ord(' '): None, ord('\t'): None}
    return gnur_line.translate(translate_table) == fastr_line.translate(translate_table)


def _capture_prompt(lines, idx):
    # The prompt can be anything, so it is hard to determine it in general.
    # We will therefore just consider the default prompt.
    default_prompt = "> "
    if idx < len(lines) and lines[idx].startswith(default_prompt):
        return default_prompt
    return None

def _find_start(content):
    marker = "Type 'q()' to quit R."
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            # skip blank lines
            j = i + 1
            while j < len(content):
                line = content[j].strip()
                if len(line) > 0:
                    return j
                j = j + 1
    return None


def _find_end(content):
    marker = "Time elapsed:"
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            return i
    # not all files have a Time elapsed:
    return len(content)


def _find_line(gnur_line, fastr_content, fastr_i):
    '''
    Search forward in fastr_content from fastr_i searching for a match with gnur_line.
    Do not match empty lines!
    '''
    if gnur_line == '\n':
        return -1
    while fastr_i < len(fastr_content):
        fastr_line = fastr_content[fastr_i]
        if fastr_line == gnur_line:
            return fastr_i
        fastr_i = fastr_i + 1
    return -1

def _is_ignored_function(fun_name, gnur_content, gnur_stmt, fastr_content, fastr_stmt):
    return gnur_stmt != -1 and fun_name in gnur_content[gnur_stmt] and fastr_stmt != -1 and fun_name in fastr_content[
        fastr_stmt]




def _is_statement_begin(captured_prompt, line):
    if captured_prompt is None:
        return False
    if line is not None:
        line_wo_prompt = line.replace(captured_prompt, "").strip()
        return line.startswith(captured_prompt) and line_wo_prompt != "" and not line_wo_prompt.startswith("#")
    return False


def _preprocess_content(output, custom_filters):
    # load file with replacement actions
    if custom_filters:
        for f in custom_filters:
            output = f.apply(output)
    else:
        # default builtin-filters
        for idx, val in enumerate(output):
            if "RUNIT TEST PROTOCOL -- " in val:
                # RUnit prints the current date and time
                output[idx] = "RUNIT TEST PROTOCOL -- <date_time>"
            else:
                # ignore differences which come from test directory paths
                output[idx] = val.replace('fastr', '<engine>').replace('gnur', '<engine>')
    return output

