/*
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import java.io.OutputStream;
import sun.net.www.protocol.ftp.FtpURLConnection;

/**
 * Support for the "internal"method of "utils::download.file". TODO take note of "quiet", "mode" and
 * "cacheOK".
 */
public abstract class Download extends RExternalBuiltinNode.Arg6 {

    private static final int BUFFER_SIZE = 8192;

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
    protected int download(String urlString, String destFile, boolean quiet, @SuppressWarnings("unused") String mode, @SuppressWarnings("unused") boolean cacheOK,
                    @SuppressWarnings("unused") Object headers,
                    @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
        try {
            String urlStr = urlString;
            URLConnection con;
            while (true) {
                con = new URL(urlStr).openConnection();
                if (con instanceof HttpURLConnection) {
                    HttpURLConnection httpCon = (HttpURLConnection) con;
                    httpCon.setInstanceFollowRedirects(false);
                    httpCon.connect();
                    int response = httpCon.getResponseCode();
                    if (response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_MOVED_TEMP) {
                        urlStr = httpCon.getHeaderField("Location");
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            try (InputStream in = con.getInputStream()) {

                TruffleFile dest = ctxRef.get().getSafeTruffleFile(destFile);
                OutputStream os = dest.newOutputStream();
                long len = 0L;
                byte[] buf = new byte[BUFFER_SIZE];
                int n;
                while ((n = in.read(buf)) > 0) {
                    os.write(buf, 0, n);
                    len += n;
                }
                if (!quiet) {

                    String contentType = null;
                    if (con instanceof HttpURLConnection) {
                        HttpURLConnection httpCon = (HttpURLConnection) con;
                        contentType = httpCon.getContentType();
                    }

                    // Transcribed from GnuR, src/modules/internet/internet.c
                    if (contentType != null) {
                        StdConnections.getStderr().writeString(String.format("Content type '%s'", contentType), false);
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
                    } else if (con instanceof FtpURLConnection) {
                        if (len >= 0) {
                            StdConnections.getStderr().writeString(String.format(" ftp data connection made, file length %d bytes", len), true);
                        } else {
                            StdConnections.getStderr().writeString(String.format(" ftp data connection made, file length unknown", len), true);
                        }
                    }

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
    @RBuiltin(name = "curlDownload", visibility = OFF, kind = INTERNAL, parameterNames = {"url", "destfile", "quite", "mode", "cacheOK", "headers"}, behavior = IO)
    public abstract static class CurlDownload extends RBuiltinNode.Arg6 {

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
        protected int download(String urlString, String destFile, boolean quiet, String mode, boolean cacheOK, Object headers,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
            return downloadBuiltin.download(urlString, destFile, quiet, mode, cacheOK, headers, ctxRef);
        }

    }
}
