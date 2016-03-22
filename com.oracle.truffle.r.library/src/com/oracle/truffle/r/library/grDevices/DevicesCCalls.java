/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2014, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.grDevices;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.grDevices.pdf.PdfGraphicsDevice;
import com.oracle.truffle.r.library.graphics.core.GraphicsEngineImpl;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;

public class DevicesCCalls {
    public static final class C_DevOff extends RExternalBuiltinNode {

        @Override
        @TruffleBoundary
        public Object call(RArgsValuesAndNames args) {
            Object firstArgument = args.getArgument(0);
            int deviceIndex = castInt(castVector(firstArgument));
            GraphicsEngineImpl.getInstance().killGraphicsDeviceByIndex(deviceIndex);
            return RNull.instance;
        }
    }

    public static final class C_DevCur extends RExternalBuiltinNode {

        @Override
        @TruffleBoundary
        public Object call(RArgsValuesAndNames args) {
            return GraphicsEngineImpl.getInstance().getCurrentGraphicsDeviceIndex();
        }
    }

    public static final class C_PDF extends RExternalBuiltinNode {

        @SuppressWarnings("unused")
        @Override
        @TruffleBoundary
        public Object call(RArgsValuesAndNames args) {
            new PdfGraphicsDevice(extractParametersFrom(args));
            // todo implement devices addition
            return RNull.instance;
        }

        private PdfGraphicsDevice.Parameters extractParametersFrom(RArgsValuesAndNames args) {
            PdfGraphicsDevice.Parameters result = new PdfGraphicsDevice.Parameters();
            result.filePath = isString(args.getArgument(0));
            result.paperSize = isString(args.getArgument(1));
            result.fontFamily = isString(args.getArgument(2));
            result.encoding = isString(args.getArgument(3));
            result.bg = isString(args.getArgument(4));
            result.fg = isString(args.getArgument(5));
            result.width = castDouble(castVector(args.getArgument(6))).getDataAt(0);
            result.height = castDouble(castVector(args.getArgument(7))).getDataAt(0);
            result.pointSize = castDouble(castVector(args.getArgument(8))).getDataAt(0);
            result.oneFile = castLogical(castVector(args.getArgument(9)));
            result.pageCenter = castLogical(castVector(args.getArgument(10)));
            result.title = isString(args.getArgument(11));
            result.fonts = extractFontsFrom(args.getArgument(12));

            result.majorVersion = castInt(castVector(args.getArgument(13)));
            result.minorVersion = castInt(castVector(args.getArgument(14)));
            result.colormodel = isString(args.getArgument(15));
            result.useDingbats = castLogical(castVector(args.getArgument(16)));
            result.useKerning = castLogical(castVector(args.getArgument(17)));
            result.fillOddEven = castLogical(castVector(args.getArgument(18)));
            result.compress = castLogical(castVector(args.getArgument(19)));
            return result;
        }

        @SuppressWarnings("static-method")
        private String[] extractFontsFrom(Object inputArgument) {
            return inputArgument == RNull.instance ? new String[0] : ((RStringVector) inputArgument).getDataCopy();
        }
    }
}
