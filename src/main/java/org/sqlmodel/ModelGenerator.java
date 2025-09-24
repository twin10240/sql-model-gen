package org.sqlmodel;

public interface ModelGenerator {
    /** SELECT … FROM … 만 처리 (별칭 포함), 간단 타입 규칙 포함 */
    String generateModel(String sql, String className);

    /** 오라클 페이징(SELECT * FROM (SELECT …)) 안전 처리, rn/rownum 포함 여부 옵션 */
    String generateModelPagingAware(String sql, String className, boolean includePagingColumn);

    /** 옵션 객체로 확장 가능한 버전 */
    default String generate(String sql, String className, GenerationOptions options) {
        if (options == null) return generateModel(sql, className);
//        return generateModelPagingAware(sql, className, options.includePagingColumn());
        return generateModelPagingAware(sql, className, options.isIncludePagingColumn());
    }
}