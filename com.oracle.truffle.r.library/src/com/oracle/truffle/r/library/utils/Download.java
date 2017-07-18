/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.utils;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.nodes.builtin.RBuiltinBaseNode;

/**
 * Support for the "internal"method of "utils::download.file". TODO take note of "quiet", "mode" and
 * "cacheOK".
 */
public abstract class Download extends RExternalBuiltinNode.Arg5 {

    static {
        Casts casts = new Casts(Download.class);
        casts.arg(0).mustBe(stringValue()).asStringVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst();
        casts.arg(1).mustBe(stringValue()).asStringVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst();
        casts.arg(2).mustBe(logicalValue()).asLogicalVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst().map(toBoolean());
        casts.arg(3).mustBe(stringValue()).asStringVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst();
        casts.arg(4).mustBe(logicalValue()).asLogicalVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst().map(toBoolean());

    }

    @Specialization
    @TruffleBoundary
    protected int download(String urlString, String destFile, boolean quiet, @SuppressWarnings("unused") String mode, @SuppressWarnings("unused") boolean cacheOK) {
        try {
            URLConnection con = new URL(urlString).openConnection();
            try (InputStream in = con.getInputStream()) {
                long len = Files.copy(in, Paths.get(destFile), StandardCopyOption.REPLACE_EXISTING);
                if (!quiet) {

                    String contentType = null;
                    if (con instanceof HttpURLConnection) {
                        HttpURLConnection httpCon = (HttpURLConnection) con;
                        contentType = httpCon.getContentType();
                    }

                    // Transcribed from GnuR, src/modules/internet/internet.c

                    StdConnections.getStderr().writeString(String.format("Content type '%s'", contentType != null ? contentType : "unknown"), false);
                    if (len > 1024 * 1024) {
                        StdConnections.getStderr().writeString(String.format(" length %d bytes (%.1f MB)", len, len / 1024.0 / 1024.0), true);
                    } else if (len > 10240) {
                        StdConnections.getStderr().writeString(String.format(" length %d bytes (%d KB)", len, len / 1024), true);
                    } else if (len >= 0) {
                        StdConnections.getStderr().writeString(String.format(" length %d bytes", len), true);
                    } else {
                        StdConnections.getStderr().writeString(" length unknown", true);
                    }
                    StdConnections.getStderr().flush();
                }

                return 0;
            } catch (IOException e) {
                if (!quiet) {

                    // Transcribed from GnuR, src/modules/internet/internet.c

                    int responseCode = -1;
                    String responseMsg = null;
                    if (con instanceof HttpURLConnection) {
                        HttpURLConnection httpCon = (HttpURLConnection) con;
                        responseCode = httpCon.getResponseCode();
                        responseMsg = httpCon.getResponseMessage();
                    }

                    warning(RError.Message.GENERIC, String.format("cannot open URL '%s': HTTP status was '%d %s'", urlString, responseCode, responseMsg != null ? responseMsg : ""));
                }
                throw e;
            }
        } catch (IOException e) {
            throw error(RError.Message.GENERIC, e.getMessage());
        }
    }

    /**
     * This builtin is a tentative implementation of the <code>curlDownload</code> internal builtin.
     * It just delegates invocations to the {@link Download} builtin.
     */
    @RBuiltin(name = "curlDownload", visibility = OFF, kind = INTERNAL, parameterNames = {"url", "destfile", "quite", "mode", "cacheOK"}, behavior = IO)
    public abstract static class CurlDownload extends RBuiltinNode.Arg5 {

        static {
            Casts casts = new Casts(CurlDownload.class);
            casts.arg(0).mustBe(stringValue()).asStringVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst();
            casts.arg(1).mustBe(stringValue()).asStringVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst();
            casts.arg(2).mustBe(logicalValue()).asLogicalVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst().map(toBoolean());
            casts.arg(3).mustBe(stringValue()).asStringVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst();
            casts.arg(4).mustBe(logicalValue()).asLogicalVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst().map(toBoolean());

        }

        @Child private Download downloadBuiltin = DownloadNodeGen.create();

        @Specialization
        protected int download(String urlString, String destFile, boolean quiet, String mode, boolean cacheOK) {
            return downloadBuiltin.download(urlString, destFile, quiet, mode, cacheOK);
        }

    }
}
