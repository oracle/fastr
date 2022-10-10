/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * This file contains wrappers for calling functions from PCRE2 library.
 * For readability, the file first lists the declarations of the functions and then their implementation.
 * We use libpcre2-8, which is a version of the library that uses 8-bit character width (uint8_t).
 * The pcre2 header uses some defines for its types, we rather use standard types and explicitly enumerate
 * the type mappings in the following table:
 * PCRE2_SIZE ... size_t
 * PCRE2_SPTR ... uint8_t *
 * PCRE2_UCHAR ... uint8_t
 */

#include <rffiutils.h>

// This define ensures that uint8_t is chosen as the basic character width for PCRE2.
#define PCRE2_CODE_UNIT_WIDTH 8
#include <pcre2.h>

// Define for printing important PCRE2-specific function calls.
//#define FASTR_PCRE2_DEBUG

// Typedefs for callbacks from native into Java.
typedef void (*match_cb_t)(size_t start_idx, size_t end_idx);
typedef void (*capture_cb_t)(size_t capture_idx, size_t start_idx, size_t end_idx);
typedef void (*set_capture_name_cb_t)(const char *name, int index);

pcre2_code *call_pcre2_compile(uint8_t *pattern, size_t pattern_len, uint32_t options, int *error_code, size_t *error_offset);
uint32_t call_pcre2_capture_count(pcre2_code *re);
/**
 * Returns the count of all the named captures. A named capture is also a capture, so the number
 * returned by this function is always lower than the number returned by `call_pcre2_capture_count`.
 */
uint32_t call_pcre2_names_count(pcre2_code *re);
int call_pcre2_get_capture_names(set_capture_name_cb_t set_capture_name_cb, pcre2_code *re);

/**
 * The implementation is heavily inspired by the demo application provided on the official
 * PCRE2 website and by the implementation of`do_regexpr` in GNU-R.
 *
 * @param match_cb A function (callback) that is called when a match occured.
 * @param capture_cb A function that is called once a match for some capture occured.
 *     If there are no captures in the pattern, this callback is never called.
 * @param re A pointer to the compiled pattern - as returned by `call_pcre2_compile`.
 *     The caller is responsible for freeing the compiled pattern via `call_pcre2_data_free`.
 * @param subject_str A subject - text that is matched against the compiled pattern.
 * @param subject_len Length of the subject.
 * @param options An option bitset. See pcre2.h.
 * @param stop_after_first_match If 1, only first match is done.
 *
 * @returns Number of matches, or PCRE2 error code (negative integer) on error. To convert
 *    the error code into string, call `call_pcre2_errcode_to_string`.
 */
int call_pcre2_match(
    match_cb_t match_cb,
    capture_cb_t capture_cb,
    pcre2_code *re,
    uint8_t *subject,
    size_t subject_len,
    uint32_t options,
    int stop_after_first_match
);
void call_pcre2_pattern_free(pcre2_code *compiled_pattern);
void call_pcre2_errcode_to_string(int errcode, uint8_t *buff, size_t buff_len);

// Helper functions
static int is_valid_index(size_t index);
static int is_utf8_continuation_byte(uint8_t byte);
static void report_captures(capture_cb_t capture_cb, uint32_t capture_count, const size_t *ovector);
static size_t advance_offset(size_t offset, int utf8, const uint8_t *subject, size_t subject_len);


pcre2_code *call_pcre2_compile(uint8_t *pattern, size_t pattern_len, uint32_t options, int *error_code, size_t *error_offset)
{
#ifdef FASTR_PCRE2_DEBUG
    printf("call_pcre2_compile: pattern_str='%s', pattern_len=%lu, pattern=[", pattern, pattern_len);
    for (int i = 0; i < pattern_len; i++) {
        printf("%u, ", pattern[i]);
    }
    printf("]\n");
#endif
    pcre2_code *compiled_pattern = pcre2_compile(pattern, pattern_len, options, error_code, (size_t *)error_offset, NULL);
    return compiled_pattern;
}

uint32_t call_pcre2_capture_count(pcre2_code *re)
{
    uint32_t capture_count = 0;
    int ret = pcre2_pattern_info(re, PCRE2_INFO_CAPTURECOUNT, &capture_count);
    if (ret != 0) {
        printf("Fatal error: pcre2_pattern_info retcode = %d\n", ret);
        exit(1);
    }
    return capture_count;
}

