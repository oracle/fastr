package com.oracle.truffle.r.test.packages.analyzer.detectors;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.r.test.packages.analyzer.Location;
import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackageTestRun;

public class ConfigureErrorDetector extends LineDetector {

    public static final ConfigureErrorDetector INSTANCE = new ConfigureErrorDetector();

    private static final String PREFIX = "configure: error: ";

    protected ConfigureErrorDetector() {
    }

    @Override
    public String getName() {
        return "Configure error detector";
    }

    @Override
    public Collection<Problem> detect(RPackageTestRun pkgTestRun, Location startLocation, List<String> body) {
        Collection<Problem> problems = new LinkedList<>();
        assert body.isEmpty() || startLocation != null;
        int lineNr = startLocation != null ? startLocation.lineNr : 0;
        for (String line : body) {
            if (line.startsWith(PREFIX)) {
                String message = line.substring(PREFIX.length());
                problems.add(new ConfigureErrorProblem(pkgTestRun, this, new Location(startLocation.file, lineNr), message));

            }
            ++lineNr;
        }
        return problems;
    }

    public static class ConfigureErrorProblem extends Problem {

        private final String message;

        protected ConfigureErrorProblem(RPackageTestRun pkg, ConfigureErrorDetector detector, Location location, String message) {
            super(pkg, detector, location);
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return getLocation() + ": configure: error: " + message;
        }

        @Override
        public String getSummary() {
            return "RInternalError";
        }

        @Override
        public String getDetails() {
            return message;
        }

        @Override
        public int getSimilarityTo(Problem other) {
            if (other.getClass() == ConfigureErrorProblem.class) {
                return Problem.computeLevenshteinDistance(getDetails().trim(), other.getDetails().trim());
            }
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isSimilarTo(Problem other) {
            return getSimilarityTo(other) < 10;
        }

    }

}
