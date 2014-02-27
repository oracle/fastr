package com.oracle.truffle.r.runtime;

import java.io.*;

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
            if (value != null && BaseRFFIFactory.getRFFI().isWriteableDirectory(value)) {
                startingTempDirPath = value;
            }
        }
        if (startingTempDirPath == null) {
            startingTempDirPath = "/tmp/"; // TODO Windows
        }
        if (!startingTempDirPath.endsWith(File.separator)) {
            startingTempDirPath += startingTempDirPath;
        }
        String t = BaseRFFIFactory.getRFFI().mkdtemp(startingTempDirPath + "Rtmp" + "XXXXXX");
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
