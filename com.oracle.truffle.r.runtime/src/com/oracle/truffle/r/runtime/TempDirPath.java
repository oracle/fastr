package com.oracle.truffle.r.runtime;

import com.oracle.truffle.r.runtime.ffi.*;

/**
 * 
 * As per the GnuR spec, the tempdir() directory is identified on startup.
 * 
 */
public class TempDirPath {

    private static String tempDirPath;

    static {
        //
        final String[] envVars = new String[]{"TMPDIR", "TMP", "TEMP"};
        String startingTempDirPath = null;
        for (String envVar : envVars) {
            String value = System.getenv(envVar);
            if (value != null && BaseRFFIFactory.getFFI().isWriteableDirectory(value)) {
                startingTempDirPath = value;
            }
        }
        if (startingTempDirPath == null) {
            startingTempDirPath = "/tmp";
        }
        String t = BaseRFFIFactory.getFFI().mkdtemp(startingTempDirPath + "/Rtmp" + "XXXXXX");
        if (t != null) {
            tempDirPath = t;
        } else {
            Utils.fail("cannot create 'R_TempDir'");
        }
    }

    public static String tempDirPath() {
        return tempDirPath;
    }
}