uint32_t call_pcre2_names_count(pcre2_code *re)
{
    uint32_t name_count = 0;
    int ret = pcre2_pattern_info(re, PCRE2_INFO_NAMECOUNT, &name_count);
    if (ret != 0) {
        printf("Fatal error: pcre2_pattern_info retcode = %d\n", ret);
        exit(1);
    }
    return name_count;
}

int call_pcre2_get_capture_names(void (*set_capture_name_cb)(const char *name, int index), pcre2_code *re)
{
    uint32_t names_count = call_pcre2_names_count(re);
    if (names_count <= 0) {
        return names_count;
    }

    uint8_t *name_table = NULL;
    uint32_t name_entry_size = 0;

    pcre2_pattern_info(re, PCRE2_INFO_NAMETABLE, &name_table);
    pcre2_pattern_info(re, PCRE2_INFO_NAMEENTRYSIZE, &name_entry_size);

    uint8_t *tabptr = name_table;
    for (uint32_t i = 0; i < names_count; i++) {
        int ovector_idx = (tabptr[0] << 8) | tabptr[1];
        // ovector begins with match, capture groups are after that.
        int capture_idx = ovector_idx - 1;
        uint8_t *name = tabptr + 2;
        set_capture_name_cb((const char *)ensure_string((const char *) name), capture_idx);
        tabptr += name_entry_size;
    }
    return names_count;
}

int call_pcre2_match(
    match_cb_t match_cb,
    capture_cb_t capture_cb,
    pcre2_code *re,
    uint8_t *subject,
    size_t subject_len,
    uint32_t first_match_options,
    int stop_after_first_match
)
{
    pcre2_match_data *match_data = pcre2_match_data_create_from_pattern(re, NULL);
    uint32_t capture_count = call_pcre2_capture_count(re);

    uint32_t pattern_option_bits = 0;
    (void)pcre2_pattern_info(re, PCRE2_INFO_ALLOPTIONS, &pattern_option_bits);
    int utf8 = (pattern_option_bits & PCRE2_UTF) != 0;
#ifdef FASTR_PCRE2_DEBUG
    printf("call_pcre2_match: utf8 option = %d\n", utf8);
#endif

    int match_count = 0;
    // We use the default match context, and subject_offset is 0.
    // rc corresponds to the count of captured groups plus one, or error code if rc is negative.
    int rc = pcre2_match(re, subject, subject_len, 0, first_match_options, match_data, NULL);
    if (rc == PCRE2_ERROR_NOMATCH) {
        pcre2_match_data_free(match_data);
        return 0;
    } else if (rc < 0) {
        pcre2_match_data_free(match_data);
        return rc;
    } else {
        match_count++;
    }

    // An array of (start_index, end_index) pairs.
    // The size of the array correponds to the number of captured pairs plus one.
    // The first pair represents the whole match, all the other pairs represent
    // matches of captured groups.
    // Note that the array is modified in `pcre2_match` call.
    size_t *ovector = pcre2_get_ovector_pointer(match_data);

    if (ovector[0] > ovector[1]) {
#ifdef FASTR_PCRE2_DEBUG
        printf("call_pcre2_match: \\K special case\n");
#endif
        // \K special case
        pcre2_match_data_free(match_data);
        return -1;
    }

#ifdef FASTR_PCRE2_DEBUG
    printf("call_pcre2_match: match_cb(%lu, %lu)\n", ovector[0], ovector[1]);
#endif
    match_cb(ovector[0], ovector[1]);
    report_captures(capture_cb, capture_count, ovector);

    if (stop_after_first_match) {
        // The case for match_count == 0 was already processed.
        if (match_count != 1) {
            fatalError("pcre2_rffi.c: match_count != 1");
        }
        pcre2_match_data_free(match_data);
        return match_count;
    }

    // Find the rest of all the matches
    while (1) {
        uint32_t options = 0;
        // start_offset is an index into subject from where to start next match.
        size_t start_offset = ovector[1];
        if (start_offset > subject_len) {
#ifdef FASTR_PCRE2_DEBUG
            printf("start_offset > subject_len\n");
#endif
            pcre2_match_data_free(match_data);
            return match_count;
        }

        // Check if the previous match was for an empty string.
        if (ovector[0] == ovector[1]) {
            if (ovector[0] == subject_len) {
                // We are at the end of the subject.
                pcre2_match_data_free(match_data);
                return match_count;
            } else {
                // We set the options here so that we prevent an infinite recursion.
                options = PCRE2_NOTEMPTY_ATSTART | PCRE2_ANCHORED;
            }
        } else {
            // Previous match was not an empty string.
            size_t prev_match_start_idx = pcre2_get_startchar(match_data);
            // Handle \K special case
            if (start_offset <= prev_match_start_idx) {
                // We have to increase start_offset
#ifdef FASTR_PCRE2_DEBUG
                printf("!! start_offset <= prev_match_start_idx\n");
#endif
                if (subject_len <= prev_match_start_idx) {
                    // Reached end of subject.
                    pcre2_match_data_free(match_data);
                    return match_count;
                } else {
                    start_offset = advance_offset(prev_match_start_idx, utf8, subject, subject_len);
                }
            }
        }

#ifdef FASTR_PCRE2_DEBUG
        printf("call_pcre2_match: Calling pcre2_match(start_offset=%lu, options=%u)\n", start_offset, options);
#endif
        // This time, we call `pcre2_match` with a specific offset into the subject.
        rc = pcre2_match(re, subject, subject_len, start_offset, options, match_data, NULL);

        // This time, NO_MATCH is not an error.
        if (rc == PCRE2_ERROR_NOMATCH) {
            if (options == 0) {
                return match_count;
            } else {
                // Options != 0 means that the previous match was for an empty string.
                ovector[1] = advance_offset(start_offset, utf8, subject, subject_len);
            }
            continue;
        } else if (rc == PCRE2_ERROR_BADUTFOFFSET) {
            // We provided pcre2_match function with bad offset into an UTF-8 character.
            pcre2_match_data_free(match_data);
            fatalError("pcre2_rffi.c: BADUTFOFFSET - should not happen");
            return -1;
        } else if (rc < 0) {
            // This error is not recoverable
            pcre2_match_data_free(match_data);
            return rc;
        } else {
            match_count++;
        }

        if (ovector[0] > ovector[1]) {
            printf("Error: Special case with \\K (see pcre2demo)");
            pcre2_match_data_free(match_data);
            return -1;
        }

        match_cb(ovector[0], ovector[1]);
        report_captures(capture_cb, capture_count, ovector);
    }
    pcre2_match_data_free(match_data);
    return match_count;
}

