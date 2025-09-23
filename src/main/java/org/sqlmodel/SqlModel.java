package org.sqlmodel;

/** 간편 정적 API (내부 싱글턴 구현에 위임) */
public final class SqlModel {
    private static final ModelGenerator GEN = new SimpleModelGeneratorImpl();

    private SqlModel() {}

    public static String generate(String sql, String className) {
        return GEN.generateModel(sql, className);
    }

    public static String generatePagingAware(String sql, String className, boolean includePagingColumn) {
        return GEN.generateModelPagingAware(sql, className, includePagingColumn);
    }

    public static String generate(String sql, String className, GenerationOptions options) {
        return GEN.generate(sql, className, options);
    }
}