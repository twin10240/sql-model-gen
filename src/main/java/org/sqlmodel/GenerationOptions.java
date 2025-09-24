package org.sqlmodel;

// rn/rownum 같은 페이징 컬럼 포함?
//public record GenerationOptions(boolean includePagingColumn) {
//
//    public static GenerationOptions defaults() {
//        return new GenerationOptions(false);
//    }
//}

// Java 8버전용
public class GenerationOptions {

    private final boolean includePagingColumn;

    public GenerationOptions(boolean includePagingColumn) {
        this.includePagingColumn = includePagingColumn;
    }

    public boolean isIncludePagingColumn() {
        return includePagingColumn;
    }

    public static GenerationOptions defaults() {
        return new GenerationOptions(false);
    }


}