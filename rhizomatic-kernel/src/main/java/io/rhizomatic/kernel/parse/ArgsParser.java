package io.rhizomatic.kernel.parse;

import org.jetbrains.annotations.Nullable;

/**
 * Parses command line arguments.
 */
public class ArgsParser {
    public static class Params {
        String layersPath;
        String configPath;
        String modulePath;

        @Nullable
        public String getLayersPath() {
            return layersPath;
        }

        @Nullable
        public String getConfigPath() {
            return configPath;
        }

        @Nullable
        public String getModulePath() {
            return modulePath;
        }
    }

    public static Params parseParams(String... args) {
        Params params = new Params();
        if (args == null || args.length == 0) {
            return params;
        } else if (args.length == 2) {
            parsePair(args, 0, params);
        } else if (args.length == 4) {
            parsePair(args, 0, params);
            parsePair(args, 2, params);
        } else if (args.length == 6) {
            parsePair(args, 0, params);
            parsePair(args, 2, params);
            parsePair(args, 4, params);
        } else {
            invalidArguments();
        }
        validate(params);
        return params;
    }

    private static void validate(Params params) {
        if (params.modulePath != null && params.layersPath != null) {
            throw new IllegalArgumentException("Layer location and module location are exclusive: Specify one or the other.");
        }
    }

    private static void parsePair(String[] args, int start, Params params) {
        String flag = args[start];
        String value = args[start + 1];
        parseArgument(flag, value, params);
    }

    private static void parseArgument(String flag, String value, Params params) {
        if ("-c".equals(flag)) {
            params.configPath = value;
        } else if ("-l".equals(flag)) {
            params.layersPath = value;
        } else if ("-m".equals(flag)) {
            params.modulePath = value;
        } else {
            invalidArguments();
        }
    }

    private static void invalidArguments() {
        throw new IllegalArgumentException("Valid options are <none>, -c <config location>, -l <layer location>, -m module <module location>");
    }


}
