/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * the type mappings in the following paragraph:
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

pcre2_code *call_pcre2_compile(char *pattern, uint32_t options, int *error_code, int *error_offset);
uint32_t call_pcre2_capture_count(pcre2_code *re);
/**
 * Returns the count of all the named captures. A named capture is also a capture, so the number
 * returned by this function is always lower than the number returned by `call_pcre2_capture_count`.
 */
uint32_t call_pcre2_names_count(pcre2_code *re);
int call_pcre2_get_capture_names(void (*set_capture_name_cb)(const char *name, int index), pcre2_code *re);

/**
 * The implementation is heavily inspired by the demo application provided on the official
 * PCRE2 website and by the implementation of`do_regexpr` in GNU-R.
 *
 * The implementation does the first match, and then continues iff `stop_after_first_match`
 * is 1. The code that analyses the match data from PCRE2 library (two for loops) seems duplicated,
 * but this is how the pcre2demo is structured.
 *
 * The given callbacks are called (reported) in appropriate time. Note that some of the capture
 * may be reported twice - once without a name and once with name. Generally, there may be
 * reported duplicate captures. A capture is uniquelly identified by its start index and end index.
 * Every named capture is also a capture, so the number of named captures is always lower than
 * the number of all the captures.
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
 * @returns Number of matches, or -1 on an error.
 */
int call_pcre2_match(
    void (*match_cb)(size_t start_idx, size_t end_idx),
    void (*capture_cb)(size_t capture_idx, size_t start_idx, size_t end_idx),
    pcre2_code *re,
    uint8_t *subject,
    uint32_t options,
    int stop_after_first_match
);

void call_pcre2_pattern_free(pcre2_code *compiled_pattern);


// TODO: error_offset should be `size_t`.
pcre2_code *call_pcre2_compile(char *pattern_str, uint32_t options, int *error_code, int *error_offset)
{
    uint8_t *pattern = (uint8_t *) pattern_str;
    size_t pattern_len = strlen(pattern);
#ifdef FASTR_PCRE2_DEBUG
    printf("call_pcre2_compile: pattern_str='%s', pattern_len=%u, pattern=[", pattern, pattern_len);
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

/**
 * In some corner cases, like nested captures, PCRE2 returns invalid indexes.
 * It is unnecessary to report these invalid indexes to Java.
 */
static int is_valid_index(size_t index)
{
    return index != ((size_t) -1);
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
        set_capture_name_cb((const char *)name, capture_idx);
        tabptr += name_entry_size;
    }
    return names_count;
}

