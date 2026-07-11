package org.sqlmodel;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        if (args.length == 1 && "--help".equals(args[0])) {
            System.out.println("Usage: modelconvertor [options]");
            return 0;
        }
        return 2;
    }
}