void call_pcre2_pattern_free(pcre2_code *compiled_pattern)
{
    pcre2_code_free(compiled_pattern);
}

void call_pcre2_errcode_to_string(int errcode, uint8_t *buff, size_t buff_len)
{
    int rc = pcre2_get_error_message(errcode, buff, buff_len);
    if (rc < 0) {
        printf("Fatal error: pcre2_get_error_message returned %d\n", rc);
        exit(1);
    }
}

/**
 * In some corner cases, like nested captures, PCRE2 returns invalid indexes.
 * It is unnecessary to report these invalid indexes to Java.
 */
static int is_valid_index(size_t index)
{
    return index != ((size_t) -1);
}

/**
 * Returns true if the given byte is a continuation byte of some Unicode string, i.e. if it is
 * not a start of some Unicode character.
 */
static int is_utf8_continuation_byte(uint8_t byte)
{
    return (byte & 0xc0) == 0x80;
}

static void report_captures(capture_cb_t capture_cb, uint32_t capture_count, const size_t *ovector)
{
    for (size_t i = 0; i < capture_count; i++) {
        size_t ovector_idx = i + 1;
        int capture_idx = (int) i;
        if (capture_count <= 0) {
            fatalError("capture_count <= 0");
        }
        size_t capt_start_idx = ovector[2 * ovector_idx];
        size_t capt_end_idx = ovector[2 * ovector_idx + 1];
        // We want to report only "valid" indexes to Java.
        if (is_valid_index(capt_start_idx) && is_valid_index(capt_end_idx)) {
            capture_cb(capture_idx, capt_start_idx, capt_end_idx);
        }
    }
}

static size_t advance_offset(size_t offset, int utf8, const uint8_t *subject, size_t subject_len)
{
    size_t next_offset = offset + 1;
    if (utf8) {
        while (is_utf8_continuation_byte(subject[next_offset]) && next_offset < subject_len) {
            next_offset++;
        }
    }
    return next_offset;
}

