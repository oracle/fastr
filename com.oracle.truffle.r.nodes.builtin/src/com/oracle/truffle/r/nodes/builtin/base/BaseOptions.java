/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public class BaseOptions implements ROptions.Handler {
    public enum Name {
        AddSmooth("add.smooth"),
        BrowserNLdisabled("browserNLdisabled"),
        CheckPackageLicense("checkPackageLicense"),
        CheckBounds("check.bounds"),
        CBoundsCheck("CBoundsCheck"),
        Continue("continue"),
        DefaultPackages("defaultPackages"),
        DeparseCutoff("deparse.cutoff"),
        DeparseMaxLines("deparse.max.lines"),
        Digits("digits"),
        DigitsSecs("digits.secs"),
        DownloadFileExtra("download.file.extra"),
        DownloadFileMethod("download.file.method"),
        Echo("echo"),
        Encoding("encoding"),
        Error("error"),
        Expressions("expressions"),
        KeepSource("keep.source"),
        KeepSourcePkgs("keep.source.pkgs"),
        MaxPrint("max.print"),
        OutDec("OutDec"),
        Pager("pager"),
        Papersize("papersize"),
        Pdfviewer("pdfviewer"),
        Printcmd("printcmd"),
        Prompt("prompt"),
        Rl_word_breaks("rl_word_breaks"),
        SaveDefaults("save.defaults"),
        SaveImageDefaults("save.image.defaults"),
        Scipen("scipen"),
        ShowWarnCalls("showWarnCalls"),
        ShowErrorCalls("showErrorCalls"),
        ShowNCalls("showNCalls"),
        ShowErrorLocations("show.error.locations"),
        ShowErrorMessages("show.error.messages"),
        StringsAsFactors("stringsAsFactors"),
        Texi2dvi("texi2dvi"),
        Timeout("timeout"),
        TopLevelEnvironment("topLevelEnvironment"),
        UseFancyQuotes("useFancyQuotes"),
        Verbose("verbose"),
        Warn("warn"),
        WarnPartialMatchArgs("warnPartialMatchArgs"),
        WarnPartialMatchAttr("warnPartialMatchAttr"),
        WarnPartialMatchDollar("warnPartialMatchDollar"),
        WarningExpression("warning.expression"),
        WarningLength("warning.length"),
        Nwarnings("nwarnings"),
        Width("width");

        private final String rName;

        private Name(String name) {
            this.rName = name;
        }

        public String getName() {
            return rName;
        }
    }

    public BaseOptions() {
        ROptions.registerHandler(this);
    }

    public void initialize() {
        // The "factory fresh" settings
        ROptions.setValue(Name.AddSmooth.rName, RDataFactory.createLogicalVectorFromScalar(true));
        ROptions.setValue(Name.CheckBounds.rName, RDataFactory.createLogicalVectorFromScalar(false));
        ROptions.setValue(Name.Continue.rName, RDataFactory.createStringVector("+ "));
        ROptions.setValue(Name.Digits.rName, RDataFactory.createIntVectorFromScalar(7));
        ROptions.setValue(Name.Echo.rName, RDataFactory.createLogicalVectorFromScalar(true));
        ROptions.setValue(Name.Encoding.rName, RDataFactory.createStringVector("native.enc"));
        ROptions.setValue(Name.Error.rName, RNull.instance);
        ROptions.setValue(Name.Expressions.rName, RDataFactory.createIntVectorFromScalar(5000));
        ROptions.setValue(Name.KeepSourcePkgs.rName, RDataFactory.createLogicalVectorFromScalar(false));
        ROptions.setValue(Name.MaxPrint.rName, RDataFactory.createIntVectorFromScalar(99999));
        ROptions.setValue(Name.OutDec.rName, RDataFactory.createStringVector("."));
        ROptions.setValue(Name.Prompt.rName, RDataFactory.createStringVector("> "));
        ROptions.setValue(Name.Scipen.rName, RDataFactory.createIntVector(0));
        ROptions.setValue(Name.ShowErrorMessages.rName, RDataFactory.createLogicalVectorFromScalar(true));
        ROptions.setValue(Name.Timeout.rName, RDataFactory.createIntVectorFromScalar(60));
        ROptions.setValue(Name.Verbose.rName, RDataFactory.createLogicalVectorFromScalar(false));
        ROptions.setValue(Name.Warn.rName, RDataFactory.createIntVectorFromScalar(0));
        ROptions.setValue(Name.WarningLength.rName, RDataFactory.createIntVectorFromScalar(1000));
        ROptions.setValue(Name.Width.rName, RDataFactory.createIntVectorFromScalar(80));

        // others
        ROptions.setValue(Name.BrowserNLdisabled.rName, RDataFactory.createLogicalVectorFromScalar(false));
    }

    public void addOptions() {
        for (Name name : Name.values()) {
            ROptions.addOption(name.rName, null);
        }
    }
}