int call_pcre2_match(
    void (*match_cb)(size_t start_idx, size_t end_idx),
    void (*capture_cb)(size_t capture_idx, size_t start_idx, size_t end_idx),
    pcre2_code *re,
    uint8_t *subject,
    uint32_t options,
    int stop_after_first_match
)
{
    size_t subject_len = strlen((char *)subject);
    //uint8_t *subject = (uint8_t *)subject_str;

#ifdef FASTR_PCRE2_DEBUG
    printf("call_pcre2_match: subject (len=%u) = [", subject_len);
    for (size_t i = 0; i < subject_len; i++) {
        printf("%d, ", subject[i]);
    }
    printf("]\n");
#endif

    pcre2_match_data *match_data = pcre2_match_data_create_from_pattern(re, NULL);

    // Check for named captures
    uint32_t capture_count = call_pcre2_capture_count(re);
    uint32_t names_count = call_pcre2_names_count(re);
    // Named group implies capture group.
    if (!(capture_count >= names_count)) {
        fatalError("[pcre2_rffi]: Unexpected Capture_count < names_count");
    }
    uint8_t *name_table = NULL;
    uint32_t name_entry_size = 0;

    if (names_count > 0) {
        pcre2_pattern_info(re, PCRE2_INFO_NAMETABLE, &name_table);
        pcre2_pattern_info(re, PCRE2_INFO_NAMEENTRYSIZE, &name_entry_size);
    }

    int match_count = 0;
    // We use the default match context, and subject_offset is 0.
    // rc corresponds to the count of captured groups plus one, or error code if rc is negative.
    int rc = pcre2_match(re, subject, subject_len, 0, options, match_data, NULL);
    if (rc == PCRE2_ERROR_NOMATCH) {
        return 0;
    } else if (rc < 0) {
        // TODO: fatalError("pcre2_match rc < 0");
        return -1;
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
        printf("Match Error: \\K special case\n");
        return -1;
    }

#ifdef FASTR_PCRE2_DEBUG
    printf("call_pcre2_match: match_cb(%u, %u)\n", ovector[0], ovector[1]);
#endif
    match_cb(ovector[0], ovector[1]);

    // The rest of the ovector is for the capture groups only.
    for (size_t i = 0; i < capture_count; i++) {
        size_t ovector_idx = i + 1;
        int capture_idx = (int) i;
        if (capture_count <= 0) {
            fatalError("[pcre2_rffi.c]: capture_count <= 0");
        }
        size_t capt_start_idx = ovector[2 * ovector_idx];
        size_t capt_end_idx = ovector[2 * ovector_idx + 1];
        // We want to report only "valid" indexes to Java.
        if (is_valid_index(capt_start_idx) && is_valid_index(capt_end_idx)) {
#ifdef FASTR_PCRE2_DEBUG
            printf("call_pcre2_match: capture_cb(%d, %u, %u)\n", capture_idx, capt_start_idx, capt_end_idx);
#endif
            capture_cb(capture_idx, capt_start_idx, capt_end_idx);
        }
    }

    if (stop_after_first_match) {
        // The case for match_count == 0 was already processed.
        if (match_count != 1) {
            fatalError("[pcre2_rffi.c]: match_count != 1");
        }
        return match_count;
    }

    // Find the rest of all the matches
    while (1) {
        // start_offset is an index into subject from where to start next match.
        size_t start_offset = ovector[1];
        uint32_t options = 0;

        // Check if the previous match was for an empty string.
        if (ovector[0] == ovector[1]) {
            if (ovector[0] == subject_len) {
                // We are at the end of the subject.
                return match_count;
            } else {
                // We set the options here so that we prevent an infinite recursion.
                options = PCRE2_NOTEMPTY_ATSTART | PCRE2_ANCHORED;
            }
        } else {
            // Previous match was not an empty string.
            size_t prev_match_start_idx = pcre2_get_startchar(match_data);
            if (start_offset <= prev_match_start_idx) {
                if (prev_match_start_idx >= subject_len) {
                    return match_count;
                } else {
                    // Advance by one character.
                    start_offset = prev_match_start_idx + 1;
                }
            }
        }

        // This time, we call `pcre2_match` with a specific offset into the subject.
        rc = pcre2_match(re, subject, subject_len, start_offset, options, match_data, NULL);

        // This time, NO_MATCH is not an error.
        if (rc == PCRE2_ERROR_NOMATCH) {
            if (options == 0) {
                return match_count;
            } else {
                // Advance one code unit.
                ovector[1] = start_offset + 1;
            }
            continue;
        } else if (rc < 0) {
            // This error is not recoverable
            printf("Matching error %d\n", rc);
            pcre2_match_data_free(match_data);
            return -1;
        } else {
            match_count++;
        }

        if (ovector[0] > ovector[1]) {
            printf("Error: Special case with \\K (see pcre2demo)");
            pcre2_match_data_free(match_data);
            return -1;
        }

#ifdef FASTR_PCRE2_DEBUG
    printf("call_pcre2_match: match_cb(%u, %u)\n", ovector[0], ovector[1]);
#endif
        match_cb(ovector[0], ovector[1]);

        for (size_t i = 0; i < capture_count; i++) {
            size_t ovector_idx = i + 1;
            int capture_idx = (int) i;
            if (capture_count <= 0) {
                fatalError("[pcre2_rffi.c]: capture_count <= 0");
            }
            size_t capt_start_idx = ovector[2 * ovector_idx];
            size_t capt_end_idx = ovector[2 * ovector_idx + 1];
            if (is_valid_index(capt_start_idx) && is_valid_index(capt_end_idx)) {
#ifdef FASTR_PCRE2_DEBUG
                printf("call_pcre2_match: capture_cb(%d, %u, %u)\n", capture_idx, capt_start_idx, capt_end_idx);
#endif
                capture_cb(capture_idx, capt_start_idx, capt_end_idx);
            }
        }
    }
    return match_count;
}

void call_pcre2_pattern_free(pcre2_code *compiled_pattern)
{
    pcre2_code_free(compiled_pattern);
}
