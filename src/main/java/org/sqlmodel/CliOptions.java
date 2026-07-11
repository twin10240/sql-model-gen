package org.sqlmodel;

import java.nio.file.Path;
import java.nio.file.Paths;

final class CliOptions {
    private Path sqlFile;
    private String className;
    private String packageName;
    private Path output;
    private Path config;
    private boolean overwrite;
    private boolean stdout;
    private boolean help;

    static CliOptions parse(String[] args) {
        CliOptions result = new CliOptions();
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            if ("--overwrite".equals(option)) result.overwrite = true;
            else if ("--stdout".equals(option)) result.stdout = true;
            else if ("--help".equals(option)) result.help = true;
            else if ("--sql-file".equals(option)) result.sqlFile = Paths.get(value(args, ++i, option));
            else if ("--class-name".equals(option)) result.className = value(args, ++i, option);
            else if ("--package".equals(option)) result.packageName = value(args, ++i, option);
            else if ("--output".equals(option)) result.output = Paths.get(value(args, ++i, option));
            else if ("--config".equals(option)) result.config = Paths.get(value(args, ++i, option));
            else throw new IllegalArgumentException("Unknown option: " + option);
        }
        return result;
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) throw new IllegalArgumentException("Missing value for " + option);
        return args[index];
    }

    Path sqlFile() { return sqlFile; }
    String className() { return className; }
    String packageName() { return packageName; }
    Path output() { return output; }
    Path config() { return config; }
    boolean overwrite() { return overwrite; }
    boolean stdout() { return stdout; }
    boolean help() { return help; }
}
