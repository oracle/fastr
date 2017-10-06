/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1997-2014, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.oracle.truffle.r.library.fastrGrid.GridState.GridPalette;
import com.oracle.truffle.r.library.fastrGrid.device.GridColor;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class GridColorUtils {

    // Note: default palette copied from GNU R
    private static final GridPalette defaultPalette = new GridPalette(new String[]{"black", "red", "green3", "blue", "cyan", "magenta", "yellow", "grey"});

    private GridColorUtils() {
        // only static members
    }

    /**
     * Converts given object into {@link GridColor}. The object may be a vector, in which case the
     * index modulo its size is used to select element of that vector.
     */
    public static GridColor getColor(Object value, int index) {
        GridColor color = GridColorUtils.getPaletteColor(value, index);
        if (color != null) {
            return color;
        }

        String strValue = null;
        if (value instanceof String) {
            strValue = (String) value;
        } else if (value instanceof RAbstractStringVector && ((RAbstractStringVector) value).getLength() > 0) {
            strValue = ((RAbstractStringVector) value).getDataAt(index % ((RAbstractStringVector) value).getLength());
        } else {
            return GridColor.TRANSPARENT;
        }

        return gridColorFromString(strValue);
    }

    public static GridPalette getDefaultPalette() {
        return defaultPalette;
    }

    /**
     * Converts the representation of color used within R, e.g. as value for
     * {@code gpar(col='value')}, to our internal representation that grid device should understand.
     * The acceptable color formats are: name of known color, HTML style hex value, and HTML style
     * hex value including alpha.
     */
    public static GridColor gridColorFromString(String value) {
        if (value.startsWith("#") && (value.length() == 7 || value.length() == 9)) {
            return parseHex(value);
        }

        if (value.equals("NA")) {
            // special case value, we want to check only for "NA", not "na".
            return GridColor.TRANSPARENT;
        }

        GridColor result = findByName(value);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Invalid color '" + value + "'.");
        }
        return result;
    }

    public static String gridColorToRString(GridColor color) {
        int size = color.getAlpha() == GridColor.OPAQUE_ALPHA ? 7 : 9;
        char[] value = new char[size];
        value[0] = '#';
        value[1] = getHexDigit(color.getRed() >> 4);
        value[2] = getHexDigit(color.getRed());
        value[3] = getHexDigit(color.getGreen() >> 4);
        value[4] = getHexDigit(color.getGreen());
        value[5] = getHexDigit(color.getBlue() >> 4);
        value[6] = getHexDigit(color.getBlue());
        if (color.getAlpha() == GridColor.OPAQUE_ALPHA) {
            value[7] = getHexDigit((color.getAlpha()) >> 4);
            value[8] = getHexDigit(color.getAlpha());
        }
        return new String(value);
    }

    public static GridColor parseHex(String value) {
        // hex format, e.g. #ffffff
        int red = Integer.parseInt(value.substring(1, 3), 16);
        int green = Integer.parseInt(value.substring(3, 5), 16);
        int blue = Integer.parseInt(value.substring(5, 7), 16);
        int alpha = GridColor.OPAQUE_ALPHA;
        if (value.length() == 9) {
            alpha = Integer.parseInt(value.substring(7, 9), 16);
        }
        return new GridColor(red, green, blue, alpha);
    }

    private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static char getHexDigit(int digit) {
        return HEX_DIGITS[digit & 0xf];
    }

    private static GridColor findByName(String synonym) {
        return (GridColor) NamesHolder.NAMES.get(normalizeColorName(synonym));
    }

    // GnuR compares the user given color name to the dictionary ignoring spaces and case. We remove
    // the spaces and make it lowercase and then use normal string comparison.
    private static String normalizeColorName(String synonym) {
        boolean isNormalized = true;
        for (int i = 0; i < synonym.length(); i++) {
            char c = synonym.charAt(i);
            isNormalized &= (!Character.isAlphabetic(c) || Character.isLowerCase(c)) && c != ' ';
        }
        return isNormalized ? synonym : synonym.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private static GridColor getPaletteColor(Object colorIdIn, int index) {
        Object colorId = colorIdIn;
        if (colorId instanceof RAbstractVector) {
            RAbstractVector vec = (RAbstractVector) colorId;
            colorId = vec.getDataAtAsObject(index % vec.getLength());
        }
        int paletteIdx = RRuntime.INT_NA;
        if (colorId instanceof Integer) {
            paletteIdx = (int) colorId;
        } else if (colorId instanceof Double && !RRuntime.isNA((Double) colorId)) {
            paletteIdx = (int) (double) colorId;
        } else if (colorId instanceof String && !RRuntime.isNA((String) colorId)) {
            paletteIdx = paletteIdxFromString((String) colorId);
        } else if (colorId instanceof Byte && !RRuntime.isNA((byte) colorId)) {
            paletteIdx = (int) (byte) colorId;
        }
        if (RRuntime.isNA(paletteIdx)) {
            return null;
        }
        if (paletteIdx < 0) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, Utils.stringFormat("numerical color values must be >= 0, found %d", paletteIdx));
        }
        if (paletteIdx == 0) {
            return GridColor.TRANSPARENT;
        }
        GridPalette palette = GridContext.getContext().getGridState().getPalette();
        GridColor result = palette.colors[(paletteIdx - 1) % palette.colors.length];
        return result; // one based index
    }

    private static int paletteIdxFromString(String colorId) {
        return RRuntime.parseIntWithNA(colorId);
    }

    private static final class NamesHolder {
        private static final HashMap<String, Object> NAMES = new HashMap<>(700);

        static {
            NAMES.put("transparent", GridColor.TRANSPARENT);
            NAMES.put("white", "#FFFFFF");
            NAMES.put("aliceblue", "#F0F8FF");
            NAMES.put("antiquewhite", "#FAEBD7");
            NAMES.put("antiquewhite1", "#FFEFDB");
            NAMES.put("antiquewhite2", "#EEDFCC");
            NAMES.put("antiquewhite3", "#CDC0B0");
            NAMES.put("antiquewhite4", "#8B8378");
            NAMES.put("aquamarine", "#7FFFD4");
            NAMES.put("aquamarine1", "#7FFFD4");
            NAMES.put("aquamarine2", "#76EEC6");
            NAMES.put("aquamarine3", "#66CDAA");
            NAMES.put("aquamarine4", "#458B74");
            NAMES.put("azure", "#F0FFFF");
            NAMES.put("azure1", "#F0FFFF");
            NAMES.put("azure2", "#E0EEEE");
            NAMES.put("azure3", "#C1CDCD");
            NAMES.put("azure4", "#838B8B");
            NAMES.put("beige", "#F5F5DC");
            NAMES.put("bisque", "#FFE4C4");
            NAMES.put("bisque1", "#FFE4C4");
            NAMES.put("bisque2", "#EED5B7");
            NAMES.put("bisque3", "#CDB79E");
            NAMES.put("bisque4", "#8B7D6B");
            NAMES.put("black", "#000000");
            NAMES.put("blanchedalmond", "#FFEBCD");
            NAMES.put("blue", "#0000FF");
            NAMES.put("blue1", "#0000FF");
            NAMES.put("blue2", "#0000EE");
            NAMES.put("blue3", "#0000CD");
            NAMES.put("blue4", "#00008B");
            NAMES.put("blueviolet", "#8A2BE2");
            NAMES.put("brown", "#A52A2A");
            NAMES.put("brown1", "#FF4040");
            NAMES.put("brown2", "#EE3B3B");
            NAMES.put("brown3", "#CD3333");
            NAMES.put("brown4", "#8B2323");
            NAMES.put("burlywood", "#DEB887");
            NAMES.put("burlywood1", "#FFD39B");
            NAMES.put("burlywood2", "#EEC591");
            NAMES.put("burlywood3", "#CDAA7D");
            NAMES.put("burlywood4", "#8B7355");
            NAMES.put("cadetblue", "#5F9EA0");
            NAMES.put("cadetblue1", "#98F5FF");
            NAMES.put("cadetblue2", "#8EE5EE");
            NAMES.put("cadetblue3", "#7AC5CD");
            NAMES.put("cadetblue4", "#53868B");
            NAMES.put("chartreuse", "#7FFF00");
            NAMES.put("chartreuse1", "#7FFF00");
            NAMES.put("chartreuse2", "#76EE00");
            NAMES.put("chartreuse3", "#66CD00");
            NAMES.put("chartreuse4", "#458B00");
            NAMES.put("chocolate", "#D2691E");
            NAMES.put("chocolate1", "#FF7F24");
            NAMES.put("chocolate2", "#EE7621");
            NAMES.put("chocolate3", "#CD661D");
            NAMES.put("chocolate4", "#8B4513");
            NAMES.put("coral", "#FF7F50");
            NAMES.put("coral1", "#FF7256");
            NAMES.put("coral2", "#EE6A50");
            NAMES.put("coral3", "#CD5B45");
            NAMES.put("coral4", "#8B3E2F");
            NAMES.put("cornflowerblue", "#6495ED");
            NAMES.put("cornsilk", "#FFF8DC");
            NAMES.put("cornsilk1", "#FFF8DC");
            NAMES.put("cornsilk2", "#EEE8CD");
            NAMES.put("cornsilk3", "#CDC8B1");
            NAMES.put("cornsilk4", "#8B8878");
            NAMES.put("cyan", "#00FFFF");
            NAMES.put("cyan1", "#00FFFF");
            NAMES.put("cyan2", "#00EEEE");
            NAMES.put("cyan3", "#00CDCD");
            NAMES.put("cyan4", "#008B8B");
            NAMES.put("darkblue", "#00008B");
            NAMES.put("darkcyan", "#008B8B");
            NAMES.put("darkgoldenrod", "#B8860B");
            NAMES.put("darkgoldenrod1", "#FFB90F");
            NAMES.put("darkgoldenrod2", "#EEAD0E");
            NAMES.put("darkgoldenrod3", "#CD950C");
            NAMES.put("darkgoldenrod4", "#8B6508");
            NAMES.put("darkgray", "#A9A9A9");
            NAMES.put("darkgreen", "#006400");
            NAMES.put("darkgrey", "#A9A9A9");
            NAMES.put("darkkhaki", "#BDB76B");
            NAMES.put("darkmagenta", "#8B008B");
            NAMES.put("darkolivegreen", "#556B2F");
            NAMES.put("darkolivegreen1", "#CAFF70");
            NAMES.put("darkolivegreen2", "#BCEE68");
            NAMES.put("darkolivegreen3", "#A2CD5A");
            NAMES.put("darkolivegreen4", "#6E8B3D");
            NAMES.put("darkorange", "#FF8C00");
            NAMES.put("darkorange1", "#FF7F00");
            NAMES.put("darkorange2", "#EE7600");
            NAMES.put("darkorange3", "#CD6600");
            NAMES.put("darkorange4", "#8B4500");
            NAMES.put("darkorchid", "#9932CC");
            NAMES.put("darkorchid1", "#BF3EFF");
            NAMES.put("darkorchid2", "#B23AEE");
            NAMES.put("darkorchid3", "#9A32CD");
            NAMES.put("darkorchid4", "#68228B");
            NAMES.put("darkred", "#8B0000");
            NAMES.put("darksalmon", "#E9967A");
            NAMES.put("darkseagreen", "#8FBC8F");
            NAMES.put("darkseagreen1", "#C1FFC1");
            NAMES.put("darkseagreen2", "#B4EEB4");
            NAMES.put("darkseagreen3", "#9BCD9B");
            NAMES.put("darkseagreen4", "#698B69");
            NAMES.put("darkslateblue", "#483D8B");
            NAMES.put("darkslategray", "#2F4F4F");
            NAMES.put("darkslategray1", "#97FFFF");
            NAMES.put("darkslategray2", "#8DEEEE");
            NAMES.put("darkslategray3", "#79CDCD");
            NAMES.put("darkslategray4", "#528B8B");
            NAMES.put("darkslategrey", "#2F4F4F");
            NAMES.put("darkturquoise", "#00CED1");
            NAMES.put("darkviolet", "#9400D3");
            NAMES.put("deeppink", "#FF1493");
            NAMES.put("deeppink1", "#FF1493");
            NAMES.put("deeppink2", "#EE1289");
            NAMES.put("deeppink3", "#CD1076");
            NAMES.put("deeppink4", "#8B0A50");
            NAMES.put("deepskyblue", "#00BFFF");
            NAMES.put("deepskyblue1", "#00BFFF");
            NAMES.put("deepskyblue2", "#00B2EE");
            NAMES.put("deepskyblue3", "#009ACD");
            NAMES.put("deepskyblue4", "#00688B");
            NAMES.put("dimgray", "#696969");
            NAMES.put("dimgrey", "#696969");
            NAMES.put("dodgerblue", "#1E90FF");
            NAMES.put("dodgerblue1", "#1E90FF");
            NAMES.put("dodgerblue2", "#1C86EE");
            NAMES.put("dodgerblue3", "#1874CD");
            NAMES.put("dodgerblue4", "#104E8B");
            NAMES.put("firebrick", "#B22222");
            NAMES.put("firebrick1", "#FF3030");
            NAMES.put("firebrick2", "#EE2C2C");
            NAMES.put("firebrick3", "#CD2626");
            NAMES.put("firebrick4", "#8B1A1A");
            NAMES.put("floralwhite", "#FFFAF0");
            NAMES.put("forestgreen", "#228B22");
            NAMES.put("gainsboro", "#DCDCDC");
            NAMES.put("ghostwhite", "#F8F8FF");
            NAMES.put("gold", "#FFD700");
            NAMES.put("gold1", "#FFD700");
            NAMES.put("gold2", "#EEC900");
            NAMES.put("gold3", "#CDAD00");
            NAMES.put("gold4", "#8B7500");
            NAMES.put("goldenrod", "#DAA520");
            NAMES.put("goldenrod1", "#FFC125");
            NAMES.put("goldenrod2", "#EEB422");
            NAMES.put("goldenrod3", "#CD9B1D");
            NAMES.put("goldenrod4", "#8B6914");
            NAMES.put("gray", "#BEBEBE");
            NAMES.put("gray0", "#000000");
            NAMES.put("gray1", "#030303");
            NAMES.put("gray2", "#050505");
            NAMES.put("gray3", "#080808");
            NAMES.put("gray4", "#0A0A0A");
            NAMES.put("gray5", "#0D0D0D");
            NAMES.put("gray6", "#0F0F0F");
            NAMES.put("gray7", "#121212");
            NAMES.put("gray8", "#141414");
            NAMES.put("gray9", "#171717");
            NAMES.put("gray10", "#1A1A1A");
            NAMES.put("gray11", "#1C1C1C");
            NAMES.put("gray12", "#1F1F1F");
            NAMES.put("gray13", "#212121");
            NAMES.put("gray14", "#242424");
            NAMES.put("gray15", "#262626");
            NAMES.put("gray16", "#292929");
            NAMES.put("gray17", "#2B2B2B");
            NAMES.put("gray18", "#2E2E2E");
            NAMES.put("gray19", "#303030");
            NAMES.put("gray20", "#333333");
            NAMES.put("gray21", "#363636");
            NAMES.put("gray22", "#383838");
            NAMES.put("gray23", "#3B3B3B");
            NAMES.put("gray24", "#3D3D3D");
            NAMES.put("gray25", "#404040");
            NAMES.put("gray26", "#424242");
            NAMES.put("gray27", "#454545");
            NAMES.put("gray28", "#474747");
            NAMES.put("gray29", "#4A4A4A");
            NAMES.put("gray30", "#4D4D4D");
            NAMES.put("gray31", "#4F4F4F");
            NAMES.put("gray32", "#525252");
            NAMES.put("gray33", "#545454");
            NAMES.put("gray34", "#575757");
            NAMES.put("gray35", "#595959");
            NAMES.put("gray36", "#5C5C5C");
            NAMES.put("gray37", "#5E5E5E");
            NAMES.put("gray38", "#616161");
            NAMES.put("gray39", "#636363");
            NAMES.put("gray40", "#666666");
            NAMES.put("gray41", "#696969");
            NAMES.put("gray42", "#6B6B6B");
            NAMES.put("gray43", "#6E6E6E");
            NAMES.put("gray44", "#707070");
            NAMES.put("gray45", "#737373");
            NAMES.put("gray46", "#757575");
            NAMES.put("gray47", "#787878");
            NAMES.put("gray48", "#7A7A7A");
            NAMES.put("gray49", "#7D7D7D");
            NAMES.put("gray50", "#7F7F7F");
            NAMES.put("gray51", "#828282");
            NAMES.put("gray52", "#858585");
            NAMES.put("gray53", "#878787");
            NAMES.put("gray54", "#8A8A8A");
            NAMES.put("gray55", "#8C8C8C");
            NAMES.put("gray56", "#8F8F8F");
            NAMES.put("gray57", "#919191");
            NAMES.put("gray58", "#949494");
            NAMES.put("gray59", "#969696");
            NAMES.put("gray60", "#999999");
            NAMES.put("gray61", "#9C9C9C");
            NAMES.put("gray62", "#9E9E9E");
            NAMES.put("gray63", "#A1A1A1");
            NAMES.put("gray64", "#A3A3A3");
            NAMES.put("gray65", "#A6A6A6");
            NAMES.put("gray66", "#A8A8A8");
            NAMES.put("gray67", "#ABABAB");
            NAMES.put("gray68", "#ADADAD");
            NAMES.put("gray69", "#B0B0B0");
            NAMES.put("gray70", "#B3B3B3");
            NAMES.put("gray71", "#B5B5B5");
            NAMES.put("gray72", "#B8B8B8");
            NAMES.put("gray73", "#BABABA");
            NAMES.put("gray74", "#BDBDBD");
            NAMES.put("gray75", "#BFBFBF");
            NAMES.put("gray76", "#C2C2C2");
            NAMES.put("gray77", "#C4C4C4");
            NAMES.put("gray78", "#C7C7C7");
            NAMES.put("gray79", "#C9C9C9");
            NAMES.put("gray80", "#CCCCCC");
            NAMES.put("gray81", "#CFCFCF");
            NAMES.put("gray82", "#D1D1D1");
            NAMES.put("gray83", "#D4D4D4");
            NAMES.put("gray84", "#D6D6D6");
            NAMES.put("gray85", "#D9D9D9");
            NAMES.put("gray86", "#DBDBDB");
            NAMES.put("gray87", "#DEDEDE");
            NAMES.put("gray88", "#E0E0E0");
            NAMES.put("gray89", "#E3E3E3");
            NAMES.put("gray90", "#E5E5E5");
            NAMES.put("gray91", "#E8E8E8");
            NAMES.put("gray92", "#EBEBEB");
            NAMES.put("gray93", "#EDEDED");
            NAMES.put("gray94", "#F0F0F0");
            NAMES.put("gray95", "#F2F2F2");
            NAMES.put("gray96", "#F5F5F5");
            NAMES.put("gray97", "#F7F7F7");
            NAMES.put("gray98", "#FAFAFA");
            NAMES.put("gray99", "#FCFCFC");
            NAMES.put("gray100", "#FFFFFF");
            NAMES.put("green", "#00FF00");
            NAMES.put("green1", "#00FF00");
            NAMES.put("green2", "#00EE00");
            NAMES.put("green3", "#00CD00");
            NAMES.put("green4", "#008B00");
            NAMES.put("greenyellow", "#ADFF2F");
            NAMES.put("grey", "#BEBEBE");
            NAMES.put("grey0", "#000000");
            NAMES.put("grey1", "#030303");
            NAMES.put("grey2", "#050505");
            NAMES.put("grey3", "#080808");
            NAMES.put("grey4", "#0A0A0A");
            NAMES.put("grey5", "#0D0D0D");
            NAMES.put("grey6", "#0F0F0F");
            NAMES.put("grey7", "#121212");
            NAMES.put("grey8", "#141414");
            NAMES.put("grey9", "#171717");
            NAMES.put("grey10", "#1A1A1A");
            NAMES.put("grey11", "#1C1C1C");
            NAMES.put("grey12", "#1F1F1F");
            NAMES.put("grey13", "#212121");
            NAMES.put("grey14", "#242424");
            NAMES.put("grey15", "#262626");
            NAMES.put("grey16", "#292929");
            NAMES.put("grey17", "#2B2B2B");
            NAMES.put("grey18", "#2E2E2E");
            NAMES.put("grey19", "#303030");
            NAMES.put("grey20", "#333333");
            NAMES.put("grey21", "#363636");
            NAMES.put("grey22", "#383838");
            NAMES.put("grey23", "#3B3B3B");
            NAMES.put("grey24", "#3D3D3D");
            NAMES.put("grey25", "#404040");
            NAMES.put("grey26", "#424242");
            NAMES.put("grey27", "#454545");
            NAMES.put("grey28", "#474747");
            NAMES.put("grey29", "#4A4A4A");
            NAMES.put("grey30", "#4D4D4D");
            NAMES.put("grey31", "#4F4F4F");
            NAMES.put("grey32", "#525252");
            NAMES.put("grey33", "#545454");
            NAMES.put("grey34", "#575757");
            NAMES.put("grey35", "#595959");
            NAMES.put("grey36", "#5C5C5C");
            NAMES.put("grey37", "#5E5E5E");
            NAMES.put("grey38", "#616161");
            NAMES.put("grey39", "#636363");
            NAMES.put("grey40", "#666666");
            NAMES.put("grey41", "#696969");
            NAMES.put("grey42", "#6B6B6B");
            NAMES.put("grey43", "#6E6E6E");
            NAMES.put("grey44", "#707070");
            NAMES.put("grey45", "#737373");
            NAMES.put("grey46", "#757575");
            NAMES.put("grey47", "#787878");
            NAMES.put("grey48", "#7A7A7A");
            NAMES.put("grey49", "#7D7D7D");
            NAMES.put("grey50", "#7F7F7F");
            NAMES.put("grey51", "#828282");
            NAMES.put("grey52", "#858585");
            NAMES.put("grey53", "#878787");
            NAMES.put("grey54", "#8A8A8A");
            NAMES.put("grey55", "#8C8C8C");
            NAMES.put("grey56", "#8F8F8F");
            NAMES.put("grey57", "#919191");
            NAMES.put("grey58", "#949494");
            NAMES.put("grey59", "#969696");
            NAMES.put("grey60", "#999999");
            NAMES.put("grey61", "#9C9C9C");
            NAMES.put("grey62", "#9E9E9E");
            NAMES.put("grey63", "#A1A1A1");
            NAMES.put("grey64", "#A3A3A3");
            NAMES.put("grey65", "#A6A6A6");
            NAMES.put("grey66", "#A8A8A8");
            NAMES.put("grey67", "#ABABAB");
            NAMES.put("grey68", "#ADADAD");
            NAMES.put("grey69", "#B0B0B0");
            NAMES.put("grey70", "#B3B3B3");
            NAMES.put("grey71", "#B5B5B5");
            NAMES.put("grey72", "#B8B8B8");
            NAMES.put("grey73", "#BABABA");
            NAMES.put("grey74", "#BDBDBD");
            NAMES.put("grey75", "#BFBFBF");
            NAMES.put("grey76", "#C2C2C2");
            NAMES.put("grey77", "#C4C4C4");
            NAMES.put("grey78", "#C7C7C7");
            NAMES.put("grey79", "#C9C9C9");
            NAMES.put("grey80", "#CCCCCC");
            NAMES.put("grey81", "#CFCFCF");
            NAMES.put("grey82", "#D1D1D1");
            NAMES.put("grey83", "#D4D4D4");
            NAMES.put("grey84", "#D6D6D6");
            NAMES.put("grey85", "#D9D9D9");
            NAMES.put("grey86", "#DBDBDB");
            NAMES.put("grey87", "#DEDEDE");
            NAMES.put("grey88", "#E0E0E0");
            NAMES.put("grey89", "#E3E3E3");
            NAMES.put("grey90", "#E5E5E5");
            NAMES.put("grey91", "#E8E8E8");
            NAMES.put("grey92", "#EBEBEB");
            NAMES.put("grey93", "#EDEDED");
            NAMES.put("grey94", "#F0F0F0");
            NAMES.put("grey95", "#F2F2F2");
            NAMES.put("grey96", "#F5F5F5");
            NAMES.put("grey97", "#F7F7F7");
            NAMES.put("grey98", "#FAFAFA");
            NAMES.put("grey99", "#FCFCFC");
            NAMES.put("grey100", "#FFFFFF");
            NAMES.put("honeydew", "#F0FFF0");
            NAMES.put("honeydew1", "#F0FFF0");
            NAMES.put("honeydew2", "#E0EEE0");
            NAMES.put("honeydew3", "#C1CDC1");
            NAMES.put("honeydew4", "#838B83");
            NAMES.put("hotpink", "#FF69B4");
            NAMES.put("hotpink1", "#FF6EB4");
            NAMES.put("hotpink2", "#EE6AA7");
            NAMES.put("hotpink3", "#CD6090");
            NAMES.put("hotpink4", "#8B3A62");
            NAMES.put("indianred", "#CD5C5C");
            NAMES.put("indianred1", "#FF6A6A");
            NAMES.put("indianred2", "#EE6363");
            NAMES.put("indianred3", "#CD5555");
            NAMES.put("indianred4", "#8B3A3A");
            NAMES.put("ivory", "#FFFFF0");
            NAMES.put("ivory1", "#FFFFF0");
            NAMES.put("ivory2", "#EEEEE0");
            NAMES.put("ivory3", "#CDCDC1");
            NAMES.put("ivory4", "#8B8B83");
            NAMES.put("khaki", "#F0E68C");
            NAMES.put("khaki1", "#FFF68F");
            NAMES.put("khaki2", "#EEE685");
            NAMES.put("khaki3", "#CDC673");
            NAMES.put("khaki4", "#8B864E");
            NAMES.put("lavender", "#E6E6FA");
            NAMES.put("lavenderblush", "#FFF0F5");
            NAMES.put("lavenderblush1", "#FFF0F5");
            NAMES.put("lavenderblush2", "#EEE0E5");
            NAMES.put("lavenderblush3", "#CDC1C5");
            NAMES.put("lavenderblush4", "#8B8386");
            NAMES.put("lawngreen", "#7CFC00");
            NAMES.put("lemonchiffon", "#FFFACD");
            NAMES.put("lemonchiffon1", "#FFFACD");
            NAMES.put("lemonchiffon2", "#EEE9BF");
            NAMES.put("lemonchiffon3", "#CDC9A5");
            NAMES.put("lemonchiffon4", "#8B8970");
            NAMES.put("lightblue", "#ADD8E6");
            NAMES.put("lightblue1", "#BFEFFF");
            NAMES.put("lightblue2", "#B2DFEE");
            NAMES.put("lightblue3", "#9AC0CD");
            NAMES.put("lightblue4", "#68838B");
            NAMES.put("lightcoral", "#F08080");
            NAMES.put("lightcyan", "#E0FFFF");
            NAMES.put("lightcyan1", "#E0FFFF");
            NAMES.put("lightcyan2", "#D1EEEE");
            NAMES.put("lightcyan3", "#B4CDCD");
            NAMES.put("lightcyan4", "#7A8B8B");
            NAMES.put("lightgoldenrod", "#EEDD82");
            NAMES.put("lightgoldenrod1", "#FFEC8B");
            NAMES.put("lightgoldenrod2", "#EEDC82");
            NAMES.put("lightgoldenrod3", "#CDBE70");
            NAMES.put("lightgoldenrod4", "#8B814C");
            NAMES.put("lightgoldenrodyellow", "#FAFAD2");
            NAMES.put("lightgray", "#D3D3D3");
            NAMES.put("lightgreen", "#90EE90");
            NAMES.put("lightgrey", "#D3D3D3");
            NAMES.put("lightpink", "#FFB6C1");
            NAMES.put("lightpink1", "#FFAEB9");
            NAMES.put("lightpink2", "#EEA2AD");
            NAMES.put("lightpink3", "#CD8C95");
            NAMES.put("lightpink4", "#8B5F65");
            NAMES.put("lightsalmon", "#FFA07A");
            NAMES.put("lightsalmon1", "#FFA07A");
            NAMES.put("lightsalmon2", "#EE9572");
            NAMES.put("lightsalmon3", "#CD8162");
            NAMES.put("lightsalmon4", "#8B5742");
            NAMES.put("lightseagreen", "#20B2AA");
            NAMES.put("lightskyblue", "#87CEFA");
            NAMES.put("lightskyblue1", "#B0E2FF");
            NAMES.put("lightskyblue2", "#A4D3EE");
            NAMES.put("lightskyblue3", "#8DB6CD");
            NAMES.put("lightskyblue4", "#607B8B");
            NAMES.put("lightslateblue", "#8470FF");
            NAMES.put("lightslategray", "#778899");
            NAMES.put("lightslategrey", "#778899");
            NAMES.put("lightsteelblue", "#B0C4DE");
            NAMES.put("lightsteelblue1", "#CAE1FF");
            NAMES.put("lightsteelblue2", "#BCD2EE");
            NAMES.put("lightsteelblue3", "#A2B5CD");
            NAMES.put("lightsteelblue4", "#6E7B8B");
            NAMES.put("lightyellow", "#FFFFE0");
            NAMES.put("lightyellow1", "#FFFFE0");
            NAMES.put("lightyellow2", "#EEEED1");
            NAMES.put("lightyellow3", "#CDCDB4");
            NAMES.put("lightyellow4", "#8B8B7A");
            NAMES.put("limegreen", "#32CD32");
            NAMES.put("linen", "#FAF0E6");
            NAMES.put("magenta", "#FF00FF");
            NAMES.put("magenta1", "#FF00FF");
            NAMES.put("magenta2", "#EE00EE");
            NAMES.put("magenta3", "#CD00CD");
            NAMES.put("magenta4", "#8B008B");
            NAMES.put("maroon", "#B03060");
            NAMES.put("maroon1", "#FF34B3");
            NAMES.put("maroon2", "#EE30A7");
            NAMES.put("maroon3", "#CD2990");
            NAMES.put("maroon4", "#8B1C62");
            NAMES.put("mediumaquamarine", "#66CDAA");
            NAMES.put("mediumblue", "#0000CD");
            NAMES.put("mediumorchid", "#BA55D3");
            NAMES.put("mediumorchid1", "#E066FF");
            NAMES.put("mediumorchid2", "#D15FEE");
            NAMES.put("mediumorchid3", "#B452CD");
            NAMES.put("mediumorchid4", "#7A378B");
            NAMES.put("mediumpurple", "#9370DB");
            NAMES.put("mediumpurple1", "#AB82FF");
            NAMES.put("mediumpurple2", "#9F79EE");
            NAMES.put("mediumpurple3", "#8968CD");
            NAMES.put("mediumpurple4", "#5D478B");
            NAMES.put("mediumseagreen", "#3CB371");
            NAMES.put("mediumslateblue", "#7B68EE");
            NAMES.put("mediumspringgreen", "#00FA9A");
            NAMES.put("mediumturquoise", "#48D1CC");
            NAMES.put("mediumvioletred", "#C71585");
            NAMES.put("midnightblue", "#191970");
            NAMES.put("mintcream", "#F5FFFA");
            NAMES.put("mistyrose", "#FFE4E1");
            NAMES.put("mistyrose1", "#FFE4E1");
            NAMES.put("mistyrose2", "#EED5D2");
            NAMES.put("mistyrose3", "#CDB7B5");
            NAMES.put("mistyrose4", "#8B7D7B");
            NAMES.put("moccasin", "#FFE4B5");
            NAMES.put("navajowhite", "#FFDEAD");
            NAMES.put("navajowhite1", "#FFDEAD");
            NAMES.put("navajowhite2", "#EECFA1");
            NAMES.put("navajowhite3", "#CDB38B");
            NAMES.put("navajowhite4", "#8B795E");
            NAMES.put("navy", "#000080");
            NAMES.put("navyblue", "#000080");
            NAMES.put("oldlace", "#FDF5E6");
            NAMES.put("olivedrab", "#6B8E23");
            NAMES.put("olivedrab1", "#C0FF3E");
            NAMES.put("olivedrab2", "#B3EE3A");
            NAMES.put("olivedrab3", "#9ACD32");
            NAMES.put("olivedrab4", "#698B22");
            NAMES.put("orange", "#FFA500");
            NAMES.put("orange1", "#FFA500");
            NAMES.put("orange2", "#EE9A00");
            NAMES.put("orange3", "#CD8500");
            NAMES.put("orange4", "#8B5A00");
            NAMES.put("orangered", "#FF4500");
            NAMES.put("orangered1", "#FF4500");
            NAMES.put("orangered2", "#EE4000");
            NAMES.put("orangered3", "#CD3700");
            NAMES.put("orangered4", "#8B2500");
            NAMES.put("orchid", "#DA70D6");
            NAMES.put("orchid1", "#FF83FA");
            NAMES.put("orchid2", "#EE7AE9");
            NAMES.put("orchid3", "#CD69C9");
            NAMES.put("orchid4", "#8B4789");
            NAMES.put("palegoldenrod", "#EEE8AA");
            NAMES.put("palegreen", "#98FB98");
            NAMES.put("palegreen1", "#9AFF9A");
            NAMES.put("palegreen2", "#90EE90");
            NAMES.put("palegreen3", "#7CCD7C");
            NAMES.put("palegreen4", "#548B54");
            NAMES.put("paleturquoise", "#AFEEEE");
            NAMES.put("paleturquoise1", "#BBFFFF");
            NAMES.put("paleturquoise2", "#AEEEEE");
            NAMES.put("paleturquoise3", "#96CDCD");
            NAMES.put("paleturquoise4", "#668B8B");
            NAMES.put("palevioletred", "#DB7093");
            NAMES.put("palevioletred1", "#FF82AB");
            NAMES.put("palevioletred2", "#EE799F");
            NAMES.put("palevioletred3", "#CD6889");
            NAMES.put("palevioletred4", "#8B475D");
            NAMES.put("papayawhip", "#FFEFD5");
            NAMES.put("peachpuff", "#FFDAB9");
            NAMES.put("peachpuff1", "#FFDAB9");
            NAMES.put("peachpuff2", "#EECBAD");
            NAMES.put("peachpuff3", "#CDAF95");
            NAMES.put("peachpuff4", "#8B7765");
            NAMES.put("peru", "#CD853F");
            NAMES.put("pink", "#FFC0CB");
            NAMES.put("pink1", "#FFB5C5");
            NAMES.put("pink2", "#EEA9B8");
            NAMES.put("pink3", "#CD919E");
            NAMES.put("pink4", "#8B636C");
            NAMES.put("plum", "#DDA0DD");
            NAMES.put("plum1", "#FFBBFF");
            NAMES.put("plum2", "#EEAEEE");
            NAMES.put("plum3", "#CD96CD");
            NAMES.put("plum4", "#8B668B");
            NAMES.put("powderblue", "#B0E0E6");
            NAMES.put("purple", "#A020F0");
            NAMES.put("purple1", "#9B30FF");
            NAMES.put("purple2", "#912CEE");
            NAMES.put("purple3", "#7D26CD");
            NAMES.put("purple4", "#551A8B");
            NAMES.put("red", "#FF0000");
            NAMES.put("red1", "#FF0000");
            NAMES.put("red2", "#EE0000");
            NAMES.put("red3", "#CD0000");
            NAMES.put("red4", "#8B0000");
            NAMES.put("rosybrown", "#BC8F8F");
            NAMES.put("rosybrown1", "#FFC1C1");
            NAMES.put("rosybrown2", "#EEB4B4");
            NAMES.put("rosybrown3", "#CD9B9B");
            NAMES.put("rosybrown4", "#8B6969");
            NAMES.put("royalblue", "#4169E1");
            NAMES.put("royalblue1", "#4876FF");
            NAMES.put("royalblue2", "#436EEE");
            NAMES.put("royalblue3", "#3A5FCD");
            NAMES.put("royalblue4", "#27408B");
            NAMES.put("snewData.addlebrown", "#8B4513");
            NAMES.put("salmon", "#FA8072");
            NAMES.put("salmon1", "#FF8C69");
            NAMES.put("salmon2", "#EE8262");
            NAMES.put("salmon3", "#CD7054");
            NAMES.put("salmon4", "#8B4C39");
            NAMES.put("sandybrown", "#F4A460");
            NAMES.put("seagreen", "#2E8B57");
            NAMES.put("seagreen1", "#54FF9F");
            NAMES.put("seagreen2", "#4EEE94");
            NAMES.put("seagreen3", "#43CD80");
            NAMES.put("seagreen4", "#2E8B57");
            NAMES.put("seashell", "#FFF5EE");
            NAMES.put("seashell1", "#FFF5EE");
            NAMES.put("seashell2", "#EEE5DE");
            NAMES.put("seashell3", "#CDC5BF");
            NAMES.put("seashell4", "#8B8682");
            NAMES.put("sienna", "#A0522D");
            NAMES.put("sienna1", "#FF8247");
            NAMES.put("sienna2", "#EE7942");
            NAMES.put("sienna3", "#CD6839");
            NAMES.put("sienna4", "#8B4726");
            NAMES.put("skyblue", "#87CEEB");
            NAMES.put("skyblue1", "#87CEFF");
            NAMES.put("skyblue2", "#7EC0EE");
            NAMES.put("skyblue3", "#6CA6CD");
            NAMES.put("skyblue4", "#4A708B");
            NAMES.put("slateblue", "#6A5ACD");
            NAMES.put("slateblue1", "#836FFF");
            NAMES.put("slateblue2", "#7A67EE");
            NAMES.put("slateblue3", "#6959CD");
            NAMES.put("slateblue4", "#473C8B");
            NAMES.put("slategray", "#708090");
            NAMES.put("slategray1", "#C6E2FF");
            NAMES.put("slategray2", "#B9D3EE");
            NAMES.put("slategray3", "#9FB6CD");
            NAMES.put("slategray4", "#6C7B8B");
            NAMES.put("slategrey", "#708090");
            NAMES.put("snow", "#FFFAFA");
            NAMES.put("snow1", "#FFFAFA");
            NAMES.put("snow2", "#EEE9E9");
            NAMES.put("snow3", "#CDC9C9");
            NAMES.put("snow4", "#8B8989");
            NAMES.put("springgreen", "#00FF7F");
            NAMES.put("springgreen1", "#00FF7F");
            NAMES.put("springgreen2", "#00EE76");
            NAMES.put("springgreen3", "#00CD66");
            NAMES.put("springgreen4", "#008B45");
            NAMES.put("steelblue", "#4682B4");
            NAMES.put("steelblue1", "#63B8FF");
            NAMES.put("steelblue2", "#5CACEE");
            NAMES.put("steelblue3", "#4F94CD");
            NAMES.put("steelblue4", "#36648B");
            NAMES.put("tan", "#D2B48C");
            NAMES.put("tan1", "#FFA54F");
            NAMES.put("tan2", "#EE9A49");
            NAMES.put("tan3", "#CD853F");
            NAMES.put("tan4", "#8B5A2B");
            NAMES.put("thistle", "#D8BFD8");
            NAMES.put("thistle1", "#FFE1FF");
            NAMES.put("thistle2", "#EED2EE");
            NAMES.put("thistle3", "#CDB5CD");
            NAMES.put("thistle4", "#8B7B8B");
            NAMES.put("tomato", "#FF6347");
            NAMES.put("tomato1", "#FF6347");
            NAMES.put("tomato2", "#EE5C42");
            NAMES.put("tomato3", "#CD4F39");
            NAMES.put("tomato4", "#8B3626");
            NAMES.put("turquoise", "#40E0D0");
            NAMES.put("turquoise1", "#00F5FF");
            NAMES.put("turquoise2", "#00E5EE");
            NAMES.put("turquoise3", "#00C5CD");
            NAMES.put("turquoise4", "#00868B");
            NAMES.put("violet", "#EE82EE");
            NAMES.put("violetred", "#D02090");
            NAMES.put("violetred1", "#FF3E96");
            NAMES.put("violetred2", "#EE3A8C");
            NAMES.put("violetred3", "#CD3278");
            NAMES.put("violetred4", "#8B2252");
            NAMES.put("wheat", "#F5DEB3");
            NAMES.put("wheat1", "#FFE7BA");
            NAMES.put("wheat2", "#EED8AE");
            NAMES.put("wheat3", "#CDBA96");
            NAMES.put("wheat4", "#8B7E66");
            NAMES.put("whitesmoke", "#F5F5F5");
            NAMES.put("yellow", "#FFFF00");
            NAMES.put("yellow1", "#FFFF00");
            NAMES.put("yellow2", "#EEEE00");
            NAMES.put("yellow3", "#CDCD00");
            NAMES.put("yellow4", "#8B8B00");
            NAMES.put("yellowgreen", "#9ACD32");
            for (Map.Entry<String, Object> entry : NAMES.entrySet()) {
                if (entry.getValue() instanceof GridColor) {
                    // nothing to do
                } else if (entry.getValue() instanceof String) {
                    entry.setValue(parseHex((String) entry.getValue()));
                } else {
                    throw RInternalError.shouldNotReachHere();
                }
            }
        }
    }
}
