package com.oracle.truffle.r.test.rpackages;

import java.nio.file.*;
import java.util.*;

import org.junit.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.test.*;

/**
 * Tests related to the loading, etc. of R packages.
 */
public class TestRPackages extends TestBase {
    private static class ThisWhiteList extends WhiteList {

        protected ThisWhiteList() {
            super("RPackagesWhiteList.test");
        }

    }

    @Test
    public void testLoadVanilla() {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path rpackages = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test", "rpackages");
        Path rpackagesLibs = rpackages.resolve("testrlibs_user");
        Path relLibsPath = cwd.relativize(rpackagesLibs);
        if (!relLibsPath.toFile().exists()) {
            relLibsPath.toFile().mkdir();
        }
        Path rpackagesDists = rpackages.resolve("distributions");
        Path vanilla = rpackagesDists.resolve("vanilla_1.0.tar.gz");
        // install the package (using GnuR for now)
        ProcessBuilder pb = new ProcessBuilder("R", "CMD", "install", vanilla.toString());
        Map<String, String> env = pb.environment();
        env.put("R_LIBS_USER", rpackagesLibs.toString());
        try {
            Process install = pb.start();
            int rc = install.waitFor();
            assertTrue(rc == 0);
            assertTemplateEval(new ThisWhiteList(), TestBase.template("{ library(\"vanilla\", lib.loc = \"%0\"); vanilla() }", new String[]{relLibsPath.toString()}));
        } catch (Exception ex) {
            assertFalse();
        }
    }

}
