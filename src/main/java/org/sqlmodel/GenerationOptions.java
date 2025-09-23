package org.sqlmodel;

// rn/rownum 같은 페이징 컬럼 포함?
public record GenerationOptions(boolean includePagingColumn) {

    public static GenerationOptions defaults() {
        return new GenerationOptions(false);
    }
}